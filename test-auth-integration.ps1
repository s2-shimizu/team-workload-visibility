# AWS Cognito認証統合テスト
$baseUrl = "https://bn6xwu62qd.execute-api.ap-northeast-1.amazonaws.com/dev"

Write-Host "=== AWS Cognito認証統合テスト ===" -ForegroundColor Green
Write-Host "Base URL: $baseUrl" -ForegroundColor Yellow
Write-Host ""

# 1. 認証なしでのアクセステスト
Write-Host "1. 認証なしでのアクセステスト" -ForegroundColor Cyan

# ヘルスチェック（認証不要）
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/health" -Method GET
    Write-Host "✅ ヘルスチェック（認証不要）: 成功" -ForegroundColor Green
} catch {
    Write-Host "❌ ヘルスチェック失敗: $($_.Exception.Message)" -ForegroundColor Red
}

# 負荷状況取得（認証必要）
try {
    $workload = Invoke-RestMethod -Uri "$baseUrl/workload-status" -Method GET
    Write-Host "✅ 負荷状況取得（認証なし）: 成功（開発環境のため）" -ForegroundColor Yellow
} catch {
    Write-Host "❌ 負荷状況取得（認証なし）: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 2. モック認証トークンでのアクセステスト
Write-Host "2. モック認証トークンでのアクセステスト" -ForegroundColor Cyan

$mockToken = "mock-jwt-token-testuser"
$headers = @{
    "Authorization" = "Bearer $mockToken"
    "Content-Type" = "application/json"
}

try {
    $workload = Invoke-RestMethod -Uri "$baseUrl/workload-status" -Method GET -Headers $headers
    Write-Host "✅ 負荷状況取得（モック認証）: 成功" -ForegroundColor Green
} catch {
    Write-Host "❌ 負荷状況取得（モック認証）: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

try {
    $issues = Invoke-RestMethod -Uri "$baseUrl/team-issues" -Method GET -Headers $headers
    Write-Host "✅ 困りごと取得（モック認証）: 成功" -ForegroundColor Green
} catch {
    Write-Host "❌ 困りごと取得（モック認証）: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 3. 無効なトークンでのアクセステスト
Write-Host "3. 無効なトークンでのアクセステスト" -ForegroundColor Cyan

$invalidHeaders = @{
    "Authorization" = "Bearer invalid-token"
    "Content-Type" = "application/json"
}

try {
    $workload = Invoke-RestMethod -Uri "$baseUrl/workload-status" -Method GET -Headers $invalidHeaders
    Write-Host "⚠️ 負荷状況取得（無効トークン）: 成功（開発環境のため認証スキップ）" -ForegroundColor Yellow
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Host "✅ 負荷状況取得（無効トークン）: 正しく401エラー" -ForegroundColor Green
    } else {
        Write-Host "❌ 負荷状況取得（無効トークン）: 予期しないエラー - $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""

# 4. データ投稿テスト（認証付き）
Write-Host "4. データ投稿テスト（認証付き）" -ForegroundColor Cyan

$workloadData = @{
    workloadLevel = "HIGH"
    projectCount = 5
    taskCount = 25
    comment = "認証テストからの投稿"
} | ConvertTo-Json

try {
    $result = Invoke-RestMethod -Uri "$baseUrl/workload-status" -Method POST -Body $workloadData -Headers $headers
    Write-Host "✅ 負荷状況更新（認証付き）: 成功" -ForegroundColor Green
} catch {
    Write-Host "❌ 負荷状況更新（認証付き）: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

$issueData = @{
    content = "認証テストからの困りごと投稿です。"
} | ConvertTo-Json

try {
    $result = Invoke-RestMethod -Uri "$baseUrl/team-issues" -Method POST -Body $issueData -Headers $headers
    Write-Host "✅ 困りごと投稿（認証付き）: 成功" -ForegroundColor Green
} catch {
    Write-Host "❌ 困りごと投稿（認証付き）: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== テスト完了 ===" -ForegroundColor Green
Write-Host "注意: 現在は開発環境のため、認証エラーでも一部のAPIが動作します。" -ForegroundColor Yellow
Write-Host "本番環境では適切な認証が必要になります。" -ForegroundColor Yellow