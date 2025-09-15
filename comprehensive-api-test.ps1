# チーム状況ダッシュボード 包括的APIテスト
$baseUrl = "https://bn6xwu62qd.execute-api.ap-northeast-1.amazonaws.com/dev"

Write-Host "=== チーム状況ダッシュボード 包括的テスト ===" -ForegroundColor Green
Write-Host "Base URL: $baseUrl" -ForegroundColor Yellow
Write-Host ""

# テスト結果を記録
$testResults = @()

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Url,
        [string]$Method = "GET",
        [hashtable]$Body = $null
    )
    
    try {
        $params = @{
            Uri = $Url
            Method = $Method
        }
        
        if ($Body) {
            $params.Body = ($Body | ConvertTo-Json)
            $params.ContentType = "application/json"
        }
        
        $response = Invoke-RestMethod @params
        Write-Host "✅ $Name - 成功" -ForegroundColor Green
        
        if ($response -is [array]) {
            Write-Host "   データ件数: $($response.Count)" -ForegroundColor White
        } elseif ($response.PSObject.Properties.Count -gt 0) {
            Write-Host "   レスポンス: $($response | ConvertTo-Json -Depth 1 -Compress)" -ForegroundColor White
        }
        
        return @{ Name = $Name; Status = "成功"; Response = $response }
    }
    catch {
        Write-Host "❌ $Name - 失敗: $($_.Exception.Message)" -ForegroundColor Red
        return @{ Name = $Name; Status = "失敗"; Error = $_.Exception.Message }
    }
}

# 1. ヘルスチェック
Write-Host "1. ヘルスチェック" -ForegroundColor Cyan
$testResults += Test-Endpoint "ヘルスチェック" "$baseUrl/health"
Write-Host ""

# 2. 負荷状況関連
Write-Host "2. 負荷状況関連テスト" -ForegroundColor Cyan
$testResults += Test-Endpoint "負荷状況一覧取得" "$baseUrl/workload-status"
$testResults += Test-Endpoint "自分の負荷状況取得" "$baseUrl/workload-status/my"

# 負荷状況更新テスト
$workloadUpdateData = @{
    workloadLevel = "HIGH"
    projectCount = 4
    taskCount = 20
    comment = "テストからの更新"
}
$testResults += Test-Endpoint "負荷状況更新" "$baseUrl/workload-status" "POST" $workloadUpdateData
Write-Host ""

# 3. 困りごと関連
Write-Host "3. 困りごと関連テスト" -ForegroundColor Cyan
$testResults += Test-Endpoint "困りごと一覧取得" "$baseUrl/team-issues"

# 困りごと投稿テスト
$issueData = @{
    content = "APIテストからの困りごと投稿です。$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')に投稿されました。"
}
$testResults += Test-Endpoint "困りごと投稿" "$baseUrl/team-issues" "POST" $issueData
Write-Host ""

# 4. API Status
Write-Host "4. その他のエンドポイント" -ForegroundColor Cyan
$testResults += Test-Endpoint "API Status" "$baseUrl/api/status"
Write-Host ""

# 5. テスト結果サマリー
Write-Host "=== テスト結果サマリー ===" -ForegroundColor Green
$successCount = ($testResults | Where-Object { $_.Status -eq "成功" }).Count
$totalCount = $testResults.Count

Write-Host "成功: $successCount / $totalCount" -ForegroundColor Green

if ($successCount -eq $totalCount) {
    Write-Host "🎉 全てのテストが成功しました！" -ForegroundColor Green
} else {
    Write-Host "⚠️  一部のテストが失敗しました。" -ForegroundColor Yellow
    $failedTests = $testResults | Where-Object { $_.Status -eq "失敗" }
    foreach ($test in $failedTests) {
        Write-Host "   - $($test.Name): $($test.Error)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=== 次のステップ ===" -ForegroundColor Yellow
Write-Host "1. フロントエンドでの機能テスト（負荷状況更新、困りごと投稿）" -ForegroundColor White
Write-Host "2. 認証機能の実装とテスト" -ForegroundColor White
Write-Host "3. DynamoDB連携の実装" -ForegroundColor White
Write-Host "4. エラーハンドリングの強化" -ForegroundColor White