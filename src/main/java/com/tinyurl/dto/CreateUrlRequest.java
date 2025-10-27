package com.tinyurl.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUrlRequest {
    
    @NotBlank(message = "Original URL is required")
    private String originalUrl;
    
    private String baseUrl;
}
