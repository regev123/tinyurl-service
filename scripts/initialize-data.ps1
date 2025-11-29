# Initialize Database with 10,000 URLs
# This script inserts sample URL data into PostgreSQL
# Run: .\initialize-data.ps1

param(
    [int]$Count = 10000,
    [string]$DbHost = "localhost",
    [int]$Port = 5433,
    [string]$Database = "tinyurl",
    [string]$Username = "postgres",
    [string]$Password = "postgres"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Database Data Initialization" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "Configuration:" -ForegroundColor Yellow
Write-Host "  Host:     $DbHost" -ForegroundColor Gray
Write-Host "  Port:     $Port" -ForegroundColor Gray
Write-Host "  Database: $Database" -ForegroundColor Gray
Write-Host "  Count:    $Count URLs`n" -ForegroundColor Gray

# Determine container name based on port
$containerName = "tinyurl-postgres-primary"

# Check if Docker is available
$dockerPath = Get-Command docker -ErrorAction SilentlyContinue
if (-not $dockerPath) {
    Write-Host "ERROR: docker command not found!" -ForegroundColor Red
    Write-Host "Please install Docker Desktop or add docker to your PATH.`n" -ForegroundColor Yellow
    exit 1
}

# Check if container is running
Write-Host "[1/4] Checking if PostgreSQL container is running..." -ForegroundColor Yellow
$containerStatus = docker ps --filter "name=$containerName" --format "{{.Names}}" 2>&1

if ($containerStatus -ne $containerName) {
    Write-Host "ERROR: Container '$containerName' is not running!" -ForegroundColor Red
    Write-Host "`nMake sure PostgreSQL is running:" -ForegroundColor Yellow
    Write-Host "  .\scripts\Database\start-postgresql-with-replication.ps1`n" -ForegroundColor Cyan
    exit 1
}

Write-Host "  Container is running!`n" -ForegroundColor Green

# Test connection
Write-Host "[2/4] Testing database connection..." -ForegroundColor Yellow
$testQuery = "SELECT 1;"
$testResult = docker exec -e PGPASSWORD=$Password $containerName psql -U $Username -d $Database -t -c $testQuery 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Cannot connect to database!" -ForegroundColor Red
    Write-Host "Error: $testResult" -ForegroundColor Red
    exit 1
}

Write-Host "  Connection successful!`n" -ForegroundColor Green

# Check if table exists
Write-Host "[3/4] Checking if url_mappings table exists..." -ForegroundColor Yellow
$tableCheck = docker exec -e PGPASSWORD=$Password $containerName psql -U $Username -d $Database -t -c "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'url_mappings');" 2>&1

if ($tableCheck -match "f") {
    Write-Host "  Table 'url_mappings' does not exist!" -ForegroundColor Yellow
    Write-Host "  Please start your Spring Boot application first to create the table.`n" -ForegroundColor Yellow
    Write-Host "  Run: mvn spring-boot:run`n" -ForegroundColor Cyan
    exit 1
}

Write-Host "  Table exists!`n" -ForegroundColor Green

# Check current count
Write-Host "[4/4] Checking current URL count..." -ForegroundColor Yellow
$currentCount = docker exec -e PGPASSWORD=$Password $containerName psql -U $Username -d $Database -t -c "SELECT COUNT(*) FROM url_mappings;" 2>&1
$currentCount = $currentCount.Trim()
Write-Host "  Current URL count: $currentCount`n" -ForegroundColor Gray

# Generate SQL for batch insert
Write-Host "Generating and inserting $Count URLs..." -ForegroundColor Yellow
Write-Host "  This may take a few moments...`n" -ForegroundColor Gray

# Base62 characters for encoding
$base62Chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

function Encode-Base62 {
    param([long]$number)
    
    if ($number -eq 0) {
        return "0"
    }
    
    $result = ""
    while ($number -gt 0) {
        $result = $base62Chars[$number % 62] + $result
        $number = [Math]::Floor($number / 62)
    }
    
    return $result
}

# Generate random URLs
$urls = @()
$shortUrls = @{}

# Generate unique short URLs
# Increase max attempts to ensure we can generate enough unique URLs
$maxAttempts = $Count * 2
$attempts = 0
$minNumber = 1L
$maxNumber = 56800235583L  # Max short URL number (62^6 - 1)

