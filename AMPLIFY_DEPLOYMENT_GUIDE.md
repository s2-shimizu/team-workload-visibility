# AWS Amplify デプロイメントガイド

## 🚀 GitHub連携によるAmplifyデプロイ手順

### 前提条件
- AWSアカウントの準備
- GitHubリポジトリの作成
- AWS CLIの設定（推奨）

## 📝 ステップ1: GitHubリポジトリの準備

### 1.1 リポジトリの作成と初期化
```bash
# Gitリポジトリの初期化（まだの場合）
git init

# .gitignoreファイルの作成
echo "node_modules/
target/
*.log
.env
.DS_Store
*.jar
!mvnw.jar" > .gitignore

# 初回コミット
git add .
git commit -m "Initial commit: Team Dashboard Application"

# GitHubリポジトリとの連携
git remote add origin https://github.com/YOUR_USERNAME/team-dashboard.git
git branch -M main
git push -u origin main
```

### 1.2 必要なファイルの確認
- ✅ `amplify.yml` - ビルド設定
- ✅ `frontend/package.json` - フロントエンドビルド設定
- ✅ `backend/pom.xml` - バックエンドビルド設定

## 🔧 ステップ2: AWS Amplifyコンソールでのセットアップ

### 2.1 Amplifyアプリケーションの作成

1. **AWS Amplifyコンソールにアクセス**
   - https://console.aws.amazon.com/amplify/

2. **新しいアプリケーションの作成**
   ```
   「Host your web app」を選択
   → 「GitHub」を選択
   → GitHubアカウントと連携
   ```

3. **リポジトリの選択**
   ```
   リポジトリ: team-dashboard
   ブランチ: main
   ```

### 2.2 ビルド設定の確認

Amplifyが自動検出した設定を確認し、必要に応じて調整：

```yaml
version: 1
applications:
  - frontend:
      phases:
        preBuild:
          commands:
            - echo "Installing frontend dependencies"
            - cd frontend
            - npm install
        build:
          commands:
            - echo "Building frontend application"
            - npm run build
      artifacts:
        baseDirectory: frontend/build
        files:
          - '**/*'
      cache:
        paths:
          - frontend/node_modules/**/*
    appRoot: frontend
  - backend:
      phases:
        preBuild:
          commands:
            - echo "Installing Java dependencies"
            - cd backend
        build:
          commands:
            - echo "Building Lambda function"
            - ./mvnw clean package -DskipTests
            - mkdir -p ../amplify/backend/function/teamDashboardApi/src/
            - cp target/team-dashboard-backend-*.jar ../amplify/backend/function/teamDashboardApi/src/
      artifacts:
        baseDirectory: amplify/backend/function/teamDashboardApi/src
        files:
          - '**/*'
    appRoot: backend
```

### 2.3 環境変数の設定

**重要**: 本番環境用の環境変数を設定

```
Environment variables:
- NODE_ENV=production
- AWS_REGION=ap-northeast-1
- DYNAMODB_TABLE_NAME=TeamDashboard
```

## ⚙️ ステップ3: バックエンドリソースの設定

### 3.1 DynamoDBテーブルの作成

AWS CLIまたはコンソールでDynamoDBテーブルを作成：

```bash
# AWS CLIでの作成例
aws dynamodb create-table \
    --table-name TeamDashboard \
    --attribute-definitions \
        AttributeName=PK,AttributeType=S \
        AttributeName=SK,AttributeType=S \
    --key-schema \
        AttributeName=PK,KeyType=HASH \
        AttributeName=SK,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --region ap-northeast-1
```

### 3.2 Lambda関数の設定

Amplifyが自動的にLambda関数を作成しますが、以下の設定を確認：

```
Runtime: Java 17
Handler: com.teamdashboard.LambdaHandler::handleRequest
Memory: 512MB
Timeout: 30秒
Environment Variables:
- SPRING_PROFILES_ACTIVE=lambda
- AWS_REGION=ap-northeast-1
```

