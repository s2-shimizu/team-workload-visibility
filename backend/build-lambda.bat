@echo off
echo Building Lambda-optimized JAR for AWS deployment...

REM Set environment variables for Lambda build
set JAVA_OPTS=-Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=100
set MAVEN_OPTS=-Xmx1024m -XX:+UseG1GC

echo Cleaning previous builds...
call mvnw.cmd clean

echo Compiling and packaging for Lambda...
call mvnw.cmd package -Plambda -DskipTests -Dspring.profiles.active=lambda

echo Verifying Lambda JAR creation...
if exist "target\*-lambda.jar" (
    echo Lambda JAR created successfully!
    dir target\*-lambda.jar
    echo.
    echo JAR file details:
    for %%f in (target\*-lambda.jar) do (
        echo File: %%f
        echo Size: %%~zf bytes
    )
) else (
    echo ERROR: Lambda JAR not found!
    exit /b 1
)

echo.
echo Lambda build completed successfully!
echo The Lambda-ready JAR is available in the target directory.