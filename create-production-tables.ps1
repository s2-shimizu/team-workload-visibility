# 本番用DynamoDBテーブル作成スクリプト
param(
    [string]$Environment = "prod",
    [string]$Region = "ap-northeast-1",
    [switch]$EnableBackup = $true,
    [switch]$EnableEncryption = $true
)

Write-Host "=== 本番用DynamoDBテーブル作成 ===" -ForegroundColor Green
Write-Host "Environment: $Environment" -ForegroundColor Yellow
Write-Host "Region: $Region" -ForegroundColor Yellow
Write-Host "Backup: $EnableBackup" -ForegroundColor Yellow
Write-Host "Encryption: $EnableEncryption" -ForegroundColor Yellow
Write-Host ""

# 前提条件チェック
Write-Host "1. 前提条件チェック" -ForegroundColor Cyan

try {
    $identity = aws sts get-caller-identity --output json | ConvertFrom-Json
    Write-Host "✅ AWS認証: $($identity.Arn)" -ForegroundColor Green
    Write-Host "   アカウント: $($identity.Account)" -ForegroundColor Gray
} catch {
    Write-Host "❌ AWS認証が設定されていません。" -ForegroundColor Red
    exit 1
}

Write-Host ""

# 2. WorkloadStatusテーブル作成
Write-Host "2. WorkloadStatusテーブル作成" -ForegroundColor Cyan

$workloadTableName = "WorkloadStatus-$Environment"

# テーブル存在確認
try {
    aws dynamodb describe-table --table-name $workloadTableName --region $Region --output json | Out-Null
    Write-Host "⚠️ テーブル '$workloadTableName' は既に存在します" -ForegroundColor Yellow
} catch {
    Write-Host "WorkloadStatusテーブルを作成中..." -ForegroundColor Gray
    
    # テーブル定義
    $workloadTableDef = @{
        TableName = $workloadTableName
        KeySchema = @(
            @{
                AttributeName = "userId"
                KeyType = "HASH"
            }
        )
        AttributeDefinitions = @(
            @{
                AttributeName = "userId"
                AttributeType = "S"
            }
        )
        BillingMode = "PAY_PER_REQUEST"
        Tags = @(
            @{
                Key = "Environment"
                Value = $Environment
            },
            @{
                Key = "Application"
                Value = "TeamDashboard"
            },
            @{
                Key = "Purpose"
                Value = "WorkloadStatus"
            }
        )
    }
    
    # 暗号化設定
    if ($EnableEncryption) {
        $workloadTableDef.SSESpecification = @{
            Enabled = $true
            SSEType = "KMS"
        }
    }
    
    # テーブル作成
    try {
        $workloadTableJson = $workloadTableDef | ConvertTo-Json -Depth 10
        $workloadTableJson | Out-File -FilePath "workload-table-def.json" -Encoding UTF8
        
        aws dynamodb create-table --cli-input-json file://workload-table-def.json --region $Region
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ WorkloadStatusテーブル作成開始" -ForegroundColor Green
        } else {
            throw "Table creation failed"
        }
        
        # テーブル作成完了を待機
        Write-Host "テーブル作成完了を待機中..." -ForegroundColor Gray
        aws dynamodb wait table-exists --table-name $workloadTableName --region $Region
        Write-Host "✅ WorkloadStatusテーブル作成完了" -ForegroundColor Green
        
    } catch {
        Write-Host "❌ WorkloadStatusテーブル作成エラー: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    } finally {
        Remove-Item -Path "workload-table-def.json" -Force -ErrorAction SilentlyContinue
    }
}

Write-Host ""

# 3. TeamIssueテーブル作成
Write-Host "3. TeamIssueテーブル作成" -ForegroundColor Cyan

$issueTableName = "TeamIssue-$Environment"

