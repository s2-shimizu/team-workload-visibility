# 既存DynamoDBテーブル確認スクリプト
param(
    [string]$Region = "ap-northeast-1"
)

Write-Host "=== 既存DynamoDBテーブル確認 ===" -ForegroundColor Green
Write-Host "リージョン: $Region" -ForegroundColor Yellow
Write-Host ""

# AWS CLIの確認
try {
    $awsVersion = aws --version
    Write-Host "✅ AWS CLI確認: $awsVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ AWS CLIが見つかりません。" -ForegroundColor Red
    exit 1
}

# AWS認証情報の確認
try {
    $identity = aws sts get-caller-identity --output json | ConvertFrom-Json
    Write-Host "✅ AWS認証確認: $($identity.Arn)" -ForegroundColor Green
} catch {
    Write-Host "❌ AWS認証が設定されていません。" -ForegroundColor Red
    exit 1
}

Write-Host ""

# 1. 全てのDynamoDBテーブルを一覧表示
Write-Host "1. DynamoDBテーブル一覧" -ForegroundColor Cyan
try {
    $tables = aws dynamodb list-tables --region $Region --output json | ConvertFrom-Json
    Write-Host "✅ テーブル一覧取得: 成功" -ForegroundColor Green
    Write-Host "   総テーブル数: $($tables.TableNames.Count)" -ForegroundColor Gray
    
    foreach ($tableName in $tables.TableNames) {
        Write-Host "   - $tableName" -ForegroundColor Gray
    }
} catch {
    Write-Host "❌ テーブル一覧取得: 失敗 - $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""

# 2. チーム関連テーブルの詳細確認
Write-Host "2. チーム関連テーブルの詳細確認" -ForegroundColor Cyan

$teamRelatedTables = $tables.TableNames | Where-Object { 
    $_ -like "*team*" -or 
    $_ -like "*workload*" -or 
    $_ -like "*issue*" -or
    $_ -like "*dashboard*" -or
    $_ -like "*Team*" -or
    $_ -like "*Workload*" -or
    $_ -like "*Issue*" -or
    $_ -like "*Dashboard*"
}

if ($teamRelatedTables.Count -eq 0) {
    Write-Host "⚠️ チーム関連のテーブルが見つかりません。" -ForegroundColor Yellow
    Write-Host "   全テーブルの詳細を確認します..." -ForegroundColor Gray
    $teamRelatedTables = $tables.TableNames | Select-Object -First 10  # 最初の10個のテーブルを確認
}

foreach ($tableName in $teamRelatedTables) {
    Write-Host ""
    Write-Host "テーブル: $tableName" -ForegroundColor Yellow
    
    try {
        $tableInfo = aws dynamodb describe-table --table-name $tableName --region $Region --output json | ConvertFrom-Json
        
        Write-Host "  ステータス: $($tableInfo.Table.TableStatus)" -ForegroundColor Gray
        Write-Host "  作成日時: $($tableInfo.Table.CreationDateTime)" -ForegroundColor Gray
        Write-Host "  アイテム数: $($tableInfo.Table.ItemCount)" -ForegroundColor Gray
        Write-Host "  テーブルサイズ: $($tableInfo.Table.TableSizeBytes) bytes" -ForegroundColor Gray
        
        # キースキーマの表示
        Write-Host "  キースキーマ:" -ForegroundColor Gray
        foreach ($key in $tableInfo.Table.KeySchema) {
            Write-Host "    - $($key.AttributeName) ($($key.KeyType))" -ForegroundColor Gray
        }
        
        # 属性定義の表示
        Write-Host "  属性定義:" -ForegroundColor Gray
        foreach ($attr in $tableInfo.Table.AttributeDefinitions) {
            Write-Host "    - $($attr.AttributeName): $($attr.AttributeType)" -ForegroundColor Gray
        }
        
        # プロビジョニング情報
        if ($tableInfo.Table.BillingModeSummary) {
            Write-Host "  課金モード: $($tableInfo.Table.BillingModeSummary.BillingMode)" -ForegroundColor Gray
        }
        
        if ($tableInfo.Table.ProvisionedThroughput) {
            Write-Host "  プロビジョニング:" -ForegroundColor Gray
            Write-Host "    読み取り: $($tableInfo.Table.ProvisionedThroughput.ReadCapacityUnits)" -ForegroundColor Gray
            Write-Host "    書き込み: $($tableInfo.Table.ProvisionedThroughput.WriteCapacityUnits)" -ForegroundColor Gray
        }
        
        # サンプルデータの確認（最初の5件）
        Write-Host "  サンプルデータ:" -ForegroundColor Gray
        try {
            $scanResult = aws dynamodb scan --table-name $tableName --limit 5 --region $Region --output json | ConvertFrom-Json
            if ($scanResult.Items.Count -gt 0) {
                Write-Host "    データ件数: $($scanResult.Items.Count) 件（最初の5件を表示）" -ForegroundColor Gray
                foreach ($item in $scanResult.Items) {
                    $itemKeys = $item.PSObject.Properties.Name | Select-Object -First 3
                    $keyValues = $itemKeys | ForEach-Object { 
                        $value = $item.$_
                        if ($value.S) { "$_=$($value.S)" }
                        elseif ($value.N) { "$_=$($value.N)" }
                        elseif ($value.BOOL) { "$_=$($value.BOOL)" }
                        else { "$_=..." }
                    }
                    Write-Host "      { $($keyValues -join ', ') }" -ForegroundColor Gray
                }
            } else {
                Write-Host "    データなし" -ForegroundColor Gray
            }
        } catch {
            Write-Host "    データ確認エラー: $($_.Exception.Message)" -ForegroundColor Yellow
        }
        
    } catch {
        Write-Host "  ❌ テーブル詳細取得エラー: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""

# 3. 推奨設定の生成
Write-Host "3. 推奨アプリケーション設定" -ForegroundColor Cyan

$workloadTable = $tables.TableNames | Where-Object { $_ -like "*workload*" -or $_ -like "*Workload*" } | Select-Object -First 1
$issueTable = $tables.TableNames | Where-Object { $_ -like "*issue*" -or $_ -like "*Issue*" -or $_ -like "*team*" -or $_ -like "*Team*" } | Select-Object -First 1

if ($workloadTable -or $issueTable) {
    Write-Host "推奨テーブル名設定:" -ForegroundColor Green
    if ($workloadTable) {
        Write-Host "  WorkloadStatus テーブル: $workloadTable" -ForegroundColor Gray
    }
    if ($issueTable) {
        Write-Host "  TeamIssue テーブル: $issueTable" -ForegroundColor Gray
    }
    
    Write-Host ""
    Write-Host "application.yml 設定例:" -ForegroundColor Yellow
    Write-Host "aws:" -ForegroundColor Gray
    Write-Host "  region: $Region" -ForegroundColor Gray
    Write-Host "  dynamodb:" -ForegroundColor Gray
    Write-Host "    tables:" -ForegroundColor Gray
    if ($workloadTable) {
        Write-Host "      workload-status: $workloadTable" -ForegroundColor Gray
    }
    if ($issueTable) {
        Write-Host "      team-issue: $issueTable" -ForegroundColor Gray
    }
} else {
    Write-Host "⚠️ 適切なテーブルが見つかりませんでした。" -ForegroundColor Yellow
    Write-Host "   手動でテーブル名を確認してください。" -ForegroundColor Gray
}

Write-Host ""
Write-Host "=== 確認完了 ===" -ForegroundColor Green
Write-Host ""
Write-Host "次のステップ:" -ForegroundColor Cyan
Write-Host "1. 上記の推奨設定をapplication.ymlに反映" -ForegroundColor Gray
Write-Host "2. リポジトリクラスでテーブル名を更新" -ForegroundColor Gray
Write-Host "3. DynamoDB統合テストを実行" -ForegroundColor Gray