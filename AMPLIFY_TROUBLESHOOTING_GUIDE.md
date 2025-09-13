# AWS Amplify デプロイメント トラブルシューティングガイド

## 概要

このガイドは、AWS Amplifyでのデプロイメント問題を効率的に診断・解決するための包括的なリソースです。一般的なエラーパターン、デバッグ手順、CloudWatchログの確認方法、設定ファイルのベストプラクティスを提供します。

## 目次

1. [一般的なデプロイエラーと解決方法](#一般的なデプロイエラーと解決方法)
2. [ステップバイステップ デバッグ手順](#ステップバイステップ-デバッグ手順)
3. [AWS CloudWatchログの確認方法](#aws-cloudwatchログの確認方法)
4. [設定ファイルのベストプラクティス](#設定ファイルのベストプラクティス)
5. [緊急時対応手順](#緊急時対応手順)

---

## 一般的なデプロイエラーと解決方法

### 1. amplify.yml設定エラー

#### エラー: "Build specification file not found"
**症状**: Amplifyがamplify.ymlファイルを見つけられない

**原因**:
- ファイルがリポジトリのルートディレクトリにない
- ファイル名のスペルミス
- ファイルの権限問題

**解決方法**:
```bash
# ファイルの存在確認
ls -la amplify.yml

# ファイルが存在しない場合、作成
cp amplify.yml.template amplify.yml

# 権限の確認と修正
chmod 644 amplify.yml
```

#### エラー: "Invalid YAML syntax"
**症状**: YAML構文エラーでビルドが失敗

**原因**:
- インデントの問題
- 特殊文字のエスケープ不足
- 不正な文字エンコーディング

**解決方法**:
```bash
# YAML構文チェック
node config-syntax-checker.js

# 手動でYAML構文を確認
python -c "import yaml; yaml.safe_load(open('amplify.yml'))"
```

### 2. フロントエンドビルドエラー

#### エラー: "File not found during build"
**症状**: 指定されたファイルが見つからない

**原因**:
- amplify.ymlで参照しているファイルが存在しない
- ファイルパスの間違い
- 大文字小文字の不一致

**解決方法**:
```bash
# ファイル存在確認
node frontend/validate-files.js

# 実際のファイル構造確認
find frontend -type f -name "*.html" -o -name "*.css" -o -name "*.js"

# amplify.ymlの修正
# artifacts.files セクションを実際のファイル構造に合わせる
```

#### エラー: "Build command failed"
**症状**: npm run buildまたはカスタムビルドコマンドが失敗

**原因**:
- package.jsonのスクリプト設定問題
- 依存関係の不足
- Node.jsバージョンの不一致

**解決方法**:
```bash
# 依存関係の確認とインストール
cd frontend
npm install

# ビルドスクリプトの手動実行
npm run build

# Node.jsバージョンの確認
node --version
npm --version
```

### 3. バックエンドビルドエラー

#### エラー: "Maven build failed"
**症状**: Java Spring Bootアプリケーションのビルドが失敗

**原因**:
- pom.xmlの依存関係問題
- Javaバージョンの不一致
- Maven設定の問題

**解決方法**:
```bash
# Mavenビルドの手動実行
cd backend
./mvnw clean package

# Java環境の確認
java -version
./mvnw -version

# 依存関係の確認
./mvnw dependency:tree
```

#### エラー: "Lambda packaging failed"
**症状**: Lambda用JARファイルの作成が失敗

**原因**:
- Spring Boot Lambda統合設定の不足
- 不適切なpom.xml設定
- メモリ不足

**解決方法**:
```bash
# Lambda用ビルドの実行
./mvnw clean package -Paws-lambda

# JARファイルの検証
java -jar target/app-lambda.jar

# メモリ設定の確認
export MAVEN_OPTS="-Xmx2048m"
```

### 4. デプロイメント実行時エラー

#### エラー: "Access denied"
**症状**: AWS リソースへのアクセスが拒否される

**原因**:
- IAMロールの権限不足
- リソースポリシーの問題
- 環境変数の設定不足

**解決方法**:
```bash
# AWS認証情報の確認
aws sts get-caller-identity

# 必要な権限の確認
node validate-aws-config.js

# 環境変数の設定確認
cat AWS_ENVIRONMENT_VARIABLES.md
```

#### エラー: "Lambda function timeout"
**症状**: Lambda関数の実行がタイムアウト

**原因**:
- 処理時間の超過
- 無限ループ
- 外部API呼び出しの遅延

**解決方法**:
```bash
# CloudWatchログの確認
aws logs describe-log-groups --log-group-name-prefix "/aws/lambda/"

# タイムアウト設定の調整（amplify.ymlまたはAWSコンソール）
# Lambda関数のメモリとタイムアウト設定を増加
```

---

## ステップバイステップ デバッグ手順

### Phase 1: 初期診断

#### Step 1: 基本環境の確認
```bash
# 1. リポジトリ構造の確認
ls -la
tree -L 2

# 2. 設定ファイルの存在確認
ls -la amplify.yml package.json backend/pom.xml

# 3. Git状態の確認
git status
git log --oneline -5
```

#### Step 2: 設定ファイルの検証
```bash
# 1. amplify.yml構文チェック
node config-syntax-checker.js

# 2. package.json検証
cd frontend && npm run validate

# 3. pom.xml検証
cd backend && ./mvnw validate
```

### Phase 2: ビルドプロセスの診断

#### Step 3: ローカルビルドテスト
```bash
# 1. フロントエンドビルド
cd frontend
npm install
npm run build
node verify-build.js

# 2. バックエンドビルド
cd ../backend
./mvnw clean package
node ../validate-lambda-jar.bat
```

#### Step 4: 依存関係の確認
```bash
# 1. フロントエンド依存関係
cd frontend
npm audit
npm outdated

# 2. バックエンド依存関係
cd ../backend
./mvnw dependency:analyze
./mvnw dependency:tree
```

### Phase 3: Amplifyデプロイメント診断

#### Step 5: デプロイ前チェック
```bash
# 1. 包括的な事前チェック実行
node pre-deployment-checker.js

# 2. AWS設定の検証
node validate-aws-config.js

# 3. 環境変数の確認
node setup-environment.js --check
```

#### Step 6: デプロイメント実行と監視
```bash
# 1. デプロイメント開始
# Amplifyコンソールまたはgit pushでトリガー

# 2. ビルドプロセスの監視
node build-process-monitor.js

# 3. リアルタイムログ監視
aws logs tail /aws/amplify/[app-id] --follow
```

### Phase 4: デプロイ後検証

#### Step 7: 動作確認
```bash
# 1. デプロイメント検証実行
node deployment-verification.js

# 2. エンドポイントテスト
curl -X GET https://[your-app-url]/api/health

# 3. フロントエンド動作確認
node test-verification.js
```

#### Step 8: パフォーマンス確認
```bash
# 1. ページ読み込み時間測定
curl -w "@curl-format.txt" -o /dev/null -s https://[your-app-url]

# 2. API応答時間測定
curl -w "@curl-format.txt" -o /dev/null -s https://[your-api-url]/health

# 3. Lambda冷起動時間確認
# CloudWatchメトリクスで確認
```

---

## AWS CloudWatchログの確認方法

### 1. ロググループの特定

#### Amplifyビルドログ
```bash
# Amplifyアプリのロググループを確認
aws logs describe-log-groups --log-group-name-prefix "/aws/amplify"

# 特定のビルドのログストリーム確認
aws logs describe-log-streams --log-group-name "/aws/amplify/[app-id]"
```

#### Lambda関数ログ
```bash
# Lambda関数のロググループ確認
aws logs describe-log-groups --log-group-name-prefix "/aws/lambda"

# 特定のLambda関数のログ確認
aws logs describe-log-streams --log-group-name "/aws/lambda/[function-name]"
```

### 2. ログの取得と分析

#### リアルタイムログ監視
```bash
# Amplifyビルドログのリアルタイム監視
aws logs tail /aws/amplify/[app-id] --follow

# Lambda関数ログのリアルタイム監視
aws logs tail /aws/lambda/[function-name] --follow

# 特定の時間範囲のログ取得
aws logs filter-log-events \
  --log-group-name "/aws/lambda/[function-name]" \
  --start-time 1640995200000 \
  --end-time 1640998800000
```

#### エラーログの検索
```bash
# エラーメッセージの検索
aws logs filter-log-events \
  --log-group-name "/aws/lambda/[function-name]" \
  --filter-pattern "ERROR"

# 特定のエラーパターンの検索
aws logs filter-log-events \
  --log-group-name "/aws/amplify/[app-id]" \
  --filter-pattern "{ $.level = \"ERROR\" }"

# タイムアウトエラーの検索
aws logs filter-log-events \
  --log-group-name "/aws/lambda/[function-name]" \
  --filter-pattern "Task timed out"
```

### 3. ログ分析のベストプラクティス

#### 構造化ログの活用
```javascript
// Lambda関数内でのログ出力例
console.log(JSON.stringify({
  timestamp: new Date().toISOString(),
  level: 'INFO',
  message: 'Processing request',
  requestId: context.awsRequestId,
  userId: event.userId,
  action: 'getUserData'
}));
```

#### CloudWatch Insightsクエリ
```sql
-- エラー率の分析
fields @timestamp, @message
| filter @message like /ERROR/
| stats count() by bin(5m)

-- レスポンス時間の分析
fields @timestamp, @duration
| filter @type = "REPORT"
| stats avg(@duration), max(@duration), min(@duration) by bin(5m)

-- 特定のエラーパターンの分析
fields @timestamp, @message
| filter @message like /NullPointerException/
| sort @timestamp desc
| limit 20
```

### 4. アラートの設定

#### CloudWatchアラームの作成
```bash
# エラー率アラームの作成
aws cloudwatch put-metric-alarm \
  --alarm-name "Lambda-Error-Rate" \
  --alarm-description "Lambda function error rate" \
  --metric-name Errors \
  --namespace AWS/Lambda \
  --statistic Sum \
  --period 300 \
  --threshold 5 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=FunctionName,Value=[function-name] \
  --evaluation-periods 2

# レスポンス時間アラームの作成
aws cloudwatch put-metric-alarm \
  --alarm-name "Lambda-Duration" \
  --alarm-description "Lambda function duration" \
  --metric-name Duration \
  --namespace AWS/Lambda \
  --statistic Average \
  --period 300 \
  --threshold 10000 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=FunctionName,Value=[function-name] \
  --evaluation-periods 2
```

---

## 設定ファイルのベストプラクティス

### 1. amplify.yml設定のベストプラクティス

#### 基本構造
```yaml
version: 1
frontend:
  phases:
    preBuild:
      commands:
        # 依存関係のインストール
        - cd frontend && npm ci
        # 環境変数の確認
        - echo "Node version:" && node --version
        - echo "NPM version:" && npm --version
    build:
      commands:
        # ビルドの実行
        - cd frontend && npm run build
        # ビルド結果の検証
        - cd frontend && node verify-build.js
    postBuild:
      commands:
        # 最終検証
        - echo "Frontend build completed successfully"
  artifacts:
    # 実際に存在するファイルのみ指定
    baseDirectory: frontend
    files:
      - '**/*'
    # 不要なファイルを除外
    exclude:
      - node_modules/**/*
      - src/**/*
      - '*.md'
      - '.git*'
  cache:
    paths:
      - frontend/node_modules/**/*
backend:
  phases:
    preBuild:
      commands:
        # Java環境の確認
        - cd backend && java -version
        - cd backend && ./mvnw -version
        # 依存関係の事前ダウンロード
        - cd backend && ./mvnw dependency:go-offline
    build:
      commands:
        # Maven ビルドの実行
        - cd backend && ./mvnw clean package -DskipTests=false
        # JARファイルの検証
        - cd backend && ls -la target/*.jar
        # Lambda用パッケージの作成
        - cd backend && ./mvnw package -Paws-lambda
    postBuild:
      commands:
        # ビルド結果の最終確認
        - echo "Backend build completed successfully"
  artifacts:
    baseDirectory: backend/target
    files:
      - '*.jar'
  cache:
    paths:
      - backend/.m2/**/*
```

#### 環境変数の管理
```yaml
# 環境固有の設定
frontend:
  phases:
    preBuild:
      commands:
        # 環境変数の設定確認
        - echo "Environment:" $AWS_BRANCH
        - echo "API Endpoint:" $API_ENDPOINT
        # 環境固有の設定ファイル作成
        - |
          if [ "$AWS_BRANCH" = "main" ]; then
            cp frontend/config/prod.env frontend/.env
          else
            cp frontend/config/dev.env frontend/.env
          fi
```

### 2. package.json設定のベストプラクティス

#### スクリプト設定
```json
{
  "name": "amplify-frontend",
  "version": "1.0.0",
  "scripts": {
    "build": "npm run validate && npm run compile && npm run optimize",
    "validate": "node validate-files.js",
    "compile": "node build-script.js",
    "optimize": "node optimize-assets.js",
    "test": "jest --coverage",
    "lint": "eslint src/**/*.js",
    "clean": "rimraf build dist"
  },
  "dependencies": {
    // 本番環境で必要な依存関係のみ
  },
  "devDependencies": {
    // 開発・ビルド時のみ必要な依存関係
    "jest": "^29.0.0",
    "eslint": "^8.0.0",
    "rimraf": "^3.0.0"
  },
  "engines": {
    "node": ">=18.0.0",
    "npm": ">=8.0.0"
  }
}
```

### 3. pom.xml設定のベストプラクティス

#### 基本設定
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.example</groupId>
    <artifactId>amplify-backend</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <spring.boot.version>3.1.0</spring.boot.version>
        <aws.lambda.version>1.2.2</aws.lambda.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <version>${spring.boot.version}</version>
        </dependency>
        
        <!-- AWS Lambda Support -->
        <dependency>
            <groupId>com.amazonaws.serverless</groupId>
            <artifactId>aws-serverless-java-container-springboot3</artifactId>
            <version>${aws.lambda.version}</version>
        </dependency>
    </dependencies>
    
    <profiles>
        <!-- Lambda用ビルドプロファイル -->
        <profile>
            <id>aws-lambda</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-maven-plugin</artifactId>
                        <configuration>
                            <classifier>aws</classifier>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>
</project>
```

### 4. 環境変数設定のベストプラクティス

#### 環境変数の分類と管理
```bash
# 必須環境変数
export AWS_REGION=ap-northeast-1
export AWS_ACCOUNT_ID=123456789012

# アプリケーション固有の環境変数
export API_ENDPOINT=https://api.example.com
export DATABASE_URL=dynamodb://table-name

# 環境固有の設定
if [ "$AWS_BRANCH" = "main" ]; then
    export ENVIRONMENT=production
    export LOG_LEVEL=warn
else
    export ENVIRONMENT=development
    export LOG_LEVEL=debug
fi
```

### 5. セキュリティのベストプラクティス

#### 機密情報の管理
```yaml
# amplify.ymlでの機密情報の扱い
frontend:
  phases:
    preBuild:
      commands:
        # AWS Systems Manager Parameter Storeから機密情報を取得
        - export DB_PASSWORD=$(aws ssm get-parameter --name "/app/db/password" --with-decryption --query "Parameter.Value" --output text)
        # 環境変数の設定（値は表示しない）
        - echo "Database password configured"
```

#### IAMロールの最小権限設定
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:GetItem",
        "dynamodb:PutItem",
        "dynamodb:UpdateItem",
        "dynamodb:DeleteItem"
      ],
      "Resource": "arn:aws:dynamodb:*:*:table/YourTableName"
    }
  ]
}
```

---

## 緊急時対応手順

### 1. 本番環境でのデプロイ失敗

#### 即座に実行すべき手順
```bash
# 1. 現在の状況確認
aws amplify get-app --app-id [app-id]
aws amplify list-jobs --app-id [app-id] --branch-name main

# 2. 前回成功したバージョンへのロールバック
aws amplify start-job --app-id [app-id] --branch-name main --job-type RELEASE --job-id [previous-successful-job-id]

# 3. ヘルスチェックの実行
curl -f https://[your-app-url]/health || echo "Application is down"
```

### 2. Lambda関数の異常

#### 緊急対応手順
```bash
# 1. Lambda関数の状態確認
aws lambda get-function --function-name [function-name]

# 2. 最新のエラーログ確認
aws logs filter-log-events --log-group-name "/aws/lambda/[function-name]" --start-time $(date -d '1 hour ago' +%s)000

# 3. 前のバージョンへのロールバック
aws lambda update-alias --function-name [function-name] --name LIVE --function-version [previous-version]
```

### 3. 通信とエスカレーション

#### 関係者への通知
```bash
# 1. Slackまたはメール通知の送信
curl -X POST -H 'Content-type: application/json' \
  --data '{"text":"🚨 Production deployment failed. Investigating..."}' \
  [SLACK_WEBHOOK_URL]

# 2. インシデント管理システムへの登録
# PagerDuty、Jira、またはその他のシステムに応じて実行
```

#### エスカレーション基準
- **Level 1**: ビルド失敗（開発者対応）
- **Level 2**: デプロイ失敗（チームリード対応）
- **Level 3**: 本番サービス停止（マネージャー対応）

### 4. 事後対応

#### インシデント後の分析
```bash
# 1. 詳細ログの収集
aws logs create-export-task \
  --log-group-name "/aws/amplify/[app-id]" \
  --from $(date -d '2 hours ago' +%s)000 \
  --to $(date +%s)000 \
  --destination [S3_BUCKET]

# 2. メトリクスの分析
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Errors \
  --dimensions Name=FunctionName,Value=[function-name] \
  --start-time $(date -d '2 hours ago' --iso-8601) \
  --end-time $(date --iso-8601) \
  --period 300 \
  --statistics Sum
```

---

## まとめ

このトラブルシューティングガイドを効果的に活用するために：

1. **予防的アプローチ**: 定期的な事前チェックとモニタリングを実施
2. **段階的診断**: 基本的な確認から詳細な分析まで段階的に進める
3. **ログの活用**: CloudWatchログを効果的に分析してroot causeを特定
4. **設定の標準化**: ベストプラクティスに従った設定ファイルの維持
5. **緊急時対応**: 迅速な対応とエスカレーションプロセスの確立

問題が解決しない場合は、このガイドの手順を実行した結果とともに、詳細な情報を収集してサポートチームに連絡してください。