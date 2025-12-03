-- Database Schema Initialization Script
-- This script runs automatically on application startup (before JPA initialization)
-- Creates partitioned table structure if it doesn't exist

-- Create partitioned table if it doesn't exist
DO $$
BEGIN
    -- Check if table exists
    IF NOT EXISTS (
        SELECT FROM pg_tables 
        WHERE schemaname = 'public' 
        AND tablename = 'url_mappings'
    ) THEN
        -- Create partitioned table
        -- Note: Cannot use UNIQUE (short_url) constraint on partitioned table without partition key
        -- Uniqueness is guaranteed by Snowflake ID generation, and we use index for fast lookups
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

        -- Create indexes
        CREATE INDEX idx_url_mappings_short_url ON url_mappings(short_url);
        CREATE INDEX idx_url_mappings_original_url ON url_mappings(original_url);
        CREATE INDEX idx_url_mappings_created_date ON url_mappings(created_date);
        CREATE INDEX idx_url_mappings_expires_at ON url_mappings(expires_at);
    END IF;
END $$;

