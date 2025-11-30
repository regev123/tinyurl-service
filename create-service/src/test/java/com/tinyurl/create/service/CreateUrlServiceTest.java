package com.tinyurl.create.service;

import com.tinyurl.create.dto.CreateUrlResult;
import com.tinyurl.entity.UrlMapping;
import com.tinyurl.create.exception.UrlGenerationException;
import com.tinyurl.create.repository.CreateUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateUrlService Tests")
class CreateUrlServiceTest {

    @Mock
    private CreateUrlRepository urlMappingRepository;

    @Mock
    private UrlCodeGenerator urlCodeGenerator;

    @Mock
    private UrlValidationService urlValidationService;

    @InjectMocks
    private CreateUrlService createUrlService;

    private String originalUrl;
    private String baseUrl;
    private String shortCode;
    private UrlMapping existingMapping;

    @BeforeEach
    void setUp() {
        originalUrl = "https://www.example.com";
        baseUrl = "https://tiny.url";
        shortCode = "abc123";

        existingMapping = new UrlMapping();
        existingMapping.setOriginalUrl(originalUrl);
        existingMapping.setShortUrl(shortCode);
    }

    @Test
    @DisplayName("createShortUrl - Success with new URL mapping")
    void createShortUrl_NewUrl_CreatesMapping() {
        // Given
        when(urlMappingRepository.findByOriginalUrl(originalUrl))
                .thenReturn(Optional.empty());
        when(urlCodeGenerator.generateUniqueCode())
                .thenReturn(shortCode);
        when(urlMappingRepository.save(any(UrlMapping.class)))
                .thenReturn(existingMapping);

        // When
        CreateUrlResult result = createUrlService.createShortUrl(originalUrl, baseUrl);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOriginalUrl()).isEqualTo(originalUrl);
        assertThat(result.getShortUrl()).isEqualTo("https://tiny.url/abc123");
        assertThat(result.getShortCode()).isEqualTo(shortCode);
        assertThat(result.getErrorCode()).isNull();

