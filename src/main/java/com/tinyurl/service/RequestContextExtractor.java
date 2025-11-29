package com.tinyurl.service;

import com.tinyurl.util.UrlBuilder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.springframework.stereotype.Component;

/**
 * Component for extracting context information from HTTP requests
 * Follows Single Responsibility Principle - only handles request context extraction
 * Follows Dependency Inversion Principle - depends on UrlBuilder utility abstraction
 */
@Component
public class RequestContextExtractor {
    
    /**
     * Extracts the base URL from the HTTP request
     * 
     * @param httpRequest the HTTP servlet request (must not be null)
     * @return the base URL (scheme://host:port)
     * @throws IllegalArgumentException if httpRequest is null
     */
    public String extractBaseUrl(@NonNull HttpServletRequest httpRequest) {
        if (httpRequest == null) {
            throw new IllegalArgumentException("HTTP request cannot be null");
        }
        
        return UrlBuilder.extractBaseUrl(
            httpRequest.getScheme(),
            httpRequest.getServerName(),
            httpRequest.getServerPort()
        );
    }
}

