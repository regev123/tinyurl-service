package com.tinyurl.config;

import com.tinyurl.cache.SimpleCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {
    
    @Bean
    public SimpleCache<String, String> urlCache() {
        return new SimpleCache<>();
    }
}
