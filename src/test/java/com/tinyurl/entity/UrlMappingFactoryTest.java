package com.tinyurl.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UrlMappingFactoryTest {

    @Test
    void testCreate() {
        // Given
        String originalUrl = "https://www.example.com";
        String shortCode = "abc123";

        // When
        UrlMapping mapping = UrlMappingFactory.create(originalUrl, shortCode);

        // Then
        assertNotNull(mapping);
        assertEquals(originalUrl, mapping.getOriginalUrl());
        assertEquals(shortCode, mapping.getShortUrl());
        assertNotNull(mapping.getCreatedAt());
        assertNotNull(mapping.getExpiresAt());
        assertTrue(mapping.getExpiresAt().isAfter(LocalDateTime.now()));
        assertEquals(0L, mapping.getAccessCount());
    }

    @Test
    void testCreate_ExpirationIsOneYearFromNow() {
        // Given
        String originalUrl = "https://www.example.com";
        String shortCode = "abc123";

        // When
        UrlMapping mapping = UrlMappingFactory.create(originalUrl, shortCode);

        // Then
        assertNotNull(mapping.getExpiresAt());
        assertTrue(mapping.getExpiresAt().isAfter(LocalDateTime.now()));
        assertTrue(mapping.getExpiresAt().isBefore(LocalDateTime.now().plusYears(2)));
    }

    @Test
    void testCreate_AccessCountIsZero() {
        // Given
        String originalUrl = "https://www.example.com";
        String shortCode = "abc123";

        // When
        UrlMapping mapping = UrlMappingFactory.create(originalUrl, shortCode);

        // Then
        assertEquals(0L, mapping.getAccessCount());
    }

    @Test
    void testCreate_AllFieldsSet() {
        // Given
        String originalUrl = "https://www.example.com";
        String shortCode = "abc123";

        // When
        UrlMapping mapping = UrlMappingFactory.create(originalUrl, shortCode);

        // Then
        assertNotNull(mapping.getOriginalUrl());
        assertNotNull(mapping.getShortUrl());
        assertNotNull(mapping.getCreatedAt());
        assertNotNull(mapping.getExpiresAt());
        assertNotNull(mapping.getAccessCount());
    }
}

