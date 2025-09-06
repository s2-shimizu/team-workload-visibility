@echo off
echo ========================================
echo DynamoDB版バックエンド起動スクリプト
echo ========================================

echo.
echo DynamoDB Localが起動していることを確認してください
echo ポート8000でDynamoDB Localが動作している必要があります

echo.
echo バックエンドを起動中...
cd /d "%~dp0backend"

echo.
echo Spring Boot起動中 (DynamoDBプロファイル)...
echo サーバー: http://localhost:8080
echo ヘルスチェック: http://localhost:8080/actuator/health
echo.

mvn spring-boot:run -Dspring-boot.run.profiles=dynamodb

pause