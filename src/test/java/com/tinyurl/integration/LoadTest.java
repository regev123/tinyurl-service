package com.tinyurl.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyurl.dto.CreateUrlRequest;
import com.tinyurl.dto.CreateUrlResult;
import com.tinyurl.entity.UrlMapping;
import com.tinyurl.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Load Test - Generate and Access Many Short URLs")
class LoadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final int NUMBER_OF_URLS = 100;
    private static final int ACCESSES_PER_URL = 5;
    private static final String BASE_URL = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        urlMappingRepository.deleteAll();
    }

    @Test
    @DisplayName("Generate 100 short URLs and access each 5 times")
    void testGenerateAndAccessManyUrls() throws Exception {
        // Given
        List<String> shortCodes = new ArrayList<>();
        List<String> originalUrls = new ArrayList<>();

        // Step 1: Generate many short URLs
        System.out.println("=== Step 1: Generating " + NUMBER_OF_URLS + " short URLs ===");
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < NUMBER_OF_URLS; i++) {
            String originalUrl = "https://www.example" + i + ".com/page" + i;
            originalUrls.add(originalUrl);

            CreateUrlRequest request = new CreateUrlRequest();
            request.setOriginalUrl(originalUrl);
            request.setBaseUrl(BASE_URL);

            String responseJson = mockMvc.perform(post("/api/v1/url/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

            CreateUrlResult result = objectMapper.readValue(responseJson, CreateUrlResult.class);
            String shortCode = result.getShortCode();
            shortCodes.add(shortCode);

            assertNotNull(shortCode, "Short code should not be null for URL " + i);
            assertFalse(shortCode.isEmpty(), "Short code should not be empty for URL " + i);
        }

        long generationTime = System.currentTimeMillis() - startTime;
        System.out.println("Generated " + NUMBER_OF_URLS + " URLs in " + generationTime + "ms");
        System.out.println("Average time per URL: " + (generationTime / NUMBER_OF_URLS) + "ms");

        // Verify all short codes are unique
        Set<String> uniqueCodes = new HashSet<>(shortCodes);
        assertEquals(NUMBER_OF_URLS, uniqueCodes.size(), 
            "All short codes should be unique");

        // Verify all URLs are in database
        assertEquals(NUMBER_OF_URLS, urlMappingRepository.count(),
            "All URLs should be saved in database");

        // Step 2: Access each URL multiple times
        System.out.println("\n=== Step 2: Accessing each URL " + ACCESSES_PER_URL + " times ===");
        startTime = System.currentTimeMillis();
        int totalAccesses = NUMBER_OF_URLS * ACCESSES_PER_URL;
        int successfulAccesses = 0;
        int failedAccesses = 0;

        for (int i = 0; i < NUMBER_OF_URLS; i++) {
            String shortCode = shortCodes.get(i);
            String expectedOriginalUrl = originalUrls.get(i);

            for (int j = 0; j < ACCESSES_PER_URL; j++) {
                try {
                    mockMvc.perform(get("/" + shortCode))
                        .andExpect(status().isFound())
                        .andExpect(header().string("Location", expectedOriginalUrl));
                    successfulAccesses++;
                } catch (Exception e) {
                    failedAccesses++;
                    System.err.println("Failed to access " + shortCode + ": " + e.getMessage());
                }
            }
        }

        long accessTime = System.currentTimeMillis() - startTime;
        System.out.println("Accessed " + totalAccesses + " URLs in " + accessTime + "ms");
        System.out.println("Successful accesses: " + successfulAccesses);
        System.out.println("Failed accesses: " + failedAccesses);
        System.out.println("Average time per access: " + (accessTime / totalAccesses) + "ms");

        // Verify all accesses were successful
        assertEquals(totalAccesses, successfulAccesses,
            "All URL accesses should be successful");
        assertEquals(0, failedAccesses,
            "No accesses should fail");

        // Step 3: Verify access counts
        System.out.println("\n=== Step 3: Verifying access counts ===");
        int urlsWithAccessCount = 0;
        for (String shortCode : shortCodes) {
            UrlMapping mapping = urlMappingRepository.findByShortUrl(shortCode)
                .orElseThrow(() -> new AssertionError("URL should exist: " + shortCode));
            
            // First access increments count, subsequent accesses hit cache
            assertTrue(mapping.getAccessCount() >= 1,
                "Access count should be at least 1 for " + shortCode);
            
            if (mapping.getAccessCount() > 0) {
                urlsWithAccessCount++;
            }
        }
        System.out.println("URLs with access count > 0: " + urlsWithAccessCount + "/" + NUMBER_OF_URLS);

        // Summary
        System.out.println("\n=== Test Summary ===");
        System.out.println("Total URLs generated: " + NUMBER_OF_URLS);
        System.out.println("Total accesses: " + totalAccesses);
        System.out.println("Generation time: " + generationTime + "ms");
        System.out.println("Access time: " + accessTime + "ms");
        System.out.println("Total test time: " + (generationTime + accessTime) + "ms");
    }

    @Test
    @DisplayName("Generate 1000 short URLs with concurrent access")
    void testConcurrentAccess() throws Exception {
        // Given
        int numberOfUrls = 1000;
        int concurrentThreads = 10;
        List<String> shortCodes = Collections.synchronizedList(new ArrayList<>());
        List<String> originalUrls = Collections.synchronizedList(new ArrayList<>());

        // Step 1: Generate URLs
        System.out.println("=== Generating " + numberOfUrls + " URLs concurrently ===");
        long startTime = System.currentTimeMillis();

        ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfUrls; i++) {
            final int index = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    String originalUrl = "https://www.example" + index + ".com/page" + index;
                    CreateUrlRequest request = new CreateUrlRequest();
                    request.setOriginalUrl(originalUrl);
                    request.setBaseUrl(BASE_URL);

                    String responseJson = mockMvc.perform(post("/api/v1/url/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                    CreateUrlResult result = objectMapper.readValue(responseJson, CreateUrlResult.class);
                    synchronized (shortCodes) {
                        shortCodes.add(result.getShortCode());
                        originalUrls.add(originalUrl);
                    }
                } catch (Exception e) {
                    System.err.println("Error creating URL " + index + ": " + e.getMessage());
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all URLs to be created
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        long generationTime = System.currentTimeMillis() - startTime;
        System.out.println("Generated " + shortCodes.size() + " URLs in " + generationTime + "ms");

        // Verify uniqueness
        Set<String> uniqueCodes = new HashSet<>(shortCodes);
        assertEquals(shortCodes.size(), uniqueCodes.size(),
            "All short codes should be unique");

        // Step 2: Concurrent access
        System.out.println("\n=== Concurrently accessing URLs ===");
        startTime = System.currentTimeMillis();
        ExecutorService accessExecutor = Executors.newFixedThreadPool(concurrentThreads);
        List<CompletableFuture<Integer>> accessFutures = new ArrayList<>();
        final int[] successfulAccesses = {0};

        for (int i = 0; i < shortCodes.size(); i++) {
            final int index = i;
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
                try {
                    String shortCode = shortCodes.get(index);
                    String expectedUrl = originalUrls.get(index);
                    mockMvc.perform(get("/" + shortCode))
                        .andExpect(status().isFound())
                        .andExpect(header().string("Location", expectedUrl));
                    synchronized (successfulAccesses) {
                        successfulAccesses[0]++;
                    }
                    return 1;
                } catch (Exception e) {
                    System.err.println("Error accessing URL " + index + ": " + e.getMessage());
                    return 0;
                }
            }, accessExecutor);
            accessFutures.add(future);
        }

        // Wait for all accesses
        CompletableFuture.allOf(accessFutures.toArray(new CompletableFuture[0])).join();
        accessExecutor.shutdown();
        accessExecutor.awaitTermination(30, TimeUnit.SECONDS);

        long accessTime = System.currentTimeMillis() - startTime;
        System.out.println("Accessed " + successfulAccesses[0] + " URLs in " + accessTime + "ms");

        assertEquals(shortCodes.size(), successfulAccesses[0],
            "All concurrent accesses should be successful");

        System.out.println("\n=== Concurrent Test Summary ===");
        System.out.println("URLs generated: " + shortCodes.size());
        System.out.println("Successful accesses: " + successfulAccesses[0]);
        System.out.println("Total time: " + (generationTime + accessTime) + "ms");
    }

    @Test
    @DisplayName("Test duplicate URL handling with many URLs")
    void testDuplicateUrlHandling() throws Exception {
        // Given
        int numberOfUniqueUrls = 50;
        int duplicatesPerUrl = 3;
        Set<String> uniqueShortCodes = new HashSet<>();

        System.out.println("=== Testing duplicate URL handling ===");
        long startTime = System.currentTimeMillis();

        // Create URLs with duplicates
        for (int i = 0; i < numberOfUniqueUrls; i++) {
            String originalUrl = "https://www.duplicate" + i + ".com";
            String firstShortCode = null;

            for (int j = 0; j < duplicatesPerUrl; j++) {
                CreateUrlRequest request = new CreateUrlRequest();
                request.setOriginalUrl(originalUrl);
                request.setBaseUrl(BASE_URL);

                String responseJson = mockMvc.perform(post("/api/v1/url/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

                CreateUrlResult result = objectMapper.readValue(responseJson, CreateUrlResult.class);
                String shortCode = result.getShortCode();

                if (j == 0) {
                    firstShortCode = shortCode;
                    uniqueShortCodes.add(shortCode);
                } else {
                    // All duplicates should return the same short code
                    assertEquals(firstShortCode, shortCode,
                        "Duplicate URL " + originalUrl + " should return same short code");
                }
            }
        }

        long time = System.currentTimeMillis() - startTime;
        System.out.println("Created " + (numberOfUniqueUrls * duplicatesPerUrl) + 
            " requests for " + numberOfUniqueUrls + " unique URLs in " + time + "ms");

        // Verify only unique URLs are in database
        assertEquals(numberOfUniqueUrls, urlMappingRepository.count(),
            "Database should contain only unique URLs");
        assertEquals(numberOfUniqueUrls, uniqueShortCodes.size(),
            "Should have only unique short codes");

        System.out.println("Test passed: Duplicate URLs correctly handled");
    }
}

