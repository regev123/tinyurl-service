package com.tinyurl.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static com.tinyurl.constants.UrlConstants.CACHE_DEFAULT_TTL_MINUTES;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RedisCacheTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "url:";
    private static final String TEST_KEY = CACHE_KEY_PREFIX + "test123";
    private static final String TEST_VALUE = "https://www.example.com";

    @BeforeEach
    void setUp() {
        // Clean up test keys before each test
        redisTemplate.delete(TEST_KEY);
    }

    @Test
    void testPutAndGet() {
        // When
        Duration ttl = Duration.ofMinutes(CACHE_DEFAULT_TTL_MINUTES);
        redisTemplate.opsForValue().set(TEST_KEY, TEST_VALUE, ttl);
        String result = redisTemplate.opsForValue().get(TEST_KEY);

        // Then
        assertNotNull(result);
        assertEquals(TEST_VALUE, result);
    }

    @Test
    void testGet_NotFound() {
        // When
        String result = redisTemplate.opsForValue().get(TEST_KEY);

        // Then
        assertNull(result);
    }

    @Test
    void testPutWithCustomTtl() {
        // Given
        Duration shortTtl = Duration.ofSeconds(2);

        // When
        redisTemplate.opsForValue().set(TEST_KEY, TEST_VALUE, shortTtl);
        String result = redisTemplate.opsForValue().get(TEST_KEY);

        // Then
        assertNotNull(result);
        assertEquals(TEST_VALUE, result);

        // Wait for expiration
        try {
            Thread.sleep(2100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify expired
        String expiredResult = redisTemplate.opsForValue().get(TEST_KEY);
        assertNull(expiredResult, "Value should be expired after TTL");
    }

    @Test
    void testUpdate() {
        // Given
        Duration ttl = Duration.ofMinutes(CACHE_DEFAULT_TTL_MINUTES);
        redisTemplate.opsForValue().set(TEST_KEY, TEST_VALUE, ttl);

        // When
        String updatedValue = "https://www.updated.com";
        redisTemplate.opsForValue().set(TEST_KEY, updatedValue, ttl);

        // Then
        String result = redisTemplate.opsForValue().get(TEST_KEY);
        assertEquals(updatedValue, result);
    }

    @Test
    void testDelete() {
        // Given
        Duration ttl = Duration.ofMinutes(CACHE_DEFAULT_TTL_MINUTES);
        redisTemplate.opsForValue().set(TEST_KEY, TEST_VALUE, ttl);

        // When
        redisTemplate.delete(TEST_KEY);

        // Then
        String result = redisTemplate.opsForValue().get(TEST_KEY);
        assertNull(result);
    }

    @Test
    void testMultipleKeys() {
        // Given
        String key1 = CACHE_KEY_PREFIX + "key1";
        String key2 = CACHE_KEY_PREFIX + "key2";
        String value1 = "https://www.example1.com";
        String value2 = "https://www.example2.com";
        Duration ttl = Duration.ofMinutes(CACHE_DEFAULT_TTL_MINUTES);

        // When
        redisTemplate.opsForValue().set(key1, value1, ttl);
        redisTemplate.opsForValue().set(key2, value2, ttl);

        // Then
        assertEquals(value1, redisTemplate.opsForValue().get(key1));
        assertEquals(value2, redisTemplate.opsForValue().get(key2));

        // Cleanup
        redisTemplate.delete(key1);
        redisTemplate.delete(key2);
    }
}

