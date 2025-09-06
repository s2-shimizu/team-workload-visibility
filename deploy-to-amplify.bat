@echo off
echo ========================================
echo AWS Amplify デプロイメント準備スクリプト
echo ========================================

echo.
echo 1. Gitリポジトリの状態確認...
git status

echo.
echo 2. 変更をコミット...
set /p commit_message="コミットメッセージを入力してください: "
git add .
git commit -m "%commit_message%"

echo.
echo 3. GitHubにプッシュ...
git push origin main

echo.
echo ========================================
echo デプロイ準備完了！
echo.
echo 次の手順:
echo 1. AWS Amplifyコンソールにアクセス
echo 2. GitHubリポジトリを連携
echo 3. 自動デプロイを開始
echo.
echo Amplifyコンソール: https://console.aws.amazon.com/amplify/
echo ========================================

pause