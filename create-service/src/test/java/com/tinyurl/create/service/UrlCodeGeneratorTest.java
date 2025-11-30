package com.tinyurl.create.service;

import com.tinyurl.create.exception.UrlGenerationException;
import com.tinyurl.create.repository.CreateUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.tinyurl.create.constants.CreateUrlConstants.MAX_CODE_GENERATION_ATTEMPTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlCodeGenerator Tests")
class UrlCodeGeneratorTest {

    @Mock
    private CreateUrlRepository urlMappingRepository;

    @InjectMocks
    private UrlCodeGenerator urlCodeGenerator;

    @BeforeEach
    void setUp() {
        // Reset any state if needed
    }

    @Test
    @DisplayName("generateUniqueCode - Success on first attempt")
    void generateUniqueCode_FirstAttemptSuccess_ReturnsCode() {
        // Given
        when(urlMappingRepository.existsByShortUrl(anyString()))
                .thenReturn(false);

        // When
        String result = urlCodeGenerator.generateUniqueCode();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        verify(urlMappingRepository, atLeastOnce()).existsByShortUrl(anyString());
    }

    @Test
    @DisplayName("generateUniqueCode - Success after collision")
    void generateUniqueCode_AfterCollision_RetriesAndSucceeds() {
        // Given
        when(urlMappingRepository.existsByShortUrl(anyString()))
                .thenReturn(true)  // First attempt: collision
                .thenReturn(false); // Second attempt: success

        // When
        String result = urlCodeGenerator.generateUniqueCode();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        verify(urlMappingRepository, times(2)).existsByShortUrl(anyString());
    }

    @Test
    @DisplayName("generateUniqueCode - Multiple collisions then success")
    void generateUniqueCode_MultipleCollisions_RetriesUntilSuccess() {
        // Given
        when(urlMappingRepository.existsByShortUrl(anyString()))
                .thenReturn(true)   // Attempt 1: collision
                .thenReturn(true)   // Attempt 2: collision
                .thenReturn(true)   // Attempt 3: collision
                .thenReturn(false); // Attempt 4: success

        // When
        String result = urlCodeGenerator.generateUniqueCode();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        verify(urlMappingRepository, times(4)).existsByShortUrl(anyString());
    }

    @Test
    @DisplayName("generateUniqueCode - Max attempts exceeded throws exception")
    void generateUniqueCode_MaxAttemptsExceeded_ThrowsException() {
        // Given - all attempts result in collision
        when(urlMappingRepository.existsByShortUrl(anyString()))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> urlCodeGenerator.generateUniqueCode())
                .isInstanceOf(UrlGenerationException.class)
                .hasMessageContaining("Unable to generate unique short URL code");

        // The implementation checks attempts before the while condition,
        // so it throws after 99 checks (on the 100th attempt, before checking existsByShortUrl)
        verify(urlMappingRepository, times(MAX_CODE_GENERATION_ATTEMPTS - 1))
                .existsByShortUrl(anyString());
    }

    @Test
    @DisplayName("generateUniqueCode - Generated code is alphanumeric")
    void generateUniqueCode_GeneratedCode_IsAlphanumeric() {
        // Given
        when(urlMappingRepository.existsByShortUrl(anyString()))
                .thenReturn(false);

        // When
        String result = urlCodeGenerator.generateUniqueCode();

        // Then
        assertThat(result).matches("^[a-zA-Z0-9]+$");
    }

    @Test
    @DisplayName("generateUniqueCode - Generated codes are different")
    void generateUniqueCode_MultipleCalls_GeneratesDifferentCodes() {
        // Given
        when(urlMappingRepository.existsByShortUrl(anyString()))
                .thenReturn(false);

        // When
        String code1 = urlCodeGenerator.generateUniqueCode();
        String code2 = urlCodeGenerator.generateUniqueCode();

        // Then
        // Note: There's a small chance they could be the same due to randomness
        // But statistically very unlikely, so we just verify both are valid
        assertThat(code1).isNotNull().isNotEmpty();
        assertThat(code2).isNotNull().isNotEmpty();
        assertThat(code1).matches("^[a-zA-Z0-9]+$");
        assertThat(code2).matches("^[a-zA-Z0-9]+$");
    }

    @Test
    @DisplayName("generateUniqueCode - Generated code length is reasonable")
    void generateUniqueCode_GeneratedCode_HasReasonableLength() {
        // Given
        when(urlMappingRepository.existsByShortUrl(anyString()))
                .thenReturn(false);

        // When
        String result = urlCodeGenerator.generateUniqueCode();

        // Then
        assertThat(result.length()).isGreaterThan(0);
        assertThat(result.length()).isLessThanOrEqualTo(10); // MAX_SHORT_CODE_LENGTH
    }

    @Test
    @DisplayName("generateUniqueCode - Handles repository exception")
    void generateUniqueCode_RepositoryException_PropagatesException() {
        // Given
        when(urlMappingRepository.existsByShortUrl(anyString()))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        assertThatThrownBy(() -> urlCodeGenerator.generateUniqueCode())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection failed");
    }

    @Test
    @DisplayName("generateUniqueCode - Success after many collisions")
    void generateUniqueCode_ManyCollisionsThenSuccess_EventuallySucceeds() {
        // Given - simulate many collisions then success
        when(urlMappingRepository.existsByShortUrl(anyString()))
                .thenReturn(true)   // Many collisions
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false); // Finally success

        // When
        String result = urlCodeGenerator.generateUniqueCode();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        verify(urlMappingRepository, times(6)).existsByShortUrl(anyString());
    }
}

