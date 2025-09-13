# AWS デプロイメントガイド

## 概要

このガイドでは、チーム状況ダッシュボードをAWS環境にデプロイする手順を説明します。

## アーキテクチャ

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Amplify       │    │   Lambda +       │    │   DynamoDB      │
│   Hosting       │────│   API Gateway    │────│                 │
│  (Frontend)     │    │   (Backend)      │    │   (Database)    │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

## 前提条件

### 必要なツール

1. **AWS CLI**
   ```bash
   # インストール確認
   aws --version
   
   # 設定
   aws configure
   ```

2. **SAM CLI**
   ```bash
   # インストール確認
   sam --version
   ```

3. **Java 17**
   ```bash
   # バージョン確認
   java -version
   ```

4. **Git**（Amplifyデプロイ用）

### AWS アカウント設定

- AWS アカウントの作成
- 適切なIAM権限の設定
- AWS CLIの認証情報設定

## デプロイ手順

### ステップ1: 自動デプロイ（推奨）

```bash
# 自動デプロイスクリプト実行
deploy-aws.bat
```

### ステップ2: 手動デプロイ

#### 2.1 バックエンドデプロイ

```bash
# 1. Mavenビルド
cd backend
mvnw clean package -DskipTests
cd ..

# 2. SAMビルド
sam build

# 3. SAMデプロイ（初回）
sam deploy --guided

# 4. SAMデプロイ（2回目以降）
sam deploy
```

#### 2.2 フロントエンドデプロイ

1. **AWS Amplifyコンソールにアクセス**
   - https://console.aws.amazon.com/amplify/

2. **新しいアプリを作成**
   - 「Host your web app」を選択
   - GitHubリポジトリを接続

3. **ビルド設定**
   - `amplify.yml`が自動検出される
   - 必要に応じて環境変数を設定

4. **デプロイ実行**
   - 自動的にビルド・デプロイが開始される

## 設定

### 環境変数

#### Lambda関数
- `SPRING_PROFILES_ACTIVE`: `lambda,dynamodb`
- `DYNAMODB_TABLE_NAME`: 自動設定

#### フロントエンド（Amplify）
- `AWS_API_URL`: API Gateway URL（自動設定）

### DynamoDB テーブル構造

```
テーブル名: TeamDashboard-{Environment}
パーティションキー: PK (String)
ソートキー: SK (String)
GSI1: GSI1PK, GSI1SK
```

## 料金見積もり

### 月額コスト（東京リージョン）

| サービス | 構成 | 月額コスト |
|---------|------|-----------|
| Amplify Hosting | 標準 | $1-5 |
| Lambda | 512MB, 1000リクエスト/日 | $1-3 |
| API Gateway | REST API | $3-10 |
| DynamoDB | オンデマンド | $1-5 |
| **合計** | | **$6-23** |

### コスト最適化

- **Lambda**: 使用量ベース課金
- **DynamoDB**: オンデマンド課金
- **Amplify**: 無料枠あり（月1GB転送まで）

## 監視・運用

### CloudWatch ログ

```bash
# Lambda関数のログ確認
aws logs describe-log-groups --log-group-name-prefix "/aws/lambda/team-dashboard"

# ログストリーム確認
aws logs describe-log-streams --log-group-name "/aws/lambda/team-dashboard-api-dev"
```

### メトリクス監視

- **Lambda**: 実行時間、エラー率、同時実行数
- **API Gateway**: リクエスト数、レイテンシ、エラー率
- **DynamoDB**: 読み取り/書き込み容量、スロットリング

## トラブルシューティング

### よくある問題

#### 1. Lambda関数のコールドスタート

**症状**: 初回リクエストが遅い

**解決策**:
- Provisioned Concurrencyの設定
- メモリサイズの調整（1024MB推奨）

#### 2. CORS エラー

**症状**: フロントエンドからAPIアクセスできない

**解決策**:
```yaml
# template.yamlのCORS設定確認
Cors:
  AllowMethods: "'GET,POST,PUT,DELETE,OPTIONS'"
  AllowHeaders: "'Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token'"
  AllowOrigin: "'*'"
```

#### 3. DynamoDB アクセスエラー

**症状**: データの読み書きができない

**解決策**:
- IAM権限の確認
- テーブル名の確認
- リージョンの確認

### ログ確認コマンド

```bash
# Lambda関数のログ
sam logs -n TeamDashboardFunction --stack-name team-dashboard --tail

# CloudFormationスタック状態
aws cloudformation describe-stacks --stack-name team-dashboard

# API Gateway テスト
curl https://your-api-id.execute-api.ap-northeast-1.amazonaws.com/dev/workload-status
```

## セキュリティ

### 推奨設定

1. **API Gateway**
   - API キーの設定（オプション）
   - レート制限の設定
   - WAFの設定（本番環境）

2. **Lambda**
   - 最小権限の原則
   - 環境変数の暗号化

3. **DynamoDB**
   - 保存時暗号化の有効化
   - VPCエンドポイント（オプション）

## 本番環境への移行

### 環境分離

```bash
# 開発環境
sam deploy --parameter-overrides Environment=dev

# ステージング環境
sam deploy --parameter-overrides Environment=staging

# 本番環境
sam deploy --parameter-overrides Environment=prod
```

### CI/CD パイプライン

GitHub Actionsを使用した自動デプロイ:

```yaml
# .github/workflows/deploy.yml
name: Deploy to AWS
on:
  push:
    branches: [main]
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: aws-actions/setup-sam@v2
      - run: sam build
      - run: sam deploy --no-confirm-changeset
```

## サポート・リソース

### AWS ドキュメント

- [AWS Lambda](https://docs.aws.amazon.com/lambda/)
- [Amazon API Gateway](https://docs.aws.amazon.com/apigateway/)
- [Amazon DynamoDB](https://docs.aws.amazon.com/dynamodb/)
- [AWS Amplify](https://docs.aws.amazon.com/amplify/)

### コミュニティ

- [AWS SAM GitHub](https://github.com/aws/serverless-application-model)
- [AWS Amplify GitHub](https://github.com/aws-amplify/amplify-js)

## 次のステップ

1. **認証機能の追加**: Amazon Cognito
2. **監視の強化**: X-Ray、CloudWatch Dashboards
3. **パフォーマンス最適化**: Lambda Provisioned Concurrency
4. **セキュリティ強化**: WAF、API Key認証