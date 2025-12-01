package com.tinyurl.stats.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "url_statistics", indexes = {
    @Index(name = "idx_short_code_stats", columnList = "shortCode", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlStatistics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 10)
    private String shortCode;
    
    @Column(nullable = false)
    private Long totalClicks;
    
    @Column(nullable = false)
    private Long clicksToday;
    
    @Column(nullable = false)
    private Long clicksThisWeek;
    
    @Column(nullable = false)
    private Long clicksThisMonth;
    
    @Column(nullable = false)
    private LocalDateTime firstClickAt;
    
    @Column(nullable = false)
    private LocalDateTime lastClickAt;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (firstClickAt == null) {
            firstClickAt = LocalDateTime.now();
        }
        if (lastClickAt == null) {
            lastClickAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

