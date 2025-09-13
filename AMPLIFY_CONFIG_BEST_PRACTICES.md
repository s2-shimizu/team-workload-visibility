# Amplify設定ファイル ベストプラクティスガイド

## 概要

AWS Amplifyの設定ファイル（amplify.yml）を最適化し、安定したデプロイメントを実現するためのベストプラクティス集です。

## 基本的なamplify.yml構造

### 推奨テンプレート
```yaml
version: 1
frontend:
  phases:
    preBuild:
      commands:
        # 環境確認
        - echo "Node.js version:" && node --version
        - echo "NPM version:" && npm --version
        - echo "Build environment:" && echo $AWS_BRANCH
        # 依存関係のインストール
        - cd frontend && npm ci --only=production
        # 環境変数の設定
        - cd frontend && npm run setup:env
    build:
      commands:
        # ビルド前の検証
        - cd frontend && npm run validate
        # メインビルド
        - cd frontend && npm run build
        # ビルド後の検証
        - cd frontend && npm run verify:build
    postBuild:
      commands:
        # 最終確認
        - echo "Frontend build completed successfully"
        - ls -la frontend/build/
  artifacts:
    baseDirectory: frontend/build
    files:
      - '**/*'
    exclude:
      - node_modules/**/*
      - src/**/*
      - '*.md'
      - '.git*'
      - '*.log'
      - 'test/**/*'
  cache:
    paths:
      - frontend/node_modules/**/*
backend:
  phases:
    preBuild:
      commands:
        # Java環境確認
        - cd backend && java -version
        - cd backend && ./mvnw -version
        # 依存関係の事前ダウンロード
        - cd backend && ./mvnw dependency:go-offline -q
    build:
      commands:
        # テスト実行
        - cd backend && ./mvnw test
        # パッケージ作成
        - cd backend && ./mvnw clean package -DskipTests=false
        # Lambda用パッケージ
        - cd backend && ./mvnw package -Paws-lambda -q
    postBuild:
      commands:
        # JARファイル確認
        - cd backend && ls -la target/*.jar
        - echo "Backend build completed successfully"
  artifacts:
    baseDirectory: backend/target
    files:
      - '*.jar'
    exclude:
      - '*-sources.jar'
      - '*-javadoc.jar'
  cache:
    paths:
      - backend/.m2/**/*
```

## 環境別設定の管理

### 1. 環境変数を使用した分岐
```yaml
frontend:
  phases:
    preBuild:
      commands:
        # 環境別設定の適用
        - |
          if [ "$AWS_BRANCH" = "main" ]; then
            echo "Production environment"
            export NODE_ENV=production
            export API_ENDPOINT=$PROD_API_ENDPOINT
          elif [ "$AWS_BRANCH" = "develop" ]; then
            echo "Development environment"
            export NODE_ENV=development
            export API_ENDPOINT=$DEV_API_ENDPOINT
          else
            echo "Feature branch environment"
            export NODE_ENV=development
            export API_ENDPOINT=$DEV_API_ENDPOINT
          fi
        # 設定ファイルの生成
        - cd frontend && npm run generate:config
```

### 2. 環境固有の設定ファイル
```yaml
frontend:
  phases:
    preBuild:
      commands:
        # 環境固有の設定ファイルをコピー
        - |
          case "$AWS_BRANCH" in
            "main")
              cp frontend/config/production.json frontend/config/app.json
              ;;
            "develop")
              cp frontend/config/development.json frontend/config/app.json
              ;;
            *)
              cp frontend/config/feature.json frontend/config/app.json
              ;;
          esac
```

## エラーハンドリングとログ出力

### 1. 詳細なログ出力
```yaml
frontend:
  phases:
    preBuild:
      commands:
        # 詳細なログ出力を有効化
        - set -e  # エラー時に即座に停止
        - set -x  # 実行コマンドを表示
        - echo "=== Pre-build phase started ==="
        - echo "Current directory: $(pwd)"
        - echo "Available disk space: $(df -h .)"
        - echo "Memory usage: $(free -h)"
    build:
      commands:
        - echo "=== Build phase started ==="
        - cd frontend
        # ビルド前の状態確認
        - echo "Source files count: $(find src -type f | wc -l)"
        - npm run build 2>&1 | tee build.log
        # ビルド後の状態確認
        - echo "Build files count: $(find build -type f | wc -l)"
        - echo "Build directory size: $(du -sh build)"
    postBuild:
      commands:
        - echo "=== Post-build phase started ==="
        # ビルド結果の検証
        - |
          if [ ! -d "frontend/build" ]; then
            echo "ERROR: Build directory not found"
            exit 1
          fi
        - |
          if [ -z "$(ls -A frontend/build)" ]; then
            echo "ERROR: Build directory is empty"
            exit 1
          fi
        - echo "Build completed successfully"
```

