package com.tinyurl.create.repository;

import com.tinyurl.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for URL creation operations
 * Part of the Create Service microservice
 * 
 * Follows Interface Segregation Principle - only exposes methods needed for creation
 * Follows Repository Pattern - abstracts data access layer
 * 
 * Spring Data JPA automatically implements these methods based on naming conventions
 */
@Repository
public interface CreateUrlRepository extends JpaRepository<UrlMapping, Long> {
    
    /**
     * Checks if a short URL code already exists
     * Used for collision detection during code generation
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

