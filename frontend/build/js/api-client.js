/**
 * API Client for Team Dashboard - Amplify対応版
 * 負荷状況と困りごと管理のためのAPIクライアント
 */

// Amplify設定を読み込み（CDN版を使用）
// import './aws-config.js';
// import { API } from 'aws-amplify';
// import { authManager } from './auth-manager.js';

class APIClient {
    constructor() {
        this.apiName = 'teamDashboardApi';
        this.baseURL = window.location.hostname === 'localhost' 
            ? 'http://localhost:8081/api' 
            : null; // Amplifyの場合はnull
        this.loadingStates = new Map();
        this.errorHandlers = new Map();
    }

    /**
     * 認証トークンを取得（Amplify対応）
     */
    async getAuthToken() {
        if (typeof window.authManager !== 'undefined') {
            return await window.authManager.getAuthToken();
        }
        return null;
    }

    /**
     * 現在のユーザーを取得
     */
    async getCurrentUser() {
        if (typeof window.authManager !== 'undefined') {
            return await window.authManager.getCurrentUser();
        }
        return null;
    }

    /**
     * HTTPリクエストのヘッダーを構築（ローカル開発用）
     */
    async buildHeaders(additionalHeaders = {}) {
        const headers = {
            'Content-Type': 'application/json',
            ...additionalHeaders
        };

        // 認証トークンがある場合は追加（ローカル開発用）
        if (this.baseURL) {
            const token = await this.getAuthToken();
            if (token) {
                headers['Authorization'] = `Bearer ${token}`;
            }
        }

        return headers;
    }

    /**
     * ローディング状態を設定
     */
    setLoading(key, isLoading) {
        this.loadingStates.set(key, isLoading);
        this.notifyLoadingChange(key, isLoading);
    }

    /**
     * ローディング状態を取得
     */
    isLoading(key) {
        return this.loadingStates.get(key) || false;
    }

    /**
     * ローディング状態変更の通知
     */
    notifyLoadingChange(key, isLoading) {
        const event = new CustomEvent('loadingStateChange', {
            detail: { key, isLoading }
        });
        document.dispatchEvent(event);
    }

    /**
     * エラーハンドラーを登録
     */
    onError(key, handler) {
        this.errorHandlers.set(key, handler);
    }

    /**
     * エラーを処理
     */
    handleError(key, error) {
        console.error(`API Error [${key}]:`, error);
        
        const handler = this.errorHandlers.get(key);
        if (handler) {
            handler(error);
        } else {
            // デフォルトエラーハンドリング
            this.showDefaultError(error);
        }
    }

    /**
     * デフォルトエラー表示
     */
    showDefaultError(error) {
        let message = 'エラーが発生しました';
        
        if (error.status === 401) {
            message = '認証が必要です';
        } else if (error.status === 403) {
            message = 'アクセス権限がありません';
        } else if (error.status === 404) {
            message = 'データが見つかりません';
        } else if (error.status >= 500) {
            message = 'サーバーエラーが発生しました';
        } else if (error.message) {
            message = error.message;
        }

        if (typeof showNotification === 'function') {
            showNotification(message, 'error');
        }
    }

    /**
     * 基本的なHTTPリクエスト（Amplify対応）
     */
    async request(method, endpoint, data = null, options = {}) {
        try {
            // ローカル開発環境の場合
            if (this.baseURL) {
                return await this.requestLocal(method, endpoint, data, options);
            }

            // Amplify API Gateway経由
            const body = data;
            const API = window.aws_amplify_api ? window.aws_amplify_api.API : (window.API || null);
            
            if (!API) {
                throw new Error('Amplify API not available');
            }

            switch (method.toUpperCase()) {
                case 'GET':
                    return await API.get(this.apiName, endpoint);
                case 'POST':
                    return await API.post(this.apiName, endpoint, { body });
                case 'PUT':
                    return await API.put(this.apiName, endpoint, { body });
                case 'DELETE':
                    return await API.del(this.apiName, endpoint);
                default:
                    throw new Error(`Unsupported method: ${method}`);
            }
        } catch (error) {
            // ネットワークエラーなどの場合
            if (!error.status && !error.response) {
                error.message = 'ネットワークエラーが発生しました';
            }
            throw error;
        }
    }

    /**
     * ローカル開発用のHTTPリクエスト
     */
    async requestLocal(method, endpoint, data = null, options = {}) {
        const url = `${this.baseURL}${endpoint}`;
        const config = {
            method,
            headers: await this.buildHeaders(options.headers),
            ...options
        };

        if (data && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
            config.body = JSON.stringify(data);
        }

        const response = await fetch(url, config);
        
        if (!response.ok) {
            const error = new Error(`HTTP ${response.status}: ${response.statusText}`);
            error.status = response.status;
            error.response = response;
            
            // レスポンスボディがある場合は取得
            try {
                const errorBody = await response.text();
                if (errorBody) {
                    error.message = errorBody;
                }
            } catch (e) {
                // エラーボディの取得に失敗した場合は無視
            }
            
            throw error;
        }

        // レスポンスが空の場合はnullを返す
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            return null;
        }

