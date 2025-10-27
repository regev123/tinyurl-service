package com.tinyurl.service;

import com.tinyurl.cache.SimpleCache;
import com.tinyurl.dto.CreateUrlResult;
import com.tinyurl.dto.UrlLookupResult;
import com.tinyurl.entity.UrlMapping;
import com.tinyurl.exception.UrlExpiredException;
import com.tinyurl.exception.UrlNotFoundException;
import com.tinyurl.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShorteningServiceTest {

    @Mock
    private UrlMappingRepository urlMappingRepository;

    @Mock
    private SimpleCache<String, String> urlCache;

    @Mock
    private UrlCodeGenerator urlCodeGenerator;

    @InjectMocks
    private UrlShorteningService urlShorteningService;

    private UrlMapping existingMapping;
    private String originalUrl = "https://www.example.com";
    private String shortCode = "abc123";

    @BeforeEach
    void setUp() {
        existingMapping = new UrlMapping();
        existingMapping.setId(1L);
        existingMapping.setOriginalUrl(originalUrl);
        existingMapping.setShortUrl(shortCode);
        existingMapping.setCreatedAt(LocalDateTime.now());
        existingMapping.setExpiresAt(LocalDateTime.now().plusYears(1));
        existingMapping.setAccessCount(0L);
    }

    @Test
    void testCreateShortUrl_NewUrl() {
        // Given
        when(urlMappingRepository.findByOriginalUrl(originalUrl))
            .thenReturn(Optional.empty());
        when(urlCodeGenerator.generateUniqueCode()).thenReturn(shortCode);
        when(urlMappingRepository.save(any(UrlMapping.class)))
            .thenReturn(existingMapping);

        // When
        CreateUrlResult result = urlShorteningService.createShortUrl(originalUrl, "http://localhost:8080");

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(originalUrl, result.getOriginalUrl());
        assertEquals("http://localhost:8080/abc123", result.getShortUrl());
        verify(urlMappingRepository, times(1)).save(any(UrlMapping.class));
        verify(urlCache, times(1)).put(eq(shortCode), eq(originalUrl));
    }

    @Test
    void testCreateShortUrl_ExistingUrl() {
        // Given
        when(urlMappingRepository.findByOriginalUrl(originalUrl))
            .thenReturn(Optional.of(existingMapping));

        // When
        CreateUrlResult result = urlShorteningService.createShortUrl(originalUrl, "http://localhost:8080");

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(originalUrl, result.getOriginalUrl());
        verify(urlMappingRepository, never()).save(any(UrlMapping.class));
        verify(urlCache, times(1)).put(eq(shortCode), eq(originalUrl));
    }

    @Test
    void testGetOriginalUrl_FromCache() {
        // Given
        when(urlCache.get(shortCode)).thenReturn(originalUrl);

        // When
        String result = urlShorteningService.getOriginalUrl(shortCode);

        // Then
        assertEquals(originalUrl, result);
        verify(urlCache, times(1)).get(shortCode);
        verify(urlMappingRepository, never()).findByShortUrl(anyString());
    }

    @Test
    void testGetOriginalUrl_FromDatabase() {
        // Given
        when(urlCache.get(shortCode)).thenReturn(null);
        when(urlMappingRepository.findByShortUrl(shortCode))
            .thenReturn(Optional.of(existingMapping));

        // When
        String result = urlShorteningService.getOriginalUrl(shortCode);

        // Then
        assertEquals(originalUrl, result);
        verify(urlCache, times(1)).put(eq(shortCode), eq(originalUrl));
        verify(urlMappingRepository, times(1)).save(existingMapping);
    }

    @Test
    void testGetOriginalUrl_Expired() {
        // Given
        existingMapping.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(urlCache.get(shortCode)).thenReturn(null);
        when(urlMappingRepository.findByShortUrl(shortCode))
            .thenReturn(Optional.of(existingMapping));

        // When & Then
        assertThrows(UrlExpiredException.class, () -> 
            urlShorteningService.getOriginalUrl(shortCode));
    }

    @Test
    void testGetOriginalUrl_NotFound() {
        // Given
        when(urlCache.get(shortCode)).thenReturn(null);
        when(urlMappingRepository.findByShortUrl(shortCode))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(UrlNotFoundException.class, () -> 
            urlShorteningService.getOriginalUrl(shortCode));
    }

    @Test
    void testLookupUrl_Success() {
        // Given
        when(urlCache.get(shortCode)).thenReturn(originalUrl);

        // When
        UrlLookupResult result = urlShorteningService.lookupUrl(shortCode);

        // Then
        assertNotNull(result);
        assertTrue(result.isFound());
        assertEquals(originalUrl, result.getOriginalUrl());
    }

    @Test
    void testLookupUrl_NotFound() {
        // Given
        when(urlCache.get(shortCode)).thenReturn(null);
        when(urlMappingRepository.findByShortUrl(shortCode))
            .thenReturn(Optional.empty());

        // When
        UrlLookupResult result = urlShorteningService.lookupUrl(shortCode);

        // Then
        assertNotNull(result);
        assertFalse(result.isFound());
        assertEquals("Short URL not found", result.getMessage());
    }

    @Test
    void testLookupUrl_Expired() {
        // Given
        existingMapping.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(urlCache.get(shortCode)).thenReturn(null);
        when(urlMappingRepository.findByShortUrl(shortCode))
            .thenReturn(Optional.of(existingMapping));

        // When
        UrlLookupResult result = urlShorteningService.lookupUrl(shortCode);

        // Then
        assertNotNull(result);
        assertFalse(result.isFound());
        assertEquals("Short URL has expired", result.getMessage());
    }
}

