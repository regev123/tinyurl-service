package com.shortify.create.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Database Partition Initializer
 * Automatically creates partitions for the partitioned table on application startup
 * 
 * This component runs after application is ready and ensures:
 * 1. Partitions are created for current month and next 12 months
 * 2. Warns if table exists but is not partitioned (requires manual migration)
 * 
 * Note: The partitioned table itself is created by schema.sql (if it doesn't exist)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabasePartitionInitializer {
    
    private final JdbcTemplate jdbcTemplate;
    private static final DateTimeFormatter PARTITION_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy_MM");
    private static volatile boolean initialized = false;
    
    /**
     * Initialize partitions after application is ready
     * This ensures database connection is available
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    public void initializePartitions() {
        if (initialized) {
            return;
        }
        
        synchronized (DatabasePartitionInitializer.class) {
            if (initialized) {
                return;
            }
            
            try {
            log.info("Initializing database partitions...");
            
            // Check if table exists
            Boolean tableExists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'url_mappings')",
                Boolean.class
            );
            
            if (!tableExists) {
                log.info("Table does not exist yet. Waiting for JPA to create it...");
                // Wait for JPA to create the table (max 5 seconds)
                for (int i = 0; i < 10; i++) {
                    Thread.sleep(500);
                    tableExists = jdbcTemplate.queryForObject(
                        "SELECT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'url_mappings')",
                        Boolean.class
                    );
                    if (Boolean.TRUE.equals(tableExists)) {
                        log.info("Table created by JPA. Proceeding with partition setup...");
                        break;
                    }
                }
            }
            
            if (Boolean.TRUE.equals(tableExists)) {
                // Check if table is partitioned
                Boolean isPartitioned = jdbcTemplate.queryForObject(
                    "SELECT relkind = 'p' FROM pg_class WHERE relname = 'url_mappings' " +
                    "AND relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public')",
                    Boolean.class
                );
                
                if (Boolean.TRUE.equals(isPartitioned)) {
                    log.info("Table is partitioned. Ensuring partitions exist...");
                    createInitialPartitions();
                } else {
                    // Table exists but is not partitioned - convert it
                    log.info("Table exists but is not partitioned. Converting to partitioned table...");
                    convertToPartitionedTable();
                    log.info("Table converted to partitioned. Creating partitions...");
                    createInitialPartitions();
                }
            } else {
                log.warn("Table still does not exist. Partitions cannot be created.");
            }
            
                log.info("Database partition initialization completed successfully.");
                initialized = true;
                
            } catch (Exception e) {
                log.error("Error initializing database partitions", e);
                // Don't fail startup - allow application to continue
                // Admin can fix partition issues manually if needed
            }
        }
    }
    
    /**
     * Converts regular table to partitioned table
     * Only works if table is empty (safe to drop and recreate)
     */
    private void convertToPartitionedTable() {
        try {
            // Check row count
            Long rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM url_mappings",
                Long.class
            );
            
            if (rowCount != null && rowCount > 0) {
                log.warn("Table contains {} rows. Cannot automatically convert to partitioned table.", rowCount);
                log.warn("Please run migration script: scripts/test-partitions/convert-to-partitioned-table.ps1");
                return;
            }
            
            log.info("Table is empty. Dropping and recreating as partitioned table...");
            
            // Drop the regular table
            jdbcTemplate.execute("DROP TABLE IF EXISTS url_mappings CASCADE");
            
            // Create partitioned table
            // Note: Cannot use UNIQUE (short_url) constraint on partitioned table without partition key
            // Uniqueness is guaranteed by Snowflake ID generation, and we use index for fast lookups
            String createTableSql = """
                CREATE TABLE url_mappings (
                    id BIGSERIAL NOT NULL,
                    original_url VARCHAR(5000) NOT NULL,
                    short_url VARCHAR(10) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    created_date DATE NOT NULL,
                    expires_at TIMESTAMP NOT NULL,
                    access_count BIGINT NOT NULL DEFAULT 0,
                    last_accessed_at TIMESTAMP,
                    shard_id INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (id, created_date)
                ) PARTITION BY RANGE (created_date);
                """;
            
            jdbcTemplate.execute(createTableSql);
            log.info("Created partitioned table: url_mappings");
            
            // Create indexes
            String createIndexesSql = """
                CREATE INDEX idx_url_mappings_short_url ON url_mappings(short_url);
                CREATE INDEX idx_url_mappings_original_url ON url_mappings(original_url);
                CREATE INDEX idx_url_mappings_created_date ON url_mappings(created_date);
                CREATE INDEX idx_url_mappings_expires_at ON url_mappings(expires_at);
                """;
            
            jdbcTemplate.execute(createIndexesSql);
            log.info("Created indexes on url_mappings");
            
        } catch (Exception e) {
            log.error("Failed to convert table to partitioned: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Creates initial partitions for current month and next 12 months
     */
    private void createInitialPartitions() {
        LocalDate today = LocalDate.now();
        
        // Create partition for current month
        createPartitionForMonth(today);
        
        // Create partitions for next 12 months
        for (int i = 1; i <= 12; i++) {
            LocalDate futureMonth = today.plusMonths(i);
            createPartitionForMonth(futureMonth);
        }
        
        log.info("Created initial partitions for current month and next 12 months");
    }
    
    /**
     * Creates a partition for a specific month if it doesn't exist
     */
    private void createPartitionForMonth(LocalDate month) {
        LocalDate startDate = month.withDayOfMonth(1);
        LocalDate endDate = startDate.plusMonths(1);
        String partitionName = "url_mappings_" + startDate.format(PARTITION_DATE_FORMAT);
        
        // Check if partition already exists
        Boolean partitionExists = jdbcTemplate.queryForObject(
            "SELECT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = ?)",
            Boolean.class,
            partitionName
        );
        
        if (Boolean.TRUE.equals(partitionExists)) {
            log.debug("Partition already exists: {}", partitionName);
            return;
        }
        
        String createPartitionSql = String.format(
            "CREATE TABLE IF NOT EXISTS %s PARTITION OF url_mappings FOR VALUES FROM ('%s') TO ('%s')",
            partitionName,
            startDate,
            endDate
        );
        
        try {
            jdbcTemplate.execute(createPartitionSql);
            log.info("Created partition: {} (from {} to {})", partitionName, startDate, endDate);
        } catch (Exception e) {
            log.warn("Failed to create partition {}: {}", partitionName, e.getMessage());
        }
    }
}

