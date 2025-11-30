package com.tinyurl.create.service;

import com.tinyurl.create.exception.UrlGenerationException;
import com.tinyurl.create.repository.CreateUrlRepository;
import com.tinyurl.create.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.tinyurl.create.constants.CreateUrlConstants.*;

/**
 * Service for generating unique short URL codes
 * Follows Single Responsibility Principle - only handles code generation
 * Follows Dependency Inversion Principle - depends on repository abstraction
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UrlCodeGenerator {
    
    private final CreateUrlRepository urlMappingRepository;
    
    /**
     * Generates a unique short URL code
     * Uses random number generation with collision detection
     * 
     * @return unique short URL code
     * @throws UrlGenerationException if unable to generate unique code after many attempts
     */
    public String generateUniqueCode() {
        String shortCode;
        int attempts = 0;
        
        do {
            long randomNumber = generateRandomNumber();
            shortCode = Base62Encoder.encode(randomNumber);
            attempts++;
            
            if (attempts >= MAX_CODE_GENERATION_ATTEMPTS) {
                log.error("Failed to generate unique short code after {} attempts", MAX_CODE_GENERATION_ATTEMPTS);
                throw new UrlGenerationException(
                    "Unable to generate unique short URL code. Database may be at capacity."
                );
            }
        } while (urlMappingRepository.existsByShortUrl(shortCode));
        
        return shortCode;
    }
    
    /**
     * Generates a random number within the valid range for short URLs
     * Uses Math.random() which is sufficient for this use case (not security-critical)
     * 
     * @return random number between MIN_SHORT_URL_NUMBER and MAX_SHORT_URL_NUMBER
     */
    private long generateRandomNumber() {
        // Math.random() returns [0.0, 1.0), so we multiply by MAX_SHORT_URL_NUMBER
        // and add MIN_SHORT_URL_NUMBER to get range [MIN, MAX)
        return MIN_SHORT_URL_NUMBER + (long) (Math.random() * (MAX_SHORT_URL_NUMBER - MIN_SHORT_URL_NUMBER + 1));
    }
}

