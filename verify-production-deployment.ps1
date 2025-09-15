# 本番デプロイ検証スクリプト
param(
    [string]$BaseUrl,
    [string]$Environment = "prod",
    [string]$Region = "ap-northeast-1",
    [string]$DomainName = "",
    [switch]$SkipLoadTest = $false
)

Write-Host "=== 本番デプロイ検証 ===" -ForegroundColor Green
Write-Host "Base URL: $BaseUrl" -ForegroundColor Yellow
Write-Host "Environment: $Environment" -ForegroundColor Yellow
Write-Host "Domain: $DomainName" -ForegroundColor Yellow
Write-Host ""

$testResults = @{
    HealthCheck = $false
    DynamoDBIntegration = $false
    RealtimeFeatures = $false
    Security = $false
    Performance = $false
    Monitoring = $false
}

# 1. ヘルスチェック
Write-Host "1. ヘルスチェック" -ForegroundColor Cyan

try {
    $health = Invoke-RestMethod -Uri "$BaseUrl/api/status" -Method GET -TimeoutSec 30
    
    if ($health.status -eq "OK") {
        Write-Host "✅ ヘルスチェック成功" -ForegroundColor Green
        Write-Host "   ステータス: $($health.status)" -ForegroundColor Gray
        Write-Host "   バージョン: $($health.version)" -ForegroundColor Gray
        Write-Host "   データベース: $($health.database)" -ForegroundColor Gray
        $testResults.HealthCheck = $true
    } else {
        Write-Host "❌ ヘルスチェック失敗: 異常なステータス" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ ヘルスチェック失敗: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 2. DynamoDB統合テスト
Write-Host "2. DynamoDB統合テスト" -ForegroundColor Cyan

try {
    Write-Host "DynamoDB統合テストを実行中..." -ForegroundColor Gray
    
    # 負荷状況テスト
    $workloads = Invoke-RestMethod -Uri "$BaseUrl/api/workload-status" -Method GET -TimeoutSec 30
    Write-Host "✅ 負荷状況取得: $($workloads.Count)件" -ForegroundColor Green
    
    # 困りごとテスト
    $issues = Invoke-RestMethod -Uri "$BaseUrl/api/team-issues" -Method GET -TimeoutSec 30
    Write-Host "✅ 困りごと取得: $($issues.Count)件" -ForegroundColor Green
    
    # データ作成テスト
    $testData = @{
        workloadLevel = "MEDIUM"
        projectCount = 3
        taskCount = 15
        comment = "本番デプロイ検証テスト - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    } | ConvertTo-Json
    
    $headers = @{ "Content-Type" = "application/json" }
    $createResult = Invoke-RestMethod -Uri "$BaseUrl/api/workload-status" -Method POST -Body $testData -Headers $headers -TimeoutSec 30
    
    if ($createResult.message) {
        Write-Host "✅ データ作成テスト成功" -ForegroundColor Green
        $testResults.DynamoDBIntegration = $true
    }
    
} catch {
    Write-Host "❌ DynamoDB統合テスト失敗: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 3. リアルタイム機能テスト
Write-Host "3. リアルタイム機能テスト" -ForegroundColor Cyan

try {
    # WebSocket接続テスト
    Write-Host "WebSocket接続をテスト中..." -ForegroundColor Gray
    
    # WebSocketエンドポイントの確認
    $wsUrl = $BaseUrl -replace 'https?://', 'wss://'
    $wsUrl = "$wsUrl/ws"
    
    Write-Host "WebSocketエンドポイント: $wsUrl" -ForegroundColor Gray
    
    # 簡易WebSocketテスト（実際の接続は困難なため、エンドポイントの存在確認）
    try {
        $wsTest = Invoke-WebRequest -Uri $wsUrl -Method GET -TimeoutSec 10 -ErrorAction SilentlyContinue
        # WebSocketエンドポイントは通常のHTTPリクエストでは400エラーを返すが、これは正常
        if ($wsTest.StatusCode -eq 400 -or $_.Exception.Response.StatusCode -eq 400) {
            Write-Host "✅ WebSocketエンドポイント確認完了" -ForegroundColor Green
            $testResults.RealtimeFeatures = $true
        }
    } catch {
        if ($_.Exception.Response.StatusCode -eq 400) {
            Write-Host "✅ WebSocketエンドポイント確認完了（400エラーは正常）" -ForegroundColor Green
            $testResults.RealtimeFeatures = $true
        } else {
            Write-Host "⚠️ WebSocketエンドポイント確認失敗: $($_.Exception.Message)" -ForegroundColor Yellow
        }
    }
    
} catch {
    Write-Host "⚠️ リアルタイム機能テスト失敗: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host ""

# 4. セキュリティテスト
Write-Host "4. セキュリティテスト" -ForegroundColor Cyan

try {
    # HTTPS強制確認
    if ($BaseUrl.StartsWith("https://")) {
        Write-Host "✅ HTTPS使用確認" -ForegroundColor Green
        
        # セキュリティヘッダー確認
        $securityTest = Invoke-WebRequest -Uri "$BaseUrl/api/status" -Method GET -TimeoutSec 30
        
        $securityHeaders = @(
            "Strict-Transport-Security",
            "X-Content-Type-Options",
            "X-Frame-Options",
            "X-XSS-Protection"
        )
        
        $headerCount = 0
        foreach ($header in $securityHeaders) {
            if ($securityTest.Headers[$header]) {
                Write-Host "   ✅ $header: $($securityTest.Headers[$header])" -ForegroundColor Green
                $headerCount++
            } else {
                Write-Host "   ⚠️ $header: 未設定" -ForegroundColor Yellow
            }
        }
        
        if ($headerCount -ge 2) {
            $testResults.Security = $true
        }
        
    } else {
        Write-Host "⚠️ HTTPSが使用されていません" -ForegroundColor Yellow
    }
    
    # CORS設定確認
    try {
        $corsTest = Invoke-WebRequest -Uri "$BaseUrl/api/status" -Method OPTIONS -TimeoutSec 10
        if ($corsTest.Headers["Access-Control-Allow-Origin"]) {
            Write-Host "✅ CORS設定確認完了" -ForegroundColor Green
        }
    } catch {
        Write-Host "⚠️ CORS設定確認失敗" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "❌ セキュリティテスト失敗: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 5. パフォーマンステスト
Write-Host "5. パフォーマンステスト" -ForegroundColor Cyan

if (-not $SkipLoadTest) {
    try {
        Write-Host "レスポンス時間テスト中..." -ForegroundColor Gray
        
        $responseTimes = @()
        for ($i = 1; $i -le 10; $i++) {
            $startTime = Get-Date
            $response = Invoke-RestMethod -Uri "$BaseUrl/api/status" -Method GET -TimeoutSec 30
            $endTime = Get-Date
            $responseTime = ($endTime - $startTime).TotalMilliseconds
            $responseTimes += $responseTime
            
            Write-Host "   リクエスト $i : $([math]::Round($responseTime, 2))ms" -ForegroundColor Gray
        }
        
        $avgResponseTime = ($responseTimes | Measure-Object -Average).Average
        $maxResponseTime = ($responseTimes | Measure-Object -Maximum).Maximum
        
        Write-Host "✅ パフォーマンステスト完了" -ForegroundColor Green
        Write-Host "   平均レスポンス時間: $([math]::Round($avgResponseTime, 2))ms" -ForegroundColor Gray
        Write-Host "   最大レスポンス時間: $([math]::Round($maxResponseTime, 2))ms" -ForegroundColor Gray
        
        if ($avgResponseTime -lt 1000) {
            $testResults.Performance = $true
        }
        
    } catch {
        Write-Host "❌ パフォーマンステスト失敗: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "⚠️ パフォーマンステストをスキップしました" -ForegroundColor Yellow
}

Write-Host ""

# 6. 監視・ログ確認
Write-Host "6. 監視・ログ確認" -ForegroundColor Cyan

try {
    # CloudWatchログ確認
    $logGroupName = "/ecs/team-dashboard-$Environment"
    
    Write-Host "CloudWatchログを確認中..." -ForegroundColor Gray
    $logGroups = aws logs describe-log-groups --log-group-name-prefix $logGroupName --region $Region --output json | ConvertFrom-Json
    
    if ($logGroups.logGroups.Count -gt 0) {
        Write-Host "✅ CloudWatchログ確認完了" -ForegroundColor Green
        Write-Host "   ロググループ: $($logGroups.logGroups[0].logGroupName)" -ForegroundColor Gray
        Write-Host "   保持期間: $($logGroups.logGroups[0].retentionInDays)日" -ForegroundColor Gray
    }
    
    # CloudWatchアラーム確認
    $alarms = aws cloudwatch describe-alarms --alarm-name-prefix "team-dashboard-$Environment" --region $Region --output json | ConvertFrom-Json
    
    if ($alarms.MetricAlarms.Count -gt 0) {
        Write-Host "✅ CloudWatchアラーム確認完了" -ForegroundColor Green
        Write-Host "   設定済みアラーム数: $($alarms.MetricAlarms.Count)" -ForegroundColor Gray
        $testResults.Monitoring = $true
    } else {
        Write-Host "⚠️ CloudWatchアラームが設定されていません" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "⚠️ 監視・ログ確認エラー: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host ""

# 7. ドメイン・DNS確認
if ($DomainName) {
    Write-Host "7. ドメイン・DNS確認" -ForegroundColor Cyan
    
    try {
        Write-Host "DNS解決テスト中..." -ForegroundColor Gray
        
        # nslookupコマンドでDNS確認
        $nslookupResult = nslookup $DomainName 2>$null
        if ($nslookupResult) {
            Write-Host "✅ DNS解決確認完了" -ForegroundColor Green
        }
        
        # HTTPSアクセステスト
        try {
            $domainTest = Invoke-RestMethod -Uri "https://$DomainName/api/status" -Method GET -TimeoutSec 30
            if ($domainTest.status -eq "OK") {
                Write-Host "✅ カスタムドメインアクセス確認完了" -ForegroundColor Green
            }
        } catch {
            Write-Host "⚠️ カスタムドメインアクセス失敗: $($_.Exception.Message)" -ForegroundColor Yellow
            Write-Host "   DNS伝播やCloudFrontデプロイの完了を待つ必要があります" -ForegroundColor Gray
        }
        
    } catch {
        Write-Host "⚠️ ドメイン・DNS確認エラー: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    
    Write-Host ""
}

# 8. 検証結果サマリー
Write-Host "=== 検証結果サマリー ===" -ForegroundColor Green

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

# 9. 推奨アクション
Write-Host "推奨アクション:" -ForegroundColor Cyan

if (-not $testResults.HealthCheck) {
    Write-Host "  🔧 アプリケーションの起動状況を確認してください" -ForegroundColor Yellow
}

if (-not $testResults.DynamoDBIntegration) {
    Write-Host "  🔧 DynamoDBテーブルとIAM権限を確認してください" -ForegroundColor Yellow
}

if (-not $testResults.RealtimeFeatures) {
    Write-Host "  🔧 WebSocket設定とロードバランサー設定を確認してください" -ForegroundColor Yellow
}

if (-not $testResults.Security) {
    Write-Host "  🔧 セキュリティヘッダーとHTTPS設定を確認してください" -ForegroundColor Yellow
}

if (-not $testResults.Performance) {
    Write-Host "  🔧 アプリケーションのパフォーマンスチューニングを検討してください" -ForegroundColor Yellow
}

if (-not $testResults.Monitoring) {
    Write-Host "  🔧 CloudWatch監視とアラート設定を確認してください" -ForegroundColor Yellow
}

Write-Host ""

# 10. 運用情報
Write-Host "運用情報:" -ForegroundColor Cyan
Write-Host "  CloudWatch Console: https://console.aws.amazon.com/cloudwatch/" -ForegroundColor Gray
Write-Host "  ECS Console: https://console.aws.amazon.com/ecs/" -ForegroundColor Gray
Write-Host "  DynamoDB Console: https://console.aws.amazon.com/dynamodb/" -ForegroundColor Gray
Write-Host "  CloudFormation Console: https://console.aws.amazon.com/cloudformation/" -ForegroundColor Gray

Write-Host ""
Write-Host "継続的な監視項目:" -ForegroundColor Cyan
Write-Host "  • CPU・メモリ使用率" -ForegroundColor Gray
Write-Host "  • レスポンス時間" -ForegroundColor Gray
Write-Host "  • エラー率" -ForegroundColor Gray
Write-Host "  • DynamoDB読み書き容量" -ForegroundColor Gray
Write-Host "  • セキュリティアラート" -ForegroundColor Gray

Write-Host ""

if ($passedTests -eq $totalTests) {
    Write-Host "🎉 全ての検証テストに合格しました！本番環境の準備が完了しています。" -ForegroundColor Green
} elseif ($passedTests -ge ($totalTests * 0.8)) {
    Write-Host "⚠️ 大部分のテストに合格していますが、いくつかの項目で改善が必要です。" -ForegroundColor Yellow
} else {
    Write-Host "❌ 複数のテストで問題が発見されました。本番運用前に修正が必要です。" -ForegroundColor Red
}

Write-Host ""
Write-Host "使用方法:" -ForegroundColor Yellow
Write-Host "  基本検証: .\verify-production-deployment.ps1 -BaseUrl 'https://your-endpoint'" -ForegroundColor Gray
Write-Host "  ドメイン付き: .\verify-production-deployment.ps1 -BaseUrl 'https://your-endpoint' -DomainName 'yourdomain.com'" -ForegroundColor Gray
Write-Host "  負荷テストなし: .\verify-production-deployment.ps1 -BaseUrl 'https://your-endpoint' -SkipLoadTest" -ForegroundColor Gray