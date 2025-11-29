package com.tinyurl.entity;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static com.tinyurl.constants.UrlConstants.DEFAULT_EXPIRATION_YEARS;

/**
 * Factory for creating UrlMapping entities
 * Follows Factory Pattern - encapsulates object creation logic
 * Follows Single Responsibility Principle - only handles entity creation
 * Follows Encapsulation - prevents direct instantiation
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UrlMappingFactory {
    
    private static final long DEFAULT_ACCESS_COUNT = 0L;
    
    /**
     * Creates a new UrlMapping entity with default values
     * Uses factory pattern to ensure consistent entity creation
     * 
     * @param originalUrl the original URL to shorten (must not be null)
     * @param shortCode the generated short code (must not be null)
     * @return new UrlMapping entity with default values set
     * @throws IllegalArgumentException if originalUrl or shortCode is null
     */
    public static UrlMapping create(String originalUrl, String shortCode) {
        if (originalUrl == null || originalUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Original URL cannot be null or empty");
        }
        if (shortCode == null || shortCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Short code cannot be null or empty");
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        UrlMapping mapping = new UrlMapping();
        mapping.setOriginalUrl(originalUrl);
        mapping.setShortUrl(shortCode);
        mapping.setCreatedAt(now);
        mapping.setExpiresAt(now.plusYears(DEFAULT_EXPIRATION_YEARS));
        mapping.setAccessCount(DEFAULT_ACCESS_COUNT);
        mapping.setLastAccessedAt(null); // Will be set on first access
        
        return mapping;
    }
}

