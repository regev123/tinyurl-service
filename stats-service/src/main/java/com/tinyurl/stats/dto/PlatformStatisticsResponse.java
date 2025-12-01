package com.tinyurl.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformStatisticsResponse {
    private Long totalUrls;
    private Long totalClicks;
    private Long clicksToday;
    private Long activeUrls;
    private LocalDateTime lastUpdated;
}

