# Simple Cognito Authentication Test
param(
    [string]$TestUserEmail = "",
    [string]$TestUserPassword = ""
)

$baseUrl = "https://bn6xwu62qd.execute-api.ap-northeast-1.amazonaws.com/dev"
$userPoolId = "ap-northeast-1_S0zRV4ais"
$clientId = "7nue9hv9e54sdrcvorl990q1t6"
$region = "ap-northeast-1"

Write-Host "=== Simple Cognito Authentication Test ===" -ForegroundColor Green
Write-Host "Base URL: $baseUrl" -ForegroundColor Yellow
Write-Host ""

# Test 1: Health Check (No Auth Required)
Write-Host "Test 1: Health Check (No Authentication)" -ForegroundColor Cyan
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/health" -Method GET -TimeoutSec 30
    Write-Host "SUCCESS: Health check passed" -ForegroundColor Green
    Write-Host "Response: $($health | ConvertTo-Json -Compress)" -ForegroundColor Gray
} catch {
    Write-Host "FAILED: Health check failed - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 2: Protected Endpoint (No Auth)
Write-Host "Test 2: Protected Endpoint (No Authentication)" -ForegroundColor Cyan
try {
    $workload = Invoke-RestMethod -Uri "$baseUrl/workload-status" -Method GET -TimeoutSec 30
    Write-Host "WARNING: Protected endpoint accessible without auth (dev mode)" -ForegroundColor Yellow
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Host "SUCCESS: Correctly returned 401 Unauthorized" -ForegroundColor Green
    } else {
        Write-Host "FAILED: Unexpected error - $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""

# Test 3: Mock Token Test
Write-Host "Test 3: Mock Token Test" -ForegroundColor Cyan
$mockHeaders = @{
    "Authorization" = "Bearer mock-jwt-token-testuser"
    "Content-Type" = "application/json"
}

try {
    $workload = Invoke-RestMethod -Uri "$baseUrl/workload-status" -Method GET -Headers $mockHeaders -TimeoutSec 30
    Write-Host "SUCCESS: Mock token accepted (dev mode)" -ForegroundColor Green
} catch {
    Write-Host "FAILED: Mock token rejected - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 4: Real Cognito Authentication (if credentials provided)
if ($TestUserEmail -and $TestUserPassword) {
    Write-Host "Test 4: Real Cognito Authentication" -ForegroundColor Cyan
    Write-Host "User: $TestUserEmail" -ForegroundColor Gray
    
    try {
        # Use AWS CLI to authenticate
        $authCommand = "aws cognito-idp initiate-auth --auth-flow USER_PASSWORD_AUTH --client-id $clientId --auth-parameters USERNAME=$TestUserEmail,PASSWORD=$TestUserPassword --region $region --output json"
        
        $authResult = Invoke-Expression $authCommand | ConvertFrom-Json
        
        if ($authResult.AuthenticationResult) {
            $accessToken = $authResult.AuthenticationResult.AccessToken
            $idToken = $authResult.AuthenticationResult.IdToken
            
            Write-Host "SUCCESS: Cognito authentication successful" -ForegroundColor Green
            
            # Test authenticated API call
            $authHeaders = @{
                "Authorization" = "Bearer $idToken"
                "Content-Type" = "application/json"
            }
            
            try {
                $workload = Invoke-RestMethod -Uri "$baseUrl/workload-status" -Method GET -Headers $authHeaders -TimeoutSec 30
                Write-Host "SUCCESS: Authenticated API call successful" -ForegroundColor Green
            } catch {
                Write-Host "FAILED: Authenticated API call failed - $($_.Exception.Message)" -ForegroundColor Red
            }
            
        } else {
            Write-Host "FAILED: No authentication result received" -ForegroundColor Red
        }
        
    } catch {
        Write-Host "FAILED: Cognito authentication failed" -ForegroundColor Red
        Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Yellow
        
        # Check if user exists
        try {
            $userCheck = aws cognito-idp admin-get-user --user-pool-id $userPoolId --username $TestUserEmail --region $region --output json 2>&1
            if ($LASTEXITCODE -eq 0) {
                Write-Host "INFO: User exists in Cognito" -ForegroundColor Gray
            } else {
                Write-Host "INFO: User may not exist in Cognito" -ForegroundColor Yellow
            }
        } catch {
            Write-Host "INFO: Could not check user existence" -ForegroundColor Gray
        }
    }
} else {
    Write-Host "Test 4: Skipped (No credentials provided)" -ForegroundColor Yellow
    Write-Host "To test real authentication, run:" -ForegroundColor Gray
    Write-Host ".\simple-auth-test.ps1 -TestUserEmail 'user@example.com' -TestUserPassword 'password'" -ForegroundColor Gray
}

Write-Host ""
Write-Host "=== Test Complete ===" -ForegroundColor Green

# Show usage if no parameters provided
if (-not $TestUserEmail -and -not $TestUserPassword) {
    Write-Host ""
    Write-Host "Usage Examples:" -ForegroundColor Cyan
    Write-Host "Basic test: .\simple-auth-test.ps1" -ForegroundColor Gray
    Write-Host "Full test:  .\simple-auth-test.ps1 -TestUserEmail 'test@example.com' -TestUserPassword 'TestPass123!'" -ForegroundColor Gray
}