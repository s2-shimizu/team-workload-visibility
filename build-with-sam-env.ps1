# SAM環境変数対応ビルドスクリプト
param(
    [string]$Profile = "lambda",
    [string]$WorkloadTableName = "",
    [string]$IssueTableName = ""
)

Write-Host "=== SAM環境変数対応ビルド ===" -ForegroundColor Green
Write-Host "Profile: $Profile" -ForegroundColor Yellow
Write-Host ""

# 環境変数の設定
if ($WorkloadTableName) {
    $env:WORKLOAD_STATUS_TABLE = $WorkloadTableName
    Write-Host "環境変数設定: WORKLOAD_STATUS_TABLE = $WorkloadTableName" -ForegroundColor Gray
}

if ($IssueTableName) {
    $env:TEAM_ISSUE_TABLE = $IssueTableName
    Write-Host "環境変数設定: TEAM_ISSUE_TABLE = $IssueTableName" -ForegroundColor Gray
}

Write-Host ""

# Mavenビルドの実行
Write-Host "Mavenビルド実行中..." -ForegroundColor Cyan
try {
    Set-Location backend
    
    # クリーンビルド
    Write-Host "1. クリーンビルド" -ForegroundColor Yellow
    mvn clean -q
    if ($LASTEXITCODE -ne 0) {
        throw "Maven clean failed"
    }
    Write-Host "✅ クリーン完了" -ForegroundColor Green
    
    # コンパイル
    Write-Host "2. コンパイル" -ForegroundColor Yellow
    mvn compile -q
    if ($LASTEXITCODE -ne 0) {
        throw "Maven compile failed"
    }
    Write-Host "✅ コンパイル完了" -ForegroundColor Green
    
    # パッケージング（Lambda用）
    Write-Host "3. パッケージング（Lambda用）" -ForegroundColor Yellow
    mvn package -P$Profile -DskipTests -q
    if ($LASTEXITCODE -ne 0) {
        throw "Maven package failed"
    }
    Write-Host "✅ パッケージング完了" -ForegroundColor Green
    
    # 生成されたJARファイルの確認
    $jarFiles = Get-ChildItem -Path "target" -Name "*.jar" | Where-Object { $_ -like "*lambda*" }
    if ($jarFiles.Count -gt 0) {
        Write-Host "✅ Lambda JAR生成: $($jarFiles[0])" -ForegroundColor Green
        $jarPath = "target/$($jarFiles[0])"
        $jarSize = (Get-Item $jarPath).Length / 1MB
        Write-Host "   ファイルサイズ: $([math]::Round($jarSize, 2)) MB" -ForegroundColor Gray
    } else {
        Write-Host "⚠️ Lambda JARが見つかりません" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "❌ ビルドエラー: $($_.Exception.Message)" -ForegroundColor Red
    Set-Location ..
    exit 1
} finally {
    Set-Location ..
}

Write-Host ""

# 設定ファイルの確認
Write-Host "設定ファイル確認" -ForegroundColor Cyan
$configFile = "backend/src/main/resources/application.yml"
if (Test-Path $configFile) {
    Write-Host "✅ application.yml存在確認" -ForegroundColor Green
    
    # DynamoDBテーブル設定の確認
    $configContent = Get-Content $configFile -Raw
    if ($configContent -match "WORKLOAD_STATUS_TABLE") {
        Write-Host "✅ WorkloadStatusテーブル設定確認" -ForegroundColor Green
    } else {
        Write-Host "⚠️ WorkloadStatusテーブル設定が見つかりません" -ForegroundColor Yellow
    }
    
    if ($configContent -match "TEAM_ISSUE_TABLE") {
        Write-Host "✅ TeamIssueテーブル設定確認" -ForegroundColor Green
    } else {
        Write-Host "⚠️ TeamIssueテーブル設定が見つかりません" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ application.ymlが見つかりません" -ForegroundColor Red
}

Write-Host ""

# SAMテンプレート例の生成
Write-Host "SAMテンプレート例" -ForegroundColor Cyan
Write-Host "以下の環境変数をSAMテンプレートに追加してください:" -ForegroundColor Yellow

$samTemplate = @"
Environment:
  Variables:
    SPRING_PROFILES_ACTIVE: lambda
    WORKLOAD_STATUS_TABLE: !Ref WorkloadStatusTable
    TEAM_ISSUE_TABLE: !Ref TeamIssueTable
    AWS_REGION: !Ref AWS::Region
"@

Write-Host $samTemplate -ForegroundColor Gray

Write-Host ""
Write-Host "=== ビルド完了 ===" -ForegroundColor Green
Write-Host ""
Write-Host "次のステップ:" -ForegroundColor Cyan
Write-Host "1. SAMテンプレートに上記の環境変数を追加" -ForegroundColor Gray
Write-Host "2. sam build でビルド" -ForegroundColor Gray
Write-Host "3. sam deploy でデプロイ" -ForegroundColor Gray
Write-Host "4. .\test-sam-dynamodb-integration.ps1 でテスト実行" -ForegroundColor Gray
Write-Host ""
Write-Host "使用方法:" -ForegroundColor Yellow
Write-Host "  基本ビルド: .\build-with-sam-env.ps1" -ForegroundColor Gray
Write-Host "  テーブル指定: .\build-with-sam-env.ps1 -WorkloadTableName 'MyWorkloadTable' -IssueTableName 'MyIssueTable'" -ForegroundColor Gray