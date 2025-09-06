# DynamoDB + Amplify 移行ガイド

## 概要

このガイドでは、現在のPostgreSQL + JPA構成からDynamoDB + Amplify構成への移行手順を説明します。

## 移行前の準備

### 1. 必要なツールのインストール

```bash
# Node.js (v16以上)
node --version

# AWS CLI
aws --version

# Amplify CLI
npm install -g @aws-amplify/cli
amplify --version
```

### 2. AWS認証情報の設定

```bash
# AWS認証情報を設定
aws configure

# 確認
aws sts get-caller-identity
```

## 段階的移行手順

### フェーズ1: ローカル開発環境での動作確認

1. **依存関係の更新**
   ```bash
   cd backend
   # pom.xmlが既に更新されているので、依存関係を再取得
   ./mvnw clean compile
   ```

2. **プロファイル設定**
   ```bash
   # DynamoDBプロファイルでアプリケーションを起動
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dynamodb
   ```

3. **フロントエンドの動作確認**
   ```bash
   # ローカルサーバーを起動してテスト
   cd frontend
   # ブラウザでindex.htmlを開いて動作確認
   ```

### フェーズ2: AWS環境の準備

1. **Amplifyプロジェクトの初期化**
   ```bash
   amplify init
   
   # 設定例:
   # Project name: teamdashboard
   # Environment: dev
   # Default editor: Visual Studio Code
   # App type: javascript
   # Framework: none
   # Source directory: frontend
   # Distribution directory: frontend
   # Build command: npm run build
   # Start command: npm start
   ```

2. **認証の追加**
   ```bash
   amplify add auth
   
   # 設定例:
   # Default configuration with username
   # Username
   # No advanced settings
   ```

3. **API（Lambda）の追加**
   ```bash
   amplify add api
   
   # 設定例:
   # REST API
   # New Lambda function
   # teamDashboardApi
   # Java runtime
   # Hello World template
   ```

4. **DynamoDBの追加**
   ```bash
   amplify add storage
   
   # 設定例:
   # NoSQL Database
   # TeamDashboard
   # PK (string), SK (string)
   # GSI1PK, GSI1SK
   ```

5. **ホスティングの追加**
   ```bash
   amplify add hosting
   
   # 設定例:
   # Amazon CloudFront and S3
   # PROD (S3 with CloudFront using HTTPS)
   ```

### フェーズ3: デプロイとテスト

1. **初回デプロイ**
   ```bash
   amplify push
   ```

2. **Lambda関数の更新**
   ```bash
   # バックエンドをビルド
   cd backend
   ./mvnw clean package -DskipTests
   
   # Lambda関数のソースを更新
   cp target/team-dashboard-backend-*.jar ../amplify/backend/function/teamDashboardApi/src/
   
   # 再デプロイ
   amplify push function
   ```

3. **フロントエンドの更新**
   ```bash
   # AWS設定を更新（amplify pushで生成されたaws-exports.jsを使用）
   # frontend/js/aws-config.jsの設定値を更新
   
   amplify publish
   ```

### フェーズ4: データ移行

1. **既存データのエクスポート**
   ```sql
   -- PostgreSQLからデータをエクスポート
   COPY (SELECT * FROM users) TO '/tmp/users.csv' WITH CSV HEADER;
   COPY (SELECT * FROM workload_status) TO '/tmp/workload_status.csv' WITH CSV HEADER;
   COPY (SELECT * FROM team_issues) TO '/tmp/team_issues.csv' WITH CSV HEADER;
   ```

2. **DynamoDBへのデータインポート**
   ```bash
   # データ移行スクリプトを作成・実行
   # （具体的なスクリプトは別途作成）
   node migrate-data.js
   ```

## 設定ファイルの更新

### 1. AWS設定の更新

`frontend/js/aws-config.js`を以下の値で更新:

```javascript
const awsConfig = {
    Auth: {
        region: 'ap-northeast-1',
        userPoolId: 'ap-northeast-1_XXXXXXXXX', // amplify pushで表示される
        userPoolWebClientId: 'XXXXXXXXXXXXXXXXXXXXXXXXXX', // amplify pushで表示される
    },
    API: {
        endpoints: [
            {
                name: "teamDashboardApi",
                endpoint: "https://XXXXXXXXXX.execute-api.ap-northeast-1.amazonaws.com/dev", // amplify pushで表示される
                region: 'ap-northeast-1'
            }
        ]
    }
};
```

### 2. 環境変数の設定

Lambda関数の環境変数:
- `DYNAMODB_TABLE_NAME`: TeamDashboard-dev
- `AWS_REGION`: ap-northeast-1
- `SPRING_PROFILES_ACTIVE`: lambda

## トラブルシューティング

### よくある問題と解決方法

1. **Lambda関数のコールドスタート**
   - 問題: 初回リクエストが遅い
   - 解決: Provisioned Concurrencyの設定、またはGraalVM Nativeコンパイル

2. **DynamoDB接続エラー**
   - 問題: IAM権限不足
   - 解決: Lambda実行ロールにDynamoDBアクセス権限を追加

3. **CORS エラー**
   - 問題: フロントエンドからAPIアクセスできない
   - 解決: API Gatewayでのコントローラーの@CrossOrigin設定確認

4. **認証エラー**
   - 問題: Cognitoトークンが無効
   - 解決: aws-config.jsの設定値確認、トークンの有効期限確認

## パフォーマンス最適化

### 1. Lambda最適化
- メモリサイズ: 1024MB
- タイムアウト: 30秒
- 環境変数: `JAVA_TOOL_OPTIONS=-XX:+TieredCompilation -XX:TieredStopAtLevel=1`

### 2. DynamoDB最適化
- 読み取り一貫性: Eventually Consistent（コスト削減）
- バッチ操作の活用
- GSIの適切な設計

### 3. フロントエンド最適化
- CloudFrontでの静的ファイルキャッシュ
- 画像・CSSの最適化
- JavaScript minification

## 運用・監視

### 1. CloudWatch監視
- Lambda関数のエラー率、実行時間
- DynamoDBの読み取り・書き込み容量
- API Gatewayのリクエスト数

### 2. ログ管理
- Lambda関数のログ
- API Gatewayのアクセスログ
- CloudFrontのアクセスログ

### 3. アラート設定
- エラー率が5%を超えた場合
- レスポンス時間が5秒を超えた場合
- DynamoDB容量の80%を超えた場合

## コスト管理

### 月額コスト見積もり（20人規模）
- DynamoDB: $1-3
- Lambda: $2-5
- API Gateway: $1-3
- Amplify Hosting: $1-2
- CloudWatch: $1-2
- **合計**: $6-15/月

### コスト削減のポイント
- DynamoDBのOn-Demand課金活用
- Lambda実行時間の最適化
- 不要なログの削除
- CloudFrontキャッシュの活用

## 次のステップ

1. **React移行**: より高度なフロントエンド機能
2. **リアルタイム機能**: DynamoDB Streams + WebSocket
3. **モバイルアプリ**: React Native + Amplify
4. **分析機能**: Amazon QuickSight連携