package com.tinyurl.create.exception;

import java.io.Serial;

/**
 * Exception thrown when unable to generate a unique short URL code
 * Part of the Create Service microservice
 * 
 * Follows Single Responsibility Principle - represents a specific error condition
 */
public class UrlGenerationException extends RuntimeException {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    public UrlGenerationException(String message) {
        super(message);
    }
    
    public UrlGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}

