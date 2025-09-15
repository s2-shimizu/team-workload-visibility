# DynamoDB統合テストスクリプト
param(
    [string]$BaseUrl = "https://bn6xwu62qd.execute-api.ap-northeast-1.amazonaws.com/dev",
    [string]$Environment = "dev"
)

Write-Host "=== DynamoDB統合テスト ===" -ForegroundColor Green
Write-Host "Base URL: $BaseUrl" -ForegroundColor Yellow
Write-Host "Environment: $Environment" -ForegroundColor Yellow
Write-Host ""

# 1. ヘルスチェック
Write-Host "1. ヘルスチェック" -ForegroundColor Cyan
try {
    $health = Invoke-RestMethod -Uri "$BaseUrl/api/status" -Method GET -TimeoutSec 30
    Write-Host "✅ ヘルスチェック: 成功" -ForegroundColor Green
    Write-Host "   データベース: $($health.database)" -ForegroundColor Gray
    Write-Host "   バージョン: $($health.version)" -ForegroundColor Gray
} catch {
    Write-Host "❌ ヘルスチェック: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 2. WorkloadStatus API テスト
Write-Host "2. WorkloadStatus API テスト" -ForegroundColor Cyan

# 全ての負荷状況を取得
Write-Host "2.1 全ての負荷状況を取得" -ForegroundColor Yellow
try {
    $allWorkloads = Invoke-RestMethod -Uri "$BaseUrl/api/workload-status" -Method GET -TimeoutSec 30
    Write-Host "✅ 負荷状況取得: 成功" -ForegroundColor Green
    Write-Host "   取得件数: $($allWorkloads.Count)" -ForegroundColor Gray
    
    if ($allWorkloads.Count -gt 0) {
        Write-Host "   サンプルデータ:" -ForegroundColor Gray
        $sample = $allWorkloads[0]
        Write-Host "     ユーザー: $($sample.displayName)" -ForegroundColor Gray
        Write-Host "     負荷レベル: $($sample.workloadLevel)" -ForegroundColor Gray
        Write-Host "     プロジェクト数: $($sample.projectCount)" -ForegroundColor Gray
    }
} catch {
    Write-Host "❌ 負荷状況取得: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

# 統計情報を取得
Write-Host "2.2 負荷状況統計を取得" -ForegroundColor Yellow
try {
    $workloadStats = Invoke-RestMethod -Uri "$BaseUrl/api/workload-status/statistics" -Method GET -TimeoutSec 30
    Write-Host "✅ 負荷状況統計: 成功" -ForegroundColor Green
    Write-Host "   総ユーザー数: $($workloadStats.totalUsers)" -ForegroundColor Gray
    Write-Host "   高負荷: $($workloadStats.highWorkload)" -ForegroundColor Gray
    Write-Host "   中負荷: $($workloadStats.mediumWorkload)" -ForegroundColor Gray
    Write-Host "   低負荷: $($workloadStats.lowWorkload)" -ForegroundColor Gray
    
    if ($workloadStats.error) {
        Write-Host "   ⚠️ エラー情報: $($workloadStats.error)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ 負荷状況統計: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

# 個人の負荷状況を取得
Write-Host "2.3 個人の負荷状況を取得" -ForegroundColor Yellow
try {
    $myWorkload = Invoke-RestMethod -Uri "$BaseUrl/api/workload-status/my" -Method GET -TimeoutSec 30
    Write-Host "✅ 個人負荷状況: 成功" -ForegroundColor Green
    Write-Host "   ユーザーID: $($myWorkload.userId)" -ForegroundColor Gray
    Write-Host "   負荷レベル: $($myWorkload.workloadLevel)" -ForegroundColor Gray
} catch {
    Write-Host "❌ 個人負荷状況: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

# 負荷状況を更新
Write-Host "2.4 負荷状況を更新" -ForegroundColor Yellow
$workloadUpdateData = @{
    workloadLevel = "HIGH"
    projectCount = 4
    taskCount = 20
    comment = "DynamoDB統合テストからの更新 - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
} | ConvertTo-Json

$headers = @{
    "Content-Type" = "application/json"
}

try {
    $updatedWorkload = Invoke-RestMethod -Uri "$BaseUrl/api/workload-status" -Method POST -Body $workloadUpdateData -Headers $headers -TimeoutSec 30
    Write-Host "✅ 負荷状況更新: 成功" -ForegroundColor Green
    Write-Host "   更新メッセージ: $($updatedWorkload.message)" -ForegroundColor Gray
    Write-Host "   新しい負荷レベル: $($updatedWorkload.workloadLevel)" -ForegroundColor Gray
    
    if ($updatedWorkload.error) {
        Write-Host "   ⚠️ エラー情報: $($updatedWorkload.error)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ 負荷状況更新: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 3. TeamIssue API テスト
Write-Host "3. TeamIssue API テスト" -ForegroundColor Cyan

# 全ての困りごとを取得
Write-Host "3.1 全ての困りごとを取得" -ForegroundColor Yellow
try {
    $allIssues = Invoke-RestMethod -Uri "$BaseUrl/api/team-issues" -Method GET -TimeoutSec 30
    Write-Host "✅ 困りごと取得: 成功" -ForegroundColor Green
    Write-Host "   取得件数: $($allIssues.Count)" -ForegroundColor Gray
    
    if ($allIssues.Count -gt 0) {
        Write-Host "   サンプルデータ:" -ForegroundColor Gray
        $sample = $allIssues[0]
        Write-Host "     ID: $($sample.issueId)" -ForegroundColor Gray
        Write-Host "     ユーザー: $($sample.displayName)" -ForegroundColor Gray
        Write-Host "     ステータス: $($sample.status)" -ForegroundColor Gray
        Write-Host "     優先度: $($sample.priority)" -ForegroundColor Gray
    }
} catch {
    Write-Host "❌ 困りごと取得: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

# オープンな困りごとを取得
Write-Host "3.2 オープンな困りごとを取得" -ForegroundColor Yellow
try {
    $openIssues = Invoke-RestMethod -Uri "$BaseUrl/api/team-issues/open" -Method GET -TimeoutSec 30
    Write-Host "✅ オープン困りごと取得: 成功" -ForegroundColor Green
    Write-Host "   オープン件数: $($openIssues.Count)" -ForegroundColor Gray
} catch {
    Write-Host "❌ オープン困りごと取得: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

# 困りごと統計を取得
Write-Host "3.3 困りごと統計を取得" -ForegroundColor Yellow
try {
    $issueStats = Invoke-RestMethod -Uri "$BaseUrl/api/team-issues/statistics" -Method GET -TimeoutSec 30
    Write-Host "✅ 困りごと統計: 成功" -ForegroundColor Green
    Write-Host "   総数: $($issueStats.total)" -ForegroundColor Gray
    Write-Host "   オープン: $($issueStats.open)" -ForegroundColor Gray
    Write-Host "   解決済み: $($issueStats.resolved)" -ForegroundColor Gray
    Write-Host "   高優先度: $($issueStats.highPriority)" -ForegroundColor Gray
    
    if ($issueStats.error) {
        Write-Host "   ⚠️ エラー情報: $($issueStats.error)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ 困りごと統計: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

# 新しい困りごとを投稿
Write-Host "3.4 新しい困りごとを投稿" -ForegroundColor Yellow
$issueData = @{
    content = "DynamoDB統合テストからの困りごと投稿です。投稿時刻: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    priority = "MEDIUM"
} | ConvertTo-Json

try {
    $newIssue = Invoke-RestMethod -Uri "$BaseUrl/api/team-issues" -Method POST -Body $issueData -Headers $headers -TimeoutSec 30
    Write-Host "✅ 困りごと投稿: 成功" -ForegroundColor Green
    Write-Host "   投稿メッセージ: $($newIssue.message)" -ForegroundColor Gray
    Write-Host "   新しいID: $($newIssue.issueId)" -ForegroundColor Gray
    
    if ($newIssue.error) {
        Write-Host "   ⚠️ エラー情報: $($newIssue.error)" -ForegroundColor Yellow
    }
    
    # 作成された困りごとのIDを保存（後で解決テストに使用）
    $createdIssueId = $newIssue.issueId
} catch {
    Write-Host "❌ 困りごと投稿: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
    $createdIssueId = $null
}

# 困りごとを解決（作成されたIDがある場合）
if ($createdIssueId -and $createdIssueId -ne "issue-" + (Get-Date).Ticks) {
    Write-Host "3.5 困りごとを解決" -ForegroundColor Yellow
    try {
        $resolvedIssue = Invoke-RestMethod -Uri "$BaseUrl/api/team-issues/$createdIssueId/resolve" -Method PUT -Headers $headers -TimeoutSec 30
        Write-Host "✅ 困りごと解決: 成功" -ForegroundColor Green
        Write-Host "   解決メッセージ: $($resolvedIssue.message)" -ForegroundColor Gray
        Write-Host "   新しいステータス: $($resolvedIssue.status)" -ForegroundColor Gray
    } catch {
        Write-Host "❌ 困りごと解決: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""

# 4. データ整合性チェック
Write-Host "4. データ整合性チェック" -ForegroundColor Cyan

try {
    # 更新後の統計を再取得
    $finalWorkloadStats = Invoke-RestMethod -Uri "$BaseUrl/api/workload-status/statistics" -Method GET -TimeoutSec 30
    $finalIssueStats = Invoke-RestMethod -Uri "$BaseUrl/api/team-issues/statistics" -Method GET -TimeoutSec 30
    
    Write-Host "✅ データ整合性チェック: 完了" -ForegroundColor Green
    Write-Host "   最終負荷状況統計:" -ForegroundColor Gray
    Write-Host "     総ユーザー数: $($finalWorkloadStats.totalUsers)" -ForegroundColor Gray
    Write-Host "   最終困りごと統計:" -ForegroundColor Gray
    Write-Host "     総数: $($finalIssueStats.total)" -ForegroundColor Gray
    Write-Host "     オープン: $($finalIssueStats.open)" -ForegroundColor Gray
    Write-Host "     解決済み: $($finalIssueStats.resolved)" -ForegroundColor Gray
} catch {
    Write-Host "❌ データ整合性チェック: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== DynamoDB統合テスト完了 ===" -ForegroundColor Green
Write-Host ""
Write-Host "使用方法:" -ForegroundColor Cyan
Write-Host "  開発環境: .\test-dynamodb-integration.ps1" -ForegroundColor Gray
Write-Host "  本番環境: .\test-dynamodb-integration.ps1 -BaseUrl 'https://prod-api.example.com' -Environment prod" -ForegroundColor Gray
Write-Host ""
Write-Host "注意事項:" -ForegroundColor Yellow
Write-Host "- DynamoDBテーブルが作成されている必要があります" -ForegroundColor Gray
Write-Host "- アプリケーションがDynamoDB統合版で動作している必要があります" -ForegroundColor Gray
Write-Host "- エラーが発生した場合はフォールバックモードで動作します" -ForegroundColor Gray