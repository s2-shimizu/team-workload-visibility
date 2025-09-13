@echo off
REM Pre-deployment Check Runner
REM Runs comprehensive pre-deployment validation

echo ========================================
echo AWS Amplify Pre-deployment Check
echo ========================================
echo.

REM Check if Node.js is available
node --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Node.js is not installed or not in PATH
    echo Please install Node.js to run the pre-deployment checker
    pause
    exit /b 1
)

REM Run the pre-deployment checker
echo Running pre-deployment checks...
echo.

node pre-deployment-checker.js --verbose

REM Check the result
if errorlevel 1 (
    echo.
    echo ========================================
    echo Pre-deployment check FAILED
    echo ========================================
    echo Please review the issues above and fix them before deploying.
    echo.
    echo Common fixes:
    echo - Ensure all required files exist
    echo - Fix amplify.yml syntax errors
    echo - Install missing dependencies
    echo - Verify build commands work
    echo.
) else (
    echo.
    echo ========================================
    echo Pre-deployment check PASSED
    echo ========================================
    echo Your application is ready for deployment!
    echo.
    echo Next steps:
    echo 1. Commit your changes to Git
    echo 2. Push to your repository
    echo 3. Deploy using AWS Amplify
    echo.
)

echo Press any key to continue...
pause >nul