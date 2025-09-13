# Pre-deployment Check Runner (PowerShell)
# Runs comprehensive pre-deployment validation

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "AWS Amplify Pre-deployment Check" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Node.js is available
try {
    $nodeVersion = node --version 2>$null
    Write-Host "Node.js version: $nodeVersion" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Node.js is not installed or not in PATH" -ForegroundColor Red
    Write-Host "Please install Node.js to run the pre-deployment checker" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

# Run the pre-deployment checker
Write-Host "Running pre-deployment checks..." -ForegroundColor Yellow
Write-Host ""

try {
    node pre-deployment-checker.js --verbose
    $exitCode = $LASTEXITCODE
    
    if ($exitCode -eq 0) {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "Pre-deployment check PASSED" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "Your application is ready for deployment!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Next steps:" -ForegroundColor Cyan
        Write-Host "1. Commit your changes to Git" -ForegroundColor White
        Write-Host "2. Push to your repository" -ForegroundColor White
        Write-Host "3. Deploy using AWS Amplify" -ForegroundColor White
        Write-Host ""
    } else {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Red
        Write-Host "Pre-deployment check FAILED" -ForegroundColor Red
        Write-Host "========================================" -ForegroundColor Red
        Write-Host "Please review the issues above and fix them before deploying." -ForegroundColor Red
        Write-Host ""
        Write-Host "Common fixes:" -ForegroundColor Yellow
        Write-Host "- Ensure all required files exist" -ForegroundColor White
        Write-Host "- Fix amplify.yml syntax errors" -ForegroundColor White
        Write-Host "- Install missing dependencies" -ForegroundColor White
        Write-Host "- Verify build commands work" -ForegroundColor White
        Write-Host ""
    }
} catch {
    Write-Host "ERROR: Failed to run pre-deployment checker" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}

Write-Host "Press any key to continue..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")