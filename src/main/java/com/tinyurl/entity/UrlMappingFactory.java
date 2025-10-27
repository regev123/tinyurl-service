package com.tinyurl.entity;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static com.tinyurl.constants.UrlConstants.DEFAULT_EXPIRATION_YEARS;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UrlMappingFactory {
    
    /**
     * Creates a new UrlMapping entity with default values
     * 
     * @param originalUrl the original URL to shorten
     * @param shortCode the generated short code
     * @return new UrlMapping entity
     */
    public static UrlMapping create(String originalUrl, String shortCode) {
        UrlMapping mapping = new UrlMapping();
        mapping.setOriginalUrl(originalUrl);
        mapping.setShortUrl(shortCode);
        mapping.setCreatedAt(LocalDateTime.now());
        mapping.setExpiresAt(LocalDateTime.now().plusYears(DEFAULT_EXPIRATION_YEARS));
        mapping.setAccessCount(0L);
        return mapping;
    }
}

