package com.tinyurl.lookup.service;

import com.tinyurl.event.ClickEvent;
import com.tinyurl.lookup.constants.MockGeoDataConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsClientService {
    
    @Value("${kafka.topic.click-events:url-click-events}")
    private String clickEventsTopic;
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public void recordClickEvent(String shortCode, String ipAddress, String userAgent, String referrer) {
        try {
            // Get random country first, then get a city from that country
            String country = getRandomCountry();
            String city = getRandomCityForCountry(country);
            
            ClickEvent event = ClickEvent.builder()
                    .shortCode(shortCode)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .deviceType(extractDeviceType(userAgent))
                    .country(country) // Mocked - Would be extracted from IP in production (e.g., using MaxMind GeoIP2)
                    .city(city) // Mocked - Would be extracted from IP in production (e.g., using MaxMind GeoIP2)
                    .timestamp(Instant.now().toEpochMilli())
                    .build();
            
            // Send to Kafka asynchronously
            CompletableFuture<SendResult<String, Object>> future = 
                    kafkaTemplate.send(clickEventsTopic, shortCode, event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Successfully sent click event to Kafka for shortCode: {}", shortCode);
                } else {
                    log.warn("Failed to send click event to Kafka for shortCode: {}", shortCode, ex);
                    // Event is lost, but lookup service continues to work
                    // In production, consider implementing a dead letter queue
                }
            });
        } catch (Exception e) {
            // Don't fail the lookup if Kafka is unavailable
            log.warn("Error sending click event to Kafka for shortCode: {}", shortCode, e);
        }
    }
    
    private String extractDeviceType(String userAgent) {
        if (userAgent == null) {
            return "UNKNOWN";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return "MOBILE";
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            return "TABLET";
        } else {
            return "DESKTOP";
        }
    }
    
    private String getRandomCountry() {
        return MockGeoDataConstants.getRandomCountry();
    }
    
    private String getRandomCityForCountry(String country) {
        return MockGeoDataConstants.getRandomCity(country);
    }
}

