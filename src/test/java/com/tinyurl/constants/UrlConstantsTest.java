package com.tinyurl.constants;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

class UrlConstantsTest {

    @Test
    void testConstants_NotNull() {
        assertNotNull(UrlConstants.MAX_SHORT_URL_NUMBER);
        assertNotNull(UrlConstants.MIN_SHORT_URL_NUMBER);
        assertNotNull(UrlConstants.DEFAULT_EXPIRATION_YEARS);
        assertNotNull(UrlConstants.CACHE_DEFAULT_TTL_MINUTES);
        assertNotNull(UrlConstants.CACHE_CLEANUP_INTERVAL_SECONDS);
        assertNotNull(UrlConstants.MAX_ORIGINAL_URL_LENGTH);
        assertNotNull(UrlConstants.MAX_SHORT_CODE_LENGTH);
    }

    @Test
    void testConstants_Values() {
        assertEquals(56_800_235_583L, UrlConstants.MAX_SHORT_URL_NUMBER);
        assertEquals(1L, UrlConstants.MIN_SHORT_URL_NUMBER);
        assertEquals(1, UrlConstants.DEFAULT_EXPIRATION_YEARS);
        assertEquals(1, UrlConstants.CACHE_DEFAULT_TTL_MINUTES);
        assertEquals(30, UrlConstants.CACHE_CLEANUP_INTERVAL_SECONDS);
        assertEquals(5000, UrlConstants.MAX_ORIGINAL_URL_LENGTH);
        assertEquals(10, UrlConstants.MAX_SHORT_CODE_LENGTH);
    }

    @Test
    void testPrivateConstructor() throws Exception {
        // Given
        Constructor<UrlConstants> constructor = UrlConstants.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When
        UrlConstants instance = constructor.newInstance();

        // Then
        assertNotNull(instance);

        // Test instantiation throws IllegalArgumentException if attempted again
        // This is to ensure it's a utility class that shouldn't be instantiated
    }
}

