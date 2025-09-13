# Deployment Verification Script for PowerShell
# 
# This script verifies that the AWS Amplify deployment is working correctly
# by checking frontend availability, static resources, and API endpoints.

param(
    [string]$FrontendUrl = $env:FRONTEND_URL,
    [string]$ApiUrl = $env:API_URL,
    [int]$Timeout = 30000,
    [int]$Retries = 3,
    [switch]$Help
)

function Show-Help {
    Write-Host @"
AWS Amplify Deployment Verification Script

Usage: .\verify-deployment.ps1 [parameters]

Parameters:
  -FrontendUrl <url>    Frontend URL to verify (required)
  -ApiUrl <url>         API URL to verify (optional)
  -Timeout <ms>         Request timeout in milliseconds (default: 30000)
  -Retries <count>      Number of retries for failed requests (default: 3)
  -Help                 Show this help message

Environment Variables:
  FRONTEND_URL         Frontend URL (alternative to -FrontendUrl)
  API_URL             API URL (alternative to -ApiUrl)

Examples:
  .\verify-deployment.ps1 -FrontendUrl "https://main.d1234567890.amplifyapp.com"
  .\verify-deployment.ps1 -FrontendUrl "https://main.d1234567890.amplifyapp.com" -ApiUrl "https://api.example.com"
  `$env:FRONTEND_URL="https://main.d1234567890.amplifyapp.com"; .\verify-deployment.ps1
"@
}

if ($Help) {
    Show-Help
    exit 0
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "AWS Amplify Deployment Verification" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Node.js is available
try {
    $nodeVersion = node --version 2>$null
    if (-not $nodeVersion) {
        throw "Node.js not found"
    }
    Write-Host "✓ Node.js version: $nodeVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Error: Node.js is not installed or not in PATH" -ForegroundColor Red
    Write-Host "Please install Node.js from https://nodejs.org/" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

# Check if verification script exists
if (-not (Test-Path "deployment-verification.js")) {
    Write-Host "❌ Error: deployment-verification.js not found" -ForegroundColor Red
    Write-Host "Please ensure the script is in the current directory" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

# Get URLs from user if not provided
if (-not $FrontendUrl) {
    Write-Host ""
    Write-Host "Please enter your Amplify frontend URL:" -ForegroundColor Yellow
    Write-Host "Example: https://main.d1234567890.amplifyapp.com" -ForegroundColor Gray
    $FrontendUrl = Read-Host "Frontend URL"
}

if (-not $FrontendUrl) {
    Write-Host "❌ Error: Frontend URL is required" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

if (-not $ApiUrl) {
    Write-Host ""
    Write-Host "Please enter your API Gateway URL (optional, press Enter to skip):" -ForegroundColor Yellow
    Write-Host "Example: https://api123456.execute-api.us-east-1.amazonaws.com/prod" -ForegroundColor Gray
    $ApiUrl = Read-Host "API URL (optional)"
}

Write-Host ""
Write-Host "Starting verification with:" -ForegroundColor Cyan
Write-Host "Frontend URL: $FrontendUrl" -ForegroundColor White
if ($ApiUrl) {
    Write-Host "API URL: $ApiUrl" -ForegroundColor White
} else {
    Write-Host "API URL: Not specified (API checks will be skipped)" -ForegroundColor Gray
}
Write-Host ""

# Prepare arguments
$args = @("deployment-verification.js", "--frontend-url", $FrontendUrl)
if ($ApiUrl) {
    $args += @("--api-url", $ApiUrl)
}
if ($Timeout -ne 30000) {
    $args += @("--timeout", $Timeout)
}
if ($Retries -ne 3) {
    $args += @("--retries", $Retries)
}

# Run the verification
try {
    $process = Start-Process -FilePath "node" -ArgumentList $args -Wait -PassThru -NoNewWindow
    $verificationResult = $process.ExitCode
} catch {
    Write-Host "❌ Error running verification: $($_.Exception.Message)" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
if ($verificationResult -eq 0) {
    Write-Host "✅ Deployment verification completed successfully!" -ForegroundColor Green
    Write-Host "Your application is ready to use." -ForegroundColor Green
} else {
    Write-Host "❌ Deployment verification failed!" -ForegroundColor Red
    Write-Host "Please check the errors above and fix any issues." -ForegroundColor Yellow
}
Write-Host "========================================" -ForegroundColor Cyan

# Check if report file was generated
if (Test-Path "deployment-verification-report.json") {
    Write-Host ""
    Write-Host "📄 Detailed report saved to: deployment-verification-report.json" -ForegroundColor Cyan
}

Write-Host ""
Read-Host "Press Enter to exit"
exit $verificationResult