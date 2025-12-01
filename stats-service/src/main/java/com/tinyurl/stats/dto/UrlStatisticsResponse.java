package com.tinyurl.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlStatisticsResponse {
    private String shortCode;
    private Long totalClicks;
    private Long clicksToday;
    private Long clicksThisWeek;
    private Long clicksThisMonth;
    private LocalDateTime firstClickAt;
    private LocalDateTime lastClickAt;
    private List<CountryStats> topCountries;
    private List<ClickTimeline> clickTimeline;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountryStats {
        private String country;
        private Long clicks;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClickTimeline {
        private String date;
        private Long clicks;
    }
}

