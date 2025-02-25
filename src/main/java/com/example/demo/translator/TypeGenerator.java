package com.example.demo.translator;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import dev.cel.common.CelVarDecl;
import dev.cel.common.types.SimpleType;
import dev.cel.common.types.CelType;
import dev.cel.common.types.ListType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.JoinColumn;

public class TypeGenerator {
    private final Class<?> rootEntityClass;
    private final Map<String, CelType> fieldMap = new HashMap<>();
    private final Set<String> blacklist = new HashSet<>();
    private final Set<Class<?>> processedClasses = new HashSet<>(); // Prevent infinite recursion

    public TypeGenerator(Class<?> rootEntityClass) {
        this.rootEntityClass = rootEntityClass;
        addFieldsFromClass(rootEntityClass, "");
    }

    /**
     * Adds fields as an array of (name, CelType) pairs.
     */
    public TypeGenerator addFields(Map<String, CelType> fields) {
        fieldMap.putAll(fields);
        return this;
    }

    public TypeGenerator blacklist(Set<String> blacklist) {
        this.blacklist.addAll(blacklist);
        return this;
    }

    public List<CelVarDecl> build() {
        return fieldMap.entrySet().stream()
                .filter(entry -> !blacklist.contains(entry.getKey()))
                .map(entry -> CelVarDecl.newVarDeclaration(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Recursively adds fields from a class, handling @OneToMany relationships.
     */
    private void addFieldsFromClass(Class<?> clazz, String prefix) {
        if (processedClasses.contains(clazz)) {
            return; // Prevent infinite recursion
        }
        processedClasses.add(clazz);

        for (Field field : clazz.getDeclaredFields()) {
            String fieldName = prefix + field.getName();
            CelType celType = getCelTypeForField(field);
            if (celType != null) {
                fieldMap.put(fieldName, celType);
            }

            // Handle @OneToMany relationships
            if (field.isAnnotationPresent(OneToMany.class)) {
                Class<?> elementType = getElementType(field);
                if (elementType != null) {
                    addFieldsFromClass(elementType, fieldName + ".");
                }
            }

            // Handle @JoinColumn fields
            if (field.isAnnotationPresent(JoinColumn.class)) {
                JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
                String joinColumnName = prefix + joinColumn.name();
                fieldMap.put(joinColumnName, SimpleType.INT);
            }
        }
    }

    /**
     * Maps a Java field type to a CelType.
     */
    private CelType getCelTypeForField(Field field) {
        Class<?> fieldType = field.getType();
        if (fieldType == int.class || fieldType == Integer.class) {
            return SimpleType.INT;
        } else if (fieldType == long.class || fieldType == Long.class) {
            return SimpleType.INT;
        } else if (fieldType == String.class) {
            return SimpleType.STRING;
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            return SimpleType.BOOL;
        } else if (fieldType == double.class || fieldType == Double.class) {
            return SimpleType.DOUBLE;
        } else if (fieldType == java.time.LocalDateTime.class) {
            return SimpleType.TIMESTAMP;
        } else if (List.class.isAssignableFrom(fieldType)) {
            return ListType.create(SimpleType.DYN); // Default to DYN for list elements
        } else if (fieldType.isAnnotationPresent(Entity.class)) {
            return null; // Ignore entity fields (handled separately)
        } else {
            throw new IllegalArgumentException("Unsupported field type: " + fieldType);
        }
    }

    /**
     * Extracts the element type from a List field (e.g., List<String> -> String).
     */
    private Class<?> getElementType(Field field) {
        if (List.class.isAssignableFrom(field.getType())) {
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericType;
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                if (typeArguments.length > 0 && typeArguments[0] instanceof Class) {
                    return (Class<?>) typeArguments[0];
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "TypeGenerator:" + rootEntityClass.getSimpleName() +
                "{" +
                "fieldMap=" + fieldMap +
                ", blacklist=" + blacklist +
                '}';
    }
}