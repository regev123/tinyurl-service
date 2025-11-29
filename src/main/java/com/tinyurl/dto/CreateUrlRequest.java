package com.tinyurl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.tinyurl.constants.UrlConstants.MAX_ORIGINAL_URL_LENGTH;

/**
 * Request DTO for creating a short URL
 * Follows Single Responsibility Principle - only holds request data
 * Follows Encapsulation - data is properly encapsulated with validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUrlRequest {
    
    @NotBlank(message = "Original URL is required")
    @Size(max = MAX_ORIGINAL_URL_LENGTH, message = "Original URL exceeds maximum length")
    private String originalUrl;
    
    private String baseUrl;
}
