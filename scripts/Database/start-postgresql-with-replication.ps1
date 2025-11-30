# PostgreSQL Replication Auto-Setup Script
# This script automatically starts Docker Compose and configures replication
# Run: .\start-postgresql-with-replication.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "PostgreSQL Replication Auto-Setup" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Step 1: Clean up old containers and volumes (if any)
Write-Host "[1/7] Cleaning up old containers and volumes (if any)..." -ForegroundColor Yellow
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Stop and remove containers with volumes (this will delete all data)
docker-compose -f "$scriptDir\docker-compose-postgresql.yml" down -v 2>$null | Out-Null

# Remove any orphaned containers with the same names
docker rm -f tinyurl-postgres-primary tinyurl-postgres-replica1 tinyurl-postgres-replica2 tinyurl-postgres-replica3 2>$null | Out-Null

# Remove volumes if they still exist (in case docker-compose down -v didn't work)
docker volume rm postgres-primary-data postgres-replica1-data postgres-replica2-data postgres-replica3-data 2>$null | Out-Null

Write-Host "  Cleanup complete! All previous data has been removed.`n" -ForegroundColor Green

# Step 2: Start Docker Compose
Write-Host "[2/7] Starting Docker Compose containers..." -ForegroundColor Yellow
docker-compose -f "$scriptDir\docker-compose-postgresql.yml" up -d

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to start containers!" -ForegroundColor Red
    exit 1
}

Write-Host "  Containers started successfully!`n" -ForegroundColor Green

# Step 3: Wait for primary to be healthy
Write-Host "[3/7] Waiting for primary database to be ready..." -ForegroundColor Yellow
$maxAttempts = 30
$attempt = 0
$primaryReady = $false

while ($attempt -lt $maxAttempts) {
    $health = docker inspect --format='{{.State.Health.Status}}' tinyurl-postgres-primary 2>$null
    if ($health -eq "healthy") {
        $primaryReady = $true
        break
    }
    Write-Host "  Waiting... ($attempt/$maxAttempts)" -ForegroundColor Gray
    Start-Sleep -Seconds 2
    $attempt++
}

if (-not $primaryReady) {
    Write-Host "ERROR: Primary database did not become healthy in time!" -ForegroundColor Red
    Write-Host "Check logs with: docker logs tinyurl-postgres-primary" -ForegroundColor Yellow
    exit 1
}

Write-Host "  Primary database is ready!`n" -ForegroundColor Green

# Step 4: Add replication entry to pg_hba.conf
Write-Host "[4/7] Configuring replication in pg_hba.conf..." -ForegroundColor Yellow

# Check if entry already exists
$existingEntry = docker exec tinyurl-postgres-primary bash -c "grep 'host replication postgres' /var/lib/postgresql/data/pg_hba.conf 2>/dev/null" 2>$null

if ($existingEntry) {
    Write-Host "  Replication entry already exists, skipping..." -ForegroundColor Gray
} else {
    docker exec -it tinyurl-postgres-primary bash -c "echo 'host replication postgres 0.0.0.0/0 md5' >> /var/lib/postgresql/data/pg_hba.conf" 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  Replication entry added successfully!" -ForegroundColor Green
    } else {
        Write-Host "  WARNING: Failed to add replication entry, but continuing..." -ForegroundColor Yellow
    }
}

# Step 5: Reload PostgreSQL configuration
Write-Host "[5/7] Reloading PostgreSQL configuration..." -ForegroundColor Yellow
docker exec tinyurl-postgres-primary psql -U postgres -t -c "SELECT pg_reload_conf();" 2>$null | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Host "  Configuration reloaded successfully!`n" -ForegroundColor Green
} else {
    Write-Host "  WARNING: Failed to reload configuration, but continuing..." -ForegroundColor Yellow
}

# Step 6: Clear replica data and restart replicas
Write-Host "[6/7] Setting up replicas (this may take 60-90 seconds)..." -ForegroundColor Yellow

