package com.tinyurl.service;

import com.tinyurl.repository.UrlMappingRepository;
import com.tinyurl.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.tinyurl.constants.UrlConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlCodeGenerator {
    
    private final UrlMappingRepository urlMappingRepository;
    
    /**
     * Generates a unique short URL code
     * Uses random number generation with collision detection
     * 
     * @return unique short URL code
     * @throws IllegalStateException if unable to generate unique code after many attempts
     */
    public String generateUniqueCode() {
        String shortCode;
        int attempts = 0;
        int maxAttempts = 100;
        
        do {
            long randomNumber = generateRandomNumber();
            shortCode = Base62Encoder.encode(randomNumber);
            attempts++;
            
            if (attempts >= maxAttempts) {
                log.error("Failed to generate unique short code after {} attempts", maxAttempts);
                throw new IllegalStateException("Unable to generate unique short URL code. Database may be at capacity.");
            }
        } while (urlMappingRepository.existsByShortUrl(shortCode));
        
        log.debug("Generated unique short code after {} attempts: {}", attempts, shortCode);
        return shortCode;
    }
    
    private long generateRandomNumber() {
        return MIN_SHORT_URL_NUMBER + (long) (Math.random() * MAX_SHORT_URL_NUMBER);
    }
}

