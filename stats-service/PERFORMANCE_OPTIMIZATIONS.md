# Stats Service Performance Optimizations

## Overview

This document explains the performance optimizations implemented to handle **100M requests/day** with a single PostgreSQL database.

## Traffic Analysis

- **100M requests/day** = ~1,157 requests/second (average)
- **Peak traffic** = ~3,500-5,800 requests/second (3-5x multiplier)
- **Database operations** (before optimization): ~9 operations per click = **~52,200 operations/sec at peak**

## Optimizations Implemented

### 1. Batch Processing ✅

**Problem:** Processing events one-by-one creates excessive database connections and transactions.

**Solution:**
- Events are collected in memory buffers
- Flushed in batches of 100 events (configurable)
- Automatic flush every 5 seconds (configurable)
- Bulk inserts using JPA batch processing

**Impact:**
- Reduces database connections from 1 per event to 1 per 100 events
- **99% reduction** in transaction overhead

**Configuration:**
```yaml
stats:
  batch:
    size: 100                          # Batch size for bulk inserts
    flush-interval-seconds: 5          # Flush batch every 5 seconds
```

### 2. Deferred Statistics Aggregation ✅

**Problem:** Recalculating statistics on every click event (4 COUNT queries + 2 SELECT + 1 UPDATE = 7 operations per click).

**Solution:**
- Statistics are updated periodically (every 10 minutes) instead of on every click
- Click events are stored immediately (fast write)
- Aggregation runs as a scheduled background job

**Impact:**
- **Before:** 7 DB operations per click = 700M operations/day
- **After:** 1 write per click + 1 aggregation per URL per 10 minutes
- For 10M unique URLs: 10M aggregations per 10 minutes = **1.44M operations/day**
- **99.8% reduction** in statistics-related operations

**Configuration:**
```yaml
stats:
  aggregation:
    update-interval-minutes: 10        # Update aggregated stats every 10 minutes
    enabled: true                      # Enable scheduled aggregation
```

### 3. Connection Pool Optimization ✅

**Problem:** Default connection pool (20 connections) insufficient for high throughput.

**Solution:**
- Increased pool size to 50 connections
- Optimized connection lifecycle (idle timeout, max lifetime)
- Connection leak detection

**Configuration:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      idle-timeout: 600000    # 10 minutes
      max-lifetime: 1800000   # 30 minutes
```

### 4. JPA Batch Processing ✅

**Problem:** Individual INSERT statements are slow.

**Solution:**
- Hibernate batch inserts (100 events per batch)
- Ordered inserts for better performance
- Disabled unnecessary statistics generation

**Configuration:**
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 100
          order_inserts: true
          order_updates: true
```

### 5. Kafka Consumer Batching ✅

**Problem:** Processing one event at a time from Kafka.

**Solution:**
- Batch listener enabled (processes up to 500 events per poll)
- Increased concurrency (3 consumer threads)
- Optimized fetch settings

**Configuration:**
```yaml
spring:
  kafka:
    consumer:
      max-poll-records: 500
      fetch-min-size: 1024
      fetch-max-wait: 500
```

## Performance Results

### Before Optimization:
- **DB Operations:** ~9 per click event
- **Peak Load:** ~52,200 operations/sec
- **Database:** ❌ Cannot handle (would require multiple instances)

### After Optimization:
- **DB Operations:** ~0.01 per click event (1 write per 100 events)
- **Peak Load:** ~58 operations/sec (for writes)
- **Aggregation:** ~1.44M operations/day (background job)
- **Database:** ✅ Single PostgreSQL can handle easily

## Capacity Analysis

### Single PostgreSQL Instance Capacity:
- **Write Throughput:** 10,000-50,000 writes/sec (well-tuned)
- **Our Peak Load:** ~58 writes/sec (after batching)
- **Headroom:** **99.4%** available capacity

### Conclusion:
✅ **A single PostgreSQL instance is MORE than sufficient** for 100M requests/day with these optimizations.

## Future Scaling Options

If traffic grows beyond 100M/day:

1. **Read Replicas** (for analytics queries)
   - Add read replicas for `getUrlStatistics()` and `getPlatformStatistics()`
   - Reduces load on primary database

2. **Table Partitioning**
   - Partition `url_click_events` by date (monthly partitions)
   - Improves query performance and enables easy data archival

3. **Separate Aggregation Database**
   - Use a separate database for aggregated statistics
   - Primary DB: raw events (write-heavy)
   - Stats DB: aggregated data (read-heavy)

4. **Time-Series Database**
   - Consider InfluxDB or TimescaleDB for click events
   - Optimized for time-series data and analytics

## Monitoring Recommendations

Monitor these metrics:
- Batch flush rate (events/second)
- Database connection pool usage
- Statistics aggregation job duration
- Kafka consumer lag
- Database write latency

## Configuration Tuning

Adjust these values based on your traffic patterns:

```yaml
stats:
  batch:
    size: 100                    # Increase for higher throughput
    flush-interval-seconds: 5    # Decrease for lower latency
    
stats:
  aggregation:
    update-interval-minutes: 10  # Decrease for more real-time stats
```

**Trade-offs:**
- Larger batch size = higher throughput, but more memory usage
- Shorter flush interval = lower latency, but more frequent DB writes
- Shorter aggregation interval = more real-time stats, but more DB load

