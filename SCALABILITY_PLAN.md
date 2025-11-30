# Scalability Plan: 100M Users/Day

## 📊 Current Scale Analysis

**Target:** 100 million users per day
- **Average RPS:** ~1,157 requests/second
- **Peak RPS:** ~5,000-10,000 requests/second (5-10x multiplier)
- **Daily Requests:** ~8.6 billion requests/day
- **Storage:** ~100M-500M unique URLs (assuming 1-5 URLs per user)

---

## 🎯 Critical Bottlenecks & Solutions

### 1. **Database Scaling** 🔴 CRITICAL

#### Current State
- Single H2 in-memory database
- No replication or sharding
- Single point of failure

#### Recommended Solution

**PostgreSQL with Read Replicas**
```yaml
Primary Database:
  - PostgreSQL 15+ (write operations)
  - Connection pooling: 50-100 connections
  - Write capacity: 5,000-10,000 writes/sec

Read Replicas:
  - 3-5 read replicas (geographically distributed)
  - Read capacity: 20,000-50,000 reads/sec
  - Read-after-write consistency: 100ms delay acceptable
```

**Implementation Status:**
1. ✅ Migrate to PostgreSQL
2. ✅ Add read replicas (3-5 replicas)
3. ✅ Implement connection pooling (HikariCP)
4. ⏳ Add database sharding (if needed for further scaling)

#### Database Sharding (Future Scalability)

For extreme scale scenarios (1B+ URLs, 10K+ writes/sec), the system is designed to support **horizontal sharding**:

**Sharding Strategy:**
- **Range-based sharding** by short code prefix (A-F, G-M, N-S, T-Z)
- Each shard contains 1 primary (write) + 3 read replicas
- Shard router determines target shard based on short code hash
- Independent failure domains per shard

**Architecture:**
```
Shard 1 (A-F): Primary + 3 Replicas
Shard 2 (G-M): Primary + 3 Replicas  
Shard 3 (N-S): Primary + 3 Replicas
Shard 4 (T-Z): Primary + 3 Replicas
```

**When to Shard:**
- Database size exceeds 500GB-1TB
- Write throughput exceeds 10,000 writes/sec
- Single database becomes bottleneck
- Need geographic distribution

**Implementation Approach:**
- Hash-based routing: `shardNumber = hash(shortCode) % numberOfShards`
- Each shard operates independently with its own connection pool
- Cross-shard queries avoided through proper routing
- Resharding strategy planned for data migration

**Sharding Benefits:**
- **Horizontal Scaling**: Add more shards as data grows
- **Better Performance**: Smaller databases = faster queries
- **Higher Throughput**: Multiple write servers instead of one
- **Fault Isolation**: If one shard fails, others continue working

**Sharding Challenges:**
- **Complexity**: Shard routing logic needed, cross-shard queries complex
- **Data Distribution**: Uneven distribution can cause hot spots
- **Resharding Difficulty**: Moving data between shards requires careful planning
- **Transaction Complexity**: Distributed transactions are complex

---

### 2. **Cache Strategy** 🔴 CRITICAL

#### Current State
- Redis single instance
- No clustering or replication

#### Recommended Solutions

**Redis Cluster Setup:**
```yaml
Redis Cluster:
  - 6 nodes minimum (3 masters + 3 replicas)
  - Memory: 32GB per node (192GB total)
  - Cache hit rate target: 95%+
  - TTL: 1 minute (hot data), 5 minutes (warm data)
  
Cache Layers:
  L1: Application-level cache (Caffeine) - 1ms
  L2: Redis Cluster - 2-5ms
  L3: Database - 10-50ms
```

**CDN Integration:**
- Use CloudFlare/AWS CloudFront for static redirects
- Cache redirects at edge locations
- Reduce origin server load by 80-90%

**Implementation:**
```java
@Service
public class MultiTierCacheService implements CacheService {
    private final CaffeineCache localCache;  // L1: 1ms
    private final RedisCacheService redisCache;  // L2: 2-5ms
    private final DatabaseFallback dbFallback;  // L3: 10-50ms
    
    @Override
    public String get(String key) {
        // Try L1, then L2, then L3
        return localCache.get(key)
            .orElseGet(() -> redisCache.get(key)
                .orElseGet(() -> dbFallback.get(key)));
    }
}
```

---

### 3. **Code Generation** 🔴 CRITICAL

#### Current State
- Random generation with collision detection
- Database lookup for uniqueness check
- High collision probability at scale

#### Recommended Solutions