### 2. エラー時の詳細情報収集
```yaml
frontend:
  phases:
    build:
      commands:
        - cd frontend
        # エラー時の情報収集を含むビルド実行
        - |
          if ! npm run build; then
            echo "=== BUILD FAILED - Collecting debug information ==="
            echo "Node.js version: $(node --version)"
            echo "NPM version: $(npm --version)"
            echo "Package.json scripts:"
            cat package.json | jq '.scripts'
            echo "Installed packages:"
            npm list --depth=0
            echo "Build log (last 50 lines):"
            tail -50 build.log || echo "No build log found"
            echo "Disk space:"
            df -h
            echo "Memory usage:"
            free -h
            exit 1
          fi
```

## パフォーマンス最適化

### 1. 並列ビルドの活用
```yaml
version: 1
frontend:
  phases:
    preBuild:
      commands:
        # 依存関係のインストールを並列化
        - cd frontend && npm ci --prefer-offline --no-audit
    build:
      commands:
        # 並列ビルドの実行
        - cd frontend && npm run build:parallel
backend:
  phases:
    preBuild:
      commands:
        # Maven依存関係の並列ダウンロード
        - cd backend && ./mvnw dependency:go-offline -T 4
    build:
      commands:
        # 並列コンパイル
        - cd backend && ./mvnw clean package -T 4 -q
```

### 2. キャッシュの最適化
```yaml
frontend:
  cache:
    paths:
      # Node.jsモジュールキャッシュ
      - frontend/node_modules/**/*
      # NPMキャッシュ
      - ~/.npm/**/*
      # ビルドキャッシュ
      - frontend/.cache/**/*
backend:
  cache:
    paths:
      # Maven依存関係キャッシュ
      - backend/.m2/**/*
      # Mavenラッパーキャッシュ
      - backend/.mvn/**/*
```

### 3. 不要なファイルの除外
```yaml
frontend:
  artifacts:
    baseDirectory: frontend/build
    files:
      - '**/*'
    exclude:
      # 開発用ファイル
      - '**/*.map'
      - '**/*.test.js'
      - '**/test/**/*'
      - '**/tests/**/*'
      - '**/__tests__/**/*'
      # ドキュメント
      - '**/*.md'
      - '**/README*'
      - '**/CHANGELOG*'
      # 設定ファイル
      - '**/.eslintrc*'
      - '**/.prettierrc*'
      - '**/tsconfig.json'
      # ログファイル
      - '**/*.log'
      - '**/logs/**/*'
      # 一時ファイル
      - '**/.tmp/**/*'
      - '**/.temp/**/*'
```

## セキュリティのベストプラクティス

### 1. 機密情報の管理
```yaml
frontend:
  phases:
    preBuild:
      commands:
        # AWS Systems Manager Parameter Storeから機密情報を取得
        - export DATABASE_URL=$(aws ssm get-parameter --name "/app/database/url" --with-decryption --query "Parameter.Value" --output text)
        - export API_KEY=$(aws ssm get-parameter --name "/app/api/key" --with-decryption --query "Parameter.Value" --output text)
        # 環境変数の設定確認（値は表示しない）
        - |
          if [ -z "$DATABASE_URL" ]; then
            echo "ERROR: DATABASE_URL not set"
            exit 1
          fi
        - echo "Environment variables configured successfully"
```

### 2. 依存関係のセキュリティチェック
```yaml
frontend:
  phases:
    preBuild:
      commands:
        - cd frontend
        # セキュリティ監査の実行
        - npm audit --audit-level moderate
        # 脆弱性の自動修正（可能な場合）
        - npm audit fix --only=prod
        # 高リスクの脆弱性チェック
        - |
          if npm audit --audit-level high --json | jq -e '.vulnerabilities | length > 0'; then
            echo "High-risk vulnerabilities found. Please review and fix."
            npm audit --audit-level high
            exit 1
          fi
backend:
  phases:
    preBuild:
      commands:
        - cd backend
        # Maven依存関係のセキュリティチェック
        - ./mvnw org.owasp:dependency-check-maven:check
```

## モニタリングとアラート

### 1. ビルド時間の監視
```yaml
frontend:
  phases:
    preBuild:
      commands:
        - export BUILD_START_TIME=$(date +%s)
        - echo "Build started at: $(date)"
    build:
      commands:
        - export COMPILE_START_TIME=$(date +%s)
        - cd frontend && npm run build
        - export COMPILE_END_TIME=$(date +%s)
        - echo "Compile time: $((COMPILE_END_TIME - COMPILE_START_TIME)) seconds"
    postBuild:
      commands:
        - export BUILD_END_TIME=$(date +%s)
        - export TOTAL_BUILD_TIME=$((BUILD_END_TIME - BUILD_START_TIME))
        - echo "Total build time: $TOTAL_BUILD_TIME seconds"
        # ビルド時間が長すぎる場合の警告
        - |
          if [ $TOTAL_BUILD_TIME -gt 600 ]; then
            echo "WARNING: Build time exceeded 10 minutes"
            # Slack通知などの処理
          fi
```