# テーブル存在確認
try {
    aws dynamodb describe-table --table-name $issueTableName --region $Region --output json | Out-Null
    Write-Host "⚠️ テーブル '$issueTableName' は既に存在します" -ForegroundColor Yellow
} catch {
    Write-Host "TeamIssueテーブルを作成中..." -ForegroundColor Gray
    
    # テーブル定義
    $issueTableDef = @{
        TableName = $issueTableName
        KeySchema = @(
            @{
                AttributeName = "issueId"
                KeyType = "HASH"
            }
        )
        AttributeDefinitions = @(
            @{
                AttributeName = "issueId"
                AttributeType = "S"
            },
            @{
                AttributeName = "status"
                AttributeType = "S"
            },
            @{
                AttributeName = "createdAt"
                AttributeType = "N"
            }
        )
        BillingMode = "PAY_PER_REQUEST"
        GlobalSecondaryIndexes = @(
            @{
                IndexName = "StatusIndex"
                KeySchema = @(
                    @{
                        AttributeName = "status"
                        KeyType = "HASH"
                    },
                    @{
                        AttributeName = "createdAt"
                        KeyType = "RANGE"
                    }
                )
                Projection = @{
                    ProjectionType = "ALL"
                }
            }
        )
        Tags = @(
            @{
                Key = "Environment"
                Value = $Environment
            },
            @{
                Key = "Application"
                Value = "TeamDashboard"
            },
            @{
                Key = "Purpose"
                Value = "TeamIssue"
            }
        )
    }
    
    # 暗号化設定
    if ($EnableEncryption) {
        $issueTableDef.SSESpecification = @{
            Enabled = $true
            SSEType = "KMS"
        }
    }
    
    # テーブル作成
    try {
        $issueTableJson = $issueTableDef | ConvertTo-Json -Depth 10
        $issueTableJson | Out-File -FilePath "issue-table-def.json" -Encoding UTF8
        
        aws dynamodb create-table --cli-input-json file://issue-table-def.json --region $Region
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ TeamIssueテーブル作成開始" -ForegroundColor Green
        } else {
            throw "Table creation failed"
        }
        
        # テーブル作成完了を待機
        Write-Host "テーブル作成完了を待機中..." -ForegroundColor Gray
        aws dynamodb wait table-exists --table-name $issueTableName --region $Region
        Write-Host "✅ TeamIssueテーブル作成完了" -ForegroundColor Green
        
    } catch {
        Write-Host "❌ TeamIssueテーブル作成エラー: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    } finally {
        Remove-Item -Path "issue-table-def.json" -Force -ErrorAction SilentlyContinue
    }
}

Write-Host ""

