# PowerShell script to start Redis (standalone) using Docker Compose
# Usage: .\start-redis.ps1 [-CleanVolumes]
#   -CleanVolumes: Remove volume for a completely fresh start (WARNING: This deletes all Redis data)

param(
    [switch]$CleanVolumes = $false
)

Write-Host "Starting Redis (Standalone)..." -ForegroundColor Green

$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$composeFile = Join-Path $scriptPath "docker-compose-redis.yml"

# Check if Docker is running
try {
    docker info | Out-Null
} catch {
    Write-Host "Error: Docker is not running. Please start Docker Desktop." -ForegroundColor Red
    exit 1
}

# Stop any existing Redis containers
Write-Host "Checking for existing Redis containers..." -ForegroundColor Yellow
$existingRedis = docker ps -a --filter "name=shortify-redis" --format "{{.Names}}"

if ($existingRedis) {
    Write-Host "Stopping existing Redis container..." -ForegroundColor Yellow
    
    if ($CleanVolumes) {
        Write-Host "Removing volume for fresh start..." -ForegroundColor Yellow
        docker-compose -f $composeFile down -v 2>$null
    } else {
        docker-compose -f $composeFile down 2>$null
    }
    
    Write-Host "Waiting for cleanup to complete..." -ForegroundColor Yellow
    Start-Sleep -Seconds 2
}

# Start Redis
Write-Host "Starting Redis..." -ForegroundColor Yellow
docker-compose -f $composeFile up -d --remove-orphans

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to start Redis container!" -ForegroundColor Red
    exit 1
}

Write-Host "Waiting for Redis to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# Verify Redis is running and responding
Write-Host "Verifying Redis is ready..." -ForegroundColor Yellow
$status = docker ps --filter "name=shortify-redis" --format "{{.Status}}"
if (-not $status) {
    Write-Host "[ERROR] Redis failed to start" -ForegroundColor Red
    Write-Host "Check logs with: docker-compose -f $composeFile logs" -ForegroundColor Yellow
    exit 1
}

$pingResult = docker exec shortify-redis redis-cli ping 2>$null
if ($pingResult -ne "PONG") {
    Write-Host "[WARNING] Redis is not responding, waiting..." -ForegroundColor Yellow
    Start-Sleep -Seconds 5
    $pingResult = docker exec shortify-redis redis-cli ping 2>$null
    if ($pingResult -ne "PONG") {
        Write-Host "[ERROR] Redis is still not responding" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Redis Started Successfully!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Connection Details:" -ForegroundColor Cyan
Write-Host "  Host: localhost" -ForegroundColor White
Write-Host "  Port: 7001" -ForegroundColor White
Write-Host ""
Write-Host "RedisInsight GUI:" -ForegroundColor Cyan
$redisinsightStatus = docker ps --filter "name=shortify-redisinsight" --format "{{.Status}}"
if ($redisinsightStatus) {
    Write-Host "  Web UI: http://localhost:8086 [RUNNING]" -ForegroundColor Green
} else {
    Write-Host "  Web UI: http://localhost:8086 [STARTING...]" -ForegroundColor Yellow
}
Write-Host "  To connect:" -ForegroundColor White
Write-Host "    1. Open http://localhost:8086 in your browser" -ForegroundColor Gray
Write-Host "    2. Click 'Add Redis Database'" -ForegroundColor Gray
Write-Host "    3. Enter Host: shortify-redis, Port: 6379, Database: 0" -ForegroundColor Gray
Write-Host "    4. Click 'Add Redis Database'" -ForegroundColor Gray
Write-Host ""
Write-Host "For Spring Boot:" -ForegroundColor Cyan
Write-Host "  spring.data.redis.host=localhost" -ForegroundColor Yellow
Write-Host "  spring.data.redis.port=7001" -ForegroundColor Yellow
Write-Host ""
Write-Host "Useful Commands:" -ForegroundColor Cyan
Write-Host "  Stop Redis: docker-compose -f $composeFile down" -ForegroundColor White
Write-Host "  Stop and remove data: docker-compose -f $composeFile down -v" -ForegroundColor White
Write-Host "  View logs: docker-compose -f $composeFile logs -f" -ForegroundColor White
Write-Host "  Connect via CLI: docker exec -it shortify-redis redis-cli" -ForegroundColor White
Write-Host "  Test connection: docker exec shortify-redis redis-cli ping" -ForegroundColor White
Write-Host ""

