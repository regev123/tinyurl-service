package com.tinyurl.create.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.tinyurl.create.constants.CreateUrlConstants.MAX_ORIGINAL_URL_LENGTH;
import static com.tinyurl.create.constants.CreateUrlConstants.MAX_SHORT_CODE_LENGTH;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlValidationService Tests")
class UrlValidationServiceTest {

    @InjectMocks
    private UrlValidationService urlValidationService;

    @BeforeEach
    void setUp() {
        // No setup needed
    }

    // ========== validateOriginalUrl Tests ==========

    @Test
    @DisplayName("validateOriginalUrl - Valid HTTPS URL succeeds")
    void validateOriginalUrl_ValidHttpsUrl_Succeeds() {
        // Given
        String validUrl = "https://www.example.com";

        // When & Then - should not throw
        urlValidationService.validateOriginalUrl(validUrl);
    }

    @Test
    @DisplayName("validateOriginalUrl - Valid HTTP URL succeeds")
    void validateOriginalUrl_ValidHttpUrl_Succeeds() {
        // Given
        String validUrl = "http://www.example.com";

        // When & Then - should not throw
        urlValidationService.validateOriginalUrl(validUrl);
    }

    @Test
    @DisplayName("validateOriginalUrl - Valid URL with path succeeds")
    void validateOriginalUrl_ValidUrlWithPath_Succeeds() {
        // Given
        String validUrl = "https://www.example.com/path/to/resource?param=value";

        // When & Then - should not throw
        urlValidationService.validateOriginalUrl(validUrl);
    }

    @Test
    @DisplayName("validateOriginalUrl - Valid URL without www succeeds")
    void validateOriginalUrl_ValidUrlWithoutWww_Succeeds() {
        // Given
        String validUrl = "https://example.com";

        // When & Then - should not throw
        urlValidationService.validateOriginalUrl(validUrl);
    }

    @Test
    @DisplayName("validateOriginalUrl - Null URL throws exception")
    void validateOriginalUrl_NullUrl_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> urlValidationService.validateOriginalUrl(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Original URL cannot be null or empty");
    }

    @Test
    @DisplayName("validateOriginalUrl - Empty URL throws exception")
    void validateOriginalUrl_EmptyUrl_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> urlValidationService.validateOriginalUrl(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Original URL cannot be null or empty");
    }

