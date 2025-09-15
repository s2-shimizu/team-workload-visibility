# 本番環境統合デプロイスクリプト
param(
    [Parameter(Mandatory=$true)]
    [string]$DomainName,
    [string]$Environment = "prod",
    [string]$Region = "ap-northeast-1",
    [string]$AppName = "team-dashboard",
    [switch]$SkipDomainSetup = $false,
    [switch]$SkipSecuritySetup = $false,
    [switch]$CreateHostedZone = $false
)

Write-Host "=== 本番環境統合デプロイ ===" -ForegroundColor Green
Write-Host "Domain: $DomainName" -ForegroundColor Yellow
Write-Host "Environment: $Environment" -ForegroundColor Yellow
Write-Host "Region: $Region" -ForegroundColor Yellow
Write-Host "App Name: $AppName" -ForegroundColor Yellow
Write-Host ""

$startTime = Get-Date

# エラーハンドリング
$ErrorActionPreference = "Stop"

try {
    # 1. 前提条件チェック
    Write-Host "=== Step 1: 前提条件チェック ===" -ForegroundColor Cyan
    
    # AWS CLI確認
    try {
        $awsVersion = aws --version
        Write-Host "✅ AWS CLI: $awsVersion" -ForegroundColor Green
    } catch {
        throw "AWS CLIが見つかりません。AWS CLIをインストールしてください。"
    }
    
    # Docker確認
    try {
        $dockerVersion = docker --version
        Write-Host "✅ Docker: $dockerVersion" -ForegroundColor Green
    } catch {
        throw "Dockerが見つかりません。Docker Desktopをインストールしてください。"
    }
    
    # AWS認証確認
    try {
        $identity = aws sts get-caller-identity --output json | ConvertFrom-Json
        Write-Host "✅ AWS認証: $($identity.Arn)" -ForegroundColor Green
        Write-Host "   アカウント: $($identity.Account)" -ForegroundColor Gray
    } catch {
        throw "AWS認証が設定されていません。aws configure を実行してください。"
    }
    
    Write-Host ""
    
    # 2. 本番用DynamoDBテーブル作成
    Write-Host "=== Step 2: 本番用DynamoDBテーブル作成 ===" -ForegroundColor Cyan
    
    Write-Host "DynamoDBテーブルを作成中..." -ForegroundColor Gray
    .\create-production-tables.ps1 -Environment $Environment -Region $Region
    
    if ($LASTEXITCODE -ne 0) {
        throw "DynamoDBテーブル作成に失敗しました"
    }
    
    Write-Host "✅ DynamoDBテーブル作成完了" -ForegroundColor Green
    Write-Host ""
    
    # 3. ECS Fargateデプロイ
    Write-Host "=== Step 3: ECS Fargateデプロイ ===" -ForegroundColor Cyan
    
    Write-Host "ECS Fargateインフラストラクチャをデプロイ中..." -ForegroundColor Gray
    .\deploy-ecs-fargate.ps1 -Environment $Environment -AppName $AppName
    
    if ($LASTEXITCODE -ne 0) {
        throw "ECS Fargateデプロイに失敗しました"
    }
    
    Write-Host "✅ ECS Fargateデプロイ完了" -ForegroundColor Green
    Write-Host ""
    
    # 4. ドメイン・SSL設定
    if (-not $SkipDomainSetup) {
        Write-Host "=== Step 4: ドメイン・SSL設定 ===" -ForegroundColor Cyan
        
        Write-Host "ドメインとSSL証明書を設定中..." -ForegroundColor Gray
        if ($CreateHostedZone) {
            .\setup-production-domain.ps1 -DomainName $DomainName -Environment $Environment -Region $Region -CreateHostedZone
        } else {
            .\setup-production-domain.ps1 -DomainName $DomainName -Environment $Environment -Region $Region
        }
        
        if ($LASTEXITCODE -ne 0) {
            Write-Host "⚠️ ドメイン設定でエラーが発生しましたが、続行します" -ForegroundColor Yellow
        } else {
            Write-Host "✅ ドメイン・SSL設定完了" -ForegroundColor Green
        }
        Write-Host ""
    }
    
    # 5. セキュリティ設定
    if (-not $SkipSecuritySetup) {
        Write-Host "=== Step 5: セキュリティ設定 ===" -ForegroundColor Cyan
        
        Write-Host "本番用セキュリティ設定を適用中..." -ForegroundColor Gray
        .\configure-production-security.ps1 -Environment $Environment -Region $Region -AppName $AppName
        
        if ($LASTEXITCODE -ne 0) {
            Write-Host "⚠️ セキュリティ設定でエラーが発生しましたが、続行します" -ForegroundColor Yellow
        } else {
            Write-Host "✅ セキュリティ設定完了" -ForegroundColor Green
        }
        Write-Host ""
    }
    
    # 6. デプロイ結果確認
    Write-Host "=== Step 6: デプロイ結果確認 ===" -ForegroundColor Cyan
    
    # ECSスタック情報取得
    $stackName = "$AppName-$Environment-ecs"
    try {
        $stackInfo = aws cloudformation describe-stacks --stack-name $stackName --region $Region --output json | ConvertFrom-Json
        
        if ($stackInfo.Stacks.Count -gt 0) {
            $stack = $stackInfo.Stacks[0]
            Write-Host "✅ ECSスタックステータス: $($stack.StackStatus)" -ForegroundColor Green
            
            if ($stack.Outputs) {
                foreach ($output in $stack.Outputs) {
                    if ($output.OutputKey -eq "LoadBalancerURL") {
                        $script:LoadBalancerURL = $output.OutputValue
                        Write-Host "   ロードバランサーURL: $($script:LoadBalancerURL)" -ForegroundColor Gray
                    }
                }
            }
        }
    } catch {
        Write-Host "⚠️ ECSスタック情報取得エラー: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    
    # DynamoDBテーブル確認
    try {
        $workloadTable = aws dynamodb describe-table --table-name "WorkloadStatus-$Environment" --region $Region --output json | ConvertFrom-Json
        $issueTable = aws dynamodb describe-table --table-name "TeamIssue-$Environment" --region $Region --output json | ConvertFrom-Json
        
        Write-Host "✅ DynamoDBテーブル確認完了" -ForegroundColor Green
        Write-Host "   WorkloadStatusテーブル: $($workloadTable.Table.TableStatus)" -ForegroundColor Gray
        Write-Host "   TeamIssueテーブル: $($issueTable.Table.TableStatus)" -ForegroundColor Gray
    } catch {
        Write-Host "⚠️ DynamoDBテーブル確認エラー: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    
    Write-Host ""
    
    # 7. 統合テスト実行
    Write-Host "=== Step 7: 統合テスト実行 ===" -ForegroundColor Cyan
    
    if ($script:LoadBalancerURL) {
        Write-Host "統合テストを実行中..." -ForegroundColor Gray
        
        # ヘルスチェック
        try {
            Start-Sleep -Seconds 30 # アプリケーション起動を待機
            $healthCheck = Invoke-RestMethod -Uri "$($script:LoadBalancerURL)/api/status" -TimeoutSec 30
            Write-Host "✅ ヘルスチェック成功: $($healthCheck.status)" -ForegroundColor Green
        } catch {
            Write-Host "⚠️ ヘルスチェック失敗: $($_.Exception.Message)" -ForegroundColor Yellow
            Write-Host "   アプリケーションの起動に時間がかかっている可能性があります" -ForegroundColor Gray
        }
        
        # DynamoDB統合テスト
        try {
            Write-Host "DynamoDB統合テストを実行中..." -ForegroundColor Gray
            .\simple-dynamodb-test.ps1 -BaseUrl $script:LoadBalancerURL
            Write-Host "✅ DynamoDB統合テスト完了" -ForegroundColor Green
        } catch {
            Write-Host "⚠️ DynamoDB統合テストでエラーが発生しました" -ForegroundColor Yellow
        }
    }
    
    Write-Host ""
    
    # 8. デプロイ完了
    $endTime = Get-Date
    $duration = $endTime - $startTime
    
    Write-Host "=== 本番環境デプロイ完了 ===" -ForegroundColor Green
    Write-Host "デプロイ時間: $($duration.Minutes)分$($duration.Seconds)秒" -ForegroundColor Gray
    Write-Host ""
    
    # デプロイサマリー
    Write-Host "🎉 デプロイ成功！" -ForegroundColor Green
    Write-Host ""
    Write-Host "デプロイされたリソース:" -ForegroundColor Cyan
    Write-Host "  Environment: $Environment" -ForegroundColor Yellow
    Write-Host "  Region: $Region" -ForegroundColor Yellow
    Write-Host "  DynamoDBテーブル: WorkloadStatus-$Environment, TeamIssue-$Environment" -ForegroundColor Yellow
    if ($script:LoadBalancerURL) {
        Write-Host "  アプリケーションURL: $($script:LoadBalancerURL)" -ForegroundColor Yellow
    }
    if (-not $SkipDomainSetup) {
        Write-Host "  カスタムドメイン: https://$DomainName" -ForegroundColor Yellow
    }
    
    Write-Host ""
    Write-Host "次のステップ:" -ForegroundColor Cyan
    Write-Host "1. SSL証明書の検証完了を待つ（数分〜数時間）" -ForegroundColor Gray
    Write-Host "2. CloudFront Distributionのデプロイ完了を待つ（15-20分）" -ForegroundColor Gray
    Write-Host "3. DNS伝播を待つ（最大48時間）" -ForegroundColor Gray
    Write-Host "4. フロントエンド設定を更新:" -ForegroundColor Gray
    if (-not $SkipDomainSetup) {
        Write-Host "   frontend/js/aws-config.js のendpointを 'https://$DomainName' に更新" -ForegroundColor Gray
    } else {
        Write-Host "   frontend/js/aws-config.js のendpointを '$($script:LoadBalancerURL)' に更新" -ForegroundColor Gray
    }
    
    Write-Host ""
    Write-Host "監視・運用:" -ForegroundColor Cyan
    Write-Host "  CloudWatch: https://console.aws.amazon.com/cloudwatch/" -ForegroundColor Gray
    Write-Host "  ECS Console: https://console.aws.amazon.com/ecs/" -ForegroundColor Gray
    Write-Host "  DynamoDB Console: https://console.aws.amazon.com/dynamodb/" -ForegroundColor Gray
    
    Write-Host ""
    Write-Host "テストコマンド:" -ForegroundColor Cyan
    if (-not $SkipDomainSetup) {
        Write-Host "  curl https://$DomainName/api/status" -ForegroundColor Gray
        Write-Host "  .\test-realtime-updates.ps1 -BaseUrl 'https://$DomainName'" -ForegroundColor Gray
    } else {
        Write-Host "  curl $($script:LoadBalancerURL)/api/status" -ForegroundColor Gray
        Write-Host "  .\test-realtime-updates.ps1 -BaseUrl '$($script:LoadBalancerURL)'" -ForegroundColor Gray
    }
    
} catch {
    Write-Host ""
    Write-Host "❌ デプロイエラー: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "トラブルシューティング:" -ForegroundColor Yellow
    Write-Host "1. AWS認証情報を確認" -ForegroundColor Gray
    Write-Host "2. 必要なIAM権限があることを確認" -ForegroundColor Gray
    Write-Host "3. リージョンが正しいことを確認" -ForegroundColor Gray
    Write-Host "4. 既存リソースとの競合がないか確認" -ForegroundColor Gray
    Write-Host ""
    Write-Host "ログ確認:" -ForegroundColor Yellow
    Write-Host "  CloudFormation: https://console.aws.amazon.com/cloudformation/" -ForegroundColor Gray
    Write-Host "  CloudWatch Logs: https://console.aws.amazon.com/cloudwatch/home#logsV2:" -ForegroundColor Gray
    
    exit 1
}

Write-Host ""
Write-Host "使用方法:" -ForegroundColor Yellow
Write-Host "  基本デプロイ: .\deploy-production.ps1 -DomainName 'yourdomain.com'" -ForegroundColor Gray
Write-Host "  新規ドメイン: .\deploy-production.ps1 -DomainName 'yourdomain.com' -CreateHostedZone" -ForegroundColor Gray
Write-Host "  ドメインなし: .\deploy-production.ps1 -DomainName 'dummy.com' -SkipDomainSetup" -ForegroundColor Gray
Write-Host "  セキュリティなし: .\deploy-production.ps1 -DomainName 'yourdomain.com' -SkipSecuritySetup" -ForegroundColor Gray