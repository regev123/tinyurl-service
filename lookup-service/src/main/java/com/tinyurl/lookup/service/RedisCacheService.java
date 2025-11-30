package com.tinyurl.lookup.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static com.tinyurl.lookup.constants.LookupUrlConstants.*;

/**
 * Optimized Redis implementation of CacheService with adaptive TTL strategy
 * 
 * Features:
 * - Sliding expiration: TTL refreshes on access
 * - Adaptive TTL: Frequently accessed URLs get longer cache time
 *   - Hot URLs (10+ accesses): 30 minutes
 *   - Warm URLs (5-9 accesses): 15 minutes  
 *   - Cold URLs (<5 accesses): 10 minutes
 * - Access frequency tracking for intelligent caching
 * 
 * Follows Single Responsibility Principle - only handles Redis caching operations
 * Follows Dependency Inversion Principle - implements CacheService interface
 */
@Slf4j
@Service
public class RedisCacheService implements CacheService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public RedisCacheService(@Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public void put(String key, String value) {
        put(key, value, CACHE_DEFAULT_TTL_MINUTES);
    }
    
    @Override
    public void put(String key, String value, int ttlMinutes) {
        if (!validateKey(key)) {
            return;
        }
        if (value == null) {
            log.warn("Attempted to cache null value for key: {}", key);
            return;
        }
        if (ttlMinutes < 0) {
            log.warn("Invalid TTL: {} minutes, using default", ttlMinutes);
            ttlMinutes = CACHE_DEFAULT_TTL_MINUTES;
        }
        
        Duration ttl = Duration.ofMinutes(ttlMinutes);
        redisTemplate.opsForValue().set(key, value, ttl);
    }
    
    @Override
    public String get(String key) {
        if (!validateKey(key)) {
            return null;
        }
        
        // Get cached value
        String value = redisTemplate.opsForValue().get(key);
        
        if (value != null) {
            // Track access frequency and apply adaptive TTL
            String accessKey = CACHE_ACCESS_COUNT_PREFIX + key;
            Long accessCount = redisTemplate.opsForValue().increment(accessKey);
            
            // Set expiration on access counter if it's new
            if (accessCount == 1) {
                Duration ttl = Duration.ofMinutes(CACHE_DEFAULT_TTL_MINUTES);
                redisTemplate.expire(accessKey, ttl);
            }
            
            // Determine adaptive TTL based on access frequency
            int ttlMinutes = determineTtl(accessCount.intValue());
            
            // Refresh TTL with adaptive value (sliding expiration)
            Duration ttl = Duration.ofMinutes(ttlMinutes);
            redisTemplate.expire(key, ttl);
        }
        
        return value;
    }
    
    /**
     * Determines TTL based on access frequency
     * Hot URLs (frequently accessed) get longer TTL to stay in cache longer
     * 
     * @param accessCount number of times URL was accessed
     * @return TTL in minutes
     */
    private int determineTtl(int accessCount) {
        if (accessCount >= CACHE_ACCESS_THRESHOLD_HOT) {
            return CACHE_HOT_TTL_MINUTES;  // 30 minutes for hot URLs
        } else if (accessCount >= CACHE_ACCESS_THRESHOLD_WARM) {
            return CACHE_WARM_TTL_MINUTES;  // 15 minutes for warm URLs
        } else {
            return CACHE_DEFAULT_TTL_MINUTES;  // 10 minutes for cold URLs
        }
    }
    
    @Override
    public void remove(String key) {
        if (!validateKey(key)) {
            return;
        }
        redisTemplate.delete(key);
    }
    
    @Override
    public boolean exists(String key) {
        if (!validateKey(key)) {
            return false;
        }
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
    
    /**
     * Validates cache key is not null or empty
     * Extracted to avoid duplication (DRY principle)
     * 
     * @param key the key to validate
     * @return true if key is valid, false otherwise
     */
    private boolean validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            log.warn("Cache operation attempted with null or empty key");
            return false;
        }
        return true;
    }
}

