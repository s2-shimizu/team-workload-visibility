# 🚀 Lambda デプロイガイド

AWS Lambdaを使用したサーバーレスデプロイの完全ガイドです。WebSocket機能は制限されますが、ポーリング更新で動作する軽量なアプリケーションを構築できます。

## 📋 Lambda デプロイの特徴

### ✅ メリット
- **低コスト**: 使用した分だけ課金
- **自動スケーリング**: トラフィックに応じて自動調整
- **サーバー管理不要**: インフラ管理が不要
- **高可用性**: AWSが可用性を保証

### ⚠️ 制限事項
- **WebSocket制限**: リアルタイム更新は利用不可
- **実行時間制限**: 最大15分
- **メモリ制限**: 最大10GB
- **コールドスタート**: 初回実行時の遅延

### 🔄 更新方式
- **ポーリング更新**: 30秒間隔でデータ取得
- **手動更新**: ユーザーが🔄ボタンで即座更新
- **自動フォールバック**: WebSocket失敗時に自動切り替え

---

## 🛠️ 前提条件

### 必要なツール
- **AWS CLI**: 最新版
- **SAM CLI**: AWS Serverless Application Model
- **Java 17**: OpenJDK推奨
- **Maven**: 3.6以上

### AWS設定
- AWS認証情報設定済み
- 適切なIAM権限
- DynamoDBアクセス権限

---

## 🚀 クイックスタート

### 1分でLambdaデプロイ
```powershell
# ワンコマンドデプロイ
.\quick-deploy.ps1 -DeployType lambda -Environment dev

# または既存のSAMスクリプト
.\deploy-sam-stack.ps1 -Environment dev
```

---

## 📝 詳細デプロイ手順

### Step 1: 前提条件確認
```powershell
# AWS CLI確認
aws --version

# SAM CLI確認
sam --version

# AWS認証確認
aws sts get-caller-identity
```

### Step 2: DynamoDBテーブル作成
```powershell
# 開発環境用テーブル
.\create-dynamodb-tables.ps1 -Environment dev

# 本番環境用テーブル
.\create-production-tables.ps1 -Environment prod
```

### Step 3: Lambdaビルド・デプロイ
```powershell
# 開発環境デプロイ
.\deploy-sam-stack.ps1 -Environment dev

# 本番環境デプロイ
.\deploy-sam-stack.ps1 -Environment prod

# ガイド付きデプロイ（初回推奨）
.\deploy-sam-stack.ps1 -Guided
```

### Step 4: デプロイ確認
```powershell
# API動作確認
curl https://your-api-gateway-url/api/status

# ポーリング更新テスト
.\test-polling-updates.ps1 -BaseUrl "https://your-api-gateway-url"
```

---

## 🏗️ Lambda アーキテクチャ

```
Internet → API Gateway → Lambda → DynamoDB
    ↓           ↓          ↓         ↓
CloudFront  認証・CORS   ビジネス   データ永続化
                        ロジック
```

### コンポーネント詳細

#### **API Gateway**
- RESTful API エンドポイント
- CORS設定
- 認証・認可
- レート制限

#### **Lambda Function**
- Spring Boot アプリケーション
- DynamoDB統合
- ビジネスロジック処理
- エラーハンドリング

#### **DynamoDB**
- NoSQLデータベース
- 自動スケーリング
- 暗号化対応
- バックアップ機能

---

## 📊 SAMテンプレート構成

### template.yaml の主要設定
```yaml
AWSTemplateFormatVersion: '2010-09-09'
Transform: AWS::Serverless-2016-10-31

Globals:
  Function:
    Timeout: 30
    MemorySize: 512
    Runtime: java17

Resources:
  TeamDashboardFunction:
    Type: AWS::Serverless::Function
    Properties:
      CodeUri: backend/
      Handler: com.teamdashboard.LambdaHandler::handleRequest
      Environment:
        Variables:
          SPRING_PROFILES_ACTIVE: lambda,dynamodb
          WORKLOAD_STATUS_TABLE: !Ref WorkloadStatusTable
          TEAM_ISSUE_TABLE: !Ref TeamIssueTable
      Events:
        Api:
          Type: Api
          Properties:
            Path: /{proxy+}
            Method: ANY
```

---

## 🔧 Lambda 最適化設定

### JVM設定
```bash
# メモリ最適化
-Xmx400m -XX:+UseG1GC -XX:MaxGCPauseMillis=100

# コールドスタート最適化
-XX:+TieredCompilation -XX:TieredStopAtLevel=1
```

### Spring Boot設定
```properties
# application-lambda.properties
spring.main.lazy-initialization=true
spring.jpa.open-in-view=false
spring.servlet.multipart.enabled=false
logging.level.org.springframework=WARN
```

### Lambda固有設定
```java
// LambdaHandler.java
@Component
public class LambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private static SpringLambdaContainerHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> handler;
    
    static {
        try {
            handler = SpringLambdaContainerHandler.getAwsProxyHandler(LambdaApplication.class);
            handler.activateSpringProfiles("lambda");
        } catch (ContainerInitializationException e) {
            throw new RuntimeException("Could not initialize Spring Boot application", e);
        }
    }
}
```

---

## 📈 パフォーマンス最適化

