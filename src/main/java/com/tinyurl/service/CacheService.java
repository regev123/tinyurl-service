package com.tinyurl.service;

/**
 * Interface for cache operations
 * Follows Dependency Inversion Principle - high-level modules depend on this abstraction
 * Allows swapping cache implementations (Redis, Memcached, etc.) without changing business logic
 */
public interface CacheService {
    
    /**
     * Stores a value in the cache with default TTL
     * 
     * @param key the cache key
     * @param value the value to cache
     */
    void put(String key, String value);
    
    /**
     * Stores a value in the cache with custom TTL
     * 
     * @param key the cache key
     * @param value the value to cache
     * @param ttlMinutes time to live in minutes
     */
    void put(String key, String value, int ttlMinutes);
    
    /**
     * Retrieves a value from the cache
     * 
     * @param key the cache key
     * @return the cached value, or null if not found
     */
    String get(String key);
    
    /**
     * Removes a value from the cache
     * 
     * @param key the cache key
     */
    void remove(String key);
    
    /**
     * Checks if a key exists in the cache
     * 
     * @param key the cache key
     * @return true if key exists, false otherwise
     */
    boolean exists(String key);
}

