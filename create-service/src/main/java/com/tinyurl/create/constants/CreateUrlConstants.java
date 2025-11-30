package com.tinyurl.create.constants;

/**
 * Constants for URL creation operations
 * Part of the Create Service microservice
 */
public final class CreateUrlConstants {
    
    private CreateUrlConstants() {
        // Utility class - prevent instantiation
    }
    
    // Base62 encoding constants
    public static final long MAX_SHORT_URL_NUMBER = 56_800_235_583L; // 62^6 - 1
    public static final long MIN_SHORT_URL_NUMBER = 1L;
    
    // URL expiration
    public static final int DEFAULT_EXPIRATION_YEARS = 1;
    
    // Validation
    public static final int MAX_ORIGINAL_URL_LENGTH = 5000;
    public static final int MAX_SHORT_CODE_LENGTH = 10;
    
    // URL Code Generation
    public static final int MAX_CODE_GENERATION_ATTEMPTS = 100;
}

