# SAMデプロイ競合解決スクリプト
param(
    [string]$Environment = "dev",
    [string]$StackName = "team-dashboard",
    [string]$Region = "ap-northeast-1",
    [ValidateSet("delete-tables", "use-existing", "new-stack")]
    [string]$Resolution = "use-existing"
)

Write-Host "=== SAMデプロイ競合解決 ===" -ForegroundColor Green
Write-Host "Environment: $Environment" -ForegroundColor Yellow
Write-Host "Stack Name: $StackName-$Environment" -ForegroundColor Yellow
Write-Host "Resolution: $Resolution" -ForegroundColor Yellow
Write-Host ""

# 1. 現在の状況確認
Write-Host "1. 現在の状況確認" -ForegroundColor Cyan

# 既存スタック確認
try {
    $existingStacks = aws cloudformation describe-stacks --region $Region --output json | ConvertFrom-Json
    $conflictingStacks = $existingStacks.Stacks | Where-Object { 
        $_.StackName -like "*team-dashboard*" -and $_.StackStatus -ne "DELETE_COMPLETE" 
    }
    
    if ($conflictingStacks.Count -gt 0) {
        Write-Host "既存のスタック:" -ForegroundColor Yellow
        foreach ($stack in $conflictingStacks) {
            Write-Host "  - $($stack.StackName): $($stack.StackStatus)" -ForegroundColor Gray
        }
    }
} catch {
    Write-Host "⚠️ スタック確認エラー: $($_.Exception.Message)" -ForegroundColor Yellow
}

# 既存DynamoDBテーブル確認
Write-Host ""
Write-Host "既存DynamoDBテーブル確認:" -ForegroundColor Yellow

$workloadTableName = "WorkloadStatus-$Environment"
$issueTableName = "TeamIssue-$Environment"

$workloadTableExists = $false
$issueTableExists = $false

try {
    aws dynamodb describe-table --table-name $workloadTableName --region $Region --output json | Out-Null
    Write-Host "  ✅ $workloadTableName: 存在" -ForegroundColor Green
    $workloadTableExists = $true
} catch {
    Write-Host "  ❌ $workloadTableName: 存在しない" -ForegroundColor Red
}

try {
    aws dynamodb describe-table --table-name $issueTableName --region $Region --output json | Out-Null
    Write-Host "  ✅ $issueTableName: 存在" -ForegroundColor Green
    $issueTableExists = $true
} catch {
    Write-Host "  ❌ $issueTableName: 存在しない" -ForegroundColor Red
}

Write-Host ""

# 2. 解決方法の実行
Write-Host "2. 解決方法の実行" -ForegroundColor Cyan

