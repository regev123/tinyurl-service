# PowerShell script to start Kafka Cluster (3 brokers) using Docker Compose

Write-Host "Starting Kafka Cluster (3 brokers)..." -ForegroundColor Green

$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$composeFile = Join-Path $scriptPath "docker-compose-kafka-cluster.yml"

# Check if Docker is running
try {
    docker info | Out-Null
} catch {
    Write-Host "Error: Docker is not running. Please start Docker Desktop." -ForegroundColor Red
    exit 1
}

# Start Kafka Cluster
Write-Host "Starting Zookeeper and Kafka Cluster (3 brokers)..." -ForegroundColor Yellow
docker-compose -f $composeFile up -d

Write-Host "Waiting for Kafka Cluster to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

# Check if Zookeeper is running
$zookeeperStatus = docker ps --filter "name=kafka-zookeeper" --format "{{.Status}}"
if ($zookeeperStatus) {
    Write-Host "✓ Zookeeper is running!" -ForegroundColor Green
} else {
    Write-Host "✗ Zookeeper failed to start" -ForegroundColor Red
}

# Check if all Kafka brokers are running
$broker1Status = docker ps --filter "name=kafka-broker-1" --format "{{.Status}}"
$broker2Status = docker ps --filter "name=kafka-broker-2" --format "{{.Status}}"
$broker3Status = docker ps --filter "name=kafka-broker-3" --format "{{.Status}}"

$allBrokersRunning = $broker1Status -and $broker2Status -and $broker3Status

if ($allBrokersRunning) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Kafka Cluster is running!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Kafka Brokers:" -ForegroundColor Cyan
    Write-Host "  • Broker 1: localhost:9092" -ForegroundColor White
    Write-Host "  • Broker 2: localhost:9093" -ForegroundColor White
    Write-Host "  • Broker 3: localhost:9094" -ForegroundColor White
    Write-Host ""
    Write-Host "Bootstrap Servers (for clients):" -ForegroundColor Cyan
    Write-Host "  localhost:9092,localhost:9093,localhost:9094" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Kafka UI: http://localhost:8084" -ForegroundColor Cyan
    Write-Host "Zookeeper: localhost:2181" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Cluster Configuration:" -ForegroundColor Cyan
    Write-Host "  • Replication Factor: 3" -ForegroundColor White
    Write-Host "  • Min In-Sync Replicas: 2" -ForegroundColor White
    Write-Host "  • Default Partitions: 6" -ForegroundColor White
    Write-Host "  • Can tolerate 1 broker failure" -ForegroundColor White
    Write-Host ""
    Write-Host "To stop Kafka Cluster, run:" -ForegroundColor Yellow
    Write-Host "  docker-compose -f $composeFile down" -ForegroundColor White
    Write-Host ""
    Write-Host "To view logs, run:" -ForegroundColor Yellow
    Write-Host "  docker-compose -f $composeFile logs -f" -ForegroundColor White
} else {
    Write-Host ""
    Write-Host "Error: Some Kafka brokers failed to start." -ForegroundColor Red
    Write-Host ""
    if ($broker1Status) {
        Write-Host "✓ Broker 1 is running" -ForegroundColor Green
    } else {
        Write-Host "✗ Broker 1 failed to start" -ForegroundColor Red
    }
    if ($broker2Status) {
        Write-Host "✓ Broker 2 is running" -ForegroundColor Green
    } else {
        Write-Host "✗ Broker 2 failed to start" -ForegroundColor Red
    }
    if ($broker3Status) {
        Write-Host "✓ Broker 3 is running" -ForegroundColor Green
    } else {
        Write-Host "✗ Broker 3 failed to start" -ForegroundColor Red
    }
    Write-Host ""
    Write-Host "Check logs with: docker-compose -f $composeFile logs" -ForegroundColor Yellow
    exit 1
}