# 4. バックアップ設定
if ($EnableBackup) {
    Write-Host "4. バックアップ設定" -ForegroundColor Cyan
    
    # WorkloadStatusテーブルのバックアップ
    Write-Host "WorkloadStatusテーブルのバックアップ設定..." -ForegroundColor Gray
    try {
        aws dynamodb put-backup-policy --table-name $workloadTableName --backup-policy BillingMode=PAY_PER_REQUEST --region $Region
        Write-Host "✅ WorkloadStatusバックアップ設定完了" -ForegroundColor Green
    } catch {
        Write-Host "⚠️ WorkloadStatusバックアップ設定エラー: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    
    # TeamIssueテーブルのバックアップ
    Write-Host "TeamIssueテーブルのバックアップ設定..." -ForegroundColor Gray
    try {
        aws dynamodb put-backup-policy --table-name $issueTableName --backup-policy BillingMode=PAY_PER_REQUEST --region $Region
        Write-Host "✅ TeamIssueバックアップ設定完了" -ForegroundColor Green
    } catch {
        Write-Host "⚠️ TeamIssueバックアップ設定エラー: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    
    Write-Host ""
}

# 5. テーブル情報確認
Write-Host "5. テーブル情報確認" -ForegroundColor Cyan

# WorkloadStatusテーブル情報
try {
    $workloadInfo = aws dynamodb describe-table --table-name $workloadTableName --region $Region --output json | ConvertFrom-Json
    Write-Host "WorkloadStatusテーブル:" -ForegroundColor Yellow
    Write-Host "  ステータス: $($workloadInfo.Table.TableStatus)" -ForegroundColor Gray
    Write-Host "  ARN: $($workloadInfo.Table.TableArn)" -ForegroundColor Gray
    Write-Host "  作成日時: $($workloadInfo.Table.CreationDateTime)" -ForegroundColor Gray
    Write-Host "  課金モード: $($workloadInfo.Table.BillingModeSummary.BillingMode)" -ForegroundColor Gray
    if ($workloadInfo.Table.SSEDescription) {
        Write-Host "  暗号化: 有効 ($($workloadInfo.Table.SSEDescription.Status))" -ForegroundColor Gray
    }
} catch {
    Write-Host "⚠️ WorkloadStatusテーブル情報取得エラー" -ForegroundColor Yellow
}

Write-Host ""

# TeamIssueテーブル情報
try {
    $issueInfo = aws dynamodb describe-table --table-name $issueTableName --region $Region --output json | ConvertFrom-Json
    Write-Host "TeamIssueテーブル:" -ForegroundColor Yellow
    Write-Host "  ステータス: $($issueInfo.Table.TableStatus)" -ForegroundColor Gray
    Write-Host "  ARN: $($issueInfo.Table.TableArn)" -ForegroundColor Gray
    Write-Host "  作成日時: $($issueInfo.Table.CreationDateTime)" -ForegroundColor Gray
    Write-Host "  課金モード: $($issueInfo.Table.BillingModeSummary.BillingMode)" -ForegroundColor Gray
    Write-Host "  GSI数: $($issueInfo.Table.GlobalSecondaryIndexes.Count)" -ForegroundColor Gray
    if ($issueInfo.Table.SSEDescription) {
        Write-Host "  暗号化: 有効 ($($issueInfo.Table.SSEDescription.Status))" -ForegroundColor Gray
    }
} catch {
    Write-Host "⚠️ TeamIssueテーブル情報取得エラー" -ForegroundColor Yellow
}

Write-Host ""

# 6. IAMポリシー生成
Write-Host "6. IAMポリシー生成" -ForegroundColor Cyan

$iamPolicy = @{
    Version = "2012-10-17"
    Statement = @(
        @{
            Effect = "Allow"
            Action = @(
                "dynamodb:GetItem",
                "dynamodb:PutItem",
                "dynamodb:UpdateItem",
                "dynamodb:DeleteItem",
                "dynamodb:Query",
                "dynamodb:Scan"
            )
            Resource = @(
                "arn:aws:dynamodb:${Region}:$($identity.Account):table/$workloadTableName",
                "arn:aws:dynamodb:${Region}:$($identity.Account):table/$issueTableName",
                "arn:aws:dynamodb:${Region}:$($identity.Account):table/$issueTableName/index/*"
            )
        }
    )
} | ConvertTo-Json -Depth 10

$iamPolicy | Out-File -FilePath "dynamodb-policy-$Environment.json" -Encoding UTF8
Write-Host "✅ IAMポリシーファイル生成: dynamodb-policy-$Environment.json" -ForegroundColor Green

Write-Host ""
Write-Host "=== 本番用DynamoDBテーブル作成完了 ===" -ForegroundColor Green

Write-Host ""
Write-Host "作成されたリソース:" -ForegroundColor Cyan
Write-Host "  WorkloadStatusテーブル: $workloadTableName" -ForegroundColor Yellow
Write-Host "  TeamIssueテーブル: $issueTableName" -ForegroundColor Yellow
Write-Host "  IAMポリシーファイル: dynamodb-policy-$Environment.json" -ForegroundColor Yellow

Write-Host ""
Write-Host "次のステップ:" -ForegroundColor Cyan
Write-Host "1. IAMロールにDynamoDBポリシーをアタッチ:" -ForegroundColor Gray
Write-Host "   aws iam attach-role-policy --role-name YourRole --policy-arn arn:aws:iam::$($identity.Account):policy/DynamoDBAccess" -ForegroundColor Gray
Write-Host ""
Write-Host "2. アプリケーション環境変数を設定:" -ForegroundColor Gray
Write-Host "   WORKLOAD_STATUS_TABLE=$workloadTableName" -ForegroundColor Gray
Write-Host "   TEAM_ISSUE_TABLE=$issueTableName" -ForegroundColor Gray
Write-Host ""
Write-Host "3. 本番デプロイを実行:" -ForegroundColor Gray
Write-Host "   .\deploy-ecs-fargate.ps1 -Environment $Environment" -ForegroundColor Gray
Write-Host ""
Write-Host "4. テーブル動作確認:" -ForegroundColor Gray
Write-Host "   .\test-dynamodb-integration.ps1 -BaseUrl 'https://your-prod-endpoint'" -ForegroundColor Gray

Write-Host ""
Write-Host "使用方法:" -ForegroundColor Yellow
Write-Host "  基本作成: .\create-production-tables.ps1 -Environment prod" -ForegroundColor Gray
Write-Host "  暗号化なし: .\create-production-tables.ps1 -Environment prod -EnableEncryption:$false" -ForegroundColor Gray
Write-Host "  バックアップなし: .\create-production-tables.ps1 -Environment prod -EnableBackup:$false" -ForegroundColor Gray