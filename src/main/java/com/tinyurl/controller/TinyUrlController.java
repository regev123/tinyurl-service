package com.tinyurl.controller;

import com.tinyurl.constants.ErrorCode;
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

/**
 * REST controller for URL shortening operations
 * Follows Single Responsibility Principle - only handles HTTP concerns
 * Follows Dependency Inversion Principle - depends on service abstractions
 */
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
        
        String baseUrl = extractBaseUrl(request, httpRequest);
        
        CreateUrlResult result = urlShorteningService.createShortUrl(
                request.getOriginalUrl(), 
                baseUrl
        );
        
        HttpStatus status = result.isSuccess() 
                ? HttpStatus.CREATED 
                : HttpStatus.INTERNAL_SERVER_ERROR;
        
        return ResponseEntity.status(status).body(result);
    }
    
    /**
     * Retrieves the original URL for a short URL and redirects
     */
    @GetMapping("/{shortUrl}")
    public ResponseEntity<?> getOriginalUrl(
            @PathVariable("shortUrl") String shortUrl) {
        
        UrlLookupResult result = urlShorteningService.lookupUrl(shortUrl);
        
        if (result.isFound()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(java.net.URI.create(result.getOriginalUrl()))
                    .build();
        } else {
            HttpStatus status = mapErrorCodeToHttpStatus(result.getErrorCode());
            return ResponseEntity.status(status).body(result);
        }
    }
    
    /**
     * Extracts base URL from request or uses provided one
     * 
     * @param request the create URL request
     * @param httpRequest the HTTP servlet request
     * @return the base URL to use
     */
    private String extractBaseUrl(CreateUrlRequest request, HttpServletRequest httpRequest) {
        String baseUrl = request.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = requestContextExtractor.extractBaseUrl(httpRequest);
        }
        return baseUrl;
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
