# チーム状況可視化アプリ

## 概要
チームメンバーのタスク状況や困りごとを共有・可視化するWebアプリケーション

## 機能
- 日報・週報投稿機能
- タスクの負担感・忙しさの自己申告
- 案件横断のタスク一覧表示
- コメント・リアクション機能
- カレンダー表示

## 技術スタック
- バックエンド: Spring Boot (Java)
- フロントエンド: HTML/CSS/JavaScript
- データベース: PostgreSQL
- 認証: Spring Security

## 開発環境
- Java 17+
- Maven
- PostgreSQL

## セットアップ
1. PostgreSQLをインストール・起動
2. バックエンドの起動: `cd backend && mvn spring-boot:run`
3. フロントエンドの起動: ブラウザで `frontend/index.html` を開く

## API仕様
- GET /api/reports - 日報一覧取得
- POST /api/reports - 日報投稿
- GET /api/users - ユーザー一覧
- POST /api/workload - 負担感登録