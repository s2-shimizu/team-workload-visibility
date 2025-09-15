# クイックデプロイスクリプト - リアルタイム機能対応
param(
    [string]$Environment = "dev",
    [ValidateSet("ecs", "ec2", "lambda")]
    [string]$DeployType = "ecs",
    [switch]$Help
)

if ($Help) {
    Write-Host "=== Team Dashboard クイックデプロイ ===" -ForegroundColor Green
    Write-Host ""
    Write-Host "使用方法:" -ForegroundColor Cyan
    Write-Host "  .\quick-deploy.ps1 -DeployType ecs -Environment dev" -ForegroundColor Gray
    Write-Host ""
    Write-Host "デプロイタイプ:" -ForegroundColor Yellow
    Write-Host "  ecs    - ECS Fargate (推奨) - WebSocket完全対応" -ForegroundColor Gray
    Write-Host "  ec2    - EC2インスタンス - WebSocket対応、シンプル" -ForegroundColor Gray
    Write-Host "  lambda - AWS Lambda - WebSocket制限あり" -ForegroundColor Gray
    Write-Host ""
    Write-Host "環境:" -ForegroundColor Yellow
    Write-Host "  dev  - 開発環境" -ForegroundColor Gray
    Write-Host "  prod - 本番環境" -ForegroundColor Gray
    Write-Host ""
    Write-Host "例:" -ForegroundColor Cyan
    Write-Host "  .\quick-deploy.ps1 -DeployType ecs" -ForegroundColor Gray
    Write-Host "  .\quick-deploy.ps1 -DeployType ec2 -Environment prod" -ForegroundColor Gray
    Write-Host "  .\quick-deploy.ps1 -DeployType lambda" -ForegroundColor Gray
    exit 0
}

Write-Host "=== Team Dashboard クイックデプロイ ===" -ForegroundColor Green
Write-Host "デプロイタイプ: $DeployType" -ForegroundColor Yellow
Write-Host "環境: $Environment" -ForegroundColor Yellow
Write-Host ""

# デプロイタイプ別の説明
switch ($DeployType) {
    "ecs" {
        Write-Host "🚀 ECS Fargate デプロイ" -ForegroundColor Cyan
        Write-Host "✅ WebSocket完全対応" -ForegroundColor Green
        Write-Host "✅ 自動スケーリング" -ForegroundColor Green
        Write-Host "✅ 高可用性" -ForegroundColor Green
        Write-Host "⚠️ 初回デプロイに時間がかかります（5-10分）" -ForegroundColor Yellow
    }
    "ec2" {
        Write-Host "🖥️ EC2 デプロイ" -ForegroundColor Cyan
        Write-Host "✅ WebSocket完全対応" -ForegroundColor Green
        Write-Host "✅ シンプルな構成" -ForegroundColor Green
        Write-Host "⚠️ キーペアが必要です" -ForegroundColor Yellow
    }
    "lambda" {
        Write-Host "⚡ Lambda デプロイ" -ForegroundColor Cyan
        Write-Host "✅ サーバーレス" -ForegroundColor Green
        Write-Host "✅ 低コスト" -ForegroundColor Green
        Write-Host "❌ WebSocket機能は制限されます" -ForegroundColor Red
    }
}

Write-Host ""

# 確認プロンプト
$confirm = Read-Host "このデプロイタイプで続行しますか？ (y/N)"
if ($confirm -ne "y" -and $confirm -ne "Y") {
    Write-Host "デプロイをキャンセルしました。" -ForegroundColor Yellow
    exit 0
}

Write-Host ""