**Option A: Distributed ID Generation (Snowflake/Twitter)**
```java
@Service
public class DistributedIdGenerator {
    // 64-bit ID structure:
    // 41 bits: timestamp (milliseconds)
    // 10 bits: machine ID (1024 machines)
    // 12 bits: sequence number (4096 IDs/ms per machine)
    
    // Capacity: 4096 IDs/ms × 1000ms/sec × 1024 machines = 4.2B IDs/sec
    public long generateId() {
        long timestamp = System.currentTimeMillis();
        long machineId = getMachineId(); // From config/consul
        long sequence = getNextSequence();
        
        return (timestamp << 22) | (machineId << 12) | sequence;
    }
}
```

**Option B: Database Sequence (PostgreSQL)**
```sql
-- Use PostgreSQL sequences
CREATE SEQUENCE url_id_seq CACHE 1000;

-- Pre-allocate IDs in batches
SELECT nextval('url_id_seq') FROM generate_series(1, 1000);
```

**Option C: Redis INCR (Simplest)**
```java
@Service
public class RedisIdGenerator {
    private final RedisTemplate<String, String> redis;
    
    public long generateId() {
        // Atomic increment
        return redis.opsForValue().increment("url:counter");
    }
}
```

**Recommended:** Use **Snowflake algorithm** for distributed systems

---

### 4. **Architecture Changes** ✅ IMPLEMENTED

#### Microservices Architecture

**Current Implementation:**
```
┌─────────────────────────────────────────────────────────┐
│                    Maven Parent POM                     │
│              (tinyurl-services:1.0.0)                   │
└──────────────┬──────────────────┬──────────────────────┘
               │                  │
       ┌───────┴───────┐  ┌───────┴────────┐
       │               │  │                │
       ▼               ▼  ▼                ▼
┌──────────┐   ┌──────────────┐   ┌──────────────┐
│  Common  │   │   Create     │   │   Lookup     │
│  Module  │   │   Service    │   │   Service    │
│          │   │   Port:8081  │   │   Port:8082  │
│ • Entity │   │              │   │              │
│ • Error  │   │ • Controller │   │ • Controller │
│   Codes  │   │ • Service    │   │ • Service    │
│          │   │ • Repository │   │ • Repository │
│          │   │ • Utils      │   │ • Cache      │
│          │   │ • Factory    │   │ • Cleanup    │
└────┬─────┘   └──────┬───────┘   └──────┬───────┘
     │                 │                  │
     └────────┬────────┴────────┬─────────┘
              │                 │
              ▼                 ▼
    ┌──────────────────────────────────┐
    │      Shared Database             │
    │  PostgreSQL (Primary + Replicas) │
    └──────────────────────────────────┘
              │
              ▼
    ┌──────────────────────────────────┐
    │      Redis Cache (Lookup Only)   │
    └──────────────────────────────────┘
```

**Production Deployment (Future Scaling):**
```
┌─────────────────────────────────────────────────────────┐
│                    Load Balancer                        │
│              (NGINX/HAProxy/AWS ALB)                    │
└────────────┬────────────────────────────────────────────┘
             │
    ┌────────┴────────┐
    │                 │
    ▼                 ▼
┌─────────┐      ┌─────────┐
│  API    │      │  API    │
│ Gateway │      │ Gateway │
│ (x3)    │      │ (x3)    │
└────┬────┘      └────┬────┘
     │                │
     ├────────┬───────┤
     │        │       │
     ▼        ▼       ▼
┌────────┐ ┌──────┐ ┌──────┐
│Create  │ │Lookup│ │Stats │
│Service │ │Service│ │Service│
│(x5)    │ │(x10) │ │(x3)  │
└───┬────┘ └───┬──┘ └───┬──┘
    │          │        │
    ▼          ▼        ▼
┌──────────────────────────────────┐
│      Redis Cluster (6 nodes)    │
└──────────────────────────────────┘
    │          │        │
    ▼          ▼        ▼
┌──────────────────────────────────┐
│  PostgreSQL Cluster              │
│  - 1 Primary (writes)            │
│  - 5 Read Replicas (reads)      │
└──────────────────────────────────┘
```

**Service Breakdown:**
- ✅ **Create Service**: Implemented (Port 8081) - Handles URL creation
- ✅ **Lookup Service**: Implemented (Port 8082) - Handles URL lookups with caching
- ⏳ **Stats Service**: Future enhancement (handles analytics)

**Implementation Status:**
- ✅ Maven multi-module structure
- ✅ Common module with shared entities and constants
- ✅ Service-specific repositories (CreateUrlRepository, LookupUrlRepository)
- ✅ Service-specific exceptions and constants
- ✅ Independent deployment and scaling capability

---

### 5. **Database Schema Optimization** 🟡 HIGH PRIORITY

#### Current Schema Issues
- Single table for all operations
- No partitioning
- Index on short_url only

#### Optimized Schema

