#!/usr/bin/env pwsh

<#
.SYNOPSIS
    Integration Test Suite Runner for Amplify Deployment

.DESCRIPTION
    Runs comprehensive integration tests for Amplify deployment including:
    - Pre-deployment validation
    - Test deployment with modified amplify.yml
    - Frontend and backend integration verification
    - Performance testing and load testing
    - Production environment final verification

.PARAMETER FrontendUrl
    Frontend URL to test (required for actual tests)

.PARAMETER ApiUrl
    API URL to test (optional)

.PARAMETER AmplifyAppId
    Amplify App ID (optional)

.PARAMETER SkipDeployment
    Skip test deployment phase

.PARAMETER PerformanceThreshold
    Performance threshold in milliseconds (default: 3000)

.PARAMETER LoadTestDuration
    Load test duration in seconds (default: 30)

.PARAMETER LoadTestConcurrency
    Number of concurrent users for load test (default: 10)

.PARAMETER RunTests
    Run integration test suite tests instead of actual integration tests

.PARAMETER Help
    Show help message

.EXAMPLE
    .\run-integration-tests.ps1
    
.EXAMPLE
    .\run-integration-tests.ps1 -FrontendUrl "https://main.d1234567890.amplifyapp.com"
    
.EXAMPLE
    .\run-integration-tests.ps1 -FrontendUrl "https://example.com" -ApiUrl "https://api.example.com" -SkipDeployment
    
.EXAMPLE
    .\run-integration-tests.ps1 -RunTests
#>

param(
    [string]$FrontendUrl = $env:FRONTEND_URL,
    [string]$ApiUrl = $env:API_URL,
    [string]$AmplifyAppId = $env:AMPLIFY_APP_ID,
    [switch]$SkipDeployment,
    [int]$PerformanceThreshold = 3000,
    [int]$LoadTestDuration = 30,
    [int]$LoadTestConcurrency = 10,
    [switch]$RunTests,
    [switch]$Help
)

