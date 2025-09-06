@echo off
echo ========================================
echo チーム状況ダッシュボード - 起動状況確認
echo ========================================

echo.
echo 1. PostgreSQL確認...
sc query postgresql-x64-14 | find "RUNNING" >nul
if %errorlevel% equ 0 (
    echo ✅ PostgreSQL: 起動中
) else (
    echo ❌ PostgreSQL: 停止中
)

echo.
echo 2. Javaプロセス確認...
tasklist | find "java.exe" >nul
if %errorlevel% equ 0 (
    echo ✅ Java: プロセス実行中
) else (
    echo ❌ Java: プロセスなし
)

echo.
echo 3. バックエンドAPI確認...
powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8081/api/reports' -TimeoutSec 3 -UseBasicParsing; Write-Host '✅ バックエンドAPI: 正常応答'; exit 0 } catch { Write-Host '❌ バックエンドAPI: 応答なし'; exit 1 }" 2>nul

echo.
echo 4. ポート使用状況確認...
netstat -an | find ":8081" >nul
if %errorlevel% equ 0 (
    echo ✅ ポート8081: 使用中
) else (
    echo ❌ ポート8081: 未使用
)

echo.
echo 5. フロントエンドファイル確認...
if exist "%~dp0frontend\index.html" (
    echo ✅ フロントエンド: ファイル存在
) else (
    echo ❌ フロントエンド: ファイルなし
)

echo.
echo ========================================
echo 手動起動コマンド:
echo - バックエンド: start-backend.bat
echo - フロントエンド: start-frontend.bat
echo - 一括起動: start-app-advanced.bat
echo ========================================
pause