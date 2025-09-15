# Simple DynamoDB Integration Test
param(
    [string]$BaseUrl = "https://bn6xwu62qd.execute-api.ap-northeast-1.amazonaws.com/dev"
)

Write-Host "=== Simple DynamoDB Integration Test ===" -ForegroundColor Green
Write-Host "Base URL: $BaseUrl" -ForegroundColor Yellow
Write-Host ""

$headers = @{
    "Authorization" = "Bearer mock-jwt-token-testuser"
    "Content-Type" = "application/json"
}

# 1. Health Check
Write-Host "1. Health Check" -ForegroundColor Cyan
try {
    $health = Invoke-RestMethod -Uri "$BaseUrl/api/status" -Method GET -TimeoutSec 30
    Write-Host "SUCCESS: API is running" -ForegroundColor Green
    Write-Host "  Status: $($health.status)" -ForegroundColor Gray
    Write-Host "  Database: $($health.database)" -ForegroundColor Gray
    Write-Host "  Version: $($health.version)" -ForegroundColor Gray
} catch {
    Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 2. Get Workload Status
Write-Host "2. Get Workload Status" -ForegroundColor Cyan
try {
    $workloads = Invoke-RestMethod -Uri "$BaseUrl/api/workload-status" -Method GET -Headers $headers -TimeoutSec 30
    Write-Host "SUCCESS: Retrieved $($workloads.Count) workload records" -ForegroundColor Green
    
    if ($workloads.Count -gt 0) {
        $sample = $workloads[0]
        Write-Host "  Sample: $($sample.displayName) - $($sample.workloadLevel)" -ForegroundColor Gray
        
        if ($sample.error) {
            Write-Host "  WARNING: Fallback mode - $($sample.error)" -ForegroundColor Yellow
        } else {
            Write-Host "  SUCCESS: Data from DynamoDB" -ForegroundColor Green
        }
    }
} catch {
    Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 3. Update Workload Status
Write-Host "3. Update Workload Status" -ForegroundColor Cyan
$updateData = @{
    workloadLevel = "HIGH"
    projectCount = 4
    taskCount = 20
    comment = "DynamoDB integration test - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
} | ConvertTo-Json

try {
    $updated = Invoke-RestMethod -Uri "$BaseUrl/api/workload-status" -Method POST -Body $updateData -Headers $headers -TimeoutSec 30
    Write-Host "SUCCESS: Workload updated" -ForegroundColor Green
    Write-Host "  Message: $($updated.message)" -ForegroundColor Gray
    Write-Host "  Level: $($updated.workloadLevel)" -ForegroundColor Gray
    
    if ($updated.error) {
        Write-Host "  WARNING: Fallback mode - $($updated.error)" -ForegroundColor Yellow
    } else {
        Write-Host "  SUCCESS: Saved to DynamoDB" -ForegroundColor Green
    }
} catch {
    Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 4. Get Team Issues
Write-Host "4. Get Team Issues" -ForegroundColor Cyan
try {
    $issues = Invoke-RestMethod -Uri "$BaseUrl/api/team-issues" -Method GET -Headers $headers -TimeoutSec 30
    Write-Host "SUCCESS: Retrieved $($issues.Count) issue records" -ForegroundColor Green
    
    if ($issues.Count -gt 0) {
        $sample = $issues[0]
        Write-Host "  Sample: $($sample.issueId) - $($sample.status)" -ForegroundColor Gray
        
        if ($sample.error) {
            Write-Host "  WARNING: Fallback mode - $($sample.error)" -ForegroundColor Yellow
        } else {
            Write-Host "  SUCCESS: Data from DynamoDB" -ForegroundColor Green
        }
    }
} catch {
    Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 5. Create Team Issue
Write-Host "5. Create Team Issue" -ForegroundColor Cyan
$issueData = @{
    content = "DynamoDB integration test issue - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    priority = "MEDIUM"
} | ConvertTo-Json

try {
    $newIssue = Invoke-RestMethod -Uri "$BaseUrl/api/team-issues" -Method POST -Body $issueData -Headers $headers -TimeoutSec 30
    Write-Host "SUCCESS: Issue created" -ForegroundColor Green
    Write-Host "  ID: $($newIssue.issueId)" -ForegroundColor Gray
    Write-Host "  Message: $($newIssue.message)" -ForegroundColor Gray
    
    if ($newIssue.error) {
        Write-Host "  WARNING: Fallback mode - $($newIssue.error)" -ForegroundColor Yellow
    } else {
        Write-Host "  SUCCESS: Saved to DynamoDB" -ForegroundColor Green
    }
} catch {
    Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== Test Complete ===" -ForegroundColor Green
Write-Host ""
Write-Host "If you see 'Fallback mode' warnings, check:" -ForegroundColor Yellow
Write-Host "- AWS credentials are configured" -ForegroundColor Gray
Write-Host "- DynamoDB tables exist" -ForegroundColor Gray
Write-Host "- IAM permissions for DynamoDB" -ForegroundColor Gray
Write-Host "- Environment variables WORKLOAD_STATUS_TABLE and TEAM_ISSUE_TABLE" -ForegroundColor Gray