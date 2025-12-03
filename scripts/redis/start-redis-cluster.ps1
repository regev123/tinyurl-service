# PowerShell script to start Redis Cluster (3 masters + 3 replicas) using Docker Compose
# Usage: .\start-redis-cluster.ps1 [-CleanVolumes]
#   -CleanVolumes: Remove all volumes for a completely fresh start (WARNING: This deletes all Redis data)
#
# Based on the working standalone Redis setup (docker-compose-redis.yml)

param(
    [switch]$CleanVolumes = $false
)

Write-Host "Starting Redis Cluster (3 masters + 3 replicas)..." -ForegroundColor Green

$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$composeFile = Join-Path $scriptPath "docker-compose-redis-cluster.yml"

# Check if Docker is running
try {
    docker info | Out-Null
} catch {
    Write-Host "Error: Docker is not running. Please start Docker Desktop." -ForegroundColor Red
    exit 1
}

# Stop any existing Redis containers (cluster or single)
Write-Host "Checking for existing Redis containers..." -ForegroundColor Yellow
$existingCluster = docker ps -a --filter "name=shortify-redis-node" --format "{{.Names}}"
$existingSingle = docker ps -a --filter "name=shortify-redis" --format "{{.Names}}"

if ($existingCluster -or $existingSingle) {
    Write-Host "Stopping existing Redis containers..." -ForegroundColor Yellow
    
    # Stop single Redis instance if it exists
    if ($existingSingle) {
        Write-Host "  Stopping single Redis instance..." -ForegroundColor Gray
        docker-compose -f (Join-Path $scriptPath "docker-compose-redis.yml") down 2>$null
    }
    
    # Stop cluster containers
    if ($existingCluster) {
        Write-Host "  Stopping Redis cluster containers..." -ForegroundColor Gray
        if ($CleanVolumes) {
            Write-Host "  Removing volumes for fresh start..." -ForegroundColor Yellow
            docker-compose -f $composeFile down -v 2>$null
        } else {
            docker-compose -f $composeFile down 2>$null
        }
    }
    
    Write-Host "Waiting for cleanup to complete..." -ForegroundColor Yellow
    Start-Sleep -Seconds 3
}

# Start Redis Cluster nodes
Write-Host "Starting Redis Cluster nodes..." -ForegroundColor Yellow
docker-compose -f $composeFile up -d --remove-orphans

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to start Redis containers!" -ForegroundColor Red
    exit 1
}

Write-Host "Waiting for Redis nodes to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Verify all nodes are ready before creating cluster
Write-Host "Verifying all nodes are ready..." -ForegroundColor Yellow
$allNodesReady = $true
for ($i = 1; $i -le 6; $i++) {
    $pingResult = docker exec shortify-redis-node-$i redis-cli ping 2>$null
    if ($pingResult -ne "PONG") {
        Write-Host "  [WARNING] Node $i is not ready yet, waiting..." -ForegroundColor Yellow
        $allNodesReady = $false
        Start-Sleep -Seconds 2
        break
    }
}

