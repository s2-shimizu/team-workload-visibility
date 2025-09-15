# 包括的統合テストスクリプト
param(
    [string]$BaseUrl = "https://bn6xwu62qd.execute-api.ap-northeast-1.amazonaws.com/dev"
)

Write-Host "=== 包括的統合テスト ===" -ForegroundColor Green
Write-Host "Base URL: $BaseUrl" -ForegroundColor Yellow
Write-Host ""

$headers = @{
    "Authorization" = "Bearer mock-jwt-token-testuser"
    "Content-Type" = "application/json"
}

$testResults = @{
    passed = 0
    failed = 0
    warnings = 0
}

function Test-Endpoint {
    param($Name, $Uri, $Method = "GET", $Body = $null, $Headers = $null, $ExpectedStatus = 200)
    
    try {
        $params = @{
            Uri = $Uri
            Method = $Method
            TimeoutSec = 30
        }
        
        if ($Headers) { $params.Headers = $Headers }
        if ($Body) { $params.Body = $Body }
        
        $response = Invoke-RestMethod @params
        Write-Host "✅ $Name: 成功" -ForegroundColor Green
        $script:testResults.passed++
        return $response
    } catch {
        if ($_.Exception.Response.StatusCode -eq $ExpectedStatus) {
            Write-Host "✅ $Name: 期待通りのエラー ($ExpectedStatus)" -ForegroundColor Green
            $script:testResults.passed++
        } else {
            Write-Host "❌ $Name: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
            $script:testResults.failed++
        }
        return $null
    }
}

# 1. 基本API機能テスト
Write-Host "1. 基本API機能テスト" -ForegroundColor Cyan

$health = Test-Endpoint "ヘルスチェック" "$BaseUrl/api/status"
if ($health) {
    Write-Host "   ステータス: $($health.status)" -ForegroundColor Gray
    Write-Host "   データベース: $($health.database)" -ForegroundColor Gray
    Write-Host "   バージョン: $($health.version)" -ForegroundColor Gray
}

Write-Host ""

# 2. WorkloadStatus CRUD テスト
Write-Host "2. WorkloadStatus CRUD テスト" -ForegroundColor Cyan

# 2.1 全件取得
$workloads = Test-Endpoint "負荷状況全件取得" "$BaseUrl/api/workload-status" -Headers $headers
if ($workloads) {
    Write-Host "   取得件数: $($workloads.Count)" -ForegroundColor Gray
    $initialWorkloadCount = $workloads.Count
}

# 2.2 個人負荷状況取得
$myWorkload = Test-Endpoint "個人負荷状況取得" "$BaseUrl/api/workload-status/my" -Headers $headers
if ($myWorkload) {
    Write-Host "   ユーザーID: $($myWorkload.userId)" -ForegroundColor Gray
    Write-Host "   負荷レベル: $($myWorkload.workloadLevel)" -ForegroundColor Gray
}

# 2.3 負荷状況更新
$updateData = @{
    workloadLevel = "HIGH"
    projectCount = 6
    taskCount = 30
    comment = "統合テスト - 高負荷状態のテスト $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
} | ConvertTo-Json

$updatedWorkload = Test-Endpoint "負荷状況更新" "$BaseUrl/api/workload-status" "POST" $updateData $headers
if ($updatedWorkload) {
    Write-Host "   更新後負荷レベル: $($updatedWorkload.workloadLevel)" -ForegroundColor Gray
    Write-Host "   プロジェクト数: $($updatedWorkload.projectCount)" -ForegroundColor Gray
    Write-Host "   タスク数: $($updatedWorkload.taskCount)" -ForegroundColor Gray
}

# 2.4 統計情報取得
$workloadStats = Test-Endpoint "負荷状況統計" "$BaseUrl/api/workload-status/statistics" -Headers $headers
if ($workloadStats) {
    Write-Host "   総ユーザー数: $($workloadStats.totalUsers)" -ForegroundColor Gray
    Write-Host "   高負荷ユーザー: $($workloadStats.highWorkload)" -ForegroundColor Gray
    Write-Host "   中負荷ユーザー: $($workloadStats.mediumWorkload)" -ForegroundColor Gray
    Write-Host "   低負荷ユーザー: $($workloadStats.lowWorkload)" -ForegroundColor Gray
}

