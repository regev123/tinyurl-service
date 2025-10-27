package com.tinyurl.cache;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.tinyurl.constants.UrlConstants.*;

@Slf4j
@Component
public class SimpleCache<K, V> {
    
    private final ConcurrentHashMap<K, CacheEntry<V>> cache;
    private final Duration defaultTtl;
    private final ScheduledExecutorService cleanupExecutor;
    
    public SimpleCache() {
        this.cache = new ConcurrentHashMap<>();
        this.defaultTtl = Duration.ofMinutes(CACHE_DEFAULT_TTL_MINUTES);
        this.cleanupExecutor = Executors.newScheduledThreadPool(1);
    }
    
    @PostConstruct
    public void init() {
        // Run cleanup every 30 seconds to remove expired entries
        cleanupExecutor.scheduleWithFixedDelay(
            this::cleanupExpiredEntries, 
            CACHE_CLEANUP_INTERVAL_SECONDS, 
            CACHE_CLEANUP_INTERVAL_SECONDS, 
            TimeUnit.SECONDS
        );
        
        log.info("SimpleCache initialized with default TTL of {} minutes", CACHE_DEFAULT_TTL_MINUTES);
    }
    
    /**
     * Store a value in the cache with default TTL (1 minute)
     */
    public void put(K key, V value) {
        put(key, value, defaultTtl);
    }
    
    /**
     * Store a value in the cache with custom TTL
     */
    public void put(K key, V value, Duration ttl) {
        LocalDateTime expiryTime = LocalDateTime.now().plus(ttl);
        cache.put(key, new CacheEntry<>(value, expiryTime));
        log.debug("Cached key: {} with expiry at: {}", key, expiryTime);
    }
    
    /**
     * Retrieve a value from the cache
     * Returns null if key doesn't exist or entry has expired
     * Uses ConcurrentHashMap for thread-safe access across multiple requests
     */
    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        
        if (entry == null) {
            log.debug("Cache miss for key: {}", key);
            return null;
        }
        
        if (entry.isExpired()) {
            log.debug("Cache entry expired for key: {}", key);
            // ConcurrentHashMap.remove() is thread-safe
            cache.remove(key);
            return null;
        }
        
        log.debug("Cache hit for key: {}", key);
        return entry.getValue();
    }
    
    /**
     * Check if key exists in cache and is not expired
     */
    public boolean containsKey(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired()) {
            cache.remove(key);
            return false;
        }
        return true;
    }
    
    /**
     * Remove a specific key from cache
     */
    public void remove(K key) {
        cache.remove(key);
        log.debug("Removed key from cache: {}", key);
    }
    
    /**
     * Clear all entries from cache
     */
    public void clear() {
        cache.clear();
        log.info("Cache cleared");
    }
    
    /**
     * Get the current number of entries in cache
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * Clean up expired entries
     */
    private void cleanupExpiredEntries() {
        int initialSize = cache.size();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int removed = initialSize - cache.size();
        
        if (removed > 0) {
            log.debug("Cleaned up {} expired cache entries", removed);
        }
    }
    
    /**
     * Shutdown the cleanup executor (called automatically when application stops)
     */
    @PreDestroy
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("SimpleCache shut down");
    }
}
