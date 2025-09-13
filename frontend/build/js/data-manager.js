/**
 * Data Manager for Team Dashboard
 * データ更新とUI反映を管理するモジュール
 */

class DataManager {
    constructor(apiClient) {
        this.apiClient = apiClient;
        this.data = {
            workloadStatuses: [],
            teamIssues: [],
            lastUpdated: {
                workloadStatuses: null,
                teamIssues: null
            }
        };
        this.updateCallbacks = new Map();
        this.autoRefreshIntervals = new Map();
        
        this.initializeEventListeners();
    }

    /**
     * イベントリスナーを初期化
     */
    initializeEventListeners() {
        // ローディング状態の変更を監視
        document.addEventListener('loadingStateChange', (event) => {
            this.handleLoadingStateChange(event.detail);
        });

        // ページの可視性変更を監視（タブ切り替え時の自動更新）
        document.addEventListener('visibilitychange', () => {
            if (!document.hidden) {
                this.refreshStaleData();
            }
        });
    }

    /**
     * ローディング状態の変更を処理
     */
    handleLoadingStateChange({ key, isLoading }) {
        // ローディングインジケーターの表示/非表示
        this.updateLoadingIndicators(key, isLoading);
    }

    /**
     * ローディングインジケーターを更新
     */
    updateLoadingIndicators(key, isLoading) {
        const indicators = {
            'workload-statuses': '#workloadStatusCards',
            'team-issues': '#teamIssuesList',
            'dashboard-data': '.dashboard-loading'
        };

        const selector = indicators[key];
        if (!selector) return;

        const element = document.querySelector(selector);
        if (!element) return;

        if (isLoading) {
            // ローディング表示
            const loadingText = this.getLoadingText(key);
            if (element.children.length === 0 || !element.querySelector('.loading-indicator')) {
                const loadingDiv = document.createElement('div');
                loadingDiv.className = 'loading-indicator';
                loadingDiv.innerHTML = `
                    <div class="loading-spinner"></div>
                    <div class="loading-text">${loadingText}</div>
                `;
                element.appendChild(loadingDiv);
            }
        } else {
            // ローディング非表示
            const loadingIndicator = element.querySelector('.loading-indicator');
            if (loadingIndicator) {
                loadingIndicator.remove();
            }
        }
    }

    /**
     * ローディングテキストを取得
     */
    getLoadingText(key) {
        const texts = {
            'workload-statuses': '負荷状況を読み込み中...',
            'team-issues': '困りごとを読み込み中...',
            'dashboard-data': 'データを読み込み中...',
            'update-workload-status': '負荷状況を更新中...',
            'create-team-issue': '困りごとを投稿中...',
            'resolve-team-issue': '解決処理中...',
            'add-issue-comment': 'コメントを投稿中...'
        };
        return texts[key] || '処理中...';
    }

    /**
     * データ更新コールバックを登録
     */
    onDataUpdate(dataType, callback) {
        if (!this.updateCallbacks.has(dataType)) {
            this.updateCallbacks.set(dataType, []);
        }
        this.updateCallbacks.get(dataType).push(callback);
    }

    /**
     * データ更新を通知
     */
    notifyDataUpdate(dataType, data) {
        const callbacks = this.updateCallbacks.get(dataType) || [];
        callbacks.forEach(callback => {
            try {
                callback(data);
            } catch (error) {
                console.error(`Data update callback error for ${dataType}:`, error);
            }
        });

        // データ新鮮度インジケーターを更新
        this.updateDataFreshnessIndicator(dataType, Date.now());
    }