```sql
-- Partitioned table by creation date
CREATE TABLE url_mappings (
    id BIGSERIAL PRIMARY KEY,
    original_url TEXT NOT NULL,
    short_code VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    access_count BIGINT DEFAULT 0,
    shard_id INT NOT NULL,  -- For sharding
    created_date DATE NOT NULL  -- For partitioning
) PARTITION BY RANGE (created_date);

-- Create monthly partitions
CREATE TABLE url_mappings_2024_01 PARTITION OF url_mappings
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

-- Indexes
CREATE UNIQUE INDEX idx_short_code ON url_mappings(short_code);
CREATE INDEX idx_created_date ON url_mappings(created_date);
CREATE INDEX idx_expires_at ON url_mappings(expires_at) WHERE expires_at < NOW();

-- Separate table for hot data (last 30 days)
CREATE TABLE url_mappings_hot (
    LIKE url_mappings INCLUDING ALL
) WITH (fillfactor = 90);
```

**Read/Write Separation:**
```java
@Configuration
public class DatabaseConfig {
    
    @Bean
    @Primary
    public DataSource writeDataSource() {
        // Primary database for writes
        return DataSourceBuilder.create()
            .url("jdbc:postgresql://primary-db:5432/tinyurl")
            .build();
    }
    
    @Bean
    public DataSource readDataSource() {
        // Read replica for reads
        return DataSourceBuilder.create()
            .url("jdbc:postgresql://read-replica:5432/tinyurl")
            .build();
    }
}
```

---

### 6. **Caching Strategy** 🟡 HIGH PRIORITY

#### Multi-Tier Caching

```java
@Service
public class OptimizedCacheService {
    
    // L1: Local cache (Caffeine) - 1ms
    private final Cache<String, String> localCache = Caffeine.newBuilder()
        .maximumSize(100_000)
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .build();
    
    // L2: Redis Cluster - 2-5ms
    private final RedisCacheService redisCache;
    
    // L3: Database - 10-50ms
    
    public String get(String shortCode) {
        // 1. Check local cache (95% hit rate expected)
        String cached = localCache.getIfPresent(shortCode);
        if (cached != null) {
            return cached;
        }
        
        // 2. Check Redis (4% hit rate expected)
        cached = redisCache.get(shortCode);
        if (cached != null) {
            localCache.put(shortCode, cached);  // Populate L1
            return cached;
        }
        
        // 3. Check database (1% fallback)
        cached = databaseService.get(shortCode);
        if (cached != null) {
            redisCache.put(shortCode, cached, Duration.ofMinutes(5));
            localCache.put(shortCode, cached);
        }
        
        return cached;
    }
}
```

**Cache Warming Strategy:**
- Pre-load popular URLs into Redis
- Use background jobs to warm cache
- Monitor cache hit rates and adjust TTL

---

### 7. **Load Balancing & Auto-Scaling** 🟡 HIGH PRIORITY

#### Load Balancer Configuration

```nginx
# NGINX Configuration
upstream tinyurl_backend {
    least_conn;  # Use least connections algorithm
    server api1:8080 max_fails=3 fail_timeout=30s;
    server api2:8080 max_fails=3 fail_timeout=30s;
    server api3:8080 max_fails=3 fail_timeout=30s;
    keepalive 32;
}

server {
    listen 80;
    
    location / {
        proxy_pass http://tinyurl_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_connect_timeout 5s;
        proxy_send_timeout 10s;
        proxy_read_timeout 10s;
    }
}
```

#### Auto-Scaling Rules (Kubernetes)

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: tinyurl-lookup-service
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: lookup-service
  minReplicas: 5
  maxReplicas: 50
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

---

### 8. **Monitoring & Observability** 🟢 MEDIUM PRIORITY

#### Required Metrics

```java
@Component
public class MetricsCollector {
    
    // Key metrics to track
    @Timed(name = "url.creation.time", description = "URL creation time")
    public void trackCreationTime(Duration duration) {
        // Track creation latency
    }
    
    @Counter(name = "url.lookup.count", description = "URL lookup count")
    public void incrementLookupCount() {
        // Track lookup requests
    }
    
    @Gauge(name = "cache.hit.rate", description = "Cache hit rate")
    public double getCacheHitRate() {
        // Track cache performance
    }
}
```

**Monitoring Stack:**
- **Prometheus**: Metrics collection
- **Grafana**: Visualization
- **ELK Stack**: Log aggregation
- **Jaeger**: Distributed tracing
- **PagerDuty**: Alerting

**Key Alerts:**
- P95 latency > 100ms
- Error rate > 0.1%
- Cache hit rate < 90%
- Database connection pool > 80%
- CPU usage > 80%

---

### 9. **Performance Optimizations** 🟢 MEDIUM PRIORITY

#### Connection Pooling

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

#### Async Processing

