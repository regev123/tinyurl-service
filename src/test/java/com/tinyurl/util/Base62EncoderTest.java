package com.tinyurl.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {

    @Test
    void testEncode_Zero() {
        // When
        String result = Base62Encoder.encode(0);

        // Then
        assertEquals("0", result);
    }

    @Test
    void testEncode_SingleDigit() {
        // When
        String result = Base62Encoder.encode(10);

        // Then
        assertEquals("a", result);
    }

    @Test
    void testEncode_TwoDigit() {
        // When
        String result = Base62Encoder.encode(62);

        // Then
        assertEquals("10", result);
    }

    @Test
    void testEncode_LargeNumber() {
        // When
        String result = Base62Encoder.encode(916132832L);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testEncode_RandomNumber() {
        // Given
        long[] testNumbers = {1L, 61L, 123L, 1000L, 100000L};

        // When & Then
        for (long num : testNumbers) {
            String encoded = Base62Encoder.encode(num);
            assertNotNull(encoded);
            assertFalse(encoded.isEmpty());
        }
    }

    @Test
    void testEncode_Idempotency() {
        // Given
        long number = 123456L;

        // When
        String encoded1 = Base62Encoder.encode(number);
        String encoded2 = Base62Encoder.encode(number);

        // Then
        assertEquals(encoded1, encoded2);
    }

    @Test
    void testEncode_NoLeadingZeros() {
        // When
        String result = Base62Encoder.encode(62);

        // Then - Should not have unnecessary leading zeros
        assertTrue(result.startsWith("1"));
    }

    @Test
    void testEncode_MaxValue() {
        // When
        String result = Base62Encoder.encode(56800235583L);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.length() >= 6);
    }
}

