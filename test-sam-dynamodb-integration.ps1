# SAM DynamoDB統合テストスクリプト
param(
    [string]$BaseUrl = "https://bn6xwu62qd.execute-api.ap-northeast-1.amazonaws.com/dev",
    [string]$Environment = "dev",
    [string]$WorkloadTableName = "",
    [string]$IssueTableName = ""
)

Write-Host "=== SAM DynamoDB統合テスト ===" -ForegroundColor Green
Write-Host "Base URL: $BaseUrl" -ForegroundColor Yellow
Write-Host "Environment: $Environment" -ForegroundColor Yellow
Write-Host ""

# 環境変数の設定（SAMテンプレートから渡される想定）
if ($WorkloadTableName) {
    $env:WORKLOAD_STATUS_TABLE = $WorkloadTableName
    Write-Host "WorkloadStatus Table: $WorkloadTableName" -ForegroundColor Gray
}

if ($IssueTableName) {
    $env:TEAM_ISSUE_TABLE = $IssueTableName
    Write-Host "TeamIssue Table: $IssueTableName" -ForegroundColor Gray
}

Write-Host ""

# 1. ヘルスチェック（DynamoDB統合確認）
Write-Host "1. ヘルスチェック（DynamoDB統合確認）" -ForegroundColor Cyan
try {
    $health = Invoke-RestMethod -Uri "$BaseUrl/api/status" -Method GET -TimeoutSec 30
    Write-Host "✅ ヘルスチェック: 成功" -ForegroundColor Green
    Write-Host "   ステータス: $($health.status)" -ForegroundColor Gray
    Write-Host "   データベース: $($health.database)" -ForegroundColor Gray
    Write-Host "   バージョン: $($health.version)" -ForegroundColor Gray
    Write-Host "   タイムスタンプ: $($health.timestamp)" -ForegroundColor Gray
} catch {
    Write-Host "❌ ヘルスチェック: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   APIが起動していない可能性があります" -ForegroundColor Yellow
}

Write-Host ""

# 2. 認証テスト（モックトークン）
Write-Host "2. 認証テスト（モックトークン）" -ForegroundColor Cyan
$mockHeaders = @{
    "Authorization" = "Bearer mock-jwt-token-testuser"
    "Content-Type" = "application/json"
}

