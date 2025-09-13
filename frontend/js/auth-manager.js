// 認証管理クラス
// import { Auth } from 'aws-amplify'; // CDN版を使用するためコメントアウト

class AuthManager {
    constructor() {
        this.currentUser = null;
        this.authToken = null;
    }

    async signIn(username, password) {
        try {
            const Auth = window.aws_amplify_auth ? window.aws_amplify_auth.Auth : (window.Auth || null);
            if (!Auth) {
                return await this.signInLocal(username);
            }
            
            const user = await Auth.signIn(username, password);
            this.currentUser = user;
            console.log('ログイン成功:', user);
            return user;
        } catch (error) {
            console.error('ログインエラー:', error);
            // フォールバック: ローカル認証
            return await this.signInLocal(username);
        }
    }

    async signOut() {
        try {
            const Auth = window.aws_amplify_auth ? window.aws_amplify_auth.Auth : (window.Auth || null);
            if (Auth) {
                await Auth.signOut();
            }
            this.currentUser = null;
            this.authToken = null;
            console.log('ログアウト成功');
        } catch (error) {
            console.error('ログアウトエラー:', error);
            // ローカル状態をクリア
            this.currentUser = null;
            this.authToken = null;
        }
    }

    async getCurrentUser() {
        try {
            if (!this.currentUser) {
                const Auth = window.aws_amplify_auth ? window.aws_amplify_auth.Auth : (window.Auth || null);
                if (Auth) {
                    this.currentUser = await Auth.currentAuthenticatedUser();
                } else {
                    // デフォルトユーザーを設定（開発用）
                    this.currentUser = {
                        username: 'testuser',
                        attributes: {
                            sub: 'local-testuser',
                            email: 'testuser@example.com',
                            name: '現在のユーザー'
                        }
                    };
                }
            }
            return this.currentUser;
        } catch (error) {
            console.log('認証されていません');
            return null;
        }
    }

    async getAuthToken() {
        try {
            if (!this.authToken) {
                const Auth = window.aws_amplify_auth ? window.aws_amplify_auth.Auth : (window.Auth || null);
                if (Auth) {
                    const session = await Auth.currentSession();
                    this.authToken = session.getIdToken().getJwtToken();
                } else {
                    // ローカル開発用のダミートークン
                    this.authToken = 'local-token-' + Date.now();
                }
            }
            return this.authToken;
        } catch (error) {
            console.error('トークン取得エラー:', error);
            return null;
        }
    }

    async getUserAttributes() {
        try {
            const user = await this.getCurrentUser();
            if (user) {
                const Auth = window.aws_amplify_auth ? window.aws_amplify_auth.Auth : (window.Auth || null);
                if (Auth) {
                    return await Auth.userAttributes(user);
                } else {
                    return user.attributes || {};
                }
            }
            return null;
        } catch (error) {
            console.error('ユーザー属性取得エラー:', error);
            return null;
        }
    }

    async isAuthenticated() {
        try {
            await this.getCurrentUser();
            return true;
        } catch (error) {
            return false;
        }
    }

    // 開発用：ローカル認証（Cognito未設定時）
    async signInLocal(username) {
        if (window.location.hostname === 'localhost') {
            this.currentUser = {
                username: username,
                attributes: {
                    sub: 'local-' + username,
                    email: username + '@example.com',
                    name: username
                }
            };
            this.authToken = 'local-token-' + Date.now();
            return this.currentUser;
        }
        throw new Error('Local auth only available in development');
    }

    getUserId() {
        if (this.currentUser) {
            return this.currentUser.attributes?.sub || this.currentUser.username;
        }
        return null;
    }

    getDisplayName() {
        if (this.currentUser) {
            return this.currentUser.attributes?.name || 
                   this.currentUser.attributes?.email || 
                   this.currentUser.username;
        }
        return null;
    }
}

// export const authManager = new AuthManager(); // 通常のスクリプトとして読み込むためコメントアウト

const authManager = new AuthManager();

// グローバルスコープでも利用可能にする
window.authManager = authManager;
window.AuthManager = AuthManager;