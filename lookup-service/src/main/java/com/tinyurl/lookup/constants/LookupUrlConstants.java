package com.tinyurl.lookup.constants;

/**
 * Constants for URL lookup operations
 * Part of the Lookup Service microservice
 */
public final class LookupUrlConstants {
    
    private LookupUrlConstants() {
        // Utility class - prevent instantiation
    }
    
    // Cache settings
    public static final int CACHE_DEFAULT_TTL_MINUTES = 10;  // 10 minutes TTL - sliding expiration
    public static final int CACHE_HOT_TTL_MINUTES = 30;      // 30 minutes for frequently accessed URLs
    public static final int CACHE_WARM_TTL_MINUTES = 15;     // 15 minutes for moderately accessed URLs
    public static final int CACHE_ACCESS_THRESHOLD_HOT = 10;  // Access count threshold for "hot" URLs
    public static final int CACHE_ACCESS_THRESHOLD_WARM = 5;  // Access count threshold for "warm" URLs
    public static final int CACHE_CLEANUP_INTERVAL_SECONDS = 30;
    public static final String CACHE_KEY_PREFIX = "url:";
    public static final String CACHE_ACCESS_COUNT_PREFIX = "url:access:";
}

