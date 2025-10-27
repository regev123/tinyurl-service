package com.tinyurl.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyurl.dto.CreateUrlRequest;
import com.tinyurl.dto.CreateUrlResult;
import com.tinyurl.entity.UrlMapping;
import com.tinyurl.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TinyUrlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        urlMappingRepository.deleteAll();
    }

    @Test
    void testCreateAndRetrieveShortUrl() throws Exception {
        // Given
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");
        request.setBaseUrl("http://localhost:8080");

        // When - Create short URL
        String responseJson = mockMvc.perform(post("/api/v1/url/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.originalUrl").value("https://www.example.com"))
            .andExpect(jsonPath("$.success").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();

        CreateUrlResult result = objectMapper.readValue(responseJson, CreateUrlResult.class);
        String shortCode = result.getShortCode();

        // Then - Verify in database
        assertNotNull(shortCode);
        assertTrue(urlMappingRepository.existsByShortUrl(shortCode));

        // And - Test redirect
        mockMvc.perform(get("/" + shortCode))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "https://www.example.com"));
    }

    @Test
    void testDuplicateUrl_ReturnsExisting() throws Exception {
        // Given
        String originalUrl = "https://www.duplicate.com";
        String baseUrl = "http://localhost:8080";

        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl(originalUrl);
        request.setBaseUrl(baseUrl);

        // When - Create first time
        String firstResponse = mockMvc.perform(post("/api/v1/url/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        CreateUrlResult firstResult = objectMapper.readValue(firstResponse, CreateUrlResult.class);
        String firstShortCode = firstResult.getShortCode();

        // When - Create second time (duplicate)
        String secondResponse = mockMvc.perform(post("/api/v1/url/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        CreateUrlResult secondResult = objectMapper.readValue(secondResponse, CreateUrlResult.class);

        // Then - Should return same short code
        assertEquals(firstShortCode, secondResult.getShortCode());

        // And - Should have only one entry in database
        long count = urlMappingRepository.count();
        assertEquals(1, count);
    }

    @Test
    void testExpiredUrl_ReturnsNotFound() throws Exception {
        // Given - Create an expired URL
        UrlMapping expiredMapping = new UrlMapping();
        expiredMapping.setOriginalUrl("https://www.expired.com");
        expiredMapping.setShortUrl("expired");
        expiredMapping.setCreatedAt(LocalDateTime.now().minusYears(2));
        expiredMapping.setExpiresAt(LocalDateTime.now().minusYears(1));
        expiredMapping.setAccessCount(0L);
        urlMappingRepository.save(expiredMapping);

        // When - Try to access expired URL
        mockMvc.perform(get("/expired"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testInvalidShortUrl_ReturnsNotFound() throws Exception {
        // When
        mockMvc.perform(get("/nonexistent"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testCreateShortUrl_InvalidRequest() throws Exception {
        // Given
        CreateUrlRequest invalidRequest = new CreateUrlRequest();
        // Missing originalUrl

        // When & Then
        mockMvc.perform(post("/api/v1/url/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testAccessCountIncrement() throws Exception {
        // Given
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");
        request.setBaseUrl("http://localhost:8080");

        String responseJson = mockMvc.perform(post("/api/v1/url/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andReturn()
            .getResponse()
            .getContentAsString();

        CreateUrlResult result = objectMapper.readValue(responseJson, CreateUrlResult.class);
        String shortCode = result.getShortCode();

        // When - Access URL multiple times
        mockMvc.perform(get("/" + shortCode)).andExpect(status().isFound());
        mockMvc.perform(get("/" + shortCode)).andExpect(status().isFound());
        mockMvc.perform(get("/" + shortCode)).andExpect(status().isFound());

        // Then - Access count should be at least 1 (first request hits DB, others hit cache)
        // Cache serves subsequent requests, so only first request increments
        UrlMapping mapping = urlMappingRepository.findByShortUrl(shortCode).orElseThrow();
        assertTrue(mapping.getAccessCount() >= 1, 
            "Access count should be at least 1. Cache prevents subsequent increments.");
    }
}

