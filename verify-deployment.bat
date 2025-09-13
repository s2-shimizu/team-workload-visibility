@echo off
REM Deployment Verification Script for Windows
REM 
REM This script verifies that the AWS Amplify deployment is working correctly
REM by checking frontend availability, static resources, and API endpoints.

setlocal enabledelayedexpansion

echo ========================================
echo AWS Amplify Deployment Verification
echo ========================================
echo.

REM Check if Node.js is available
node --version >nul 2>&1
if errorlevel 1 (
    echo Error: Node.js is not installed or not in PATH
    echo Please install Node.js from https://nodejs.org/
    pause
    exit /b 1
)

REM Check if verification script exists
if not exist "deployment-verification.js" (
    echo Error: deployment-verification.js not found
    echo Please ensure the script is in the current directory
    pause
    exit /b 1
)

REM Get URLs from user if not provided as environment variables
if "%FRONTEND_URL%"=="" (
    echo.
    echo Please enter your Amplify frontend URL:
    echo Example: https://main.d1234567890.amplifyapp.com
    set /p FRONTEND_URL="Frontend URL: "
)

if "%FRONTEND_URL%"=="" (
    echo Error: Frontend URL is required
    pause
    exit /b 1
)

if "%API_URL%"=="" (
    echo.
    echo Please enter your API Gateway URL (optional, press Enter to skip):
    echo Example: https://api123456.execute-api.us-east-1.amazonaws.com/prod
    set /p API_URL="API URL (optional): "
)

echo.
echo Starting verification with:
echo Frontend URL: %FRONTEND_URL%
if not "%API_URL%"=="" (
    echo API URL: %API_URL%
) else (
    echo API URL: Not specified (API checks will be skipped)
)
echo.

REM Run the verification
if not "%API_URL%"=="" (
    node deployment-verification.js --frontend-url "%FRONTEND_URL%" --api-url "%API_URL%"
) else (
    node deployment-verification.js --frontend-url "%FRONTEND_URL%"
)

set VERIFICATION_RESULT=%errorlevel%

echo.
echo ========================================
if %VERIFICATION_RESULT%==0 (
    echo ✅ Deployment verification completed successfully!
    echo Your application is ready to use.
) else (
    echo ❌ Deployment verification failed!
    echo Please check the errors above and fix any issues.
)
echo ========================================

REM Check if report file was generated
if exist "deployment-verification-report.json" (
    echo.
    echo 📄 Detailed report saved to: deployment-verification-report.json
)

echo.
pause
exit /b %VERIFICATION_RESULT%