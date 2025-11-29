package com.tinyurl.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static com.tinyurl.constants.UrlConstants.CACHE_DEFAULT_TTL_MINUTES;

/**
 * Redis implementation of CacheService
 * Follows Single Responsibility Principle - only handles Redis caching operations
 * Follows Dependency Inversion Principle - implements CacheService interface
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService implements CacheService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
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
        log.debug("Cached key: {} with expiry in: {} minutes", key, ttlMinutes);
    }
    
    @Override
    public String get(String key) {
        if (!validateKey(key)) {
            return null;
        }
        
        String value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            log.debug("Cache hit for key: {}", key);
        } else {
            log.debug("Cache miss for key: {}", key);
        }
        return value;
    }
    
    @Override
    public void remove(String key) {
        if (!validateKey(key)) {
            return;
        }
        redisTemplate.delete(key);
        log.debug("Removed key from cache: {}", key);
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

