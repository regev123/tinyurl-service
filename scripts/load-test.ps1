# Load Test Script for TinyURL Service (PowerShell)
# This script uses Invoke-WebRequest for load testing

$BaseUrl = "http://localhost:8080"
$ApiUrl = "$BaseUrl/api/v1/url/shorten"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "TinyURL Service Load Test" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Check if server is running
Write-Host "Checking if server is running..." -ForegroundColor Yellow
try {
    $testBody = '{"originalUrl":"health-check","baseUrl":"http://localhost:8080"}'
    $response = Invoke-WebRequest -Uri $ApiUrl -Method POST -Body $testBody -ContentType "application/json" -TimeoutSec 2 -UseBasicParsing -ErrorAction Stop
    Write-Host "Server is running!" -ForegroundColor Green
} catch {
    Write-Host "Server is not running on $BaseUrl" -ForegroundColor Red
    Write-Host "Please start the Spring Boot application first" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Function to run load test
function Run-LoadTest {
    param(
        [int]$TotalRequests,
        [int]$ConcurrentRequests,
        [string]$TestName
    )
    
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host "$TestName" -ForegroundColor Cyan
    Write-Host "$TotalRequests requests, $ConcurrentRequests concurrent users" -ForegroundColor Cyan
    Write-Host "==========================================" -ForegroundColor Cyan
    
    # First, create some existing URLs for cache testing
    Write-Host "  Creating base URLs for mixed test..." -ForegroundColor Gray
    $existingUrls = @()
    for ($i = 1; $i -le 100; $i++) {
        try {
            $existingBody = "{`"originalUrl`":`"https://www.example.com/existing-url-$i`",`"baseUrl`":`"http://localhost:8080`"}"
            $existingResponse = Invoke-WebRequest -Uri $ApiUrl -Method POST -Body $existingBody -ContentType "application/json" -TimeoutSec 10 -UseBasicParsing -ErrorAction Stop
            $existingUrls += "https://www.example.com/existing-url-$i"
        } catch {
            # Ignore errors
        }
    }
    Write-Host "  Created $($existingUrls.Count) existing URLs" -ForegroundColor Gray
    
    $totalSuccess = 0
    $totalErrors = 0
    $totalTime = 0
    $totalCacheHits = 0
    $totalNewRequests = 0
    
    $elapsed = Measure-Command {
        $jobs = @()
        
        for ($i = 0; $i -lt $ConcurrentRequests; $i++) {
            $batchSize = [Math]::Ceiling($TotalRequests / $ConcurrentRequests)
            $remaining = $TotalRequests - ($i * $batchSize)
            $currentBatchSize = [Math]::Min($batchSize, $remaining)
            
            if ($currentBatchSize -gt 0) {
                $job = Start-Job -ScriptBlock {
                    param($url, $batch, $threadId, $existingUrlsArray)
                    
                    $result = @{
                        Success = 0
                        Errors = 0
                        TotalTime = 0
                        CacheHits = 0
                        NewRequests = 0
                    }
                    
                    foreach ($req in 1..$batch) {
                        try {
                            # 70% cache hits (existing URLs), 30% new URLs
                            $isExisting = (Get-Random -Maximum 100) -lt 70
                            
                            if ($isExisting -and $existingUrlsArray.Count -gt 0) {
                                # Request an existing URL (cache hit)
                                $existingUrl = $existingUrlsArray[(Get-Random -Maximum $existingUrlsArray.Count)]
                                $body = "{`"originalUrl`":`"$existingUrl`",`"baseUrl`":`"http://localhost:8080`"}"
                                $result.CacheHits++
                            } else {
                                # Create a new unique URL (database write)
                                $timestamp = Get-Date -Format "yyyyMMddHHmmss.fff"
                                $requestId = "$threadId-$req-$timestamp"
                                $body = "{`"originalUrl`":`"https://www.example.com/test?param=$requestId`",`"baseUrl`":`"http://localhost:8080`"}"
                                $result.NewRequests++
                            }
                            
                            $start = Get-Date
                            $response = Invoke-WebRequest -Uri $url -Method POST -Body $body -ContentType "application/json" -TimeoutSec 30 -UseBasicParsing -ErrorAction Stop
                            $end = Get-Date
                            
                            $result.Success++
                            $result.TotalTime += ($end - $start).TotalMilliseconds
                        } catch {
                            $result.Errors++
                        }
                    }
                    
                    return $result
                } -ArgumentList $ApiUrl, $currentBatchSize, $i, $existingUrls
                
                $jobs += $job
            }
        }
        
        # Wait for all jobs and collect results
        foreach ($job in $jobs) {
            $result = Wait-Job $job | Receive-Job
            $totalSuccess += $result.Success
            $totalErrors += $result.Errors
            $totalTime += $result.TotalTime
            $totalCacheHits += $result.CacheHits
            $totalNewRequests += $result.NewRequests
            Remove-Job $job
        }
    }
    
    $avgResponseTime = if ($totalSuccess -gt 0) { [Math]::Round($totalTime / $totalSuccess, 2) } else { 0 }
    $requestsPerSecond = if ($elapsed.TotalSeconds -gt 0) { [Math]::Round($totalSuccess / $elapsed.TotalSeconds, 2) } else { 0 }
    
    Write-Host ""
    Write-Host "Results:" -ForegroundColor Green
    Write-Host "  Requests: $TotalRequests"
    Write-Host "  Successful: $totalSuccess" -ForegroundColor Green
    Write-Host "  Errors: $totalErrors" -ForegroundColor $(if ($totalErrors -eq 0) { "Green" } else { "Red" })
    Write-Host "  Cache Hits (existing URLs): $totalCacheHits" -ForegroundColor Yellow
    Write-Host "  New URLs Created: $totalNewRequests" -ForegroundColor Cyan
    Write-Host "  Total Time: $([Math]::Round($elapsed.TotalSeconds, 2))s"
    Write-Host "  Average Response Time: ${avgResponseTime}ms"
    Write-Host "  Requests per Second: $requestsPerSecond" -ForegroundColor Cyan
    Write-Host ""
}

# Run tests
Run-LoadTest -TotalRequests 100 -ConcurrentRequests 10 -TestName "Test 1: Baseline Performance"
Run-LoadTest -TotalRequests 1000 -ConcurrentRequests 50 -TestName "Test 2: Medium Load"
Run-LoadTest -TotalRequests 5000 -ConcurrentRequests 100 -TestName "Test 3: High Load"
Run-LoadTest -TotalRequests 10000 -ConcurrentRequests 200 -TestName "Test 4: Stress Test"

Write-Host "Load testing completed!" -ForegroundColor Green
