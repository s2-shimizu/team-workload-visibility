@echo off
echo ========================================
echo チーム状況ダッシュボード - クイックスタート
echo ========================================

echo.
echo 環境確認済み:
echo ✅ Java 17.0.12
echo ✅ Maven 3.9.11  
echo ✅ PostgreSQL 17.6 (起動中)

echo.
echo データベース選択:
echo 1. PostgreSQL使用 (本格運用)
echo 2. H2データベース使用 (簡単テスト)
echo.
set /p choice="選択してください (1 または 2): "

if "%choice%"=="1" goto postgresql_start
if "%choice%"=="2" goto h2_start

echo 無効な選択です
pause
exit /b 1

:postgresql_start
echo.
echo PostgreSQLデータベースでバックエンドを起動します...
cd /d "%~dp0backend"
echo.
echo 依存関係のダウンロード中...
mvn clean compile -q
echo.
echo Spring Boot起動中...
echo サーバー: http://localhost:8081/api
start "チーム状況ダッシュボード - バックエンド" cmd /k "mvn spring-boot:run"
goto start_frontend

:h2_start
echo.
echo H2データベースでバックエンドを起動します...
cd /d "%~dp0backend"
echo.
echo 依存関係のダウンロード中...
mvn clean compile -q
echo.
echo Spring Boot起動中 (H2データベース)...
echo サーバー: http://localhost:8081/api
echo H2コンソール: http://localhost:8081/api/h2-console
start "チーム状況ダッシュボード - バックエンド (H2)" cmd /k "mvn spring-boot:run -Dspring-boot.run.profiles=h2"
goto start_frontend

:start_frontend
echo.
echo バックエンドの起動を待機中...
timeout /t 15 /nobreak

echo.
echo フロントエンドを起動します...
cd /d "%~dp0"
start "" "%~dp0frontend\index.html"

echo.
echo ========================================
echo 🎉 アプリケーション起動完了！
echo.
echo 📊 アクセス先:
echo - フロントエンド: ブラウザで自動的に開かれます
echo - バックエンドAPI: http://localhost:8081/api
echo.
echo 🎯 使い方:
echo 1. ダッシュボードでチーム状況確認
echo 2. 日報投稿で作業内容・負荷レベル入力  
echo 3. カレンダーで履歴確認
echo.
echo 停止: バックエンドウィンドウでCtrl+C
echo ========================================
pause