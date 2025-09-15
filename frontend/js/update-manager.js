/**
 * 更新マネージャー - WebSocketとポーリングの自動切り替え
 * 環境に応じて最適な更新方法を選択
 */

class UpdateManager {
    constructor() {
        this.updateMethod = null; // 'websocket' | 'polling' | null
        this.isInitialized = false;
        this.connectionTimeout = 5000; // 5秒
        this.fallbackDelay = 3000; // 3秒後にフォールバック
        this.eventHandlers = new Map();
    }

    /**
     * 更新システムを初期化
     */
    async initialize() {
        if (this.isInitialized) {
            console.log('UpdateManager は既に初期化されています');
            return;
        }

        console.log('UpdateManager を初期化中...');
        
        // WebSocket接続を試行
        const webSocketAvailable = await this.tryWebSocketConnection();
        
        if (webSocketAvailable) {
            await this.initializeWebSocket();
        } else {
            await this.initializePolling();
        }
        
        this.isInitialized = true;
        console.log(`UpdateManager 初期化完了: ${this.updateMethod}`);
    }

    /**
     * WebSocket接続を試行
     */
    async tryWebSocketConnection() {
        // WebSocketライブラリの確認
        if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
            console.log('WebSocketライブラリが見つかりません。ポーリングモードを使用します。');
            return false;
        }

        // realtimeClientの確認
        if (typeof realtimeClient === 'undefined') {
            console.log('realtimeClientが見つかりません。ポーリングモードを使用します。');
            return false;
        }

        console.log('WebSocket接続を試行中...');
        
