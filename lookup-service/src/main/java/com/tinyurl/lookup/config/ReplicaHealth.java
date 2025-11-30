package com.tinyurl.lookup.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the health status of a PostgreSQL replica
 * Follows Single Responsibility Principle - only holds health status data
 * Follows Encapsulation - data is properly encapsulated with getters/setters
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplicaHealth {
    
    @Builder.Default
    private boolean healthy = true;
    
    @Builder.Default
    private String reason = "Not checked yet";
    
    @Builder.Default
    private long replicationLagBytes = 0L;
    
    @Builder.Default
    private long lastChecked = 0L;
}

