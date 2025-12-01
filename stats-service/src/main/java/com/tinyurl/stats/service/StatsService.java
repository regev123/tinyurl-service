package com.tinyurl.stats.service;

import com.tinyurl.stats.dto.ClickEventRequest;
import com.tinyurl.stats.dto.PlatformStatisticsResponse;
import com.tinyurl.stats.dto.UrlStatisticsResponse;
import com.tinyurl.stats.entity.UrlClickEvent;
import com.tinyurl.stats.entity.UrlStatistics;
import com.tinyurl.stats.repository.UrlClickEventRepository;
import com.tinyurl.stats.repository.UrlStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {
    
    private final UrlClickEventRepository clickEventRepository;
    private final UrlStatisticsRepository statisticsRepository;
    private final BatchEventProcessor batchEventProcessor;
    
    /**
     * Record click event using batch processing for high throughput.
     * Events are buffered and flushed in batches to reduce database load.
     * Statistics are updated separately by scheduled aggregation job.
     */
    @Async
    public void recordClickEvent(ClickEventRequest request) {
        try {
            // Add to batch processor (non-blocking)
            batchEventProcessor.addEvent(request);
            log.debug("Queued click event for shortCode: {}", request.getShortCode());
        } catch (Exception e) {
            log.error("Error queuing click event for shortCode: {}", request.getShortCode(), e);
        }
    }
    
    /**
     * Update statistics for a specific URL.
     * This method is now primarily used by StatisticsAggregationService for batch updates.
     * For individual updates, use StatisticsAggregationService.updateStatisticsForUrl().
     */
    @Transactional
    public void updateStatistics(String shortCode) {
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
    
    public UrlStatisticsResponse getUrlStatistics(String shortCode) {
        UrlStatistics stats = statisticsRepository.findByShortCode(shortCode)
                .orElse(UrlStatistics.builder()
                        .shortCode(shortCode)
                        .totalClicks(0L)
                        .clicksToday(0L)
                        .clicksThisWeek(0L)
                        .clicksThisMonth(0L)
                        .firstClickAt(LocalDateTime.now())
                        .lastClickAt(LocalDateTime.now())
                        .build());
        
        // Get top countries
        List<UrlStatisticsResponse.CountryStats> topCountries = clickEventRepository
                .findTopCountriesByShortCode(shortCode)
                .stream()
                .limit(10)
                .map(result -> UrlStatisticsResponse.CountryStats.builder()
                        .country((String) result[0])
                        .clicks((Long) result[1])
                        .build())
                .collect(Collectors.toList());
        
        // Get click timeline (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<UrlStatisticsResponse.ClickTimeline> clickTimeline = clickEventRepository
                .findClickTimelineByShortCode(shortCode, thirtyDaysAgo)
                .stream()
                .map(result -> {
                    // Native query returns: [java.sql.Date, Long]
                    java.sql.Date date = (java.sql.Date) result[0];
                    Long clicks = ((Number) result[1]).longValue();
                    return UrlStatisticsResponse.ClickTimeline.builder()
                            .date(date.toString())
                            .clicks(clicks)
                            .build();
                })
                .collect(Collectors.toList());
        
        return UrlStatisticsResponse.builder()
                .shortCode(stats.getShortCode())
                .totalClicks(stats.getTotalClicks())
                .clicksToday(stats.getClicksToday())
                .clicksThisWeek(stats.getClicksThisWeek())
                .clicksThisMonth(stats.getClicksThisMonth())
                .firstClickAt(stats.getFirstClickAt())
                .lastClickAt(stats.getLastClickAt())
                .topCountries(topCountries)
                .clickTimeline(clickTimeline)
                .build();
    }
    
    public PlatformStatisticsResponse getPlatformStatistics() {
        // This would typically query aggregated data
        // For now, return basic stats
        long totalUrls = statisticsRepository.count();
        long totalClicks = statisticsRepository.findAll().stream()
                .mapToLong(UrlStatistics::getTotalClicks)
                .sum();
        long clicksToday = statisticsRepository.findAll().stream()
                .mapToLong(UrlStatistics::getClicksToday)
                .sum();
        
        return PlatformStatisticsResponse.builder()
                .totalUrls(totalUrls)
                .totalClicks(totalClicks)
                .clicksToday(clicksToday)
                .activeUrls(totalUrls) // All URLs in stats are active (expired ones are deleted by cleanup job)
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}

