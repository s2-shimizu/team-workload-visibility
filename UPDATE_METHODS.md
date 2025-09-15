# 📡 更新方法ガイド - WebSocket vs ポーリング

チーム状況ダッシュボードでは、環境に応じて2つの更新方法を自動選択します。

## 🔄 更新方法の種類

### 1. **WebSocket（リアルタイム更新）** 🚀
**ECS Fargate・EC2環境で利用可能**

#### 特徴
- ✅ **即座に反映**: データ変更が瞬時に全クライアントに反映
- ✅ **双方向通信**: サーバーからクライアントへのプッシュ通知
- ✅ **効率的**: 変更があった時のみ通信
- ✅ **アニメーション**: 更新時の視覚的フィードバック

#### 動作方式
```
ユーザーA: 負荷状況更新 → サーバー → 即座に全ユーザーに通知
ユーザーB: 画面に即座に反映 ⚡
ユーザーC: 画面に即座に反映 ⚡
```

#### 接続状態表示
- 🟢 **リアルタイム**: WebSocket接続中
- 🔴 **オフライン**: 接続切断中

---

### 2. **ポーリング（定期更新）** 🔄
**Lambda環境・WebSocket制限環境で利用可能**

#### 特徴
- ✅ **確実な動作**: どの環境でも動作
- ✅ **自動調整**: ページ状態に応じて更新間隔を調整
- ✅ **手動更新**: 必要時に即座に更新可能
- ⚠️ **遅延あり**: 最大30秒の更新遅延

#### 動作方式
```
クライアント: 30秒ごとにサーバーにデータ要求
サーバー: 最新データを返送
クライアント: 変更があれば画面を更新
```

#### 更新間隔の自動調整
- **通常**: 30秒間隔
- **ページ非表示**: 60秒間隔
- **ユーザー非アクティブ**: 90秒間隔
- **手動更新後**: 5秒間隔（30秒間）

#### 接続状態表示
- 🔄 **定期更新**: ポーリング動作中
- 🔴 **オフライン**: 更新停止中

---

## 🤖 自動切り替え機能

### 初期化プロセス
1. **WebSocket接続試行**（5秒タイムアウト）
2. **成功** → WebSocketモード
3. **失敗** → ポーリングモード

### フォールバック機能
- WebSocket切断時 → 自動的にポーリングに切り替え
- 復旧時 → WebSocketへの復帰を試行

### 環境別の動作

| 環境 | 初期選択 | フォールバック | 手動切り替え |
|------|----------|----------------|--------------|
| **ECS Fargate** | WebSocket | ポーリング | ✅ |
| **EC2** | WebSocket | ポーリング | ✅ |
| **Lambda** | ポーリング | - | ❌ |

---

## 🎛️ ユーザーインターフェース

### 接続状態インジケーター
ヘッダー右上に表示される現在の更新方法:

```
🟢 リアルタイム  ← WebSocket接続中
🔄 定期更新      ← ポーリング動作中  
🔴 オフライン    ← 更新停止中
```

### 手動更新ボタン
接続状態の隣にある 🔄 ボタン:
- **クリック**: 即座にデータ更新
- **WebSocket時**: データ再取得
- **ポーリング時**: 更新間隔を一時的に短縮

---

## 📊 パフォーマンス比較

| 項目 | WebSocket | ポーリング |
|------|-----------|------------|
| **更新速度** | 即座（<1秒） | 最大30秒 |
| **サーバー負荷** | 低 | 中 |
| **ネットワーク使用量** | 低 | 中 |
| **バッテリー消費** | 低 | 中 |
| **環境対応** | 限定的 | 全環境 |

---

## 🔧 開発者向け情報

### JavaScript API

#### UpdateManager
```javascript
// 現在の更新方法を確認
console.log(updateManager.getUpdateMethod()); // 'websocket' | 'polling'

// 状態情報を取得
console.log(updateManager.getStatus());

// 手動更新を実行
await updateManager.manualUpdate();

// イベントハンドラーを登録
updateManager.on('workload-update', (data) => {
    console.log('負荷状況更新:', data);
});
```

