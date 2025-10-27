package com.tinyurl.service;

import com.tinyurl.cache.SimpleCache;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShorteningService implements UrlCreationService, UrlLookupService {
    
    private final UrlMappingRepository urlMappingRepository;
    private final SimpleCache<String, String> urlCache;
    private final UrlCodeGenerator urlCodeGenerator;
    
    /**
     * {@inheritDoc}
     * Creates a short URL for the given original URL
     * Checks for existing mapping first, then generates new one if needed
     */
    @Override
    @Transactional
    public CreateUrlResult createShortUrl(String originalUrl, String baseUrl) {
        String shortCode = getOrCreateShortCode(originalUrl);
        String shortUrlDisplay = UrlBuilder.buildShortUrl(baseUrl, shortCode);
        
        return new CreateUrlResult(originalUrl, shortUrlDisplay, shortCode, true, null);
    }
    
    /**
     * Gets existing short code or creates a new one
     * 
     * @param originalUrl the original URL
     * @return the short code for the URL
     */
    @Transactional
    private String getOrCreateShortCode(String originalUrl) {
        // Check if URL already exists - optimized with index lookup
        Optional<UrlMapping> existingMapping = urlMappingRepository.findByOriginalUrl(originalUrl);
        
        if (existingMapping.isPresent()) {
            String shortCode = existingMapping.get().getShortUrl();
            log.info("Found existing mapping for URL: {}", originalUrl);
            // Cache the existing mapping
            urlCache.put(shortCode, originalUrl);
            return shortCode;
        }
        
        // Generate new short code
        String shortCode = urlCodeGenerator.generateUniqueCode();
        
        // Create and save new mapping using factory
        UrlMapping urlMapping = UrlMappingFactory.create(originalUrl, shortCode);
        urlMappingRepository.save(urlMapping);
        
        log.info("Created new short URL: {} for {}", shortCode, originalUrl);
        
        // Cache the new mapping
        urlCache.put(shortCode, originalUrl);
        
        return shortCode;
    }
    
    /**
     * {@inheritDoc}
     * Gets the original URL for a given short code
     */
    @Override
    @Transactional
    public String getOriginalUrl(String shortCode) {
        // Check cache first
        String cachedUrl = urlCache.get(shortCode);
        if (cachedUrl != null) {
            log.debug("Cache hit for short code: {}", shortCode);
            return cachedUrl;
        }
        
        log.debug("Cache miss, looking up in database for short code: {}", shortCode);
        
        UrlMapping mapping = urlMappingRepository.findByShortUrl(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));
        
        if (mapping.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Short URL has expired: {}", shortCode);
            throw new UrlExpiredException("Short URL has expired: " + shortCode);
        }
        
        // Cache the result
        urlCache.put(shortCode, mapping.getOriginalUrl());
        
        // Increment access count
        mapping.setAccessCount(mapping.getAccessCount() + 1);
        urlMappingRepository.save(mapping);
        
        return mapping.getOriginalUrl();
    }
    
    /**
     * {@inheritDoc}
     * Looks up a short URL and returns lookup result
     */
    @Override
    public UrlLookupResult lookupUrl(String shortCode) {
        try {
            String originalUrl = getOriginalUrl(shortCode);
            log.debug("Found original URL for {}: {}", shortCode, originalUrl);
            return new UrlLookupResult(shortCode, originalUrl, true, null);
        } catch (UrlNotFoundException e) {
            log.debug("Short URL not found: {}", shortCode);
            return new UrlLookupResult(shortCode, null, false, "Short URL not found");
        } catch (UrlExpiredException e) {
            log.debug("Short URL expired: {}", shortCode);
            return new UrlLookupResult(shortCode, null, false, "Short URL has expired");
        } catch (Exception e) {
            log.error("Error looking up short URL: {}", shortCode, e);
            return new UrlLookupResult(shortCode, null, false, "Internal server error");
        }
    }
}
