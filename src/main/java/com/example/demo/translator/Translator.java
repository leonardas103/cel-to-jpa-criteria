package com.example.demo.translator;

import com.example.demo.entity.Metadata;
import com.google.common.collect.ImmutableList;
import dev.cel.common.ast.CelConstant;
import dev.cel.common.ast.CelExpr;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.util.List;
import java.util.Optional;

public class Translator {
    private static final Logger logger = LoggerFactory.getLogger(Translator.class);

    public static <T> Specification<T> translate(String celExpression, Class<T> entityClass) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            try {
                CelCompiler celCompiler = CelCompilerFactory.standardCelCompilerBuilder()
                        .addVarDeclarations(new TypeGenerator(entityClass).build())
                        .build();
                query.distinct(true);
                return createPredicate(celCompiler.compile(celExpression).getAst().getExpr(), root, query, cb);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        };
    }

    private static <T> Predicate createPredicate(CelExpr expr, Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        logger.info("Processing expression of kind: {}", expr.getKind());
        switch (expr.getKind()) {
            case CALL:
                return processFunctionCall(expr.call(), root, query, cb);
            case CONSTANT:
                return processConstant(expr, cb, Predicate.class);
            case IDENT:
                return cb.conjunction();
            case SELECT:
                return cb.equal(
                        resolveFieldPath(expr.select().operand(), root, cb, query).get(expr.select().field()),
                        processConstant(expr.select().operand(), cb, Expression.class)
                );
            default:
                throw new UnsupportedOperationException("Unsupported CEL expression kind: " + expr.getKind());
        }
    }

    // The function call processing now passes the query along to every subcall.
    private static <T> Predicate processFunctionCall(CelExpr.CelCall callExpr, Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        String operator = callExpr.function();
        List<CelExpr> arguments = callExpr.args();
        Optional<CelExpr> target = callExpr.target();
        logger.info("Handling Function: {} \nArguments: {} \nTarget: {}", operator, arguments, target);

        switch (operator) {
            case "_&&_":
                return cb.and(
                        createPredicate(arguments.get(0), root, query, cb),
                        createPredicate(arguments.get(1), root, query, cb)
                );
            case "_||_":
                return cb.or(
                        createPredicate(arguments.get(0), root, query, cb),
                        createPredicate(arguments.get(1), root, query, cb)
                );
            case "!_":
                CelExpr negatedExpr = arguments.get(0);
                return handleNegation(negatedExpr, root, query, cb);
            case "_==_":
                return cb.equal(
                        resolveFieldPath(arguments.get(0), root, cb, query),
                        processConstant(arguments.get(1), cb, Expression.class)
                );
            case "_!=_":
                return cb.notEqual(
                        resolveFieldPath(arguments.get(0), root, cb, query),
                        processConstant(arguments.get(1), cb, Expression.class)
                );
            case "_>_":
                return cb.gt(
                        resolveFieldPath(arguments.get(0), root, cb, query),
                        processConstant(arguments.get(1), cb, Number.class)
                );
            case "_>=_":
                return cb.ge(
                        resolveFieldPath(arguments.get(0), root, cb, query),
                        processConstant(arguments.get(1), cb, Number.class)
                );
            case "_<_":
                return cb.lt(
                        resolveFieldPath(arguments.get(0), root, cb, query),
                        processConstant(arguments.get(1), cb, Number.class)
                );
            case "_<=_":
                return cb.le(
                        resolveFieldPath(arguments.get(0), root, cb, query),
                        processConstant(arguments.get(1), cb, Number.class)
                );
            case "@in":
                CriteriaBuilder.In<Object> inPredicate = cb.in(resolveFieldPath(arguments.get(0), root, cb, query));
                ImmutableList<CelExpr> elements = arguments.get(1).list().elements();
                Class<?> clazz = elements.get(0).constant().getKind() == CelConstant.Kind.STRING_VALUE ? String.class : Number.class;
                elements.stream()
                        .map(element -> processConstant(element, cb, clazz))
                        .forEach(inPredicate::value);
                return inPredicate;
            case "startsWith":
                return processString(target, arguments, root, cb, "%s%%", query);
            case "contains":
                return processString(target, arguments, root, cb, "%%%s%%", query);
            default:
                throw new UnsupportedOperationException("Unsupported CEL operator: " + operator);
        }
    }

    // When handling negation, we now pass the query parameter along.
    private static <T> Predicate handleNegation(CelExpr celExpr, Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        if (celExpr.getKind() == CelExpr.ExprKind.Kind.CALL) {
            CelExpr.CelCall innerCall = celExpr.call();
            String innerOperator = innerCall.function();
            List<CelExpr> innerArgs = innerCall.args();

            // Apply De Morgan’s laws: invert &&/|| accordingly.
            if ("_&&_".equals(innerOperator)) {
                return cb.or(
                        handleNegation(innerArgs.get(0), root, query, cb),
                        handleNegation(innerArgs.get(1), root, query, cb)
                );
            } else if ("_||_".equals(innerOperator)) {
                return cb.and(
                        handleNegation(innerArgs.get(0), root, query, cb),
                        handleNegation(innerArgs.get(1), root, query, cb)
                );
            }
        }
        return cb.not(createPredicate(celExpr, root, query, cb));
    }


    // This helper walks through a dot‐notation field path (e.g. “metadata.id”)
    // using property navigation rather than an explicit join.
    private static <T, Y> Path<Y> resolveFieldPath(CelExpr expr, Root<T> root, CriteriaBuilder cb, CriteriaQuery<?> query) {
        logger.info("Resolving path: {}", expr.toString().replace("\n", " "));
        switch (expr.getKind()) {
            case IDENT:
                String[] parts = expr.ident().name().split("\\.");
                From<?, ?> currentFrom = root;
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i];
                    if (i == parts.length - 1) {
                        return currentFrom.get(part);
                    } else {
                        // Join the association (supports OneToMany/ManyToOne)
                        currentFrom = currentFrom.join(part, JoinType.LEFT);
                    }
                }
                throw new IllegalArgumentException("Invalid path: " + expr.ident().name());
            case SELECT:
                // Handle nested paths via joins
                Path<?> parentPath = resolveFieldPath(expr.select().operand(), root, cb, query);
                return parentPath.get(expr.select().field());
            default:
                throw new IllegalArgumentException("Unsupported field path expression: " + expr.getKind());
        }
    }

    // Process a constant value from the CEL AST.
    private static <T, Y> Y processConstant(CelExpr expr, CriteriaBuilder cb, Class<Y> returnType) {
        if (expr.getKind() != CelExpr.ExprKind.Kind.CONSTANT) {
            throw new IllegalArgumentException("Expected constant expression, got: " + expr.getKind());
        }
        Object result = getConstant(expr.constant(), cb, returnType);
        return returnType.cast(result);
    }

    // In case you need a “like” predicate.
    private static <T> Predicate processString(Optional<CelExpr> target, List<CelExpr> arguments,
                                               Root<T> root, CriteriaBuilder cb, String patternFormat, CriteriaQuery<?> query) {
        String value = processConstant(arguments.get(0), cb, String.class);
        String pattern = String.format(patternFormat, value);
        Path<String> fieldPath = resolveFieldPath(target.orElseThrow(), root, cb, query);
        return cb.like(fieldPath, cb.literal(pattern));
    }

    // Translate CEL constants to CriteriaBuilder literals or predicates.
    private static Object getConstant(CelConstant constant, CriteriaBuilder cb, Class<?> returnType) {
        switch (constant.getKind()) {
            case INT64_VALUE:
                if (returnType == Expression.class) return cb.literal(constant.int64Value());
                return constant.int64Value();
            case DOUBLE_VALUE:
                if (returnType == Expression.class) return cb.literal(constant.doubleValue());
                return constant.doubleValue();
            case UINT64_VALUE:
                if (returnType == Expression.class) return cb.literal(constant.uint64Value());
                return constant.uint64Value();
            case BOOLEAN_VALUE:
                if (returnType == Predicate.class) {
                    return constant.booleanValue() ? cb.conjunction() : cb.disjunction();
                }
                if (returnType == Expression.class) return cb.literal(constant.booleanValue());
                return constant.booleanValue();
            case STRING_VALUE:
                if (returnType == Predicate.class) {
                    return cb.equal(cb.literal(constant.stringValue()), cb.literal(constant.stringValue()));
                }
                if (returnType == Expression.class) return cb.literal(constant.stringValue());
                return constant.stringValue();
            default:
                throw new UnsupportedOperationException("Unsupported constant type: " + constant.getKind());
        }
    }

}