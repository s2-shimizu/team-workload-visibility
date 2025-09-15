# リアルタイム更新機能

チーム状況ダッシュボードにリアルタイム更新機能を実装しました。WebSocketを使用して、データの変更を即座に全クライアントに反映します。

## 🚀 機能概要

### 実装された機能
- **負荷状況のリアルタイム更新**: メンバーが負荷状況を更新すると、即座に全員のダッシュボードに反映
- **困りごとのリアルタイム通知**: 新しい困りごとの投稿、解決、再オープンを即座に通知
- **接続状態の可視化**: WebSocket接続状態をヘッダーに表示
- **自動再接続**: 接続が切れた場合の自動復旧機能
- **アニメーション効果**: 更新時の視覚的フィードバック

### リアルタイム通知の種類
1. **負荷状況更新** (`WORKLOAD_STATUS_UPDATE`)
2. **困りごと投稿** (`TEAM_ISSUE_CREATED`)
3. **困りごと解決** (`TEAM_ISSUE_RESOLVED`)
4. **困りごと再オープン** (`TEAM_ISSUE_REOPENED`)
5. **困りごと削除** (`TEAM_ISSUE_DELETED`)
6. **システムメッセージ** (`SYSTEM_MESSAGE`)
7. **ユーザー接続状態** (`USER_CONNECTION_STATUS`)

## 🔧 技術仕様

### バックエンド
- **WebSocket**: Spring Boot WebSocket + STOMP
- **メッセージブローカー**: Simple Broker
- **エンドポイント**: `/ws` (SockJS対応)
- **トピック**:
  - `/topic/workload-updates`: 負荷状況更新
  - `/topic/issue-updates`: 困りごと更新
  - `/topic/system-messages`: システムメッセージ
  - `/topic/user-status`: ユーザー状態

### フロントエンド
- **ライブラリ**: SockJS + STOMP.js
- **自動再接続**: 指数バックオフ方式
- **UI更新**: DOM操作 + CSS アニメーション
- **通知システム**: カスタム通知コンポーネント

## 📱 ユーザーインターフェース

### 接続状態インジケーター
ヘッダー右上に表示される接続状態:
- 🟢 **リアルタイム**: WebSocket接続中
- 🔴 **オフライン**: 接続切断中
- 🟡 **接続中...**: 接続試行中

### 更新アニメーション
- **緑色の枠線**: 更新されたアイテム
- **フェードイン**: 新しく追加されたアイテム
- **スライドアウト**: 削除されたアイテム

### 通知メッセージ
画面右上に表示される通知:
- **成功**: 緑色背景
- **情報**: 青色背景
- **警告**: オレンジ色背景
- **エラー**: 赤色背景

## 🛠️ 開発・デプロイ

### 必要な依存関係

#### バックエンド (pom.xml)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

#### フロントエンド (HTML)
```html
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1.6.1/dist/sockjs.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7.0.0/bundles/stomp.umd.min.js"></script>
```

### 設定ファイル

#### WebSocket設定 (WebSocketConfig.java)
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    // 設定内容は実装済み
}
```

#### リアルタイム通知サービス (RealtimeNotificationService.java)
```java
@Service
public class RealtimeNotificationService {
    // 各種通知メソッドを実装済み
}
```

## 🧪 テスト方法

### 1. 手動テスト
1. 複数のブラウザタブでダッシュボードを開く
2. 一つのタブで負荷状況を更新
3. 他のタブで即座に反映されることを確認

### 2. 自動テスト
```powershell
# リアルタイム更新テストスクリプトを実行
.\test-realtime-updates.ps1

# カスタム設定でテスト
.\test-realtime-updates.ps1 -IntervalSeconds 3 -TestDurationMinutes 5
```

### 3. 接続テスト
```javascript
// ブラウザ開発者ツールで実行
console.log('WebSocket状態:', realtimeClient.getConnectionStatus());

// イベントハンドラーを追加
realtimeClient.on('workload-update', (data) => {
    console.log('負荷状況更新:', data);
});
```

## 🔍 トラブルシューティング

### よくある問題と解決方法

#### 1. WebSocket接続エラー
**症状**: 接続状態が「🔴 オフライン」のまま
**原因**: 
- サーバーが起動していない
- ファイアウォールでWebSocketがブロックされている
- プロキシ設定の問題

**解決方法**:
```javascript
// 接続URLを確認
console.log('WebSocket URL:', realtimeClient.getWebSocketUrl());

