package com.tinyurl.service;

import com.tinyurl.constants.ErrorCode;
import com.tinyurl.dto.CreateUrlResult;
import com.tinyurl.dto.UrlLookupResult;
import com.tinyurl.entity.UrlMapping;
import com.tinyurl.entity.UrlMappingFactory;
import com.tinyurl.exception.UrlExpiredException;
import com.tinyurl.exception.UrlNotFoundException;
import com.tinyurl.repository.UrlMappingRepository;
import com.tinyurl.util.UrlBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.tinyurl.constants.UrlConstants.CACHE_KEY_PREFIX;

/**
 * Main service for URL shortening operations
 * Follows Single Responsibility Principle - handles URL creation and lookup business logic
 * Follows Dependency Inversion Principle - depends on CacheService abstraction
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShorteningService implements UrlCreationService, UrlLookupService {
    
    private final UrlMappingRepository urlMappingRepository;
    private final CacheService cacheService;
    private final UrlCodeGenerator urlCodeGenerator;
    private final UrlValidationService urlValidationService;
    
    /**
     * {@inheritDoc}
     * Creates a short URL for the given original URL
     * Checks for existing mapping first, then generates new one if needed
     */
    @Override
    @Transactional
    public CreateUrlResult createShortUrl(String originalUrl, String baseUrl) {
        // Validate input
        urlValidationService.validateOriginalUrl(originalUrl);
        urlValidationService.validateBaseUrl(baseUrl);
        
        String shortCode = getOrCreateShortCode(originalUrl);
        String shortUrlDisplay = UrlBuilder.buildShortUrl(baseUrl, shortCode);
        
        return CreateUrlResult.builder()
            .originalUrl(originalUrl)
            .shortUrl(shortUrlDisplay)
            .shortCode(shortCode)
            .success(true)
            .errorCode(null)
            .build();
    }
    
    /**
     * Gets existing short code or creates a new one
     * Optimized: First checks read replica, then writes to primary if needed
     * 
     * @param originalUrl the original URL
     * @return the short code for the URL
     */
    private String getOrCreateShortCode(String originalUrl) {
        // First, try to find existing mapping using read replica (read-only)
        Optional<UrlMapping> existingMapping = findExistingMapping(originalUrl);
        
        if (existingMapping.isPresent()) {
            String shortCode = existingMapping.get().getShortUrl();
            log.info("Found existing mapping for URL: {}", originalUrl);
            return shortCode;
        }
        
        // Not found in read replica, create new one (write to primary)
        return createNewMapping(originalUrl);
    }
    
    /**
     * Finds existing mapping using read replica (read-only transaction)
     * Uses NOT_SUPPORTED propagation to suspend parent transaction and use read replica
     * 
     * @param originalUrl the original URL to search for
     * @return Optional containing the mapping if found
     */
    @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
    private Optional<UrlMapping> findExistingMapping(String originalUrl) {
        return urlMappingRepository.findByOriginalUrl(originalUrl);
    }
    
    /**
     * Creates a new URL mapping (write transaction to primary)
     * 
     * @param originalUrl the original URL
     * @return the generated short code
     */
    @Transactional
    private String createNewMapping(String originalUrl) {
        // Generate new short code
        String shortCode = urlCodeGenerator.generateUniqueCode();
        
        // Create and save new mapping using factory
        UrlMapping urlMapping = UrlMappingFactory.create(originalUrl, shortCode);
        urlMappingRepository.save(urlMapping);
        
        log.info("Created new short URL: {} for {}", shortCode, originalUrl);
        
        return shortCode;
    }
    
    /**
     * {@inheritDoc}
     * Gets the original URL for a given short code
     * Uses read-only transaction to route to read replica
     */
    @Override
    @Transactional(readOnly = true)
    public String getOriginalUrl(String shortCode) {
        // Basic null/empty validation only - don't validate format for lookup
        // Format validation is only needed when creating short codes
        if (shortCode == null || shortCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Short code cannot be null or empty");
        }
        
        // Check cache first
        String cachedUrl = getCachedUrl(shortCode);
        if (cachedUrl != null) {
            log.debug("Cache hit for short code: {}", shortCode);
            return cachedUrl;
        }
        
        // Cache miss - fetch from database
        return fetchFromDatabaseAndCache(shortCode);
    }
    
    /**
     * Fetches URL from database, validates expiration, caches result, and increments access count
     * Uses read-only transaction for the initial lookup (goes to replica)
     * Then uses write transaction for access count update (goes to primary)
     * 
     * @param shortCode the short code to look up
     * @return the original URL
     * @throws UrlNotFoundException if URL not found
     * @throws UrlExpiredException if URL has expired
     */
    private String fetchFromDatabaseAndCache(String shortCode) {
        log.debug("Cache miss, looking up in database for short code: {}", shortCode);
        
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
     * 
     * @param mapping the URL mapping to validate
     * @param shortCode the short code for logging
     * @throws UrlExpiredException if URL has expired
     */
    private void validateUrlNotExpired(UrlMapping mapping, String shortCode) {
        if (mapping.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Short URL has expired: {}", shortCode);
            throw new UrlExpiredException("Short URL has expired: " + shortCode);
        }
    }
    
    /**
     * Increments the access count for a URL mapping
     * Uses a new write transaction (REQUIRES_NEW) to ensure it routes to primary database
     * even when called from within a read-only transaction
     * 
     * @param mapping the URL mapping to update
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementAccessCount(UrlMapping mapping) {
        mapping.setAccessCount(mapping.getAccessCount() + 1);
        urlMappingRepository.save(mapping);
    }
    
    /**
     * {@inheritDoc}
     * Looks up a short URL and returns lookup result
     * Uses strategy pattern for error handling - each exception type is handled separately
     */
    @Override
    public UrlLookupResult lookupUrl(String shortCode) {
        try {
            String originalUrl = getOriginalUrl(shortCode);
            log.debug("Found original URL for {}: {}", shortCode, originalUrl);
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
    
    /**
     * Builds a successful lookup result
     * 
     * @param shortCode the short code
     * @param originalUrl the original URL
     * @return successful lookup result
     */
    private UrlLookupResult buildSuccessResult(String shortCode, String originalUrl) {
        return UrlLookupResult.builder()
                .shortUrl(shortCode)
                .originalUrl(originalUrl)
                .found(true)
                .build();
    }
    
    /**
     * Handles URL not found exception
     * 
     * @param shortCode the short code that was not found
     * @return lookup result indicating not found
     */
    private UrlLookupResult handleUrlNotFound(String shortCode) {
        log.debug("Short URL not found: {}", shortCode);
        return UrlLookupResult.builder()
                .shortUrl(shortCode)
                .found(false)
                .message(ErrorCode.URL_NOT_FOUND.getMessage())
                .errorCode(ErrorCode.URL_NOT_FOUND)
                .build();
    }
    
    /**
     * Handles URL expired exception
     * 
     * @param shortCode the expired short code
     * @return lookup result indicating expired
     */
    private UrlLookupResult handleUrlExpired(String shortCode) {
        log.debug("Short URL expired: {}", shortCode);
        return UrlLookupResult.builder()
                .shortUrl(shortCode)
                .found(false)
                .message(ErrorCode.URL_EXPIRED.getMessage())
                .errorCode(ErrorCode.URL_EXPIRED)
                .build();
    }
    
    /**
     * Handles invalid input exception
     * 
     * @param shortCode the invalid short code
     * @param e the exception
     * @return lookup result indicating invalid input
     */
    private UrlLookupResult handleInvalidInput(String shortCode, IllegalArgumentException e) {
        log.warn("Invalid input for short code: {}", shortCode, e);
        return UrlLookupResult.builder()
                .shortUrl(shortCode)
                .found(false)
                .message(ErrorCode.INVALID_INPUT.getMessage())
                .errorCode(ErrorCode.INVALID_INPUT)
                .build();
    }
    
    /**
     * Handles database access exception
     * 
     * @param shortCode the short code being looked up
     * @param e the database exception
     * @return lookup result indicating server error
     */
    private UrlLookupResult handleDatabaseError(String shortCode, DataAccessException e) {
        log.error("Database error looking up short URL: {}", shortCode, e);
        return UrlLookupResult.builder()
                .shortUrl(shortCode)
                .found(false)
                .message(ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                .errorCode(ErrorCode.INTERNAL_SERVER_ERROR)
                .build();
    }
    
    /**
     * Cache a URL mapping with default TTL
     * 
     * @param shortCode the short code
     * @param originalUrl the original URL to cache
     */
    private void cacheUrl(String shortCode, String originalUrl) {
        String cacheKey = buildCacheKey(shortCode);
        cacheService.put(cacheKey, originalUrl);
    }
    
    /**
     * Retrieve a URL from cache
     * 
     * @param shortCode the short code
     * @return the cached URL or null if not found
     */
    private String getCachedUrl(String shortCode) {
        String cacheKey = buildCacheKey(shortCode);
        return cacheService.get(cacheKey);
    }
    
    /**
     * Builds a cache key from short code
     * Extracted to single method to avoid duplication (DRY principle)
     * 
     * @param shortCode the short code
     * @return the cache key
     */
    private String buildCacheKey(String shortCode) {
        return CACHE_KEY_PREFIX + shortCode;
    }
}
