# DynamoDBアクセス権限設定手順

## 概要

Team DashboardアプリケーションでDynamoDBに安全にアクセスするための権限設定手順を説明します。AWS Lambda、API Gateway、およびローカル開発環境での適切な権限設定を含みます。

## 1. DynamoDBテーブル設計

### テーブル構造

```yaml
TableName: TeamDashboard-{Environment}
PartitionKey: PK (String)
SortKey: SK (String)
GlobalSecondaryIndexes:
  - GSI1:
      PartitionKey: GSI1PK (String)
      SortKey: GSI1SK (String)
```

### データアクセスパターン

| エンティティ | PK | SK | GSI1PK | GSI1SK |
|-------------|----|----|--------|--------|
| User | USER#{userId} | PROFILE | USER#{userId} | {createdAt} |
| Team | TEAM#{teamId} | METADATA | TEAM#{teamId} | {createdAt} |
| Task | TEAM#{teamId} | TASK#{taskId} | USER#{assigneeId} | TASK#{dueDate} |
| Project | TEAM#{teamId} | PROJECT#{projectId} | PROJECT#{status} | {priority} |

## 2. IAMロールとポリシー設定

### Lambda実行ロール

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DynamoDBTableAccess",
      "Effect": "Allow",
      "Action": [
        "dynamodb:GetItem",
        "dynamodb:PutItem",
        "dynamodb:UpdateItem",
        "dynamodb:DeleteItem",
        "dynamodb:Query",
        "dynamodb:Scan",
        "dynamodb:BatchGetItem",
        "dynamodb:BatchWriteItem"
      ],
      "Resource": [
        "arn:aws:dynamodb:${AWS::Region}:${AWS::AccountId}:table/TeamDashboard-${Environment}",
        "arn:aws:dynamodb:${AWS::Region}:${AWS::AccountId}:table/TeamDashboard-${Environment}/index/*"
      ]
    },
    {
      "Sid": "DynamoDBStreamAccess",
      "Effect": "Allow",
      "Action": [
        "dynamodb:DescribeStream",
        "dynamodb:GetRecords",
        "dynamodb:GetShardIterator",
        "dynamodb:ListStreams"
      ],
      "Resource": "arn:aws:dynamodb:${AWS::Region}:${AWS::AccountId}:table/TeamDashboard-${Environment}/stream/*"
    },
    {
      "Sid": "CloudWatchLogsAccess",
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:${AWS::Region}:${AWS::AccountId}:log-group:/aws/lambda/team-dashboard-api-${Environment}:*"
    }
  ]
}
```

### CloudFormationテンプレートでの実装

```yaml
# Lambda実行ロール
LambdaExecutionRole:
  Type: AWS::IAM::Role
  Properties:
    RoleName: !Sub 'TeamDashboard-Lambda-${Environment}-Role'
    AssumeRolePolicyDocument:
      Version: '2012-10-17'
      Statement:
        - Effect: Allow
          Principal:
            Service: lambda.amazonaws.com
          Action: sts:AssumeRole
    ManagedPolicyArns:
      - arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
    Policies:
      - PolicyName: DynamoDBAccess
        PolicyDocument:
          Version: '2012-10-17'
          Statement:
            - Effect: Allow
              Action:
                - dynamodb:GetItem
                - dynamodb:PutItem
                - dynamodb:UpdateItem
                - dynamodb:DeleteItem
                - dynamodb:Query
                - dynamodb:Scan
                - dynamodb:BatchGetItem
                - dynamodb:BatchWriteItem
              Resource:
                - !GetAtt TeamDashboardTable.Arn
                - !Sub '${TeamDashboardTable.Arn}/index/*'
            - Effect: Allow
              Action:
                - dynamodb:DescribeStream
                - dynamodb:GetRecords
                - dynamodb:GetShardIterator
                - dynamodb:ListStreams
              Resource: !Sub '${TeamDashboardTable.Arn}/stream/*'
```

## 3. 環境別権限設定

### 開発環境 (dev)

```bash
# 開発環境用IAMポリシー
aws iam create-policy \
  --policy-name TeamDashboard-Dev-DynamoDB-Policy \
  --policy-document '{
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Action": [
          "dynamodb:*"
        ],
        "Resource": [
          "arn:aws:dynamodb:ap-northeast-1:*:table/TeamDashboard-dev",
          "arn:aws:dynamodb:ap-northeast-1:*:table/TeamDashboard-dev/index/*"
        ]
      }
    ]
  }'
