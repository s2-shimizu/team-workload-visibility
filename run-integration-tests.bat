@echo off
REM Integration Test Suite Runner for Windows
REM Runs comprehensive integration tests for Amplify deployment

echo ========================================
echo Integration Test Suite Runner
echo ========================================
echo.

REM Check if Node.js is available
node --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Node.js is not installed or not in PATH
    echo Please install Node.js to run integration tests
    pause
    exit /b 1
)

echo Node.js version:
node --version
echo.

REM Set default environment variables if not provided
if "%FRONTEND_URL%"=="" (
    echo WARNING: FRONTEND_URL environment variable not set
    echo You can set it with: set FRONTEND_URL=https://your-amplify-app.amplifyapp.com
    echo.
)

if "%API_URL%"=="" (
    echo WARNING: API_URL environment variable not set
    echo You can set it with: set API_URL=https://your-api-gateway-url.amazonaws.com
    echo.
)

if "%AMPLIFY_APP_ID%"=="" (
    echo WARNING: AMPLIFY_APP_ID environment variable not set
    echo You can set it with: set AMPLIFY_APP_ID=your-amplify-app-id
    echo.
)

REM Parse command line arguments
set SKIP_DEPLOYMENT=false
set PERFORMANCE_THRESHOLD=3000
set LOAD_TEST_DURATION=30
set LOAD_TEST_CONCURRENCY=10
set RUN_TESTS=false

:parse_args
if "%1"=="" goto end_parse
if "%1"=="--skip-deployment" (
    set SKIP_DEPLOYMENT=true
    shift
    goto parse_args
)
if "%1"=="--performance-threshold" (
    set PERFORMANCE_THRESHOLD=%2
    shift
    shift
    goto parse_args
)
if "%1"=="--load-test-duration" (
    set LOAD_TEST_DURATION=%2
    shift
    shift
    goto parse_args
)
if "%1"=="--load-test-concurrency" (
    set LOAD_TEST_CONCURRENCY=%2
    shift
    shift
    goto parse_args
)
if "%1"=="--test" (
    set RUN_TESTS=true
    shift
    goto parse_args
)
if "%1"=="--no-pause" (
    shift
    goto parse_args
)
if "%1"=="--help" (
    echo.
    echo Usage: run-integration-tests.bat [options]
    echo.
    echo Options:
    echo   --skip-deployment           Skip test deployment phase
    echo   --performance-threshold N   Set performance threshold in milliseconds (default: 3000)
    echo   --load-test-duration N      Set load test duration in seconds (default: 30)
    echo   --load-test-concurrency N   Set number of concurrent users (default: 10)
    echo   --test                      Run integration test suite tests instead of actual integration tests
    echo   --help                      Show this help message
    echo.
    echo Environment Variables:
    echo   FRONTEND_URL               Frontend URL to test (required for actual tests)
    echo   API_URL                   API URL to test (optional)
    echo   AMPLIFY_APP_ID            Amplify App ID (optional)
    echo.
    echo Examples:
    echo   run-integration-tests.bat
    echo   run-integration-tests.bat --skip-deployment
    echo   run-integration-tests.bat --performance-threshold 5000 --load-test-duration 60
    echo   run-integration-tests.bat --test
    echo.
    pause
    exit /b 0
)
shift
goto parse_args

:end_parse

echo Configuration:
echo   Skip deployment: %SKIP_DEPLOYMENT%
echo   Performance threshold: %PERFORMANCE_THRESHOLD%ms
echo   Load test duration: %LOAD_TEST_DURATION%s
echo   Load test concurrency: %LOAD_TEST_CONCURRENCY%
echo   Run tests mode: %RUN_TESTS%
echo.

REM Check if required files exist
if not exist "integration-test-suite.js" (
    echo ERROR: integration-test-suite.js not found
    echo Please ensure you are running this script from the correct directory
    pause
    exit /b 1
)

if "%RUN_TESTS%"=="true" (
    if not exist "test-integration-suite.js" (
        echo ERROR: test-integration-suite.js not found
        echo Please ensure you are running this script from the correct directory
        pause
        exit /b 1
    )
)

REM Run integration tests or test suite tests
if "%RUN_TESTS%"=="true" (
    echo Running integration test suite tests...
    echo =====================================
    echo.
    node test-integration-suite.js
    set TEST_EXIT_CODE=%errorlevel%
) else (
    echo Running integration test suite...
    echo ===============================
    echo.
    
    REM Build command arguments
    set ARGS=
    if "%FRONTEND_URL%" neq "" set ARGS=%ARGS% --frontend-url "%FRONTEND_URL%"
    if "%API_URL%" neq "" set ARGS=%ARGS% --api-url "%API_URL%"
    if "%AMPLIFY_APP_ID%" neq "" set ARGS=%ARGS% --amplify-app-id "%AMPLIFY_APP_ID%"
    if "%SKIP_DEPLOYMENT%"=="true" set ARGS=%ARGS% --skip-deployment
    set ARGS=%ARGS% --performance-threshold %PERFORMANCE_THRESHOLD%
    set ARGS=%ARGS% --load-test-duration %LOAD_TEST_DURATION%
    set ARGS=%ARGS% --load-test-concurrency %LOAD_TEST_CONCURRENCY%
    
    REM Run the integration test suite
    node integration-test-suite.js %ARGS%
    set TEST_EXIT_CODE=%errorlevel%
)

echo.
echo ========================================

REM Check results
if %TEST_EXIT_CODE% equ 0 (
    echo SUCCESS: Integration tests completed successfully!
    echo.
    if "%RUN_TESTS%"=="true" (
        echo All integration test suite tests passed.
        if exist "integration-test-suite-test-report.json" (
            echo Detailed test report: integration-test-suite-test-report.json
        )
    ) else (
        echo All integration test phases passed.
        if exist "integration-test-report.json" (
            echo Detailed integration report: integration-test-report.json
        )
        if exist "deployment-verification-report.json" (
            echo Deployment verification report: deployment-verification-report.json
        )
    )
) else (
    echo FAILURE: Integration tests failed!
    echo.
    if "%RUN_TESTS%"=="true" (
        echo Some integration test suite tests failed.
        echo Check the output above for details.
    ) else (
        echo Some integration test phases failed.
        echo Check the integration-test-report.json for detailed information.
    )
    echo.
    echo Common issues:
    echo - Frontend URL not accessible
    echo - API URL not responding
    echo - Performance thresholds not met
    echo - Load test failures
    echo - Production verification issues
)

echo.
echo Integration test execution completed.
echo ========================================

REM Pause only if running interactively (not from another script)
if /i "%1" neq "--no-pause" (
    pause
)

exit /b %TEST_EXIT_CODE%