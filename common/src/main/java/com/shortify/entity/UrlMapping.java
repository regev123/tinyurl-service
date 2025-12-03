package com.shortify.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a URL mapping (original URL to short URL)
 * Follows Single Responsibility Principle - only represents URL mapping data
 * Follows Encapsulation - data is properly encapsulated with JPA annotations
 * 
 * Optimized for partitioning by creation date for better scalability
 */
@Entity
@Table(name = "url_mappings", indexes = {
    @Index(columnList = "shortUrl"),
    @Index(columnList = "originalUrl"),
    @Index(columnList = "createdDate"),
    @Index(columnList = "expiresAt")
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
    
    @Column(nullable = false, length = 10)
    private String shortUrl;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * Date field for partitioning (extracted from createdAt)
     * Used for table partitioning by date range
     */
    @Column(nullable = false)
    private LocalDate createdDate;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(nullable = false)
    private Long accessCount = 0L;
    
    @Column
    private LocalDateTime lastAccessedAt;
    
    /**
     * Shard ID for future horizontal sharding support
     * Currently set to 0 (single shard), can be used for multi-shard distribution
     */
    @Column(nullable = false)
    private Integer shardId = 0;
    
    /**
     * Pre-persist hook to automatically set createdDate from createdAt
     * Ensures createdDate is always synchronized with createdAt
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt != null && createdDate == null) {
            createdDate = createdAt.toLocalDate();
        }
    }
    
    /**
     * Pre-update hook to ensure createdDate stays synchronized
     */
    @PreUpdate
    protected void onUpdate() {
        if (createdAt != null && (createdDate == null || !createdDate.equals(createdAt.toLocalDate()))) {
            createdDate = createdAt.toLocalDate();
        }
    }
}