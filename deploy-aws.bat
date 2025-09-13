@echo off
echo ========================================
echo AWS デプロイメント開始
echo ========================================

echo.
echo 前提条件チェック...

REM AWS CLI確認
aws --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ エラー: AWS CLIがインストールされていません
    echo AWS CLIをインストールしてください: https://aws.amazon.com/cli/
    pause
    exit /b 1
) else (
    echo ✅ AWS CLI: インストール済み
)

REM SAM CLI確認
sam --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ エラー: SAM CLIがインストールされていません
    echo SAM CLIをインストールしてください: https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html
    pause
    exit /b 1
) else (
    echo ✅ SAM CLI: インストール済み
)

REM Java確認
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ エラー: Java 17がインストールされていません
    pause
    exit /b 1
) else (
    echo ✅ Java: インストール済み
)

echo.
echo ========================================
echo ステップ1: バックエンドビルド
echo ========================================

cd backend
echo Maven ビルド実行中...
call mvnw clean package -DskipTests
if %errorlevel% neq 0 (
    echo ❌ エラー: Mavenビルドに失敗しました
    cd ..
    pause
    exit /b 1
)
cd ..

echo ✅ バックエンドビルド完了

echo.
echo ========================================
echo ステップ2: SAMデプロイ
echo ========================================

echo SAMビルド実行中...
sam build
if %errorlevel% neq 0 (
    echo ❌ エラー: SAMビルドに失敗しました
    pause
    exit /b 1
)

echo SAMデプロイ実行中...
sam deploy --guided
if %errorlevel% neq 0 (
    echo ❌ エラー: SAMデプロイに失敗しました
    pause
    exit /b 1
)

echo ✅ バックエンドデプロイ完了

echo.
echo ========================================
echo ステップ3: フロントエンドデプロイ準備
echo ========================================

echo API Gateway URLを取得中...
for /f "tokens=*" %%i in ('aws cloudformation describe-stacks --stack-name team-dashboard --query "Stacks[0].Outputs[?OutputKey=='ApiGatewayEndpoint'].OutputValue" --output text') do set API_URL=%%i

if "%API_URL%"=="" (
    echo ❌ 警告: API Gateway URLの取得に失敗しました
    echo 手動でAPI URLを設定してください
) else (
    echo ✅ API Gateway URL: %API_URL%
    
    REM フロントエンドのAPI URLを更新
    powershell -Command "(Get-Content frontend/aws-index.html) -replace 'https://your-api-gateway-url.execute-api.ap-northeast-1.amazonaws.com/dev', '%API_URL%' | Set-Content frontend/aws-index.html"
    echo ✅ フロントエンドのAPI URL更新完了
)

echo.
echo ========================================
echo デプロイ完了情報
echo ========================================

echo.
echo 🎉 バックエンドデプロイが完了しました！
echo.
echo 📋 次のステップ:
echo 1. AWS Amplifyコンソールでフロントエンドアプリを作成
echo 2. GitHubリポジトリを接続
echo 3. ビルド設定でamplify.ymlを使用
echo 4. デプロイ実行
echo.
echo 🔗 便利なリンク:
echo - AWS Amplify Console: https://console.aws.amazon.com/amplify/
echo - API Gateway Console: https://console.aws.amazon.com/apigateway/
echo - DynamoDB Console: https://console.aws.amazon.com/dynamodb/
echo - CloudWatch Logs: https://console.aws.amazon.com/cloudwatch/
echo.

if not "%API_URL%"=="" (
    echo 🌐 API エンドポイント: %API_URL%
    echo.
    echo 📝 テスト用コマンド:
    echo curl "%API_URL%workload-status"
    echo curl "%API_URL%team-issues"
    echo.
)

echo ========================================
pause