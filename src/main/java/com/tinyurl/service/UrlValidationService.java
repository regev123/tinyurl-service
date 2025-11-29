package com.tinyurl.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import static com.tinyurl.constants.UrlConstants.*;

/**
 * Service for URL and short code validation
 * Follows Single Responsibility Principle - only handles input validation
 * Provides centralized validation logic that can be reused across the application
 */
@Slf4j
@Service
public class UrlValidationService {
    
    // Pre-compiled patterns for better performance (Pattern.compile is expensive)
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)$"
    );
    
    private static final Pattern SHORT_CODE_PATTERN = Pattern.compile("^[a-zA-Z0-9]{1,10}$");
    
    // Protocol constants
    private static final String HTTP_PROTOCOL = "http";
    private static final String HTTPS_PROTOCOL = "https";
    
    /**
     * Validates an original URL
     * 
     * @param originalUrl the URL to validate
     * @throws IllegalArgumentException if URL is invalid
     */
    public void validateOriginalUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Original URL cannot be null or empty");
        }
        
        if (originalUrl.length() > MAX_ORIGINAL_URL_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Original URL exceeds maximum length of %d characters", MAX_ORIGINAL_URL_LENGTH)
            );
        }
        
        // Try to parse as URL
        try {
            URL url = new URL(originalUrl);
            String protocol = url.getProtocol();
            if (!isValidProtocol(protocol)) {
                throw new IllegalArgumentException(
                        String.format("URL must use %s or %s protocol, got: %s", 
                                HTTP_PROTOCOL, HTTPS_PROTOCOL, protocol));
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL format: " + originalUrl, e);
        }
        
        // Additional regex validation
        if (!URL_PATTERN.matcher(originalUrl).matches()) {
            throw new IllegalArgumentException("URL format is invalid");
        }
    }
    
    /**
     * Validates a short code
     * 
     * @param shortCode the short code to validate
     * @throws IllegalArgumentException if short code is invalid
     */
    public void validateShortCode(String shortCode) {
        if (shortCode == null || shortCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Short code cannot be null or empty");
        }
        
        if (shortCode.length() > MAX_SHORT_CODE_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Short code exceeds maximum length of %d characters", MAX_SHORT_CODE_LENGTH)
            );
        }
        
        if (!SHORT_CODE_PATTERN.matcher(shortCode).matches()) {
            throw new IllegalArgumentException("Short code contains invalid characters. Only alphanumeric characters are allowed");
        }
    }
    
    /**
     * Validates a base URL
     * 
     * @param baseUrl the base URL to validate
     * @throws IllegalArgumentException if base URL is invalid
     */
    public void validateBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Base URL cannot be null or empty");
        }
        
        try {
            URL url = new URL(baseUrl);
            String protocol = url.getProtocol();
            if (!isValidProtocol(protocol)) {
                throw new IllegalArgumentException(
                        String.format("Base URL must use %s or %s protocol, got: %s", 
                                HTTP_PROTOCOL, HTTPS_PROTOCOL, protocol));
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid base URL format: " + baseUrl, e);
        }
    }
    
    /**
     * Validates if protocol is http or https
     * Extracted to avoid duplication (DRY principle)
     * 
     * @param protocol the protocol to validate
     * @return true if protocol is http or https
     */
    private boolean isValidProtocol(String protocol) {
        return HTTP_PROTOCOL.equals(protocol) || HTTPS_PROTOCOL.equals(protocol);
    }
}

