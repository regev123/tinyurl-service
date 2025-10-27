package com.tinyurl.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UrlNotFoundExceptionTest {

    @Test
    void testConstructor_WithMessage() {
        // Given
        String message = "URL not found";

        // When
        UrlNotFoundException exception = new UrlNotFoundException(message);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void testConstructor_WithNullMessage() {
        // When & Then
        UrlNotFoundException exception = new UrlNotFoundException(null);

        assertNotNull(exception);
    }
}