        verify(urlValidationService).validateOriginalUrl(originalUrl);
        verify(urlValidationService).validateBaseUrl(baseUrl);
        verify(urlMappingRepository).findByOriginalUrl(originalUrl);
        verify(urlCodeGenerator).generateUniqueCode();
        verify(urlMappingRepository).save(any(UrlMapping.class));
    }

    @Test
    @DisplayName("createShortUrl - Success with existing URL mapping")
    void createShortUrl_ExistingUrl_ReturnsExistingMapping() {
        // Given
        when(urlMappingRepository.findByOriginalUrl(originalUrl))
                .thenReturn(Optional.of(existingMapping));

        // When
        CreateUrlResult result = createUrlService.createShortUrl(originalUrl, baseUrl);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOriginalUrl()).isEqualTo(originalUrl);
        assertThat(result.getShortUrl()).isEqualTo("https://tiny.url/abc123");
        assertThat(result.getShortCode()).isEqualTo(shortCode);

        verify(urlMappingRepository).findByOriginalUrl(originalUrl);
        verify(urlCodeGenerator, never()).generateUniqueCode();
        verify(urlMappingRepository, never()).save(any(UrlMapping.class));
    }

    @Test
    @DisplayName("createShortUrl - Validates original URL")
    void createShortUrl_InvalidOriginalUrl_ThrowsException() {
        // Given
        String invalidUrl = "not-a-valid-url";
        doThrow(new IllegalArgumentException("Invalid URL format"))
                .when(urlValidationService).validateOriginalUrl(invalidUrl);

        // When & Then
        assertThatThrownBy(() -> createUrlService.createShortUrl(invalidUrl, baseUrl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid URL format");

        verify(urlValidationService).validateOriginalUrl(invalidUrl);
        verify(urlValidationService, never()).validateBaseUrl(anyString());
        verify(urlMappingRepository, never()).findByOriginalUrl(anyString());
    }

    @Test
    @DisplayName("createShortUrl - Validates base URL")
    void createShortUrl_InvalidBaseUrl_ThrowsException() {
        // Given
        String invalidBaseUrl = "not-a-valid-url";
        doThrow(new IllegalArgumentException("Invalid base URL format"))
                .when(urlValidationService).validateBaseUrl(invalidBaseUrl);

        // When & Then
        assertThatThrownBy(() -> createUrlService.createShortUrl(originalUrl, invalidBaseUrl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid base URL format");

        verify(urlValidationService).validateOriginalUrl(originalUrl);
        verify(urlValidationService).validateBaseUrl(invalidBaseUrl);
        verify(urlMappingRepository, never()).findByOriginalUrl(anyString());
    }

    @Test
    @DisplayName("createShortUrl - Handles code generation failure")
    void createShortUrl_CodeGenerationFails_ThrowsException() {
        // Given
        when(urlMappingRepository.findByOriginalUrl(originalUrl))
                .thenReturn(Optional.empty());
        when(urlCodeGenerator.generateUniqueCode())
                .thenThrow(new UrlGenerationException("Unable to generate unique code"));

        // When & Then
        assertThatThrownBy(() -> createUrlService.createShortUrl(originalUrl, baseUrl))
                .isInstanceOf(UrlGenerationException.class)
                .hasMessageContaining("Unable to generate unique code");

        verify(urlCodeGenerator).generateUniqueCode();
        verify(urlMappingRepository, never()).save(any(UrlMapping.class));
    }

    @Test
    @DisplayName("createShortUrl - Base URL with trailing slash")
    void createShortUrl_BaseUrlWithTrailingSlash_NormalizesCorrectly() {
        // Given
        String baseUrlWithSlash = "https://tiny.url/";
        when(urlMappingRepository.findByOriginalUrl(originalUrl))
                .thenReturn(Optional.empty());
        when(urlCodeGenerator.generateUniqueCode())
                .thenReturn(shortCode);
        when(urlMappingRepository.save(any(UrlMapping.class)))
                .thenReturn(existingMapping);

        // When
        CreateUrlResult result = createUrlService.createShortUrl(originalUrl, baseUrlWithSlash);

        // Then
        assertThat(result.getShortUrl()).isEqualTo("https://tiny.url/abc123");
        // Should not have double slashes after the protocol (no "//" between base URL and short code)
        // Replace protocol separator temporarily to check for double slashes in the path part
        String urlWithoutProtocol = result.getShortUrl().replaceFirst("https?://", "");
        assertThat(urlWithoutProtocol).doesNotContain("//");
    }

    @Test
    @DisplayName("createShortUrl - Base URL without trailing slash")
    void createShortUrl_BaseUrlWithoutTrailingSlash_WorksCorrectly() {
        // Given
        when(urlMappingRepository.findByOriginalUrl(originalUrl))
                .thenReturn(Optional.empty());
        when(urlCodeGenerator.generateUniqueCode())
                .thenReturn(shortCode);
        when(urlMappingRepository.save(any(UrlMapping.class)))
                .thenReturn(existingMapping);

        // When
        CreateUrlResult result = createUrlService.createShortUrl(originalUrl, baseUrl);

        // Then
        assertThat(result.getShortUrl()).isEqualTo("https://tiny.url/abc123");
    }

    @Test
    @DisplayName("createShortUrl - Multiple calls with same URL returns same code")
    void createShortUrl_MultipleCallsWithSameUrl_ReturnsSameCode() {
        // Given
        when(urlMappingRepository.findByOriginalUrl(originalUrl))
                .thenReturn(Optional.of(existingMapping));

        // When
        CreateUrlResult result1 = createUrlService.createShortUrl(originalUrl, baseUrl);
        CreateUrlResult result2 = createUrlService.createShortUrl(originalUrl, baseUrl);

        // Then
        assertThat(result1.getShortCode()).isEqualTo(result2.getShortCode());
        assertThat(result1.getShortCode()).isEqualTo(shortCode);
        verify(urlCodeGenerator, never()).generateUniqueCode();
        verify(urlMappingRepository, never()).save(any(UrlMapping.class));
    }

    @Test
    @DisplayName("createShortUrl - Different URLs return different codes")
    void createShortUrl_DifferentUrls_ReturnsDifferentCodes() {
        // Given
        String url1 = "https://www.example1.com";
        String url2 = "https://www.example2.com";
        String code1 = "code1";
        String code2 = "code2";

        when(urlMappingRepository.findByOriginalUrl(url1))
                .thenReturn(Optional.empty());
        when(urlMappingRepository.findByOriginalUrl(url2))
                .thenReturn(Optional.empty());
        when(urlCodeGenerator.generateUniqueCode())
                .thenReturn(code1)
                .thenReturn(code2);

        UrlMapping mapping1 = new UrlMapping();
        mapping1.setShortUrl(code1);
        UrlMapping mapping2 = new UrlMapping();
        mapping2.setShortUrl(code2);

        when(urlMappingRepository.save(any(UrlMapping.class)))
                .thenReturn(mapping1)
                .thenReturn(mapping2);

        // When
        CreateUrlResult result1 = createUrlService.createShortUrl(url1, baseUrl);
        CreateUrlResult result2 = createUrlService.createShortUrl(url2, baseUrl);

        // Then
        assertThat(result1.getShortCode()).isNotEqualTo(result2.getShortCode());
        assertThat(result1.getShortCode()).isEqualTo(code1);
        assertThat(result2.getShortCode()).isEqualTo(code2);
    }
}