        return new Promise((resolve) => {
            let resolved = false;
            
            // タイムアウト設定
            const timeout = setTimeout(() => {
                if (!resolved) {
                    resolved = true;
                    console.log('WebSocket接続タイムアウト。ポーリングモードにフォールバックします。');
                    resolve(false);
                }
            }, this.connectionTimeout);

            // 接続状態リスナーを一時的に追加
            const connectionListener = (isConnected) => {
                if (!resolved && isConnected) {
                    resolved = true;
                    clearTimeout(timeout);
                    console.log('WebSocket接続成功');
                    resolve(true);
                }
            };

            realtimeClient.onConnectionChange(connectionListener);
            
            // 接続試行
            try {
                realtimeClient.connect();
            } catch (error) {
                if (!resolved) {
                    resolved = true;
                    clearTimeout(timeout);
                    console.log('WebSocket接続エラー:', error.message);
                    resolve(false);
                }
            }
        });
    }

    /**
     * WebSocket更新を初期化
     */
    async initializeWebSocket() {
        console.log('WebSocketベースの更新を初期化中...');
        this.updateMethod = 'websocket';
        
        // イベントハンドラーを設定
        realtimeClient.on('workload-update', (data) => {
            this.triggerEvent('workload-update', data);
        });

        realtimeClient.on('issue-update', (data) => {
            this.triggerEvent('issue-update', data);
        });

        realtimeClient.on('system-message', (data) => {
            this.triggerEvent('system-message', data);
        });

        // 接続状態の監視
        realtimeClient.onConnectionChange((isConnected) => {
            if (!isConnected && this.updateMethod === 'websocket') {
                console.log('WebSocket接続が切断されました。ポーリングにフォールバックします。');
                setTimeout(() => {
                    this.fallbackToPolling();
                }, this.fallbackDelay);
            }
        });

        // 接続状態インジケーターを更新
        this.updateConnectionStatus('websocket', true);
        
        console.log('WebSocketベースの更新が有効になりました');
        this.showNotification('リアルタイム更新が有効になりました', 'success');
    }

    /**
     * ポーリング更新を初期化
     */
    async initializePolling() {
        console.log('ポーリングベースの更新を初期化中...');
        this.updateMethod = 'polling';
        
        // pollingClientが利用可能か確認
        if (typeof pollingClient === 'undefined') {
            console.error('pollingClientが見つかりません');
            return;
        }

        // イベントハンドラーを設定
        pollingClient.on('workload-update', (data) => {
            this.triggerEvent('workload-update', data);
        });

        pollingClient.on('issue-update', (data) => {
            this.triggerEvent('issue-update', data);
        });

        // ポーリング開始
        pollingClient.start();
        
        // 接続状態インジケーターを更新
        this.updateConnectionStatus('polling', true);
        
        console.log('ポーリングベースの更新が有効になりました');
        this.showNotification('定期更新が有効になりました（30秒間隔）', 'info');
    }

    /**
     * ポーリングにフォールバック
     */
    async fallbackToPolling() {
        if (this.updateMethod === 'polling') {
            return; // 既にポーリングモード
        }

        console.log('ポーリングモードにフォールバック中...');
        
        // WebSocket接続を停止
        if (typeof realtimeClient !== 'undefined') {
            realtimeClient.disconnect();
        }
        
        // ポーリングを初期化
        await this.initializePolling();
        
        this.showNotification('定期更新モードに切り替えました', 'warning');
    }

    /**
     * WebSocketに復帰を試行
     */
    async tryReconnectWebSocket() {
        if (this.updateMethod === 'websocket') {
            return; // 既にWebSocketモード
        }

        console.log('WebSocket復帰を試行中...');
        
        const webSocketAvailable = await this.tryWebSocketConnection();
        
        if (webSocketAvailable) {
            // ポーリングを停止
            if (typeof pollingClient !== 'undefined') {
                pollingClient.stop();
            }
            
            // WebSocketを初期化
            await this.initializeWebSocket();
            
            this.showNotification('リアルタイム更新に復帰しました', 'success');
        } else {
            console.log('WebSocket復帰に失敗しました');
        }
    }

    /**
     * 手動更新を実行
     */
    async manualUpdate() {
        console.log('手動更新を実行中...');
        
        switch (this.updateMethod) {
            case 'websocket':
                // WebSocketの場合は即座にデータを再取得
                this.showNotification('データを更新中...', 'info');
                await this.loadDashboardData();
                this.showNotification('データ更新完了', 'success');
                break;
                
            case 'polling':
                // ポーリングの場合は手動更新を実行
                if (typeof pollingClient !== 'undefined') {
                    await pollingClient.manualUpdate();
                }
                break;
                
            default:
                console.warn('更新方法が設定されていません');
                break;
        }
    }

    /**
     * ダッシュボードデータを読み込み
     */
    async loadDashboardData() {
        try {
            // 既存のloadDashboardData関数を呼び出し
            if (typeof loadDashboardData === 'function') {
                await loadDashboardData();
            } else {
                // フォールバック: 直接データを読み込み
                await Promise.all([
                    this.loadWorkloadStatus(),
                    this.loadTeamIssues()
                ]);
            }
        } catch (error) {
            console.error('ダッシュボードデータ読み込みエラー:', error);
        }
    }

    /**
     * 負荷状況を読み込み
     */
    async loadWorkloadStatus() {
        try {
            let data;
            if (typeof apiClient !== 'undefined') {
                data = await apiClient.getWorkloadStatuses();
            } else {
                const response = await fetch(`${API_BASE_URL}/api/workload-status`);
                data = response.ok ? await response.json() : [];
            }
            
            if (typeof displayWorkloadStatus === 'function') {
                displayWorkloadStatus(data);
            }
        } catch (error) {
            console.error('負荷状況読み込みエラー:', error);
        }
    }

    /**
     * 困りごとを読み込み
     */
    async loadTeamIssues() {
        try {
            let data;
            if (typeof apiClient !== 'undefined') {
                data = await apiClient.getTeamIssues();
            } else {
                const response = await fetch(`${API_BASE_URL}/api/team-issues`);
                data = response.ok ? await response.json() : [];
            }
            
            if (typeof displayTeamIssues === 'function') {
                displayTeamIssues(data);
            }
        } catch (error) {
            console.error('困りごと読み込みエラー:', error);
        }
    }

    /**
     * 接続状態インジケーターを更新
     */
    updateConnectionStatus(method, isConnected) {
        const statusIndicator = document.getElementById('connectionStatus');
        if (!statusIndicator) {
            return;
        }

        if (isConnected) {
            statusIndicator.className = 'connection-status connected';
            switch (method) {
                case 'websocket':
                    statusIndicator.textContent = '🟢 リアルタイム';
                    break;
                case 'polling':
                    statusIndicator.textContent = '🔄 定期更新';
                    break;
            }
        } else {
            statusIndicator.className = 'connection-status disconnected';
            statusIndicator.textContent = '🔴 オフライン';
        }
    }

    /**
     * 更新方法を取得
     */
    getUpdateMethod() {
        return this.updateMethod;
    }

    /**
     * 状態情報を取得
     */
    getStatus() {
        const baseStatus = {
            updateMethod: this.updateMethod,
            isInitialized: this.isInitialized
        };

        switch (this.updateMethod) {
            case 'websocket':
                return {
                    ...baseStatus,
                    websocket: typeof realtimeClient !== 'undefined' ? realtimeClient.getConnectionStatus() : null
                };
                
            case 'polling':
                return {
                    ...baseStatus,
                    polling: typeof pollingClient !== 'undefined' ? pollingClient.getStatus() : null
                };
                
            default:
                return baseStatus;
        }
    }

    /**
     * イベントハンドラーを登録
     */
    on(eventType, handler) {
        if (!this.eventHandlers.has(eventType)) {
            this.eventHandlers.set(eventType, []);
        }
        this.eventHandlers.get(eventType).push(handler);
    }

    /**
     * イベントを発火
     */
    triggerEvent(eventType, data) {
        if (this.eventHandlers.has(eventType)) {
            this.eventHandlers.get(eventType).forEach(handler => {
                try {
                    handler(data);
                } catch (error) {
                    console.error(`Error in event handler for ${eventType}:`, error);
                }
            });
        }
    }

    /**
     * 通知を表示
     */
    showNotification(message, type = 'info') {
        if (typeof showNotification === 'function') {
            showNotification(message, type);
        } else {
            console.log(`[${type.toUpperCase()}] ${message}`);
        }
    }

    /**
     * 更新システムを停止
     */
    stop() {
        console.log('UpdateManager を停止中...');
        
        switch (this.updateMethod) {
            case 'websocket':
                if (typeof realtimeClient !== 'undefined') {
                    realtimeClient.disconnect();
                }
                break;
                
            case 'polling':
                if (typeof pollingClient !== 'undefined') {
                    pollingClient.stop();
                }
                break;
        }
        
        this.updateConnectionStatus(this.updateMethod, false);
        this.isInitialized = false;
        this.updateMethod = null;
        
        console.log('UpdateManager が停止されました');
    }
}

// グローバルインスタンスを作成
const updateManager = new UpdateManager();

// モジュールとしてエクスポート
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { UpdateManager, updateManager };
}

// グローバルスコープでも利用可能にする
window.updateManager = updateManager;
window.UpdateManager = UpdateManager;