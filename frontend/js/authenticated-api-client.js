// 認証付きAPI呼び出しクライアント
class AuthenticatedApiClient {
    constructor() {
        this.baseURL = this.getApiBaseUrl();
    }

    getApiBaseUrl() {
        // 本番環境では環境変数またはAmplify設定を使用
        if (window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1') {
            return window.AWS_API_URL || 'https://bn6xwu62qd.execute-api.ap-northeast-1.amazonaws.com/dev';
        }
        return 'http://localhost:8081';
    }

    async getAuthHeaders() {
        const headers = {
            'Content-Type': 'application/json'
        };

        try {
            // Cognitoトークンを取得
            const token = await authManager.getAuthToken();
            if (token) {
                headers['Authorization'] = `Bearer ${token}`;
            }
        } catch (error) {
            console.warn('認証トークンの取得に失敗:', error);
        }

        return headers;
    }

    async makeRequest(endpoint, options = {}) {
        const url = `${this.baseURL}${endpoint}`;
        const headers = await this.getAuthHeaders();

        const config = {
            method: 'GET',
            headers: headers,
            ...options
        };

        // リクエストボディがある場合
        if (options.body && typeof options.body === 'object') {
            config.body = JSON.stringify(options.body);
        }

        console.log(`API Request: ${config.method} ${url}`);

        try {
            const response = await fetch(url, config);
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return await response.json();
            } else {
                return await response.text();
            }
        } catch (error) {
            console.error(`API Error for ${endpoint}:`, error);
            throw error;
        }
    }

    // 負荷状況API
    async getWorkloadStatus() {
        return await this.makeRequest('/api/workload-status');
    }

    async updateWorkloadStatus(userId, data) {
        return await this.makeRequest(`/api/workload-status/${userId}`, {
            method: 'PUT',
            body: data
        });
    }

    // 困りごとAPI
    async getTeamIssues() {
        return await this.makeRequest('/api/team-issues');
    }

    async createTeamIssue(data) {
        return await this.makeRequest('/api/team-issues', {
            method: 'POST',
            body: data
        });
    }

    // ヘルスチェック
    async healthCheck() {
        return await this.makeRequest('/health');
    }

    // API状態確認
    async getApiStatus() {
        return await this.makeRequest('/api/status');
    }
}

// グローバルインスタンス
const apiClient = new AuthenticatedApiClient();
window.apiClient = apiClient;