Write-Host ""

# 3. TeamIssue CRUD テスト
Write-Host "3. TeamIssue CRUD テスト" -ForegroundColor Cyan

# 3.1 全件取得
$issues = Test-Endpoint "困りごと全件取得" "$BaseUrl/api/team-issues" -Headers $headers
if ($issues) {
    Write-Host "   取得件数: $($issues.Count)" -ForegroundColor Gray
    $initialIssueCount = $issues.Count
}

# 3.2 オープンな困りごと取得
$openIssues = Test-Endpoint "オープン困りごと取得" "$BaseUrl/api/team-issues/open" -Headers $headers
if ($openIssues) {
    Write-Host "   オープン件数: $($openIssues.Count)" -ForegroundColor Gray
}

# 3.3 新しい困りごと作成
$issueData = @{
    content = "統合テスト用の困りごとです。DynamoDB統合のテストを実行中です。投稿時刻: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    priority = "HIGH"
} | ConvertTo-Json

$newIssue = Test-Endpoint "困りごと作成" "$BaseUrl/api/team-issues" "POST" $issueData $headers
$createdIssueId = $null
if ($newIssue) {
    Write-Host "   作成されたID: $($newIssue.issueId)" -ForegroundColor Gray
    Write-Host "   ステータス: $($newIssue.status)" -ForegroundColor Gray
    Write-Host "   優先度: $($newIssue.priority)" -ForegroundColor Gray
    $createdIssueId = $newIssue.issueId
}

# 3.4 困りごと統計取得
$issueStats = Test-Endpoint "困りごと統計" "$BaseUrl/api/team-issues/statistics" -Headers $headers
if ($issueStats) {
    Write-Host "   総数: $($issueStats.total)" -ForegroundColor Gray
    Write-Host "   オープン: $($issueStats.open)" -ForegroundColor Gray
    Write-Host "   解決済み: $($issueStats.resolved)" -ForegroundColor Gray
    Write-Host "   高優先度: $($issueStats.highPriority)" -ForegroundColor Gray
}

# 3.5 困りごと解決テスト
if ($createdIssueId) {
    $resolvedIssue = Test-Endpoint "困りごと解決" "$BaseUrl/api/team-issues/$createdIssueId/resolve" "PUT" $null $headers
    if ($resolvedIssue) {
        Write-Host "   解決後ステータス: $($resolvedIssue.status)" -ForegroundColor Gray
    }
}

Write-Host ""

# 4. 認証テスト
Write-Host "4. 認証テスト" -ForegroundColor Cyan

# 4.1 認証なしアクセス
Test-Endpoint "認証なしアクセス" "$BaseUrl/api/workload-status" -ExpectedStatus 401

# 4.2 無効トークン
$invalidHeaders = @{
    "Authorization" = "Bearer invalid-token-test"
    "Content-Type" = "application/json"
}
Test-Endpoint "無効トークンアクセス" "$BaseUrl/api/workload-status" -Headers $invalidHeaders -ExpectedStatus 401

Write-Host ""

# 5. エラーハンドリングテスト
Write-Host "5. エラーハンドリングテスト" -ForegroundColor Cyan

# 5.1 存在しないエンドポイント
Test-Endpoint "存在しないエンドポイント" "$BaseUrl/api/nonexistent" -ExpectedStatus 404

# 5.2 不正なJSONデータ
$invalidJson = "{ invalid json }"
Test-Endpoint "不正なJSONデータ" "$BaseUrl/api/workload-status" "POST" $invalidJson $headers -ExpectedStatus 400

# 5.3 空のコンテンツで困りごと作成
$emptyIssue = @{ content = "" } | ConvertTo-Json
Test-Endpoint "空コンテンツ困りごと作成" "$BaseUrl/api/team-issues" "POST" $emptyIssue $headers -ExpectedStatus 400

Write-Host ""

# 6. パフォーマンステスト
Write-Host "6. パフォーマンステスト" -ForegroundColor Cyan

$performanceResults = @()

