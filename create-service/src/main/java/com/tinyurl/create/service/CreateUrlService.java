package com.tinyurl.create.service;

import com.tinyurl.create.dto.CreateUrlResult;
import com.tinyurl.entity.UrlMapping;
import com.tinyurl.create.entity.UrlMappingFactory;
import com.tinyurl.create.repository.CreateUrlRepository;
import com.tinyurl.create.util.UrlBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service dedicated to URL creation operations
 * Part of the Create Service microservice architecture
 * 
 * Follows Single Responsibility Principle - only handles URL creation business logic
 * Follows Dependency Inversion Principle - depends on repository and service abstractions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateUrlService implements UrlCreationService {
    
    private final CreateUrlRepository urlMappingRepository;
    private final UrlCodeGenerator urlCodeGenerator;
    private final UrlValidationService urlValidationService;
    
    /**
     * {@inheritDoc}
     * Creates a short URL for the given original URL
     * Checks for existing mapping first, then generates new one if needed
     * 
     * This is the main entry point for the Create Service
     */
    @Override
    @Transactional
    public CreateUrlResult createShortUrl(String originalUrl, String baseUrl) {
        // Validate input
        urlValidationService.validateOriginalUrl(originalUrl);
        urlValidationService.validateBaseUrl(baseUrl);
        
        // Get or create short code
        String shortCode = getOrCreateShortCode(originalUrl);
        
        // Build the full short URL
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
     * This method implements the deduplication logic - if the same URL
     * is shortened multiple times, it returns the existing short code
     * 
     * @param originalUrl the original URL
     * @return the short code for the URL (existing or newly created)
     */
    private String getOrCreateShortCode(String originalUrl) {
        // First, try to find existing mapping using read replica (read-only)
        Optional<UrlMapping> existingMapping = findExistingMapping(originalUrl);
        
        if (existingMapping.isPresent()) {
            String shortCode = existingMapping.get().getShortUrl();
            return shortCode;
        }
        
        // Not found in read replica, create new one (write to primary)
        return createNewMapping(originalUrl);
    }
    
    /**
     * Finds existing mapping using read replica (read-only transaction)
     * Uses NOT_SUPPORTED propagation to suspend parent transaction and use read replica
     * 
     * This optimization allows us to check for duplicates on read replicas
     * before committing to a write operation on the primary database
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
     * This method handles the actual creation of a new URL mapping:
     * 1. Generates a unique short code
     * 2. Creates the UrlMapping entity using the factory
     * 3. Saves it to the database (primary)
     * 
     * @param originalUrl the original URL
     * @return the generated short code
     */
    @Transactional
    private String createNewMapping(String originalUrl) {
        // Generate new unique short code
        String shortCode = urlCodeGenerator.generateUniqueCode();
        
        // Create and save new mapping using factory pattern
        UrlMapping urlMapping = UrlMappingFactory.create(originalUrl, shortCode);
        urlMappingRepository.save(urlMapping);
        
        return shortCode;
    }
}

