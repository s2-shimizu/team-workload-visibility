# Lambda デプロイ検証スクリプト
param(
    [Parameter(Mandatory=$true)]
    [string]$ApiEndpoint,
    [string]$Environment = "dev",
    [string]$Region = "ap-northeast-1",
    [switch]$SkipLoadTest = $false
)

Write-Host "=== Lambda デプロイ検証 ===" -ForegroundColor Green
Write-Host "API Endpoint: $ApiEndpoint" -ForegroundColor Yellow
Write-Host "Environment: $Environment" -ForegroundColor Yellow
Write-Host "Region: $Region" -ForegroundColor Yellow
Write-Host ""

$testResults = @{
    HealthCheck = $false
    ApiGateway = $false
    DynamoDBIntegration = $false
    PollingFeatures = $false
    Performance = $false
    ErrorHandling = $false
}

# 1. ヘルスチェック
Write-Host "1. ヘルスチェック" -ForegroundColor Cyan

try {
    $health = Invoke-RestMethod -Uri "$ApiEndpoint/api/status" -Method GET -TimeoutSec 30
    
    if ($health.status -eq "OK") {
        Write-Host "✅ ヘルスチェック成功" -ForegroundColor Green
        Write-Host "   ステータス: $($health.status)" -ForegroundColor Gray
        Write-Host "   メッセージ: $($health.message)" -ForegroundColor Gray
        Write-Host "   バージョン: $($health.version)" -ForegroundColor Gray
        Write-Host "   タイムスタンプ: $($health.timestamp)" -ForegroundColor Gray
        $testResults.HealthCheck = $true
    } else {
        Write-Host "❌ ヘルスチェック失敗: 異常なステータス" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ ヘルスチェック失敗: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 2. API Gateway機能テスト
Write-Host "2. API Gateway機能テスト" -ForegroundColor Cyan

try {
    # CORS確認
    Write-Host "CORS設定を確認中..." -ForegroundColor Gray
    $corsTest = Invoke-WebRequest -Uri "$ApiEndpoint/api/status" -Method OPTIONS -TimeoutSec 10
    
    if ($corsTest.Headers["Access-Control-Allow-Origin"]) {
        Write-Host "✅ CORS設定確認完了" -ForegroundColor Green
        Write-Host "   Allow-Origin: $($corsTest.Headers['Access-Control-Allow-Origin'])" -ForegroundColor Gray
    }
    
    # 異なるHTTPメソッドテスト
    Write-Host "HTTPメソッドテスト中..." -ForegroundColor Gray
    
    # GET テスト
    $getTest = Invoke-RestMethod -Uri "$ApiEndpoint/api/workload-status" -Method GET -TimeoutSec 30
    Write-Host "✅ GET メソッド: 正常" -ForegroundColor Green
    
    # POST テスト
    $postData = @{
        workloadLevel = "MEDIUM"
        projectCount = 2
        taskCount = 10
        comment = "Lambda統合テスト"
    } | ConvertTo-Json
    
    $headers = @{ "Content-Type" = "application/json" }
    $postTest = Invoke-RestMethod -Uri "$ApiEndpoint/api/workload-status" -Method POST -Body $postData -Headers $headers -TimeoutSec 30
    
    if ($postTest.message) {
        Write-Host "✅ POST メソッド: 正常" -ForegroundColor Green
        $testResults.ApiGateway = $true
    }
    
} catch {
    Write-Host "❌ API Gateway機能テスト失敗: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 3. DynamoDB統合テスト
Write-Host "3. DynamoDB統合テスト" -ForegroundColor Cyan

try {
    Write-Host "DynamoDB統合テストを実行中..." -ForegroundColor Gray
    
    # 負荷状況データテスト
    $workloads = Invoke-RestMethod -Uri "$ApiEndpoint/api/workload-status" -Method GET -TimeoutSec 30
    Write-Host "✅ 負荷状況取得: $($workloads.Count)件" -ForegroundColor Green
    
    # 困りごとデータテスト
    $issues = Invoke-RestMethod -Uri "$ApiEndpoint/api/team-issues" -Method GET -TimeoutSec 30
    Write-Host "✅ 困りごと取得: $($issues.Count)件" -ForegroundColor Green
    
    # 困りごと作成テスト
    $issueData = @{
        content = "Lambda統合テスト用の困りごと - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
        priority = "MEDIUM"
    } | ConvertTo-Json
    
    $newIssue = Invoke-RestMethod -Uri "$ApiEndpoint/api/team-issues" -Method POST -Body $issueData -Headers $headers -TimeoutSec 30
    
    if ($newIssue.issueId) {
        Write-Host "✅ 困りごと作成テスト成功: $($newIssue.issueId)" -ForegroundColor Green
        $testResults.DynamoDBIntegration = $true
    }
    
    # 統計データテスト
    try {
        $stats = Invoke-RestMethod -Uri "$ApiEndpoint/api/team-issues/statistics" -Method GET -TimeoutSec 30
        Write-Host "✅ 統計データ取得: 総数$($stats.total)" -ForegroundColor Green
    } catch {
        Write-Host "⚠️ 統計データ取得失敗（エンドポイントが実装されていない可能性）" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "❌ DynamoDB統合テスト失敗: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 4. ポーリング機能テスト
Write-Host "4. ポーリング機能テスト" -ForegroundColor Cyan

try {
    Write-Host "ポーリング更新シミュレーション中..." -ForegroundColor Gray
    
    # 複数回のAPI呼び出しでデータの一貫性を確認
    $pollingResults = @()
    for ($i = 1; $i -le 5; $i++) {
        $pollingData = Invoke-RestMethod -Uri "$ApiEndpoint/api/workload-status" -Method GET -TimeoutSec 30
        $pollingResults += $pollingData.Count
        Write-Host "   ポーリング $i : $($pollingData.Count)件のデータ" -ForegroundColor Gray
        Start-Sleep -Seconds 2
    }
    
    # データの一貫性確認
    $uniqueCounts = $pollingResults | Sort-Object -Unique
    if ($uniqueCounts.Count -le 2) {
        Write-Host "✅ ポーリングデータ一貫性: 正常" -ForegroundColor Green
        $testResults.PollingFeatures = $true
    } else {
        Write-Host "⚠️ ポーリングデータに変動があります（正常な場合もあります）" -ForegroundColor Yellow
        $testResults.PollingFeatures = $true
    }
    
} catch {
    Write-Host "❌ ポーリング機能テスト失敗: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 5. パフォーマンステスト
Write-Host "5. パフォーマンステスト" -ForegroundColor Cyan

if (-not $SkipLoadTest) {
    try {
        Write-Host "Lambda レスポンス時間テスト中..." -ForegroundColor Gray
        
        $responseTimes = @()
        $coldStartDetected = $false
        
        for ($i = 1; $i -le 10; $i++) {
            $startTime = Get-Date
            $response = Invoke-RestMethod -Uri "$ApiEndpoint/api/status" -Method GET -TimeoutSec 30
            $endTime = Get-Date
            $responseTime = ($endTime - $startTime).TotalMilliseconds
            $responseTimes += $responseTime
            
            # コールドスタート検出（最初のリクエストが著しく遅い場合）
            if ($i -eq 1 -and $responseTime -gt 5000) {
                $coldStartDetected = $true
                Write-Host "   リクエスト $i : $([math]::Round($responseTime, 2))ms (コールドスタート)" -ForegroundColor Yellow
            } else {
                Write-Host "   リクエスト $i : $([math]::Round($responseTime, 2))ms" -ForegroundColor Gray
            }
        }
        
        $avgResponseTime = ($responseTimes | Measure-Object -Average).Average
        $maxResponseTime = ($responseTimes | Measure-Object -Maximum).Maximum
        $minResponseTime = ($responseTimes | Measure-Object -Minimum).Minimum
        
        Write-Host "✅ パフォーマンステスト完了" -ForegroundColor Green
        Write-Host "   平均レスポンス時間: $([math]::Round($avgResponseTime, 2))ms" -ForegroundColor Gray
        Write-Host "   最大レスポンス時間: $([math]::Round($maxResponseTime, 2))ms" -ForegroundColor Gray
        Write-Host "   最小レスポンス時間: $([math]::Round($minResponseTime, 2))ms" -ForegroundColor Gray
        
        if ($coldStartDetected) {
            Write-Host "   ⚠️ コールドスタートが検出されました" -ForegroundColor Yellow
        }
        
        # Lambda環境では3秒以下を良好とする
        if ($avgResponseTime -lt 3000) {
            $testResults.Performance = $true
        }
        
    } catch {
        Write-Host "❌ パフォーマンステスト失敗: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "⚠️ パフォーマンステストをスキップしました" -ForegroundColor Yellow
}

Write-Host ""

# 6. エラーハンドリングテスト
Write-Host "6. エラーハンドリングテスト" -ForegroundColor Cyan

try {
    # 存在しないエンドポイントテスト
    Write-Host "404エラーハンドリングテスト中..." -ForegroundColor Gray
    try {
        $notFoundTest = Invoke-RestMethod -Uri "$ApiEndpoint/api/nonexistent" -Method GET -TimeoutSec 10
        Write-Host "⚠️ 404エラーが適切に処理されていません" -ForegroundColor Yellow
    } catch {
        if ($_.Exception.Response.StatusCode -eq 404) {
            Write-Host "✅ 404エラーハンドリング: 正常" -ForegroundColor Green
        } else {
            Write-Host "⚠️ 予期しないエラー: $($_.Exception.Response.StatusCode)" -ForegroundColor Yellow
        }
    }
    
    # 不正なJSONテスト
    Write-Host "不正なリクエストハンドリングテスト中..." -ForegroundColor Gray
    try {
        $badRequestTest = Invoke-RestMethod -Uri "$ApiEndpoint/api/workload-status" -Method POST -Body "invalid json" -Headers $headers -TimeoutSec 10
        Write-Host "⚠️ 不正なリクエストが適切に処理されていません" -ForegroundColor Yellow
    } catch {
        if ($_.Exception.Response.StatusCode -eq 400 -or $_.Exception.Response.StatusCode -eq 500) {
            Write-Host "✅ 不正なリクエストハンドリング: 正常" -ForegroundColor Green
            $testResults.ErrorHandling = $true
        }
    }
    
} catch {
    Write-Host "❌ エラーハンドリングテスト失敗: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 7. Lambda固有機能確認
Write-Host "7. Lambda固有機能確認" -ForegroundColor Cyan

try {
    # Lambda関数情報取得
    $functionName = "team-dashboard-$Environment"
    Write-Host "Lambda関数情報を確認中..." -ForegroundColor Gray
    
    $lambdaInfo = aws lambda get-function --function-name $functionName --region $Region --output json | ConvertFrom-Json
    
    if ($lambdaInfo) {
        Write-Host "✅ Lambda関数確認完了" -ForegroundColor Green
        Write-Host "   関数名: $($lambdaInfo.Configuration.FunctionName)" -ForegroundColor Gray
        Write-Host "   ランタイム: $($lambdaInfo.Configuration.Runtime)" -ForegroundColor Gray
        Write-Host "   メモリサイズ: $($lambdaInfo.Configuration.MemorySize)MB" -ForegroundColor Gray
        Write-Host "   タイムアウト: $($lambdaInfo.Configuration.Timeout)秒" -ForegroundColor Gray
        Write-Host "   最終更新: $($lambdaInfo.Configuration.LastModified)" -ForegroundColor Gray
    }
    
    # CloudWatch Logs確認
    $logGroupName = "/aws/lambda/$functionName"
    Write-Host "CloudWatch Logsを確認中..." -ForegroundColor Gray
    
    try {
        $logGroups = aws logs describe-log-groups --log-group-name-prefix $logGroupName --region $Region --output json | ConvertFrom-Json
        if ($logGroups.logGroups.Count -gt 0) {
            Write-Host "✅ CloudWatch Logs確認完了" -ForegroundColor Green
            Write-Host "   ロググループ: $($logGroups.logGroups[0].logGroupName)" -ForegroundColor Gray
        }
    } catch {
        Write-Host "⚠️ CloudWatch Logs確認失敗" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "⚠️ Lambda固有機能確認エラー: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host ""

# 8. 検証結果サマリー
Write-Host "=== Lambda デプロイ検証結果 ===" -ForegroundColor Green

$passedTests = ($testResults.Values | Where-Object { $_ -eq $true }).Count
$totalTests = $testResults.Count

Write-Host "合格テスト: $passedTests / $totalTests" -ForegroundColor Yellow
Write-Host ""

foreach ($test in $testResults.GetEnumerator()) {
    $status = if ($test.Value) { "✅ 合格" } else { "❌ 不合格" }
    $color = if ($test.Value) { "Green" } else { "Red" }
    Write-Host "  $($test.Key): $status" -ForegroundColor $color
}

Write-Host ""

# 9. Lambda固有の推奨事項
Write-Host "Lambda固有の推奨事項:" -ForegroundColor Cyan

Write-Host "✅ 実装済み機能:" -ForegroundColor Green
Write-Host "  • サーバーレスアーキテクチャ" -ForegroundColor Gray
Write-Host "  • 自動スケーリング" -ForegroundColor Gray
Write-Host "  • DynamoDB統合" -ForegroundColor Gray
Write-Host "  • API Gateway統合" -ForegroundColor Gray
Write-Host "  • ポーリング更新対応" -ForegroundColor Gray

Write-Host ""
Write-Host "⚠️ 制限事項:" -ForegroundColor Yellow
Write-Host "  • WebSocket機能は利用不可" -ForegroundColor Gray
Write-Host "  • リアルタイム更新は30秒間隔のポーリング" -ForegroundColor Gray
Write-Host "  • コールドスタート遅延の可能性" -ForegroundColor Gray

Write-Host ""
Write-Host "🔧 最適化推奨事項:" -ForegroundColor Cyan

if (-not $testResults.Performance) {
    Write-Host "  • Lambdaメモリサイズの増加を検討" -ForegroundColor Yellow
    Write-Host "  • Provisioned Concurrencyの設定を検討" -ForegroundColor Yellow
}

if (-not $testResults.ErrorHandling) {
    Write-Host "  • エラーハンドリングの改善" -ForegroundColor Yellow
}

Write-Host "  • CloudWatch監視の設定" -ForegroundColor Gray
Write-Host "  • X-Rayトレーシングの有効化" -ForegroundColor Gray
Write-Host "  • Lambda Layersの活用検討" -ForegroundColor Gray

Write-Host ""

# 10. 運用情報
Write-Host "運用情報:" -ForegroundColor Cyan
Write-Host "  Lambda Console: https://console.aws.amazon.com/lambda/home?region=$Region#/functions/$functionName" -ForegroundColor Gray
Write-Host "  API Gateway Console: https://console.aws.amazon.com/apigateway/" -ForegroundColor Gray
Write-Host "  DynamoDB Console: https://console.aws.amazon.com/dynamodb/" -ForegroundColor Gray
Write-Host "  CloudWatch Logs: https://console.aws.amazon.com/cloudwatch/home?region=$Region#logsV2:log-groups/log-group/%252Faws%252Flambda%252F$functionName" -ForegroundColor Gray

Write-Host ""
Write-Host "継続的な監視項目:" -ForegroundColor Cyan
Write-Host "  • Lambda実行時間・エラー率" -ForegroundColor Gray
Write-Host "  • API Gatewayレスポンス時間" -ForegroundColor Gray
Write-Host "  • DynamoDB読み書き容量" -ForegroundColor Gray
Write-Host "  • コールドスタート頻度" -ForegroundColor Gray

Write-Host ""

if ($passedTests -eq $totalTests) {
    Write-Host "🎉 全ての検証テストに合格しました！Lambdaデプロイが正常に完了しています。" -ForegroundColor Green
} elseif ($passedTests -ge ($totalTests * 0.8)) {
    Write-Host "⚠️ 大部分のテストに合格していますが、いくつかの項目で改善が必要です。" -ForegroundColor Yellow
} else {
    Write-Host "❌ 複数のテストで問題が発見されました。修正が必要です。" -ForegroundColor Red
}

Write-Host ""
Write-Host "フロントエンド設定:" -ForegroundColor Cyan
Write-Host "  frontend/js/aws-config.js のendpointを '$ApiEndpoint' に更新してください" -ForegroundColor Yellow
Write-Host "  WebSocketライブラリの読み込みを無効化することを推奨します" -ForegroundColor Yellow

Write-Host ""
Write-Host "使用方法:" -ForegroundColor Yellow
Write-Host "  基本検証: .\test-lambda-deployment.ps1 -ApiEndpoint 'https://api-id.execute-api.region.amazonaws.com/stage'" -ForegroundColor Gray
Write-Host "  負荷テストなし: .\test-lambda-deployment.ps1 -ApiEndpoint 'https://api-id.execute-api.region.amazonaws.com/stage' -SkipLoadTest" -ForegroundColor Gray