package com.tinyurl.constants;

public final class UrlConstants {
    
    private UrlConstants() {
        // Utility class - prevent instantiation
    }
    
    // Base62 encoding constants
    public static final long MAX_SHORT_URL_NUMBER = 56_800_235_583L; // 62^6 - 1
    public static final long MIN_SHORT_URL_NUMBER = 1L;
    
    // URL expiration
    public static final int DEFAULT_EXPIRATION_YEARS = 1;
    
    // Cache settings
    public static final int CACHE_DEFAULT_TTL_MINUTES = 1;
    public static final int CACHE_CLEANUP_INTERVAL_SECONDS = 30;
    
    // Validation
    public static final int MAX_ORIGINAL_URL_LENGTH = 5000;
    public static final int MAX_SHORT_CODE_LENGTH = 10;
}

