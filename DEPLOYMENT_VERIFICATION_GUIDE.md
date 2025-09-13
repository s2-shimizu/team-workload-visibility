# デプロイメント検証ガイド

このガイドでは、AWS Amplifyデプロイ後の検証プロセスについて説明します。

## 概要

デプロイメント検証システムは、AWS Amplifyでのデプロイが完了した後に、アプリケーションが正常に動作していることを自動的に確認するツールです。

### 検証項目

1. **フロントエンドページ可用性チェック** (要件 4.1)
   - メインページ（index.html）の表示確認
   - HTMLの構造検証
   - レスポンス時間の測定

2. **APIエンドポイント応答確認** (要件 4.2)
   - ヘルスチェックエンドポイント
   - 主要なAPIエンドポイント
   - レスポンス形式の検証

3. **静的リソース配信確認** (要件 4.3)
   - CSS ファイル（style.css）
   - JavaScript ファイル（app.js, api-client.js）
   - 設定ファイル（package.json）
   - コンテンツタイプの検証

4. **検証結果レポート生成** (要件 4.3)
   - 詳細な検証結果の出力
   - JSON形式のレポートファイル生成
   - エラーと警告の分類

## ファイル構成

```
deployment-verification.js    # メインの検証スクリプト（Node.js）
verify-deployment.bat        # Windows用実行スクリプト
verify-deployment.ps1        # PowerShell用実行スクリプト
DEPLOYMENT_VERIFICATION_GUIDE.md  # このガイド
```

## 使用方法

### 1. 環境変数を使用した実行

```bash
# 環境変数を設定
set FRONTEND_URL=https://main.d1234567890.amplifyapp.com
set API_URL=https://api123456.execute-api.us-east-1.amazonaws.com/prod

# 検証実行
node deployment-verification.js
```

### 2. コマンドライン引数を使用した実行

```bash
node deployment-verification.js --frontend-url "https://main.d1234567890.amplifyapp.com" --api-url "https://api123456.execute-api.us-east-1.amazonaws.com/prod"
```

### 3. Windows バッチファイルを使用した実行

```cmd
verify-deployment.bat
```

### 4. PowerShell スクリプトを使用した実行

```powershell
.\verify-deployment.ps1 -FrontendUrl "https://main.d1234567890.amplifyapp.com"
```

## コマンドライン オプション

| オプション | 説明 | デフォルト値 |
|-----------|------|-------------|
| `--frontend-url` | フロントエンドURL（必須） | 環境変数 FRONTEND_URL |
| `--api-url` | API URL（オプション） | 環境変数 API_URL |
| `--timeout` | リクエストタイムアウト（ミリ秒） | 30000 |
| `--retries` | 失敗時のリトライ回数 | 3 |
| `--help` | ヘルプメッセージを表示 | - |

## 検証対象エンドポイント

### フロントエンド
- `/` - メインページ
- `/index.html` - インデックスページ直接アクセス

### 静的リソース
- `/css/style.css` - メインスタイルシート
- `/js/app.js` - メインJavaScriptファイル
- `/js/api-client.js` - API クライアント
- `/package.json` - パッケージ設定ファイル

### API エンドポイント
- `/health` - シンプルヘルスチェック
- `/actuator/health` - Spring Boot Actuatorヘルスチェック
- `/api/status` - API ステータス
- `/api/workload-status` - 負荷状況一覧
- `/api/workload-status/my` - 自分の負荷状況
- `/api/team-issues` - チーム課題一覧
- `/api/team-issues/open` - 未解決課題一覧
- `/api/team-issues/statistics` - 課題統計

## 出力例

### 成功時の出力

```
🚀 Starting deployment verification...
Frontend URL: https://main.d1234567890.amplifyapp.com
API URL: https://api123456.execute-api.us-east-1.amazonaws.com/prod

🔧 Validating configuration...
✓ Configuration validated

🌐 Verifying frontend page availability...
✓ Main page (index.html): Available
✓ Index page direct access: Available

📁 Verifying static resources delivery...
✓ CSS: /css/style.css - Available (2048 bytes)
✓ JavaScript: /js/app.js - Available (15360 bytes)
✓ JavaScript: /js/api-client.js - Available (8192 bytes)
✓ JSON: /package.json - Available (512 bytes)

🔌 Verifying API endpoints...
✓ Health check: GET /health - OK (150ms)
✓ API status: GET /api/status - OK (200ms)
✓ Workload status list: GET /api/workload-status - OK (180ms)

📊 Deployment Verification Report
==================================
Total checks: 11
Successful: 11
Failed: 0
Warnings: 0
Errors: 0

✅ Deployment verification completed successfully
```

### 失敗時の出力

