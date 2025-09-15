# SAMスタックデプロイスクリプト
param(
    [string]$Environment = "dev",
    [string]$StackName = "team-dashboard",
    [switch]$Build = $true,
    [switch]$Deploy = $true,
    [switch]$Guided = $false
)

Write-Host "=== SAMスタックデプロイ ===" -ForegroundColor Green
Write-Host "Environment: $Environment" -ForegroundColor Yellow
Write-Host "Stack Name: $StackName" -ForegroundColor Yellow
Write-Host ""

# 前提条件チェック
Write-Host "1. 前提条件チェック" -ForegroundColor Cyan

# SAM CLIの確認
try {
    $samVersion = sam --version
    Write-Host "✅ SAM CLI: $samVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ SAM CLIが見つかりません。SAM CLIをインストールしてください。" -ForegroundColor Red
    Write-Host "   インストール: https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html" -ForegroundColor Gray
    exit 1
}

# AWS CLIの確認
try {
    $awsVersion = aws --version
    Write-Host "✅ AWS CLI: $awsVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ AWS CLIが見つかりません。" -ForegroundColor Red
    exit 1
}

# AWS認証の確認
try {
    $identity = aws sts get-caller-identity --output json | ConvertFrom-Json
    Write-Host "✅ AWS認証: $($identity.Arn)" -ForegroundColor Green
    Write-Host "   アカウント: $($identity.Account)" -ForegroundColor Gray
} catch {
    Write-Host "❌ AWS認証が設定されていません。aws configure を実行してください。" -ForegroundColor Red
    exit 1
}

Write-Host ""

# 2. Mavenビルド
if ($Build) {
    Write-Host "2. Mavenビルド" -ForegroundColor Cyan
    
    try {
        Set-Location backend
        
        Write-Host "Maven clean..." -ForegroundColor Gray
        mvn clean -q
        if ($LASTEXITCODE -ne 0) {
            throw "Maven clean failed"
        }
        
        Write-Host "Maven package (Lambda profile)..." -ForegroundColor Gray
        mvn package -Plambda -DskipTests -q
        if ($LASTEXITCODE -ne 0) {
            throw "Maven package failed"
        }
        
        # JARファイルの確認
        $jarFiles = Get-ChildItem -Path "target" -Name "*.jar" | Where-Object { $_ -like "*lambda*" }
        if ($jarFiles.Count -gt 0) {
            Write-Host "✅ Lambda JAR生成: $($jarFiles[0])" -ForegroundColor Green
            $jarSize = (Get-Item "target/$($jarFiles[0])").Length / 1MB
            Write-Host "   サイズ: $([math]::Round($jarSize, 2)) MB" -ForegroundColor Gray
        } else {
            throw "Lambda JARが見つかりません"
        }
        
    } catch {
        Write-Host "❌ Mavenビルドエラー: $($_.Exception.Message)" -ForegroundColor Red
        Set-Location ..
        exit 1
    } finally {
        Set-Location ..
    }
    
    Write-Host "✅ Mavenビルド完了" -ForegroundColor Green
    Write-Host ""
}

# 3. SAMビルド
Write-Host "3. SAMビルド" -ForegroundColor Cyan
try {
    Write-Host "sam build実行中..." -ForegroundColor Gray
    sam build --use-container
    if ($LASTEXITCODE -ne 0) {
        throw "SAM build failed"
    }
    Write-Host "✅ SAMビルド完了" -ForegroundColor Green
} catch {
    Write-Host "❌ SAMビルドエラー: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""

# 4. SAMデプロイ
if ($Deploy) {
    Write-Host "4. SAMデプロイ" -ForegroundColor Cyan
    
    try {
        if ($Guided) {
            Write-Host "ガイド付きデプロイを実行中..." -ForegroundColor Gray
            sam deploy --guided
        } else {
            Write-Host "デプロイ実行中..." -ForegroundColor Gray
            sam deploy --stack-name "$StackName-$Environment" --parameter-overrides Environment=$Environment --capabilities CAPABILITY_IAM --resolve-s3
        }
        
        if ($LASTEXITCODE -ne 0) {
            throw "SAM deploy failed"
        }
        
        Write-Host "✅ SAMデプロイ完了" -ForegroundColor Green
    } catch {
        Write-Host "❌ SAMデプロイエラー: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
    
    Write-Host ""
    
    # 5. デプロイ結果の確認
    Write-Host "5. デプロイ結果確認" -ForegroundColor Cyan
    
    try {
        Write-Host "スタック情報を取得中..." -ForegroundColor Gray
        $stackInfo = aws cloudformation describe-stacks --stack-name "$StackName-$Environment" --output json | ConvertFrom-Json
        
        if ($stackInfo.Stacks.Count -gt 0) {
            $stack = $stackInfo.Stacks[0]
            Write-Host "✅ スタックステータス: $($stack.StackStatus)" -ForegroundColor Green
            
            # Outputsの表示
            if ($stack.Outputs) {
                Write-Host ""
                Write-Host "スタックOutputs:" -ForegroundColor Yellow
                foreach ($output in $stack.Outputs) {
                    Write-Host "  $($output.OutputKey): $($output.OutputValue)" -ForegroundColor Gray
                    
                    # 重要な値を変数に保存
                    if ($output.OutputKey -eq "ApiGatewayEndpoint") {
                        $script:ApiEndpoint = $output.OutputValue
                    } elseif ($output.OutputKey -eq "WorkloadStatusTableName") {
                        $script:WorkloadTableName = $output.OutputValue
                    } elseif ($output.OutputKey -eq "TeamIssueTableName") {
                        $script:IssueTableName = $output.OutputValue
                    }
                }
            }
        }
    } catch {
        Write-Host "⚠️ スタック情報取得エラー: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "=== デプロイ完了 ===" -ForegroundColor Green

if ($Deploy -and $script:ApiEndpoint) {
    Write-Host ""
    Write-Host "次のステップ:" -ForegroundColor Cyan
    Write-Host "1. 統合テストの実行:" -ForegroundColor Gray
    Write-Host "   .\test-deployed-stack.ps1 -ApiEndpoint '$($script:ApiEndpoint)' -WorkloadTable '$($script:WorkloadTableName)' -IssueTable '$($script:IssueTableName)'" -ForegroundColor Gray
    Write-Host ""
    Write-Host "2. フロントエンドの設定更新:" -ForegroundColor Gray
    Write-Host "   frontend/js/aws-config.js のendpointを '$($script:ApiEndpoint)' に更新" -ForegroundColor Gray
    Write-Host ""
    Write-Host "3. 本番テストの実行:" -ForegroundColor Gray
    Write-Host "   .\simple-dynamodb-test.ps1 -BaseUrl '$($script:ApiEndpoint)'" -ForegroundColor Gray
}

Write-Host ""
Write-Host "使用方法:" -ForegroundColor Yellow
Write-Host "  基本デプロイ: .\deploy-sam-stack.ps1" -ForegroundColor Gray
Write-Host "  ガイド付き: .\deploy-sam-stack.ps1 -Guided" -ForegroundColor Gray
Write-Host "  本番環境: .\deploy-sam-stack.ps1 -Environment prod" -ForegroundColor Gray
Write-Host "  ビルドのみ: .\deploy-sam-stack.ps1 -Deploy:$false" -ForegroundColor Gray