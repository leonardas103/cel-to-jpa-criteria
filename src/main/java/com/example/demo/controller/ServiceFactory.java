package com.example.demo.controller;

import com.example.demo.entity.Content;
import com.example.demo.entity.Metadata;
import com.example.demo.repository.ContentRepository;
import com.example.demo.repository.MetadataRepository;
import com.example.demo.service.BaseService;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class ServiceFactory {

    private final Map<String, BaseService<?>> services = new HashMap<>();

    public ServiceFactory(ContentRepository contentRepository, MetadataRepository metadataRepository) {
        // Map entity names to their respective services
        this.services.put("content", new BaseService<>(contentRepository, Content.class));
        this.services.put("metadata", new BaseService<>(metadataRepository, Metadata.class));
    }

    public BaseService<?> getService(String entity) {
        BaseService<?> service = services.get(entity.toLowerCase());
        if (service == null) {
            throw new IllegalArgumentException("Invalid entity type: " + entity);
        }
        return service;
    }
}