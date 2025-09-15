# 本番環境 AWS Cognito認証統合テスト
param(
    [string]$Environment = "prod",
    [string]$TestUserEmail = "",
    [string]$TestUserPassword = ""
)

# 設定
$config = @{
    dev = @{
        baseUrl = "https://bn6xwu62qd.execute-api.ap-northeast-1.amazonaws.com/dev"
        userPoolId = "ap-northeast-1_S0zRV4ais"
        clientId = "7nue9hv9e54sdrcvorl990q1t6"
    }
    prod = @{
        baseUrl = "https://your-prod-api-gateway-url.amazonaws.com/prod"
        userPoolId = "ap-northeast-1_S0zRV4ais"  # 本番用に更新が必要
        clientId = "7nue9hv9e54sdrcvorl990q1t6"   # 本番用に更新が必要
    }
}

$currentConfig = $config[$Environment]
$region = "ap-northeast-1"

Write-Host "=== 本番環境 AWS Cognito認証統合テスト ===" -ForegroundColor Green
Write-Host "環境: $Environment" -ForegroundColor Yellow
Write-Host "Base URL: $($currentConfig.baseUrl)" -ForegroundColor Yellow
Write-Host "User Pool ID: $($currentConfig.userPoolId)" -ForegroundColor Yellow
Write-Host ""

# AWS CLIの確認
try {
    $awsVersion = aws --version
    Write-Host "✅ AWS CLI確認: $awsVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ AWS CLIが見つかりません。AWS CLIをインストールしてください。" -ForegroundColor Red
    exit 1
}

# 1. 認証なしでのアクセステスト
Write-Host "1. 認証なしでのアクセステスト" -ForegroundColor Cyan

# ヘルスチェック（認証不要）
try {
    $health = Invoke-RestMethod -Uri "$($currentConfig.baseUrl)/health" -Method GET -TimeoutSec 30
    Write-Host "✅ ヘルスチェック（認証不要）: 成功" -ForegroundColor Green
    Write-Host "   レスポンス: $($health | ConvertTo-Json -Compress)" -ForegroundColor Gray
} catch {
    Write-Host "❌ ヘルスチェック失敗: $($_.Exception.Message)" -ForegroundColor Red
}