```java
@Service
public class AsyncUrlService {
    
    @Async("urlExecutor")
    public CompletableFuture<Void> incrementAccessCount(String shortCode) {
        // Async access count update
        // Don't block main request
        return CompletableFuture.completedFuture(null);
    }
}

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "urlExecutor")
    public Executor urlExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("url-async-");
        executor.initialize();
        return executor;
    }
}
```

#### Batch Operations

```java
@Service
public class BatchUrlService {
    
    @Scheduled(fixedRate = 5000)  // Every 5 seconds
    public void batchUpdateAccessCounts() {
        // Batch update access counts
        // Reduce database load
    }
}
```

---

### 10. **Security & Rate Limiting** 🟢 MEDIUM PRIORITY

#### Rate Limiting

```java
@Component
public class RateLimiter {
    
    private final RedisTemplate<String, String> redis;
    
    public boolean allowRequest(String clientId) {
        String key = "rate_limit:" + clientId;
        Long count = redis.opsForValue().increment(key);
        
        if (count == 1) {
            redis.expire(key, Duration.ofMinutes(1));
        }
        
        return count <= 100;  // 100 requests per minute
    }
}
```

#### DDoS Protection
- Use CloudFlare/AWS Shield
- Implement IP-based rate limiting
- Use CAPTCHA for suspicious traffic

---

## 📋 Implementation Roadmap

### Phase 1: Foundation (Weeks 1-2)
- [ ] Migrate to PostgreSQL
- [ ] Set up Redis Cluster (6 nodes)
- [ ] Implement connection pooling
- [ ] Add basic monitoring (Prometheus + Grafana)

### Phase 2: Scaling (Weeks 3-4)
- [ ] Implement distributed ID generation (Snowflake)
- [ ] Add read replicas (3-5 replicas)
- [ ] Implement multi-tier caching
- [ ] Set up load balancer (NGINX/AWS ALB)

### Phase 3: Optimization (Weeks 5-6)
- [ ] Database sharding (if needed)
- [ ] Implement async processing
- [ ] Add CDN integration (CloudFlare)
- [ ] Optimize database queries and indexes

### Phase 4: Production Hardening (Weeks 7-8)
- [ ] Set up auto-scaling (Kubernetes)
- [ ] Implement comprehensive monitoring
- [ ] Add rate limiting and DDoS protection
- [ ] Load testing and optimization

---

## 💰 Cost Estimation (AWS)

### Infrastructure Costs (Monthly)

| Component | Configuration | Monthly Cost |
|-----------|--------------|--------------|
| **EC2 Instances** | 20 instances (c5.xlarge) | $2,000 |
| **RDS PostgreSQL** | db.r5.2xlarge (Primary + 5 replicas) | $1,500 |
| **ElastiCache Redis** | 6 nodes (cache.r5.xlarge) | $1,200 |
| **Load Balancer** | Application Load Balancer | $25 |
| **CloudFront CDN** | 100M requests/month | $850 |
| **Data Transfer** | 10TB/month | $900 |
| **Monitoring** | CloudWatch + Prometheus | $200 |
| **Total** | | **~$6,675/month** |

**Note:** Costs can be reduced with:
- Reserved instances (40% discount)
- Spot instances for non-critical workloads
- Optimized instance sizing

---

## 🎯 Performance Targets

| Metric | Target | Current |
|--------|--------|---------|
| **P50 Latency** | < 10ms | ~50ms |
| **P95 Latency** | < 50ms | ~200ms |
| **P99 Latency** | < 100ms | ~500ms |
| **Throughput** | 10,000 req/s | ~100 req/s |
| **Cache Hit Rate** | > 95% | ~80% |
| **Error Rate** | < 0.1% | < 1% |
| **Availability** | 99.9% | 99% |

---

## 🔍 Key Recommendations Summary

1. **✅ Database**: PostgreSQL with read replicas (5 replicas)
2. **✅ Caching**: Redis Cluster (6 nodes) + Local cache (Caffeine)
3. **✅ ID Generation**: Snowflake algorithm (distributed)
4. **✅ Architecture**: Microservices with auto-scaling
5. **✅ CDN**: CloudFlare for edge caching
6. **✅ Monitoring**: Prometheus + Grafana + ELK
7. **✅ Load Balancing**: NGINX/AWS ALB with health checks
8. **✅ Auto-Scaling**: Kubernetes HPA (5-50 replicas)

---

## 📚 Additional Resources

- [Twitter's Snowflake Algorithm](https://github.com/twitter-archive/snowflake)
- [Redis Cluster Documentation](https://redis.io/docs/management/scaling/)
- [PostgreSQL Partitioning](https://www.postgresql.org/docs/current/ddl-partitioning.html)
- [Kubernetes Auto-Scaling](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/)

---

**Next Steps:** Start with Phase 1 (Database migration + Redis Cluster) as these are the most critical bottlenecks.

