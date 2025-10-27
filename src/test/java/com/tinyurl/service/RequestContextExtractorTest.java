package com.tinyurl.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestContextExtractorTest {

    private RequestContextExtractor requestContextExtractor;
    private HttpServletRequest httpRequest;

    @BeforeEach
    void setUp() {
        requestContextExtractor = new RequestContextExtractor();
        httpRequest = mock(HttpServletRequest.class);
    }

    @Test
    void testExtractBaseUrl_StandardPort() {
        // Given
        when(httpRequest.getScheme()).thenReturn("http");
        when(httpRequest.getServerName()).thenReturn("localhost");
        when(httpRequest.getServerPort()).thenReturn(80);

        // When
        String baseUrl = requestContextExtractor.extractBaseUrl(httpRequest);

        // Then
        assertEquals("http://localhost", baseUrl);
    }

    @Test
    void testExtractBaseUrl_CustomPort() {
        // Given
        when(httpRequest.getScheme()).thenReturn("http");
        when(httpRequest.getServerName()).thenReturn("localhost");
        when(httpRequest.getServerPort()).thenReturn(8080);

        // When
        String baseUrl = requestContextExtractor.extractBaseUrl(httpRequest);

        // Then
        assertEquals("http://localhost:8080", baseUrl);
    }

    @Test
    void testExtractBaseUrl_Https() {
        // Given
        when(httpRequest.getScheme()).thenReturn("https");
        when(httpRequest.getServerName()).thenReturn("example.com");
        when(httpRequest.getServerPort()).thenReturn(443);

        // When
        String baseUrl = requestContextExtractor.extractBaseUrl(httpRequest);

        // Then
        assertEquals("https://example.com", baseUrl);
    }

    @Test
    void testExtractBaseUrl_HttpsCustomPort() {
        // Given
        when(httpRequest.getScheme()).thenReturn("https");
        when(httpRequest.getServerName()).thenReturn("example.com");
        when(httpRequest.getServerPort()).thenReturn(8443);

        // When
        String baseUrl = requestContextExtractor.extractBaseUrl(httpRequest);

        // Then
        assertEquals("https://example.com:8443", baseUrl);
    }
}

