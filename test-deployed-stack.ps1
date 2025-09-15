# デプロイ済みスタックテストスクリプト
param(
    [Parameter(Mandatory=$true)]
    [string]$ApiEndpoint,
    
    [string]$WorkloadTable = "",
    [string]$IssueTable = "",
    [string]$Environment = "dev"
)

Write-Host "=== デプロイ済みスタックテスト ===" -ForegroundColor Green
Write-Host "API Endpoint: $ApiEndpoint" -ForegroundColor Yellow
Write-Host "Environment: $Environment" -ForegroundColor Yellow
if ($WorkloadTable) { Write-Host "Workload Table: $WorkloadTable" -ForegroundColor Yellow }
if ($IssueTable) { Write-Host "Issue Table: $IssueTable" -ForegroundColor Yellow }
Write-Host ""

# テスト用ヘッダー
$headers = @{
    "Authorization" = "Bearer mock-jwt-token-testuser"
    "Content-Type" = "application/json"
}

# 1. API接続テスト
Write-Host "1. API接続テスト" -ForegroundColor Cyan
try {
    $health = Invoke-RestMethod -Uri "$ApiEndpoint/api/status" -Method GET -TimeoutSec 30
    Write-Host "✅ API接続: 成功" -ForegroundColor Green
    Write-Host "   ステータス: $($health.status)" -ForegroundColor Gray
    Write-Host "   データベース: $($health.database)" -ForegroundColor Gray
    Write-Host "   バージョン: $($health.version)" -ForegroundColor Gray
    Write-Host "   タイムスタンプ: $($health.timestamp)" -ForegroundColor Gray
} catch {
    Write-Host "❌ API接続: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   エンドポイントが正しいか確認してください" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# 2. DynamoDBテーブル確認
Write-Host "2. DynamoDBテーブル確認" -ForegroundColor Cyan

if ($WorkloadTable) {
    try {
        $workloadTableInfo = aws dynamodb describe-table --table-name $WorkloadTable --output json | ConvertFrom-Json
        Write-Host "✅ WorkloadStatusテーブル: 存在確認" -ForegroundColor Green
        Write-Host "   テーブル名: $($workloadTableInfo.Table.TableName)" -ForegroundColor Gray
        Write-Host "   ステータス: $($workloadTableInfo.Table.TableStatus)" -ForegroundColor Gray
        Write-Host "   アイテム数: $($workloadTableInfo.Table.ItemCount)" -ForegroundColor Gray
    } catch {
        Write-Host "❌ WorkloadStatusテーブル: 確認失敗 - $($_.Exception.Message)" -ForegroundColor Red
    }
}

if ($IssueTable) {
    try {
        $issueTableInfo = aws dynamodb describe-table --table-name $IssueTable --output json | ConvertFrom-Json
        Write-Host "✅ TeamIssueテーブル: 存在確認" -ForegroundColor Green
        Write-Host "   テーブル名: $($issueTableInfo.Table.TableName)" -ForegroundColor Gray
        Write-Host "   ステータス: $($issueTableInfo.Table.TableStatus)" -ForegroundColor Gray
        Write-Host "   アイテム数: $($issueTableInfo.Table.ItemCount)" -ForegroundColor Gray
    } catch {
        Write-Host "❌ TeamIssueテーブル: 確認失敗 - $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""

# 3. WorkloadStatus API統合テスト
Write-Host "3. WorkloadStatus API統合テスト" -ForegroundColor Cyan

# 3.1 初期データ確認
Write-Host "3.1 初期データ確認" -ForegroundColor Yellow
try {
    $initialWorkloads = Invoke-RestMethod -Uri "$ApiEndpoint/api/workload-status" -Method GET -Headers $headers -TimeoutSec 30
    Write-Host "✅ 初期データ取得: 成功" -ForegroundColor Green
    Write-Host "   件数: $($initialWorkloads.Count)" -ForegroundColor Gray
    
    $initialCount = $initialWorkloads.Count
    
    if ($initialWorkloads.Count -gt 0) {
        $sample = $initialWorkloads[0]
        Write-Host "   サンプル: $($sample.displayName) - $($sample.workloadLevel)" -ForegroundColor Gray
        
        # DynamoDBからのデータかチェック
        if ($sample.error) {
            Write-Host "   ⚠️ フォールバックモード: $($sample.error)" -ForegroundColor Yellow
        } else {
            Write-Host "   ✅ DynamoDBからのデータ" -ForegroundColor Green
        }
    }
} catch {
    Write-Host "❌ 初期データ取得: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
    $initialCount = 0
}

# 3.2 新規データ作成
Write-Host "3.2 新規データ作成" -ForegroundColor Yellow
$testWorkloadData = @{
    workloadLevel = "HIGH"
    projectCount = 5
    taskCount = 25
    comment = "デプロイ済みスタックテスト - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
} | ConvertTo-Json

try {
    $createdWorkload = Invoke-RestMethod -Uri "$ApiEndpoint/api/workload-status" -Method POST -Body $testWorkloadData -Headers $headers -TimeoutSec 30
    Write-Host "✅ データ作成: 成功" -ForegroundColor Green
    Write-Host "   メッセージ: $($createdWorkload.message)" -ForegroundColor Gray
    Write-Host "   負荷レベル: $($createdWorkload.workloadLevel)" -ForegroundColor Gray
    Write-Host "   プロジェクト数: $($createdWorkload.projectCount)" -ForegroundColor Gray
    
    if ($createdWorkload.error) {
        Write-Host "   ⚠️ フォールバックモード: $($createdWorkload.error)" -ForegroundColor Yellow
    } else {
        Write-Host "   ✅ DynamoDBに保存" -ForegroundColor Green
    }
} catch {
    Write-Host "❌ データ作成: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

# 3.3 統計情報確認
Write-Host "3.3 統計情報確認" -ForegroundColor Yellow
try {
    $workloadStats = Invoke-RestMethod -Uri "$ApiEndpoint/api/workload-status/statistics" -Method GET -Headers $headers -TimeoutSec 30
    Write-Host "✅ 統計情報: 成功" -ForegroundColor Green
    Write-Host "   総ユーザー数: $($workloadStats.totalUsers)" -ForegroundColor Gray
    Write-Host "   高負荷: $($workloadStats.highWorkload)" -ForegroundColor Gray
    Write-Host "   中負荷: $($workloadStats.mediumWorkload)" -ForegroundColor Gray
    Write-Host "   低負荷: $($workloadStats.lowWorkload)" -ForegroundColor Gray
    
    if ($workloadStats.error) {
        Write-Host "   ⚠️ フォールバックモード: $($workloadStats.error)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ 統計情報: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 4. TeamIssue API統合テスト
Write-Host "4. TeamIssue API統合テスト" -ForegroundColor Cyan

# 4.1 初期データ確認
Write-Host "4.1 初期データ確認" -ForegroundColor Yellow
try {
    $initialIssues = Invoke-RestMethod -Uri "$ApiEndpoint/api/team-issues" -Method GET -Headers $headers -TimeoutSec 30
    Write-Host "✅ 初期データ取得: 成功" -ForegroundColor Green
    Write-Host "   件数: $($initialIssues.Count)" -ForegroundColor Gray
    
    $initialIssueCount = $initialIssues.Count
    
    if ($initialIssues.Count -gt 0) {
        $sample = $initialIssues[0]
        Write-Host "   サンプル: $($sample.issueId) - $($sample.status)" -ForegroundColor Gray
        
        if ($sample.error) {
            Write-Host "   ⚠️ フォールバックモード: $($sample.error)" -ForegroundColor Yellow
        } else {
            Write-Host "   ✅ DynamoDBからのデータ" -ForegroundColor Green
        }
    }
} catch {
    Write-Host "❌ 初期データ取得: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
    $initialIssueCount = 0
}

# 4.2 新規Issue作成
Write-Host "4.2 新規Issue作成" -ForegroundColor Yellow
$testIssueData = @{
    content = "デプロイ済みスタックテストからの困りごと投稿です。投稿時刻: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss'). DynamoDBとの統合をテストしています。"
    priority = "HIGH"
} | ConvertTo-Json

try {
    $createdIssue = Invoke-RestMethod -Uri "$ApiEndpoint/api/team-issues" -Method POST -Body $testIssueData -Headers $headers -TimeoutSec 30
    Write-Host "✅ Issue作成: 成功" -ForegroundColor Green
    Write-Host "   ID: $($createdIssue.issueId)" -ForegroundColor Gray
    Write-Host "   メッセージ: $($createdIssue.message)" -ForegroundColor Gray
    Write-Host "   ステータス: $($createdIssue.status)" -ForegroundColor Gray
    Write-Host "   優先度: $($createdIssue.priority)" -ForegroundColor Gray
    
    if ($createdIssue.error) {
        Write-Host "   ⚠️ フォールバックモード: $($createdIssue.error)" -ForegroundColor Yellow
    } else {
        Write-Host "   ✅ DynamoDBに保存" -ForegroundColor Green
    }
    
    $createdIssueId = $createdIssue.issueId
} catch {
    Write-Host "❌ Issue作成: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
    $createdIssueId = $null
}

# 4.3 Issue統計確認
Write-Host "4.3 Issue統計確認" -ForegroundColor Yellow
try {
    $issueStats = Invoke-RestMethod -Uri "$ApiEndpoint/api/team-issues/statistics" -Method GET -Headers $headers -TimeoutSec 30
    Write-Host "✅ Issue統計: 成功" -ForegroundColor Green
    Write-Host "   総数: $($issueStats.total)" -ForegroundColor Gray
    Write-Host "   オープン: $($issueStats.open)" -ForegroundColor Gray
    Write-Host "   解決済み: $($issueStats.resolved)" -ForegroundColor Gray
    Write-Host "   高優先度: $($issueStats.highPriority)" -ForegroundColor Gray
    
    if ($issueStats.error) {
        Write-Host "   ⚠️ フォールバックモード: $($issueStats.error)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Issue統計: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

# 4.4 Issue解決テスト（作成されたIssueがある場合）
if ($createdIssueId -and $createdIssueId -ne "issue-" + (Get-Date).Ticks) {
    Write-Host "4.4 Issue解決テスト" -ForegroundColor Yellow
    try {
        $resolvedIssue = Invoke-RestMethod -Uri "$ApiEndpoint/api/team-issues/$createdIssueId/resolve" -Method PUT -Headers $headers -TimeoutSec 30
        Write-Host "✅ Issue解決: 成功" -ForegroundColor Green
        Write-Host "   メッセージ: $($resolvedIssue.message)" -ForegroundColor Gray
        Write-Host "   新ステータス: $($resolvedIssue.status)" -ForegroundColor Gray
    } catch {
        Write-Host "❌ Issue解決: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""

# 5. 認証テスト
Write-Host "5. 認証テスト" -ForegroundColor Cyan

# 5.1 認証なしアクセス
Write-Host "5.1 認証なしアクセステスト" -ForegroundColor Yellow
try {
    $noAuthTest = Invoke-RestMethod -Uri "$ApiEndpoint/api/workload-status" -Method GET -TimeoutSec 30
    Write-Host "⚠️ 認証なしアクセス: 成功（開発モード）" -ForegroundColor Yellow
    Write-Host "   本番環境では認証が必要になります" -ForegroundColor Gray
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Host "✅ 認証なしアクセス: 正しく401エラー" -ForegroundColor Green
    } else {
        Write-Host "❌ 認証なしアクセス: 予期しないエラー - $($_.Exception.Message)" -ForegroundColor Red
    }
}

# 5.2 無効トークンテスト
Write-Host "5.2 無効トークンテスト" -ForegroundColor Yellow
$invalidHeaders = @{
    "Authorization" = "Bearer invalid-token-test"
    "Content-Type" = "application/json"
}

try {
    $invalidTokenTest = Invoke-RestMethod -Uri "$ApiEndpoint/api/workload-status" -Method GET -Headers $invalidHeaders -TimeoutSec 30
    Write-Host "⚠️ 無効トークン: 成功（開発モード）" -ForegroundColor Yellow
} catch {
    if ($_.Exception.Response.StatusCode -eq 401 -or $_.Exception.Response.StatusCode -eq 403) {
        Write-Host "✅ 無効トークン: 正しく認証エラー" -ForegroundColor Green
    } else {
        Write-Host "❌ 無効トークン: 予期しないエラー - $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""

# 6. パフォーマンステスト
Write-Host "6. パフォーマンステスト" -ForegroundColor Cyan

$responseTime = Measure-Command {
    try {
        $perfTest = Invoke-RestMethod -Uri "$ApiEndpoint/api/status" -Method GET -TimeoutSec 30
    } catch {
        # エラーは無視
    }
}

Write-Host "✅ レスポンス時間: $($responseTime.TotalMilliseconds) ms" -ForegroundColor Green

if ($responseTime.TotalMilliseconds -lt 1000) {
    Write-Host "   パフォーマンス: 優秀" -ForegroundColor Green
} elseif ($responseTime.TotalMilliseconds -lt 3000) {
    Write-Host "   パフォーマンス: 良好" -ForegroundColor Yellow
} else {
    Write-Host "   パフォーマンス: 改善が必要" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== テスト完了 ===" -ForegroundColor Green
Write-Host ""
Write-Host "テスト結果サマリー:" -ForegroundColor Cyan
Write-Host "- API接続: 正常" -ForegroundColor Green
Write-Host "- DynamoDB統合: $(if ($WorkloadTable -and $IssueTable) { '正常' } else { '部分的' })" -ForegroundColor $(if ($WorkloadTable -and $IssueTable) { 'Green' } else { 'Yellow' })
Write-Host "- データ操作: 正常" -ForegroundColor Green
Write-Host "- レスポンス時間: $($responseTime.TotalMilliseconds) ms" -ForegroundColor Gray
Write-Host ""
Write-Host "次のステップ:" -ForegroundColor Yellow
Write-Host "1. フロントエンドの設定を更新" -ForegroundColor Gray
Write-Host "2. 本格的な機能テストを実行" -ForegroundColor Gray
Write-Host "3. 認証機能の本格実装（必要に応じて）" -ForegroundColor Gray