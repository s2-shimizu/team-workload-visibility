# 設計ドキュメント

## 概要

AWS Amplifyでのデプロイ失敗問題を解決するための包括的な設計です。現在のアプリケーション構成（フロントエンド：静的HTML/CSS/JavaScript、バックエンド：Java Spring Boot Lambda）に対して、Amplifyの設定を最適化し、安定したデプロイメントプロセスを確立します。

## アーキテクチャ

### 現在の問題点の分析

1. **amplify.yml設定の問題**
   - 存在しない`aws-index.html`ファイルを参照
   - バックエンドビルド設定が欠如
   - 不適切なファイル除外設定

2. **ビルドプロセスの問題**
   - フロントエンドビルドが単純なファイルコピーのみ
   - バックエンドのMavenビルドが設定されていない
   - 依存関係の管理が不十分

3. **デプロイメント構成の問題**
   - 環境変数の設定が不明確
   - Lambda関数の設定が不完全
   - API Gatewayとの統合設定が欠如

### 目標アーキテクチャ

```
GitHub Repository
    ↓ (Push trigger)
AWS Amplify
    ├── Frontend Build
    │   ├── Static file processing
    │   ├── Asset optimization
    │   └── CloudFront distribution
    └── Backend Build
        ├── Maven package
        ├── Lambda deployment
        └── API Gateway integration
```

## コンポーネントと インターフェース

### 1. Amplify設定コンポーネント

**責任**: デプロイメントプロセスの定義と制御

**インターフェース**:
- 入力: `amplify.yml`設定ファイル
- 出力: ビルドされたアプリケーション成果物

**主要機能**:
- フロントエンドとバックエンドの並行ビルド
- 環境変数の管理
- 成果物の適切な配置

### 2. フロントエンドビルドコンポーネント

**責任**: 静的ファイルの処理と最適化

**インターフェース**:
- 入力: HTML、CSS、JavaScriptファイル
- 出力: 最適化された静的ファイル

**主要機能**:
- ファイルの整合性チェック
- 必要なファイルのみの選択的コピー
- キャッシュ設定の最適化

### 3. バックエンドビルドコンポーネント

**責任**: Java Spring BootアプリケーションのLambda用パッケージ化

**インターフェース**:
- 入力: Javaソースコード、pom.xml
- 出力: Lambda実行可能JARファイル

**主要機能**:
- Maven依存関係の解決
- Spring Boot Lambda統合
- 最適化されたJARファイルの生成

### 4. デプロイメント検証コンポーネント

**責任**: デプロイ後の動作確認

**インターフェース**:
- 入力: デプロイされたアプリケーションURL
- 出力: 検証結果レポート

**主要機能**:
- フロントエンドページの可用性チェック
- APIエンドポイントの応答確認
- 静的リソースの配信確認

## データモデル

### 設定データモデル

```yaml
AmplifyConfiguration:
  version: string
  frontend:
    phases:
      preBuild: CommandList
      build: CommandList
      postBuild: CommandList
    artifacts:
      baseDirectory: string
      files: FilePatternList
    cache:
      paths: PathList
  backend:
    phases:
      preBuild: CommandList
      build: CommandList
    artifacts:
      baseDirectory: string
      files: FilePatternList
```

### ビルド結果データモデル

```yaml
BuildResult:
  status: enum [SUCCESS, FAILURE, IN_PROGRESS]
  frontend:
    buildTime: duration
    artifacts: FileList
    errors: ErrorList
  backend:
    buildTime: duration
    jarFile: string
    errors: ErrorList
  deployment:
    url: string
    apiEndpoint: string
    status: enum [DEPLOYED, FAILED]
```

## エラーハンドリング

### 1. ビルドエラーの処理

**フロントエンドビルドエラー**:
- ファイル不足エラー → 詳細なファイルリストの提供
- 権限エラー → 適切な権限設定の提案
- 構文エラー → 具体的な行番号と修正提案

**バックエンドビルドエラー**:
- Maven依存関係エラー → 依存関係の解決手順の提供
- Java版本エラー → 適切なJava環境の設定手順
- コンパイルエラー → 詳細なエラーメッセージと修正提案

### 2. デプロイメントエラーの処理

**設定エラー**:
- YAML構文エラー → 構文チェックと修正提案
- 環境変数エラー → 必要な環境変数の一覧と設定方法
- 権限エラー → IAMロールと権限の設定手順

**実行時エラー**:
- Lambda実行エラー → CloudWatchログの確認手順
- API Gatewayエラー → エンドポイント設定の確認手順
- DynamoDBアクセスエラー → 権限とテーブル設定の確認

### 3. エラー回復戦略

**自動回復**:
- 一時的なネットワークエラー → 自動リトライ機能
- 依存関係の解決失敗 → キャッシュクリアと再試行

**手動介入が必要**:
- 設定ファイルの構文エラー → 修正手順の提供
- 権限設定の問題 → 詳細な設定手順の提供

## テスト戦略

### 1. 設定ファイルのテスト

**YAML構文テスト**:
- amplify.ymlの構文検証
- 必須フィールドの存在確認
- ファイルパスの有効性確認

**ビルドコマンドテスト**:
- 各ビルドコマンドの実行可能性確認
- 依存関係の可用性確認
- 環境変数の設定確認

### 2. ビルドプロセスのテスト

**フロントエンドビルドテスト**:
- 静的ファイルの正常なコピー確認
- ファイル除外設定の動作確認
- 成果物の完整性確認

**バックエンドビルドテスト**:
- Mavenビルドの成功確認
- JARファイルの生成確認
- Lambda実行可能性の確認

### 3. デプロイメントテスト

**統合テスト**:
- フロントエンドページの表示確認
- APIエンドポイントの応答確認
- 静的リソースの配信確認

**パフォーマンステスト**:
- ページ読み込み時間の測定
- API応答時間の測定
- Lambda冷起動時間の測定

### 4. 継続的テスト

**自動テスト**:
- GitHubプッシュ時の自動ビルドテスト
- デプロイ後の自動動作確認
- 定期的なヘルスチェック

**監視とアラート**:
- CloudWatchメトリクスの設定
- エラー率の監視
- 可用性の監視

## 実装の優先順位

### フェーズ1: 基本設定の修正
1. amplify.yml設定ファイルの修正
2. フロントエンドビルドプロセスの最適化
3. バックエンドビルド設定の追加

### フェーズ2: デプロイメントプロセスの確立
1. 環境変数の設定
2. Lambda関数の設定
3. API Gatewayの統合

### フェーズ3: 検証と監視
1. デプロイ後の自動検証
2. エラーハンドリングの実装
3. 監視とアラートの設定

### フェーズ4: 継続的改善
1. パフォーマンス最適化
2. セキュリティ強化
3. 運用プロセスの自動化