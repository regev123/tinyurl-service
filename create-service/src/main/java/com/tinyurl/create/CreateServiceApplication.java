package com.tinyurl.create;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Create Service Application
 * Microservice dedicated to URL creation operations
 * 
 * Scans common module for entities and repositories
 */
@SpringBootApplication(scanBasePackages = {"com.tinyurl.create", "com.tinyurl"})
@EntityScan(basePackages = "com.tinyurl.entity")
@EnableJpaRepositories(basePackages = "com.tinyurl.create.repository")
public class CreateServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CreateServiceApplication.class, args);
    }
}

