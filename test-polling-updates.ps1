# ポーリング更新テストスクリプト
param(
    [string]$BaseUrl = "https://bn6xwu62qd.execute-api.ap-northeast-1.amazonaws.com/dev",
    [int]$IntervalSeconds = 10,
    [int]$TestDurationMinutes = 3
)

Write-Host "=== ポーリング更新テスト ===" -ForegroundColor Green
Write-Host "Base URL: $BaseUrl" -ForegroundColor Yellow
Write-Host "更新間隔: ${IntervalSeconds}秒" -ForegroundColor Yellow
Write-Host "テスト時間: ${TestDurationMinutes}分" -ForegroundColor Yellow
Write-Host ""

$headers = @{
    "Authorization" = "Bearer mock-jwt-token-testuser"
    "Content-Type" = "application/json"
}

$testUsers = @(
    @{ name = "ポーリングテスト太郎"; level = "HIGH"; projects = 5; tasks = 25 },
    @{ name = "定期更新花子"; level = "MEDIUM"; projects = 3; tasks = 15 },
    @{ name = "Lambda一郎"; level = "LOW"; projects = 1; tasks = 5 }
)

$issueTemplates = @(
    "ポーリング更新のテストです。",
    "Lambda環境での動作確認中です。",
    "定期更新機能をテストしています。",
    "WebSocketなしでの更新テストです。"
)

$priorities = @("HIGH", "MEDIUM", "LOW")
$workloadLevels = @("HIGH", "MEDIUM", "LOW")

$endTime = (Get-Date).AddMinutes($TestDurationMinutes)
$updateCount = 0

Write-Host "ポーリング更新テストを開始します..." -ForegroundColor Cyan
Write-Host "Webブラウザでダッシュボードを開いて、定期更新を確認してください。" -ForegroundColor Yellow
Write-Host "注意: WebSocket機能は無効になっているため、リアルタイム更新は動作しません。" -ForegroundColor Yellow
Write-Host ""

while ((Get-Date) -lt $endTime) {
    $updateCount++
    
    Write-Host "[$updateCount] $(Get-Date -Format 'HH:mm:ss') - データ更新実行中..." -ForegroundColor Cyan
    
    # ランダムに負荷状況を更新
    if ((Get-Random -Minimum 1 -Maximum 10) -le 8) {  # 80%の確率で負荷状況更新
        $user = $testUsers | Get-Random
        $workloadData = @{
            workloadLevel = $workloadLevels | Get-Random
            projectCount = Get-Random -Minimum 1 -Maximum 6
            taskCount = Get-Random -Minimum 5 -Maximum 30
            comment = "ポーリングテスト更新 #$updateCount - $(Get-Date -Format 'HH:mm:ss')"
        } | ConvertTo-Json
        
        try {
            $result = Invoke-RestMethod -Uri "$BaseUrl/api/workload-status" -Method POST -Body $workloadData -Headers $headers -TimeoutSec 10
            Write-Host "  ✅ 負荷状況更新: $($user.name) -> $($result.workloadLevel)" -ForegroundColor Green
        } catch {
            Write-Host "  ❌ 負荷状況更新失敗: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
    # ランダムに困りごとを投稿
    if ((Get-Random -Minimum 1 -Maximum 10) -le 4) {  # 40%の確率で困りごと投稿
        $template = $issueTemplates | Get-Random
        $priority = $priorities | Get-Random
        $issueData = @{
            content = "$template (ポーリングテスト #$updateCount - $(Get-Date -Format 'HH:mm:ss'))"
            priority = $priority
        } | ConvertTo-Json
        
        try {
            $result = Invoke-RestMethod -Uri "$BaseUrl/api/team-issues" -Method POST -Body $issueData -Headers $headers -TimeoutSec 10
            Write-Host "  ✅ 困りごと投稿: $($result.issueId) ($priority)" -ForegroundColor Green
        } catch {
            Write-Host "  ❌ 困りごと投稿失敗: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
    # 進捗表示
    $remainingTime = $endTime - (Get-Date)
    $remainingMinutes = [math]::Floor($remainingTime.TotalMinutes)
    $remainingSeconds = $remainingTime.Seconds
    Write-Host "  残り時間: ${remainingMinutes}分${remainingSeconds}秒" -ForegroundColor Gray
    Write-Host "  次回更新まで: ${IntervalSeconds}秒" -ForegroundColor Gray
    
    # 指定間隔で待機
    Start-Sleep -Seconds $IntervalSeconds
}

Write-Host ""
Write-Host "=== ポーリング更新テスト完了 ===" -ForegroundColor Green
Write-Host "総更新回数: $updateCount" -ForegroundColor Yellow
Write-Host ""

# 最終データ確認
Write-Host "最終データ確認:" -ForegroundColor Cyan

try {
    Write-Host "負荷状況データ確認..." -ForegroundColor Gray
    $finalWorkloads = Invoke-RestMethod -Uri "$BaseUrl/api/workload-status" -Method GET -Headers $headers -TimeoutSec 10
    Write-Host "✅ 負荷状況: $($finalWorkloads.Count)件" -ForegroundColor Green
    
    Write-Host "困りごとデータ確認..." -ForegroundColor Gray
    $finalIssues = Invoke-RestMethod -Uri "$BaseUrl/api/team-issues" -Method GET -Headers $headers -TimeoutSec 10
    Write-Host "✅ 困りごと: $($finalIssues.Count)件" -ForegroundColor Green
} catch {
    Write-Host "❌ 最終データ確認エラー: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "確認項目:" -ForegroundColor Cyan
Write-Host "✓ ダッシュボードで負荷状況が定期的に更新されたか" -ForegroundColor Gray
Write-Host "✓ 困りごとが定期的に表示されたか" -ForegroundColor Gray
Write-Host "✓ 接続状態が「🔄 定期更新」と表示されているか" -ForegroundColor Gray
Write-Host "✓ 手動更新ボタン（🔄）が動作するか" -ForegroundColor Gray
Write-Host "✓ WebSocketエラーが表示されていないか" -ForegroundColor Gray
Write-Host ""

Write-Host "ポーリング更新の特徴:" -ForegroundColor Yellow
Write-Host "• 更新は30秒間隔で実行されます" -ForegroundColor Gray
Write-Host "• ページが非表示の場合、更新間隔が延長されます" -ForegroundColor Gray
Write-Host "• ユーザーが非アクティブの場合、更新間隔が延長されます" -ForegroundColor Gray
Write-Host "• 手動更新ボタンで即座に更新できます" -ForegroundColor Gray
Write-Host "• WebSocketのようなリアルタイム性はありませんが、確実に動作します" -ForegroundColor Gray
Write-Host ""

Write-Host "Lambda環境での使用方法:" -ForegroundColor Cyan
Write-Host "1. WebSocketライブラリを読み込まない" -ForegroundColor Gray
Write-Host "2. 自動的にポーリングモードに切り替わる" -ForegroundColor Gray
Write-Host "3. 定期更新で最新データを取得" -ForegroundColor Gray
Write-Host "4. 必要に応じて手動更新を実行" -ForegroundColor Gray