    /**
     * データの新鮮度インジケーターを更新
     */
    updateDataFreshnessIndicator(dataType, timestamp) {
        const indicatorIds = {
            'workloadStatuses': 'workloadDataFreshness',
            'teamIssues': 'issuesDataFreshness'
        };

        const indicatorId = indicatorIds[dataType];
        if (!indicatorId) return;

        const indicator = document.getElementById(indicatorId);
        if (!indicator) return;

        const now = Date.now();
        const age = now - timestamp;
        const ageMinutes = Math.floor(age / (1000 * 60));

        if (ageMinutes < 1) {
            indicator.textContent = 'たった今更新';
            indicator.className = 'data-freshness fresh';
        } else if (ageMinutes < 5) {
            indicator.textContent = `${ageMinutes}分前に更新`;
            indicator.className = 'data-freshness fresh';
        } else if (ageMinutes < 30) {
            indicator.textContent = `${ageMinutes}分前に更新`;
            indicator.className = 'data-freshness';
        } else {
            indicator.textContent = `${ageMinutes}分前に更新（古い可能性があります）`;
            indicator.className = 'data-freshness stale';
        }
    }

    /**
     * 自動更新を開始
     */
    startAutoRefresh(dataType, intervalMs = 30000) {
        this.stopAutoRefresh(dataType);
        
        const interval = setInterval(() => {
            this.refreshData(dataType);
        }, intervalMs);
        
        this.autoRefreshIntervals.set(dataType, interval);

        // データ新鮮度インジケーターの定期更新も開始
        this.startFreshnessUpdater();
    }

    /**
     * データ新鮮度インジケーターの定期更新を開始
     */
    startFreshnessUpdater() {
        // 既存のタイマーがあれば停止
        if (this.freshnessUpdateInterval) {
            clearInterval(this.freshnessUpdateInterval);
        }

        // 1分ごとに新鮮度インジケーターを更新
        this.freshnessUpdateInterval = setInterval(() => {
            Object.keys(this.data.lastUpdated).forEach(dataType => {
                const lastUpdated = this.data.lastUpdated[dataType];
                if (lastUpdated) {
                    this.updateDataFreshnessIndicator(dataType, lastUpdated);
                }
            });
        }, 60000); // 1分間隔
    }

    /**
     * 自動更新を停止
     */
    stopAutoRefresh(dataType) {
        const interval = this.autoRefreshIntervals.get(dataType);
        if (interval) {
            clearInterval(interval);
            this.autoRefreshIntervals.delete(dataType);
        }
    }

    /**
     * 古いデータを更新
     */
    refreshStaleData(maxAgeMs = 60000) {
        const now = Date.now();
        
        Object.keys(this.data.lastUpdated).forEach(dataType => {
            const lastUpdated = this.data.lastUpdated[dataType];
            if (!lastUpdated || (now - lastUpdated) > maxAgeMs) {
                this.refreshData(dataType);
            }
        });
    }

    /**
     * データを更新
     */
    async refreshData(dataType) {
        try {
            switch (dataType) {
                case 'workloadStatuses':
                    await this.refreshWorkloadStatuses();
                    break;
                case 'teamIssues':
                    await this.refreshTeamIssues();
                    break;
                case 'all':
                    await this.refreshAllData();
                    break;
            }
        } catch (error) {
            console.error(`Failed to refresh ${dataType}:`, error);
        }
    }

    /**
     * 負荷状況データを更新
     */
    async refreshWorkloadStatuses() {
        return await this.enhancedDataUpdate('workloadStatuses', async () => {
            const workloadStatuses = await this.apiClient.getWorkloadStatuses();
            this.data.workloadStatuses = workloadStatuses;
            this.data.lastUpdated.workloadStatuses = Date.now();
            
            this.notifyDataUpdate('workloadStatuses', workloadStatuses);
            this.updateWorkloadStatusUI(workloadStatuses);
            
            return workloadStatuses;
        });
    }

    /**
     * 困りごとデータを更新
     */
    async refreshTeamIssues() {
        return await this.enhancedDataUpdate('teamIssues', async () => {
            const teamIssues = await this.apiClient.getTeamIssues();
            this.data.teamIssues = teamIssues;
            this.data.lastUpdated.teamIssues = Date.now();
            
            this.notifyDataUpdate('teamIssues', teamIssues);
            this.updateTeamIssuesUI(teamIssues);
            
            return teamIssues;
        });
    }