### 3.3 API Gatewayの設定

```
API Type: REST API
Integration: Lambda Proxy Integration
CORS: 有効化
Authorization: AWS_IAM または Cognito（認証実装後）
```

## 🔐 ステップ4: 認証設定（Cognito）

### 4.1 Cognito User Poolの作成

```bash
# AWS CLIでの作成例
aws cognito-idp create-user-pool \
    --pool-name TeamDashboardUserPool \
    --policies PasswordPolicy='{MinimumLength=8,RequireUppercase=true,RequireLowercase=true,RequireNumbers=true}' \
    --region ap-northeast-1
```

### 4.2 Cognito App Clientの作成

```bash
aws cognito-idp create-user-pool-client \
    --user-pool-id us-west-2_XXXXXXXXX \
    --client-name TeamDashboardWebClient \
    --generate-secret \
    --explicit-auth-flows ADMIN_NO_SRP_AUTH USER_PASSWORD_AUTH
```

## 🚀 ステップ5: デプロイの実行

### 5.1 自動デプロイの開始

1. **Amplifyコンソールで「Deploy」をクリック**
2. **ビルドプロセスの監視**
   ```
   Provision → Build → Deploy → Verify
   ```

### 5.2 デプロイ状況の確認

```bash
# AWS CLIでデプロイ状況確認
aws amplify list-apps
aws amplify get-app --app-id YOUR_APP_ID
```

## 🔍 ステップ6: デプロイ後の確認

### 6.1 フロントエンドの動作確認

1. **Amplifyが提供するURLにアクセス**
   ```
   https://main.XXXXXXXXXX.amplifyapp.com
   ```

2. **基本機能のテスト**
   - ページの読み込み
   - タブ切り替え
   - API接続（エラーが出ても正常）

### 6.2 バックエンドAPIの確認

```bash
# API Gatewayエンドポイントのテスト
curl -X GET https://YOUR_API_ID.execute-api.ap-northeast-1.amazonaws.com/prod/api/workload-status
```

### 6.3 ログの確認

```bash
# Lambda関数のログ確認
aws logs describe-log-groups --log-group-name-prefix /aws/lambda/
aws logs get-log-events --log-group-name /aws/lambda/YOUR_FUNCTION_NAME --log-stream-name LATEST
```

## 🔧 トラブルシューティング

### よくある問題と解決方法

1. **ビルドエラー: Maven not found**
   ```yaml
   # amplify.ymlのpreBuildに追加
   - yum install -y maven
   ```

2. **Java 17 not available**
   ```yaml
   # amplify.ymlのpreBuildに追加
   - export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
   ```

3. **DynamoDB Access Denied**
   ```
   Lambda実行ロールにDynamoDBアクセス権限を追加
   ```

4. **CORS エラー**
   ```
   API GatewayでCORSを有効化
   ```

## 📊 監視とメンテナンス

### 6.1 CloudWatchでの監視設定

```bash
# アラームの設定例
aws cloudwatch put-metric-alarm \
    --alarm-name "TeamDashboard-HighErrorRate" \
    --alarm-description "High error rate detected" \
    --metric-name Errors \
    --namespace AWS/Lambda \
    --statistic Sum \
    --period 300 \
    --threshold 10 \
    --comparison-operator GreaterThanThreshold
```

### 6.2 自動デプロイの設定

```
GitHub Actions または Amplify Console での
継続的デプロイメント設定
```

## 🎯 次のステップ

1. **カスタムドメインの設定**
2. **SSL証明書の設定**
3. **パフォーマンス最適化**
4. **セキュリティ強化**
5. **監視・アラート設定**

---

## 📞 サポート情報

- AWS Amplify Documentation: https://docs.amplify.aws/
- AWS Lambda Java: https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html
- Spring Boot on Lambda: https://github.com/awslabs/aws-serverless-java-container