package com.tinyurl.repository;

import com.tinyurl.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for UrlMapping entity
 * Follows Repository Pattern - abstracts data access layer
 * Follows Single Responsibility Principle - only handles data access operations
 * 
 * Spring Data JPA automatically implements these methods based on naming conventions
 */
@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {
    
    /**
     * Finds a URL mapping by short URL code
     * 
     * @param shortUrl the short URL code
     * @return Optional containing the mapping if found
     */
    Optional<UrlMapping> findByShortUrl(String shortUrl);
    
    /**
     * Checks if a short URL code already exists
     * 
     * @param shortUrl the short URL code to check
     * @return true if exists, false otherwise
     */
    boolean existsByShortUrl(String shortUrl);
    
    /**
     * Finds a URL mapping by original URL
     * Used to check for duplicate URLs before creating new mappings
     * 
     * @param originalUrl the original URL
     * @return Optional containing the mapping if found
     */
    Optional<UrlMapping> findByOriginalUrl(String originalUrl);
}