```

### ステージング環境 (staging)

```bash
# ステージング環境用IAMポリシー（読み取り専用制限あり）
aws iam create-policy \
  --policy-name TeamDashboard-Staging-DynamoDB-Policy \
  --policy-document '{
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Action": [
          "dynamodb:GetItem",
          "dynamodb:PutItem",
          "dynamodb:UpdateItem",
          "dynamodb:DeleteItem",
          "dynamodb:Query",
          "dynamodb:Scan",
          "dynamodb:BatchGetItem",
          "dynamodb:BatchWriteItem"
        ],
        "Resource": [
          "arn:aws:dynamodb:ap-northeast-1:*:table/TeamDashboard-staging",
          "arn:aws:dynamodb:ap-northeast-1:*:table/TeamDashboard-staging/index/*"
        ]
      }
    ]
  }'
```

### 本番環境 (prod)

```bash
# 本番環境用IAMポリシー（最小権限の原則）
aws iam create-policy \
  --policy-name TeamDashboard-Prod-DynamoDB-Policy \
  --policy-document '{
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Action": [
          "dynamodb:GetItem",
          "dynamodb:PutItem",
          "dynamodb:UpdateItem",
          "dynamodb:Query",
          "dynamodb:BatchGetItem"
        ],
        "Resource": [
          "arn:aws:dynamodb:ap-northeast-1:*:table/TeamDashboard-prod",
          "arn:aws:dynamodb:ap-northeast-1:*:table/TeamDashboard-prod/index/*"
        ],
        "Condition": {
          "ForAllValues:StringEquals": {
            "dynamodb:Attributes": [
              "PK",
              "SK",
              "GSI1PK",
              "GSI1SK",
              "userId",
              "teamId",
              "taskId",
              "projectId",
              "status",
              "createdAt",
              "updatedAt"
            ]
          }
        }
      },
      {
        "Effect": "Allow",
        "Action": [
          "dynamodb:DeleteItem"
        ],
        "Resource": [
          "arn:aws:dynamodb:ap-northeast-1:*:table/TeamDashboard-prod"
        ],
        "Condition": {
          "StringEquals": {
            "dynamodb:LeadingKeys": ["USER#${aws:userid}"]
          }
        }
      }
    ]
  }'
```

## 4. セキュリティベストプラクティス

### 最小権限の原則

```yaml
# 機能別権限分離
UserManagementPolicy:
  Version: '2012-10-17'
  Statement:
    - Effect: Allow
      Action:
        - dynamodb:GetItem
        - dynamodb:PutItem
        - dynamodb:UpdateItem
      Resource: !GetAtt TeamDashboardTable.Arn
      Condition:
        ForAllValues:StringLike:
          dynamodb:LeadingKeys:
            - 'USER#*'

TeamManagementPolicy:
  Version: '2012-10-17'
  Statement:
    - Effect: Allow
      Action:
        - dynamodb:GetItem
        - dynamodb:PutItem
        - dynamodb:UpdateItem
        - dynamodb:Query
      Resource: 
        - !GetAtt TeamDashboardTable.Arn
        - !Sub '${TeamDashboardTable.Arn}/index/*'
      Condition:
        ForAllValues:StringLike:
          dynamodb:LeadingKeys:
            - 'TEAM#*'
```

### 条件付きアクセス制御

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:GetItem",
        "dynamodb:Query"
      ],
      "Resource": "arn:aws:dynamodb:region:account:table/TeamDashboard-prod",
      "Condition": {
        "ForAllValues:StringEquals": {
          "dynamodb:Select": ["SpecificAttributes"],
          "dynamodb:Attributes": [
            "PK",
            "SK",
            "userId",
            "teamId",
            "status",
            "createdAt"
          ]
        }
      }
    }
  ]
}
```

## 5. ローカル開発環境設定

### DynamoDB Local設定

