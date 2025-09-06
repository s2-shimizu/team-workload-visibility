@echo off
echo ========================================
echo チーム状況ダッシュボード - 一括起動
echo ========================================

echo.
echo PostgreSQLサービスを開始しています...
net start postgresql-x64-14 2>nul
if %errorlevel% equ 0 (
    echo PostgreSQLサービスが開始されました
) else (
    echo PostgreSQLサービスの開始に失敗しました（既に起動している可能性があります）
)

echo.
echo バックエンドを起動しています...
start "チーム状況ダッシュボード - バックエンド" cmd /k "cd /d "%~dp0backend" && mvn spring-boot:run"

echo.
echo バックエンドの起動を待機しています...
echo サーバーが起動するまで30秒待機します...

:wait_loop
set /a counter=0
:check_server
set /a counter+=1
echo 起動確認中... (%counter%/30)

REM バックエンドの起動確認（簡易版）
timeout /t 1 /nobreak >nul

if %counter% geq 30 goto start_frontend
goto check_server

:start_frontend
echo.
echo フロントエンドを起動しています...
start "" "%~dp0frontend\index.html"

echo.
echo アプリケーションが起動しました！
echo - バックエンド: http://localhost:8081
echo - フロントエンド: ブラウザで自動的に開かれます
echo.
echo 各機能の確認:
echo 1. ダッシュボードでチーム状況を確認
echo 2. 日報投稿で新しい日報を作成
echo 3. カレンダーで過去の日報を確認
echo.
echo 停止するには各ウィンドウでCtrl+Cを押してください
pause