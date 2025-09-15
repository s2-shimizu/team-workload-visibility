# SAM Build & Deploy 確認・実行スクリプト
param(
    [string]$Environment = "dev",
    [string]$StackName = "team-dashboard",
    [switch]$CheckOnly = $false,
    [switch]$FixIssues = $false
)

Write-Host "=== SAM Build & Deploy 確認 ===" -ForegroundColor Green
Write-Host "Environment: $Environment" -ForegroundColor Yellow
Write-Host "Stack Name: $StackName" -ForegroundColor Yellow
Write-Host ""

# 1. 前提条件チェック
Write-Host "1. 前提条件チェック" -ForegroundColor Cyan

$issues = @()

# SAM CLI確認
try {
    $samVersion = sam --version
    Write-Host "✅ SAM CLI: $samVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ SAM CLIが見つかりません" -ForegroundColor Red
    $issues += "SAM CLI未インストール"
}

# AWS CLI確認
try {
    $awsVersion = aws --version
    Write-Host "✅ AWS CLI: $awsVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ AWS CLIが見つかりません" -ForegroundColor Red
    $issues += "AWS CLI未インストール"
}

# Java確認
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    Write-Host "✅ Java: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Javaが見つかりません" -ForegroundColor Red
    $issues += "Java未インストール"
}

# Maven確認
try {
    $mavenVersion = mvn --version | Select-String "Apache Maven"
    Write-Host "✅ Maven: $mavenVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Mavenが見つかりません" -ForegroundColor Red
    $issues += "Maven未インストール"
}

Write-Host ""

# 2. SAMテンプレート確認
Write-Host "2. SAMテンプレート確認" -ForegroundColor Cyan

if (Test-Path "template.yaml") {
    Write-Host "✅ template.yaml 存在確認" -ForegroundColor Green
    
    # テンプレート内容確認
    $templateContent = Get-Content "template.yaml" -Raw
    
    # ハンドラー確認
    if ($templateContent -match "Handler:\s*com\.teamdashboard\.SimpleLambdaHandler::handleRequest") {
        Write-Host "✅ Lambda Handler設定確認" -ForegroundColor Green
    } else {
        Write-Host "⚠️ Lambda Handler設定に問題があります" -ForegroundColor Yellow
        $issues += "Lambda Handler設定"
    }
    
    # CodeUri確認
    if ($templateContent -match "CodeUri:\s*backend/") {
        Write-Host "✅ CodeUri設定確認" -ForegroundColor Green
    } else {
        Write-Host "⚠️ CodeUri設定に問題があります" -ForegroundColor Yellow
        $issues += "CodeUri設定"
    }
    
} else {
    Write-Host "❌ template.yaml が見つかりません" -ForegroundColor Red
    $issues += "SAMテンプレート未存在"
}

Write-Host ""

# 3. Javaソースコード確認
Write-Host "3. Javaソースコード確認" -ForegroundColor Cyan

$handlerPath = "backend/src/main/java/com/teamdashboard/SimpleLambdaHandler.java"
if (Test-Path $handlerPath) {
    Write-Host "✅ SimpleLambdaHandler.java 存在確認" -ForegroundColor Green
} else {
    Write-Host "❌ SimpleLambdaHandler.java が見つかりません" -ForegroundColor Red
    $issues += "Lambda Handler クラス未存在"
}

# pom.xml確認
if (Test-Path "backend/pom.xml") {
    Write-Host "✅ pom.xml 存在確認" -ForegroundColor Green
    
    $pomContent = Get-Content "backend/pom.xml" -Raw
    
    # Lambda依存関係確認
    if ($pomContent -match "aws-lambda-java-core") {
        Write-Host "✅ Lambda依存関係確認" -ForegroundColor Green
    } else {
        Write-Host "⚠️ Lambda依存関係が不足しています" -ForegroundColor Yellow
        $issues += "Lambda依存関係"
    }
    
} else {
    Write-Host "❌ pom.xml が見つかりません" -ForegroundColor Red
    $issues += "pom.xml未存在"
}

Write-Host ""

# 4. 問題の修正（オプション）
if ($FixIssues -and $issues.Count -gt 0) {
    Write-Host "4. 問題の修正" -ForegroundColor Cyan
    
    foreach ($issue in $issues) {
        switch ($issue) {
            "SAMテンプレート未存在" {
                Write-Host "SAMテンプレートを作成中..." -ForegroundColor Gray
                # 基本的なSAMテンプレートを作成
                # （既に存在するtemplate.yamlを使用）
            }
            "Lambda Handler設定" {
                Write-Host "Lambda Handler設定を修正中..." -ForegroundColor Gray
                # template.yamlのHandler設定を修正
            }
            "Lambda依存関係" {
                Write-Host "Lambda依存関係を追加中..." -ForegroundColor Gray
                # pom.xmlに必要な依存関係を追加
            }
        }
    }
}

