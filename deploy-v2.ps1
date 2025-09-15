# Deploy Team Dashboard V2
param(
    [string]$Environment = "dev"
)

Write-Host "=== Team Dashboard V2 Deploy ===" -ForegroundColor Green
Write-Host "Environment: $Environment" -ForegroundColor Yellow
Write-Host ""

# Build Maven project (skip tests)
Write-Host "1. Building Maven Project" -ForegroundColor Cyan
try {
    Set-Location backend
    
    Write-Host "Maven clean..." -ForegroundColor Gray
    mvn clean -q
    
    Write-Host "Maven package..." -ForegroundColor Gray
    mvn package -Plambda -DskipTests -q
    
    Write-Host "Maven build completed" -ForegroundColor Green
    
} catch {
    Write-Host "Maven build failed: $($_.Exception.Message)" -ForegroundColor Red
    Set-Location ..
    exit 1
} finally {
    Set-Location ..
}

Write-Host ""

# SAM Build
Write-Host "2. SAM Build" -ForegroundColor Cyan
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
Write-Host "3. SAM Deploy" -ForegroundColor Cyan
$stackName = "team-dashboard-v2-$Environment"

try {
    Write-Host "Deploying stack: $stackName" -ForegroundColor Gray
    sam deploy --stack-name $stackName --parameter-overrides Environment=$Environment --capabilities CAPABILITY_IAM --resolve-s3 --no-confirm-changeset
    
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
Write-Host "4. Getting Stack Outputs" -ForegroundColor Cyan
try {
    $stackInfo = aws cloudformation describe-stacks --stack-name $stackName --output json | ConvertFrom-Json
    
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
    Write-Host "Deployment Summary:" -ForegroundColor Cyan
    Write-Host "API Endpoint: $($script:ApiEndpoint)" -ForegroundColor Yellow
    if ($script:WorkloadTableName) {
        Write-Host "Workload Table: $($script:WorkloadTableName)" -ForegroundColor Yellow
    }
    if ($script:IssueTableName) {
        Write-Host "Issue Table: $($script:IssueTableName)" -ForegroundColor Yellow
    }
    
    Write-Host ""
    Write-Host "Next Steps:" -ForegroundColor Cyan
    Write-Host "1. Test the deployment:" -ForegroundColor Gray
    Write-Host "   .\simple-dynamodb-test.ps1 -BaseUrl '$($script:ApiEndpoint)'" -ForegroundColor Gray
    Write-Host ""
    Write-Host "2. Update frontend config:" -ForegroundColor Gray
    Write-Host "   Update frontend/js/aws-config.js endpoint to '$($script:ApiEndpoint)'" -ForegroundColor Gray
}