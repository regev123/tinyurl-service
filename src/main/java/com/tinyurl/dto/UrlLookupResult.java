package com.tinyurl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlLookupResult {
    private String shortUrl;
    private String originalUrl;
    private boolean found;
    private String message;
}