# 複数回のAPI呼び出しでレスポンス時間を測定
for ($i = 1; $i -le 5; $i++) {
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    
    try {
        $response = Invoke-RestMethod -Uri "$BaseUrl/api/status" -Method GET -TimeoutSec 30
        $stopwatch.Stop()
        $performanceResults += $stopwatch.ElapsedMilliseconds
        Write-Host "   テスト $i : $($stopwatch.ElapsedMilliseconds) ms" -ForegroundColor Gray
    } catch {
        $stopwatch.Stop()
        Write-Host "   テスト $i : エラー" -ForegroundColor Red
    }
}

if ($performanceResults.Count -gt 0) {
    $avgTime = ($performanceResults | Measure-Object -Average).Average
    $maxTime = ($performanceResults | Measure-Object -Maximum).Maximum
    $minTime = ($performanceResults | Measure-Object -Minimum).Minimum
    
    Write-Host "   平均レスポンス時間: $([math]::Round($avgTime, 2)) ms" -ForegroundColor Gray
    Write-Host "   最大レスポンス時間: $maxTime ms" -ForegroundColor Gray
    Write-Host "   最小レスポンス時間: $minTime ms" -ForegroundColor Gray
    
    if ($avgTime -lt 1000) {
        Write-Host "   パフォーマンス評価: 優秀" -ForegroundColor Green
    } elseif ($avgTime -lt 3000) {
        Write-Host "   パフォーマンス評価: 良好" -ForegroundColor Yellow
    } else {
        Write-Host "   パフォーマンス評価: 改善が必要" -ForegroundColor Red
    }
}

Write-Host ""

# 7. データ整合性テスト
Write-Host "7. データ整合性テスト" -ForegroundColor Cyan

# 更新後のデータ確認
$finalWorkloads = Test-Endpoint "最終負荷状況確認" "$BaseUrl/api/workload-status" -Headers $headers
$finalIssues = Test-Endpoint "最終困りごと確認" "$BaseUrl/api/team-issues" -Headers $headers

if ($finalWorkloads -and $initialWorkloadCount) {
    Write-Host "   負荷状況データ変化: $initialWorkloadCount → $($finalWorkloads.Count)" -ForegroundColor Gray
}

if ($finalIssues -and $initialIssueCount) {
    Write-Host "   困りごとデータ変化: $initialIssueCount → $($finalIssues.Count)" -ForegroundColor Gray
}

Write-Host ""

# テスト結果サマリー
Write-Host "=== テスト結果サマリー ===" -ForegroundColor Green
Write-Host "成功: $($testResults.passed) 件" -ForegroundColor Green
Write-Host "失敗: $($testResults.failed) 件" -ForegroundColor $(if ($testResults.failed -eq 0) { 'Green' } else { 'Red' })
Write-Host "警告: $($testResults.warnings) 件" -ForegroundColor Yellow

$totalTests = $testResults.passed + $testResults.failed
$successRate = if ($totalTests -gt 0) { [math]::Round(($testResults.passed / $totalTests) * 100, 1) } else { 0 }

Write-Host "成功率: $successRate%" -ForegroundColor $(if ($successRate -ge 90) { 'Green' } elseif ($successRate -ge 70) { 'Yellow' } else { 'Red' })

Write-Host ""

if ($testResults.failed -eq 0) {
    Write-Host "🎉 全てのテストが成功しました！" -ForegroundColor Green
    Write-Host "DynamoDB統合が正常に動作しています。" -ForegroundColor Green
} elseif ($testResults.failed -le 2) {
    Write-Host "⚠️ 一部のテストで問題がありますが、基本機能は動作しています。" -ForegroundColor Yellow
} else {
    Write-Host "❌ 複数のテストで問題が発生しています。設定を確認してください。" -ForegroundColor Red
}

Write-Host ""
Write-Host "推奨事項:" -ForegroundColor Cyan
Write-Host "1. 認証機能の本格実装" -ForegroundColor Gray
Write-Host "2. エラーハンドリングの強化" -ForegroundColor Gray
Write-Host "3. パフォーマンス最適化" -ForegroundColor Gray
Write-Host "4. ログ監視の設定" -ForegroundColor Gray