package com.tinyurl.repository;

import com.tinyurl.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
    
    /**
     * Deletes URLs that haven't been accessed since the cutoff date OR have expired
     * Uses native query with CTE (Common Table Expression) for efficient batch deletion
     * 
     * Deletes URLs that match ANY of these conditions:
     * 1. lastAccessedAt < accessCutoffDate (or NULL and created_at < accessCutoffDate)
     * 2. expiresAt < currentTime (expired URLs)
     * 
     * @param accessCutoffDate URLs with lastAccessedAt before this date will be deleted
     * @param currentTime Current time to check expiration
     * @param batchSize maximum number of URLs to delete in this batch
     * @return number of URLs deleted
     */
    @Modifying
    @Query(value = "WITH ids_to_delete AS (" +
                   "  SELECT id FROM url_mappings " +
                   "  WHERE (" +
                   "    (last_accessed_at < :accessCutoffDate OR (last_accessed_at IS NULL AND created_at < :accessCutoffDate)) " +
                   "    OR expires_at < :currentTime" +
                   "  ) " +
                   "  LIMIT :batchSize" +
                   ") " +
                   "DELETE FROM url_mappings WHERE id IN (SELECT id FROM ids_to_delete)", 
           nativeQuery = true)
    int deleteUnusedOrExpiredUrls(
            @Param("accessCutoffDate") LocalDateTime accessCutoffDate,
            @Param("currentTime") LocalDateTime currentTime,
            @Param("batchSize") int batchSize);
}