    @Test
    @DisplayName("validateOriginalUrl - Whitespace-only URL throws exception")
    void validateOriginalUrl_WhitespaceOnlyUrl_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> urlValidationService.validateOriginalUrl("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Original URL cannot be null or empty");
    }

    @Test
    @DisplayName("validateOriginalUrl - URL exceeds max length throws exception")
    void validateOriginalUrl_UrlTooLong_ThrowsException() {
        // Given
        String longUrl = "https://www.example.com/" + "a".repeat(MAX_ORIGINAL_URL_LENGTH);

        // When & Then
        assertThatThrownBy(() -> urlValidationService.validateOriginalUrl(longUrl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum length");
    }

    @Test
    @DisplayName("validateOriginalUrl - Invalid protocol throws exception")
    void validateOriginalUrl_InvalidProtocol_ThrowsException() {
        // Given
        String invalidUrl = "ftp://www.example.com";

        // When & Then
        assertThatThrownBy(() -> urlValidationService.validateOriginalUrl(invalidUrl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must use http or https protocol");
    }

    @Test
    @DisplayName("validateOriginalUrl - Malformed URL throws exception")
    void validateOriginalUrl_MalformedUrl_ThrowsException() {
        // Given
        String malformedUrl = "not-a-valid-url";

        // When & Then
        assertThatThrownBy(() -> urlValidationService.validateOriginalUrl(malformedUrl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid URL format");
    }

    @Test
    @DisplayName("validateOriginalUrl - URL without protocol throws exception")
    void validateOriginalUrl_UrlWithoutProtocol_ThrowsException() {
        // Given
        String urlWithoutProtocol = "www.example.com";

        // When & Then
        assertThatThrownBy(() -> urlValidationService.validateOriginalUrl(urlWithoutProtocol))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ========== validateShortCode Tests ==========

    @Test
    @DisplayName("validateShortCode - Valid alphanumeric code succeeds")
    void validateShortCode_ValidAlphanumericCode_Succeeds() {
        // Given
        String validCode = "abc123";

        // When & Then - should not throw
        urlValidationService.validateShortCode(validCode);
    }

    @Test
    @DisplayName("validateShortCode - Valid code with only letters succeeds")
    void validateShortCode_ValidCodeWithOnlyLetters_Succeeds() {
        // Given
        String validCode = "abcdef";

        // When & Then - should not throw
        urlValidationService.validateShortCode(validCode);
    }

    @Test
    @DisplayName("validateShortCode - Valid code with only numbers succeeds")
    void validateShortCode_ValidCodeWithOnlyNumbers_Succeeds() {
        // Given
        String validCode = "123456";

        // When & Then - should not throw
        urlValidationService.validateShortCode(validCode);
    }

    @Test
    @DisplayName("validateShortCode - Null code throws exception")
    void validateShortCode_NullCode_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> urlValidationService.validateShortCode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Short code cannot be null or empty");
    }

    @Test
    @DisplayName("validateShortCode - Empty code throws exception")
    void validateShortCode_EmptyCode_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> urlValidationService.validateShortCode(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Short code cannot be null or empty");
    }

    @Test
    @DisplayName("validateShortCode - Whitespace-only code throws exception")
    void validateShortCode_WhitespaceOnlyCode_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> urlValidationService.validateShortCode("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Short code cannot be null or empty");
    }

    @Test
    @DisplayName("validateShortCode - Code exceeds max length throws exception")
    void validateShortCode_CodeTooLong_ThrowsException() {
        // Given
        String longCode = "a".repeat(MAX_SHORT_CODE_LENGTH + 1);

        // When & Then
        assertThatThrownBy(() -> urlValidationService.validateShortCode(longCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum length");
    }

    @Test
    @DisplayName("validateShortCode - Code with special characters throws exception")
    void validateShortCode_CodeWithSpecialCharacters_ThrowsException() {
        // Given
        String invalidCode = "abc-123";

        // When & Then
        assertThatThrownBy(() -> urlValidationService.validateShortCode(invalidCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid characters");
    }

    @Test
    @DisplayName("validateShortCode - Code with spaces throws exception")
    void validateShortCode_CodeWithSpaces_ThrowsException() {
        // Given
        String invalidCode = "abc 123";

        // When & Then
        assertThatThrownBy(() -> urlValidationService.validateShortCode(invalidCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid characters");
    }

    // ========== validateBaseUrl Tests ==========

    @Test
    @DisplayName("validateBaseUrl - Valid HTTPS base URL succeeds")
    void validateBaseUrl_ValidHttpsBaseUrl_Succeeds() {
        // Given
        String validBaseUrl = "https://tiny.url";

        // When & Then - should not throw
        urlValidationService.validateBaseUrl(validBaseUrl);
    }

    @Test
    @DisplayName("validateBaseUrl - Valid HTTP base URL succeeds")
    void validateBaseUrl_ValidHttpBaseUrl_Succeeds() {
        // Given
        String validBaseUrl = "http://tiny.url";

        // When & Then - should not throw
        urlValidationService.validateBaseUrl(validBaseUrl);
    }

    @Test
    @DisplayName("validateBaseUrl - Valid base URL with port succeeds")
    void validateBaseUrl_ValidBaseUrlWithPort_Succeeds() {
        // Given
        String validBaseUrl = "https://tiny.url:8080";

        // When & Then - should not throw
        urlValidationService.validateBaseUrl(validBaseUrl);
    }

    @Test
    @DisplayName("validateBaseUrl - Null base URL throws exception")
    void validateBaseUrl_NullBaseUrl_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> urlValidationService.validateBaseUrl(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Base URL cannot be null or empty");
    }

    @Test
    @DisplayName("validateBaseUrl - Empty base URL throws exception")
    void validateBaseUrl_EmptyBaseUrl_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> urlValidationService.validateBaseUrl(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Base URL cannot be null or empty");
    }

    @Test
    @DisplayName("validateBaseUrl - Whitespace-only base URL throws exception")
    void validateBaseUrl_WhitespaceOnlyBaseUrl_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> urlValidationService.validateBaseUrl("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Base URL cannot be null or empty");
    }

    @Test
    @DisplayName("validateBaseUrl - Invalid protocol throws exception")
    void validateBaseUrl_InvalidProtocol_ThrowsException() {
        // Given
        String invalidBaseUrl = "ftp://tiny.url";

        // When & Then
        assertThatThrownBy(() -> urlValidationService.validateBaseUrl(invalidBaseUrl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must use http or https protocol");
    }

    @Test
    @DisplayName("validateBaseUrl - Malformed base URL throws exception")
    void validateBaseUrl_MalformedBaseUrl_ThrowsException() {
        // Given
        String malformedBaseUrl = "not-a-valid-url";

        // When & Then
        assertThatThrownBy(() -> urlValidationService.validateBaseUrl(malformedBaseUrl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid base URL format");
    }

    @Test
    @DisplayName("validateBaseUrl - Base URL without protocol throws exception")
    void validateBaseUrl_BaseUrlWithoutProtocol_ThrowsException() {
        // Given
        String baseUrlWithoutProtocol = "tiny.url";

        // When & Then
        assertThatThrownBy(() -> urlValidationService.validateBaseUrl(baseUrlWithoutProtocol))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

