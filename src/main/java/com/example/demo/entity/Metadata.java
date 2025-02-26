package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

@Getter
@Setter
@ToString
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

}