package com.tinyurl.service;

import com.tinyurl.dto.CreateUrlResult;

/**
 * Interface for URL creation operations
 * Follows Interface Segregation Principle
 */
public interface UrlCreationService {
    
    /**
     * Creates a short URL for the given original URL
     * 
     * @param originalUrl the original URL to shorten
     * @param baseUrl the base URL for the short link
     * @return short URL result containing the full short URL
     */
    CreateUrlResult createShortUrl(String originalUrl, String baseUrl);
}