### コールドスタート対策
1. **Provisioned Concurrency**: 本番環境で設定
2. **レイヤー使用**: 共通ライブラリを分離
3. **メモリ調整**: 適切なメモリサイズ設定
4. **依存関係最小化**: 不要なライブラリ除去

### メモリ・タイムアウト設定
```yaml
# 推奨設定
MemorySize: 512  # 開発環境
MemorySize: 1024 # 本番環境
Timeout: 30      # API Gateway制限
```

---

## 🔒 セキュリティ設定

### IAM権限（最小権限）
```json
{
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
        "dynamodb:Scan"
      ],
      "Resource": [
        "arn:aws:dynamodb:region:account:table/WorkloadStatus-*",
        "arn:aws:dynamodb:region:account:table/TeamIssue-*"
      ]
    }
  ]
}
```

### 環境変数暗号化
```yaml
Environment:
  Variables:
    SPRING_PROFILES_ACTIVE: lambda
    # KMS暗号化対応
    DB_PASSWORD: 
      Ref: EncryptedPassword
```

---

## 📊 監視・ログ

### CloudWatch監視項目
- **実行時間**: 平均・最大実行時間
- **エラー率**: 失敗率の監視
- **同時実行数**: スロットリング監視
- **メモリ使用量**: メモリ最適化

### ログ設定
```properties
# CloudWatch Logs最適化
logging.level.com.teamdashboard=INFO
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
```

### アラート設定
```yaml
HighErrorRateAlarm:
  Type: AWS::CloudWatch::Alarm
  Properties:
    AlarmName: !Sub "${AWS::StackName}-HighErrorRate"
    MetricName: Errors
    Namespace: AWS/Lambda
    Statistic: Sum
    Period: 300
    EvaluationPeriods: 2
    Threshold: 5
```

---

## 🧪 テスト・検証

### 統合テスト
```powershell
# Lambda固有テスト
.\test-lambda-deployment.ps1 -ApiEndpoint "https://api-id.execute-api.region.amazonaws.com/stage"

# ポーリング機能テスト
.\test-polling-updates.ps1 -BaseUrl "https://api-id.execute-api.region.amazonaws.com/stage"

# DynamoDB統合テスト
.\simple-dynamodb-test.ps1 -BaseUrl "https://api-id.execute-api.region.amazonaws.com/stage"
```

### 負荷テスト
```bash
# API Gateway負荷テスト
for i in {1..100}; do
  curl -s "https://api-id.execute-api.region.amazonaws.com/stage/api/status" &
done
wait
```

---

## 💰 コスト最適化

### 課金要素
- **実行時間**: ミリ秒単位
- **メモリ使用量**: MB単位
- **リクエスト数**: 100万リクエストまで無料

### コスト削減策
1. **メモリ最適化**: 必要最小限に設定
2. **実行時間短縮**: コード最適化
3. **Provisioned Concurrency**: 必要時のみ使用
4. **DynamoDB On-Demand**: 予測困難な場合

### 月額コスト例
```
# 想定: 10万リクエスト/月、512MB、平均500ms実行
リクエスト料金: $0.20
実行時間料金: $0.83
合計: 約$1.03/月
```

---

## 🔄 CI/CD パイプライン

### GitHub Actions例
```yaml
name: Lambda Deploy
on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Build and Deploy
        run: |
          sam build
          sam deploy --no-confirm-changeset
```

---

## 🚨 トラブルシューティング

### よくある問題

#### 1. コールドスタート遅延
**症状**: 初回リクエストが遅い
**対処**: 
- Provisioned Concurrency設定
- メモリサイズ増加
- 依存関係最小化

#### 2. タイムアウトエラー
**症状**: 30秒でタイムアウト
**対処**:
- 処理の最適化
- 非同期処理の導入
- バッチ処理の分割

#### 3. メモリ不足
**症状**: OutOfMemoryError
**対処**:
- メモリサイズ増加
- JVMヒープ設定調整
- メモリリーク確認

#### 4. DynamoDB接続エラー
**症状**: DynamoDB操作失敗
**対処**:
- IAM権限確認
- VPC設定確認（該当する場合）
- リージョン設定確認

### デバッグ方法
```bash
# CloudWatch Logsでデバッグ
aws logs tail /aws/lambda/function-name --follow

# X-Rayトレーシング有効化
sam deploy --parameter-overrides TracingConfig=Active
```

---

## 📚 参考リソース

### AWS公式ドキュメント
- [AWS Lambda Developer Guide](https://docs.aws.amazon.com/lambda/)
- [SAM Developer Guide](https://docs.aws.amazon.com/serverless-application-model/)
- [API Gateway Developer Guide](https://docs.aws.amazon.com/apigateway/)

### ベストプラクティス
- [Lambda Performance Optimization](https://aws.amazon.com/lambda/performance-optimization/)
- [Serverless Security Best Practices](https://aws.amazon.com/serverless/security-best-practices/)

---

## 🎯 まとめ

### Lambda デプロイの適用場面
- **小〜中規模アプリケーション**
- **不定期なトラフィック**
- **コスト重視**
- **運用負荷軽減**

### 推奨しない場面
- **リアルタイム性が重要**
- **長時間実行処理**
- **大量の同時接続**
- **WebSocket必須**

Lambdaデプロイにより、運用コストを大幅に削減しながら、スケーラブルなアプリケーションを構築できます。ポーリング更新により、WebSocketなしでも実用的なユーザー体験を提供可能です。