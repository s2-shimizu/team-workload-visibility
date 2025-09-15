# リアルタイム更新テストスクリプト
param(
    [string]$BaseUrl = "https://bn6xwu62qd.execute-api.ap-northeast-1.amazonaws.com/dev",
    [int]$IntervalSeconds = 5,
    [int]$TestDurationMinutes = 2
)

Write-Host "=== リアルタイム更新テスト ===" -ForegroundColor Green
Write-Host "Base URL: $BaseUrl" -ForegroundColor Yellow
Write-Host "更新間隔: ${IntervalSeconds}秒" -ForegroundColor Yellow
Write-Host "テスト時間: ${TestDurationMinutes}分" -ForegroundColor Yellow
Write-Host ""

$headers = @{
    "Authorization" = "Bearer mock-jwt-token-testuser"
    "Content-Type" = "application/json"
}

$testUsers = @(
    @{ name = "田中太郎"; level = "HIGH"; projects = 5; tasks = 25 },
    @{ name = "佐藤花子"; level = "MEDIUM"; projects = 3; tasks = 15 },
    @{ name = "鈴木一郎"; level = "LOW"; projects = 1; tasks = 5 },
    @{ name = "山田次郎"; level = "HIGH"; projects = 4; tasks = 20 }
)

$issueTemplates = @(
    "新しい技術の学習で詰まっています。",
    "プロジェクトの進め方で悩んでいます。",
    "チーム内のコミュニケーションについて相談があります。",
    "タスクの優先順位付けで困っています。",
    "コードレビューの進め方について質問があります。"
)

$priorities = @("HIGH", "MEDIUM", "LOW")
$workloadLevels = @("HIGH", "MEDIUM", "LOW")

$endTime = (Get-Date).AddMinutes($TestDurationMinutes)
$updateCount = 0

Write-Host "リアルタイム更新テストを開始します..." -ForegroundColor Cyan
Write-Host "Webブラウザでダッシュボードを開いて、リアルタイム更新を確認してください。" -ForegroundColor Yellow
Write-Host ""

while ((Get-Date) -lt $endTime) {
    $updateCount++
    
    Write-Host "[$updateCount] $(Get-Date -Format 'HH:mm:ss') - 更新実行中..." -ForegroundColor Cyan
    
    # ランダムに負荷状況を更新
    if ((Get-Random -Minimum 1 -Maximum 10) -le 7) {  # 70%の確率で負荷状況更新
        $user = $testUsers | Get-Random
        $workloadData = @{
            workloadLevel = $workloadLevels | Get-Random
            projectCount = Get-Random -Minimum 1 -Maximum 6
            taskCount = Get-Random -Minimum 5 -Maximum 30
            comment = "リアルタイムテスト更新 #$updateCount - $(Get-Date -Format 'HH:mm:ss')"
        } | ConvertTo-Json
        
        try {
            $result = Invoke-RestMethod -Uri "$BaseUrl/api/workload-status" -Method POST -Body $workloadData -Headers $headers -TimeoutSec 10
            Write-Host "  ✅ 負荷状況更新: $($user.name) -> $($result.workloadLevel)" -ForegroundColor Green
        } catch {
            Write-Host "  ❌ 負荷状況更新失敗: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
    # ランダムに困りごとを投稿
    if ((Get-Random -Minimum 1 -Maximum 10) -le 3) {  # 30%の確率で困りごと投稿
        $template = $issueTemplates | Get-Random
        $priority = $priorities | Get-Random
        $issueData = @{
            content = "$template (リアルタイムテスト #$updateCount - $(Get-Date -Format 'HH:mm:ss'))"
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
    
    # 指定間隔で待機
    Start-Sleep -Seconds $IntervalSeconds
}

Write-Host ""
Write-Host "=== リアルタイム更新テスト完了 ===" -ForegroundColor Green
Write-Host "総更新回数: $updateCount" -ForegroundColor Yellow
Write-Host ""
Write-Host "確認項目:" -ForegroundColor Cyan
Write-Host "✓ ダッシュボードで負荷状況がリアルタイムで更新されたか" -ForegroundColor Gray
Write-Host "✓ 困りごとがリアルタイムで表示されたか" -ForegroundColor Gray
Write-Host "✓ 接続状態インジケーターが正常に動作したか" -ForegroundColor Gray
Write-Host "✓ 通知が適切に表示されたか" -ForegroundColor Gray
Write-Host ""
Write-Host "WebSocketの接続状況をブラウザの開発者ツールで確認してください。" -ForegroundColor Yellow