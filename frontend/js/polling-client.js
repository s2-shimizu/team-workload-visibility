/**
 * ポーリングベースの更新クライアント
 * WebSocketが利用できない環境（Lambda等）でのデータ更新機能
 */

class PollingClient {
    constructor() {
        this.isPolling = false;
        this.pollingInterval = null;
        this.updateInterval = 30000; // 30秒間隔
        this.fastUpdateInterval = 5000; // 5秒間隔（アクティブ時）
        this.currentInterval = this.updateInterval;
        this.lastUpdateTime = new Map();
        this.eventHandlers = new Map();
        this.isUserActive = true;
        this.visibilityChangeSupported = typeof document.hidden !== 'undefined';
        
        // ページの可視性変更を監視
        this.setupVisibilityChangeListener();
        
        // ユーザーアクティビティを監視
        this.setupActivityListener();
    }

    /**
     * ページ可視性変更の監視
     */
    setupVisibilityChangeListener() {
        if (this.visibilityChangeSupported) {
            document.addEventListener('visibilitychange', () => {
                if (document.hidden) {
                    // ページが非表示になった場合、更新間隔を長くする
                    this.setUpdateInterval(this.updateInterval * 2); // 60秒間隔
                    console.log('ページが非表示になりました。更新間隔を延長します。');
                } else {
                    // ページが表示された場合、通常の更新間隔に戻す
                    this.setUpdateInterval(this.updateInterval);
                    console.log('ページが表示されました。通常の更新間隔に戻します。');
                    // 即座に更新を実行
                    this.performUpdate();
                }
            });
        }
    }

