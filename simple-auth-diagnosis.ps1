# Simple Authentication Test Diagnosis
Write-Host "=== Authentication Test Diagnosis ===" -ForegroundColor Green
Write-Host ""

# 1. Check PowerShell Execution Policy
Write-Host "1. PowerShell Execution Policy Check" -ForegroundColor Cyan
$executionPolicy = Get-ExecutionPolicy
Write-Host "Current Policy: $executionPolicy" -ForegroundColor Yellow

if ($executionPolicy -eq "Restricted") {
    Write-Host "WARNING: Execution policy is restricted!" -ForegroundColor Red
    Write-Host "Run: Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser" -ForegroundColor Yellow
}

Write-Host ""

# 2. Check AWS CLI
Write-Host "2. AWS CLI Check" -ForegroundColor Cyan
try {
    $awsVersion = aws --version
    Write-Host "AWS CLI Found: $awsVersion" -ForegroundColor Green
} catch {
    Write-Host "AWS CLI NOT FOUND - Please install AWS CLI" -ForegroundColor Red
    Write-Host "Download from: https://aws.amazon.com/cli/" -ForegroundColor Yellow
}

Write-Host ""

# 3. Check AWS Credentials
Write-Host "3. AWS Credentials Check" -ForegroundColor Cyan
try {
    $identity = aws sts get-caller-identity --output json | ConvertFrom-Json
    Write-Host "AWS Authentication: SUCCESS" -ForegroundColor Green
    Write-Host "Account: $($identity.Account)" -ForegroundColor Gray
} catch {
    Write-Host "AWS Authentication: FAILED" -ForegroundColor Red
    Write-Host "Run: aws configure" -ForegroundColor Yellow
}

Write-Host ""

# 4. Test Network Connection
Write-Host "4. Network Connection Test" -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri "https://bn6xwu62qd.execute-api.ap-northeast-1.amazonaws.com/dev/health" -TimeoutSec 10 -UseBasicParsing
    Write-Host "API Connection: SUCCESS (Status: $($response.StatusCode))" -ForegroundColor Green
} catch {
    Write-Host "API Connection: FAILED" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host ""

# 5. Test Cognito Access
Write-Host "5. Cognito Access Test" -ForegroundColor Cyan
try {
    $pools = aws cognito-idp list-user-pools --max-items 1 --region ap-northeast-1 --output json | ConvertFrom-Json
    Write-Host "Cognito Access: SUCCESS" -ForegroundColor Green
    Write-Host "User Pools Found: $($pools.UserPools.Count)" -ForegroundColor Gray
} catch {
    Write-Host "Cognito Access: FAILED" -ForegroundColor Red
    Write-Host "Check IAM permissions for Cognito" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Diagnosis Complete ===" -ForegroundColor Green