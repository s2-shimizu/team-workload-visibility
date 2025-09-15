# 認証テストの問題診断スクリプト
Write-Host "=== 認証テスト問題診断 ===" -ForegroundColor Green
Write-Host ""

# 1. PowerShell実行ポリシーの確認
Write-Host "1. PowerShell実行ポリシーの確認" -ForegroundColor Cyan
try {
    $executionPolicy = Get-ExecutionPolicy
    Write-Host "✅ 実行ポリシー: $executionPolicy" -ForegroundColor Green
    
    if ($executionPolicy -eq "Restricted") {
        Write-Host "⚠️ 実行ポリシーが制限されています。以下のコマンドで変更してください:" -ForegroundColor Yellow
        Write-Host "   Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser" -ForegroundColor Gray
    }
} catch {
    Write-Host "❌ 実行ポリシー確認エラー: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 2. AWS CLIの確認
Write-Host "2. AWS CLIの確認" -ForegroundColor Cyan
try {
    $awsVersion = aws --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ AWS CLI: $awsVersion" -ForegroundColor Green
    } else {
        Write-Host "❌ AWS CLIが見つかりません" -ForegroundColor Red
        Write-Host "   インストール方法: https://aws.amazon.com/cli/" -ForegroundColor Gray
    }
} catch {
    Write-Host "❌ AWS CLI確認エラー: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   AWS CLIをインストールしてください" -ForegroundColor Yellow
}

Write-Host ""

# 3. AWS認証情報の確認
Write-Host "3. AWS認証情報の確認" -ForegroundColor Cyan
try {
    $identity = aws sts get-caller-identity --output json 2>&1
    if ($LASTEXITCODE -eq 0) {
        $identityObj = $identity | ConvertFrom-Json
        Write-Host "✅ AWS認証: $($identityObj.Arn)" -ForegroundColor Green
        Write-Host "   アカウントID: $($identityObj.Account)" -ForegroundColor Gray
        Write-Host "   ユーザーID: $($identityObj.UserId)" -ForegroundColor Gray
    } else {
        Write-Host "❌ AWS認証が設定されていません" -ForegroundColor Red
        Write-Host "   設定方法: aws configure" -ForegroundColor Gray
        Write-Host "   エラー詳細: $identity" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ AWS認証確認エラー: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 4. ネットワーク接続の確認
Write-Host "4. ネットワーク接続の確認" -ForegroundColor Cyan
$testUrls = @(
    "https://cognito-idp.ap-northeast-1.amazonaws.com",
    "https://bn6xwu62qd.execute-api.ap-northeast-1.amazonaws.com/dev/health"
)

foreach ($url in $testUrls) {
    try {
        $response = Invoke-WebRequest -Uri $url -Method GET -TimeoutSec 10 -UseBasicParsing
        Write-Host "✅ 接続確認 ($url): HTTP $($response.StatusCode)" -ForegroundColor Green
    } catch {
        Write-Host "❌ 接続確認 ($url): $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""

# 5. 必要なPowerShellモジュールの確認
Write-Host "5. PowerShellモジュールの確認" -ForegroundColor Cyan
$requiredModules = @("Microsoft.PowerShell.Utility")

foreach ($module in $requiredModules) {
    try {
        $moduleInfo = Get-Module -Name $module -ListAvailable
        if ($moduleInfo) {
            Write-Host "✅ モジュール ($module): 利用可能" -ForegroundColor Green
        } else {
            Write-Host "⚠️ モジュール ($module): 見つかりません" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "❌ モジュール確認エラー ($module): $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""

# 6. 設定ファイルの確認
Write-Host "6. 設定ファイルの確認" -ForegroundColor Cyan
$configFiles = @(
    "frontend/js/aws-config.js",
    "cognito-setup.json",
    "production-auth-config.json"
)

foreach ($file in $configFiles) {
    if (Test-Path $file) {
        Write-Host "✅ 設定ファイル ($file): 存在" -ForegroundColor Green
    } else {
        Write-Host "⚠️ 設定ファイル ($file): 見つかりません" -ForegroundColor Yellow
    }
}

Write-Host ""

# 7. 簡単な認証テスト
Write-Host "7. 簡単な認証テスト" -ForegroundColor Cyan
try {
    # ヘルスチェックエンドポイントのテスト
    $healthUrl = "https://bn6xwu62qd.execute-api.ap-northeast-1.amazonaws.com/dev/health"
    $healthResponse = Invoke-RestMethod -Uri $healthUrl -Method GET -TimeoutSec 10
    Write-Host "✅ ヘルスチェック: 成功" -ForegroundColor Green
    Write-Host "   レスポンス: $($healthResponse | ConvertTo-Json -Compress)" -ForegroundColor Gray
} catch {
    Write-Host "❌ ヘルスチェック: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 8. Cognitoサービスの確認
Write-Host "8. Cognitoサービスの確認" -ForegroundColor Cyan
try {
    $userPools = aws cognito-idp list-user-pools --max-items 5 --region ap-northeast-1 --output json 2>&1
    if ($LASTEXITCODE -eq 0) {
        $poolsObj = $userPools | ConvertFrom-Json
        Write-Host "✅ Cognitoアクセス: 成功" -ForegroundColor Green
        Write-Host "   利用可能なユーザープール数: $($poolsObj.UserPools.Count)" -ForegroundColor Gray
        
        foreach ($pool in $poolsObj.UserPools) {
            Write-Host "   - $($pool.Name) (ID: $($pool.Id))" -ForegroundColor Gray
        }
    } else {
        Write-Host "❌ Cognitoアクセス: 失敗" -ForegroundColor Red
        Write-Host "   エラー詳細: $userPools" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Cognitoアクセスエラー: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== 診断完了 ===" -ForegroundColor Green
Write-Host ""
Write-Host "Problem resolution guide:" -ForegroundColor Cyan
Write-Host "1. PowerShell Execution Policy: Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser" -ForegroundColor Gray
Write-Host "2. AWS CLI not installed: Download from https://aws.amazon.com/cli/" -ForegroundColor Gray
Write-Host "3. AWS credentials not configured: Run 'aws configure'" -ForegroundColor Gray
Write-Host "4. Network issues: Check proxy settings and firewall" -ForegroundColor Gray
Write-Host "5. Cognito permissions: Check IAM permissions" -ForegroundColor Gray