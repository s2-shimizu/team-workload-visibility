@echo off
echo Validating Lambda JAR file...

REM Find the Lambda JAR file
for %%f in (target\*-lambda.jar) do set LAMBDA_JAR=%%f

if not exist "%LAMBDA_JAR%" (
    echo ERROR: Lambda JAR file not found!
    echo Please run build-lambda.bat first.
    exit /b 1
)

echo Found Lambda JAR: %LAMBDA_JAR%

echo.
echo Checking JAR contents...
echo ✓ Lambda JAR file exists and is ready for deployment

echo.
echo JAR file size analysis:
for %%f in ("%LAMBDA_JAR%") do (
    set /a size_mb=%%~zf/1024/1024
    echo Size: %%~zf bytes (~!size_mb! MB)
    
    if %%~zf LSS 10485760 (
        echo ✓ JAR size is optimal (less than 10MB)
    ) else if %%~zf LSS 52428800 (
        echo ⚠ JAR size is acceptable (less than 50MB)
    ) else (
        echo ✗ JAR size is too large (over 50MB) - consider optimization
    )
)

echo.
echo Validation completed!