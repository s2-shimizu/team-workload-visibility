# Lambda専用デプロイスクリプト
param(
    [string]$Environment = "dev",
    [string]$StackName = "team-dashboard-lambda",
    [string]$Region = "ap-northeast-1",
    [switch]$SkipBuild = $false,
    [switch]$SkipTests = $true,
    [switch]$Guided = $false
)

Write-Host "=== Lambda専用デプロイ ===" -ForegroundColor Green
Write-Host "Environment: $Environment" -ForegroundColor Yellow
Write-Host "Stack Name: $StackName-$Environment" -ForegroundColor Yellow
Write-Host "Region: $Region" -ForegroundColor Yellow
Write-Host ""

$startTime = Get-Date

# エラーハンドリング
$ErrorActionPreference = "Stop"

try {
    # 1. 前提条件チェック
    Write-Host "=== Step 1: 前提条件チェック ===" -ForegroundColor Cyan
    
    # Java確認
    try {
        $javaVersion = java -version 2>&1 | Select-String "version"
        Write-Host "✅ Java: $javaVersion" -ForegroundColor Green
    } catch {
        throw "Javaが見つかりません。Java 17をインストールしてください。"
    }
    
    # Maven確認
    try {
        $mavenVersion = mvn --version | Select-String "Apache Maven"
        Write-Host "✅ Maven: $mavenVersion" -ForegroundColor Green
    } catch {
        throw "Mavenが見つかりません。Mavenをインストールしてください。"
    }
    
    # AWS CLI確認
    try {
        $awsVersion = aws --version
        Write-Host "✅ AWS CLI: $awsVersion" -ForegroundColor Green
    } catch {
        throw "AWS CLIが見つかりません。AWS CLIをインストールしてください。"
    }
    
    # SAM CLI確認
    try {
        $samVersion = sam --version
        Write-Host "✅ SAM CLI: $samVersion" -ForegroundColor Green
    } catch {
        throw "SAM CLIが見つかりません。SAM CLIをインストールしてください。"
    }
    
    # AWS認証確認
    try {
        $identity = aws sts get-caller-identity --output json | ConvertFrom-Json
        Write-Host "✅ AWS認証: $($identity.Arn)" -ForegroundColor Green
        Write-Host "   アカウント: $($identity.Account)" -ForegroundColor Gray
        Write-Host "   リージョン: $Region" -ForegroundColor Gray
    } catch {
        throw "AWS認証が設定されていません。aws configure を実行してください。"
    }
    
    Write-Host ""
    
    # 2. DynamoDBテーブル確認・作成
    Write-Host "=== Step 2: DynamoDBテーブル確認・作成 ===" -ForegroundColor Cyan
    
    $workloadTableName = "WorkloadStatus-$Environment"
    $issueTableName = "TeamIssue-$Environment"
    
    # WorkloadStatusテーブル確認
    try {
        aws dynamodb describe-table --table-name $workloadTableName --region $Region --output json | Out-Null
        Write-Host "✅ WorkloadStatusテーブル確認: $workloadTableName" -ForegroundColor Green
    } catch {
        Write-Host "⚠️ WorkloadStatusテーブルが存在しません。作成します..." -ForegroundColor Yellow
        .\create-dynamodb-tables.ps1 -Environment $Environment -Region $Region
        if ($LASTEXITCODE -ne 0) {
            throw "DynamoDBテーブル作成に失敗しました"
        }
    }
    
    # TeamIssueテーブル確認
    try {
        aws dynamodb describe-table --table-name $issueTableName --region $Region --output json | Out-Null
        Write-Host "✅ TeamIssueテーブル確認: $issueTableName" -ForegroundColor Green
    } catch {
        Write-Host "⚠️ TeamIssueテーブルが存在しません。作成します..." -ForegroundColor Yellow
        if (-not (Test-Path "create-dynamodb-tables.ps1")) {
            .\create-production-tables.ps1 -Environment $Environment -Region $Region
        }
    }
    
    Write-Host ""
    
    # 3. Mavenビルド
    if (-not $SkipBuild) {
        Write-Host "=== Step 3: Mavenビルド ===" -ForegroundColor Cyan
        
        try {
            Set-Location backend
            
            Write-Host "Maven clean..." -ForegroundColor Gray
            if ($SkipTests) {
                mvn clean -q
            } else {
                mvn clean test -q
            }
            if ($LASTEXITCODE -ne 0) {
                throw "Maven clean failed"
            }
            
            Write-Host "Maven package (Lambda profile)..." -ForegroundColor Gray
            if ($SkipTests) {
                mvn package -Plambda -DskipTests -q
            } else {
                mvn package -Plambda -q
            }
            if ($LASTEXITCODE -ne 0) {
                throw "Maven package failed"
            }
            
            # JARファイルの確認
            $jarFiles = Get-ChildItem -Path "target" -Name "*.jar" | Where-Object { $_ -like "*lambda*" }
            if ($jarFiles.Count -gt 0) {
                Write-Host "✅ Lambda JAR生成: $($jarFiles[0])" -ForegroundColor Green
                $jarSize = (Get-Item "target/$($jarFiles[0])").Length / 1MB
                Write-Host "   サイズ: $([math]::Round($jarSize, 2)) MB" -ForegroundColor Gray
                
                if ($jarSize -gt 250) {
                    Write-Host "   ⚠️ JARサイズが大きいです。最適化を検討してください。" -ForegroundColor Yellow
                }
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
    
    # 4. SAMテンプレート確認・作成
    Write-Host "=== Step 4: SAMテンプレート確認 ===" -ForegroundColor Cyan
    
    if (-not (Test-Path "template.yaml")) {
        Write-Host "SAMテンプレートを作成中..." -ForegroundColor Gray
        
        $samTemplate = @"
AWSTemplateFormatVersion: '2010-09-09'
Transform: AWS::Serverless-2016-10-31
Description: Team Dashboard Lambda Application

Parameters:
  Environment:
    Type: String
    Default: dev
    AllowedValues: [dev, staging, prod]

Globals:
  Function:
    Timeout: 30
    MemorySize: 512
    Runtime: java17
    Architectures:
      - x86_64
    Environment:
      Variables:
        SPRING_PROFILES_ACTIVE: lambda,dynamodb
        AWS_REGION: !Ref AWS::Region

Resources:
  TeamDashboardFunction:
    Type: AWS::Serverless::Function
    Properties:
      FunctionName: !Sub 'team-dashboard-\${Environment}'
      CodeUri: backend/target/team-dashboard-backend-1.0.0-lambda.jar
      Handler: com.teamdashboard.LambdaHandler::handleRequest
      Description: Team Dashboard Lambda Function
      Environment:
        Variables:
          WORKLOAD_STATUS_TABLE: !Ref WorkloadStatusTable
          TEAM_ISSUE_TABLE: !Ref TeamIssueTable
          ENVIRONMENT: !Ref Environment
      Policies:
        - DynamoDBCrudPolicy:
            TableName: !Ref WorkloadStatusTable
        - DynamoDBCrudPolicy:
            TableName: !Ref TeamIssueTable
      Events:
        ApiEvent:
          Type: Api
          Properties:
            Path: /{proxy+}
            Method: ANY
            RestApiId: !Ref TeamDashboardApi
        RootApiEvent:
          Type: Api
          Properties:
            Path: /
            Method: ANY
            RestApiId: !Ref TeamDashboardApi

  TeamDashboardApi:
    Type: AWS::Serverless::Api
    Properties:
      Name: !Sub 'team-dashboard-api-\${Environment}'
      StageName: !Ref Environment
      Cors:
        AllowMethods: "'GET,POST,PUT,DELETE,OPTIONS'"
        AllowHeaders: "'Content-Type,Authorization,X-Requested-With'"
        AllowOrigin: "'*'"
      GatewayResponses:
        DEFAULT_4XX:
          ResponseParameters:
            Headers:
              Access-Control-Allow-Origin: "'*'"
              Access-Control-Allow-Headers: "'Content-Type,Authorization,X-Requested-With'"
        DEFAULT_5XX:
          ResponseParameters:
            Headers:
              Access-Control-Allow-Origin: "'*'"
              Access-Control-Allow-Headers: "'Content-Type,Authorization,X-Requested-With'"

  WorkloadStatusTable:
    Type: AWS::DynamoDB::Table
    Properties:
      TableName: !Sub 'WorkloadStatus-\${Environment}'
      BillingMode: PAY_PER_REQUEST
      AttributeDefinitions:
        - AttributeName: userId
          AttributeType: S
      KeySchema:
        - AttributeName: userId
          KeyType: HASH
      Tags:
        - Key: Environment
          Value: !Ref Environment
        - Key: Application
          Value: TeamDashboard

  TeamIssueTable:
    Type: AWS::DynamoDB::Table
    Properties:
      TableName: !Sub 'TeamIssue-\${Environment}'
      BillingMode: PAY_PER_REQUEST
      AttributeDefinitions:
        - AttributeName: issueId
          AttributeType: S
        - AttributeName: status
          AttributeType: S
        - AttributeName: createdAt
          AttributeType: N
      KeySchema:
        - AttributeName: issueId
          KeyType: HASH
      GlobalSecondaryIndexes:
        - IndexName: StatusIndex
          KeySchema:
            - AttributeName: status
              KeyType: HASH
            - AttributeName: createdAt
              KeyType: RANGE
          Projection:
            ProjectionType: ALL
      Tags:
        - Key: Environment
          Value: !Ref Environment
        - Key: Application
          Value: TeamDashboard

Outputs:
  ApiGatewayEndpoint:
    Description: API Gateway endpoint URL
    Value: !Sub 'https://\${TeamDashboardApi}.execute-api.\${AWS::Region}.amazonaws.com/\${Environment}'
    Export:
      Name: !Sub '\${AWS::StackName}-ApiEndpoint'
  
  LambdaFunction:
    Description: Lambda Function ARN
    Value: !GetAtt TeamDashboardFunction.Arn
    Export:
      Name: !Sub '\${AWS::StackName}-LambdaArn'
  
  WorkloadStatusTableName:
    Description: WorkloadStatus DynamoDB Table Name
    Value: !Ref WorkloadStatusTable
    Export:
      Name: !Sub '\${AWS::StackName}-WorkloadTable'
  
  TeamIssueTableName:
    Description: TeamIssue DynamoDB Table Name
    Value: !Ref TeamIssueTable
    Export:
      Name: !Sub '\${AWS::StackName}-IssueTable'
"@
        
        $samTemplate | Out-File -FilePath "template.yaml" -Encoding UTF8
        Write-Host "✅ SAMテンプレート作成完了" -ForegroundColor Green
    } else {
        Write-Host "✅ SAMテンプレート確認完了" -ForegroundColor Green
    }
    
    Write-Host ""
    
    # 5. SAMビルド
    Write-Host "=== Step 5: SAMビルド ===" -ForegroundColor Cyan
    
    try {
        Write-Host "sam build実行中..." -ForegroundColor Gray
        sam build --region $Region
        if ($LASTEXITCODE -ne 0) {
            throw "SAM build failed"
        }
        Write-Host "✅ SAMビルド完了" -ForegroundColor Green
    } catch {
        Write-Host "❌ SAMビルドエラー: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
    
    Write-Host ""
    
    # 6. SAMデプロイ
    Write-Host "=== Step 6: SAMデプロイ ===" -ForegroundColor Cyan
    
    try {
        if ($Guided) {
            Write-Host "ガイド付きデプロイを実行中..." -ForegroundColor Gray
            sam deploy --guided --region $Region
        } else {
            Write-Host "デプロイ実行中..." -ForegroundColor Gray
            sam deploy `
                --stack-name "$StackName-$Environment" `
                --parameter-overrides Environment=$Environment `
                --capabilities CAPABILITY_IAM `
                --resolve-s3 `
                --region $Region `
                --no-confirm-changeset
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
    
    # 7. デプロイ結果確認
    Write-Host "=== Step 7: デプロイ結果確認 ===" -ForegroundColor Cyan
    
    try {
        Write-Host "スタック情報を取得中..." -ForegroundColor Gray
        $stackInfo = aws cloudformation describe-stacks --stack-name "$StackName-$Environment" --region $Region --output json | ConvertFrom-Json
        
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
                    } elseif ($output.OutputKey -eq "LambdaFunction") {
                        $script:LambdaArn = $output.OutputValue
                    }
                }
            }
        }
    } catch {
        Write-Host "⚠️ スタック情報取得エラー: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    
    Write-Host ""
    
    # 8. 統合テスト実行
    Write-Host "=== Step 8: 統合テスト実行 ===" -ForegroundColor Cyan
    
    if ($script:ApiEndpoint) {
        Write-Host "統合テストを実行中..." -ForegroundColor Gray
        
        # ヘルスチェック
        try {
            Start-Sleep -Seconds 10 # Lambda初期化を待機
            $healthCheck = Invoke-RestMethod -Uri "$($script:ApiEndpoint)/api/status" -TimeoutSec 30
            Write-Host "✅ ヘルスチェック成功: $($healthCheck.status)" -ForegroundColor Green
        } catch {
            Write-Host "⚠️ ヘルスチェック失敗: $($_.Exception.Message)" -ForegroundColor Yellow
            Write-Host "   Lambdaの初期化に時間がかかっている可能性があります" -ForegroundColor Gray
        }
        
        # ポーリング更新テスト
        try {
            Write-Host "ポーリング更新テストを実行中..." -ForegroundColor Gray
            .\test-polling-updates.ps1 -BaseUrl $script:ApiEndpoint -IntervalSeconds 5 -TestDurationMinutes 1
            Write-Host "✅ ポーリング更新テスト完了" -ForegroundColor Green
        } catch {
            Write-Host "⚠️ ポーリング更新テストでエラーが発生しました" -ForegroundColor Yellow
        }
    }
    
    Write-Host ""
    
    # 9. デプロイ完了
    $endTime = Get-Date
    $duration = $endTime - $startTime
    
    Write-Host "=== Lambdaデプロイ完了 ===" -ForegroundColor Green
    Write-Host "デプロイ時間: $($duration.Minutes)分$($duration.Seconds)秒" -ForegroundColor Gray
    Write-Host ""
    
    # デプロイサマリー
    Write-Host "🎉 Lambdaデプロイ成功！" -ForegroundColor Green
    Write-Host ""
    Write-Host "デプロイされたリソース:" -ForegroundColor Cyan
    Write-Host "  Environment: $Environment" -ForegroundColor Yellow
    Write-Host "  Region: $Region" -ForegroundColor Yellow
    if ($script:ApiEndpoint) {
        Write-Host "  API Gateway URL: $($script:ApiEndpoint)" -ForegroundColor Yellow
    }
    if ($script:LambdaArn) {
        Write-Host "  Lambda Function: $($script:LambdaArn)" -ForegroundColor Yellow
    }
    Write-Host "  DynamoDBテーブル: $workloadTableName, $issueTableName" -ForegroundColor Yellow
    
    Write-Host ""
    Write-Host "重要な注意事項:" -ForegroundColor Cyan
    Write-Host "  ⚠️ WebSocket機能は利用できません" -ForegroundColor Yellow
    Write-Host "  ✅ ポーリング更新（30秒間隔）で動作します" -ForegroundColor Green
    Write-Host "  ✅ 手動更新ボタン（🔄）で即座更新可能" -ForegroundColor Green
    
    Write-Host ""
    Write-Host "次のステップ:" -ForegroundColor Cyan
    Write-Host "1. フロントエンド設定を更新:" -ForegroundColor Gray
    Write-Host "   frontend/js/aws-config.js のendpointを '$($script:ApiEndpoint)' に更新" -ForegroundColor Gray
    Write-Host ""
    Write-Host "2. ポーリング更新の動作確認:" -ForegroundColor Gray
    Write-Host "   ブラウザでダッシュボードを開いて「🔄 定期更新」表示を確認" -ForegroundColor Gray
    Write-Host ""
    Write-Host "3. 継続的なテスト:" -ForegroundColor Gray
    Write-Host "   .\test-polling-updates.ps1 -BaseUrl '$($script:ApiEndpoint)'" -ForegroundColor Gray
    
    Write-Host ""
    Write-Host "監視・運用:" -ForegroundColor Cyan
    Write-Host "  Lambda Console: https://console.aws.amazon.com/lambda/" -ForegroundColor Gray
    Write-Host "  API Gateway Console: https://console.aws.amazon.com/apigateway/" -ForegroundColor Gray
    Write-Host "  DynamoDB Console: https://console.aws.amazon.com/dynamodb/" -ForegroundColor Gray
    Write-Host "  CloudWatch Logs: https://console.aws.amazon.com/cloudwatch/home#logsV2:" -ForegroundColor Gray
    
} catch {
    Write-Host ""
    Write-Host "❌ Lambdaデプロイエラー: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "トラブルシューティング:" -ForegroundColor Yellow
    Write-Host "1. Java 17がインストールされていることを確認" -ForegroundColor Gray
    Write-Host "2. AWS認証情報を確認" -ForegroundColor Gray
    Write-Host "3. 必要なIAM権限があることを確認" -ForegroundColor Gray
    Write-Host "4. リージョンが正しいことを確認" -ForegroundColor Gray
    Write-Host "5. DynamoDBテーブル名の競合がないか確認" -ForegroundColor Gray
    Write-Host ""
    Write-Host "ログ確認:" -ForegroundColor Yellow
    Write-Host "  CloudFormation: https://console.aws.amazon.com/cloudformation/" -ForegroundColor Gray
    Write-Host "  Lambda Logs: https://console.aws.amazon.com/cloudwatch/home#logsV2:log-groups/log-group/%252Faws%252Flambda%252Fteam-dashboard-$Environment" -ForegroundColor Gray
    
    exit 1
}

Write-Host ""
Write-Host "使用方法:" -ForegroundColor Yellow
Write-Host "  基本デプロイ: .\deploy-lambda.ps1 -Environment dev" -ForegroundColor Gray
Write-Host "  本番デプロイ: .\deploy-lambda.ps1 -Environment prod" -ForegroundColor Gray
Write-Host "  ガイド付き: .\deploy-lambda.ps1 -Guided" -ForegroundColor Gray
Write-Host "  ビルドスキップ: .\deploy-lambda.ps1 -SkipBuild" -ForegroundColor Gray
Write-Host "  テスト実行: .\deploy-lambda.ps1 -SkipTests:$false" -ForegroundColor Gray