while ($urls.Count -lt $Count -and $attempts -lt $maxAttempts) {
    # Use Get-Random which supports long values
    $randomNumber = Get-Random -Minimum $minNumber -Maximum $maxNumber
    $shortUrl = Encode-Base62 $randomNumber
    
    if (-not $shortUrls.ContainsKey($shortUrl)) {
        $shortUrls[$shortUrl] = $true
        
        # Generate random original URL
        $urlId = $urls.Count + 1
        $randomId = Get-Random -Minimum 1000 -Maximum 9999
        $originalUrl = "https://example.com/page/$urlId?ref=test&id=$randomId"
        
        # Generate timestamps
        $daysAgo = Get-Random -Minimum 0 -Maximum 365
        $createdAt = (Get-Date).AddDays(-$daysAgo).ToString("yyyy-MM-dd HH:mm:ss")
        $expiryOffset = Get-Random -Minimum -30 -Maximum 30
        $expiresAt = (Get-Date).AddYears(1).AddDays($expiryOffset).ToString("yyyy-MM-dd HH:mm:ss")
        $accessCount = Get-Random -Minimum 0 -Maximum 10000
        
        $urls += @{
            OriginalUrl = $originalUrl
            ShortUrl = $shortUrl
            CreatedAt = $createdAt
            ExpiresAt = $expiresAt
            AccessCount = $accessCount
        }
    }
    
    $attempts++
    
    if ($urls.Count % 1000 -eq 0) {
        Write-Host "  Generated $($urls.Count) URLs..." -ForegroundColor Gray
    }
}

if ($urls.Count -lt $Count) {
    Write-Host "  WARNING: Only generated $($urls.Count) unique URLs (attempted $attempts times)" -ForegroundColor Yellow
}

# Insert in batches for better performance
# Reduced batch size to avoid Windows command line length limits
$batchSize = 100
$totalBatches = [Math]::Ceiling($urls.Count / $batchSize)
$inserted = 0

# Create temporary directory for SQL files
$tempDir = [System.IO.Path]::GetTempPath()
$tempFile = Join-Path $tempDir "tinyurl_insert_$(Get-Date -Format 'yyyyMMddHHmmss').sql"

for ($batch = 0; $batch -lt $totalBatches; $batch++) {
    $startIdx = $batch * $batchSize
    $endIdx = [Math]::Min($startIdx + $batchSize, $urls.Count)
    $batchUrls = $urls[$startIdx..($endIdx - 1)]
    
    # Build SQL INSERT statement
    $sql = "INSERT INTO url_mappings (original_url, short_url, created_at, expires_at, access_count) VALUES "
    $values = @()
    
    foreach ($url in $batchUrls) {
        $escapedOriginalUrl = $url.OriginalUrl.Replace("'", "''")
        $escapedShortUrl = $url.ShortUrl.Replace("'", "''")
        $values += "('$escapedOriginalUrl', '$escapedShortUrl', '$($url.CreatedAt)', '$($url.ExpiresAt)', $($url.AccessCount))"
    }
    
    $sql += ($values -join ", ") + " ON CONFLICT (short_url) DO NOTHING;"
    
    # Write SQL to temporary file to avoid command line length limits
    $sql | Out-File -FilePath $tempFile -Encoding UTF8 -NoNewline
    
    # Copy file to container and execute
    try {
        # Copy file to container
        docker cp $tempFile "${containerName}:/tmp/insert.sql" 2>&1 | Out-Null
        
        # Execute SQL from file inside container
        $result = docker exec -e PGPASSWORD=$Password $containerName psql -U $Username -d $Database -f /tmp/insert.sql 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            $inserted += $batchUrls.Count
            Write-Host "  Inserted batch $($batch + 1)/$totalBatches ($inserted/$($urls.Count) URLs)" -ForegroundColor Green
        } else {
            Write-Host "  WARNING: Batch $($batch + 1) had some errors" -ForegroundColor Yellow
            # Try to count successful inserts from result
            if ($result -match "INSERT (\d+)") {
                $inserted += [int]$matches[1]
            }
        }
    } catch {
        Write-Host "  ERROR: Failed to insert batch $($batch + 1): $_" -ForegroundColor Red
    } finally {
        # Clean up temp file
        if (Test-Path $tempFile) {
            Remove-Item $tempFile -Force -ErrorAction SilentlyContinue
        }
    }
}

# Verify final count
Write-Host "`nVerifying final count..." -ForegroundColor Yellow
$finalCount = docker exec -e PGPASSWORD=$Password $containerName psql -U $Username -d $Database -t -c "SELECT COUNT(*) FROM url_mappings;" 2>&1
$finalCount = $finalCount.Trim()

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Initialization Complete!" -ForegroundColor Green
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "Summary:" -ForegroundColor Yellow
Write-Host "  URLs generated:  $($urls.Count)" -ForegroundColor Gray
Write-Host "  URLs inserted:   $inserted" -ForegroundColor Gray
Write-Host "  Total in DB:      $finalCount" -ForegroundColor Green

Write-Host "`n[SUCCESS] Database initialized with sample data!" -ForegroundColor Green
Write-Host "[INFO] Data will automatically replicate to all read replicas.`n" -ForegroundColor Green