#### WebSocketクライアント
```javascript
// WebSocket接続状態
console.log(realtimeClient.getConnectionStatus());

// 手動接続
realtimeClient.connect();

// 切断
realtimeClient.disconnect();
```

#### ポーリングクライアント
```javascript
// ポーリング状態
console.log(pollingClient.getStatus());

// 手動更新
await pollingClient.manualUpdate();

// 更新間隔変更
pollingClient.setUpdateInterval(10000); // 10秒
```

### 設定カスタマイズ

#### ポーリング間隔の調整
```javascript
// polling-client.js で設定
this.updateInterval = 30000; // 通常間隔（30秒）
this.fastUpdateInterval = 5000; // 高速間隔（5秒）
```

#### WebSocket接続タイムアウト
```javascript
// update-manager.js で設定
this.connectionTimeout = 5000; // 5秒
this.fallbackDelay = 3000; // フォールバック遅延
```

---

## 🧪 テスト方法

### WebSocket機能テスト
```powershell
# リアルタイム更新テスト（ECS/EC2環境）
.\test-realtime-updates.ps1 -BaseUrl "https://your-endpoint"
```

### ポーリング機能テスト
```powershell
# 定期更新テスト（Lambda環境）
.\test-polling-updates.ps1 -BaseUrl "https://your-endpoint"
```

### 手動テスト手順
1. **複数タブでダッシュボードを開く**
2. **接続状態を確認**（🟢 または 🔄）
3. **一つのタブでデータを更新**
4. **他のタブでの反映を確認**
   - WebSocket: 即座に反映
   - ポーリング: 最大30秒で反映

---

## 🔍 トラブルシューティング

### よくある問題

#### 1. 更新が反映されない
**症状**: データ変更が他のクライアントに反映されない

**WebSocket環境での対処**:
```javascript
// 接続状態を確認
console.log(realtimeClient.getConnectionStatus());

// 再接続を試行
realtimeClient.disconnect();
setTimeout(() => realtimeClient.connect(), 1000);
```

**ポーリング環境での対処**:
```javascript
// 手動更新を実行
await updateManager.manualUpdate();

// ポーリング状態を確認
console.log(pollingClient.getStatus());
```

#### 2. 接続状態が「オフライン」
**原因と対処**:
- **ネットワーク問題**: インターネット接続を確認
- **サーバー問題**: サーバーの稼働状況を確認
- **ブラウザ問題**: ページを再読み込み

#### 3. 更新が遅い
**ポーリング環境での対処**:
- 手動更新ボタン（🔄）をクリック
- ページをアクティブにする
- ブラウザタブを前面に表示

### デバッグコマンド

```javascript
// 全体の状態確認
console.log('UpdateManager:', updateManager.getStatus());
console.log('WebSocket:', realtimeClient?.getConnectionStatus());
console.log('Polling:', pollingClient?.getStatus());

// 強制的にポーリングモードに切り替え
updateManager.fallbackToPolling();

// WebSocket復帰を試行
updateManager.tryReconnectWebSocket();
```

---

## 📈 最適化のヒント

### ユーザー向け
1. **アクティブに使用**: ページを前面に表示して更新頻度を上げる
2. **手動更新**: 🔄 ボタンで即座に最新データを取得
3. **複数タブ**: 複数タブで開いて更新を確認

### 開発者向け
1. **適切な環境選択**: リアルタイム性が重要ならECS/EC2を選択
2. **更新間隔調整**: 用途に応じてポーリング間隔をカスタマイズ
3. **エラーハンドリング**: 接続エラー時の適切なフォールバック実装

---

## 🚀 今後の拡張予定

### Phase 2: 高度な更新機能
- [ ] **Server-Sent Events (SSE)**: WebSocketの代替手段
- [ ] **Progressive Web App**: オフライン対応
- [ ] **Background Sync**: バックグラウンド同期

### Phase 3: パフォーマンス最適化
- [ ] **差分更新**: 変更部分のみ更新
- [ ] **キャッシュ戦略**: 効率的なデータキャッシュ
- [ ] **圧縮**: データ転送量の最適化

---

この更新システムにより、どの環境でも最適なユーザー体験を提供できます。WebSocketが利用できない環境でも、ポーリングによる確実な更新機能で、チームの状況を常に最新に保つことができます。