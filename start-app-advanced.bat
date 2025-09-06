@echo off
setlocal enabledelayedexpansion

echo ========================================
echo チーム状況ダッシュボード - 高度な一括起動
echo ========================================

echo.
echo 1. PostgreSQLサービス確認...
sc query postgresql-x64-14 | find "RUNNING" >nul
if %errorlevel% equ 0 (
    echo PostgreSQLは既に起動しています
) else (
    echo PostgreSQLを起動しています...
    net start postgresql-x64-14 2>nul
    if !errorlevel! equ 0 (
        echo PostgreSQLサービスが開始されました
    ) else (
        echo 警告: PostgreSQLの起動に失敗しました
        echo 手動でPostgreSQLを起動してください
    )
)

echo.
echo 2. 既存のJavaプロセス確認...
tasklist | find "java.exe" >nul
if %errorlevel% equ 0 (
    echo 既存のJavaプロセスが検出されました
    echo 既にバックエンドが起動している可能性があります
)

echo.
echo 3. バックエンドを起動しています...
cd /d "%~dp0backend"
start "チーム状況ダッシュボード - バックエンド" cmd /k "mvn spring-boot:run"

echo.
echo 4. バックエンドの起動を待機しています...
cd /d "%~dp0"

set /a attempts=0
set max_attempts=60

:wait_for_backend
set /a attempts+=1
echo 起動確認中... (%attempts%/%max_attempts%)

REM curlまたはpowershellでヘルスチェック
powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8081/api/reports' -TimeoutSec 2 -UseBasicParsing; exit 0 } catch { exit 1 }" >nul 2>&1

if %errorlevel% equ 0 (
    echo バックエンドが正常に起動しました！
    goto start_frontend
)

if %attempts% geq %max_attempts% (
    echo タイムアウト: バックエンドの起動に時間がかかっています
    echo 手動でブラウザを開いてください: %~dp0frontend\index.html
    goto end
)

timeout /t 1 /nobreak >nul
goto wait_for_backend

:start_frontend
echo.
echo 5. フロントエンドを起動しています...
start "" "%~dp0frontend\index.html"

echo.
echo ✅ アプリケーションが正常に起動しました！
echo.
echo 📊 アクセス情報:
echo - バックエンドAPI: http://localhost:8081/api
echo - フロントエンド: ブラウザで自動的に開かれます
echo.
echo 🎯 使用方法:
echo 1. ダッシュボード: チーム全体の状況を確認
echo 2. 日報投稿: 今日の作業内容と負荷レベルを入力
echo 3. カレンダー: 過去の日報履歴を確認
echo.
echo 🛑 停止方法:
echo - バックエンド: バックエンドウィンドウでCtrl+C
echo - フロントエンド: ブラウザを閉じる
echo.

:end
pause