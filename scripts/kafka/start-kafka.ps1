# PowerShell script to start Kafka using Docker Compose

Write-Host "Starting Kafka cluster..." -ForegroundColor Green

$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$composeFile = Join-Path $scriptPath "docker-compose-kafka.yml"

# Check if Docker is running
try {
    docker info | Out-Null
} catch {
    Write-Host "Error: Docker is not running. Please start Docker Desktop." -ForegroundColor Red
    exit 1
}

# Start Kafka
Write-Host "Starting Zookeeper and Kafka..." -ForegroundColor Yellow
docker-compose -f $composeFile up -d

Write-Host "Waiting for Kafka to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Check if Kafka is running
$kafkaStatus = docker ps --filter "name=kafka" --format "{{.Status}}"
if ($kafkaStatus) {
    Write-Host "Kafka is running!" -ForegroundColor Green
    Write-Host "Kafka Broker: localhost:9092" -ForegroundColor Cyan
    Write-Host "Kafka UI: http://localhost:8084" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "To stop Kafka, run: docker-compose -f $composeFile down" -ForegroundColor Yellow
} else {
    Write-Host "Error: Kafka failed to start. Check logs with: docker-compose -f $composeFile logs" -ForegroundColor Red
}

