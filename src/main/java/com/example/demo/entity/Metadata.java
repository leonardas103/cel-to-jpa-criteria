package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;

@Entity
@Table(name = "metadata")
public class Metadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "content_id", nullable = false) // Matches the schema
    @JsonBackReference
    private Content content;

    @Column(name = "datakey", nullable = false)
    private String datakey;

    @Column(name = "datavalue", nullable = false)
    private String datavalue;

    public Metadata() {}

    public Metadata(Content content, String datakey, String datavalue) {
        this.content = content;
        this.datakey = datakey;
        this.datavalue = datavalue;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public Content getContent() {
        return content;
    }

    public String getDatakey() {
        return datakey;
    }

    public String getDatavalue() {
        return datavalue;
    }

    public void setContent(Content content) {
        this.content = content;
    }

    public void setDatakey(String datakey) {
        this.datakey = datakey;
    }

    public void setDatavalue(String datavalue) {
        this.datavalue = datavalue;
    }
}