if (-not $allNodesReady) {
    Write-Host "Waiting additional time for nodes to be ready..." -ForegroundColor Yellow
    Start-Sleep -Seconds 5
    
    # Retry verification
    $allNodesReady = $true
    for ($i = 1; $i -le 6; $i++) {
        $pingResult = docker exec shortify-redis-node-$i redis-cli ping 2>$null
        if ($pingResult -ne "PONG") {
            Write-Host "  [ERROR] Node $i is still not responding" -ForegroundColor Red
            $allNodesReady = $false
        }
    }
    
    if (-not $allNodesReady) {
        Write-Host "Error: Some Redis nodes failed to start or are not responding." -ForegroundColor Red
        Write-Host "Check logs with: docker-compose -f $composeFile logs" -ForegroundColor Yellow
        exit 1
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Redis Cluster Nodes Started!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

# Check if cluster already exists and is healthy
Write-Host "Checking if cluster already exists..." -ForegroundColor Yellow
$existingClusterInfo = docker exec shortify-redis-node-1 redis-cli cluster info 2>$null
$existingNodes = docker exec shortify-redis-node-1 redis-cli cluster nodes 2>$null
$nodeCount = ($existingNodes -split "`n" | Where-Object { $_.Trim() -ne "" }).Count

if ($existingClusterInfo -match "cluster_state:ok" -and $nodeCount -ge 6) {
    Write-Host "[OK] Cluster already exists and is healthy ($nodeCount nodes)!" -ForegroundColor Green
    
    # Configure cluster-announce settings for Spring Boot connectivity
    Write-Host "Configuring cluster for Spring Boot connectivity..." -ForegroundColor Yellow
    $announceSettings = @(
        @{node=1; port=7001; bus=17001},
        @{node=2; port=7002; bus=17002},
        @{node=3; port=7003; bus=17003},
        @{node=4; port=7004; bus=17004},
        @{node=5; port=7005; bus=17005},
        @{node=6; port=7006; bus=17006}
    )
    
    foreach ($setting in $announceSettings) {
        docker exec shortify-redis-node-$($setting.node) redis-cli CONFIG SET cluster-announce-ip host.docker.internal 2>$null | Out-Null
        docker exec shortify-redis-node-$($setting.node) redis-cli CONFIG SET cluster-announce-port $($setting.port) 2>$null | Out-Null
        docker exec shortify-redis-node-$($setting.node) redis-cli CONFIG SET cluster-announce-bus-port $($setting.bus) 2>$null | Out-Null
    }
    
    Write-Host "[OK] Cluster configured for Spring Boot!" -ForegroundColor Green
    Write-Host "[INFO] Cluster advertises host.docker.internal:7001-7006" -ForegroundColor Cyan
    Write-Host "[WARNING] Monitor replication - may need adjustment" -ForegroundColor Yellow
} else {
    # Reset all nodes to ensure clean state before creating cluster
    Write-Host "Resetting all nodes to ensure clean state..." -ForegroundColor Yellow
    for ($i = 1; $i -le 6; $i++) {
        Write-Host "  Resetting node $i..." -ForegroundColor Gray
        docker exec shortify-redis-node-$i redis-cli cluster reset hard 2>$null | Out-Null
    }
    Start-Sleep -Seconds 3
    
    # Create Redis cluster (without cluster-announce settings for reliable creation)
    Write-Host "Creating Redis cluster (3 masters + 3 replicas)..." -ForegroundColor Yellow
    Write-Host "This typically takes 10-30 seconds..." -ForegroundColor Cyan
    Write-Host ""
    
    # Create cluster using internal Docker network names
    docker exec shortify-redis-node-1 sh -c "echo yes | redis-cli --cluster create shortify-redis-node-1:6379 shortify-redis-node-2:6379 shortify-redis-node-3:6379 shortify-redis-node-4:6379 shortify-redis-node-5:6379 shortify-redis-node-6:6379 --cluster-replicas 1" 2>&1 | Out-Host
    
    Write-Host ""
    Write-Host "Waiting for cluster to stabilize..." -ForegroundColor Yellow
    Start-Sleep -Seconds 5
    
    # Verify cluster status
    Write-Host "Verifying cluster status..." -ForegroundColor Yellow
    $clusterInfo = docker exec shortify-redis-node-1 redis-cli cluster info 2>$null
    $nodes = docker exec shortify-redis-node-1 redis-cli cluster nodes 2>$null
    $nodeCount = ($nodes -split "`n" | Where-Object { $_.Trim() -ne "" }).Count
    
    if ($clusterInfo -match "cluster_state:ok" -and $nodeCount -ge 6) {
        Write-Host "[OK] Cluster created successfully! ($nodeCount nodes)" -ForegroundColor Green
        
        # Configure cluster-announce settings for Spring Boot connectivity
        # Use host.docker.internal (Docker Desktop's host gateway) so Spring Boot can reach Redis
        Write-Host "Configuring cluster for Spring Boot connectivity..." -ForegroundColor Yellow
        $announceSettings = @(
            @{node=1; port=7001; bus=17001},
            @{node=2; port=7002; bus=17002},
            @{node=3; port=7003; bus=17003},
            @{node=4; port=7004; bus=17004},
            @{node=5; port=7005; bus=17005},
            @{node=6; port=7006; bus=17006}
        )
        
        foreach ($setting in $announceSettings) {
            Write-Host "  Configuring node $($setting.node)..." -ForegroundColor Gray
            # Use host.docker.internal so Spring Boot (on host) can reach Redis
            docker exec shortify-redis-node-$($setting.node) redis-cli CONFIG SET cluster-announce-ip host.docker.internal 2>$null | Out-Null
            docker exec shortify-redis-node-$($setting.node) redis-cli CONFIG SET cluster-announce-port $($setting.port) 2>$null | Out-Null
            docker exec shortify-redis-node-$($setting.node) redis-cli CONFIG SET cluster-announce-bus-port $($setting.bus) 2>$null | Out-Null
        }
        
        Write-Host "[OK] Cluster configured for Spring Boot!" -ForegroundColor Green
        Write-Host "[INFO] Cluster advertises host.docker.internal:7001-7006 for Spring Boot" -ForegroundColor Cyan
        Write-Host "[WARNING] Internal replication may use these addresses - monitor for issues" -ForegroundColor Yellow
    } else {
        Write-Host "[WARNING] Cluster may still be forming. Checking again..." -ForegroundColor Yellow
        Start-Sleep -Seconds 5
        $clusterInfo = docker exec shortify-redis-node-1 redis-cli cluster info 2>$null
        if ($clusterInfo -match "cluster_state:ok") {
            Write-Host "[OK] Cluster is now healthy!" -ForegroundColor Green
            
            # Configure cluster-announce settings
            Write-Host "Configuring cluster for Spring Boot connectivity..." -ForegroundColor Yellow
            for ($i = 1; $i -le 6; $i++) {
                $port = 7000 + $i
                $busPort = 17000 + $i
                docker exec shortify-redis-node-$i redis-cli CONFIG SET cluster-announce-ip host.docker.internal 2>$null | Out-Null
                docker exec shortify-redis-node-$i redis-cli CONFIG SET cluster-announce-port $port 2>$null | Out-Null
                docker exec shortify-redis-node-$i redis-cli CONFIG SET cluster-announce-bus-port $busPort 2>$null | Out-Null
            }
            Write-Host "[OK] Cluster configured for Spring Boot!" -ForegroundColor Green
            Write-Host "[INFO] Cluster advertises host.docker.internal:7001-7006" -ForegroundColor Cyan
        } else {
            Write-Host "[WARNING] Cluster status: $($clusterInfo -match 'cluster_state:(\w+)' | ForEach-Object { $matches[1] })" -ForegroundColor Yellow
            Write-Host "Wait 30 seconds and check manually:" -ForegroundColor Yellow
            Write-Host "  docker exec shortify-redis-node-1 redis-cli cluster info" -ForegroundColor White
        }
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Redis Cluster Configuration" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Cluster Nodes:" -ForegroundColor Cyan
Write-Host "  - Node 1: localhost:7001 (Master)" -ForegroundColor White
Write-Host "  - Node 2: localhost:7002 (Master)" -ForegroundColor White
Write-Host "  - Node 3: localhost:7003 (Master)" -ForegroundColor White
Write-Host "  - Node 4: localhost:7004 (Replica)" -ForegroundColor White
Write-Host "  - Node 5: localhost:7005 (Replica)" -ForegroundColor White
Write-Host "  - Node 6: localhost:7006 (Replica)" -ForegroundColor White
Write-Host ""
Write-Host "For Spring Boot (use any 3 nodes, client will discover cluster):" -ForegroundColor Cyan
Write-Host "  spring.data.redis.cluster.nodes=localhost:7001,localhost:7002,localhost:7003" -ForegroundColor Yellow
Write-Host ""
Write-Host "RedisInsight GUI:" -ForegroundColor Cyan
$redisinsightStatus = docker ps --filter "name=shortify-redisinsight" --format "{{.Status}}"
if ($redisinsightStatus) {
    Write-Host "  Web UI: http://localhost:8086 [RUNNING]" -ForegroundColor Green
} else {
    Write-Host "  Web UI: http://localhost:8086 [STARTING...]" -ForegroundColor Yellow
}
Write-Host "  To connect to cluster:" -ForegroundColor White
Write-Host "    1. Open http://localhost:8086 in your browser" -ForegroundColor Gray
Write-Host "    2. Click 'Add Redis Database'" -ForegroundColor Gray
Write-Host "    3. Select 'Cluster' mode (toggle at top)" -ForegroundColor Gray
Write-Host "    4. Enter Host: shortify-redis-node-1 (or localhost), Port: 6379 (or 7001)" -ForegroundColor Gray
Write-Host "    5. Click 'Add Redis Database'" -ForegroundColor Gray
Write-Host "    6. RedisInsight will automatically discover all 6 cluster nodes" -ForegroundColor Gray
Write-Host ""
Write-Host "Useful Commands:" -ForegroundColor Cyan
Write-Host "  Stop cluster: docker-compose -f $composeFile down" -ForegroundColor White
Write-Host "  Stop and remove data: docker-compose -f $composeFile down -v" -ForegroundColor White
Write-Host "  View logs: docker-compose -f $composeFile logs -f" -ForegroundColor White
Write-Host "  Check cluster: docker exec shortify-redis-node-1 redis-cli cluster info" -ForegroundColor White
Write-Host "  Cluster nodes: docker exec shortify-redis-node-1 redis-cli cluster nodes" -ForegroundColor White
Write-Host ""
