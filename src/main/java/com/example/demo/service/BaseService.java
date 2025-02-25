package com.example.demo.service;

import com.example.demo.translator.Translator;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;

public class BaseService<T> {

    private final JpaRepository<T, Long> repository;
    private final Class<T> entityClass; // Store the entity class

    public BaseService(JpaRepository<T, Long> repository, Class<T> entityClass) {
        this.repository = repository;
        this.entityClass = entityClass;
    }

    public List<T> findAll() {
        return repository.findAll();
    }

    public Optional<T> findById(Long id) {
        return repository.findById(id);
    }

    public List<T> filterEntity(String celExpression) throws Exception {
        Specification<T> spec = Translator.translate(celExpression, entityClass); // Pass entityClass
        return ((JpaSpecificationExecutor<T>) repository).findAll(spec);
    }
}