@echo off
echo ===================================
echo 本番環境 Cognito認証テスト実行
echo ===================================
echo.

REM 設定ファイルの確認
if exist "production-auth-config.json" (
    echo 設定ファイルが見つかりました: production-auth-config.json
    echo.
) else (
    echo 警告: 設定ファイルが見つかりません。
    echo 最初にセットアップスクリプトを実行してください:
    echo   powershell -ExecutionPolicy Bypass -File setup-production-auth-test.ps1
    echo.
    pause
    exit /b 1
)

REM ユーザー入力
set /p TEST_EMAIL="テストユーザーのメールアドレスを入力してください: "
if "%TEST_EMAIL%"=="" (
    echo エラー: メールアドレスが入力されていません。
    pause
    exit /b 1
)

set /p TEST_PASSWORD="テストユーザーのパスワードを入力してください: "
if "%TEST_PASSWORD%"=="" (
    echo エラー: パスワードが入力されていません。
    pause
    exit /b 1
)

echo.
echo テスト実行中...
echo.

REM PowerShellスクリプトの実行
powershell -ExecutionPolicy Bypass -File test-auth-production.ps1 -Environment prod -TestUserEmail "%TEST_EMAIL%" -TestUserPassword "%TEST_PASSWORD%"

echo.
echo テスト完了。結果を確認してください。
echo.
pause