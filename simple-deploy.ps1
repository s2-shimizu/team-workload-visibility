# Simple SAM Deploy Script
param(
    [string]$Environment = "dev",
    [string]$StackName = "team-dashboard"
)

Write-Host "=== SAM Deploy ===" -ForegroundColor Green
Write-Host "Environment: $Environment" -ForegroundColor Yellow
Write-Host "Stack Name: $StackName-$Environment" -ForegroundColor Yellow
Write-Host ""

# Check prerequisites
Write-Host "1. Checking Prerequisites" -ForegroundColor Cyan

# Check SAM CLI
try {
    $samVersion = sam --version
    Write-Host "SAM CLI: $samVersion" -ForegroundColor Green
} catch {
    Write-Host "SAM CLI not found. Please install SAM CLI." -ForegroundColor Red
    exit 1
}

# Check AWS CLI
try {
    $awsVersion = aws --version
    Write-Host "AWS CLI: $awsVersion" -ForegroundColor Green
} catch {
    Write-Host "AWS CLI not found." -ForegroundColor Red
    exit 1
}

# Check AWS credentials
try {
    $identity = aws sts get-caller-identity --output json | ConvertFrom-Json
    Write-Host "AWS Account: $($identity.Account)" -ForegroundColor Green
} catch {
    Write-Host "AWS credentials not configured. Run 'aws configure'." -ForegroundColor Red
    exit 1
}

Write-Host ""

# Build Maven project
Write-Host "2. Building Maven Project" -ForegroundColor Cyan
try {
    Set-Location backend
    
    Write-Host "Maven clean..." -ForegroundColor Gray
    mvn clean -q
    
    Write-Host "Maven package..." -ForegroundColor Gray
    mvn package -Plambda -DskipTests -q
    
    $jarFiles = Get-ChildItem -Path "target" -Name "*.jar" | Where-Object { $_ -like "*lambda*" }
    if ($jarFiles.Count -gt 0) {
        Write-Host "Lambda JAR created: $($jarFiles[0])" -ForegroundColor Green
    }
    
} catch {
    Write-Host "Maven build failed: $($_.Exception.Message)" -ForegroundColor Red
    Set-Location ..
    exit 1
} finally {
    Set-Location ..
}

Write-Host ""

# SAM Build
Write-Host "3. SAM Build" -ForegroundColor Cyan
try {
    Write-Host "Running sam build..." -ForegroundColor Gray
    sam build
    if ($LASTEXITCODE -ne 0) {
        throw "SAM build failed"
    }
    Write-Host "SAM build completed" -ForegroundColor Green
} catch {
    Write-Host "SAM build error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""

# SAM Deploy
Write-Host "4. SAM Deploy" -ForegroundColor Cyan
try {
    Write-Host "Running sam deploy..." -ForegroundColor Gray
    sam deploy --stack-name "$StackName-$Environment" --parameter-overrides Environment=$Environment --capabilities CAPABILITY_IAM --resolve-s3 --no-confirm-changeset
    
    if ($LASTEXITCODE -ne 0) {
        throw "SAM deploy failed"
    }
    
    Write-Host "SAM deploy completed" -ForegroundColor Green
} catch {
    Write-Host "SAM deploy error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Get stack outputs
Write-Host "5. Getting Stack Outputs" -ForegroundColor Cyan
try {
    $stackInfo = aws cloudformation describe-stacks --stack-name "$StackName-$Environment" --output json | ConvertFrom-Json
    
    if ($stackInfo.Stacks.Count -gt 0) {
        $stack = $stackInfo.Stacks[0]
        Write-Host "Stack Status: $($stack.StackStatus)" -ForegroundColor Green
        
        if ($stack.Outputs) {
            Write-Host ""
            Write-Host "Stack Outputs:" -ForegroundColor Yellow
            foreach ($output in $stack.Outputs) {
                Write-Host "  $($output.OutputKey): $($output.OutputValue)" -ForegroundColor Gray
                
                if ($output.OutputKey -eq "ApiGatewayEndpoint") {
                    $script:ApiEndpoint = $output.OutputValue
                } elseif ($output.OutputKey -eq "WorkloadStatusTableName") {
                    $script:WorkloadTableName = $output.OutputValue
                } elseif ($output.OutputKey -eq "TeamIssueTableName") {
                    $script:IssueTableName = $output.OutputValue
                }
            }
        }
    }
} catch {
    Write-Host "Failed to get stack info: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Deploy Complete ===" -ForegroundColor Green

if ($script:ApiEndpoint) {
    Write-Host ""
    Write-Host "Next Steps:" -ForegroundColor Cyan
    Write-Host "1. Test the deployment:" -ForegroundColor Gray
    Write-Host "   .\test-deployed-stack.ps1 -ApiEndpoint '$($script:ApiEndpoint)' -WorkloadTable '$($script:WorkloadTableName)' -IssueTable '$($script:IssueTableName)'" -ForegroundColor Gray
    Write-Host ""
    Write-Host "2. Quick test:" -ForegroundColor Gray
    Write-Host "   .\simple-dynamodb-test.ps1 -BaseUrl '$($script:ApiEndpoint)'" -ForegroundColor Gray
    Write-Host ""
    Write-Host "3. Update frontend config:" -ForegroundColor Gray
    Write-Host "   Update frontend/js/aws-config.js endpoint to '$($script:ApiEndpoint)'" -ForegroundColor Gray
}