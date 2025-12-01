package com.tinyurl.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickEventRequest {
    private String shortCode;
    private String country;
    private String city;
    private String userAgent;
    private String deviceType;
    private String referrer;
    private String ipAddress;
}

