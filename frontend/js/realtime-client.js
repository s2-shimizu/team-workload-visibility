/**
 * リアルタイム通信クライアント
 * WebSocketを使用してサーバーからのリアルタイム更新を受信
 */

class RealtimeClient {
    constructor() {
        this.stompClient = null;
        this.isConnected = false;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 1000; // 1秒
        this.eventHandlers = new Map();
        this.connectionListeners = [];
        
        // WebSocketライブラリの読み込み確認
        this.checkDependencies();
    }

    /**
     * 依存関係の確認
     */
    checkDependencies() {
        if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
            console.warn('SockJS or STOMP library not loaded. Loading from CDN...');
            this.loadDependencies();
        }
    }

    /**
     * 必要なライブラリを動的に読み込み
     */
    loadDependencies() {
        const sockjsScript = document.createElement('script');
        sockjsScript.src = 'https://cdn.jsdelivr.net/npm/sockjs-client@1.6.1/dist/sockjs.min.js';
        sockjsScript.onload = () => {
            const stompScript = document.createElement('script');
            stompScript.src = 'https://cdn.jsdelivr.net/npm/@stomp/stompjs@7.0.0/bundles/stomp.umd.min.js';
            stompScript.onload = () => {
                console.log('WebSocket dependencies loaded');
            };
            document.head.appendChild(stompScript);
        };
        document.head.appendChild(sockjsScript);
    }

    /**
     * WebSocket接続を開始
     */
    connect() {
        if (this.isConnected) {
            console.log('Already connected to WebSocket');
            return;
        }

        try {
            // WebSocketエンドポイントURL
            const wsUrl = this.getWebSocketUrl();
            console.log('Connecting to WebSocket:', wsUrl);

            // SockJS接続を作成
            const socket = new SockJS(wsUrl);
            this.stompClient = Stomp.over(socket);

            // デバッグログを無効化（本番環境では）
            this.stompClient.debug = (str) => {
                if (window.location.hostname === 'localhost') {
                    console.log('STOMP:', str);
                }
            };

            // 接続
            this.stompClient.connect({}, 
                (frame) => this.onConnected(frame),
                (error) => this.onError(error)
            );

        } catch (error) {
            console.error('WebSocket connection failed:', error);
            this.scheduleReconnect();
        }
    }

    /**
     * WebSocketエンドポイントURLを取得
     */
    getWebSocketUrl() {
        const isLocal = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
        
        if (isLocal) {
            return 'http://localhost:8080/ws';
        } else {
            // 本番環境のWebSocketエンドポイント
            return 'https://bn6xwu62qd.execute-api.ap-northeast-1.amazonaws.com/dev/ws';
        }
    }

    /**
     * 接続成功時の処理
     */
    onConnected(frame) {
        console.log('Connected to WebSocket:', frame);
        this.isConnected = true;
        this.reconnectAttempts = 0;

        // 各種トピックを購読
        this.subscribeToTopics();

        // 接続リスナーに通知
        this.notifyConnectionListeners(true);

        // 接続成功通知
        this.showNotification('リアルタイム更新が有効になりました', 'success');
    }

    /**
     * エラー時の処理
     */
    onError(error) {
        console.error('WebSocket error:', error);
        this.isConnected = false;
        
        // 接続リスナーに通知
        this.notifyConnectionListeners(false);

        // 再接続を試行
        this.scheduleReconnect();
    }

    /**
     * トピックを購読
     */
    subscribeToTopics() {
        // 負荷状況の更新を購読
        this.stompClient.subscribe('/topic/workload-updates', (message) => {
            const data = JSON.parse(message.body);
            this.handleWorkloadUpdate(data);
        });

        // 困りごとの更新を購読
        this.stompClient.subscribe('/topic/issue-updates', (message) => {
            const data = JSON.parse(message.body);
            this.handleIssueUpdate(data);
        });

        // システムメッセージを購読
        this.stompClient.subscribe('/topic/system-messages', (message) => {
            const data = JSON.parse(message.body);
            this.handleSystemMessage(data);
        });

        // ユーザーステータスを購読
        this.stompClient.subscribe('/topic/user-status', (message) => {
            const data = JSON.parse(message.body);
            this.handleUserStatusUpdate(data);
        });

        console.log('Subscribed to all topics');
    }

    /**
     * 負荷状況更新の処理
     */
    handleWorkloadUpdate(data) {
        console.log('Workload update received:', data);
        
        // イベントハンドラーを呼び出し
        this.triggerEvent('workload-update', data);

        // UI更新
        this.updateWorkloadStatusUI(data);

        // 通知表示
        this.showNotification(
            `${data.displayName}さんが負荷状況を更新しました (${this.getWorkloadLevelText(data.workloadLevel)})`,
            'info'
        );
    }

    /**
     * 困りごと更新の処理
     */
    handleIssueUpdate(data) {
        console.log('Issue update received:', data);
        
        // イベントハンドラーを呼び出し
        this.triggerEvent('issue-update', data);

        // UI更新
        this.updateIssueUI(data);

        // 通知表示
        let message = '';
        switch (data.type) {
            case 'TEAM_ISSUE_CREATED':
                message = `${data.displayName}さんが新しい困りごとを投稿しました`;
                break;
            case 'TEAM_ISSUE_RESOLVED':
                message = `${data.displayName}さんが困りごとを解決しました`;
                break;
            case 'TEAM_ISSUE_REOPENED':
                message = `${data.displayName}さんが困りごとを再オープンしました`;
                break;
            case 'TEAM_ISSUE_DELETED':
                message = `${data.displayName}さんが困りごとを削除しました`;
                break;
        }
        
        if (message) {
            this.showNotification(message, 'info');
        }
    }

    /**
     * システムメッセージの処理
     */
    handleSystemMessage(data) {
        console.log('System message received:', data);
        
        // イベントハンドラーを呼び出し
        this.triggerEvent('system-message', data);

        // 通知表示
        const notificationType = data.messageType === 'ERROR' ? 'error' : 
                               data.messageType === 'WARNING' ? 'warning' : 'info';
        this.showNotification(data.message, notificationType);
    }

    /**
     * ユーザーステータス更新の処理
     */
    handleUserStatusUpdate(data) {
        console.log('User status update received:', data);
        
        // イベントハンドラーを呼び出し
        this.triggerEvent('user-status-update', data);

        // オンライン/オフライン通知
        const status = data.isOnline ? 'オンライン' : 'オフライン';
        this.showNotification(`${data.displayName}さんが${status}になりました`, 'info');
    }

    /**
     * 負荷状況UIの更新
     */
    updateWorkloadStatusUI(data) {
        // 既存の負荷状況カードを更新または新規作成
        const container = document.getElementById('workloadStatusCards');
        if (!container) return;

        let existingCard = container.querySelector(`[data-user-id="${data.userId}"]`);
        
        if (existingCard) {
            // 既存カードを更新
            this.updateWorkloadCard(existingCard, data);
        } else {
            // 新しいカードを作成
            const newCard = this.createWorkloadCard(data);
            container.appendChild(newCard);
        }

        // アニメーション効果
        const card = container.querySelector(`[data-user-id="${data.userId}"]`);
        if (card) {
            card.classList.add('updated');
            setTimeout(() => card.classList.remove('updated'), 2000);
        }
    }

    /**
     * 困りごとUIの更新
     */
    updateIssueUI(data) {
        const container = document.getElementById('teamIssuesList');
        if (!container) return;

        switch (data.type) {
            case 'TEAM_ISSUE_CREATED':
                const newIssueElement = this.createIssueElement(data);
                container.insertBefore(newIssueElement, container.firstChild);
                break;
                
            case 'TEAM_ISSUE_RESOLVED':
            case 'TEAM_ISSUE_REOPENED':
                const existingIssue = container.querySelector(`[data-issue-id="${data.issueId}"]`);
                if (existingIssue) {
                    this.updateIssueStatus(existingIssue, data.status);
                }
                break;
                
            case 'TEAM_ISSUE_DELETED':
                const issueToDelete = container.querySelector(`[data-issue-id="${data.issueId}"]`);
                if (issueToDelete) {
                    issueToDelete.remove();
                }
                break;
        }
    }

    /**
     * 負荷状況カードを作成
     */
    createWorkloadCard(data) {
        const card = document.createElement('div');
        card.className = `workload-card level-${data.workloadLevel}`;
        card.setAttribute('data-user-id', data.userId);
        
        card.innerHTML = `
            <div class="user-name">${data.displayName}</div>
            <div class="workload-level">
                <span class="workload-level-badge ${data.workloadLevel}">
                    ${this.getWorkloadLevelText(data.workloadLevel)}
                </span>
                <span>${this.getWorkloadLevelEmoji(data.workloadLevel)}</span>
            </div>
            <div class="workload-details">
                ${data.projectCount ? `<div>📁 ${data.projectCount}案件</div>` : ''}
                ${data.taskCount ? `<div>📋 ${data.taskCount}タスク</div>` : ''}
            </div>
            <div class="last-updated">最終更新: たった今</div>
        `;
        
        return card;
    }

    /**
     * 負荷状況カードを更新
     */
    updateWorkloadCard(card, data) {
        card.className = `workload-card level-${data.workloadLevel}`;
        
        const levelBadge = card.querySelector('.workload-level-badge');
        if (levelBadge) {
            levelBadge.className = `workload-level-badge ${data.workloadLevel}`;
            levelBadge.textContent = this.getWorkloadLevelText(data.workloadLevel);
        }

        const emoji = card.querySelector('.workload-level span:last-child');
        if (emoji) {
            emoji.textContent = this.getWorkloadLevelEmoji(data.workloadLevel);
        }

        const details = card.querySelector('.workload-details');
        if (details) {
            details.innerHTML = `
                ${data.projectCount ? `<div>📁 ${data.projectCount}案件</div>` : ''}
                ${data.taskCount ? `<div>📋 ${data.taskCount}タスク</div>` : ''}
            `;
        }

        const lastUpdated = card.querySelector('.last-updated');
        if (lastUpdated) {
            lastUpdated.textContent = '最終更新: たった今';
        }
    }

    /**
     * 困りごと要素を作成
     */
    createIssueElement(data) {
        const item = document.createElement('div');
        item.className = `issue-item ${data.status.toLowerCase()}`;
        item.setAttribute('data-issue-id', data.issueId);
        
        item.innerHTML = `
            <div class="issue-header">
                <div class="issue-author">
                    <span class="name">${data.displayName}</span>
                    <span class="date">たった今</span>
                </div>
                <div class="issue-status">
                    <span class="status-badge ${data.status}">
                        ${data.status === 'OPEN' ? '未解決' : '解決済み'}
                    </span>
                </div>
            </div>
            <div class="issue-content">${data.content}</div>
        `;
        
        return item;
    }

    /**
     * 困りごとステータスを更新
     */
    updateIssueStatus(element, status) {
        element.className = `issue-item ${status.toLowerCase()}`;
        
        const statusBadge = element.querySelector('.status-badge');
        if (statusBadge) {
            statusBadge.className = `status-badge ${status}`;
            statusBadge.textContent = status === 'OPEN' ? '未解決' : '解決済み';
        }
    }

    /**
     * 再接続をスケジュール
     */
    scheduleReconnect() {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error('Max reconnection attempts reached');
            this.showNotification('リアルタイム更新の接続に失敗しました', 'error');
            return;
        }

        this.reconnectAttempts++;
        const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1); // 指数バックオフ

        console.log(`Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts})`);
        
        setTimeout(() => {
            this.connect();
        }, delay);
    }

    /**
     * 接続を切断
     */
    disconnect() {
        if (this.stompClient && this.isConnected) {
            this.stompClient.disconnect(() => {
                console.log('Disconnected from WebSocket');
                this.isConnected = false;
                this.notifyConnectionListeners(false);
            });
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
     * イベントハンドラーを削除
     */
    off(eventType, handler) {
        if (this.eventHandlers.has(eventType)) {
            const handlers = this.eventHandlers.get(eventType);
            const index = handlers.indexOf(handler);
            if (index > -1) {
                handlers.splice(index, 1);
            }
        }
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
     * 接続状態リスナーを追加
     */
    onConnectionChange(listener) {
        this.connectionListeners.push(listener);
    }

    /**
     * 接続状態リスナーに通知
     */
    notifyConnectionListeners(isConnected) {
        this.connectionListeners.forEach(listener => {
            try {
                listener(isConnected);
            } catch (error) {
                console.error('Error in connection listener:', error);
            }
        });
    }

    /**
     * 接続状態を取得
     */
    getConnectionStatus() {
        return {
            isConnected: this.isConnected,
            reconnectAttempts: this.reconnectAttempts
        };
    }

    /**
     * ユーティリティメソッド
     */
    getWorkloadLevelText(level) {
        const texts = { 'LOW': '低', 'MEDIUM': '中', 'HIGH': '高' };
        return texts[level] || '未設定';
    }

    getWorkloadLevelEmoji(level) {
        const emojis = { 'LOW': '😊', 'MEDIUM': '😐', 'HIGH': '😰' };
        return emojis[level] || '❓';
    }

    showNotification(message, type = 'info') {
        // 既存の通知システムを使用
        if (typeof showNotification === 'function') {
            showNotification(message, type);
        } else {
            console.log(`[${type.toUpperCase()}] ${message}`);
        }
    }
}

// グローバルインスタンスを作成
const realtimeClient = new RealtimeClient();

// モジュールとしてエクスポート
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { RealtimeClient, realtimeClient };
}

// グローバルスコープでも利用可能にする
window.realtimeClient = realtimeClient;
window.RealtimeClient = RealtimeClient;