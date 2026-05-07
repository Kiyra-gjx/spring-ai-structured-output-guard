package io.github.kiyragjx.saiguard.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Example Spring Boot application for the structured-output guard starter.
 */
@SpringBootApplication
public class ExampleApplication {

    /**
     * Creates the example application bootstrap object.
     */
    public ExampleApplication() {
    }

    /**
     * Starts the example application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }
}
