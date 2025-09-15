# DynamoDBテーブル作成スクリプト
param(
    [string]$Region = "ap-northeast-1",
    [string]$Environment = "dev"
)

Write-Host "=== DynamoDB テーブル作成 ===" -ForegroundColor Green
Write-Host "リージョン: $Region" -ForegroundColor Yellow
Write-Host "環境: $Environment" -ForegroundColor Yellow
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

# 1. WorkloadStatusテーブルの作成
Write-Host "1. WorkloadStatusテーブルの作成" -ForegroundColor Cyan
$workloadTableName = "WorkloadStatus"
if ($Environment -ne "prod") {
    $workloadTableName = "$Environment-WorkloadStatus"
}

try {
    # テーブルが既に存在するかチェック
    $existingTable = aws dynamodb describe-table --table-name $workloadTableName --region $Region --output json 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "⚠️ テーブル '$workloadTableName' は既に存在します" -ForegroundColor Yellow
    } else {
        # テーブルを作成
        $createWorkloadTable = aws dynamodb create-table `
            --table-name $workloadTableName `
            --attribute-definitions AttributeName=userId,AttributeType=S `
            --key-schema AttributeName=userId,KeyType=HASH `
            --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 `
            --region $Region `
            --output json
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ テーブル '$workloadTableName' を作成しました" -ForegroundColor Green
            
            # テーブルがアクティブになるまで待機
            Write-Host "テーブルがアクティブになるまで待機中..." -ForegroundColor Gray
            aws dynamodb wait table-exists --table-name $workloadTableName --region $Region
            Write-Host "✅ テーブル '$workloadTableName' がアクティブになりました" -ForegroundColor Green
        } else {
            Write-Host "❌ テーブル '$workloadTableName' の作成に失敗しました" -ForegroundColor Red
            Write-Host "エラー: $createWorkloadTable" -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "❌ WorkloadStatusテーブル作成エラー: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 2. TeamIssueテーブルの作成
Write-Host "2. TeamIssueテーブルの作成" -ForegroundColor Cyan
$issueTableName = "TeamIssue"
if ($Environment -ne "prod") {
    $issueTableName = "$Environment-TeamIssue"
}

try {
    # テーブルが既に存在するかチェック
    $existingTable = aws dynamodb describe-table --table-name $issueTableName --region $Region --output json 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "⚠️ テーブル '$issueTableName' は既に存在します" -ForegroundColor Yellow
    } else {
        # テーブルを作成
        $createIssueTable = aws dynamodb create-table `
            --table-name $issueTableName `
            --attribute-definitions AttributeName=issueId,AttributeType=S `
            --key-schema AttributeName=issueId,KeyType=HASH `
            --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 `
            --region $Region `
            --output json
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ テーブル '$issueTableName' を作成しました" -ForegroundColor Green
            
            # テーブルがアクティブになるまで待機
            Write-Host "テーブルがアクティブになるまで待機中..." -ForegroundColor Gray
            aws dynamodb wait table-exists --table-name $issueTableName --region $Region
            Write-Host "✅ テーブル '$issueTableName' がアクティブになりました" -ForegroundColor Green
        } else {
            Write-Host "❌ テーブル '$issueTableName' の作成に失敗しました" -ForegroundColor Red
            Write-Host "エラー: $createIssueTable" -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "❌ TeamIssueテーブル作成エラー: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 3. サンプルデータの投入
Write-Host "3. サンプルデータの投入" -ForegroundColor Cyan

# WorkloadStatusサンプルデータ
Write-Host "WorkloadStatusサンプルデータを投入中..." -ForegroundColor Gray
$sampleWorkloadData = @(
    @{
        userId = "user1"
        displayName = "田中太郎"
        workloadLevel = "MEDIUM"
        projectCount = 3
        taskCount = 15
        comment = "現在のプロジェクトは順調に進んでいます"
        updatedAt = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
        createdAt = (Get-Date).AddDays(-7).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
    },
    @{
        userId = "user2"
        displayName = "佐藤花子"
        workloadLevel = "HIGH"
        projectCount = 5
        taskCount = 25
        comment = "複数のプロジェクトが重なって忙しい状況です"
        updatedAt = (Get-Date).AddHours(-2).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
        createdAt = (Get-Date).AddDays(-14).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
    },
    @{
        userId = "user3"
        displayName = "鈴木一郎"
        workloadLevel = "LOW"
        projectCount = 1
        taskCount = 5
        comment = "新しいプロジェクトの準備段階です"
        updatedAt = (Get-Date).AddDays(-1).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
        createdAt = (Get-Date).AddDays(-3).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
    }
)

foreach ($data in $sampleWorkloadData) {
    $item = @{
        userId = @{ S = $data.userId }
        displayName = @{ S = $data.displayName }
        workloadLevel = @{ S = $data.workloadLevel }
        projectCount = @{ N = $data.projectCount.ToString() }
        taskCount = @{ N = $data.taskCount.ToString() }
        comment = @{ S = $data.comment }
        updatedAt = @{ S = $data.updatedAt }
        createdAt = @{ S = $data.createdAt }
    }
    
    $itemJson = $item | ConvertTo-Json -Depth 3 -Compress
    
    try {
        aws dynamodb put-item --table-name $workloadTableName --item $itemJson --region $Region --output json | Out-Null
        Write-Host "  ✅ $($data.displayName) のデータを投入" -ForegroundColor Green
    } catch {
        Write-Host "  ❌ $($data.displayName) のデータ投入に失敗" -ForegroundColor Red
    }
}

# TeamIssueサンプルデータ
Write-Host "TeamIssueサンプルデータを投入中..." -ForegroundColor Gray
$sampleIssueData = @(
    @{
        issueId = "issue-1"
        userId = "user1"
        displayName = "田中太郎"
        content = "新しい技術の学習で詰まっています。React Hooksの使い方がよくわからず、コンポーネントの状態管理で困っています。"
        status = "OPEN"
        priority = "HIGH"
        createdAt = (Get-Date).AddDays(-1).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
        updatedAt = (Get-Date).AddDays(-1).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
    },
    @{
        issueId = "issue-2"
        userId = "user2"
        displayName = "佐藤花子"
        content = "プロジェクトの進め方で悩んでいます。タスクの優先順位をどう決めればよいかアドバイスをください。"
        status = "RESOLVED"
        priority = "MEDIUM"
        createdAt = (Get-Date).AddDays(-2).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
        updatedAt = (Get-Date).AddHours(-6).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
        resolvedAt = (Get-Date).AddHours(-6).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
    },
    @{
        issueId = "issue-3"
        userId = "user3"
        displayName = "鈴木一郎"
        content = "開発環境のセットアップで問題が発生しています。Dockerコンテナが正常に起動しません。"
        status = "OPEN"
        priority = "HIGH"
        createdAt = (Get-Date).AddHours(-4).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
        updatedAt = (Get-Date).AddHours(-4).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
    }
)

foreach ($data in $sampleIssueData) {
    $item = @{
        issueId = @{ S = $data.issueId }
        userId = @{ S = $data.userId }
        displayName = @{ S = $data.displayName }
        content = @{ S = $data.content }
        status = @{ S = $data.status }
        priority = @{ S = $data.priority }
        createdAt = @{ S = $data.createdAt }
        updatedAt = @{ S = $data.updatedAt }
    }
    
    if ($data.resolvedAt) {
        $item.resolvedAt = @{ S = $data.resolvedAt }
    }
    
    $itemJson = $item | ConvertTo-Json -Depth 3 -Compress
    
    try {
        aws dynamodb put-item --table-name $issueTableName --item $itemJson --region $Region --output json | Out-Null
        Write-Host "  ✅ $($data.issueId) のデータを投入" -ForegroundColor Green
    } catch {
        Write-Host "  ❌ $($data.issueId) のデータ投入に失敗" -ForegroundColor Red
    }
}

Write-Host ""

# 4. テーブル情報の確認
Write-Host "4. テーブル情報の確認" -ForegroundColor Cyan

try {
    $workloadTableInfo = aws dynamodb describe-table --table-name $workloadTableName --region $Region --output json | ConvertFrom-Json
    Write-Host "✅ $workloadTableName テーブル:" -ForegroundColor Green
    Write-Host "   ステータス: $($workloadTableInfo.Table.TableStatus)" -ForegroundColor Gray
    Write-Host "   アイテム数: $($workloadTableInfo.Table.ItemCount)" -ForegroundColor Gray
    Write-Host "   作成日時: $($workloadTableInfo.Table.CreationDateTime)" -ForegroundColor Gray
} catch {
    Write-Host "❌ $workloadTableName テーブル情報取得エラー" -ForegroundColor Red
}

try {
    $issueTableInfo = aws dynamodb describe-table --table-name $issueTableName --region $Region --output json | ConvertFrom-Json
    Write-Host "✅ $issueTableName テーブル:" -ForegroundColor Green
    Write-Host "   ステータス: $($issueTableInfo.Table.TableStatus)" -ForegroundColor Gray
    Write-Host "   アイテム数: $($issueTableInfo.Table.ItemCount)" -ForegroundColor Gray
    Write-Host "   作成日時: $($issueTableInfo.Table.CreationDateTime)" -ForegroundColor Gray
} catch {
    Write-Host "❌ $issueTableName テーブル情報取得エラー" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== DynamoDBテーブル作成完了 ===" -ForegroundColor Green
Write-Host ""
Write-Host "使用方法:" -ForegroundColor Cyan
Write-Host "  開発環境: .\create-dynamodb-tables.ps1 -Environment dev" -ForegroundColor Gray
Write-Host "  本番環境: .\create-dynamodb-tables.ps1 -Environment prod" -ForegroundColor Gray
Write-Host ""
Write-Host "次のステップ:" -ForegroundColor Yellow
Write-Host "1. アプリケーションの設定でテーブル名を確認" -ForegroundColor Gray
Write-Host "2. Spring Bootアプリケーションを起動してテスト" -ForegroundColor Gray
Write-Host "3. API エンドポイントでデータの読み書きを確認" -ForegroundColor Gray