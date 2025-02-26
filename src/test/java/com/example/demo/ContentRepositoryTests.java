package com.example.demo;

import com.example.demo.controller.FilterController;
import com.example.demo.entity.Content;
import com.example.demo.entity.Metadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.engine.discovery.predicates.IsPotentialTestContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback
public class ContentRepositoryTests {

    @Autowired
    private EntityManager entityManager;
    private static final Logger logger = LoggerFactory.getLogger(ContentRepositoryTests.class);

    @BeforeEach
    public void setup() {
//        entityManager.createQuery("DELETE FROM Metadata").executeUpdate();
//        entityManager.createQuery("DELETE FROM Content").executeUpdate();
//
//        Content content = new Content(LocalDateTime.now(), "Sample Content Name");
//
//        Metadata metadata1 = new Metadata(content, "author", "Alice");
//        Metadata metadata2 = new Metadata(content, "title", "Spring Boot Testing");
//
//        content.getMetadata().add(metadata1);
//        content.getMetadata().add(metadata2);
//
//        entityManager.persist(content);
//        entityManager.flush();
    }



    @Test
    public void testFindContentsByMetadata() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Content> query = cb.createQuery(Content.class);
        Root<Content> root = query.from(Content.class);
        System.out.println("metadata: " + root.get("metadata"));
        System.out.println("Entity Class: " + root.getJavaType().getSimpleName());


        // Use Metamodel to retrieve entity attributes
        Metamodel metamodel = entityManager.getMetamodel();
        EntityType<Content> entityType = metamodel.entity(Content.class);

        // Print singular attributes (basic fields)
        System.out.println("Singular Attributes:");
        Set<SingularAttribute<? super Content, ?>> singularAttributes = entityType.getSingularAttributes();
        for (SingularAttribute<? super Content, ?> attribute : singularAttributes) {
            System.out.println("- " + attribute.getName() +":"+ attribute.getJavaType());
        }

        // Print collection attributes (like metadata)
        System.out.println("Collection Attributes:");
        Set<PluralAttribute<? super Content, ?, ?>> collectionAttributes = entityType.getPluralAttributes();
        for (PluralAttribute<? super Content, ?, ?> attribute : collectionAttributes) {
            System.out.println("- " + attribute.getName()+":"+attribute.getCollectionType());
        }

//        query.select(root).where(cb.isNotEmpty(root.get("metadata")));  // Content where metadata is not empty
//        query.select(root).where(cb.equal(metadataJoin.get("id"), 1));

        List<Content> results = entityManager.createQuery(query).getResultList();
        for (Content content : results) {
            System.out.println("############: " + content);
            System.out.println("Metadata:" + content.getMetadata());
            System.out.println("----------------------");
        }
    }

    @Test
    public void test2() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Content> query = cb.createQuery(Content.class);
        Root<Content> root = query.from(Content.class);
        Join<Content, Metadata> metadataJoin = root.join("metadata");

        query.select(root)
                .where(cb.equal(metadataJoin.get("datakey"), "author"),
                        cb.equal(metadataJoin.get("datavalue"), "Alice"));

        List<Content> results = entityManager.createQuery(query).getResultList();
        System.out.println("####################################################################");
        System.out.println("metadata: " + root.get("metadata").getAlias());
        System.out.println("metadataJoin: " + root.get("metadata.id").getAlias());
        System.out.println("metadataJoin.datakey: " + metadataJoin.get("datakey").getAlias());
        System.out.println("metadataJoin.datavalue: " + metadataJoin.get("datavalue").getAlias());

        logger.info("CriteriaQuery structure: {}", query.toString());
        logger.info("CriteriaQuery getParameters: {}", query.getParameters());
        logger.info("CriteriaQuery getRoots: {}", query.getRoots());
        logger.info("CriteriaQuery getRoots: {}", query.getRoots());

        assertEquals(1, results.size());

        // Logging results using SLF4J
        logger.info("Number of Content items retrieved: {}", results.size());
        for (Content content : results) {
            logger.info("Content ID: {}, Name: {}", content.getId(), content.getName());
            for (Metadata metadata : content.getMetadata()) {
                logger.info(" - Metadata Key: {}, Value: {}", metadata.getDatakey(), metadata.getDatavalue());
            }
        }
    }

}
