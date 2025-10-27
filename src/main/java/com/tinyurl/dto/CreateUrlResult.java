package com.tinyurl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUrlResult {
    private String originalUrl;
    private String shortUrl;
    private String shortCode;
    private boolean success;
    private String message;
}