switch ($Resolution) {
    "delete-tables" {
        Write-Host "既存テーブルを削除します..." -ForegroundColor Yellow
        
        if ($workloadTableExists) {
            Write-Host "WorkloadStatusテーブルを削除中..." -ForegroundColor Gray
            try {
                aws dynamodb delete-table --table-name $workloadTableName --region $Region
                Write-Host "✅ WorkloadStatusテーブル削除開始" -ForegroundColor Green
            } catch {
                Write-Host "❌ WorkloadStatusテーブル削除エラー: $($_.Exception.Message)" -ForegroundColor Red
            }
        }
        
        if ($issueTableExists) {
            Write-Host "TeamIssueテーブルを削除中..." -ForegroundColor Gray
            try {
                aws dynamodb delete-table --table-name $issueTableName --region $Region
                Write-Host "✅ TeamIssueテーブル削除開始" -ForegroundColor Green
            } catch {
                Write-Host "❌ TeamIssueテーブル削除エラー: $($_.Exception.Message)" -ForegroundColor Red
            }
        }
        
        Write-Host ""
        Write-Host "⏳ テーブル削除完了を待機中（約2-3分）..." -ForegroundColor Yellow
        
        # テーブル削除完了を待機
        if ($workloadTableExists) {
            Write-Host "WorkloadStatusテーブル削除完了を待機中..." -ForegroundColor Gray
            aws dynamodb wait table-not-exists --table-name $workloadTableName --region $Region
        }
        
        if ($issueTableExists) {
            Write-Host "TeamIssueテーブル削除完了を待機中..." -ForegroundColor Gray
            aws dynamodb wait table-not-exists --table-name $issueTableName --region $Region
        }
        
        Write-Host "✅ テーブル削除完了" -ForegroundColor Green
    }
    
    "use-existing" {
        Write-Host "既存テーブルを使用するようにSAMテンプレートを修正します..." -ForegroundColor Yellow
        
        # template.yamlをバックアップ
        Copy-Item "template.yaml" "template.yaml.backup"
        Write-Host "✅ template.yamlをバックアップしました" -ForegroundColor Green
        
        # template.yamlを読み込み
        $templateContent = Get-Content "template.yaml" -Raw
        
        # DynamoDBテーブル定義を削除し、既存テーブルを参照するように修正
        $modifiedTemplate = $templateContent -replace '(?s)  WorkloadStatusTable:.*?(?=  \w+:|Outputs:)', ''
        $modifiedTemplate = $modifiedTemplate -replace '(?s)  TeamIssueTable:.*?(?=Outputs:)', ''
        
        # 環境変数を既存テーブル名に設定
        $modifiedTemplate = $modifiedTemplate -replace 'WORKLOAD_STATUS_TABLE: !Ref WorkloadStatusTable', "WORKLOAD_STATUS_TABLE: $workloadTableName"
        $modifiedTemplate = $modifiedTemplate -replace 'TEAM_ISSUE_TABLE: !Ref TeamIssueTable', "TEAM_ISSUE_TABLE: $issueTableName"
        
        # DynamoDBCrudPolicyを既存テーブル名に変更
        $modifiedTemplate = $modifiedTemplate -replace 'TableName: !Ref WorkloadStatusTable', "TableName: $workloadTableName"
        $modifiedTemplate = $modifiedTemplate -replace 'TableName: !Ref TeamIssueTable', "TableName: $issueTableName"
        
        # 修正されたテンプレートを保存
        $modifiedTemplate | Out-File -FilePath "template-existing-tables.yaml" -Encoding UTF8
        
        Write-Host "✅ 既存テーブル用のSAMテンプレートを作成しました: template-existing-tables.yaml" -ForegroundColor Green
    }
    
    "new-stack" {
        Write-Host "新しいスタック名でデプロイします..." -ForegroundColor Yellow
        $newStackName = "$StackName-v3-$Environment"
        Write-Host "新しいスタック名: $newStackName" -ForegroundColor Gray
        
        # 新しいテーブル名を使用するようにテンプレートを修正
        $templateContent = Get-Content "template.yaml" -Raw
        $modifiedTemplate = $templateContent -replace "WorkloadStatus-\\\${Environment}", "WorkloadStatus-v3-\\\${Environment}"
        $modifiedTemplate = $modifiedTemplate -replace "TeamIssue-\\\${Environment}", "TeamIssue-v3-\\\${Environment}"
        
        $modifiedTemplate | Out-File -FilePath "template-v3.yaml" -Encoding UTF8
        Write-Host "✅ 新しいスタック用のSAMテンプレートを作成しました: template-v3.yaml" -ForegroundColor Green
    }
}

Write-Host ""

# 3. 失敗したスタックのクリーンアップ
Write-Host "3. 失敗したスタックのクリーンアップ" -ForegroundColor Cyan

