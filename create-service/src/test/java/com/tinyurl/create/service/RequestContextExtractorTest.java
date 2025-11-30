package com.tinyurl.create.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestContextExtractor Tests")
class RequestContextExtractorTest {

    @InjectMocks
    private RequestContextExtractor requestContextExtractor;

    private HttpServletRequest httpServletRequest;

    @BeforeEach
    void setUp() {
        httpServletRequest = mock(HttpServletRequest.class);
    }

    @Test
    @DisplayName("extractBaseUrl - HTTPS with standard port")
    void extractBaseUrl_HttpsStandardPort_ReturnsUrlWithoutPort() {
        // Given
        when(httpServletRequest.getScheme()).thenReturn("https");
        when(httpServletRequest.getServerName()).thenReturn("tiny.url");
        when(httpServletRequest.getServerPort()).thenReturn(443);

        // When
        String result = requestContextExtractor.extractBaseUrl(httpServletRequest);

        // Then
        assertThat(result).isEqualTo("https://tiny.url");
    }

    @Test
    @DisplayName("extractBaseUrl - HTTP with standard port")
    void extractBaseUrl_HttpStandardPort_ReturnsUrlWithoutPort() {
        // Given
        when(httpServletRequest.getScheme()).thenReturn("http");
        when(httpServletRequest.getServerName()).thenReturn("tiny.url");
        when(httpServletRequest.getServerPort()).thenReturn(80);

        // When
        String result = requestContextExtractor.extractBaseUrl(httpServletRequest);

        // Then
        assertThat(result).isEqualTo("http://tiny.url");
    }

    @Test
    @DisplayName("extractBaseUrl - HTTPS with non-standard port")
    void extractBaseUrl_HttpsNonStandardPort_ReturnsUrlWithPort() {
        // Given
        when(httpServletRequest.getScheme()).thenReturn("https");
        when(httpServletRequest.getServerName()).thenReturn("tiny.url");
        when(httpServletRequest.getServerPort()).thenReturn(8080);

        // When
        String result = requestContextExtractor.extractBaseUrl(httpServletRequest);

        // Then
        assertThat(result).isEqualTo("https://tiny.url:8080");
    }

    @Test
    @DisplayName("extractBaseUrl - HTTP with non-standard port")
    void extractBaseUrl_HttpNonStandardPort_ReturnsUrlWithPort() {
        // Given
        when(httpServletRequest.getScheme()).thenReturn("http");
        when(httpServletRequest.getServerName()).thenReturn("tiny.url");
        when(httpServletRequest.getServerPort()).thenReturn(8080);

        // When
        String result = requestContextExtractor.extractBaseUrl(httpServletRequest);

        // Then
        assertThat(result).isEqualTo("http://tiny.url:8080");
    }

    @Test
    @DisplayName("extractBaseUrl - HTTPS with port 8443")
    void extractBaseUrl_HttpsPort8443_ReturnsUrlWithPort() {
        // Given
        when(httpServletRequest.getScheme()).thenReturn("https");
        when(httpServletRequest.getServerName()).thenReturn("tiny.url");
        when(httpServletRequest.getServerPort()).thenReturn(8443);

        // When
        String result = requestContextExtractor.extractBaseUrl(httpServletRequest);

        // Then
        assertThat(result).isEqualTo("https://tiny.url:8443");
    }

    @Test
    @DisplayName("extractBaseUrl - HTTP with port 3000")
    void extractBaseUrl_HttpPort3000_ReturnsUrlWithPort() {
        // Given
        when(httpServletRequest.getScheme()).thenReturn("http");
        when(httpServletRequest.getServerName()).thenReturn("tiny.url");
        when(httpServletRequest.getServerPort()).thenReturn(3000);

        // When
        String result = requestContextExtractor.extractBaseUrl(httpServletRequest);

        // Then
        assertThat(result).isEqualTo("http://tiny.url:3000");
    }

    @Test
    @DisplayName("extractBaseUrl - Null request throws exception")
    void extractBaseUrl_NullRequest_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> requestContextExtractor.extractBaseUrl(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTP request cannot be null");
    }

    @Test
    @DisplayName("extractBaseUrl - Localhost with port")
    void extractBaseUrl_LocalhostWithPort_ReturnsCorrectUrl() {
        // Given
        when(httpServletRequest.getScheme()).thenReturn("http");
        when(httpServletRequest.getServerName()).thenReturn("localhost");
        when(httpServletRequest.getServerPort()).thenReturn(8081);

        // When
        String result = requestContextExtractor.extractBaseUrl(httpServletRequest);

        // Then
        assertThat(result).isEqualTo("http://localhost:8081");
    }

    @Test
    @DisplayName("extractBaseUrl - IP address with port")
    void extractBaseUrl_IpAddressWithPort_ReturnsCorrectUrl() {
        // Given
        when(httpServletRequest.getScheme()).thenReturn("https");
        when(httpServletRequest.getServerName()).thenReturn("192.168.1.1");
        when(httpServletRequest.getServerPort()).thenReturn(8443);

        // When
        String result = requestContextExtractor.extractBaseUrl(httpServletRequest);

        // Then
        assertThat(result).isEqualTo("https://192.168.1.1:8443");
    }

    @Test
    @DisplayName("extractBaseUrl - IP address with standard port")
    void extractBaseUrl_IpAddressStandardPort_ReturnsUrlWithoutPort() {
        // Given
        when(httpServletRequest.getScheme()).thenReturn("http");
        when(httpServletRequest.getServerName()).thenReturn("192.168.1.1");
        when(httpServletRequest.getServerPort()).thenReturn(80);

        // When
        String result = requestContextExtractor.extractBaseUrl(httpServletRequest);

        // Then
        assertThat(result).isEqualTo("http://192.168.1.1");
    }

    @Test
    @DisplayName("extractBaseUrl - Domain with subdomain")
    void extractBaseUrl_DomainWithSubdomain_ReturnsCorrectUrl() {
        // Given
        when(httpServletRequest.getScheme()).thenReturn("https");
        when(httpServletRequest.getServerName()).thenReturn("api.tiny.url");
        when(httpServletRequest.getServerPort()).thenReturn(443);

        // When
        String result = requestContextExtractor.extractBaseUrl(httpServletRequest);

        // Then
        assertThat(result).isEqualTo("https://api.tiny.url");
    }
}

