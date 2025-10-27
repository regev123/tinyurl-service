package com.tinyurl.controller;

import com.tinyurl.dto.CreateUrlRequest;
import com.tinyurl.dto.CreateUrlResult;
import com.tinyurl.dto.UrlLookupResult;
import com.tinyurl.service.RequestContextExtractor;
import com.tinyurl.service.UrlShorteningService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TinyUrlController {
    
    private final UrlShorteningService urlShorteningService;
    private final RequestContextExtractor requestContextExtractor;
    
    /**
     * Creates a short URL for the given original URL
     */
    @PostMapping("/api/v1/url/shorten")
    public ResponseEntity<CreateUrlResult> createShortUrl(
            @Valid @RequestBody CreateUrlRequest request,
            HttpServletRequest httpRequest) {
        
        String baseUrl = request.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = requestContextExtractor.extractBaseUrl(httpRequest);
        }
        
        CreateUrlResult result = urlShorteningService.createShortUrl(
                request.getOriginalUrl(), 
                baseUrl
        );
        
        HttpStatus status = result.isSuccess() 
                ? HttpStatus.CREATED 
                : HttpStatus.INTERNAL_SERVER_ERROR;
        
        return ResponseEntity.status(status).body(result);
    }
    
    @GetMapping("/{shortUrl}")
    public ResponseEntity<?> getOriginalUrl(
            @PathVariable("shortUrl") String shortUrl) {
        
        UrlLookupResult result = urlShorteningService.lookupUrl(shortUrl);
        
        if (result.isFound()) {
            // Redirect to original URL
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(java.net.URI.create(result.getOriginalUrl()))
                    .build();
        } else {
            HttpStatus status = result.getMessage().contains("Internal server error")
                    ? HttpStatus.INTERNAL_SERVER_ERROR
                    : HttpStatus.NOT_FOUND;
            return ResponseEntity.status(status).body(result);
        }
    }
}