    /**
     * 全データを更新
     */
    async refreshAllData() {
        try {
            const dashboardData = await this.apiClient.getDashboardData();
            
            this.data.workloadStatuses = dashboardData.workloadStatuses;
            this.data.teamIssues = dashboardData.teamIssues;
            this.data.lastUpdated.workloadStatuses = Date.now();
            this.data.lastUpdated.teamIssues = Date.now();
            
            this.notifyDataUpdate('workloadStatuses', dashboardData.workloadStatuses);
            this.notifyDataUpdate('teamIssues', dashboardData.teamIssues);
            
            this.updateWorkloadStatusUI(dashboardData.workloadStatuses);
            this.updateTeamIssuesUI(dashboardData.teamIssues);
            
            return dashboardData;
        } catch (error) {
            console.error('Failed to refresh all data:', error);
            throw error;
        }
    }

    /**
     * 負荷状況UIを更新
     */
    updateWorkloadStatusUI(workloadStatuses) {
        const container = document.getElementById('workloadStatusCards');
        if (!container) return;

        // ローディングインジケーターを削除
        const loadingIndicator = container.querySelector('.loading-indicator');
        if (loadingIndicator) {
            loadingIndicator.remove();
        }

        // 既存のカードを削除
        container.innerHTML = '';

        if (workloadStatuses.length === 0) {
            container.innerHTML = '<div class="workload-empty">まだ負荷状況が登録されていません</div>';
            return;
        }

        // 新しいカードを作成
        workloadStatuses.forEach(status => {
            const card = this.createWorkloadCard(status);
            container.appendChild(card);
        });

        // アニメーション効果
        this.animateCards(container);
    }

    /**
     * 困りごとUIを更新
     */
    updateTeamIssuesUI(teamIssues) {
        // 既存のallIssuesグローバル変数を更新
        if (typeof window.allIssues !== 'undefined') {
            window.allIssues = teamIssues;
        }

        // フィルタリングされた表示を更新
        if (typeof window.displayFilteredIssues === 'function') {
            window.displayFilteredIssues();
        } else {
            // フォールバック: 直接UIを更新
            this.updateTeamIssuesUIFallback(teamIssues);
        }
    }

    /**
     * 困りごとUI更新のフォールバック
     */
    updateTeamIssuesUIFallback(teamIssues) {
        const container = document.getElementById('teamIssuesList');
        if (!container) return;

        // ローディングインジケーターを削除
        const loadingIndicator = container.querySelector('.loading-indicator');
        if (loadingIndicator) {
            loadingIndicator.remove();
        }

        container.innerHTML = '';

        if (teamIssues.length === 0) {
            container.innerHTML = '<div class="issues-empty">まだ困りごとが投稿されていません</div>';
            return;
        }

        // 日付順でソート（新しい順）
        const sortedIssues = [...teamIssues].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

        sortedIssues.forEach(issue => {
            const item = this.createIssueItem(issue);
            container.appendChild(item);
        });

        // アニメーション効果
        this.animateCards(container);
    }

    /**
     * 負荷状況カードを作成
     */
    createWorkloadCard(status) {
        if (typeof window.createWorkloadCard === 'function') {
            return window.createWorkloadCard(status);
        }

        // フォールバック実装
        const card = document.createElement('div');
        card.className = `workload-card level-${status.workloadLevel}`;
        
        const lastUpdated = status.updatedAt ? 
            this.formatDateTime(status.updatedAt) : '未更新';
        
        const projectCount = status.projectCount ? 
            `<div class="workload-detail">📁 ${status.projectCount}案件</div>` : '';
        
        const taskCount = status.taskCount ? 
            `<div class="workload-detail">📋 ${status.taskCount}タスク</div>` : '';
        
        card.innerHTML = `
            <div class="user-name">${status.displayName}</div>
            <div class="workload-level">
                <span class="workload-level-badge ${status.workloadLevel}">
                    ${this.getWorkloadLevelText(status.workloadLevel)}
                </span>
                <span>${this.getWorkloadLevelEmoji(status.workloadLevel)}</span>
            </div>
            <div class="workload-details">
                ${projectCount}
                ${taskCount}
            </div>
            <div class="last-updated">最終更新: ${lastUpdated}</div>
        `;
        
        return card;
    }

