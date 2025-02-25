package com.example.demo.repository;

import com.example.demo.entity.Metadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MetadataRepository extends JpaRepository<Metadata, Long>, JpaSpecificationExecutor<Metadata> {
    // Custom query methods if needed
}
