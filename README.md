# チーム状況ダッシュボード

チームメンバーの負荷状況と困りごとを可視化・共有するWebアプリケーション

## 🎯 機能

- **負荷状況の可視化**: チームメンバーの負荷レベル（高・中・低）を一目で把握
- **負荷状況の更新**: 個人の負荷レベル、案件数、タスク数を簡単に報告
- **困りごと共有**: チーム内の課題や困りごとを投稿・共有
- **リアルタイム更新**: 情報の即座な反映と通知

## 🏗️ アーキテクチャ

### AWS サーバーレス構成

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Amplify       │    │   Lambda +       │    │   DynamoDB      │
│   Hosting       │────│   API Gateway    │────│                 │
│  (Frontend)     │    │   (Backend)      │    │   (Database)    │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

### 技術スタック

**フロントエンド:**
- HTML5 / CSS3 / JavaScript (ES6+)
- AWS Amplify Hosting

**バックエンド:**
- Java 17 + Spring Boot
- AWS Lambda + API Gateway
- Amazon DynamoDB

**デプロイ:**
- AWS SAM (Serverless Application Model)
- AWS Amplify

## 🚀 デプロイ手順

### 前提条件

- AWS CLI
- SAM CLI
- Java 17
- Git

### バックエンドデプロイ

```bash
# 1. リポジトリをクローン
git clone <repository-url>
cd team-dashboard

# 2. バックエンドビルド
cd backend
mvn clean package -DskipTests
cd ..

# 3. SAMデプロイ
sam build
sam deploy --guided
```

### フロントエンドデプロイ

1. **AWS Amplifyコンソール**にアクセス
2. **GitHubリポジトリを接続**
3. **自動ビルド・デプロイ**を実行

詳細な手順は [AWS_DEPLOYMENT_GUIDE.md](AWS_DEPLOYMENT_GUIDE.md) を参照

## 📁 プロジェクト構造

```
├── backend/                 # Spring Boot バックエンド
│   ├── src/main/java/      # Javaソースコード
│   ├── src/test/java/      # テストコード
│   └── pom.xml             # Maven設定
├── frontend/               # フロントエンド
│   ├── index.html          # メインHTML
│   ├── css/style.css       # スタイルシート
│   └── js/                 # JavaScript（未使用）
├── .kiro/specs/            # 機能仕様書
├── template.yaml           # SAM テンプレート
├── amplify.yml             # Amplify ビルド設定
└── deploy-aws.bat          # デプロイスクリプト
```

## 🔧 ローカル開発

### バックエンド

```bash
cd backend
mvn spring-boot:run
```

### フロントエンド

```bash
# 簡易HTTPサーバーを起動
python -m http.server 3000 --directory frontend
# または
npx serve frontend
```

## 📊 API エンドポイント

### 負荷状況 API

- `GET /workload-status` - 全メンバーの負荷状況取得
- `GET /workload-status/my` - 自分の負荷状況取得
- `POST /workload-status` - 負荷状況更新

### 困りごと API

- `GET /team-issues` - 困りごと一覧取得
- `POST /team-issues` - 困りごと投稿
- `PUT /team-issues/{id}/resolve` - 困りごと解決マーク

## 🧪 テスト

```bash
# バックエンドテスト
cd backend
mvn test

# API テスト
curl "https://your-api-gateway-url/dev/workload-status"
```

## 📈 監視・運用

- **CloudWatch Logs**: Lambda関数のログ監視
- **CloudWatch Metrics**: API Gateway、Lambda、DynamoDBのメトリクス
- **AWS X-Ray**: 分散トレーシング（オプション）

## 💰 コスト見積もり

月額 $6-23 (東京リージョン)
- Amplify Hosting: $1-5
- Lambda: $1-3
- API Gateway: $3-10
- DynamoDB: $1-5

## 📝 ライセンス

MIT License

## 🤝 コントリビューション

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📞 サポート

問題や質問がある場合は、GitHubのIssuesを作成してください。

---

**開発チーム**: Team Dashboard Project