    /**
     * 困りごとアイテムを作成
     */
    createIssueItem(issue) {
        if (typeof window.createIssueItem === 'function') {
            return window.createIssueItem(issue);
        }

        // フォールバック実装
        const item = document.createElement('div');
        item.className = `issue-item ${issue.status.toLowerCase()}`;
        item.setAttribute('data-issue-id', issue.id);
        
        const createdDate = this.formatDateTime(issue.createdAt);
        
        item.innerHTML = `
            <div class="issue-header">
                <div class="issue-author">
                    <span class="name">${issue.displayName}</span>
                    <span class="date">${createdDate}</span>
                </div>
                <div class="issue-status">
                    <span class="status-badge ${issue.status}">
                        ${issue.status === 'OPEN' ? '未解決' : '解決済み'}
                    </span>
                </div>
            </div>
            <div class="issue-content">${issue.content}</div>
        `;
        
        return item;
    }

    /**
     * カードアニメーション
     */
    animateCards(container) {
        const cards = container.querySelectorAll('.workload-card, .issue-item');
        cards.forEach((card, index) => {
            card.style.opacity = '0';
            card.style.transform = 'translateY(20px)';
            
            setTimeout(() => {
                card.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
                card.style.opacity = '1';
                card.style.transform = 'translateY(0)';
            }, index * 50);
        });
    }

    /**
     * 負荷状況を更新
     */
    async updateWorkloadStatus(workloadData) {
        try {
            // 更新処理を実行
            const result = await this.apiClient.updateWorkloadStatus(workloadData);
            
            // データを即座に更新してUIに反映
            await this.refreshWorkloadStatuses();
            
            // 更新成功の追加通知
            this.showUpdateSuccessNotification('workload');
            
            // 自動更新インジケーターを表示
            this.showAutoRefreshIndicator('負荷状況を更新しました');
            
            return result;
        } catch (error) {
            console.error('Failed to update workload status:', error);
            this.showUpdateErrorNotification('workload', error);
            throw error;
        }
    }

    /**
     * 困りごとを投稿
     */
    async createTeamIssue(issueData) {
        try {
            // 投稿処理を実行
            const newIssue = await this.apiClient.createTeamIssue(issueData);
            
            // データを即座に更新してUIに反映
            await this.refreshTeamIssues();
            
            // 投稿成功の追加通知
            this.showUpdateSuccessNotification('issue');
            
            // 自動更新インジケーターを表示
            this.showAutoRefreshIndicator('困りごとを投稿しました');
            
            return newIssue;
        } catch (error) {
            console.error('Failed to create team issue:', error);
            this.showUpdateErrorNotification('issue', error);
            throw error;
        }
    }

    /**
     * 困りごとを解決
     */
    async resolveTeamIssue(issueId) {
        try {
            // 解決処理を実行
            await this.apiClient.resolveTeamIssue(issueId);
            
            // データを即座に更新してUIに反映
            await this.refreshTeamIssues();
            
            // 解決成功の追加通知
            this.showUpdateSuccessNotification('resolve');
            
            // 自動更新インジケーターを表示
            this.showAutoRefreshIndicator('困りごとを解決しました');
            
            return true;
        } catch (error) {
            console.error('Failed to resolve team issue:', error);
            this.showUpdateErrorNotification('resolve', error);
            throw error;
        }
    }