```bash
# DynamoDB Localの起動
docker run -p 8000:8000 amazon/dynamodb-local

# テーブル作成スクリプト
aws dynamodb create-table \
  --table-name TeamDashboard-local \
  --attribute-definitions \
    AttributeName=PK,AttributeType=S \
    AttributeName=SK,AttributeType=S \
    AttributeName=GSI1PK,AttributeType=S \
    AttributeName=GSI1SK,AttributeType=S \
  --key-schema \
    AttributeName=PK,KeyType=HASH \
    AttributeName=SK,KeyType=RANGE \
  --global-secondary-indexes \
    IndexName=GSI1,KeySchema=[{AttributeName=GSI1PK,KeyType=HASH},{AttributeName=GSI1SK,KeyType=RANGE}],Projection={ProjectionType=ALL},ProvisionedThroughput={ReadCapacityUnits=5,WriteCapacityUnits=5} \
  --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
  --endpoint-url http://localhost:8000
```

### ローカル開発用IAMユーザー

```bash
# 開発用IAMユーザー作成
aws iam create-user --user-name team-dashboard-dev-user

# ポリシーアタッチ
aws iam attach-user-policy \
  --user-name team-dashboard-dev-user \
  --policy-arn arn:aws:iam::account:policy/TeamDashboard-Dev-DynamoDB-Policy

# アクセスキー作成
aws iam create-access-key --user-name team-dashboard-dev-user
```

### 環境変数設定

```bash
# ローカル開発用環境変数
export AWS_REGION=ap-northeast-1
export DYNAMODB_ENDPOINT=http://localhost:8000
export DYNAMODB_TABLE_NAME=TeamDashboard-local
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
```

## 6. 権限テストスクリプト

### DynamoDB権限テストスクリプト

```javascript
// test-dynamodb-permissions.js
const { DynamoDBClient } = require('@aws-sdk/client-dynamodb');
const { DynamoDBDocumentClient, PutCommand, GetCommand, QueryCommand } = require('@aws-sdk/lib-dynamodb');

const client = new DynamoDBClient({
  region: process.env.AWS_REGION || 'ap-northeast-1',
  endpoint: process.env.DYNAMODB_ENDPOINT
});

const docClient = DynamoDBDocumentClient.from(client);
const tableName = process.env.DYNAMODB_TABLE_NAME || 'TeamDashboard-dev';

async function testPermissions() {
  console.log('=== DynamoDB権限テスト開始 ===');
  
  try {
    // 1. PutItem テスト
    console.log('1. PutItem テスト...');
    const putResult = await docClient.send(new PutCommand({
      TableName: tableName,
      Item: {
        PK: 'USER#test-user-001',
        SK: 'PROFILE',
        userId: 'test-user-001',
        name: 'Test User',
        email: 'test@example.com',
        createdAt: new Date().toISOString()
      }
    }));
    console.log('✅ PutItem 成功');
    
    // 2. GetItem テスト
    console.log('2. GetItem テスト...');
    const getResult = await docClient.send(new GetCommand({
      TableName: tableName,
      Key: {
        PK: 'USER#test-user-001',
        SK: 'PROFILE'
      }
    }));
    console.log('✅ GetItem 成功:', getResult.Item);
    
    // 3. Query テスト
    console.log('3. Query テスト...');
    const queryResult = await docClient.send(new QueryCommand({
      TableName: tableName,
      KeyConditionExpression: 'PK = :pk',
      ExpressionAttributeValues: {
        ':pk': 'USER#test-user-001'
      }
    }));
    console.log('✅ Query 成功:', queryResult.Items.length, '件');
    
    // 4. GSI Query テスト
    console.log('4. GSI Query テスト...');
    const gsiQueryResult = await docClient.send(new QueryCommand({
      TableName: tableName,
      IndexName: 'GSI1',
      KeyConditionExpression: 'GSI1PK = :gsi1pk',
      ExpressionAttributeValues: {
        ':gsi1pk': 'USER#test-user-001'
      }
    }));
    console.log('✅ GSI Query 成功:', gsiQueryResult.Items.length, '件');
    
    console.log('=== すべてのテストが成功しました ===');
    
  } catch (error) {
    console.error('❌ テスト失敗:', error.message);
    
    // エラー種別の判定
    if (error.name === 'AccessDeniedException') {
      console.error('権限エラー: IAMポリシーを確認してください');
    } else if (error.name === 'ResourceNotFoundException') {
      console.error('リソースエラー: テーブルが存在しません');
    } else if (error.name === 'ValidationException') {
      console.error('バリデーションエラー: リクエストパラメータを確認してください');
    }
    
    process.exit(1);
  }
}

// テスト実行
testPermissions();
```

