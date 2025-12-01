package com.tinyurl.create.controller;

import com.tinyurl.create.dto.CreateUrlRequest;
import com.tinyurl.create.dto.CreateUrlResult;
import com.tinyurl.create.service.CreateUrlService;
import com.tinyurl.create.service.QrCodeService;
import com.tinyurl.create.service.RequestContextExtractor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for URL creation operations
 * Part of the Create Service microservice architecture
 * 
 * Follows Single Responsibility Principle - only handles URL creation HTTP concerns
 * Follows Dependency Inversion Principle - depends on UrlCreationService abstraction
 * 
 * Endpoint: POST /api/v1/create/shorten
 * 
 * Uses CreateUrlService which is dedicated to creation operations only
 */
@RestController
@RequestMapping("/api/v1/create")
@RequiredArgsConstructor
@Slf4j
public class CreateUrlController {
    
    private final CreateUrlService createUrlService;
    private final RequestContextExtractor requestContextExtractor;
    private final QrCodeService qrCodeService;
    
    /**
     * Creates a short URL for the given original URL
     * 
     * This is the dedicated endpoint for the Create Service
     * Handles only URL creation operations
     * 
     * @param request the create URL request containing originalUrl and optional baseUrl
     * @param httpRequest the HTTP servlet request for extracting base URL
     * @return ResponseEntity containing the CreateUrlResult
     */
    @PostMapping("/shorten")
    public ResponseEntity<CreateUrlResult> createShortUrl(
            @Valid @RequestBody CreateUrlRequest request,
            HttpServletRequest httpRequest) {
        
        String baseUrl = extractBaseUrl(request, httpRequest);
        
        CreateUrlResult result = createUrlService.createShortUrl(
                request.getOriginalUrl(), 
                baseUrl
        );
        
        HttpStatus status = result.isSuccess() 
                ? HttpStatus.CREATED 
                : HttpStatus.INTERNAL_SERVER_ERROR;
        
        return ResponseEntity.status(status).body(result);
    }
    
    /**
     * Extracts base URL from request or uses provided one
     * 
     * @param request the create URL request
     * @param httpRequest the HTTP servlet request
     * @return the base URL to use for constructing the short URL
     */
    /**
     * Generates a QR code image for a short URL
     * 
     * @param shortUrl the short URL to encode in the QR code
     * @return ResponseEntity containing the QR code image as PNG
     */
    @GetMapping("/qr")
    public ResponseEntity<byte[]> generateQrCode(@RequestParam("shortUrl") String shortUrl) {
        try {
            byte[] qrCodeImage = qrCodeService.generateQrCode(shortUrl);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(qrCodeImage.length);
            headers.setCacheControl("public, max-age=3600"); // Cache for 1 hour
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(qrCodeImage);
        } catch (Exception e) {
            log.error("Error generating QR code for URL: {}", shortUrl, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Extracts base URL from request or uses provided one
     * 
     * @param request the create URL request
     * @param httpRequest the HTTP servlet request
     * @return the base URL to use for constructing the short URL
     */
    private String extractBaseUrl(CreateUrlRequest request, HttpServletRequest httpRequest) {
        String baseUrl = request.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = requestContextExtractor.extractBaseUrl(httpRequest);
        }
        return baseUrl;
    }
}

