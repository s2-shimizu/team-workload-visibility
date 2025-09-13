# AWS環境変数設定ガイド

## 概要

このドキュメントでは、Team Dashboard アプリケーションのAWS Amplifyデプロイメントに必要な環境変数の定義と設定手順を説明します。

## 必要な環境変数

### 1. アプリケーション基本設定

| 環境変数名 | 説明 | 必須 | デフォルト値 | 例 |
|-----------|------|------|-------------|-----|
| `SPRING_PROFILES_ACTIVE` | Spring Bootプロファイル | ✅ | `lambda,dynamodb` | `lambda,dynamodb` |
| `JAVA_TOOL_OPTIONS` | JVM最適化オプション | ✅ | `-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Djava.awt.headless=true` | 左記参照 |
| `AWS_REGION` | AWSリージョン | ✅ | `ap-northeast-1` | `ap-northeast-1` |

### 2. DynamoDB設定

| 環境変数名 | 説明 | 必須 | デフォルト値 | 例 |
|-----------|------|------|-------------|-----|
| `DYNAMODB_TABLE_NAME` | DynamoDBテーブル名 | ✅ | - | `TeamDashboard-dev` |
| `DYNAMODB_ENDPOINT` | DynamoDBエンドポイント（ローカル開発用） | ❌ | - | `http://localhost:8000` |
| `AWS_ACCESS_KEY_ID` | AWSアクセスキー（ローカル開発用） | ❌ | - | `AKIAIOSFODNN7EXAMPLE` |
| `AWS_SECRET_ACCESS_KEY` | AWSシークレットキー（ローカル開発用） | ❌ | - | `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY` |

### 3. API Gateway設定

| 環境変数名 | 説明 | 必須 | デフォルト値 | 例 |
|-----------|------|------|-------------|-----|
| `API_GATEWAY_STAGE` | API Gatewayステージ名 | ✅ | `dev` | `dev`, `staging`, `prod` |
| `CORS_ALLOWED_ORIGINS` | CORS許可オリジン | ✅ | `*` | `https://example.com,https://app.example.com` |
| `API_BASE_PATH` | APIベースパス | ❌ | `/` | `/api/v1` |

### 4. セキュリティ設定

| 環境変数名 | 説明 | 必須 | デフォルト値 | 例 |
|-----------|------|------|-------------|-----|
| `JWT_SECRET` | JWT署名用シークレット | ✅ | - | `your-256-bit-secret` |
| `JWT_EXPIRATION` | JWTトークン有効期限（秒） | ❌ | `86400` | `86400` |
| `COGNITO_USER_POOL_ID` | Cognito User Pool ID | ❌ | - | `ap-northeast-1_XXXXXXXXX` |
| `COGNITO_CLIENT_ID` | Cognito Client ID | ❌ | - | `1234567890abcdefghijklmnop` |

### 5. ログ設定

| 環境変数名 | 説明 | 必須 | デフォルト値 | 例 |
|-----------|------|------|-------------|-----|
| `LOG_LEVEL` | ログレベル | ❌ | `INFO` | `DEBUG`, `INFO`, `WARN`, `ERROR` |
| `LOG_FORMAT` | ログフォーマット | ❌ | `JSON` | `JSON`, `PLAIN` |

## AWS Amplify環境変数設定手順

### 1. Amplifyコンソールでの設定

1. AWS Amplifyコンソールにアクセス
2. 対象のアプリケーションを選択
3. 左側メニューから「Environment variables」を選択
4. 「Manage variables」をクリック
5. 以下の環境変数を追加：

```bash
# 基本設定
SPRING_PROFILES_ACTIVE=lambda,dynamodb
JAVA_TOOL_OPTIONS=-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Djava.awt.headless=true
AWS_REGION=ap-northeast-1

# DynamoDB設定
DYNAMODB_TABLE_NAME=TeamDashboard-dev

# API Gateway設定
API_GATEWAY_STAGE=dev
CORS_ALLOWED_ORIGINS=*

# セキュリティ設定
JWT_SECRET=your-secure-256-bit-secret-key-here
JWT_EXPIRATION=86400

# ログ設定
LOG_LEVEL=INFO
LOG_FORMAT=JSON
```

### 2. amplify.ymlでの環境変数参照

```yaml
version: 1
backend:
  phases:
    build:
      commands:
        - echo "Environment: $AWS_REGION"
        - echo "DynamoDB Table: $DYNAMODB_TABLE_NAME"
        - echo "Spring Profile: $SPRING_PROFILES_ACTIVE"
        - cd backend
        - mvn package -DskipTests -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE
```