### 権限テスト実行スクリプト

```bash
#!/bin/bash
# run-permission-tests.sh

echo "=== DynamoDB権限テスト実行 ==="

# 環境変数チェック
if [ -z "$DYNAMODB_TABLE_NAME" ]; then
    echo "❌ DYNAMODB_TABLE_NAME が設定されていません"
    exit 1
fi

if [ -z "$AWS_REGION" ]; then
    echo "❌ AWS_REGION が設定されていません"
    exit 1
fi

echo "テーブル名: $DYNAMODB_TABLE_NAME"
echo "リージョン: $AWS_REGION"

# Node.js依存関係インストール
if [ ! -d "node_modules" ]; then
    echo "依存関係をインストール中..."
    npm install @aws-sdk/client-dynamodb @aws-sdk/lib-dynamodb
fi

# テスト実行
echo "権限テストを実行中..."
node test-dynamodb-permissions.js

if [ $? -eq 0 ]; then
    echo "✅ すべての権限テストが成功しました"
else
    echo "❌ 権限テストが失敗しました"
    exit 1
fi
```

## 7. 監視とアラート設定

### CloudWatch メトリクス監視

```yaml
# DynamoDB監視アラーム
DynamoDBThrottleAlarm:
  Type: AWS::CloudWatch::Alarm
  Properties:
    AlarmName: !Sub 'TeamDashboard-${Environment}-DynamoDB-Throttles'
    AlarmDescription: 'DynamoDB throttling alarm'
    MetricName: UserErrors
    Namespace: AWS/DynamoDB
    Statistic: Sum
    Period: 300
    EvaluationPeriods: 2
    Threshold: 5
    ComparisonOperator: GreaterThanThreshold
    Dimensions:
      - Name: TableName
        Value: !Ref TeamDashboardTable

DynamoDBErrorAlarm:
  Type: AWS::CloudWatch::Alarm
  Properties:
    AlarmName: !Sub 'TeamDashboard-${Environment}-DynamoDB-Errors'
    AlarmDescription: 'DynamoDB system errors alarm'
    MetricName: SystemErrors
    Namespace: AWS/DynamoDB
    Statistic: Sum
    Period: 300
    EvaluationPeriods: 1
    Threshold: 1
    ComparisonOperator: GreaterThanThreshold
    Dimensions:
      - Name: TableName
        Value: !Ref TeamDashboardTable
```

### アクセスログ監視

```json
{
  "filterPattern": "[timestamp, request_id, level=\"ERROR\", message=\"*AccessDenied*\"]",
  "logGroupName": "/aws/lambda/team-dashboard-api-dev",
  "metricTransformations": [
    {
      "metricName": "DynamoDBAccessDeniedErrors",
      "metricNamespace": "TeamDashboard/Security",
      "metricValue": "1"
    }
  ]
}
```

## 8. トラブルシューティング

### よくある権限エラー

1. **AccessDeniedException**
   ```
   原因: IAMポリシーで必要なアクションが許可されていない
   解決: IAMポリシーを確認し、必要なアクションを追加
   ```

2. **ResourceNotFoundException**
   ```
   原因: テーブルまたはインデックスが存在しない
   解決: テーブル名とリージョンを確認
   ```

3. **ValidationException**
   ```
   原因: リクエストパラメータが不正
   解決: キー属性とデータ型を確認
   ```

### デバッグ手順

```bash
# 1. IAMロールの確認
aws sts get-caller-identity

# 2. テーブルの存在確認
aws dynamodb describe-table --table-name TeamDashboard-dev

# 3. IAMポリシーシミュレーション
aws iam simulate-principal-policy \
  --policy-source-arn arn:aws:iam::account:role/TeamDashboard-Lambda-dev-Role \
  --action-names dynamodb:GetItem \
  --resource-arns arn:aws:dynamodb:ap-northeast-1:account:table/TeamDashboard-dev

# 4. CloudWatchログの確認
aws logs filter-log-events \
  --log-group-name /aws/lambda/team-dashboard-api-dev \
  --filter-pattern "ERROR"
```

この設定手順に従うことで、Team DashboardアプリケーションのDynamoDBアクセス権限を適切に設定し、セキュアで効率的なデータアクセスを実現できます。