package com.tinyurl.service;

import com.tinyurl.repository.UrlMappingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlCodeGeneratorTest {

    @Mock
    private UrlMappingRepository urlMappingRepository;

    @InjectMocks
    private UrlCodeGenerator urlCodeGenerator;

    @Test
    void testGenerateUniqueCode_Success() {
        // Given
        when(urlMappingRepository.existsByShortUrl(anyString()))
            .thenReturn(false);

        // When
        String code = urlCodeGenerator.generateUniqueCode();

        // Then
        assertNotNull(code);
        assertFalse(code.isEmpty());
        verify(urlMappingRepository, atLeastOnce()).existsByShortUrl(anyString());
    }

    @Test
    void testGenerateUniqueCode_WithCollision() {
        // Given - first 2 attempts have collision, 3rd is unique
        when(urlMappingRepository.existsByShortUrl(anyString()))
            .thenReturn(true, true, false);

        // When
        String code = urlCodeGenerator.generateUniqueCode();

        // Then
        assertNotNull(code);
        assertFalse(code.isEmpty());
        verify(urlMappingRepository, times(3)).existsByShortUrl(anyString());
    }

    @Test
    void testGenerateUniqueCode_CapacityExceeded() {
        // Given
        when(urlMappingRepository.existsByShortUrl(anyString()))
            .thenReturn(true); // All codes taken (simulate full database)

        // When & Then
        assertThrows(IllegalStateException.class, () -> 
            urlCodeGenerator.generateUniqueCode());
    }
}

