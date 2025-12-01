package com.tinyurl.stats.service;

import com.tinyurl.stats.entity.UrlClickEvent;
import com.tinyurl.stats.entity.UrlStatistics;
import com.tinyurl.stats.repository.UrlClickEventRepository;
import com.tinyurl.stats.repository.UrlStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scheduled service to aggregate statistics periodically.
 * This avoids recalculating stats on every click event, significantly reducing database load.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsAggregationService {
    
    private final UrlClickEventRepository clickEventRepository;
    private final UrlStatisticsRepository statisticsRepository;
    
    @Value("${stats.aggregation.update-interval-minutes:10}")
    private int updateIntervalMinutes;
    
    @Value("${stats.aggregation.enabled:true}")
    private boolean aggregationEnabled;
    
    /**
     * Scheduled job to update aggregated statistics for all URLs.
     * Runs every N minutes (configurable via update-interval-minutes).
     * 
     * This approach is much more efficient than updating stats on every click:
     * - Instead of 9 DB operations per click, we do 1 aggregation per URL per interval
     * - For 100M clicks/day, if we have 10M unique URLs, that's 10M aggregations per 10 minutes
     * - vs 900M DB operations per day with the old approach
     */
    @Scheduled(fixedDelayString = "${stats.aggregation.update-interval-minutes:10}00000")
    public void aggregateStatistics() {
        if (!aggregationEnabled) {
            log.debug("Statistics aggregation is disabled");
            return;
        }
        
        log.info("Starting statistics aggregation...");
        long startTime = System.currentTimeMillis();
        
        try {
            // Get all unique short codes that have click events
            Set<String> shortCodes = clickEventRepository.findAll()
                    .stream()
                    .map(UrlClickEvent::getShortCode)
                    .collect(Collectors.toSet());
            
            log.info("Aggregating statistics for {} unique URLs", shortCodes.size());
            
            int processed = 0;
            for (String shortCode : shortCodes) {
                try {
                    updateStatisticsForUrl(shortCode);
                    processed++;
                    
                    // Log progress every 1000 URLs
                    if (processed % 1000 == 0) {
                        log.debug("Processed {} URLs...", processed);
                    }
                } catch (Exception e) {
                    log.error("Error aggregating statistics for shortCode: {}", shortCode, e);
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Completed statistics aggregation for {} URLs in {} ms", processed, duration);
            
        } catch (Exception e) {
            log.error("Error during statistics aggregation", e);
        }
    }
    
    /**
     * Update statistics for a specific URL.
     * This is the same logic as StatsService.updateStatistics but optimized for batch processing.
     */
    @Transactional
    public void updateStatisticsForUrl(String shortCode) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime startOfWeek = startOfDay.minusDays(now.getDayOfWeek().getValue() - 1);
        LocalDateTime startOfMonth = LocalDateTime.of(now.getYear(), now.getMonth(), 1, 0, 0);
        
        Long totalClicks = clickEventRepository.countByShortCode(shortCode);
        Long clicksToday = clickEventRepository.countByShortCodeAndClickedAtAfter(shortCode, startOfDay);
        Long clicksThisWeek = clickEventRepository.countByShortCodeAndClickedAtAfter(shortCode, startOfWeek);
        Long clicksThisMonth = clickEventRepository.countByShortCodeAndClickedAtAfter(shortCode, startOfMonth);
        
        Optional<UrlClickEvent> firstClick = clickEventRepository.findByShortCodeOrderByClickedAtDesc(shortCode)
                .stream()
                .reduce((first, second) -> second); // Get first (oldest)
        
        Optional<UrlClickEvent> lastClick = clickEventRepository.findByShortCodeOrderByClickedAtDesc(shortCode)
                .stream()
                .findFirst(); // Get last (newest)
        
        UrlStatistics stats = statisticsRepository.findByShortCode(shortCode)
                .orElse(UrlStatistics.builder()
                        .shortCode(shortCode)
                        .totalClicks(0L)
                        .clicksToday(0L)
                        .clicksThisWeek(0L)
                        .clicksThisMonth(0L)
                        .build());
        
        stats.setTotalClicks(totalClicks);
        stats.setClicksToday(clicksToday);
        stats.setClicksThisWeek(clicksThisWeek);
        stats.setClicksThisMonth(clicksThisMonth);
        
        if (firstClick.isPresent()) {
            stats.setFirstClickAt(firstClick.get().getClickedAt());
        }
        if (lastClick.isPresent()) {
            stats.setLastClickAt(lastClick.get().getClickedAt());
        }
        
        statisticsRepository.save(stats);
    }
}