# Clear replica data directories if they exist (for fresh base backup)
Write-Host "  Clearing replica data directories..." -ForegroundColor Gray
docker exec tinyurl-postgres-replica1 bash -c "rm -rf /var/lib/postgresql/data/* /var/lib/postgresql/data/.* 2>/dev/null; exit 0" 2>$null | Out-Null
docker exec tinyurl-postgres-replica2 bash -c "rm -rf /var/lib/postgresql/data/* /var/lib/postgresql/data/.* 2>/dev/null; exit 0" 2>$null | Out-Null
docker exec tinyurl-postgres-replica3 bash -c "rm -rf /var/lib/postgresql/data/* /var/lib/postgresql/data/.* 2>/dev/null; exit 0" 2>$null | Out-Null

# Restart replicas
Write-Host "  Restarting replicas..." -ForegroundColor Gray
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
docker-compose -f "$scriptDir\docker-compose-postgresql.yml" restart postgres-replica1 postgres-replica2 postgres-replica3 2>$null | Out-Null

# Wait for replicas to initialize
Write-Host "  Waiting for replicas to take base backup and start replication (60 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 60

# Step 7: Verification
Write-Host "[7/7] Verifying replication setup..." -ForegroundColor Yellow

# Check replica status (with retries)
$maxRetries = 5
$retryCount = 0
$replica1Status = ""
$replica2Status = ""
$replica3Status = ""

while ($retryCount -lt $maxRetries) {
    $replica1Status = docker exec tinyurl-postgres-replica1 psql -U postgres -t -c "SELECT pg_is_in_recovery();" 2>$null
    $replica2Status = docker exec tinyurl-postgres-replica2 psql -U postgres -t -c "SELECT pg_is_in_recovery();" 2>$null
    $replica3Status = docker exec tinyurl-postgres-replica3 psql -U postgres -t -c "SELECT pg_is_in_recovery();" 2>$null
    
    if ($replica1Status -match "t" -and $replica2Status -match "t" -and $replica3Status -match "t") {
        break
    }
    
    $retryCount++
    if ($retryCount -lt $maxRetries) {
        Write-Host "  Retrying verification... ($retryCount/$maxRetries)" -ForegroundColor Gray
        Start-Sleep -Seconds 10
    }
}

$replicaCount = docker exec tinyurl-postgres-primary psql -U postgres -d tinyurl -t -c "SELECT count(*) FROM pg_stat_replication;" 2>$null

# Display results
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Verification Results" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "  Replica 1 (port 5434): " -NoNewline
if ($replica1Status -match "t") {
    Write-Host "[OK] (in recovery mode)" -ForegroundColor Green
} else {
    Write-Host "[FAILED]" -ForegroundColor Red
}

Write-Host "  Replica 2 (port 5435): " -NoNewline
if ($replica2Status -match "t") {
    Write-Host "[OK] (in recovery mode)" -ForegroundColor Green
} else {
    Write-Host "[FAILED]" -ForegroundColor Red
}

Write-Host "  Replica 3 (port 5436): " -NoNewline
if ($replica3Status -match "t") {
    Write-Host "[OK] (in recovery mode)" -ForegroundColor Green
} else {
    Write-Host "[FAILED]" -ForegroundColor Red
}

Write-Host "  Connected replicas: " -NoNewline
if ($replicaCount -match "\d+") {
    Write-Host "$replicaCount" -ForegroundColor Green
} else {
    Write-Host "0" -ForegroundColor Yellow
}

# Final summary
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Setup Complete!" -ForegroundColor Green
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "Connection Details:" -ForegroundColor Yellow
Write-Host "  Primary (Write):   localhost:5433" -ForegroundColor Cyan
Write-Host "  Replica 1 (Read):  localhost:5434" -ForegroundColor Cyan
Write-Host "  Replica 2 (Read):  localhost:5435" -ForegroundColor Cyan
Write-Host "  Replica 3 (Read):  localhost:5436" -ForegroundColor Cyan
Write-Host "  Username:          postgres" -ForegroundColor Cyan
Write-Host "  Password:          postgres" -ForegroundColor Cyan
Write-Host "  Database:          tinyurl" -ForegroundColor Cyan

Write-Host "`n[SUCCESS] All databases are synced and ready!" -ForegroundColor Green
Write-Host "[INFO] When Hibernate creates tables on primary, they will automatically appear on all replicas.`n" -ForegroundColor Green