        return await response.json();
    }

    /**
     * GETリクエスト
     */
    async get(endpoint, options = {}) {
        return this.request('GET', endpoint, null, options);
    }

    /**
     * POSTリクエスト
     */
    async post(endpoint, data, options = {}) {
        return this.request('POST', endpoint, data, options);
    }

    /**
     * PUTリクエスト
     */
    async put(endpoint, data = null, options = {}) {
        return this.request('PUT', endpoint, data, options);
    }

    /**
     * DELETEリクエスト
     */
    async delete(endpoint, options = {}) {
        return this.request('DELETE', endpoint, null, options);
    }

    // ===== 負荷状況API =====

    /**
     * 全メンバーの負荷状況を取得
     */
    async getWorkloadStatuses() {
        const key = 'workload-statuses';
        this.setLoading(key, true);
        
        try {
            const result = await this.get('/workload-status');
            return result || [];
        } catch (error) {
            this.handleError(key, error);
            throw error;
        } finally {
            this.setLoading(key, false);
        }
    }

    /**
     * 自分の負荷状況を取得
     */
    async getMyWorkloadStatus() {
        const key = 'my-workload-status';
        this.setLoading(key, true);
        
        try {
            return await this.get('/workload-status/my');
        } catch (error) {
            // 404の場合は新規作成として扱う
            if (error.status === 404) {
                return null;
            }
            this.handleError(key, error);
            throw error;
        } finally {
            this.setLoading(key, false);
        }
    }

    /**
     * 負荷状況を更新
     */
    async updateWorkloadStatus(workloadData) {
        const key = 'update-workload-status';
        this.setLoading(key, true);
        
        try {
            const result = await this.post('/workload-status', workloadData);
            
            // 基本的な成功通知（詳細な通知はDataManagerで行う）
            if (typeof showSuccessNotification === 'function') {
                showSuccessNotification('負荷状況を更新中...', 1500);
            } else if (typeof showNotification === 'function') {
                showNotification('負荷状況を更新中...', 'success', 1500);
            }
            
            return result;
        } catch (error) {
            this.handleError(key, error);
            throw error;
        } finally {
            this.setLoading(key, false);
        }
    }

    // ===== 困りごとAPI =====

    /**
     * チーム困りごと一覧を取得
     */
    async getTeamIssues() {
        const key = 'team-issues';
        this.setLoading(key, true);
        
        try {
            const result = await this.get('/team-issues');
            return result || [];
        } catch (error) {
            this.handleError(key, error);
            throw error;
        } finally {
            this.setLoading(key, false);
        }
    }

    /**
     * 困りごとを投稿
     */
    async createTeamIssue(issueData) {
        const key = 'create-team-issue';
        this.setLoading(key, true);
        
        try {
            const result = await this.post('/team-issues', issueData);
            
            // 基本的な成功通知（詳細な通知はDataManagerで行う）
            if (typeof showSuccessNotification === 'function') {
                showSuccessNotification('困りごとを投稿中...', 1500);
            } else if (typeof showNotification === 'function') {
                showNotification('困りごとを投稿中...', 'success', 1500);
            }
            
            return result;
        } catch (error) {
            this.handleError(key, error);
            throw error;
        } finally {
            this.setLoading(key, false);
        }
    }

    /**
     * 困りごとを解決済みにする
     */
    async resolveTeamIssue(issueId) {
        const key = 'resolve-team-issue';
        this.setLoading(key, true);
        
        try {
            const result = await this.put(`/team-issues/${issueId}/resolve`);
            
            // 基本的な成功通知（詳細な通知はDataManagerで行う）
            if (typeof showSuccessNotification === 'function') {
                showSuccessNotification('解決処理中...', 1500);
            } else if (typeof showNotification === 'function') {
                showNotification('解決処理中...', 'success', 1500);
            }
            
            return result;
        } catch (error) {
            this.handleError(key, error);
            throw error;
        } finally {
            this.setLoading(key, false);
        }
    }

    /**
     * 困りごとのコメント一覧を取得
     */
    async getIssueComments(issueId) {
        const key = `issue-comments-${issueId}`;
        this.setLoading(key, true);
        
        try {
            const result = await this.get(`/team-issues/${issueId}/comments`);
            return result || [];
        } catch (error) {
            this.handleError(key, error);
            throw error;
        } finally {
            this.setLoading(key, false);
        }
    }

    /**
     * 困りごとにコメントを投稿
     */
    async addIssueComment(issueId, commentData) {
        const key = 'add-issue-comment';
        this.setLoading(key, true);
        
        try {
            const result = await this.post(`/team-issues/${issueId}/comments`, commentData);
            
            // 基本的な成功通知（詳細な通知はDataManagerで行う）
            if (typeof showSuccessNotification === 'function') {
                showSuccessNotification('コメントを投稿中...', 1500);
            } else if (typeof showNotification === 'function') {
                showNotification('コメントを投稿中...', 'success', 1500);
            }
            
            return result;
        } catch (error) {
            this.handleError(key, error);
            throw error;
        } finally {
            this.setLoading(key, false);
        }
    }

    // ===== バッチ操作 =====

    /**
     * ダッシュボードに必要な全データを一括取得
     */
    async getDashboardData() {
        const key = 'dashboard-data';
        this.setLoading(key, true);
        
        try {
            const [workloadStatuses, teamIssues] = await Promise.all([
                this.getWorkloadStatuses().catch(error => {
                    console.warn('負荷状況の取得に失敗:', error);
                    return [];
                }),
                this.getTeamIssues().catch(error => {
                    console.warn('困りごとの取得に失敗:', error);
                    return [];
                })
            ]);

            return {
                workloadStatuses,
                teamIssues
            };
        } catch (error) {
            this.handleError(key, error);
            throw error;
        } finally {
            this.setLoading(key, false);
        }
    }
}

// グローバルインスタンスを作成
const apiClient = new APIClient();

// モジュールとしてエクスポート（ES6モジュール対応）
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { APIClient, apiClient };
}

// グローバルスコープでも利用可能にする
window.apiClient = apiClient;
window.APIClient = APIClient;