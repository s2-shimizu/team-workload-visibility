# Integration Test Script
param(
    [string]$BaseUrl = "https://bn6xwu62qd.execute-api.ap-northeast-1.amazonaws.com/dev"
)

Write-Host "=== Integration Test ===" -ForegroundColor Green
Write-Host "Base URL: $BaseUrl" -ForegroundColor Yellow
Write-Host ""

$headers = @{
    "Authorization" = "Bearer mock-jwt-token-testuser"
    "Content-Type" = "application/json"
}

$testsPassed = 0
$testsFailed = 0

function Test-API {
    param($Name, $Uri, $Method = "GET", $Body = $null, $Headers = $null)
    
    try {
        $params = @{
            Uri = $Uri
            Method = $Method
            TimeoutSec = 30
        }
        
        if ($Headers) { $params.Headers = $Headers }
        if ($Body) { $params.Body = $Body }
        
        $response = Invoke-RestMethod @params
        Write-Host "SUCCESS: $Name" -ForegroundColor Green
        $script:testsPassed++
        return $response
    } catch {
        Write-Host "FAILED: $Name - $($_.Exception.Message)" -ForegroundColor Red
        $script:testsFailed++
        return $null
    }
}

# 1. Basic API Tests
Write-Host "1. Basic API Tests" -ForegroundColor Cyan

$health = Test-API "Health Check" "$BaseUrl/api/status"
if ($health) {
    Write-Host "  Status: $($health.status)" -ForegroundColor Gray
    Write-Host "  Database: $($health.database)" -ForegroundColor Gray
}

Write-Host ""

# 2. WorkloadStatus Tests
Write-Host "2. WorkloadStatus Tests" -ForegroundColor Cyan

$workloads = Test-API "Get All Workloads" "$BaseUrl/api/workload-status" -Headers $headers
if ($workloads) {
    Write-Host "  Count: $($workloads.Count)" -ForegroundColor Gray
}

$myWorkload = Test-API "Get My Workload" "$BaseUrl/api/workload-status/my" -Headers $headers
if ($myWorkload) {
    Write-Host "  User ID: $($myWorkload.userId)" -ForegroundColor Gray
}

$updateData = @{
    workloadLevel = "HIGH"
    projectCount = 5
    taskCount = 25
    comment = "Integration test update - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
} | ConvertTo-Json

$updated = Test-API "Update Workload" "$BaseUrl/api/workload-status" "POST" $updateData $headers
if ($updated) {
    Write-Host "  Updated Level: $($updated.workloadLevel)" -ForegroundColor Gray
}

$stats = Test-API "Get Workload Stats" "$BaseUrl/api/workload-status/statistics" -Headers $headers
if ($stats) {
    Write-Host "  Total Users: $($stats.totalUsers)" -ForegroundColor Gray
    Write-Host "  High Workload: $($stats.highWorkload)" -ForegroundColor Gray
}

Write-Host ""

# 3. TeamIssue Tests
Write-Host "3. TeamIssue Tests" -ForegroundColor Cyan

$issues = Test-API "Get All Issues" "$BaseUrl/api/team-issues" -Headers $headers
if ($issues) {
    Write-Host "  Count: $($issues.Count)" -ForegroundColor Gray
}

$openIssues = Test-API "Get Open Issues" "$BaseUrl/api/team-issues/open" -Headers $headers
if ($openIssues) {
    Write-Host "  Open Count: $($openIssues.Count)" -ForegroundColor Gray
}

$issueData = @{
    content = "Integration test issue - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    priority = "MEDIUM"
} | ConvertTo-Json

$newIssue = Test-API "Create Issue" "$BaseUrl/api/team-issues" "POST" $issueData $headers
$createdIssueId = $null
if ($newIssue) {
    Write-Host "  Created ID: $($newIssue.issueId)" -ForegroundColor Gray
    $createdIssueId = $newIssue.issueId
}

$issueStats = Test-API "Get Issue Stats" "$BaseUrl/api/team-issues/statistics" -Headers $headers
if ($issueStats) {
    Write-Host "  Total: $($issueStats.total)" -ForegroundColor Gray
    Write-Host "  Open: $($issueStats.open)" -ForegroundColor Gray
    Write-Host "  Resolved: $($issueStats.resolved)" -ForegroundColor Gray
}

# Resolve the created issue
if ($createdIssueId) {
    $resolved = Test-API "Resolve Issue" "$BaseUrl/api/team-issues/$createdIssueId/resolve" "PUT" $null $headers
    if ($resolved) {
        Write-Host "  Resolved Status: $($resolved.status)" -ForegroundColor Gray
    }
}

Write-Host ""

# 4. Performance Test
Write-Host "4. Performance Test" -ForegroundColor Cyan

$times = @()
for ($i = 1; $i -le 3; $i++) {
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    
    try {
        $response = Invoke-RestMethod -Uri "$BaseUrl/api/status" -Method GET -TimeoutSec 30
        $stopwatch.Stop()
        $times += $stopwatch.ElapsedMilliseconds
        Write-Host "  Test $i : $($stopwatch.ElapsedMilliseconds) ms" -ForegroundColor Gray
    } catch {
        $stopwatch.Stop()
        Write-Host "  Test $i : Error" -ForegroundColor Red
    }
}

if ($times.Count -gt 0) {
    $avgTime = ($times | Measure-Object -Average).Average
    Write-Host "  Average Response Time: $([math]::Round($avgTime, 2)) ms" -ForegroundColor Gray
    
    if ($avgTime -lt 1000) {
        Write-Host "  Performance: Excellent" -ForegroundColor Green
    } elseif ($avgTime -lt 3000) {
        Write-Host "  Performance: Good" -ForegroundColor Yellow
    } else {
        Write-Host "  Performance: Needs Improvement" -ForegroundColor Red
    }
}

Write-Host ""

# Test Summary
Write-Host "=== Test Summary ===" -ForegroundColor Green
Write-Host "Passed: $testsPassed" -ForegroundColor Green
Write-Host "Failed: $testsFailed" -ForegroundColor $(if ($testsFailed -eq 0) { 'Green' } else { 'Red' })

$total = $testsPassed + $testsFailed
$successRate = if ($total -gt 0) { [math]::Round(($testsPassed / $total) * 100, 1) } else { 0 }
Write-Host "Success Rate: $successRate%" -ForegroundColor $(if ($successRate -ge 90) { 'Green' } elseif ($successRate -ge 70) { 'Yellow' } else { 'Red' })

Write-Host ""

if ($testsFailed -eq 0) {
    Write-Host "All tests passed! DynamoDB integration is working correctly." -ForegroundColor Green
} else {
    Write-Host "Some tests failed. Please check the configuration." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "1. Implement real authentication" -ForegroundColor Gray
Write-Host "2. Add monitoring and logging" -ForegroundColor Gray
Write-Host "3. Optimize performance" -ForegroundColor Gray
Write-Host "4. Deploy to production" -ForegroundColor Gray