```
❌ Frontend page unavailable: Main page (index.html) - HTTP 404
❌ Static resource unavailable: /css/style.css - HTTP 404
❌ API endpoint failed: /api/status - Request timeout after 30000ms

📊 Deployment Verification Report
==================================
Total checks: 11
Successful: 8
Failed: 3
Warnings: 1
Errors: 3

❌ Errors:
  - Frontend page unavailable: Main page (index.html) - HTTP 404
  - Static resource unavailable: /css/style.css - HTTP 404
  - API endpoint failed: /api/status - Request timeout after 30000ms

❌ Deployment verification failed
```

## レポートファイル

検証完了後、`deployment-verification-report.json` ファイルが生成されます。

### レポート構造

```json
{
  "timestamp": "2024-01-15T10:30:00.000Z",
  "success": true,
  "frontend": {
    "pageAvailability": [
      {
        "url": "https://main.d1234567890.amplifyapp.com/",
        "name": "Main page (index.html)",
        "success": true,
        "statusCode": 200,
        "responseTime": 250,
        "contentLength": "1024",
        "htmlValid": true,
        "hasTitle": true
      }
    ],
    "staticResources": [
      {
        "url": "https://main.d1234567890.amplifyapp.com/css/style.css",
        "path": "/css/style.css",
        "type": "CSS",
        "success": true,
        "statusCode": 200,
        "contentType": "text/css",
        "contentLength": "2048",
        "responseTime": 180
      }
    ]
  },
  "api": {
    "endpoints": [
      {
        "url": "https://api123456.execute-api.us-east-1.amazonaws.com/prod/health",
        "path": "/health",
        "method": "GET",
        "name": "Health check",
        "success": true,
        "statusCode": 200,
        "responseTime": 150,
        "contentType": "application/json",
        "responseData": {
          "status": "OK",
          "message": "Lambda function is running"
        }
      }
    ]
  },
  "errors": [],
  "warnings": []
}
```

## トラブルシューティング

### よくある問題と解決方法

#### 1. フロントエンドページが404エラー

**原因**: Amplifyのビルド設定が正しくない、またはファイルが正しくデプロイされていない

**解決方法**:
- `amplify.yml` の `artifacts` 設定を確認
- フロントエンドビルドプロセスが正常に完了しているか確認
- Amplifyコンソールでビルドログを確認

#### 2. 静的リソースが読み込めない

**原因**: ファイルパスが間違っている、またはファイルがビルド成果物に含まれていない

**解決方法**:
- `amplify.yml` の `files` パターンを確認
- ビルド後のファイル構造を確認
- キャッシュ設定を確認

#### 3. APIエンドポイントがタイムアウト

**原因**: Lambda関数の冷起動、またはAPI Gatewayの設定問題

**解決方法**:
- Lambda関数のログを CloudWatch で確認
- API Gateway の設定を確認
- タイムアウト値を増やして再試行

#### 4. CORS エラー

**原因**: API側でCORSが正しく設定されていない

**解決方法**:
- Spring Boot の `@CrossOrigin` アノテーションを確認
- API Gateway の CORS 設定を確認

## 継続的インテグレーション

### GitHub Actions での使用例

```yaml
name: Deployment Verification
on:
  workflow_run:
    workflows: ["Deploy to Amplify"]
    types:
      - completed

jobs:
  verify:
    runs-on: ubuntu-latest
    if: ${{ github.event.workflow_run.conclusion == 'success' }}
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
      - name: Run deployment verification
        run: |
          node deployment-verification.js \
            --frontend-url "${{ secrets.AMPLIFY_URL }}" \
            --api-url "${{ secrets.API_URL }}"
      - name: Upload verification report
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: verification-report
          path: deployment-verification-report.json
```

## カスタマイズ

### 追加のエンドポイント検証

`deployment-verification.js` の `endpoints` 配列に新しいエンドポイントを追加できます：

```javascript
const endpoints = [
    // 既存のエンドポイント...
    { path: '/api/custom-endpoint', method: 'GET', name: 'Custom endpoint' }
];
```

### カスタム検証ロジック

`validateApiResponse` メソッドを拡張して、特定のAPIレスポンスの検証ロジックを追加できます。

### 通知の追加

検証結果をSlackやメールで通知する機能を追加することも可能です。

## セキュリティ考慮事項

- 本番環境のURLや認証情報を環境変数で管理する
- 検証スクリプトは読み取り専用の操作のみ実行する
- レポートファイルに機密情報が含まれないよう注意する

## サポート

問題が発生した場合は、以下の情報を含めて報告してください：

1. 実行したコマンド
2. エラーメッセージ
3. `deployment-verification-report.json` の内容
4. 環境情報（Node.js バージョン、OS など）