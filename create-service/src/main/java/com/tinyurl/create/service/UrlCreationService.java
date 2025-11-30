package com.tinyurl.create.service;

import com.tinyurl.create.dto.CreateUrlResult;

/**
 * Interface for URL creation operations
 * Part of the Create Service microservice
 * 
 * Follows Interface Segregation Principle - focused on creation operations only
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

