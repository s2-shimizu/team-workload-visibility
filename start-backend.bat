@echo off
setlocal enabledelayedexpansion

echo ========================================
echo チーム状況ダッシュボード - バックエンド起動
echo ========================================

echo.
echo 1. Javaバージョン確認...
java -version 2>nul
if %errorlevel% neq 0 (
    echo ❌ エラー: Javaがインストールされていません
    echo Java 17以上をインストールしてください
    echo ダウンロード: https://adoptium.net/
    pause
    exit /b 1
) else (
    echo ✅ Java: インストール済み
)

echo.
echo 2. Mavenバージョン確認...
mvn -version 2>nul
if %errorlevel% neq 0 (
    echo ❌ エラー: Mavenがインストールされていません
    echo Apache Mavenをインストールしてください
    echo ダウンロード: https://maven.apache.org/download.cgi
    pause
    exit /b 1
) else (
    echo ✅ Maven: インストール済み
)

echo.
echo 3. PostgreSQL確認...
call setup-postgresql.bat
if %errorlevel% neq 0 (
    echo PostgreSQLのセットアップに失敗しました
    pause
    exit /b 1
)

echo.
echo 4. バックエンドディレクトリに移動...
if not exist "%~dp0backend" (
    echo ❌ エラー: backendディレクトリが見つかりません
    pause
    exit /b 1
)
cd /d "%~dp0backend"

echo.
echo 5. 依存関係のダウンロードとコンパイル...
echo これには数分かかる場合があります...
mvn clean compile -q
if %errorlevel% neq 0 (
    echo ❌ エラー: コンパイルに失敗しました
    echo 詳細なエラーを確認するには: mvn clean compile
    pause
    exit /b 1
) else (
    echo ✅ コンパイル: 成功
)

echo.
echo 6. Spring Bootアプリケーション起動...
echo サーバーは http://localhost:8081 で起動します
echo 起動には30秒程度かかります...
echo 停止するには Ctrl+C を押してください
echo.
echo ========================================
echo 起動中... しばらくお待ちください
echo ========================================
mvn spring-boot:run

pause