// 手動で再接続を試行
realtimeClient.connect();
```

#### 2. 通知が表示されない
**症状**: データは更新されるが通知が出ない
**原因**: 通知システムの初期化エラー

**解決方法**:
```javascript
// 通知システムを手動でテスト
showNotification('テスト通知', 'info');
```

#### 3. 更新が反映されない
**症状**: 他のクライアントで更新が見えない
**原因**: トピック購読の失敗

**解決方法**:
```javascript
// 購読状態を確認
console.log('接続状態:', realtimeClient.isConnected);

// 再接続を実行
realtimeClient.disconnect();
setTimeout(() => realtimeClient.connect(), 1000);
```

### デバッグ方法

#### 1. ブラウザ開発者ツール
```javascript
// WebSocketメッセージをログ出力
realtimeClient.stompClient.debug = console.log;

// イベントハンドラーを追加してデバッグ
realtimeClient.on('workload-update', console.log);
realtimeClient.on('issue-update', console.log);
```

#### 2. サーバーログ
```bash
# Spring Bootログでメッセージ送信を確認
tail -f logs/application.log | grep "STOMP"
```

#### 3. ネットワークタブ
- ブラウザの開発者ツール > ネットワークタブ
- WebSocketセクションでメッセージを確認

## 🚀 本番環境での考慮事項

### 1. スケーラビリティ
- **Redis**: 複数サーバー間でのメッセージ共有
- **ロードバランサー**: Sticky Sessionの設定
- **接続数制限**: 同時接続数の監視

### 2. セキュリティ
- **認証**: WebSocket接続時の認証確認
- **CORS**: 適切なオリジン設定
- **レート制限**: メッセージ送信頻度の制限

### 3. 監視
- **接続数**: アクティブなWebSocket接続数
- **メッセージ量**: 送受信メッセージの監視
- **エラー率**: 接続エラーの追跡

## 📚 API リファレンス

### JavaScript API

#### RealtimeClient クラス
```javascript
// インスタンス取得
const client = window.realtimeClient;

// 接続
client.connect();

// 切断
client.disconnect();

// イベントハンドラー登録
client.on('workload-update', (data) => { /* 処理 */ });

// 接続状態確認
const status = client.getConnectionStatus();
```

#### イベントデータ形式
```javascript
// 負荷状況更新
{
    type: "WORKLOAD_STATUS_UPDATE",
    userId: "user123",
    displayName: "田中太郎",
    workloadLevel: "HIGH",
    projectCount: 5,
    taskCount: 25,
    timestamp: 1640995200000
}

// 困りごと投稿
{
    type: "TEAM_ISSUE_CREATED",
    issueId: "issue-456",
    userId: "user123",
    displayName: "田中太郎",
    content: "困りごとの内容",
    priority: "HIGH",
    status: "OPEN",
    timestamp: 1640995200000
}
```

## 🔄 今後の拡張予定

### Phase 2: 高度な機能
- [ ] **プライベートメッセージ**: 1対1のリアルタイムチャット
- [ ] **画面共有**: リアルタイムでの画面共有機能
- [ ] **音声通知**: 重要な更新時の音声アラート
- [ ] **モバイル対応**: プッシュ通知との連携

### Phase 3: 分析機能
- [ ] **リアルタイム分析**: 負荷状況の傾向をリアルタイム表示
- [ ] **アクティビティフィード**: 全ての更新履歴の表示
- [ ] **統計ダッシュボード**: チーム活動の可視化

---

## 📞 サポート

リアルタイム更新機能に関する質問や問題がある場合は、以下の方法でサポートを受けられます:

1. **ログ確認**: ブラウザの開発者ツールでエラーを確認
2. **テストスクリプト実行**: `test-realtime-updates.ps1` でシステム状態を確認
3. **設定確認**: WebSocket設定とネットワーク環境を確認

リアルタイム更新により、チームの状況をより迅速に把握し、効果的なコラボレーションが可能になります。