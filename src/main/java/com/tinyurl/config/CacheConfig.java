package com.tinyurl.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuration for Redis cache
 * Follows Single Responsibility Principle - only handles cache configuration
 * Follows Dependency Inversion Principle - provides RedisTemplate abstraction
 */
@Configuration
public class CacheConfig {
    
    // Default Redis configuration
    private static final String DEFAULT_REDIS_HOST = "localhost";
    private static final int DEFAULT_REDIS_PORT = 6379;
    
    @Value("${spring.data.redis.host:" + DEFAULT_REDIS_HOST + "}")
    private String redisHost;
    
    @Value("${spring.data.redis.port:" + DEFAULT_REDIS_PORT + "}")
    private int redisPort;
    
    @Value("${spring.data.redis.username:}")
    private String redisUsername;
    
    @Value("${spring.data.redis.password:}")
    private String redisPassword;
    
    /**
     * Creates Redis connection factory
     * Handles optional username/password configuration
     * 
     * @return configured Redis connection factory
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        
        // Only set username if it's not empty (for Redis ACL)
        if (isNotEmpty(redisUsername)) {
            config.setUsername(redisUsername);
        }
        
        // Only set password if it's not empty
        if (isNotEmpty(redisPassword)) {
            config.setPassword(redisPassword);
        }
        
        return new LettuceConnectionFactory(config);
    }
    
    /**
     * Checks if a string is not empty
     * 
     * @param value the string to check
     * @return true if not null and not empty after trimming
     */
    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
    
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
