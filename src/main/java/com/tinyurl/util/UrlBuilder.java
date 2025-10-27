package com.tinyurl.util;

public final class UrlBuilder {
    
    private UrlBuilder() {
        // Utility class - prevent instantiation
    }
    
    public static String buildShortUrl(String baseUrl, String shortCode) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("Base URL cannot be null or empty");
        }
        if (shortCode == null || shortCode.isEmpty()) {
            throw new IllegalArgumentException("Short code cannot be null or empty");
        }
        
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return base + "/" + shortCode;
    }
    
    public static String extractBaseUrl(String scheme, String serverName, int serverPort) {
        boolean includePort = serverPort != 80 && serverPort != 443;
        String port = includePort ? ":" + serverPort : "";
        return scheme + "://" + serverName + port;
    }
}

