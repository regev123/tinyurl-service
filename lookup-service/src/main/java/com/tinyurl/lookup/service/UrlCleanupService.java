package com.tinyurl.lookup.service;

import com.tinyurl.lookup.repository.LookupUrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for cleaning up unused URLs
 * Part of the Lookup Service microservice architecture
 * 
 * Follows Single Responsibility Principle - only handles URL cleanup operations
 * Follows Open/Closed Principle - configurable retention period via properties
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UrlCleanupService {
    
    private final LookupUrlRepository urlMappingRepository;
    
    @Value("${url.cleanup.retention-months:6}")
    private int retentionMonths;
    
    @Value("${url.cleanup.enabled:true}")
    private boolean cleanupEnabled;
    
    @Value("${url.cleanup.batch-size:1000}")
    private int batchSize;
    
    /**
     * Scheduled cleanup job that runs daily at 2 AM
     * Deletes URLs that:
     * 1. Haven't been accessed in the configured retention period (6 months by default)
     * 2. Have expired (expiresAt < now)
     * 
     * Cron expression: "0 0 2 * * ?" = Every day at 2:00 AM
     * 
     * Note: Each batch deletion runs in its own transaction to avoid long-running transactions
     * and prevent connection pool exhaustion. The sleep between batches happens outside transactions.
     */
    @Scheduled(cron = "${url.cleanup.cron:0 0 2 * * ?}")
    public void cleanupUnusedUrls() {
        if (!cleanupEnabled) {
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime accessCutoffDate = now.minusMonths(retentionMonths);
        long deletedCount = 0;
        
        try {
            // Delete in batches, each batch in its own transaction
            // This prevents long-running transactions and connection pool exhaustion
            boolean hasMore = true;
            while (hasMore) {
                int deleted = deleteBatch(accessCutoffDate, now);
                deletedCount += deleted;
                
                if (deleted < batchSize) {
                    hasMore = false;
                } else {
                    // Small delay between batches to avoid overwhelming the database
                    // Sleep happens OUTSIDE transaction to avoid holding connections
                    Thread.sleep(100);
                }
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("URL cleanup job interrupted", e);
        } catch (Exception e) {
            log.error("Error during URL cleanup job", e);
            // Don't re-throw - allow job to continue next time
            // Logging the error is sufficient for scheduled jobs
        }
    }
    
    /**
     * Deletes a single batch of URLs in its own transaction
     * Uses REQUIRES_NEW propagation to ensure each batch is independent
     * 
     * @param accessCutoffDate cutoff date for access-based deletion
     * @param currentTime current time for expiration check
     * @return number of URLs deleted in this batch
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private int deleteBatch(LocalDateTime accessCutoffDate, LocalDateTime currentTime) {
        return urlMappingRepository.deleteUnusedOrExpiredUrls(accessCutoffDate, currentTime, batchSize);
    }
    
    /**
     * Manual cleanup method for testing or ad-hoc execution
     * 
     * @return number of URLs deleted
     */
    @Transactional
    public long cleanupUnusedUrlsManually() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime accessCutoffDate = now.minusMonths(retentionMonths);
        return urlMappingRepository.deleteUnusedOrExpiredUrls(accessCutoffDate, now, Integer.MAX_VALUE);
    }
}