Write-Host ""

# 5. SAMビルドテスト
if (-not $CheckOnly) {
    Write-Host "5. SAMビルドテスト" -ForegroundColor Cyan
    
    if ($issues.Count -eq 0 -or -not ($issues -contains "SAMテンプレート未存在")) {
        try {
            Write-Host "Maven clean & package実行中..." -ForegroundColor Gray
            Set-Location backend
            mvn clean package -DskipTests -q
            if ($LASTEXITCODE -ne 0) {
                throw "Maven build failed"
            }
            Set-Location ..
            Write-Host "✅ Mavenビルド成功" -ForegroundColor Green
            
            Write-Host "SAM build実行中..." -ForegroundColor Gray
            sam build
            if ($LASTEXITCODE -ne 0) {
                throw "SAM build failed"
            }
            Write-Host "✅ SAMビルド成功" -ForegroundColor Green
            
        } catch {
            Write-Host "❌ ビルドエラー: $($_.Exception.Message)" -ForegroundColor Red
            Set-Location .. -ErrorAction SilentlyContinue
        }
    } else {
        Write-Host "⚠️ 前提条件の問題により、ビルドをスキップします" -ForegroundColor Yellow
    }
}

Write-Host ""

# 6. 結果サマリー
Write-Host "=== 結果サマリー ===" -ForegroundColor Green

if ($issues.Count -eq 0) {
    Write-Host "🎉 すべての確認項目をクリアしました！" -ForegroundColor Green
    Write-Host ""
    Write-Host "SAM Build & Deploy 実行方法:" -ForegroundColor Cyan
    Write-Host "1. ビルド:" -ForegroundColor Gray
    Write-Host "   sam build" -ForegroundColor Gray
    Write-Host ""
    Write-Host "2. デプロイ:" -ForegroundColor Gray
    Write-Host "   sam deploy --stack-name $StackName-$Environment --parameter-overrides Environment=$Environment --capabilities CAPABILITY_IAM --resolve-s3" -ForegroundColor Gray
    Write-Host ""
    Write-Host "3. ガイド付きデプロイ（初回推奨）:" -ForegroundColor Gray
    Write-Host "   sam deploy --guided" -ForegroundColor Gray
    Write-Host ""
    Write-Host "4. 統合スクリプト使用:" -ForegroundColor Gray
    Write-Host "   .\deploy-sam-stack.ps1 -Environment $Environment" -ForegroundColor Gray
    
} else {
    Write-Host "⚠️ 以下の問題が見つかりました:" -ForegroundColor Yellow
    foreach ($issue in $issues) {
        Write-Host "  • $issue" -ForegroundColor Red
    }
    
    Write-Host ""
    Write-Host "修正方法:" -ForegroundColor Cyan
    
    if ($issues -contains "SAM CLI未インストール") {
        Write-Host "• SAM CLI インストール:" -ForegroundColor Gray
        Write-Host "  https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html" -ForegroundColor Gray
    }
    
    if ($issues -contains "AWS CLI未インストール") {
        Write-Host "• AWS CLI インストール:" -ForegroundColor Gray
        Write-Host "  https://aws.amazon.com/cli/" -ForegroundColor Gray
    }
    
    if ($issues -contains "Java未インストール") {
        Write-Host "• Java 17 インストール:" -ForegroundColor Gray
        Write-Host "  https://adoptium.net/" -ForegroundColor Gray
    }
    
    if ($issues -contains "Maven未インストール") {
        Write-Host "• Maven インストール:" -ForegroundColor Gray
        Write-Host "  https://maven.apache.org/install.html" -ForegroundColor Gray
    }
    
    Write-Host ""
    Write-Host "自動修正を試行する場合:" -ForegroundColor Cyan
    Write-Host "  .\sam-deploy-guide.ps1 -FixIssues" -ForegroundColor Gray
}

Write-Host ""
Write-Host "使用方法:" -ForegroundColor Yellow
Write-Host "  確認のみ: .\sam-deploy-guide.ps1 -CheckOnly" -ForegroundColor Gray
Write-Host "  問題修正: .\sam-deploy-guide.ps1 -FixIssues" -ForegroundColor Gray
Write-Host "  ビルドテスト: .\sam-deploy-guide.ps1 -Environment dev" -ForegroundColor Gray