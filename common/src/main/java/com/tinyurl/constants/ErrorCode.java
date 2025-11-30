package com.tinyurl.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Error codes for the application
 * Replaces string-based error messages for better type safety and maintainability
 * Follows Open/Closed Principle - can be extended without modifying existing code
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
    
    /**
     * URL not found error
     */
    URL_NOT_FOUND("URL_NOT_FOUND", "Short URL not found"),
    
    /**
     * URL expired error
     */
    URL_EXPIRED("URL_EXPIRED", "Short URL has expired"),
    
    /**
     * Internal server error
     */
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "Internal server error"),
    
    /**
     * URL generation failed error
     */
    URL_GENERATION_FAILED("URL_GENERATION_FAILED", "Unable to generate unique short URL code"),
    
    /**
     * Invalid input error
     */
    INVALID_INPUT("INVALID_INPUT", "Invalid input provided");
    
    private final String code;
    private final String message;
}

