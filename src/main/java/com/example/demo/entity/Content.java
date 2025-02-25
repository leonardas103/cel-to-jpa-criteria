package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "content")
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "name", nullable = false)
    private String name;

    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Metadata> metadata = new ArrayList<>();

    // Constructors
    public Content() {}

    public Content(LocalDateTime createdAt, String name) {
        this.createdAt = createdAt;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getName() {
        return name;
    }

    public List<Metadata> getMetadata() {
        return metadata;
    }

    public void setName(String name) {
        this.name = name;
    }
}