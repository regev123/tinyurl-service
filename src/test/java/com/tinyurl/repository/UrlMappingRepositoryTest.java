package com.tinyurl.repository;

import com.tinyurl.entity.UrlMapping;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UrlMappingRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @Test
    void testFindByShortUrl_Exists() {
        // Given
        UrlMapping mapping = createTestMapping("https://example.com", "abc123");
        entityManager.persistAndFlush(mapping);

        // When
        Optional<UrlMapping> result = urlMappingRepository.findByShortUrl("abc123");

        // Then
        assertTrue(result.isPresent());
        assertEquals("https://example.com", result.get().getOriginalUrl());
    }

    @Test
    void testFindByShortUrl_NotExists() {
        // When
        Optional<UrlMapping> result = urlMappingRepository.findByShortUrl("nonexistent");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testFindByOriginalUrl_Exists() {
        // Given
        UrlMapping mapping = createTestMapping("https://example.com", "abc123");
        entityManager.persistAndFlush(mapping);

        // When
        Optional<UrlMapping> result = urlMappingRepository.findByOriginalUrl("https://example.com");

        // Then
        assertTrue(result.isPresent());
        assertEquals("abc123", result.get().getShortUrl());
    }

    @Test
    void testExistsByShortUrl_True() {
        // Given
        UrlMapping mapping = createTestMapping("https://example.com", "abc123");
        entityManager.persistAndFlush(mapping);

        // When
        boolean exists = urlMappingRepository.existsByShortUrl("abc123");

        // Then
        assertTrue(exists);
    }

    @Test
    void testExistsByShortUrl_False() {
        // When
        boolean exists = urlMappingRepository.existsByShortUrl("nonexistent");

        // Then
        assertFalse(exists);
    }

    @Test
    void testSave() {
        // Given
        UrlMapping mapping = createTestMapping("https://newurl.com", "xyz789");

        // When
        UrlMapping saved = urlMappingRepository.save(mapping);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertNotNull(saved.getId());
        Optional<UrlMapping> retrieved = urlMappingRepository.findByShortUrl("xyz789");
        assertTrue(retrieved.isPresent());
    }

    @Test
    void testUniqueConstraint() {
        // Given
        UrlMapping mapping1 = createTestMapping("https://example1.com", "duplicate");
        UrlMapping mapping2 = createTestMapping("https://example2.com", "duplicate");

        // When
        entityManager.persistAndFlush(mapping1);

        // Then
        assertThrows(Exception.class, () -> {
            urlMappingRepository.save(mapping2);
            entityManager.flush();
        });
    }

    private UrlMapping createTestMapping(String originalUrl, String shortUrl) {
        UrlMapping mapping = new UrlMapping();
        mapping.setOriginalUrl(originalUrl);
        mapping.setShortUrl(shortUrl);
        mapping.setCreatedAt(LocalDateTime.now());
        mapping.setExpiresAt(LocalDateTime.now().plusYears(1));
        mapping.setAccessCount(0L);
        return mapping;
    }
}