### 2. リソース使用量の監視
```yaml
frontend:
  phases:
    preBuild:
      commands:
        # 初期リソース状態の記録
        - echo "Initial disk usage: $(df -h . | tail -1)"
        - echo "Initial memory usage: $(free -h | grep Mem)"
    build:
      commands:
        # ビルド中のリソース監視
        - cd frontend
        - |
          # バックグラウンドでリソース監視
          (
            while true; do
              echo "$(date): Disk: $(df -h . | tail -1 | awk '{print $4}') Memory: $(free -h | grep Mem | awk '{print $7}')"
              sleep 30
            done
          ) &
          MONITOR_PID=$!
          
          # ビルド実行
          npm run build
          
          # 監視プロセス終了
          kill $MONITOR_PID 2>/dev/null || true
    postBuild:
      commands:
        # 最終リソース状態の確認
        - echo "Final disk usage: $(df -h . | tail -1)"
        - echo "Final memory usage: $(free -h | grep Mem)"
```

## 高度な設定パターン

### 1. 条件付きビルドステップ
```yaml
frontend:
  phases:
    build:
      commands:
        # ファイル変更に基づく条件付きビルド
        - cd frontend
        - |
          if git diff --name-only HEAD~1 HEAD | grep -E '\.(js|jsx|ts|tsx)$'; then
            echo "JavaScript/TypeScript files changed, running full build"
            npm run build:full
          elif git diff --name-only HEAD~1 HEAD | grep -E '\.(css|scss|sass)$'; then
            echo "Style files changed, running style build"
            npm run build:styles
          else
            echo "No significant changes, running minimal build"
            npm run build:minimal
          fi
```

### 2. 多段階ビルド
```yaml
frontend:
  phases:
    build:
      commands:
        - cd frontend
        # Stage 1: 依存関係の解決
        - echo "=== Stage 1: Dependency Resolution ==="
        - npm run resolve:dependencies
        
        # Stage 2: コンパイル
        - echo "=== Stage 2: Compilation ==="
        - npm run compile
        
        # Stage 3: 最適化
        - echo "=== Stage 3: Optimization ==="
        - npm run optimize
        
        # Stage 4: バンドル
        - echo "=== Stage 4: Bundling ==="
        - npm run bundle
        
        # Stage 5: 検証
        - echo "=== Stage 5: Validation ==="
        - npm run validate:build
```

### 3. カスタムビルドツールの統合
```yaml
frontend:
  phases:
    preBuild:
      commands:
        # カスタムビルドツールのインストール
        - npm install -g @custom/build-tool
        - custom-build-tool --version
    build:
      commands:
        - cd frontend
        # カスタムビルドツールを使用したビルド
        - custom-build-tool build --config amplify.config.js
        # 結果の検証
        - custom-build-tool verify --output build/
```

## トラブルシューティング用の設定

### 1. デバッグモードの有効化
```yaml
frontend:
  phases:
    preBuild:
      commands:
        # デバッグモードの条件付き有効化
        - |
          if [ "$AWS_BRANCH" = "debug" ] || [ "$ENABLE_DEBUG" = "true" ]; then
            export DEBUG=true
            export VERBOSE=true
            set -x  # コマンドの詳細表示
            echo "Debug mode enabled"
          fi
    build:
      commands:
        - cd frontend
        # デバッグ情報付きビルド
        - |
          if [ "$DEBUG" = "true" ]; then
            npm run build:debug
          else
            npm run build
          fi
```

### 2. 失敗時の情報収集
```yaml
frontend:
  phases:
    build:
      commands:
        - cd frontend
        # ビルド実行とエラー時の情報収集
        - |
          if ! npm run build; then
            echo "=== BUILD FAILURE ANALYSIS ==="
            echo "Environment variables:"
            env | grep -E '^(NODE_|NPM_|AWS_)' | sort
            echo "Package versions:"
            npm list --depth=0 || true
            echo "Recent git commits:"
            git log --oneline -5 || true
            echo "File system state:"
            find . -name "*.log" -exec echo "=== {} ===" \; -exec cat {} \; || true
            echo "System resources:"
            df -h
            free -h
            exit 1
          fi
```

## まとめ

効果的なamplify.yml設定のために：

1. **環境別設定**を適切に管理
2. **詳細なログ出力**でデバッグを容易に
3. **パフォーマンス最適化**でビルド時間を短縮
4. **セキュリティ**を考慮した機密情報の管理
5. **モニタリング**でビルドプロセスを可視化
6. **エラーハンドリング**で問題の迅速な特定

これらのベストプラクティスを適用することで、安定したAmplifyデプロイメントを実現できます。