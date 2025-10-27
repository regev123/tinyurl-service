# Performance Testing Scripts

This directory contains scripts to performance test the TinyURL service with large datasets.

## Available Scripts

### 1. Load Testing

#### For Linux/Mac (bash):

```bash
chmod +x scripts/load-test.sh
./scripts/load-test.sh
```

#### For Windows (PowerShell):

```powershell
.\scripts\load-test.ps1
```

**What it does:**

- Runs 4 different load tests with increasing intensity:
  - **Test 1**: 100 requests, 10 concurrent users (baseline)
  - **Test 2**: 1,000 requests, 50 concurrent users (medium load)
  - **Test 3**: 5,000 requests, 100 concurrent users (high load)
  - **Test 4**: 10,000 requests, 200 concurrent users (stress test)

**Required tools:**

- For Linux/Mac: Apache Bench (ab) - install with `sudo apt-get install apache2-utils` or `brew install httpd`
- For Windows: The PowerShell script uses built-in `Invoke-WebRequest`

### 2. Load Test Data

#### For Linux/Mac (bash):

```bash
chmod +x scripts/load-data.sh
./scripts/load-data.sh
```

#### For Windows (PowerShell):

```powershell
.\scripts\load-data.ps1
```

**What it does:**

- Pre-populates the database with 1,000 test URLs
- Useful for testing lookup performance with existing data

## Running Tests

### Step 1: Start the Application

```bash
mvn spring-boot:run
```

### Step 2: Load Test Data (Optional)

If you want to test with existing data in the database:

```bash
./scripts/load-data.sh
```

### Step 3: Run Load Tests

```bash
./scripts/load-test.sh
```

## Understanding the Results

The load tests provide the following metrics:

- **Requests per second**: How many requests your service can handle
- **Time per request**: Average response time
- **Response time percentiles**: 50%, 66%, 75%, 80%, 90%, 95%, 98%, 99%, 100%
- **Error count**: Number of failed requests

## Expected Performance

Based on the cache configuration (10,000 entries, 1 hour expiration), you should expect:

- **Fast responses** for cached lookups (< 1ms)
- **Slightly slower** responses for database lookups (10-50ms)
- **Baseline**: 500-1000 requests/second
- **Cached lookups**: 5000-10000 requests/second

## Customizing Tests

You can modify the scripts to:

- Change the number of requests
- Adjust concurrent users
- Test different endpoints
- Use different URL patterns

## Troubleshooting

### Server not running

Make sure the Spring Boot application is running on `http://localhost:8080`

### Apache Bench not found

Install Apache Bench:

- Linux: `sudo apt-get install apache2-utils`
- Mac: `brew install httpd`
- Windows: Download Apache HTTP Server

### Connection refused

Check that the server is running and accessible at the configured port.
