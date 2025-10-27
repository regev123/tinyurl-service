package com.tinyurl.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UrlBuilderTest {

    @Test
    void testBuildShortUrl_WithBaseUrl() {
        // When
        String result = UrlBuilder.buildShortUrl("http://localhost:8080", "abc123");

        // Then
        assertEquals("http://localhost:8080/abc123", result);
    }

    @Test
    void testBuildShortUrl_WithTrailingSlash() {
        // When
        String result = UrlBuilder.buildShortUrl("http://localhost:8080/", "abc123");

        // Then
        assertEquals("http://localhost:8080/abc123", result);
    }

    @Test
    void testBuildShortUrl_WithComplexPath() {
        // When
        String result = UrlBuilder.buildShortUrl("https://example.com/api", "xyz789");

        // Then
        assertEquals("https://example.com/api/xyz789", result);
    }

    @Test
    void testBuildShortUrl_NullBaseUrl() {
        // Then
        assertThrows(IllegalArgumentException.class, () ->
            UrlBuilder.buildShortUrl(null, "abc123"));
    }

    @Test
    void testBuildShortUrl_EmptyBaseUrl() {
        // Then
        assertThrows(IllegalArgumentException.class, () ->
            UrlBuilder.buildShortUrl("", "abc123"));
    }

    @Test
    void testBuildShortUrl_NullShortCode() {
        // Then
        assertThrows(IllegalArgumentException.class, () ->
            UrlBuilder.buildShortUrl("http://localhost:8080", null));
    }

    @Test
    void testExtractBaseUrl_StandardPort() {
        // When
        String baseUrl = UrlBuilder.extractBaseUrl("http", "localhost", 80);

        // Then
        assertEquals("http://localhost", baseUrl);
    }

    @Test
    void testExtractBaseUrl_CustomPort() {
        // When
        String baseUrl = UrlBuilder.extractBaseUrl("http", "localhost", 8080);

        // Then
        assertEquals("http://localhost:8080", baseUrl);
    }

    @Test
    void testExtractBaseUrl_Https() {
        // When
        String baseUrl = UrlBuilder.extractBaseUrl("https", "example.com", 443);

        // Then
        assertEquals("https://example.com", baseUrl);
    }

    @Test
    void testExtractBaseUrl_HttpsCustomPort() {
        // When
        String baseUrl = UrlBuilder.extractBaseUrl("https", "example.com", 8443);

        // Then
        assertEquals("https://example.com:8443", baseUrl);
    }
}

