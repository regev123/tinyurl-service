package com.tinyurl.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a URL mapping (original URL to short URL)
 * Follows Single Responsibility Principle - only represents URL mapping data
 * Follows Encapsulation - data is properly encapsulated with JPA annotations
 */
@Entity
@Table(name = "url_mappings", indexes = {
    @Index(columnList = "shortUrl"),
    @Index(columnList = "originalUrl")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlMapping {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 5000)
    private String originalUrl;
    
    @Column(nullable = false, unique = true, length = 10)
    private String shortUrl;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(nullable = false)
    private Long accessCount = 0L;
    
    @Column
    private LocalDateTime lastAccessedAt;
}