    /**
     * コメントを投稿
     */
    async addIssueComment(issueId, commentData) {
        try {
            // コメント投稿処理を実行
            const newComment = await this.apiClient.addIssueComment(issueId, commentData);
            
            // コメント数を更新
            await this.updateCommentCount(issueId);
            
            // コメント投稿成功の追加通知
            this.showUpdateSuccessNotification('comment');
            
            // 自動更新インジケーターを表示
            this.showAutoRefreshIndicator('コメントを投稿しました');
            
            return newComment;
        } catch (error) {
            console.error('Failed to add issue comment:', error);
            this.showUpdateErrorNotification('comment', error);
            throw error;
        }
    }

    /**
     * コメント数を更新
     */
    async updateCommentCount(issueId) {
        try {
            const comments = await this.apiClient.getIssueComments(issueId);
            const countElement = document.getElementById(`commentCount-${issueId}`);
            if (countElement) {
                countElement.textContent = comments.length;
            }
        } catch (error) {
            console.error('Failed to update comment count:', error);
        }
    }

    /**
     * ユーティリティ: 日時フォーマット
     */
    formatDateTime(dateTimeString) {
        if (typeof window.formatDateTime === 'function') {
            return window.formatDateTime(dateTimeString);
        }

        // フォールバック実装
        const date = new Date(dateTimeString);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / (1000 * 60));
        const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
        const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
        
