package com.tinyurl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyurl.dto.CreateUrlRequest;
import com.tinyurl.dto.CreateUrlResult;
import com.tinyurl.dto.UrlLookupResult;
import com.tinyurl.service.RequestContextExtractor;
import com.tinyurl.service.UrlShorteningService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TinyUrlController.class)
class TinyUrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UrlShorteningService urlShorteningService;

    @MockBean
    private RequestContextExtractor requestContextExtractor;

    private CreateUrlRequest validRequest;
    private CreateUrlResult successResult;
    private UrlLookupResult lookupResult;

    @BeforeEach
    void setUp() {
        validRequest = new CreateUrlRequest();
        validRequest.setOriginalUrl("https://www.example.com");
        validRequest.setBaseUrl("http://localhost:8080");

        successResult = CreateUrlResult.builder()
            .originalUrl("https://www.example.com")
            .shortUrl("http://localhost:8080/abc123")
            .shortCode("abc123")
            .success(true)
            .build();

        lookupResult = UrlLookupResult.builder()
            .shortUrl("abc123")
            .originalUrl("https://www.example.com")
            .found(true)
            .build();
    }

    @Test
    void testCreateShortUrl_Success() throws Exception {
        // Given
        when(urlShorteningService.createShortUrl(anyString(), anyString()))
            .thenReturn(successResult);

        // When & Then
        mockMvc.perform(post("/api/v1/url/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.originalUrl").value("https://www.example.com"))
            .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/abc123"))
            .andExpect(jsonPath("$.shortCode").value("abc123"))
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testCreateShortUrl_AutoDetectBaseUrl() throws Exception {
        // Given
        CreateUrlRequest requestWithoutBaseUrl = new CreateUrlRequest();
        requestWithoutBaseUrl.setOriginalUrl("https://www.example.com");
        // baseUrl is null

        when(requestContextExtractor.extractBaseUrl(any(HttpServletRequest.class)))
            .thenReturn("http://localhost:8080");
        when(urlShorteningService.createShortUrl(anyString(), anyString()))
            .thenReturn(successResult);

        // When & Then
        mockMvc.perform(post("/api/v1/url/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithoutBaseUrl)))
            .andExpect(status().isCreated());
    }

    @Test
    void testCreateShortUrl_InvalidRequest() throws Exception {
        // Given
        CreateUrlRequest invalidRequest = new CreateUrlRequest();
        // originalUrl is missing

        // When & Then
        mockMvc.perform(post("/api/v1/url/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testGetOriginalUrl_Redirect() throws Exception {
        // Given
        when(urlShorteningService.lookupUrl("abc123")).thenReturn(lookupResult);

        // When & Then
        mockMvc.perform(get("/abc123"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "https://www.example.com"));
    }

    @Test
    void testGetOriginalUrl_NotFound() throws Exception {
        // Given
        UrlLookupResult notFoundResult = UrlLookupResult.builder()
            .shortUrl("abc123")
            .found(false)
            .message("Short URL not found")
            .build();
        when(urlShorteningService.lookupUrl("abc123")).thenReturn(notFoundResult);

        // When & Then
        mockMvc.perform(get("/abc123"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.found").value(false));
    }

    @Test
    void testCreateShortUrl_ServiceError() throws Exception {
        // Given
        CreateUrlResult errorResult = CreateUrlResult.builder()
            .originalUrl("https://www.example.com")
            .success(false)
            .message("Failed to create short URL")
            .build();
        when(urlShorteningService.createShortUrl(anyString(), anyString()))
            .thenReturn(errorResult);

        // When & Then
        mockMvc.perform(post("/api/v1/url/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false));
    }
}

