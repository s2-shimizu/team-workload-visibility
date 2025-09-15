# Create Test User for Cognito Authentication
param(
    [Parameter(Mandatory=$true)]
    [string]$TestUserEmail,
    
    [Parameter(Mandatory=$true)]
    [string]$TestUserPassword
)

$userPoolId = "ap-northeast-1_S0zRV4ais"
$region = "ap-northeast-1"

Write-Host "=== Creating Test User ===" -ForegroundColor Green
Write-Host "User Pool ID: $userPoolId" -ForegroundColor Yellow
Write-Host "Email: $TestUserEmail" -ForegroundColor Yellow
Write-Host ""

# Check if user already exists
Write-Host "Checking if user already exists..." -ForegroundColor Cyan
try {
    $existingUser = aws cognito-idp admin-get-user --user-pool-id $userPoolId --username $TestUserEmail --region $region --output json 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "User already exists!" -ForegroundColor Yellow
        $userObj = $existingUser | ConvertFrom-Json
        Write-Host "Username: $($userObj.Username)" -ForegroundColor Gray
        Write-Host "Status: $($userObj.UserStatus)" -ForegroundColor Gray
        Write-Host "Created: $($userObj.UserCreateDate)" -ForegroundColor Gray
        
        # Try to set password anyway (in case it needs to be reset)
        Write-Host ""
        Write-Host "Updating password..." -ForegroundColor Cyan
        try {
            aws cognito-idp admin-set-user-password --user-pool-id $userPoolId --username $TestUserEmail --password $TestUserPassword --permanent --region $region
            Write-Host "SUCCESS: Password updated" -ForegroundColor Green
        } catch {
            Write-Host "WARNING: Could not update password - $($_.Exception.Message)" -ForegroundColor Yellow
        }
        
    } else {
        Write-Host "User does not exist. Creating new user..." -ForegroundColor Cyan
        
        # Create new user
        $createResult = aws cognito-idp admin-create-user --user-pool-id $userPoolId --username $TestUserEmail --user-attributes Name=email,Value=$TestUserEmail Name=email_verified,Value=true --temporary-password $TestUserPassword --message-action SUPPRESS --region $region --output json
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "SUCCESS: User created" -ForegroundColor Green
            
            # Set permanent password
            Write-Host "Setting permanent password..." -ForegroundColor Cyan
            aws cognito-idp admin-set-user-password --user-pool-id $userPoolId --username $TestUserEmail --password $TestUserPassword --permanent --region $region
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host "SUCCESS: Password set" -ForegroundColor Green
            } else {
                Write-Host "WARNING: Could not set permanent password" -ForegroundColor Yellow
            }
        } else {
            Write-Host "FAILED: Could not create user" -ForegroundColor Red
            Write-Host "Error: $createResult" -ForegroundColor Yellow
            exit 1
        }
    }
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== User Setup Complete ===" -ForegroundColor Green
Write-Host ""
Write-Host "Now you can test authentication with:" -ForegroundColor Cyan
Write-Host ".\simple-auth-test.ps1 -TestUserEmail '$TestUserEmail' -TestUserPassword '$TestUserPassword'" -ForegroundColor Gray