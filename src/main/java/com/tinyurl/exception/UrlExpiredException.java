package com.tinyurl.exception;

import java.io.Serial;

/**
 * Exception thrown when a short URL has expired
 * Follows Single Responsibility Principle - represents a specific error condition
 */
public class UrlExpiredException extends RuntimeException {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    public UrlExpiredException(String message) {
        super(message);
    }
    
    public UrlExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}

