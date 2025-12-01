package com.tinyurl.stats.service;

import com.tinyurl.stats.dto.ClickEventRequest;
import com.tinyurl.stats.entity.UrlClickEvent;
import com.tinyurl.stats.repository.UrlClickEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Batch processor for click events to optimize database writes.
 * Collects events in memory and flushes them in batches to reduce database load.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BatchEventProcessor {
    
    private final UrlClickEventRepository clickEventRepository;
    
    @Value("${stats.batch.size:100}")
    private int batchSize;
    
    @Value("${stats.batch.flush-interval-seconds:5}")
    private int flushIntervalSeconds;
    
    private final List<ClickEventRequest> eventBuffer = new ArrayList<>();
    private final ReentrantLock bufferLock = new ReentrantLock();
    
    /**
     * Add event to buffer. Flushes automatically when buffer is full.
     */
    public void addEvent(ClickEventRequest request) {
        bufferLock.lock();
        try {
            eventBuffer.add(request);
            
            // Flush if buffer is full
            if (eventBuffer.size() >= batchSize) {
                flushBuffer();
            }
        } finally {
            bufferLock.unlock();
        }
    }
    
    /**
     * Scheduled flush to ensure events are processed even if buffer doesn't fill up.
     * Runs every N seconds (configurable via flush-interval-seconds).
     */
    @Scheduled(fixedDelayString = "${stats.batch.flush-interval-seconds:5}000")
    public void scheduledFlush() {
        bufferLock.lock();
        try {
            if (!eventBuffer.isEmpty()) {
                flushBuffer();
            }
        } finally {
            bufferLock.unlock();
        }
    }
    
    /**
     * Flush all events in buffer to database using bulk insert.
     */
    @Transactional
    private void flushBuffer() {
        if (eventBuffer.isEmpty()) {
            return;
        }
        
        List<ClickEventRequest> eventsToProcess;
        bufferLock.lock();
        try {
            eventsToProcess = new ArrayList<>(eventBuffer);
            eventBuffer.clear();
        } finally {
            bufferLock.unlock();
        }
        
        try {
            // Convert to entities
            List<UrlClickEvent> events = eventsToProcess.stream()
                    .map(request -> UrlClickEvent.builder()
                            .shortCode(request.getShortCode())
                            .clickedAt(LocalDateTime.now())
                            .country(request.getCountry())
                            .city(request.getCity())
                            .userAgent(request.getUserAgent())
                            .deviceType(request.getDeviceType())
                            .referrer(request.getReferrer())
                            .ipAddress(request.getIpAddress())
                            .build())
                    .toList();
            
            // Bulk insert (JPA will batch based on hibernate.jdbc.batch_size)
            clickEventRepository.saveAll(events);
            
            log.debug("Flushed {} events to database", events.size());
            
            // Note: Statistics aggregation is handled separately by scheduled job
            // to avoid blocking the event processing pipeline
            
        } catch (Exception e) {
            log.error("Error flushing event batch (size: {})", eventsToProcess.size(), e);
            // In production, consider sending failed events to a dead letter queue
        }
    }
    
    /**
     * Force flush remaining events (useful for shutdown).
     */
    @Transactional
    public void forceFlush() {
        bufferLock.lock();
        try {
            flushBuffer();
        } finally {
            bufferLock.unlock();
        }
    }
}

