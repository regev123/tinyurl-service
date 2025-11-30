package com.tinyurl.lookup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Lookup Service Application
 * Microservice dedicated to URL lookup and redirect operations
 * 
 * Scans common module for entities and repositories
 */
@SpringBootApplication(scanBasePackages = {"com.tinyurl.lookup", "com.tinyurl"})
@EntityScan(basePackages = "com.tinyurl.entity")
@EnableJpaRepositories(basePackages = "com.tinyurl.lookup.repository")
@EnableScheduling  // For cleanup jobs if needed
public class LookupServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LookupServiceApplication.class, args);
    }
}

