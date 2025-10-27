package com.tinyurl.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SimpleCacheTest {

    @InjectMocks
    private SimpleCache<String, String> simpleCache;

    @BeforeEach
    void setUp() {
        simpleCache = new SimpleCache<>();
        simpleCache.init();
    }

    @Test
    void testPutAndGet() {
        // When
        simpleCache.put("key1", "value1");
        String result = simpleCache.get("key1");

        // Then
        assertEquals("value1", result);
    }

    @Test
    void testGet_NotFound() {
        // When
        String result = simpleCache.get("nonexistent");

        // Then
        assertNull(result);
    }

    @Test
    void testContainsKey_Exists() {
        // Given
        simpleCache.put("key1", "value1");

        // When
        boolean exists = simpleCache.containsKey("key1");

        // Then
        assertTrue(exists);
    }

    @Test
    void testContainsKey_NotExists() {
        // When
        boolean exists = simpleCache.containsKey("nonexistent");

        // Then
        assertFalse(exists);
    }

    @Test
    void testRemove() {
        // Given
        simpleCache.put("key1", "value1");

        // When
        simpleCache.remove("key1");

        // Then
        assertNull(simpleCache.get("key1"));
    }

    @Test
    void testClear() {
        // Given
        simpleCache.put("key1", "value1");
        simpleCache.put("key2", "value2");

        // When
        simpleCache.clear();

        // Then
        assertNull(simpleCache.get("key1"));
        assertNull(simpleCache.get("key2"));
        assertEquals(0, simpleCache.size());
    }

    @Test
    void testSize() {
        // Given
        simpleCache.put("key1", "value1");
        simpleCache.put("key2", "value2");

        // When
        int size = simpleCache.size();

        // Then
        assertEquals(2, size);
    }

    @Test
    void testPutWithCustomTtl() {
        // Given
        Duration shortTtl = Duration.ofSeconds(1);

        // When
        simpleCache.put("key1", "value1", shortTtl);

        // Then
        assertEquals("value1", simpleCache.get("key1"));
    }

    @Test
    void testUpdate() {
        // Given
        simpleCache.put("key1", "value1");

        // When
        simpleCache.put("key1", "updated");

        // Then
        assertEquals("updated", simpleCache.get("key1"));
        assertEquals(1, simpleCache.size());
    }
}

