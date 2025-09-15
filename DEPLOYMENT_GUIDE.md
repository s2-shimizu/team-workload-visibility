# 🚀 デプロイガイド - リアルタイム機能対応

チーム状況ダッシュボードのデプロイ方法を説明します。WebSocket機能があるため、複数のデプロイオプションを提供します。

## 📋 デプロイオプション

### Option 1: ECS Fargate (推奨) 🌟
**WebSocket完全対応、スケーラブル**
- ✅ WebSocket完全サポート
- ✅ 自動スケーリング
- ✅ ロードバランサー対応
- ✅ 本番環境に最適

### Option 2: EC2 + Application Load Balancer
**従来型、安定性重視**
- ✅ WebSocket完全サポート
- ✅ 固定インスタンス
- ✅ コスト予測しやすい
- ⚠️ 手動スケーリング

### Option 3: Lambda + API Gateway (制限あり)
**サーバーレス、WebSocket制限**
- ⚠️ WebSocket機能制限
- ✅ サーバーレス
- ✅ 低コスト
- ❌ リアルタイム機能なし

## 🚀 クイックスタート

### 1分でデプロイ開始
```powershell
# 対話式クイックデプロイ
.\quick-deploy.ps1

# ECS Fargate（推奨）
.\quick-deploy.ps1 -DeployType ecs -Environment dev

# EC2（シンプル）
.\quick-deploy.ps1 -DeployType ec2 -Environment dev

# Lambda（WebSocket制限）
.\quick-deploy.ps1 -DeployType lambda -Environment dev
```

## 🎯 詳細デプロイ手順

### 1. ECS Fargate（推奨）

#### 前提条件
- AWS CLI設定済み
- Docker Desktop インストール済み
- 適切なIAM権限

#### デプロイコマンド
```powershell
# フルデプロイ
.\deploy-ecs-fargate.ps1 -Environment dev

# ビルドのみ
.\deploy-ecs-fargate.ps1 -Environment dev -BuildOnly

# デプロイのみ（ビルド済み）
.\deploy-ecs-fargate.ps1 -Environment dev -DeployOnly
```

### 2. EC2デプロイ

#### 前提条件
- AWS CLI設定済み
- EC2キーペア作成済み

#### デプロイコマンド
```powershell
# 新規インスタンス作成
.\deploy-ec2.ps1 -Environment dev -KeyName my-key -CreateInstance

# 既存インスタンス使用
.\deploy-ec2.ps1 -Environment dev -KeyName my-key
```

### 3. Lambda（制限あり）

#### デプロイコマンド
```powershell
# SAMデプロイ
.\deploy-sam-stack.ps1 -Environment dev
```

---

## 🐳 ECS Fargate デプロイ（推奨）

### 特徴
- WebSocket完全対応
- 自動スケーリング
- マネージドサービス
- 高可用性

### デプロイ手順

#### Step 1: Dockerイメージ作成
```bash
# Dockerfileを作成してビルド
docker build -t team-dashboard .
```

#### Step 2: ECRにプッシュ
```bash
# ECRリポジトリ作成とプッシュ
aws ecr create-repository --repository-name team-dashboard
docker tag team-dashboard:latest <account-id>.dkr.ecr.ap-northeast-1.amazonaws.com/team-dashboard:latest
docker push <account-id>.dkr.ecr.ap-northeast-1.amazonaws.com/team-dashboard:latest
```

#### Step 3: ECSクラスター作成
```bash
# CloudFormationでECSクラスター作成
aws cloudformation deploy --template-file ecs-infrastructure.yaml --stack-name team-dashboard-ecs
```

---

## 🖥️ EC2 デプロイ（シンプル）

### 特徴
- 従来型デプロイ
- 設定が簡単
- WebSocket完全対応
- 固定コスト

### デプロイ手順

#### Step 1: EC2インスタンス起動
```bash
# EC2インスタンス作成
aws ec2 run-instances --image-id ami-0c3fd0f5d33134a76 --instance-type t3.medium --key-name my-key
```

#### Step 2: アプリケーションデプロイ
```bash
# SSH接続してアプリケーション配置
scp -i my-key.pem target/team-dashboard.jar ec2-user@<instance-ip>:~/
ssh -i my-key.pem ec2-user@<instance-ip>
java -jar team-dashboard.jar
```

---

## ⚡ Lambda デプロイ（制限あり）

### 特徴
- サーバーレス
- 低コスト
- WebSocket制限
- リアルタイム機能なし

### デプロイ手順
```powershell
# 既存のSAMデプロイを使用
.\deploy-sam-stack.ps1 -Environment dev
```

**注意**: WebSocket機能は動作しません。ポーリングベースの更新のみ。

---

## 🔧 設定ファイル

### 環境別設定

#### 開発環境 (dev)
- 小規模インスタンス
- 開発用データベース
- デバッグログ有効

#### 本番環境 (prod)
- 高可用性設定
- 本番用データベース
- セキュリティ強化

### 環境変数
```bash
# 必須環境変数
export AWS_REGION=ap-northeast-1
export WORKLOAD_STATUS_TABLE=WorkloadStatus-dev
export TEAM_ISSUE_TABLE=TeamIssue-dev
export SPRING_PROFILES_ACTIVE=prod
```

---

## 📊 コスト比較

| デプロイ方法 | 月額コスト (概算) | WebSocket | スケーラビリティ |
|-------------|------------------|-----------|------------------|
| ECS Fargate | $30-100 | ✅ | 自動 |
| EC2 t3.medium | $25-50 | ✅ | 手動 |
| Lambda | $5-20 | ❌ | 自動 |

---

## 🚀 クイックスタート

### 1分でデプロイ（ECS Fargate）
```powershell
# 全自動デプロイ
.\quick-deploy-ecs.ps1
```

### 1分でデプロイ（EC2）
```powershell
# EC2デプロイ
.\quick-deploy-ec2.ps1
```

### 既存Lambda（WebSocketなし）
```powershell
# 既存デプロイ
.\deploy-sam-stack.ps1
```

---

## 🔍 デプロイ後の確認

### ヘルスチェック
```bash
# API確認
curl https://your-endpoint/api/status

# WebSocket確認（ECS/EC2のみ）
curl -H "Upgrade: websocket" https://your-endpoint/ws
```

### 統合テスト
```powershell
# 全機能テスト
.\test-deployed-stack.ps1 -ApiEndpoint "https://your-endpoint"

# リアルタイム機能テスト（ECS/EC2のみ）
.\test-realtime-updates.ps1 -BaseUrl "https://your-endpoint"
```

---

## 🛠️ トラブルシューティング

### よくある問題

#### WebSocket接続エラー
**症状**: リアルタイム更新が動作しない
**原因**: Lambda環境でWebSocketを使用
**解決**: ECSまたはEC2にデプロイ

#### CORS エラー
**症状**: フロントエンドからAPI呼び出しエラー
**解決**: CORS設定を確認

#### DynamoDB接続エラー
**症状**: データが保存されない
**解決**: IAM権限とテーブル名を確認

---

## 📞 サポート

デプロイに関する質問は以下の方法で確認：

1. **ログ確認**: CloudWatch Logs
2. **ヘルスチェック**: `/api/status` エンドポイント
3. **統合テスト**: デプロイ後テストスクリプト実行

各デプロイ方法の詳細な手順は、対応するスクリプトファイルを参照してください。