# 認証が必要なエンドポイントのテスト（認証なし）
try {
    $workload = Invoke-RestMethod -Uri "$($currentConfig.baseUrl)/workload-status" -Method GET -TimeoutSec 30
    Write-Host "⚠️ 負荷状況取得（認証なし）: 成功（認証が無効化されている可能性）" -ForegroundColor Yellow
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Host "✅ 負荷状況取得（認証なし）: 正しく401エラー" -ForegroundColor Green
    } else {
        Write-Host "❌ 負荷状況取得（認証なし）: 予期しないエラー - $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""

# 2. テストユーザーでの認証テスト
if ($TestUserEmail -and $TestUserPassword) {
    Write-Host "2. テストユーザーでの認証テスト" -ForegroundColor Cyan
    Write-Host "テストユーザー: $TestUserEmail" -ForegroundColor Gray
    
    try {
        # AWS CLIを使用してCognitoトークンを取得
        $authResult = aws cognito-idp initiate-auth `
            --auth-flow USER_PASSWORD_AUTH `
            --client-id $currentConfig.clientId `
            --auth-parameters "USERNAME=$TestUserEmail,PASSWORD=$TestUserPassword" `
            --region $region `
            --output json | ConvertFrom-Json
        
        $accessToken = $authResult.AuthenticationResult.AccessToken
        $idToken = $authResult.AuthenticationResult.IdToken
        
        Write-Host "✅ Cognito認証: 成功" -ForegroundColor Green
        Write-Host "   Access Token取得: 成功" -ForegroundColor Gray
        Write-Host "   ID Token取得: 成功" -ForegroundColor Gray
        
        # 認証付きAPIテスト
        $authHeaders = @{
            "Authorization" = "Bearer $idToken"
            "Content-Type" = "application/json"
        }
        
        # 負荷状況取得テスト
        try {
            $workload = Invoke-RestMethod -Uri "$($currentConfig.baseUrl)/workload-status" -Method GET -Headers $authHeaders -TimeoutSec 30
            Write-Host "✅ 負荷状況取得（認証付き）: 成功" -ForegroundColor Green
            Write-Host "   レスポンス: $($workload | ConvertTo-Json -Compress)" -ForegroundColor Gray
        } catch {
            Write-Host "❌ 負荷状況取得（認証付き）: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
        }
        
        # 困りごと取得テスト
        try {
            $issues = Invoke-RestMethod -Uri "$($currentConfig.baseUrl)/team-issues" -Method GET -Headers $authHeaders -TimeoutSec 30
            Write-Host "✅ 困りごと取得（認証付き）: 成功" -ForegroundColor Green
            Write-Host "   レスポンス: $($issues | ConvertTo-Json -Compress)" -ForegroundColor Gray
        } catch {
            Write-Host "❌ 困りごと取得（認証付き）: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
        }
        
        # データ投稿テスト
        Write-Host ""
        Write-Host "3. データ投稿テスト（認証付き）" -ForegroundColor Cyan
        
        $workloadData = @{
            workloadLevel = "MEDIUM"
            projectCount = 3
            taskCount = 15
            comment = "本番環境認証テストからの投稿 - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
        } | ConvertTo-Json
        
        try {
            $result = Invoke-RestMethod -Uri "$($currentConfig.baseUrl)/workload-status" -Method POST -Body $workloadData -Headers $authHeaders -TimeoutSec 30
            Write-Host "✅ 負荷状況更新（認証付き）: 成功" -ForegroundColor Green
            Write-Host "   レスポンス: $($result | ConvertTo-Json -Compress)" -ForegroundColor Gray
        } catch {
            Write-Host "❌ 負荷状況更新（認証付き）: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
        }
        
        $issueData = @{
            content = "本番環境認証テストからの困りごと投稿です。投稿時刻: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
        } | ConvertTo-Json
        
        try {
            $result = Invoke-RestMethod -Uri "$($currentConfig.baseUrl)/team-issues" -Method POST -Body $issueData -Headers $authHeaders -TimeoutSec 30
            Write-Host "✅ 困りごと投稿（認証付き）: 成功" -ForegroundColor Green
            Write-Host "   レスポンス: $($result | ConvertTo-Json -Compress)" -ForegroundColor Gray
        } catch {
            Write-Host "❌ 困りごと投稿（認証付き）: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
        }
        
    } catch {
        Write-Host "❌ Cognito認証失敗: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "   ユーザー名とパスワードを確認してください。" -ForegroundColor Yellow
    }
} else {
    Write-Host "2. テストユーザー認証をスキップ" -ForegroundColor Yellow
    Write-Host "   テストユーザーの認証を行うには、-TestUserEmail と -TestUserPassword パラメータを指定してください。" -ForegroundColor Gray
}

Write-Host ""

# 4. 無効なトークンでのアクセステスト
Write-Host "4. 無効なトークンでのアクセステスト" -ForegroundColor Cyan

$invalidHeaders = @{
    "Authorization" = "Bearer invalid-token-for-production-test"
    "Content-Type" = "application/json"
}

try {
    $workload = Invoke-RestMethod -Uri "$($currentConfig.baseUrl)/workload-status" -Method GET -Headers $invalidHeaders -TimeoutSec 30
    Write-Host "⚠️ 負荷状況取得（無効トークン）: 成功（認証が無効化されている可能性）" -ForegroundColor Yellow
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Host "✅ 負荷状況取得（無効トークン）: 正しく401エラー" -ForegroundColor Green
    } elseif ($_.Exception.Response.StatusCode -eq 403) {
        Write-Host "✅ 負荷状況取得（無効トークン）: 正しく403エラー" -ForegroundColor Green
    } else {
        Write-Host "❌ 負荷状況取得（無効トークン）: 予期しないエラー - $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""

# 5. トークンの有効性確認
if ($TestUserEmail -and $TestUserPassword) {
    Write-Host "5. トークンの有効性確認" -ForegroundColor Cyan
    
    try {
        # 現在のユーザー情報を取得
        $userInfo = aws cognito-idp get-user `
            --access-token $accessToken `
            --region $region `
            --output json | ConvertFrom-Json
        
        Write-Host "✅ ユーザー情報取得: 成功" -ForegroundColor Green
        Write-Host "   ユーザー名: $($userInfo.Username)" -ForegroundColor Gray
        Write-Host "   ユーザー属性数: $($userInfo.UserAttributes.Count)" -ForegroundColor Gray
        
        # ユーザー属性の表示
        foreach ($attr in $userInfo.UserAttributes) {
            if ($attr.Name -eq "email" -or $attr.Name -eq "name") {
                Write-Host "   $($attr.Name): $($attr.Value)" -ForegroundColor Gray
            }
        }
        
    } catch {
        Write-Host "❌ ユーザー情報取得失敗: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=== テスト完了 ===" -ForegroundColor Green
Write-Host ""
Write-Host "使用方法:" -ForegroundColor Cyan
Write-Host "  開発環境テスト: .\test-auth-production.ps1 -Environment dev" -ForegroundColor Gray
Write-Host "  本番環境テスト: .\test-auth-production.ps1 -Environment prod -TestUserEmail user@example.com -TestUserPassword password" -ForegroundColor Gray
Write-Host ""
Write-Host "注意事項:" -ForegroundColor Yellow
Write-Host "- 本番環境のURL、User Pool ID、Client IDを正しく設定してください" -ForegroundColor Gray
Write-Host "- AWS CLIが設定されている必要があります" -ForegroundColor Gray
Write-Host "- テストユーザーは事前にCognitoに登録されている必要があります" -ForegroundColor Gray