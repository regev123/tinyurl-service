package com.tinyurl.lookup.service;

import com.tinyurl.constants.ErrorCode;
import com.tinyurl.lookup.dto.UrlLookupResult;
import com.tinyurl.entity.UrlMapping;
import com.tinyurl.lookup.exception.UrlExpiredException;
import com.tinyurl.lookup.exception.UrlNotFoundException;
import com.tinyurl.lookup.repository.LookupUrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.tinyurl.lookup.constants.LookupUrlConstants.CACHE_KEY_PREFIX;

/**
 * Service dedicated to URL lookup operations
 * Part of the Lookup Service microservice architecture
 * 
 * Follows Single Responsibility Principle - only handles URL lookup business logic
 * Follows Dependency Inversion Principle - depends on repository and service abstractions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LookupUrlService implements UrlLookupService {
    
    private final LookupUrlRepository urlMappingRepository;
    private final CacheService cacheService;
    
    /**
     * {@inheritDoc}
     * Gets the original URL for a given short code
     * Uses read-only transaction to route to read replica
     */
    @Override
    @Transactional(readOnly = true)
    public String getOriginalUrl(String shortCode) {
        // Basic null/empty validation only
        if (shortCode == null || shortCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Short code cannot be null or empty");
        }
        
        // Check cache first
        String cachedUrl = getCachedUrl(shortCode);
        if (cachedUrl != null) {
            return cachedUrl;
        }
        
        // Cache miss - fetch from database
        return fetchFromDatabaseAndCache(shortCode);
    }
    
    /**
     * Fetches URL from database, validates expiration, caches result, and increments access count
     * Uses read-only transaction for the initial lookup (goes to replica)
     * Then uses write transaction for access count update (goes to primary)
     */
    private String fetchFromDatabaseAndCache(String shortCode) {
        // Read from replica (read-only transaction)
        UrlMapping mapping = urlMappingRepository.findByShortUrl(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));
        
        validateUrlNotExpired(mapping, shortCode);
        
        String originalUrl = mapping.getOriginalUrl();
        
        // Cache the result
        cacheUrl(shortCode, originalUrl);
        
        // Increment access count (write operation - goes to primary)
        incrementAccessCount(mapping);
        
        return originalUrl;
    }
    
    /**
     * Validates that the URL mapping has not expired
     */
    private void validateUrlNotExpired(UrlMapping mapping, String shortCode) {
        if (mapping.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Lookup Service: Short URL has expired: {}", shortCode);
            throw new UrlExpiredException("Short URL has expired: " + shortCode);
        }
    }
    
    /**
     * Increments the access count for a URL mapping and updates last accessed timestamp
     * Uses a new write transaction (REQUIRES_NEW) to ensure it routes to primary database
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementAccessCount(UrlMapping mapping) {
        mapping.setAccessCount(mapping.getAccessCount() + 1);
        mapping.setLastAccessedAt(LocalDateTime.now());
        urlMappingRepository.save(mapping);
    }
    
    /**
     * {@inheritDoc}
     * Looks up a short URL and returns lookup result
     */
    @Override
    public UrlLookupResult lookupUrl(String shortCode) {
        try {
            String originalUrl = getOriginalUrl(shortCode);
            return buildSuccessResult(shortCode, originalUrl);
        } catch (UrlNotFoundException e) {
            return handleUrlNotFound(shortCode);
        } catch (UrlExpiredException e) {
            return handleUrlExpired(shortCode);
        } catch (IllegalArgumentException e) {
            return handleInvalidInput(shortCode, e);
        } catch (DataAccessException e) {
            return handleDatabaseError(shortCode, e);
        }
    }
    
    private UrlLookupResult buildSuccessResult(String shortCode, String originalUrl) {
        return UrlLookupResult.builder()
                .shortUrl(shortCode)
                .originalUrl(originalUrl)
                .found(true)
                .build();
    }
    
    private UrlLookupResult handleUrlNotFound(String shortCode) {
        return UrlLookupResult.builder()
                .shortUrl(shortCode)
                .found(false)
                .message(ErrorCode.URL_NOT_FOUND.getMessage())
                .errorCode(ErrorCode.URL_NOT_FOUND)
                .build();
    }
    
    private UrlLookupResult handleUrlExpired(String shortCode) {
        return UrlLookupResult.builder()
                .shortUrl(shortCode)
                .found(false)
                .message(ErrorCode.URL_EXPIRED.getMessage())
                .errorCode(ErrorCode.URL_EXPIRED)
                .build();
    }
    
    private UrlLookupResult handleInvalidInput(String shortCode, IllegalArgumentException e) {
        log.warn("Lookup Service: Invalid input for short code: {}", shortCode, e);
        return UrlLookupResult.builder()
                .shortUrl(shortCode)
                .found(false)
                .message(ErrorCode.INVALID_INPUT.getMessage())
                .errorCode(ErrorCode.INVALID_INPUT)
                .build();
    }
    
    private UrlLookupResult handleDatabaseError(String shortCode, DataAccessException e) {
        log.error("Lookup Service: Database error looking up short URL: {}", shortCode, e);
        return UrlLookupResult.builder()
                .shortUrl(shortCode)
                .found(false)
                .message(ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                .errorCode(ErrorCode.INTERNAL_SERVER_ERROR)
                .build();
    }
    
    private void cacheUrl(String shortCode, String originalUrl) {
        String cacheKey = buildCacheKey(shortCode);
        cacheService.put(cacheKey, originalUrl);
    }
    
    private String getCachedUrl(String shortCode) {
        String cacheKey = buildCacheKey(shortCode);
        return cacheService.get(cacheKey);
    }
    
    private String buildCacheKey(String shortCode) {
        return CACHE_KEY_PREFIX + shortCode;
    }
}

