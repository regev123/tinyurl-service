package com.tinyurl.lookup.exception;

import java.io.Serial;

/**
 * Exception thrown when a short URL is not found in the system
 * Part of the Lookup Service microservice
 * 
 * Follows Single Responsibility Principle - represents a specific error condition
 */
public class UrlNotFoundException extends RuntimeException {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    public UrlNotFoundException(String message) {
        super(message);
    }
    
    public UrlNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

