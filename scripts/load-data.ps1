# Script to load sample data into the TinyURL service (PowerShell)
# This will create 1000 URLs for testing

$BaseUrl = "http://localhost:8080"
$ApiUrl = "$BaseUrl/api/v1/url/shorten"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Loading Test Data into TinyURL Service" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Check if server is running
try {
    $response = Invoke-WebRequest -Uri $BaseUrl -TimeoutSec 2 -UseBasicParsing -ErrorAction SilentlyContinue
} catch {
    Write-Host "Server is not running on $BaseUrl" -ForegroundColor Red
    Write-Host "Please start the Spring Boot application first" -ForegroundColor Red
    exit 1
}

Write-Host "Creating test URLs..." -ForegroundColor Yellow
Write-Host ""

# Create multiple URLs
for ($i = 1; $i -le 1000; $i++) {
    $timestamp = Get-Date -UFormat %s
    
    $body = @{
        originalUrl = "https://www.example.com/test-$i?param=value-$i&timestamp=$timestamp"
        baseUrl = "http://localhost:8080"
    } | ConvertTo-Json
    
    $headers = @{
        "Content-Type" = "application/json"
    }
    
    try {
        $response = Invoke-WebRequest -Uri $ApiUrl -Method POST -Body $body -Headers $headers -UseBasicParsing -ErrorAction SilentlyContinue
        
        # Print progress every 100 URLs
        if ($i % 100 -eq 0) {
            Write-Host "Created $i URLs..." -ForegroundColor Green
        }
    } catch {
        Write-Host "Error creating URL $i" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Test data loading completed!" -ForegroundColor Green
Write-Host "Created 1000 URLs in the database" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan
