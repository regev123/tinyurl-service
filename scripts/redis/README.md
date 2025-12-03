# Redis Setup Guide

This directory contains two Redis setup options:

## Option 1: Single Redis Instance (Standalone)

**Best for:** Development, testing, simple deployments

**Files:**
- `docker-compose-redis.yml` - Docker Compose configuration
- `start-redis.ps1` - PowerShell startup script

**Usage:**
```powershell
cd scripts\redis
.\start-redis.ps1
```

**Connection Details:**
- Host: `localhost`
- Port: `7001`
- Database: `0` (default)

**RedisInsight GUI:**
- Web UI: `http://localhost:8086`
- To connect:
  1. Open `http://localhost:8086` in your browser
  2. Click "Add Redis Database"
  3. Enter:
     - **Host:** `shortify-redis` (or `localhost` if connecting from host)
     - **Port:** `6379` (or `7001` if connecting from host)
     - **Database Alias:** `Shortify Redis` (optional)
     - **Database Index:** `0` (default)
  4. Click "Add Redis Database"

**Spring Boot Configuration:**
Use standalone mode in `application.yml`:
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 7001
      database: 0
```

## Option 2: Redis Cluster (3 Masters + 3 Replicas)

**Best for:** Production, high availability, horizontal scaling

**Files:**
- `docker-compose-redis-cluster.yml` - Docker Compose configuration
- `start-redis-cluster.ps1` - PowerShell startup script

**Usage:**
```powershell
cd scripts\redis
.\start-redis-cluster.ps1
```

**Connection Details:**
- Cluster Nodes: `localhost:7001`, `localhost:7002`, `localhost:7003`, `localhost:7004`, `localhost:7005`, `localhost:7006`
- RedisInsight GUI: `http://localhost:8086`
- To connect RedisInsight to cluster:
  1. Open `http://localhost:8086` in your browser
  2. Click "Add Redis Database"
  3. Select **"Cluster"** mode (toggle at top)
  4. Enter:
     - **Host:** `shortify-redis-node-1` (or `localhost` if connecting from host)
     - **Port:** `6379` (or `7001` if connecting from host)
     - **Database Alias:** `Shortify Redis Cluster` (optional)
  5. Click "Add Redis Database"
  6. RedisInsight will automatically discover all 6 cluster nodes

**Spring Boot Configuration:**
Use cluster mode in `application.yml`:
```yaml
spring:
  data:
    redis:
      cluster:
        nodes: localhost:7001,localhost:7002,localhost:7003
        max-redirects: 3
        refresh:
          adaptive: true
          period: 30s
      lettuce:
        cluster:
          refresh:
            adaptive: true
            period: 30s
```

## Switching Between Modes

### To Switch from Standalone to Cluster:

1. **Stop standalone Redis:**
   ```powershell
   cd scripts\redis
   docker-compose -f docker-compose-redis.yml down
   ```

2. **Start Redis cluster:**
   ```powershell
   .\start-redis-cluster.ps1
   ```

3. **Update Spring Boot configuration:**
   - In `lookup-service/src/main/resources/application.yml`
   - In `api-gateway/src/main/resources/application.yml`
   - Comment out standalone config, uncomment cluster config

4. **Restart Spring Boot services**

### To Switch from Cluster to Standalone:

1. **Stop Redis cluster:**
   ```powershell
   cd scripts\redis
   docker-compose -f docker-compose-redis-cluster.yml down
   ```

2. **Start standalone Redis:**
   ```powershell
   .\start-redis.ps1
   ```

3. **Update Spring Boot configuration:**
   - In `lookup-service/src/main/resources/application.yml`
   - In `api-gateway/src/main/resources/application.yml`
   - Comment out cluster config, uncomment standalone config

4. **Restart Spring Boot services**

## Troubleshooting

### Cluster Replication Issues

If you see "Connection refused" errors in cluster logs:
- Ensure all nodes are running: `docker ps --filter "name=shortify-redis"`
- Check cluster status: `docker exec shortify-redis-node-1 redis-cli cluster info`
- Verify all nodes are connected: `docker exec shortify-redis-node-1 redis-cli cluster nodes`

### Spring Boot Connection Issues

If Spring Boot can't connect to cluster:
- Verify cluster is healthy: `docker exec shortify-redis-node-1 redis-cli cluster info`
- Check that cluster mode is enabled in `application.yml`
- Ensure refresh settings are configured for cluster topology discovery
- Try connecting to a single node first to verify connectivity

### Port Conflicts

If ports 7001-7006 are already in use:
- Check what's using them: `netstat -an | findstr "7001"`
- Stop conflicting services or modify port mappings in docker-compose files

## Useful Commands

**Check Redis status:**
```powershell
docker exec shortify-redis redis-cli ping
```

**Check cluster status:**
```powershell
docker exec shortify-redis-node-1 redis-cli cluster info
docker exec shortify-redis-node-1 redis-cli cluster nodes
```

**View logs:**
```powershell
# Standalone
docker-compose -f docker-compose-redis.yml logs -f

# Cluster
docker-compose -f docker-compose-redis-cluster.yml logs -f
```

**Stop and remove all data:**
```powershell
# Standalone
docker-compose -f docker-compose-redis.yml down -v

# Cluster
docker-compose -f docker-compose-redis-cluster.yml down -v
```

