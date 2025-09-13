@echo off
echo ========================================
echo チーム状況ダッシュボード - フロントエンド起動
echo ========================================

echo.
echo バックエンドが起動していることを確認してください
echo バックエンドURL: http://localhost:8081
echo.

echo フロントエンドをブラウザで開きます...
echo URL: %~dp0frontend\index.html
echo.

start "" "%~dp0frontend\index.html"

echo.
echo フロントエンドがブラウザで開かれました
echo バックエンドとの通信にはCORSが設定済みです
echo.
pause