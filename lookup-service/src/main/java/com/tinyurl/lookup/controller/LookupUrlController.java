package com.tinyurl.lookup.controller;

import com.tinyurl.constants.ErrorCode;
import com.tinyurl.lookup.dto.UrlLookupResult;
import com.tinyurl.lookup.service.LookupUrlService;
import com.tinyurl.lookup.service.StatsClientService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for URL lookup operations
 * Part of the Lookup Service microservice architecture
 * 
 * Follows Single Responsibility Principle - only handles URL lookup HTTP concerns
 * Follows Dependency Inversion Principle - depends on UrlLookupService abstraction
 * 
 * Note: URL creation has been moved to CreateUrlController (Create Service)
 * This controller handles only lookup/redirect operations
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class LookupUrlController {
    
    private final LookupUrlService lookupUrlService;
    private final StatsClientService statsClientService;
    
    /**
     * Retrieves the original URL for a short URL and redirects
     * 
     * This is the main endpoint for the Lookup Service
     * Handles URL lookups and redirects users to the original URL
     * 
     * @param shortUrl the short URL code to look up
     * @return ResponseEntity with redirect (302) or error response
     */
    @GetMapping("/{shortUrl}")
    public ResponseEntity<?> getOriginalUrl(
            @PathVariable("shortUrl") String shortUrl,
            HttpServletRequest request) {
        
        UrlLookupResult result = lookupUrlService.lookupUrl(shortUrl);
        
        if (result.isFound()) {
            // Record click event to Kafka (non-blocking, decoupled)
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            String referrer = request.getHeader("Referer");
            statsClientService.recordClickEvent(shortUrl, ipAddress, userAgent, referrer);
            
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(java.net.URI.create(result.getOriginalUrl()))
                    .build();
        } else {
            HttpStatus status = mapErrorCodeToHttpStatus(result.getErrorCode());
            return ResponseEntity.status(status).body(result);
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
    
    /**
     * Maps error code to appropriate HTTP status
     * Follows Open/Closed Principle - can be extended with new error codes without modification
     * 
     * @param errorCode the error code (may be null)
     * @return the HTTP status code
     */
    private HttpStatus mapErrorCodeToHttpStatus(ErrorCode errorCode) {
        if (errorCode == null) {
            return HttpStatus.NOT_FOUND;
        }
        
        return switch (errorCode) {
            case INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case URL_NOT_FOUND, URL_EXPIRED -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}