# Show help if requested
if ($Help) {
    Get-Help $MyInvocation.MyCommand.Path -Detailed
    exit 0
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Integration Test Suite Runner" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Node.js is available
try {
    $nodeVersion = node --version 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw "Node.js not found"
    }
    Write-Host "Node.js version: $nodeVersion" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Node.js is not installed or not in PATH" -ForegroundColor Red
    Write-Host "Please install Node.js to run integration tests" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Show warnings for missing environment variables
if (-not $FrontendUrl) {
    Write-Host "WARNING: FRONTEND_URL environment variable not set" -ForegroundColor Yellow
    Write-Host "You can set it with: `$env:FRONTEND_URL='https://your-amplify-app.amplifyapp.com'" -ForegroundColor Yellow
    Write-Host ""
}

if (-not $ApiUrl) {
    Write-Host "WARNING: API_URL environment variable not set" -ForegroundColor Yellow
    Write-Host "You can set it with: `$env:API_URL='https://your-api-gateway-url.amazonaws.com'" -ForegroundColor Yellow
    Write-Host ""
}

if (-not $AmplifyAppId) {
    Write-Host "WARNING: AMPLIFY_APP_ID environment variable not set" -ForegroundColor Yellow
    Write-Host "You can set it with: `$env:AMPLIFY_APP_ID='your-amplify-app-id'" -ForegroundColor Yellow
    Write-Host ""
}

# Show configuration
Write-Host "Configuration:" -ForegroundColor Cyan
Write-Host "  Frontend URL: $($FrontendUrl -or 'Not set')"
Write-Host "  API URL: $($ApiUrl -or 'Not set')"
Write-Host "  Amplify App ID: $($AmplifyAppId -or 'Not set')"
Write-Host "  Skip deployment: $SkipDeployment"
Write-Host "  Performance threshold: ${PerformanceThreshold}ms"
Write-Host "  Load test duration: ${LoadTestDuration}s"
Write-Host "  Load test concurrency: $LoadTestConcurrency"
Write-Host "  Run tests mode: $RunTests"
Write-Host ""

# Check if required files exist
if (-not (Test-Path "integration-test-suite.js")) {
    Write-Host "ERROR: integration-test-suite.js not found" -ForegroundColor Red
    Write-Host "Please ensure you are running this script from the correct directory" -ForegroundColor Red
    exit 1
}

if ($RunTests -and -not (Test-Path "test-integration-suite.js")) {
    Write-Host "ERROR: test-integration-suite.js not found" -ForegroundColor Red
    Write-Host "Please ensure you are running this script from the correct directory" -ForegroundColor Red
    exit 1
}

# Run integration tests or test suite tests
try {
    if ($RunTests) {
        Write-Host "Running integration test suite tests..." -ForegroundColor Cyan
        Write-Host "=====================================" -ForegroundColor Cyan
        Write-Host ""
        
        $process = Start-Process -FilePath "node" -ArgumentList "test-integration-suite.js" -Wait -PassThru -NoNewWindow
        $exitCode = $process.ExitCode
    } else {
        Write-Host "Running integration test suite..." -ForegroundColor Cyan
        Write-Host "===============================" -ForegroundColor Cyan
        Write-Host ""
        
        # Build command arguments
        $args = @()
        if ($FrontendUrl) { $args += "--frontend-url", $FrontendUrl }
        if ($ApiUrl) { $args += "--api-url", $ApiUrl }
        if ($AmplifyAppId) { $args += "--amplify-app-id", $AmplifyAppId }
        if ($SkipDeployment) { $args += "--skip-deployment" }
        $args += "--performance-threshold", $PerformanceThreshold
        $args += "--load-test-duration", $LoadTestDuration
        $args += "--load-test-concurrency", $LoadTestConcurrency
        
        # Run the integration test suite
        $process = Start-Process -FilePath "node" -ArgumentList (@("integration-test-suite.js") + $args) -Wait -PassThru -NoNewWindow
        $exitCode = $process.ExitCode
    }
} catch {
    Write-Host "ERROR: Failed to run integration tests: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan

# Check results
if ($exitCode -eq 0) {
    Write-Host "SUCCESS: Integration tests completed successfully!" -ForegroundColor Green
    Write-Host ""
    
    if ($RunTests) {
        Write-Host "All integration test suite tests passed." -ForegroundColor Green
        if (Test-Path "integration-test-suite-test-report.json") {
            Write-Host "Detailed test report: integration-test-suite-test-report.json" -ForegroundColor Cyan
        }
    } else {
        Write-Host "All integration test phases passed." -ForegroundColor Green
        if (Test-Path "integration-test-report.json") {
            Write-Host "Detailed integration report: integration-test-report.json" -ForegroundColor Cyan
        }
        if (Test-Path "deployment-verification-report.json") {
            Write-Host "Deployment verification report: deployment-verification-report.json" -ForegroundColor Cyan
        }
    }
} else {
    Write-Host "FAILURE: Integration tests failed!" -ForegroundColor Red
    Write-Host ""
    
    if ($RunTests) {
        Write-Host "Some integration test suite tests failed." -ForegroundColor Red
        Write-Host "Check the output above for details." -ForegroundColor Yellow
    } else {
        Write-Host "Some integration test phases failed." -ForegroundColor Red
        Write-Host "Check the integration-test-report.json for detailed information." -ForegroundColor Yellow
    }
    
    Write-Host ""
    Write-Host "Common issues:" -ForegroundColor Yellow
    Write-Host "- Frontend URL not accessible" -ForegroundColor Yellow
    Write-Host "- API URL not responding" -ForegroundColor Yellow
    Write-Host "- Performance thresholds not met" -ForegroundColor Yellow
    Write-Host "- Load test failures" -ForegroundColor Yellow
    Write-Host "- Production verification issues" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Integration test execution completed." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

exit $exitCode