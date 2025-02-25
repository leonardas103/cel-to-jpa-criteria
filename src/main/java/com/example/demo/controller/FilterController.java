package com.example.demo.controller;

import com.example.demo.repository.ContentRepository;
import com.example.demo.repository.MetadataRepository;
import com.example.demo.service.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/{entity}")
public class FilterController {

    private static final Logger logger = LoggerFactory.getLogger(FilterController.class);
    private final ServiceFactory services;
    public static class Resp { public String filter; }

    public FilterController(ServiceFactory serviceFactory) {
        this.services = serviceFactory;
    }

    @PostMapping
    public ResponseEntity<?> applyFilter(@PathVariable String entity, @RequestBody Resp response) {
        if (response == null || response.filter == null || response.filter.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Filter cannot be empty");
        }
        try {
            BaseService<?> service = services.getService(entity);
            return ResponseEntity.ok(service.filterEntity(response.filter));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing request: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> applyParamFilter(@PathVariable String entity, @RequestParam(required = false) String filter) {
        logger.info("Calling [{}] with {}", entity, filter);
        try {
            BaseService<?> service = services.getService(entity);
            if (filter != null && !filter.trim().isEmpty()) {
                logger.info("Calling service: {}", service);
                return ResponseEntity.ok(service.filterEntity(filter));
            }
            return ResponseEntity.ok(service.findAll());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing request: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEntityById(@PathVariable String entity, @PathVariable Long id) {
        logger.info("Calling [{}] with id={}", entity, id);

        try {
            BaseService<?> service = services.getService(entity);
            logger.info("Using service: {}", service.getClass().getSimpleName());

            Optional<?> entityResult = service.findById(id);
            return  entityResult.isPresent()? ResponseEntity.ok(entityResult.get()) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error processing request", e);
            return ResponseEntity.badRequest().body("Error processing request: " + e.getMessage());
        }
    }

}