### 3. 環境別設定

#### 開発環境 (dev)
```bash
SPRING_PROFILES_ACTIVE=lambda,dynamodb,dev
DYNAMODB_TABLE_NAME=TeamDashboard-dev
API_GATEWAY_STAGE=dev
LOG_LEVEL=DEBUG
```

#### ステージング環境 (staging)
```bash
SPRING_PROFILES_ACTIVE=lambda,dynamodb,staging
DYNAMODB_TABLE_NAME=TeamDashboard-staging
API_GATEWAY_STAGE=staging
LOG_LEVEL=INFO
```

#### 本番環境 (prod)
```bash
SPRING_PROFILES_ACTIVE=lambda,dynamodb,prod
DYNAMODB_TABLE_NAME=TeamDashboard-prod
API_GATEWAY_STAGE=prod
LOG_LEVEL=WARN
CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

## ローカル開発環境での設定

### 1. .env ファイルの作成

プロジェクトルートに `.env` ファイルを作成：

```bash
# .env
SPRING_PROFILES_ACTIVE=local
AWS_REGION=ap-northeast-1
DYNAMODB_ENDPOINT=http://localhost:8000
DYNAMODB_TABLE_NAME=TeamDashboard-local
AWS_ACCESS_KEY_ID=dummy
AWS_SECRET_ACCESS_KEY=dummy
LOG_LEVEL=DEBUG
```

### 2. application.properties での参照

```properties
# backend/src/main/resources/application-local.properties
spring.profiles.active=${SPRING_PROFILES_ACTIVE:local}
aws.region=${AWS_REGION:ap-northeast-1}
aws.dynamodb.endpoint=${DYNAMODB_ENDPOINT:}
aws.dynamodb.table-name=${DYNAMODB_TABLE_NAME:TeamDashboard-local}
logging.level.com.teamdashboard=${LOG_LEVEL:DEBUG}
```

## セキュリティ考慮事項

### 1. 機密情報の管理

- **JWT_SECRET**: 256ビット以上の強力なランダム文字列を使用
- **AWS認証情報**: Amplifyの場合はIAMロールを使用し、直接的なキー設定は避ける
- **データベース接続情報**: AWS Systems Manager Parameter Storeの使用を推奨

### 2. 環境変数の暗号化

```bash
# AWS Systems Manager Parameter Storeを使用した機密情報管理
aws ssm put-parameter \
    --name "/team-dashboard/dev/jwt-secret" \
    --value "your-secret-key" \
    --type "SecureString" \
    --region ap-northeast-1
```

### 3. アクセス制御

- 環境変数へのアクセスは必要最小限のIAMロールに制限
- 本番環境では開発者の直接アクセスを制限
- 監査ログの有効化

## トラブルシューティング

### 1. 環境変数が認識されない場合

```bash
# Amplifyビルドログで環境変数を確認
echo "Current environment variables:"
printenv | grep -E "(SPRING|AWS|DYNAMODB|JWT)"
```

### 2. DynamoDB接続エラー

```bash
# DynamoDBテーブルの存在確認
aws dynamodb describe-table --table-name $DYNAMODB_TABLE_NAME --region $AWS_REGION
```

### 3. Lambda実行時エラー

```bash
# CloudWatchログでエラー確認
aws logs describe-log-groups --log-group-name-prefix "/aws/lambda/team-dashboard"
```

## 検証スクリプト

環境変数の設定を検証するためのスクリプト：

```bash
#!/bin/bash
# validate-environment.sh

echo "=== 環境変数検証 ==="

# 必須環境変数のチェック
required_vars=("SPRING_PROFILES_ACTIVE" "AWS_REGION" "DYNAMODB_TABLE_NAME")

for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        echo "❌ $var が設定されていません"
        exit 1
    else
        echo "✅ $var = ${!var}"
    fi
done

# DynamoDBテーブルの存在確認
if aws dynamodb describe-table --table-name "$DYNAMODB_TABLE_NAME" --region "$AWS_REGION" >/dev/null 2>&1; then
    echo "✅ DynamoDBテーブル '$DYNAMODB_TABLE_NAME' が存在します"
else
    echo "❌ DynamoDBテーブル '$DYNAMODB_TABLE_NAME' が見つかりません"
fi

echo "=== 検証完了 ==="
```

このガイドに従って環境変数を適切に設定することで、Team DashboardアプリケーションのAWS Amplifyデプロイメントが正常に動作します。