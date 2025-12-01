package com.tinyurl.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Click event DTO for Kafka messaging
 * Shared between lookup-service (producer) and stats-service (consumer)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickEvent implements Serializable {
    private String shortCode;
    private String ipAddress;
    private String userAgent;
    private String referrer;
    private String country;
    private String city;
    private String deviceType;
    private Long timestamp; // Unix timestamp in milliseconds
}

