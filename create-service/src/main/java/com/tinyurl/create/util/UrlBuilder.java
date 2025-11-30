package com.tinyurl.create.util;

/**
 * Utility class for building URLs
 * Part of the Create Service microservice
 * 
 * Follows Single Responsibility Principle - only handles URL building
 * Follows Encapsulation - static utility methods with no state
 */
public final class UrlBuilder {
    
    // Standard HTTP/HTTPS ports
    private static final int HTTP_PORT = 80;
    private static final int HTTPS_PORT = 443;
    private static final String URL_SEPARATOR = "/";
    private static final String PROTOCOL_SEPARATOR = "://";
    private static final String PORT_SEPARATOR = ":";
    
    private UrlBuilder() {
        // Utility class - prevent instantiation
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Builds a complete short URL from base URL and short code
     * 
     * @param baseUrl the base URL (must not be null or empty)
     * @param shortCode the short code (must not be null or empty)
     * @return complete short URL
     * @throws IllegalArgumentException if baseUrl or shortCode is null or empty
     */
    public static String buildShortUrl(String baseUrl, String shortCode) {
        validateInput(baseUrl, "Base URL");
        validateInput(shortCode, "Short code");
        
        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
        return normalizedBaseUrl + URL_SEPARATOR + shortCode;
    }
    
    /**
     * Extracts base URL from HTTP request components
     * 
     * @param scheme the URL scheme (http/https)
     * @param serverName the server name
     * @param serverPort the server port
     * @return base URL in format scheme://host:port (port omitted if standard)
     */
    public static String extractBaseUrl(String scheme, String serverName, int serverPort) {
        boolean includePort = shouldIncludePort(serverPort);
        String portSegment = includePort ? PORT_SEPARATOR + serverPort : "";
        return scheme + PROTOCOL_SEPARATOR + serverName + portSegment;
    }
    
    /**
     * Normalizes base URL by removing trailing slash
     * 
     * @param baseUrl the base URL to normalize
     * @return normalized base URL without trailing slash
     */
    private static String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith(URL_SEPARATOR) 
                ? baseUrl.substring(0, baseUrl.length() - 1) 
                : baseUrl;
    }
    
    /**
     * Determines if port should be included in URL
     * Standard ports (80 for HTTP, 443 for HTTPS) are omitted
     * 
     * @param port the port number
     * @return true if port should be included
     */
    private static boolean shouldIncludePort(int port) {
        return port != HTTP_PORT && port != HTTPS_PORT;
    }
    
    /**
     * Validates input string is not null or empty
     * 
     * @param value the value to validate
     * @param fieldName the field name for error message
     * @throws IllegalArgumentException if value is null or empty
     */
    private static void validateInput(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }
}