# デプロイタイプ別実行
switch ($DeployType) {
    "ecs" {
        Write-Host "ECS Fargateデプロイを開始します..." -ForegroundColor Cyan
        
        # 前提条件チェック
        Write-Host "前提条件をチェック中..." -ForegroundColor Gray
        
        # Docker確認
        try {
            docker --version | Out-Null
            Write-Host "✅ Docker確認完了" -ForegroundColor Green
        } catch {
            Write-Host "❌ Dockerが見つかりません。Docker Desktopをインストールしてください。" -ForegroundColor Red
            Write-Host "   ダウンロード: https://www.docker.com/products/docker-desktop" -ForegroundColor Gray
            exit 1
        }
        
        # ECS Fargateデプロイ実行
        Write-Host ""
        Write-Host "ECS Fargateデプロイスクリプトを実行中..." -ForegroundColor Cyan
        .\deploy-ecs-fargate.ps1 -Environment $Environment
    }
    
    "ec2" {
        Write-Host "EC2デプロイを開始します..." -ForegroundColor Cyan
        
        # キーペア確認
        Write-Host "キーペアを確認中..." -ForegroundColor Gray
        try {
            $keyPairs = aws ec2 describe-key-pairs --output json | ConvertFrom-Json
            if ($keyPairs.KeyPairs.Count -eq 0) {
                Write-Host "❌ キーペアが見つかりません。" -ForegroundColor Red
                Write-Host "   AWS EC2コンソールでキーペアを作成してください。" -ForegroundColor Gray
                exit 1
            }
            
            Write-Host "利用可能なキーペア:" -ForegroundColor Yellow
            for ($i = 0; $i -lt $keyPairs.KeyPairs.Count; $i++) {
                Write-Host "  $($i + 1). $($keyPairs.KeyPairs[$i].KeyName)" -ForegroundColor Gray
            }
            
            $keyIndex = Read-Host "使用するキーペア番号を選択してください (1-$($keyPairs.KeyPairs.Count))"
            $selectedKey = $keyPairs.KeyPairs[$keyIndex - 1].KeyName
            
            Write-Host "選択されたキーペア: $selectedKey" -ForegroundColor Green
        } catch {
            Write-Host "❌ キーペア確認エラー: $($_.Exception.Message)" -ForegroundColor Red
            exit 1
        }
        
        # 新規インスタンス作成確認
        $createNew = Read-Host "新しいEC2インスタンスを作成しますか？ (y/N)"
        $createInstance = ($createNew -eq "y" -or $createNew -eq "Y")
        
        # EC2デプロイ実行
        Write-Host ""
        Write-Host "EC2デプロイスクリプトを実行中..." -ForegroundColor Cyan
        if ($createInstance) {
            .\deploy-ec2.ps1 -Environment $Environment -KeyName $selectedKey -CreateInstance
        } else {
            .\deploy-ec2.ps1 -Environment $Environment -KeyName $selectedKey
        }
    }
    
    "lambda" {
        Write-Host "Lambdaデプロイを開始します..." -ForegroundColor Cyan
        Write-Host "⚠️ 注意: WebSocket機能は動作しません。" -ForegroundColor Yellow
        Write-Host "✅ ポーリング更新（30秒間隔）で動作します。" -ForegroundColor Green
        
        $confirm = Read-Host "WebSocket機能なしで続行しますか？ (y/N)"
        if ($confirm -ne "y" -and $confirm -ne "Y") {
            Write-Host "デプロイをキャンセルしました。" -ForegroundColor Yellow
            Write-Host "WebSocket機能が必要な場合は、ECSまたはEC2デプロイを選択してください。" -ForegroundColor Gray
            exit 0
        }
        
        # 前提条件チェック
        Write-Host "前提条件をチェック中..." -ForegroundColor Gray
        
        # Java確認
        try {
            java -version 2>&1 | Out-Null
            Write-Host "✅ Java確認完了" -ForegroundColor Green
        } catch {
            Write-Host "❌ Javaが見つかりません。Java 17をインストールしてください。" -ForegroundColor Red
            Write-Host "   ダウンロード: https://adoptium.net/" -ForegroundColor Gray
            exit 1
        }
        
        # SAM CLI確認
        try {
            sam --version | Out-Null
            Write-Host "✅ SAM CLI確認完了" -ForegroundColor Green
        } catch {
            Write-Host "❌ SAM CLIが見つかりません。SAM CLIをインストールしてください。" -ForegroundColor Red
            Write-Host "   インストール: https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html" -ForegroundColor Gray
            exit 1
        }
        
        # Lambdaデプロイ実行
        Write-Host ""
        Write-Host "Lambda専用デプロイスクリプトを実行中..." -ForegroundColor Cyan
        .\deploy-lambda.ps1 -Environment $Environment
    }
}

Write-Host ""
Write-Host "=== クイックデプロイ完了 ===" -ForegroundColor Green

# デプロイ後の推奨アクション
Write-Host ""
Write-Host "推奨される次のステップ:" -ForegroundColor Cyan

switch ($DeployType) {
    "ecs" {
        Write-Host "1. アプリケーションの起動完了を待つ（2-3分）" -ForegroundColor Gray
        Write-Host "2. ロードバランサーURLでヘルスチェック実行" -ForegroundColor Gray
        Write-Host "3. リアルタイム機能テストを実行" -ForegroundColor Gray
        Write-Host "4. フロントエンド設定を更新" -ForegroundColor Gray
    }
    "ec2" {
        Write-Host "1. アプリケーションの起動完了を待つ（1-2分）" -ForegroundColor Gray
        Write-Host "2. パブリックIPでヘルスチェック実行" -ForegroundColor Gray
        Write-Host "3. リアルタイム機能テストを実行" -ForegroundColor Gray
        Write-Host "4. フロントエンド設定を更新" -ForegroundColor Gray
    }
    "lambda" {
        Write-Host "1. API Gatewayエンドポイントでヘルスチェック実行" -ForegroundColor Gray
        Write-Host "2. 基本API機能テストを実行" -ForegroundColor Gray
        Write-Host "3. フロントエンド設定を更新" -ForegroundColor Gray
        Write-Host "⚠️ WebSocket機能は利用できません" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "テストコマンド例:" -ForegroundColor Yellow
Write-Host "  ヘルスチェック: curl <your-endpoint>/api/status" -ForegroundColor Gray
if ($DeployType -ne "lambda") {
    Write-Host "  リアルタイムテスト: .\test-realtime-updates.ps1 -BaseUrl '<your-endpoint>'" -ForegroundColor Gray
}
Write-Host "  API機能テスト: .\simple-dynamodb-test.ps1 -BaseUrl '<your-endpoint>'" -ForegroundColor Gray

Write-Host ""
Write-Host "🎉 デプロイが完了しました！" -ForegroundColor Green