package com.tinyurl.create.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Database configuration for read/write splitting
 * Routes read operations to replicas and write operations to primary
 * 
 * Follows Single Responsibility Principle - only handles database configuration
 * Follows Dependency Inversion Principle - depends on ReplicaHealthChecker abstraction
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DatabaseConfig {
    
    // Constants for routing keys
    private static final String READ_ROUTING_KEY = "read";
    private static final String WRITE_ROUTING_KEY = "write";
    
    private final ReplicaHealthChecker healthChecker;
    
    @Value("${spring.datasource.url}")
    private String primaryUrl;
    
    @Value("${spring.datasource.username}")
    private String username;
    
    @Value("${spring.datasource.password}")
    private String password;
    
    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;
    
    @Value("${spring.datasource.read.replicas:localhost:5434,localhost:5435,localhost:5436}")
    private String replicaUrls;
    
    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int maxPoolSize;
    
    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minIdle;
    
    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;
    
    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;
    
    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;
    
    /**
     * Primary datasource for write operations
     */
    @Bean(name = "writeDataSource")
    public DataSource writeDataSource() {
        return createDataSource(primaryUrl, "TinyUrlWritePool");
    }
    
    /**
     * Read replica datasources
     * Creates multiple datasources, one for each replica
     */
    @Bean(name = "readDataSources")
    public List<DataSource> readDataSources() {
        String[] replicaUrlArray = replicaUrls.split(",");
        List<DataSource> replicas = java.util.Arrays.stream(replicaUrlArray)
                .map(url -> url.trim())
                .map(url -> createDataSource("jdbc:postgresql://" + url + "/tinyurl", 
                        "TinyUrlReadPool-" + url))
                .toList();
        
        // Initialize health checks for replicas
        healthChecker.initializeHealthChecks(replicas);
        
        return replicas;
    }
    
    /**
     * Routing datasource that routes reads to replicas and writes to primary
     */
    @Bean(name = "routingDataSource")
    @Primary
    public DataSource routingDataSource(
            @Qualifier("writeDataSource") DataSource writeDataSource,
            @Qualifier("readDataSources") List<DataSource> readDataSources) {
        
        AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
            // Thread-safe counter for round-robin selection
            private final AtomicInteger counter = new AtomicInteger(0);
            
            @Override
            protected Object determineCurrentLookupKey() {
                // Check if current transaction is read-only
                boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
                
                if (isReadOnly && !readDataSources.isEmpty()) {
                    // Route to read replica using round-robin
                    return READ_ROUTING_KEY;
                } else {
                    // Route to primary for writes
                    return WRITE_ROUTING_KEY;
                }
            }
            
            @Override
            protected DataSource determineTargetDataSource() {
                String lookupKey = (String) determineCurrentLookupKey();
                
                if (READ_ROUTING_KEY.equals(lookupKey) && !readDataSources.isEmpty()) {
                    // Get only healthy replicas
                    List<DataSource> healthyReplicas = healthChecker.getHealthyReplicas(readDataSources);
                    
                    if (healthyReplicas.isEmpty()) {
                        // No healthy replicas - fallback to primary (better than failing)
                        log.warn("No healthy replicas available, routing to primary");
                        return writeDataSource;
                    }
                    
                    // Round-robin selection from healthy replicas only
                    int index = Math.abs(counter.getAndIncrement()) % healthyReplicas.size();
                    return healthyReplicas.get(index);
                } else {
                    return writeDataSource;
                }
            }
        };
        
        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put(WRITE_ROUTING_KEY, writeDataSource);
        // Note: We override determineTargetDataSource() to handle read routing dynamically
        
        routingDataSource.setTargetDataSources(dataSourceMap);
        routingDataSource.setDefaultTargetDataSource(writeDataSource);
        
        return routingDataSource;
    }
    
    /**
     * Creates a datasource with HikariCP connection pool configuration
     */
    private DataSource createDataSource(String url, String poolName) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setDriverClassName(driverClassName);
        hikariConfig.setMaximumPoolSize(maxPoolSize);
        hikariConfig.setMinimumIdle(minIdle);
        hikariConfig.setConnectionTimeout(connectionTimeout);
        hikariConfig.setIdleTimeout(idleTimeout);
        hikariConfig.setMaxLifetime(maxLifetime);
        hikariConfig.setPoolName(poolName);
        
        return new HikariDataSource(hikariConfig);
    }
}

