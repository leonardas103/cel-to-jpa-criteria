package com.example.demo;

import com.example.demo.entity.Content;
import com.example.demo.entity.Metadata;
import com.example.demo.translator.TypeGenerator;
import dev.cel.common.CelVarDecl;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;

//@SpringBootTest
public class LoggingTest {

    private static final Logger logger = LoggerFactory.getLogger(LoggingTest.class);

    @Test
    public void testLoggingOutput() {

        // Create the generator and add fields
        TypeGenerator generator = new TypeGenerator(Content.class);
//                .add(Metadata.class)

        // Print the current state of the generator
        System.out.println("Generator State:");
        System.out.println(generator);

        // Build the final list of CelVarDecl objects
        List<CelVarDecl> varDeclarations = generator.build();

        // Print the final result
        System.out.println("\nGenerated CelVarDecl Objects:");
        varDeclarations.forEach(System.out::println);


//        System.out.println("This is a system out print.");
//        logger.debug("This is a debug log.");
//        logger.info("This is an info log.");
//        logger.warn("This is a warning log.");
//        logger.error("This is an error log.");
    }
}