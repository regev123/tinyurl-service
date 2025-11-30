package com.tinyurl.create.dto;

import com.tinyurl.constants.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result DTO for URL creation operation
 * Part of the Create Service microservice
 * 
 * Follows Single Responsibility Principle - only holds creation result data
 * Follows Immutability - uses builder pattern for construction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUrlResult {
    
    private String originalUrl;
    private String shortUrl;
    private String shortCode;
    
    @Builder.Default
    private boolean success = true;
    
    private String message;
    private ErrorCode errorCode;
}