$failedStackName = "$StackName-$Environment"
try {
    $stackInfo = aws cloudformation describe-stacks --stack-name $failedStackName --region $Region --output json | ConvertFrom-Json
    $stackStatus = $stackInfo.Stacks[0].StackStatus
    
    if ($stackStatus -eq "CREATE_FAILED" -or $stackStatus -eq "ROLLBACK_COMPLETE") {
        Write-Host "失敗したスタックを削除中: $failedStackName" -ForegroundColor Gray
        aws cloudformation delete-stack --stack-name $failedStackName --region $Region
        Write-Host "✅ 失敗したスタック削除開始" -ForegroundColor Green
        
        Write-Host "スタック削除完了を待機中..." -ForegroundColor Gray
        aws cloudformation wait stack-delete-complete --stack-name $failedStackName --region $Region
        Write-Host "✅ スタック削除完了" -ForegroundColor Green
    }
} catch {
    Write-Host "⚠️ スタック削除スキップ（スタックが存在しないか、削除不要）" -ForegroundColor Yellow
}

Write-Host ""

# 4. 推奨される次のステップ
Write-Host "4. 推奨される次のステップ" -ForegroundColor Cyan

switch ($Resolution) {
    "delete-tables" {
        Write-Host "テーブルを削除しました。通常のSAMデプロイを実行してください:" -ForegroundColor Green
        Write-Host "  sam build" -ForegroundColor Gray
        Write-Host "  sam deploy --stack-name $StackName-$Environment --parameter-overrides Environment=$Environment --capabilities CAPABILITY_IAM --resolve-s3" -ForegroundColor Gray
    }
    
    "use-existing" {
        Write-Host "既存テーブル用のテンプレートを作成しました。以下のコマンドでデプロイしてください:" -ForegroundColor Green
        Write-Host "  sam build --template template-existing-tables.yaml" -ForegroundColor Gray
        Write-Host "  sam deploy --template-file .aws-sam/build/template.yaml --stack-name $StackName-$Environment --parameter-overrides Environment=$Environment --capabilities CAPABILITY_IAM --resolve-s3" -ForegroundColor Gray
    }
    
    "new-stack" {
        Write-Host "新しいスタック用のテンプレートを作成しました。以下のコマンドでデプロイしてください:" -ForegroundColor Green
        Write-Host "  sam build --template template-v3.yaml" -ForegroundColor Gray
        Write-Host "  sam deploy --template-file .aws-sam/build/template.yaml --stack-name $StackName-v3-$Environment --parameter-overrides Environment=$Environment --capabilities CAPABILITY_IAM --resolve-s3" -ForegroundColor Gray
    }
}

Write-Host ""

# 5. 統合デプロイスクリプトの提案
Write-Host "5. 統合デプロイスクリプトの使用" -ForegroundColor Cyan
Write-Host "より簡単な方法として、以下のスクリプトを使用することもできます:" -ForegroundColor Yellow

switch ($Resolution) {
    "delete-tables" {
        Write-Host "  .\deploy-lambda.ps1 -Environment $Environment" -ForegroundColor Gray
    }
    "use-existing" {
        Write-Host "  # 既存テーブルを使用する場合は、deploy-lambda.ps1を修正が必要" -ForegroundColor Gray
        Write-Host "  # または手動でSAMデプロイを実行してください" -ForegroundColor Gray
    }
    "new-stack" {
        Write-Host "  # 新しいスタック名でdeploy-lambda.ps1を実行" -ForegroundColor Gray
        Write-Host "  .\deploy-lambda.ps1 -Environment $Environment -StackName team-dashboard-v3" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "=== 競合解決完了 ===" -ForegroundColor Green

Write-Host ""
Write-Host "使用方法:" -ForegroundColor Yellow
Write-Host "  テーブル削除: .\fix-sam-deploy-conflict.ps1 -Resolution delete-tables" -ForegroundColor Gray
Write-Host "  既存テーブル使用: .\fix-sam-deploy-conflict.ps1 -Resolution use-existing" -ForegroundColor Gray
Write-Host "  新しいスタック: .\fix-sam-deploy-conflict.ps1 -Resolution new-stack" -ForegroundColor Gray