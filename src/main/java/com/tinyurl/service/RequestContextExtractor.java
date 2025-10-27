package com.tinyurl.service;

import com.tinyurl.util.UrlBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class RequestContextExtractor {
    
    /**
     * Extracts the base URL from the HTTP request
     * 
     * @param httpRequest the HTTP servlet request
     * @return the base URL (scheme://host:port)
     */
    public String extractBaseUrl(HttpServletRequest httpRequest) {
        return UrlBuilder.extractBaseUrl(
            httpRequest.getScheme(),
            httpRequest.getServerName(),
            httpRequest.getServerPort()
        );
    }
}