try {
    $authTest = Invoke-RestMethod -Uri "$BaseUrl/api/workload-status/my" -Method GET -Headers $mockHeaders -TimeoutSec 30
    Write-Host "✅ 認証テスト: 成功" -ForegroundColor Green
    Write-Host "   ユーザーID: $($authTest.userId)" -ForegroundColor Gray
    Write-Host "   負荷レベル: $($authTest.workloadLevel)" -ForegroundColor Gray
} catch {
    Write-Host "❌ 認証テスト: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 3. WorkloadStatus DynamoDB統合テスト
Write-Host "3. WorkloadStatus DynamoDB統合テスト" -ForegroundColor Cyan

# 3.1 全ての負荷状況を取得
Write-Host "3.1 全ての負荷状況を取得" -ForegroundColor Yellow
try {
    $allWorkloads = Invoke-RestMethod -Uri "$BaseUrl/api/workload-status" -Method GET -Headers $mockHeaders -TimeoutSec 30
    Write-Host "✅ 負荷状況取得: 成功" -ForegroundColor Green
    Write-Host "   取得件数: $($allWorkloads.Count)" -ForegroundColor Gray
    
    if ($allWorkloads.Count -gt 0) {
        $sample = $allWorkloads[0]
        Write-Host "   サンプルデータ:" -ForegroundColor Gray
        Write-Host "     ユーザー: $($sample.displayName)" -ForegroundColor Gray
        Write-Host "     負荷レベル: $($sample.workloadLevel)" -ForegroundColor Gray
        Write-Host "     プロジェクト数: $($sample.projectCount)" -ForegroundColor Gray
        Write-Host "     タスク数: $($sample.taskCount)" -ForegroundColor Gray
        
        # DynamoDBからのデータかフォールバックかを判定
        if ($sample.error) {
            Write-Host "   ⚠️ フォールバックモード: $($sample.error)" -ForegroundColor Yellow
        } else {
            Write-Host "   ✅ DynamoDBからのデータ" -ForegroundColor Green
        }
    } else {
        Write-Host "   ⚠️ データが空です（DynamoDBテーブルが空の可能性）" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ 負荷状況取得: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

# 3.2 負荷状況の更新
Write-Host "3.2 負荷状況の更新" -ForegroundColor Yellow
$workloadUpdateData = @{
    workloadLevel = "HIGH"
    projectCount = 4
    taskCount = 20
    comment = "SAM DynamoDB統合テストからの更新 - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
} | ConvertTo-Json

try {
    $updatedWorkload = Invoke-RestMethod -Uri "$BaseUrl/api/workload-status" -Method POST -Body $workloadUpdateData -Headers $mockHeaders -TimeoutSec 30
    Write-Host "✅ 負荷状況更新: 成功" -ForegroundColor Green
    Write-Host "   更新メッセージ: $($updatedWorkload.message)" -ForegroundColor Gray
    Write-Host "   新しい負荷レベル: $($updatedWorkload.workloadLevel)" -ForegroundColor Gray
    Write-Host "   プロジェクト数: $($updatedWorkload.projectCount)" -ForegroundColor Gray
    Write-Host "   タスク数: $($updatedWorkload.taskCount)" -ForegroundColor Gray
    
    if ($updatedWorkload.error) {
        Write-Host "   ⚠️ フォールバックモード: $($updatedWorkload.error)" -ForegroundColor Yellow
    } else {
        Write-Host "   ✅ DynamoDBに保存されました" -ForegroundColor Green
    }
} catch {
    Write-Host "❌ 負荷状況更新: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

# 3.3 統計情報の取得
Write-Host "3.3 負荷状況統計の取得" -ForegroundColor Yellow
try {
    $workloadStats = Invoke-RestMethod -Uri "$BaseUrl/api/workload-status/statistics" -Method GET -Headers $mockHeaders -TimeoutSec 30
    Write-Host "✅ 負荷状況統計: 成功" -ForegroundColor Green
    Write-Host "   総ユーザー数: $($workloadStats.totalUsers)" -ForegroundColor Gray
    Write-Host "   高負荷: $($workloadStats.highWorkload)" -ForegroundColor Gray
    Write-Host "   中負荷: $($workloadStats.mediumWorkload)" -ForegroundColor Gray
    Write-Host "   低負荷: $($workloadStats.lowWorkload)" -ForegroundColor Gray
    Write-Host "   平均プロジェクト数: $($workloadStats.averageProjectCount)" -ForegroundColor Gray
    Write-Host "   平均タスク数: $($workloadStats.averageTaskCount)" -ForegroundColor Gray
    
    if ($workloadStats.error) {
        Write-Host "   ⚠️ フォールバックモード: $($workloadStats.error)" -ForegroundColor Yellow
    } else {
        Write-Host "   ✅ DynamoDBからの統計" -ForegroundColor Green
    }
} catch {
    Write-Host "❌ 負荷状況統計: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 4. TeamIssue DynamoDB統合テスト
Write-Host "4. TeamIssue DynamoDB統合テスト" -ForegroundColor Cyan

# 4.1 全ての困りごとを取得
Write-Host "4.1 全ての困りごとを取得" -ForegroundColor Yellow
try {
    $allIssues = Invoke-RestMethod -Uri "$BaseUrl/api/team-issues" -Method GET -Headers $mockHeaders -TimeoutSec 30
    Write-Host "✅ 困りごと取得: 成功" -ForegroundColor Green
    Write-Host "   取得件数: $($allIssues.Count)" -ForegroundColor Gray
    
    if ($allIssues.Count -gt 0) {
        $sample = $allIssues[0]
        Write-Host "   サンプルデータ:" -ForegroundColor Gray
        Write-Host "     ID: $($sample.issueId)" -ForegroundColor Gray
        Write-Host "     ユーザー: $($sample.displayName)" -ForegroundColor Gray
        Write-Host "     ステータス: $($sample.status)" -ForegroundColor Gray
        Write-Host "     優先度: $($sample.priority)" -ForegroundColor Gray
        Write-Host "     内容: $($sample.content.Substring(0, [Math]::Min(50, $sample.content.Length)))..." -ForegroundColor Gray
        
        if ($sample.error) {
            Write-Host "   ⚠️ フォールバックモード: $($sample.error)" -ForegroundColor Yellow
        } else {
            Write-Host "   ✅ DynamoDBからのデータ" -ForegroundColor Green
        }
    } else {
        Write-Host "   ⚠️ データが空です（DynamoDBテーブルが空の可能性）" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ 困りごと取得: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

# 4.2 新しい困りごとの投稿
Write-Host "4.2 新しい困りごとの投稿" -ForegroundColor Yellow
$issueData = @{
    content = "SAM DynamoDB統合テストからの困りごと投稿です。投稿時刻: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss'). DynamoDBテーブルとの連携をテストしています。"
    priority = "MEDIUM"
} | ConvertTo-Json

try {
    $newIssue = Invoke-RestMethod -Uri "$BaseUrl/api/team-issues" -Method POST -Body $issueData -Headers $mockHeaders -TimeoutSec 30
    Write-Host "✅ 困りごと投稿: 成功" -ForegroundColor Green
    Write-Host "   投稿メッセージ: $($newIssue.message)" -ForegroundColor Gray
    Write-Host "   新しいID: $($newIssue.issueId)" -ForegroundColor Gray
    Write-Host "   ステータス: $($newIssue.status)" -ForegroundColor Gray
    Write-Host "   優先度: $($newIssue.priority)" -ForegroundColor Gray
    
    if ($newIssue.error) {
        Write-Host "   ⚠️ フォールバックモード: $($newIssue.error)" -ForegroundColor Yellow
    } else {
        Write-Host "   ✅ DynamoDBに保存されました" -ForegroundColor Green
    }
    
    $createdIssueId = $newIssue.issueId
} catch {
    Write-Host "❌ 困りごと投稿: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
    $createdIssueId = $null
}

# 4.3 困りごと統計の取得
Write-Host "4.3 困りごと統計の取得" -ForegroundColor Yellow
try {
    $issueStats = Invoke-RestMethod -Uri "$BaseUrl/api/team-issues/statistics" -Method GET -Headers $mockHeaders -TimeoutSec 30
    Write-Host "✅ 困りごと統計: 成功" -ForegroundColor Green
    Write-Host "   総数: $($issueStats.total)" -ForegroundColor Gray
    Write-Host "   オープン: $($issueStats.open)" -ForegroundColor Gray
    Write-Host "   解決済み: $($issueStats.resolved)" -ForegroundColor Gray
    Write-Host "   高優先度: $($issueStats.highPriority)" -ForegroundColor Gray
    Write-Host "   中優先度: $($issueStats.mediumPriority)" -ForegroundColor Gray
    Write-Host "   低優先度: $($issueStats.lowPriority)" -ForegroundColor Gray
    
    if ($issueStats.error) {
        Write-Host "   ⚠️ フォールバックモード: $($issueStats.error)" -ForegroundColor Yellow
    } else {
        Write-Host "   ✅ DynamoDBからの統計" -ForegroundColor Green
    }
} catch {
    Write-Host "❌ 困りごと統計: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 5. 総合評価
Write-Host "5. 総合評価" -ForegroundColor Cyan

$dynamodbWorking = $true
$apiWorking = $true

# APIの動作確認
try {
    $finalHealth = Invoke-RestMethod -Uri "$BaseUrl/api/status" -Method GET -TimeoutSec 10
    if ($finalHealth.status -eq "OK") {
        Write-Host "✅ API動作: 正常" -ForegroundColor Green
    } else {
        Write-Host "⚠️ API動作: 異常" -ForegroundColor Yellow
        $apiWorking = $false
    }
} catch {
    Write-Host "❌ API動作: 失敗" -ForegroundColor Red
    $apiWorking = $false
}

# DynamoDB統合の確認
try {
    $testWorkload = Invoke-RestMethod -Uri "$BaseUrl/api/workload-status" -Method GET -Headers $mockHeaders -TimeoutSec 10
    $testIssues = Invoke-RestMethod -Uri "$BaseUrl/api/team-issues" -Method GET -Headers $mockHeaders -TimeoutSec 10
    
    $hasWorkloadError = $testWorkload | Where-Object { $_.error }
    $hasIssueError = $testIssues | Where-Object { $_.error }
    
    if ($hasWorkloadError -or $hasIssueError) {
        Write-Host "⚠️ DynamoDB統合: フォールバックモード" -ForegroundColor Yellow
        $dynamodbWorking = $false
    } else {
        Write-Host "✅ DynamoDB統合: 正常" -ForegroundColor Green
    }
} catch {
    Write-Host "❌ DynamoDB統合: 失敗" -ForegroundColor Red
    $dynamodbWorking = $false
}

Write-Host ""
Write-Host "=== テスト結果サマリー ===" -ForegroundColor Green

if ($apiWorking -and $dynamodbWorking) {
    Write-Host "🎉 全ての機能が正常に動作しています！" -ForegroundColor Green
    Write-Host "   - API: 正常動作" -ForegroundColor Green
    Write-Host "   - DynamoDB統合: 正常動作" -ForegroundColor Green
} elseif ($apiWorking -and -not $dynamodbWorking) {
    Write-Host "⚠️ APIは動作していますが、DynamoDBはフォールバックモードです" -ForegroundColor Yellow
    Write-Host "   - API: 正常動作" -ForegroundColor Green
    Write-Host "   - DynamoDB統合: フォールバックモード" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "推奨対応:" -ForegroundColor Cyan
    Write-Host "1. AWS認証情報を確認" -ForegroundColor Gray
    Write-Host "2. DynamoDBテーブルの存在を確認" -ForegroundColor Gray
    Write-Host "3. IAM権限を確認" -ForegroundColor Gray
    Write-Host "4. 環境変数WORKLOAD_STATUS_TABLE, TEAM_ISSUE_TABLEを確認" -ForegroundColor Gray
} else {
    Write-Host "❌ 問題が発生しています" -ForegroundColor Red
    Write-Host "   - API: $(if ($apiWorking) { '正常動作' } else { '異常' })" -ForegroundColor $(if ($apiWorking) { 'Green' } else { 'Red' })
    Write-Host "   - DynamoDB統合: $(if ($dynamodbWorking) { '正常動作' } else { 'フォールバックモード' })" -ForegroundColor $(if ($dynamodbWorking) { 'Green' } else { 'Yellow' })
}

Write-Host ""
Write-Host "使用方法:" -ForegroundColor Cyan
Write-Host "  基本テスト: .\test-sam-dynamodb-integration.ps1" -ForegroundColor Gray
Write-Host "  テーブル指定: .\test-sam-dynamodb-integration.ps1 -WorkloadTableName 'MyWorkloadTable' -IssueTableName 'MyIssueTable'" -ForegroundColor Gray
Write-Host "  本番環境: .\test-sam-dynamodb-integration.ps1 -BaseUrl 'https://prod-api.example.com' -Environment prod" -ForegroundColor Gray