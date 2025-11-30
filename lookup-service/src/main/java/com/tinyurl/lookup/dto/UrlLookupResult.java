package com.tinyurl.lookup.dto;

import com.tinyurl.constants.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result DTO for URL lookup operation
 * Part of the Lookup Service microservice
 * 
 * Follows Single Responsibility Principle - only holds lookup result data
 * Follows Immutability - uses builder pattern for construction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlLookupResult {
    
    private String shortUrl;
    private String originalUrl;
    
    @Builder.Default
    private boolean found = false;
    
    private String message;
    private ErrorCode errorCode;
}

