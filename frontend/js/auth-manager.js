// 認証管理クラス
import { Auth } from 'aws-amplify';

class AuthManager {
    constructor() {
        this.currentUser = null;
        this.authToken = null;
    }

    async signIn(username, password) {
        try {
            const user = await Auth.signIn(username, password);
            this.currentUser = user;
            console.log('ログイン成功:', user);
            return user;
        } catch (error) {
            console.error('ログインエラー:', error);
            throw error;
        }
    }

    async signOut() {
        try {
            await Auth.signOut();
            this.currentUser = null;
            this.authToken = null;
            console.log('ログアウト成功');
        } catch (error) {
            console.error('ログアウトエラー:', error);
            throw error;
        }
    }

    async getCurrentUser() {
        try {
            if (!this.currentUser) {
                this.currentUser = await Auth.currentAuthenticatedUser();
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
                const session = await Auth.currentSession();
                this.authToken = session.getIdToken().getJwtToken();
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
                return await Auth.userAttributes(user);
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

export const authManager = new AuthManager();