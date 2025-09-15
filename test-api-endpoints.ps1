# Team Dashboard API テストスクリプト
# API Gateway エンドポイントのテスト

$baseUrl = "https://bn6xwu62qd.execute-api.ap-northeast-1.amazonaws.com/dev"

Write-Host "=== Team Dashboard API テスト ===" -ForegroundColor Green
Write-Host "Base URL: $baseUrl" -ForegroundColor Yellow
Write-Host ""

# ヘルスチェック
Write-Host "1. ヘルスチェック" -ForegroundColor Cyan
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/health" -Method GET
    Write-Host "✅ ヘルスチェック成功" -ForegroundColor Green
    Write-Host "   Status: $($health.status)" -ForegroundColor White
    Write-Host "   Message: $($health.message)" -ForegroundColor White
} catch {
    Write-Host "❌ ヘルスチェック失敗: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# API Status
Write-Host "2. API Status" -ForegroundColor Cyan
try {
    $apiStatus = Invoke-RestMethod -Uri "$baseUrl/api/status" -Method GET
    Write-Host "✅ API Status成功" -ForegroundColor Green
    Write-Host "   Status: $($apiStatus.status)" -ForegroundColor White
} catch {
    Write-Host "❌ API Status失敗: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 負荷状況一覧取得
Write-Host "3. 負荷状況一覧取得" -ForegroundColor Cyan
try {
    $workloadStatuses = Invoke-RestMethod -Uri "$baseUrl/api/workload-status" -Method GET
    Write-Host "✅ 負荷状況一覧取得成功" -ForegroundColor Green
    Write-Host "   取得件数: $($workloadStatuses.Count)" -ForegroundColor White
    foreach ($status in $workloadStatuses) {
        Write-Host "   - $($status.displayName): $($status.workloadLevel) (プロジェクト: $($status.projectCount), タスク: $($status.taskCount))" -ForegroundColor White
    }
} catch {
    Write-Host "❌ 負荷状況一覧取得失敗: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 自分の負荷状況取得
Write-Host "4. 自分の負荷状況取得" -ForegroundColor Cyan
try {
    $myWorkload = Invoke-RestMethod -Uri "$baseUrl/api/workload-status/my" -Method GET
    Write-Host "✅ 自分の負荷状況取得成功" -ForegroundColor Green
    Write-Host "   ユーザー: $($myWorkload.displayName)" -ForegroundColor White
    Write-Host "   負荷レベル: $($myWorkload.workloadLevel)" -ForegroundColor White
} catch {
    Write-Host "❌ 自分の負荷状況取得失敗: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 困りごと一覧取得
Write-Host "5. 困りごと一覧取得" -ForegroundColor Cyan
try {
    $teamIssues = Invoke-RestMethod -Uri "$baseUrl/api/team-issues" -Method GET
    Write-Host "✅ 困りごと一覧取得成功" -ForegroundColor Green
    Write-Host "   取得件数: $($teamIssues.Count)" -ForegroundColor White
    foreach ($issue in $teamIssues) {
        Write-Host "   - [$($issue.status)] $($issue.displayName): $($issue.content.Substring(0, [Math]::Min(50, $issue.content.Length)))..." -ForegroundColor White
    }
} catch {
    Write-Host "❌ 困りごと一覧取得失敗: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# オープンな困りごと取得
Write-Host "6. オープンな困りごと取得" -ForegroundColor Cyan
try {
    $openIssues = Invoke-RestMethod -Uri "$baseUrl/api/team-issues/open" -Method GET
    Write-Host "✅ オープンな困りごと取得成功" -ForegroundColor Green
    Write-Host "   取得件数: $($openIssues.Count)" -ForegroundColor White
} catch {
    Write-Host "❌ オープンな困りごと取得失敗: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 困りごと統計取得
Write-Host "7. 困りごと統計取得" -ForegroundColor Cyan
try {
    $statistics = Invoke-RestMethod -Uri "$baseUrl/api/team-issues/statistics" -Method GET
    Write-Host "✅ 困りごと統計取得成功" -ForegroundColor Green
    Write-Host "   オープン: $($statistics.openCount)" -ForegroundColor White
    Write-Host "   解決済み: $($statistics.resolvedCount)" -ForegroundColor White
    Write-Host "   合計: $($statistics.totalCount)" -ForegroundColor White
} catch {
    Write-Host "❌ 困りごと統計取得失敗: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 負荷状況更新テスト
Write-Host "8. 負荷状況更新テスト" -ForegroundColor Cyan
try {
    $updateData = @{
        workloadLevel = "HIGH"
        projectCount = 4
        taskCount = 20
        comment = "新しいプロジェクトが追加されました"
    } | ConvertTo-Json

    $headers = @{
        "Content-Type" = "application/json"
    }

    $updateResult = Invoke-RestMethod -Uri "$baseUrl/api/workload-status" -Method POST -Body $updateData -Headers $headers
    Write-Host "✅ 負荷状況更新成功" -ForegroundColor Green
    Write-Host "   メッセージ: $($updateResult.message)" -ForegroundColor White
} catch {
    Write-Host "❌ 負荷状況更新失敗: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 困りごと投稿テスト
Write-Host "9. 困りごと投稿テスト" -ForegroundColor Cyan
try {
    $issueData = @{
        content = "APIテストからの困りごと投稿です。Lambda関数が正常に動作しているかテストしています。"
    } | ConvertTo-Json

    $headers = @{
        "Content-Type" = "application/json"
    }

    $newIssue = Invoke-RestMethod -Uri "$baseUrl/api/team-issues" -Method POST -Body $issueData -Headers $headers
    Write-Host "✅ 困りごと投稿成功" -ForegroundColor Green
    Write-Host "   ID: $($newIssue.id)" -ForegroundColor White
    Write-Host "   内容: $($newIssue.content)" -ForegroundColor White
} catch {
    Write-Host "❌ 困りごと投稿失敗: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

Write-Host "=== テスト完了 ===" -ForegroundColor Green
Write-Host "API Gateway エンドポイント: $baseUrl" -ForegroundColor Yellow
Write-Host "Lambda関数名: team-dashboard-api-dev" -ForegroundColor Yellow