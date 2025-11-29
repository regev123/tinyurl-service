package com.tinyurl.util;

/**
 * Utility class for Base62 encoding/decoding
 * Follows Single Responsibility Principle - only handles Base62 encoding
 * Follows Encapsulation - static utility methods with no state
 * 
 * Base62 uses characters: 0-9, a-z, A-Z (62 characters total)
 */
public final class Base62Encoder {
    
    private static final String BASE62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = 62;
    private static final long MIN_VALUE = 0L;
    
    private Base62Encoder() {
        // Utility class - prevent instantiation
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Encodes a number to Base62 string
     * 
     * @param number the number to encode (must be >= 0)
     * @return Base62 encoded string
     * @throws IllegalArgumentException if number is negative
     */
    public static String encode(long number) {
        if (number < MIN_VALUE) {
            throw new IllegalArgumentException("Number must be non-negative, got: " + number);
        }
        
        if (number == 0) {
            return "0";
        }
        
        StringBuilder encoded = new StringBuilder();
        long remaining = number;
        
        while (remaining > 0) {
            int index = (int) (remaining % BASE);
            encoded.append(BASE62_CHARS.charAt(index));
            remaining /= BASE;
        }
        
        return encoded.reverse().toString();
    }
}
