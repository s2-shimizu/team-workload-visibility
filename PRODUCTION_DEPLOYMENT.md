# 🚀 本番環境デプロイガイド

チーム状況ダッシュボードを本番環境にデプロイするための包括的なガイドです。

## 📋 本番デプロイ前チェックリスト

### 🔒 セキュリティ要件
- [ ] AWS IAM権限の最小化
- [ ] DynamoDBテーブルの暗号化設定
- [ ] VPCとセキュリティグループの設定
- [ ] SSL/TLS証明書の準備
- [ ] 認証システム（Cognito）の設定
- [ ] 環境変数の暗号化

### 🏗️ インフラ要件
- [ ] 本番用DynamoDBテーブル作成
- [ ] CloudWatch監視設定
- [ ] バックアップ戦略の確立
- [ ] ドメイン名とDNS設定
- [ ] CDN（CloudFront）設定

### 🧪 テスト要件
- [ ] 統合テストの実行
- [ ] 負荷テストの実行
- [ ] セキュリティテストの実行
- [ ] 災害復旧テストの実行

---

## 🎯 推奨本番アーキテクチャ

### Option 1: ECS Fargate + ALB（推奨）
```
Internet → CloudFront → ALB → ECS Fargate → DynamoDB
                              ↓
                         CloudWatch Logs
```

### Option 2: EC2 + Auto Scaling
```
Internet → CloudFront → ALB → EC2 Auto Scaling → DynamoDB
                              ↓
                         CloudWatch Logs
```

### Option 3: Lambda（WebSocket制限）
```
Internet → CloudFront → API Gateway → Lambda → DynamoDB
                                      ↓
                               CloudWatch Logs
```

---

## 🚀 本番デプロイ手順

### Step 1: 事前準備

#### 1.1 本番用DynamoDBテーブル作成
```powershell
# 本番用テーブル作成スクリプト
.\create-production-tables.ps1 -Environment prod
```

#### 1.2 ドメインとSSL証明書準備
```bash
# Route 53でドメイン設定
aws route53 create-hosted-zone --name yourdomain.com

# ACM証明書作成
aws acm request-certificate --domain-name yourdomain.com --validation-method DNS
```

#### 1.3 本番用設定ファイル作成
```yaml
# backend/src/main/resources/application-prod.yml
spring:
  profiles:
    active: prod
  
aws:
  region: ap-northeast-1
  dynamodb:
    tables:
      workload-status: WorkloadStatus-prod
      team-issue: TeamIssue-prod

logging:
  level:
    com.teamdashboard: INFO
    org.springframework.security: WARN
```

### Step 2: ECS Fargate本番デプロイ（推奨）

#### 2.1 本番用デプロイ実行
```powershell
# 本番環境デプロイ
.\deploy-ecs-fargate.ps1 -Environment prod -AppName team-dashboard-prod
```

#### 2.2 カスタムドメイン設定
```powershell
# ドメイン設定スクリプト
.\setup-production-domain.ps1 -DomainName yourdomain.com -Environment prod
```

### Step 3: セキュリティ強化

#### 3.1 WAF設定
```powershell
# WAF設定スクリプト
.\setup-production-waf.ps1 -Environment prod
```

#### 3.2 セキュリティグループ最適化
```powershell
# セキュリティ設定
.\configure-production-security.ps1 -Environment prod
```

---

## 📊 監視・ログ設定

### CloudWatch監視
- CPU使用率
- メモリ使用率
- レスポンス時間
- エラー率
- DynamoDB読み書き容量

### アラート設定
- 高CPU使用率（80%以上）
- 高エラー率（5%以上）
- DynamoDB制限エラー
- ヘルスチェック失敗

---

## 🔧 本番用設定最適化

### パフォーマンス設定
- JVMヒープサイズ調整
- 接続プール設定
- DynamoDB読み書き容量設定
- CDNキャッシュ設定

### セキュリティ設定
- HTTPS強制
- セキュリティヘッダー
- CORS設定最適化
- 認証トークン有効期限

---

## 🚨 災害復旧・バックアップ

### バックアップ戦略
- DynamoDBポイントインタイム復旧
- 設定ファイルのバージョン管理
- データベーススナップショット

### 復旧手順
- 自動フェイルオーバー
- 手動復旧プロセス
- データ整合性チェック

---

## 📈 スケーリング戦略

### 自動スケーリング
- ECS Fargateタスク数
- DynamoDB読み書き容量
- CloudFrontキャッシュ

### 監視指標
- 同時接続数
- レスポンス時間
- スループット