    /**
     * ユーザーアクティビティの監視
     */
    setupActivityListener() {
        let activityTimer;
        
        const resetActivityTimer = () => {
            this.isUserActive = true;
            clearTimeout(activityTimer);
            
            // 5分間非アクティブの場合、更新間隔を長くする
            activityTimer = setTimeout(() => {
                this.isUserActive = false;
                this.setUpdateInterval(this.updateInterval * 3); // 90秒間隔
                console.log('ユーザーが非アクティブです。更新間隔を延長します。');
            }, 300000); // 5分
        };

        // ユーザーアクティビティイベント
        ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart', 'click'].forEach(event => {
            document.addEventListener(event, resetActivityTimer, true);
        });

        // 初期化
        resetActivityTimer();
    }

    /**
     * ポーリング開始
     */
    start() {
        if (this.isPolling) {
            console.log('ポーリングは既に開始されています');
            return;
        }

        console.log('ポーリングベースの更新を開始します');
        this.isPolling = true;
        
        // 即座に初回更新を実行
        this.performUpdate();
        
        // 定期更新を開始
        this.scheduleNextUpdate();
        
        // 接続状態を通知
        this.notifyConnectionChange(true);
        
        // 開始通知
        this.showNotification('定期更新が開始されました', 'info');
    }

    /**
     * ポーリング停止
     */
    stop() {
        if (!this.isPolling) {
            return;
        }

        console.log('ポーリングを停止します');
        this.isPolling = false;
        
        if (this.pollingInterval) {
            clearTimeout(this.pollingInterval);
            this.pollingInterval = null;
        }
        
        // 接続状態を通知
        this.notifyConnectionChange(false);
    }

    /**
     * 更新間隔を設定
     */
    setUpdateInterval(interval) {
        if (this.currentInterval !== interval) {
            this.currentInterval = interval;
            console.log(`更新間隔を${interval / 1000}秒に変更しました`);
            
            // 既存のタイマーをリセット
            if (this.pollingInterval) {
                clearTimeout(this.pollingInterval);
                this.scheduleNextUpdate();
            }
        }
    }

    /**
     * 次回更新をスケジュール
     */
    scheduleNextUpdate() {
        if (!this.isPolling) {
            return;
        }

        this.pollingInterval = setTimeout(() => {
            this.performUpdate();
            this.scheduleNextUpdate();
        }, this.currentInterval);
    }

    /**
     * 更新実行
     */
    async performUpdate() {
        if (!this.isPolling) {
            return;
        }

        console.log('データ更新を実行中...');
        
        try {
            // 並行して両方のデータを取得
            const [workloadData, issueData] = await Promise.all([
                this.fetchWorkloadStatus(),
                this.fetchTeamIssues()
            ]);

            // データの変更をチェックして更新
            this.checkAndUpdateWorkloadStatus(workloadData);
            this.checkAndUpdateTeamIssues(issueData);

            // 最終更新時刻を記録
            this.lastUpdateTime.set('workload', Date.now());
            this.lastUpdateTime.set('issues', Date.now());

            // 成功通知（デバッグ時のみ）
            if (window.location.hostname === 'localhost') {
                console.log('データ更新完了');
            }

        } catch (error) {
            console.error('データ更新エラー:', error);
            
            // エラー時は更新間隔を短くして再試行
            if (this.currentInterval > this.fastUpdateInterval) {
                this.setUpdateInterval(this.fastUpdateInterval);
            }
        }
    }

    /**
     * 負荷状況データを取得
     */
    async fetchWorkloadStatus() {
        try {
            if (typeof apiClient !== 'undefined') {
                return await apiClient.getWorkloadStatuses();
            } else {
                // フォールバック: 直接API呼び出し
                const response = await fetch(`${API_BASE_URL}/api/workload-status`);
                if (response.ok) {
                    return await response.json();
                }
                throw new Error(`HTTP ${response.status}`);
            }
        } catch (error) {
            console.warn('負荷状況取得エラー:', error.message);
            return [];
        }
    }

    /**
     * 困りごとデータを取得
     */
    async fetchTeamIssues() {
        try {
            if (typeof apiClient !== 'undefined') {
                return await apiClient.getTeamIssues();
            } else {
                // フォールバック: 直接API呼び出し
                const response = await fetch(`${API_BASE_URL}/api/team-issues`);
                if (response.ok) {
                    return await response.json();
                }
                throw new Error(`HTTP ${response.status}`);
            }
        } catch (error) {
            console.warn('困りごと取得エラー:', error.message);
            return [];
        }
    }

    /**
     * 負荷状況の変更をチェックして更新
     */
    checkAndUpdateWorkloadStatus(newData) {
        if (!Array.isArray(newData)) {
            return;
        }

        const container = document.getElementById('workloadStatusCards');
        if (!container) {
            return;
        }

        // 既存データと比較
        const existingCards = container.querySelectorAll('[data-user-id]');
        const existingUserIds = Array.from(existingCards).map(card => card.getAttribute('data-user-id'));
        const newUserIds = newData.map(item => item.userId);

        // 新しいユーザーまたは更新されたデータを検出
        let hasChanges = false;

        newData.forEach(item => {
            const existingCard = container.querySelector(`[data-user-id="${item.userId}"]`);
            
            if (!existingCard) {
                // 新しいユーザー
                hasChanges = true;
                this.addWorkloadCard(container, item, true);
            } else {
                // 既存ユーザーの更新チェック
                if (this.hasWorkloadChanged(existingCard, item)) {
                    hasChanges = true;
                    this.updateWorkloadCard(existingCard, item, true);
                }
            }
        });

        // 削除されたユーザーを検出
        existingUserIds.forEach(userId => {
            if (!newUserIds.includes(userId)) {
                hasChanges = true;
                const cardToRemove = container.querySelector(`[data-user-id="${userId}"]`);
                if (cardToRemove) {
                    this.removeWorkloadCard(cardToRemove);
                }
            }
        });

        if (hasChanges) {
            console.log('負荷状況データが更新されました');
            this.triggerEvent('workload-update', { type: 'POLLING_UPDATE', data: newData });
        }
    }

    /**
     * 困りごとの変更をチェックして更新
     */
    checkAndUpdateTeamIssues(newData) {
        if (!Array.isArray(newData)) {
            return;
        }

        const container = document.getElementById('teamIssuesList');
        if (!container) {
            return;
        }

        // 既存データと比較
        const existingItems = container.querySelectorAll('[data-issue-id]');
        const existingIssueIds = Array.from(existingItems).map(item => item.getAttribute('data-issue-id'));
        const newIssueIds = newData.map(item => item.issueId);

        let hasChanges = false;

        // 新しい困りごとまたは更新された困りごとを検出
        newData.forEach(item => {
            const existingItem = container.querySelector(`[data-issue-id="${item.issueId}"]`);
            
            if (!existingItem) {
                // 新しい困りごと
                hasChanges = true;
                this.addIssueItem(container, item, true);
            } else {
                // 既存困りごとの更新チェック
                if (this.hasIssueChanged(existingItem, item)) {
                    hasChanges = true;
                    this.updateIssueItem(existingItem, item, true);
                }
            }
        });

        // 削除された困りごとを検出
        existingIssueIds.forEach(issueId => {
            if (!newIssueIds.includes(issueId)) {
                hasChanges = true;
                const itemToRemove = container.querySelector(`[data-issue-id="${issueId}"]`);
                if (itemToRemove) {
                    this.removeIssueItem(itemToRemove);
                }
            }
        });

        if (hasChanges) {
            console.log('困りごとデータが更新されました');
            this.triggerEvent('issue-update', { type: 'POLLING_UPDATE', data: newData });
        }
    }

    /**
     * 負荷状況の変更を検出
     */
    hasWorkloadChanged(cardElement, newData) {
        const currentLevel = cardElement.className.match(/level-(\w+)/)?.[1];
        const currentProjects = cardElement.querySelector('.workload-details')?.textContent.match(/(\d+)案件/)?.[1];
        const currentTasks = cardElement.querySelector('.workload-details')?.textContent.match(/(\d+)タスク/)?.[1];

        return (
            currentLevel !== newData.workloadLevel ||
            parseInt(currentProjects || '0') !== (newData.projectCount || 0) ||
            parseInt(currentTasks || '0') !== (newData.taskCount || 0)
        );
    }

    /**
     * 困りごとの変更を検出
     */
    hasIssueChanged(itemElement, newData) {
        const currentStatus = itemElement.className.includes('resolved') ? 'RESOLVED' : 'OPEN';
        return currentStatus !== newData.status;
    }

    /**
     * 負荷状況カードを追加
     */
    addWorkloadCard(container, data, animate = false) {
        const card = this.createWorkloadCard(data);
        if (animate) {
            card.classList.add('new-item');
        }
        container.appendChild(card);
        
        if (animate) {
            this.showNotification(`${data.displayName}さんの負荷状況が追加されました`, 'info');
        }
    }

    /**
     * 負荷状況カードを更新
     */
    updateWorkloadCard(cardElement, data, animate = false) {
        // カードの内容を更新
        cardElement.className = `workload-card level-${data.workloadLevel}`;
        cardElement.setAttribute('data-user-id', data.userId);

        const levelBadge = cardElement.querySelector('.workload-level-badge');
        if (levelBadge) {
            levelBadge.className = `workload-level-badge ${data.workloadLevel}`;
            levelBadge.textContent = this.getWorkloadLevelText(data.workloadLevel);
        }

        const emoji = cardElement.querySelector('.workload-level span:last-child');
        if (emoji) {
            emoji.textContent = this.getWorkloadLevelEmoji(data.workloadLevel);
        }

        const details = cardElement.querySelector('.workload-details');
        if (details) {
            details.innerHTML = `
                ${data.projectCount ? `<div>📁 ${data.projectCount}案件</div>` : ''}
                ${data.taskCount ? `<div>📋 ${data.taskCount}タスク</div>` : ''}
            `;
        }

        const lastUpdated = cardElement.querySelector('.last-updated');
        if (lastUpdated) {
            lastUpdated.textContent = '最終更新: たった今';
        }

        if (animate) {
            cardElement.classList.add('updated');
            setTimeout(() => cardElement.classList.remove('updated'), 2000);
            this.showNotification(`${data.displayName}さんの負荷状況が更新されました`, 'info');
        }
    }

    /**
     * 負荷状況カードを削除
     */
    removeWorkloadCard(cardElement) {
        const displayName = cardElement.querySelector('.user-name')?.textContent || 'ユーザー';
        cardElement.classList.add('item-removing');
        setTimeout(() => {
            cardElement.remove();
        }, 500);
        this.showNotification(`${displayName}さんの負荷状況が削除されました`, 'info');
    }

    /**
     * 困りごとアイテムを追加
     */
    addIssueItem(container, data, animate = false) {
        const item = this.createIssueItem(data);
        if (animate) {
            item.classList.add('new-item');
        }
        container.insertBefore(item, container.firstChild);
        
        if (animate) {
            this.showNotification(`${data.displayName}さんが新しい困りごとを投稿しました`, 'info');
        }
    }

    /**
     * 困りごとアイテムを更新
     */
    updateIssueItem(itemElement, data, animate = false) {
        itemElement.className = `issue-item ${data.status.toLowerCase()}`;
        
        const statusBadge = itemElement.querySelector('.status-badge');
        if (statusBadge) {
            statusBadge.className = `status-badge ${data.status}`;
            statusBadge.textContent = data.status === 'OPEN' ? '未解決' : '解決済み';
        }

        if (animate) {
            itemElement.classList.add('updated');
            setTimeout(() => itemElement.classList.remove('updated'), 2000);
            
            const action = data.status === 'RESOLVED' ? '解決しました' : '再オープンしました';
            this.showNotification(`${data.displayName}さんが困りごとを${action}`, 'info');
        }
    }

    /**
     * 困りごとアイテムを削除
     */
    removeIssueItem(itemElement) {
        itemElement.classList.add('item-removing');
        setTimeout(() => {
            itemElement.remove();
        }, 500);
        this.showNotification('困りごとが削除されました', 'info');
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
     * 困りごとアイテムを作成
     */
    createIssueItem(data) {
        const item = document.createElement('div');
        item.className = `issue-item ${data.status.toLowerCase()}`;
        item.setAttribute('data-issue-id', data.issueId);
        
        item.innerHTML = `
            <div class="issue-header">
                <div class="issue-author">
                    <span class="name">${data.displayName}</span>
                    <span class="date">${this.formatDateTime(data.createdAt)}</span>
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
     * 手動更新を実行
     */
    async manualUpdate() {
        console.log('手動更新を実行中...');
        this.showNotification('データを更新中...', 'info');
        
        // 更新間隔を一時的に短くする
        const originalInterval = this.currentInterval;
        this.setUpdateInterval(this.fastUpdateInterval);
        
        await this.performUpdate();
        
        // 元の更新間隔に戻す
        setTimeout(() => {
            this.setUpdateInterval(originalInterval);
        }, 30000); // 30秒後に元に戻す
        
        this.showNotification('データ更新完了', 'success');
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
     * 接続状態変更を通知
     */
    notifyConnectionChange(isConnected) {
        const statusIndicator = document.getElementById('connectionStatus');
        if (statusIndicator) {
            statusIndicator.className = isConnected ? 'connected' : 'disconnected';
            statusIndicator.textContent = isConnected ? '🔄 定期更新' : '🔴 オフライン';
        }
    }

    /**
     * 状態を取得
     */
    getStatus() {
        return {
            isPolling: this.isPolling,
            currentInterval: this.currentInterval,
            isUserActive: this.isUserActive,
            lastUpdate: {
                workload: this.lastUpdateTime.get('workload'),
                issues: this.lastUpdateTime.get('issues')
            }
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

    formatDateTime(dateTimeString) {
        const date = new Date(dateTimeString);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / (1000 * 60));
        
        if (diffMins < 1) return 'たった今';
        if (diffMins < 60) return `${diffMins}分前`;
        if (diffMins < 1440) return `${Math.floor(diffMins / 60)}時間前`;
        return date.toLocaleDateString('ja-JP');
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
const pollingClient = new PollingClient();

// モジュールとしてエクスポート
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { PollingClient, pollingClient };
}

// グローバルスコープでも利用可能にする
window.pollingClient = pollingClient;
window.PollingClient = PollingClient;