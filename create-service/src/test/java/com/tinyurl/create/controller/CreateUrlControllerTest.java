package com.tinyurl.create.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyurl.create.dto.CreateUrlRequest;
import com.tinyurl.create.dto.CreateUrlResult;
import com.tinyurl.create.service.CreateUrlService;
import com.tinyurl.create.service.RequestContextExtractor;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = {CreateUrlControllerTest.TestConfig.class, CreateUrlController.class}
)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
})
@DisplayName("CreateUrlController Tests")
class CreateUrlControllerTest {

    @Configuration
    @ComponentScan(
            basePackages = {"com.tinyurl.create.controller"},
            excludeFilters = @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = com.tinyurl.create.CreateServiceApplication.class
            )
    )
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration(exclude = {
            org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
            org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
            org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration.class,
            org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
    })
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
            return new com.fasterxml.jackson.databind.ObjectMapper();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateUrlService createUrlService;

    @MockBean
    private RequestContextExtractor requestContextExtractor;

    private CreateUrlRequest validRequest;
    private CreateUrlResult successResult;

    @BeforeEach
    void setUp() {
        validRequest = CreateUrlRequest.builder()
                .originalUrl("https://www.example.com")
                .baseUrl("https://tiny.url")
                .build();

        successResult = CreateUrlResult.builder()
                .originalUrl("https://www.example.com")
                .shortUrl("https://tiny.url/abc123")
                .shortCode("abc123")
                .success(true)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/create/shorten - Success with baseUrl in request")
    void createShortUrl_WithBaseUrlInRequest_ReturnsCreated() throws Exception {
        // Given
        when(createUrlService.createShortUrl(anyString(), anyString()))
                .thenReturn(successResult);

        // When & Then
        mockMvc.perform(post("/api/v1/create/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.originalUrl").value("https://www.example.com"))
                .andExpect(jsonPath("$.shortUrl").value("https://tiny.url/abc123"))
                .andExpect(jsonPath("$.shortCode").value("abc123"));

        verify(createUrlService).createShortUrl("https://www.example.com", "https://tiny.url");
        verify(requestContextExtractor, never()).extractBaseUrl(any());
    }

    @Test
    @DisplayName("POST /api/v1/create/shorten - Success without baseUrl, extracts from request")
    void createShortUrl_WithoutBaseUrl_ExtractsFromRequest() throws Exception {
        // Given
        CreateUrlRequest requestWithoutBaseUrl = CreateUrlRequest.builder()
                .originalUrl("https://www.example.com")
                .build();

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getScheme()).thenReturn("https");
        when(mockRequest.getServerName()).thenReturn("tiny.url");
        when(mockRequest.getServerPort()).thenReturn(443);

        when(requestContextExtractor.extractBaseUrl(any(HttpServletRequest.class)))
                .thenReturn("https://tiny.url");
        when(createUrlService.createShortUrl(anyString(), anyString()))
                .thenReturn(successResult);

        // When & Then
        mockMvc.perform(post("/api/v1/create/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithoutBaseUrl)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        verify(requestContextExtractor).extractBaseUrl(any(HttpServletRequest.class));
        verify(createUrlService).createShortUrl("https://www.example.com", "https://tiny.url");
    }

    @Test
    @DisplayName("POST /api/v1/create/shorten - Success with empty baseUrl, extracts from request")
    void createShortUrl_WithEmptyBaseUrl_ExtractsFromRequest() throws Exception {
        // Given
        CreateUrlRequest requestWithEmptyBaseUrl = CreateUrlRequest.builder()
                .originalUrl("https://www.example.com")
                .baseUrl("")
                .build();

        when(requestContextExtractor.extractBaseUrl(any(HttpServletRequest.class)))
                .thenReturn("https://tiny.url");
        when(createUrlService.createShortUrl(anyString(), anyString()))
                .thenReturn(successResult);

        // When & Then
        mockMvc.perform(post("/api/v1/create/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithEmptyBaseUrl)))
                .andExpect(status().isCreated());

        verify(requestContextExtractor).extractBaseUrl(any(HttpServletRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/create/shorten - Service failure returns 500")
    void createShortUrl_ServiceFailure_ReturnsInternalServerError() throws Exception {
        // Given
        CreateUrlResult failureResult = CreateUrlResult.builder()
                .success(false)
                .errorCode(com.tinyurl.constants.ErrorCode.INTERNAL_SERVER_ERROR)
                .build();

        when(createUrlService.createShortUrl(anyString(), anyString()))
                .thenReturn(failureResult);

        // When & Then
        mockMvc.perform(post("/api/v1/create/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/create/shorten - Invalid request body returns 400")
    void createShortUrl_InvalidRequestBody_ReturnsBadRequest() throws Exception {
        // Given - missing required originalUrl
        CreateUrlRequest invalidRequest = CreateUrlRequest.builder()
                .baseUrl("https://tiny.url")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/create/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(createUrlService, never()).createShortUrl(anyString(), anyString());
    }

    @Test
    @DisplayName("POST /api/v1/create/shorten - Null originalUrl returns 400")
    void createShortUrl_NullOriginalUrl_ReturnsBadRequest() throws Exception {
        // Given
        CreateUrlRequest requestWithNullUrl = CreateUrlRequest.builder()
                .originalUrl(null)
                .baseUrl("https://tiny.url")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/create/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithNullUrl)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/create/shorten - Empty originalUrl returns 400")
    void createShortUrl_EmptyOriginalUrl_ReturnsBadRequest() throws Exception {
        // Given
        CreateUrlRequest requestWithEmptyUrl = CreateUrlRequest.builder()
                .originalUrl("")
                .baseUrl("https://tiny.url")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/create/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithEmptyUrl)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/create/shorten - Invalid JSON returns 400")
    void createShortUrl_InvalidJson_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/create/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/create/health - Returns OK")
    void health_ReturnsOk() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/create/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Create Service is healthy"));

        verify(createUrlService, never()).createShortUrl(anyString(), anyString());
        verify(requestContextExtractor, never()).extractBaseUrl(any());
    }

    @Test
    @DisplayName("POST /api/v1/create/shorten - Validates originalUrl length")
    void createShortUrl_OriginalUrlTooLong_ReturnsBadRequest() throws Exception {
        // Given - URL exceeds MAX_ORIGINAL_URL_LENGTH (5000)
        String longUrl = "https://www.example.com/" + "a".repeat(5000);
        CreateUrlRequest requestWithLongUrl = CreateUrlRequest.builder()
                .originalUrl(longUrl)
                .baseUrl("https://tiny.url")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/create/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithLongUrl)))
                .andExpect(status().isBadRequest());
    }
}

