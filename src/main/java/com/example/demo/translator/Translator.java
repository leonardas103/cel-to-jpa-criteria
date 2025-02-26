/*
 * CEL Expression Notes:
 * - `dev.cel.common.ast.CelExpr`: Represents the parsed/compiled expression in the AST (Parsing Phase)
 *   Obtained via: `celCompiler.compile(expression).getAst()`
 * - `dev.cel.expr.Expr`: Represents an expression during evaluation (Execution Phase)
 *   Obtained via: `celRuntime.createProgram(ast).expression()`
 * - Operator References:
 *   - Go: https://pkg.go.dev/github.com/google/cel-go/common/operators
 *   - Java: https://javadoc.io/doc/dev.cel/cel/latest/dev/cel/parser/Operator.html
 * - Warning: This will not work with joined tables.
 */

package com.example.demo.translator;

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


import static dev.cel.common.ast.CelExpr.ExprKind.Kind.*;

public class Translator {
    private static final Logger logger = LoggerFactory.getLogger(Translator.class);

    public static <T> Specification<T> translate(String celExpression, Class<T> entityClass) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            try {
                CelCompiler celCompiler = CelCompilerFactory.standardCelCompilerBuilder()
                        .addVarDeclarations(new TypeGenerator(entityClass).build())
                        .build();
                return createPredicate(celCompiler.compile(celExpression).getAst().getExpr(), root, cb);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        };
    }

    private static <T> Predicate createPredicate(CelExpr expr, Root<T> root, CriteriaBuilder cb) {
        logger.info("Processing expression of kind: {}", expr.getKind());
        switch (expr.getKind()) {
            case CALL:
                return processFunctionCall(expr.call(), root, cb);
            case IDENT:
                return cb.conjunction();
            case SELECT:
                return cb.equal(
                        resolveFieldPath(expr.select().operand(), root).get(expr.select().field()),
                        extractConstant(expr.select().operand())
                );
            default:
                throw new UnsupportedOperationException("Unsupported CEL expression kind: " + expr.getKind());
        }
    }

    private static <T> Predicate processFunctionCall(CelExpr.CelCall callExpr, Root<T> root, CriteriaBuilder cb) {
        String operator = callExpr.function();
        List<CelExpr> arguments = callExpr.args();
        Optional<CelExpr> target = callExpr.target();
        logger.info("Handling Function: {} \nArguments: {} \nTarget: {}", operator, arguments, target);
        switch (operator) {
            case "_&&_":
                return cb.and(
                        createPredicate(arguments.get(0), root, cb),
                        createPredicate(arguments.get(1), root, cb)
                );
            case "_||_":
                return cb.or(
                        createPredicate(arguments.get(0), root, cb),
                        createPredicate(arguments.get(1), root, cb)
                );
            case "_==_":
                return cb.equal(
                        resolveFieldPath(arguments.get(0), root),
                        extractConstant(arguments.get(1))
                );
            case "_!=_":
                Path<?> resolvedFieldPath = resolveFieldPath(arguments.get(0), root);
                Object value = extractConstant(arguments.get(1));
                return cb.notEqual(resolvedFieldPath, value);
            case "_>_":
                return cb.gt(
                        resolveFieldPath(arguments.get(0), root),
                        (Number) extractConstant(arguments.get(1))
                );
            case "_>=_":
                return cb.ge(
                        resolveFieldPath(arguments.get(0), root),
                        (Number) extractConstant(arguments.get(1))
                );
            case "_<_":
                return cb.lt(
                        resolveFieldPath(arguments.get(0), root),
                        (Number) extractConstant(arguments.get(1))
                );
            case "_<=_":
                return cb.le(
                        resolveFieldPath(arguments.get(0), root),
                        (Number) extractConstant(arguments.get(1))
                );
            case "@in":
                CriteriaBuilder.In<Object> inPredicate = cb.in(resolveFieldPath(arguments.get(0), root));
                arguments.get(1).list().elements().stream().map(Translator::extractConstant).forEach(inPredicate::value);
                return inPredicate;
            case "startsWith":
                return cb.like(resolveFieldPath(target.orElseThrow(), root), arguments.get(0).constant().stringValue() +"%");
            case "contains":
                return cb.like(resolveFieldPath(target.orElseThrow(), root), "%" + arguments.get(0).constant().stringValue() +"%");
            default:
                throw new UnsupportedOperationException("Unsupported CEL operator: " + operator);
        }
    }

    private static <T, Y> Path<Y> resolveFieldPath(CelExpr expr, Root<T> root) {
        logger.info("Resolving path: {}", expr);
        switch (expr.getKind()) {
            case IDENT:
                return root.get(expr.ident().name());
            case SELECT:
                return resolveFieldPath(expr.select().operand(), root).get(expr.select().field());
            default:
                throw new IllegalArgumentException("Unsupported field path expression: " + expr.getKind());
        }
    }

    private static Object extractConstant(CelExpr node) {
        if (node.getKind() == CONSTANT) {
            CelConstant constant = node.constantOrDefault();
            switch (constant.getKind()) {
                case STRING_VALUE:
                    return constant.stringValue();
                case INT64_VALUE:
                    return constant.int64Value();
                case DOUBLE_VALUE:
                    return constant.doubleValue();
                case BOOLEAN_VALUE:
                    return constant.booleanValue();
                default:
                    throw new UnsupportedOperationException("Unsupported constant type: " + constant);
            }
        }
        throw new IllegalArgumentException("Expected a constant value but got: " + node);
    }
}