# 本番環境認証テスト用セットアップスクリプト
param(
    [string]$UserPoolId = "",
    [string]$ClientId = "",
    [string]$ProductionApiUrl = "",
    [switch]$CreateTestUser = $false,
    [string]$TestUserEmail = "",
    [string]$TestUserPassword = ""
)

Write-Host "=== 本番環境認証テスト セットアップ ===" -ForegroundColor Green
Write-Host ""

# AWS CLIの確認
try {
    $awsVersion = aws --version
    Write-Host "✅ AWS CLI確認: $awsVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ AWS CLIが見つかりません。AWS CLIをインストールしてください。" -ForegroundColor Red
    exit 1
}

# AWS認証情報の確認
try {
    $identity = aws sts get-caller-identity --output json | ConvertFrom-Json
    Write-Host "✅ AWS認証確認: $($identity.Arn)" -ForegroundColor Green
} catch {
    Write-Host "❌ AWS認証が設定されていません。aws configure を実行してください。" -ForegroundColor Red
    exit 1
}

Write-Host ""

# 1. Cognitoユーザープールの確認
Write-Host "1. Cognitoユーザープールの確認" -ForegroundColor Cyan

if ($UserPoolId) {
    try {
        $userPool = aws cognito-idp describe-user-pool --user-pool-id $UserPoolId --region ap-northeast-1 --output json | ConvertFrom-Json
        Write-Host "✅ ユーザープール確認: $($userPool.UserPool.Name)" -ForegroundColor Green
        Write-Host "   作成日: $($userPool.UserPool.CreationDate)" -ForegroundColor Gray
        Write-Host "   ステータス: $($userPool.UserPool.Status)" -ForegroundColor Gray
    } catch {
        Write-Host "❌ ユーザープール確認失敗: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "⚠️ User Pool IDが指定されていません。" -ForegroundColor Yellow
    
    # 利用可能なユーザープールを一覧表示
    try {
        $userPools = aws cognito-idp list-user-pools --max-items 10 --region ap-northeast-1 --output json | ConvertFrom-Json
        Write-Host "利用可能なユーザープール:" -ForegroundColor Gray
        foreach ($pool in $userPools.UserPools) {
            Write-Host "  - $($pool.Name) (ID: $($pool.Id))" -ForegroundColor Gray
        }
    } catch {
        Write-Host "❌ ユーザープール一覧取得失敗: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""

# 2. アプリクライアントの確認
Write-Host "2. アプリクライアントの確認" -ForegroundColor Cyan

if ($UserPoolId -and $ClientId) {
    try {
        $client = aws cognito-idp describe-user-pool-client --user-pool-id $UserPoolId --client-id $ClientId --region ap-northeast-1 --output json | ConvertFrom-Json
        Write-Host "✅ アプリクライアント確認: $($client.UserPoolClient.ClientName)" -ForegroundColor Green
        Write-Host "   認証フロー: $($client.UserPoolClient.ExplicitAuthFlows -join ', ')" -ForegroundColor Gray
    } catch {
        Write-Host "❌ アプリクライアント確認失敗: $($_.Exception.Message)" -ForegroundColor Red
    }
} elseif ($UserPoolId) {
    Write-Host "⚠️ Client IDが指定されていません。" -ForegroundColor Yellow
    
    # 利用可能なクライアントを一覧表示
    try {
        $clients = aws cognito-idp list-user-pool-clients --user-pool-id $UserPoolId --region ap-northeast-1 --output json | ConvertFrom-Json
        Write-Host "利用可能なアプリクライアント:" -ForegroundColor Gray
        foreach ($client in $clients.UserPoolClients) {
            Write-Host "  - $($client.ClientName) (ID: $($client.ClientId))" -ForegroundColor Gray
        }
    } catch {
        Write-Host "❌ アプリクライアント一覧取得失敗: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""

# 3. テストユーザーの作成（オプション）
if ($CreateTestUser -and $TestUserEmail -and $TestUserPassword -and $UserPoolId) {
    Write-Host "3. テストユーザーの作成" -ForegroundColor Cyan
    
    try {
        # ユーザーの存在確認
        try {
            $existingUser = aws cognito-idp admin-get-user --user-pool-id $UserPoolId --username $TestUserEmail --region ap-northeast-1 --output json | ConvertFrom-Json
            Write-Host "⚠️ ユーザーは既に存在します: $TestUserEmail" -ForegroundColor Yellow
        } catch {
            # ユーザーが存在しない場合、作成
            $createResult = aws cognito-idp admin-create-user `
                --user-pool-id $UserPoolId `
                --username $TestUserEmail `
                --user-attributes Name=email,Value=$TestUserEmail Name=email_verified,Value=true `
                --temporary-password $TestUserPassword `
                --message-action SUPPRESS `
                --region ap-northeast-1 `
                --output json | ConvertFrom-Json
            
            Write-Host "✅ テストユーザー作成: $TestUserEmail" -ForegroundColor Green
            
            # パスワードを永続化
            $setPasswordResult = aws cognito-idp admin-set-user-password `
                --user-pool-id $UserPoolId `
                --username $TestUserEmail `
                --password $TestUserPassword `
                --permanent `
                --region ap-northeast-1
            
            Write-Host "✅ パスワード設定完了" -ForegroundColor Green
        }
    } catch {
        Write-Host "❌ テストユーザー作成失敗: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""

# 4. API Gateway エンドポイントの確認
Write-Host "4. API Gateway エンドポイントの確認" -ForegroundColor Cyan

if ($ProductionApiUrl) {
    try {
        $response = Invoke-RestMethod -Uri "$ProductionApiUrl/health" -Method GET -TimeoutSec 10
        Write-Host "✅ API Gateway接続確認: 成功" -ForegroundColor Green
        Write-Host "   レスポンス: $($response | ConvertTo-Json -Compress)" -ForegroundColor Gray
    } catch {
        Write-Host "❌ API Gateway接続確認失敗: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "⚠️ 本番環境API URLが指定されていません。" -ForegroundColor Yellow
}

Write-Host ""

# 5. 設定ファイルの生成
Write-Host "5. 設定ファイルの生成" -ForegroundColor Cyan

$configData = @{
    production = @{
        userPoolId = $UserPoolId
        clientId = $ClientId
        apiUrl = $ProductionApiUrl
        region = "ap-northeast-1"
        testUser = @{
            email = $TestUserEmail
            # パスワードは保存しない（セキュリティ上の理由）
        }
    }
    lastUpdated = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
}

$configJson = $configData | ConvertTo-Json -Depth 3
$configJson | Out-File -FilePath "production-auth-config.json" -Encoding UTF8

Write-Host "✅ 設定ファイル生成: production-auth-config.json" -ForegroundColor Green

Write-Host ""
Write-Host "=== セットアップ完了 ===" -ForegroundColor Green
Write-Host ""
Write-Host "次のステップ:" -ForegroundColor Cyan
Write-Host "1. 本番環境認証テストの実行:" -ForegroundColor Gray
Write-Host "   .\test-auth-production.ps1 -Environment prod -TestUserEmail $TestUserEmail -TestUserPassword [パスワード]" -ForegroundColor Gray
Write-Host ""
Write-Host "2. 設定の確認:" -ForegroundColor Gray
Write-Host "   Get-Content production-auth-config.json | ConvertFrom-Json" -ForegroundColor Gray
Write-Host ""
Write-Host "使用方法例:" -ForegroundColor Yellow
Write-Host ".\setup-production-auth-test.ps1 -UserPoolId 'ap-northeast-1_XXXXXXXXX' -ClientId 'XXXXXXXXXXXXXXXXXXXXXXXXXX' -ProductionApiUrl 'https://api.example.com/prod' -CreateTestUser -TestUserEmail 'test@example.com' -TestUserPassword 'TempPassword123!'" -ForegroundColor Gray