        if (diffMins < 1) {
            return 'たった今';
        } else if (diffMins < 60) {
            return `${diffMins}分前`;
        } else if (diffHours < 24) {
            return `${diffHours}時間前`;
        } else if (diffDays < 7) {
            return `${diffDays}日前`;
        } else {
            return date.toLocaleDateString('ja-JP', {
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
        }
    }

    /**
     * ユーティリティ: 負荷レベルテキスト
     */
    getWorkloadLevelText(level) {
        const texts = {
            'LOW': '低',
            'MEDIUM': '中',
            'HIGH': '高'
        };
        return texts[level] || '未設定';
    }

    /**
     * ユーティリティ: 負荷レベル絵文字
     */
    getWorkloadLevelEmoji(level) {
        const emojis = {
            'LOW': '😊',
            'MEDIUM': '😐',
            'HIGH': '😰'
        };
        return emojis[level] || '❓';
    }

    /**
     * データを取得（キャッシュ優先）
     */
    getData(dataType) {
        return this.data[dataType] || [];
    }

    /**
     * 最終更新時刻を取得
     */
    getLastUpdated(dataType) {
        return this.data.lastUpdated[dataType];
    }

    /**
     * 更新成功通知を表示
     */
    showUpdateSuccessNotification(type) {
        const messages = {
            'workload': '負荷状況が正常に更新され、ダッシュボードに反映されました',
            'issue': '困りごとが投稿され、チーム全体で共有されました',
            'resolve': '困りごとが解決済みとしてマークされました',
            'comment': 'コメントが投稿されました'
        };

        const message = messages[type] || '更新が完了しました';
        
        if (typeof showNotification === 'function') {
            showNotification(message, 'success');
        }
    }

    /**
     * 更新エラー通知を表示
     */
    showUpdateErrorNotification(type, error) {
        const messages = {
            'workload': '負荷状況の更新に失敗しました',
            'issue': '困りごとの投稿に失敗しました',
            'resolve': '困りごとの解決処理に失敗しました',
            'comment': 'コメントの投稿に失敗しました'
        };

        let message = messages[type] || '更新に失敗しました';
        
        // エラーの詳細があれば追加
        if (error && error.message) {
            message += `: ${error.message}`;
        }
        
        if (typeof showNotification === 'function') {
            showNotification(message, 'error');
        }
    }

    /**
     * 自動更新インジケーターを表示
     */
    showAutoRefreshIndicator(message) {
        let indicator = document.getElementById('autoRefreshIndicator');
        
        if (!indicator) {
            indicator = document.createElement('div');
            indicator.id = 'autoRefreshIndicator';
            indicator.className = 'auto-refresh-indicator';
            document.body.appendChild(indicator);
        }

        indicator.innerHTML = `
            <div class="spinner"></div>
            <span>${message}</span>
        `;

        indicator.classList.add('show');

        // 3秒後に非表示
        setTimeout(() => {
            indicator.classList.remove('show');
        }, 3000);
    }

    /**
     * データ更新アニメーションを実行
     */
    animateDataUpdate(containerSelector) {
        const container = document.querySelector(containerSelector);
        if (!container) return;

        // 更新中のスタイルを適用
        container.classList.add('updating');

        // アニメーション完了後にスタイルを削除
        setTimeout(() => {
            container.classList.remove('updating');
        }, 300);
    }

    /**
     * リアルタイム更新の状態を表示
     */
    showRealTimeUpdateStatus(isUpdating) {
        const statusElements = document.querySelectorAll('.data-freshness');
        
        statusElements.forEach(element => {
            if (isUpdating) {
                element.textContent = '更新中...';
                element.className = 'data-freshness updating';
            } else {
                element.textContent = 'たった今更新';
                element.className = 'data-freshness fresh';
            }
        });
    }

    /**
     * 接続状態を表示
     */
    showConnectionStatus(status) {
        let statusIndicator = document.getElementById('connectionStatus');
        
        if (!statusIndicator) {
            statusIndicator = document.createElement('div');
            statusIndicator.id = 'connectionStatus';
            statusIndicator.className = 'connection-status';
            document.body.appendChild(statusIndicator);
        }

        const messages = {
            'online': '🟢 オンライン',
            'offline': '🔴 オフライン',
            'reconnecting': '🟡 再接続中...'
        };

        statusIndicator.textContent = messages[status] || status;
        statusIndicator.className = `connection-status ${status} show`;

        // オンライン状態の場合は3秒後に非表示
        if (status === 'online') {
            setTimeout(() => {
                statusIndicator.classList.remove('show');
            }, 3000);
        }
    }

    /**
     * 強化されたデータ更新処理
     */
    async enhancedDataUpdate(dataType, updateFunction) {
        try {
            // リアルタイム更新状態を表示
            this.showRealTimeUpdateStatus(true);

            // データ更新を実行
            const result = await updateFunction();

            // 更新アニメーションを実行
            const containerSelectors = {
                'workloadStatuses': '#workloadStatusCards',
                'teamIssues': '#teamIssuesList'
            };

            const selector = containerSelectors[dataType];
            if (selector) {
                this.animateDataUpdate(selector);
            }

            // リアルタイム更新状態を完了に変更
            this.showRealTimeUpdateStatus(false);

            return result;
        } catch (error) {
            // エラー時の状態更新
            this.showRealTimeUpdateStatus(false);
            throw error;
        }
    }

    /**
     * クリーンアップ
     */
    destroy() {
        // 自動更新を停止
        this.autoRefreshIntervals.forEach((interval, dataType) => {
            this.stopAutoRefresh(dataType);
        });

        // 新鮮度更新タイマーを停止
        if (this.freshnessUpdateInterval) {
            clearInterval(this.freshnessUpdateInterval);
        }

        // コールバックをクリア
        this.updateCallbacks.clear();

        // インジケーターを削除
        const indicators = ['autoRefreshIndicator', 'connectionStatus'];
        indicators.forEach(id => {
            const element = document.getElementById(id);
            if (element) {
                element.remove();
            }
        });
    }
}

// グローバルインスタンスを作成
const dataManager = new DataManager(window.apiClient);

// モジュールとしてエクスポート（ES6モジュール対応）
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { DataManager, dataManager };
}

// グローバルスコープでも利用可能にする
window.dataManager = dataManager;
window.DataManager = DataManager;