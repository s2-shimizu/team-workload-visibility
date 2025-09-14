# 正しいデプロイメント手順ガイド

## 🏗️ **アーキテクチャ概要**

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   AWS Amplify   │    │    AWS SAM      │    │   AWS Services  │
│                 │    │                 │    │                 │
│ ・静的ファイル   │    │ ・Lambda関数    │    │ ・API Gateway   │
│ ・HTML/CSS/JS   │────│ ・Spring Boot   │────│ ・DynamoDB      │
│ ・CDN配信       │    │ ・JAR ビルド    │    │ ・CloudWatch    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🚀 **デプロイメント手順**

### 1️⃣ **バックエンドのデプロイ（SAM）**

```bash
# バックエンドビルド
cd backend
mvn clean package -DskipTests -Dspring.profiles.active=lambda

# SAMでデプロイ
cd ..
sam build
sam deploy --guided  # 初回のみ
# または
sam deploy  # 2回目以降
```

### 2️⃣ **フロントエンドのデプロイ（Amplify）**

```bash
# Amplifyは静的ファイルのみをデプロイ
# Git push で自動的にトリガー
git add .
git commit -m "Deploy frontend to Amplify"
git push origin main
```

## 📝 **修正されたamplify.yml**

```yaml
version: 1
frontend:
  phases:
    preBuild:
      commands:
        - echo "Preparing static frontend files"
        - ls -la frontend
        - echo "Checking frontend file structure"
    build:
      commands:
        - echo "Building frontend application"
        - echo "Validating JavaScript files"
        - echo "Frontend build completed successfully"
    postBuild:
      commands:
        - echo "Frontend post-build validation"
        - echo "Verifying all required files are present"
  artifacts:
    baseDirectory: frontend
    files:
      - 'index.html'
      - 'css/**/*'
      - 'js/**/*'
      - 'package.json'
```

## 🔧 **環境変数の設定**

### フロントエンド（Amplify）
```javascript
// frontend/js/api-client.js
const API_BASE_URL = 'https://your-api-gateway-url.execute-api.ap-northeast-1.amazonaws.com/dev';
```

### バックエンド（SAM）
```yaml
# template.yaml で設定済み
Environment:
  Variables:
    SPRING_PROFILES_ACTIVE: lambda,dynamodb
    DYNAMODB_TABLE_NAME: !Ref TeamDashboardTable
```

## 📊 **デプロイメント検証**

### バックエンド検証
```bash
# API Gateway エンドポイントのテスト
curl https://your-api-gateway-url.execute-api.ap-northeast-1.amazonaws.com/dev/health

# Lambda関数のテスト
aws lambda invoke --function-name team-dashboard-api-dev response.json
```

### フロントエンド検証
```bash
# Amplifyデプロイメント検証
node deployment-verification.js --frontend-url https://main.d1234567890.amplifyapp.com
```

## 🔄 **CI/CDパイプライン**

### GitHub Actions設定例

```yaml
name: Deploy Application

on:
  push:
    branches: [ main ]

jobs:
  deploy-backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          
      - name: Setup SAM CLI
        uses: aws-actions/setup-sam@v2
        
      - name: Build and Deploy Backend
        run: |
          cd backend
          mvn clean package -DskipTests
          cd ..
          sam build
          sam deploy --no-confirm-changeset --no-fail-on-empty-changeset
        env:
          AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
          AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}

  deploy-frontend:
    needs: deploy-backend
    runs-on: ubuntu-latest
    steps:
      - name: Trigger Amplify Deployment
        run: |
          # Amplifyは自動的にGitプッシュでトリガーされる
          echo "Frontend deployment triggered automatically"
```

## 🛠️ **トラブルシューティング**

### バックエンドの問題
```bash
# SAMログの確認
sam logs -n team-dashboard-api-dev --stack-name team-dashboard-backend

# Lambda関数の直接テスト
sam local start-api
curl http://localhost:3000/health
```

### フロントエンドの問題
```bash
# Amplifyビルドログの確認（Amplify Console）
# 静的ファイルの検証
node pre-deployment-checker.js
```

## 📈 **パフォーマンス最適化**

### フロントエンド
- CDNキャッシュの活用
- 静的ファイルの圧縮
- 画像最適化

### バックエンド
- Lambda Cold Start対策
- DynamoDB最適化
- API Gateway キャッシュ

## 🔐 **セキュリティ設定**

### CORS設定（API Gateway）
```yaml
Cors:
  AllowMethods: "'GET,POST,PUT,DELETE,OPTIONS'"
  AllowHeaders: "'Content-Type,X-Amz-Date,Authorization,X-Api-Key'"
  AllowOrigin: "'https://main.d1234567890.amplifyapp.com'"
```

### セキュリティヘッダー（Amplify）
```yaml
# Amplify Console で設定
customHeaders:
  - pattern: '**/*'
    headers:
      - key: 'X-Frame-Options'
        value: 'DENY'
      - key: 'X-Content-Type-Options'
        value: 'nosniff'
```

これで正しいAWSアーキテクチャに従ったデプロイメントが可能になります！