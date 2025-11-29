package com.tinyurl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class
 * Enables scheduling for cleanup jobs and other scheduled tasks
 */
@SpringBootApplication
@EnableScheduling
public class TinyUrlServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TinyUrlServiceApplication.class, args);
    }
}
