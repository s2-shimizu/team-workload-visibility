/**
 * AWS Cognito認証マネージャー
 * ユーザー認証、サインアップ、サインインを管理
 */

class AuthManager {
    constructor() {
        this.currentUser = null;
        this.isInitialized = false;
        this.authCallbacks = [];
        
        // Amplify設定の確認
        this.checkAmplifyAvailability();
    }

    checkAmplifyAvailability() {
        // CDN版のAmplifyが利用可能かチェック
        if (typeof window.aws_amplify_auth !== 'undefined') {
            this.Auth = window.aws_amplify_auth.Auth;
            console.log('Amplify Auth (CDN版) が利用可能です');
        } else if (typeof window.Auth !== 'undefined') {
            this.Auth = window.Auth;
            console.log('Amplify Auth が利用可能です');
        } else {
            console.warn('Amplify Auth が利用できません。モック認証を使用します。');
            this.Auth = null;
        }
    }

    /**
     * 認証状態変更のコールバックを登録
     */
    onAuthStateChange(callback) {
        this.authCallbacks.push(callback);
    }

    /**
     * 認証状態変更を通知
     */
    notifyAuthStateChange(user) {
        this.authCallbacks.forEach(callback => {
            try {
                callback(user);
            } catch (error) {
                console.error('認証状態変更コールバックエラー:', error);
            }
        });
    }

    /**
     * 初期化 - 現在のユーザー状態を確認
     */
    async initialize() {
        if (this.isInitialized) {
            return this.currentUser;
        }

        try {
            if (this.Auth) {
                this.currentUser = await this.Auth.currentAuthenticatedUser();
                console.log('認証済みユーザーを検出:', this.currentUser.username);
            } else {
                // モック認証の場合
                const mockUser = this.getMockUser();
                if (mockUser) {
                    this.currentUser = mockUser;
                    console.log('モック認証ユーザー:', mockUser.username);
                }
            }
        } catch (error) {
            console.log('認証済みユーザーが見つかりません:', error.message);
            this.currentUser = null;
        }

        this.isInitialized = true;
        this.notifyAuthStateChange(this.currentUser);
        return this.currentUser;
    }

    /**
     * サインアップ
     */
    async signUp(username, password, email, displayName) {
        try {
            if (this.Auth) {
                const result = await this.Auth.signUp({
                    username,
                    password,
                    attributes: {
                        email,
                        name: displayName
                    }
                });
                
                console.log('サインアップ成功:', result);
                return {
                    success: true,
                    user: result.user,
                    needsConfirmation: !result.user.userConfirmed
                };
            } else {
                // モック認証
                console.log('モックサインアップ:', { username, email, displayName });
                const mockUser = {
                    username,
                    attributes: { email, name: displayName },
                    userConfirmed: true
                };
                this.setMockUser(mockUser);
                return {
                    success: true,
                    user: mockUser,
                    needsConfirmation: false
                };
            }
        } catch (error) {
            console.error('サインアップエラー:', error);
            return {
                success: false,
                error: error.message || 'サインアップに失敗しました'
            };
        }
    }

    /**
     * 確認コード送信
     */
    async confirmSignUp(username, confirmationCode) {
        try {
            if (this.Auth) {
                await this.Auth.confirmSignUp(username, confirmationCode);
                console.log('アカウント確認成功');
                return { success: true };
            } else {
                // モック認証では確認不要
                console.log('モック確認成功');
                return { success: true };
            }
        } catch (error) {
            console.error('確認コードエラー:', error);
            return {
                success: false,
                error: error.message || '確認に失敗しました'
            };
        }
    }

    /**
     * サインイン
     */
    async signIn(username, password) {
        try {
            if (this.Auth) {
                const user = await this.Auth.signIn(username, password);
                this.currentUser = user;
                console.log('サインイン成功:', user.username);
                
                this.notifyAuthStateChange(this.currentUser);
                return {
                    success: true,
                    user: user
                };
            } else {
                // モック認証
                const mockUser = this.getMockUser();
                if (mockUser && mockUser.username === username) {
                    this.currentUser = mockUser;
                    console.log('モックサインイン成功:', username);
                    this.notifyAuthStateChange(this.currentUser);
                    return {
                        success: true,
                        user: mockUser
                    };
                } else {
                    throw new Error('ユーザーが見つかりません');
                }
            }
        } catch (error) {
            console.error('サインインエラー:', error);
            return {
                success: false,
                error: error.message || 'サインインに失敗しました'
            };
        }
    }

    /**
     * サインアウト
     */
    async signOut() {
        try {
            if (this.Auth) {
                await this.Auth.signOut();
            } else {
                // モック認証
                this.clearMockUser();
            }
            
            this.currentUser = null;
            console.log('サインアウト成功');
            this.notifyAuthStateChange(null);
            
            return { success: true };
        } catch (error) {
            console.error('サインアウトエラー:', error);
            return {
                success: false,
                error: error.message || 'サインアウトに失敗しました'
            };
        }
    }

    /**
     * 現在のユーザーを取得
     */
    async getCurrentUser() {
        if (!this.isInitialized) {
            await this.initialize();
        }
        return this.currentUser;
    }

    /**
     * 認証トークンを取得
     */
    async getAuthToken() {
        try {
            if (this.Auth && this.currentUser) {
                const session = await this.Auth.currentSession();
                return session.getIdToken().getJwtToken();
            } else if (this.currentUser) {
                // モック認証の場合
                return 'mock-jwt-token-' + this.currentUser.username;
            }
            return null;
        } catch (error) {
            console.error('トークン取得エラー:', error);
            return null;
        }
    }

    /**
     * ユーザー情報を取得
     */
    async getUserInfo() {
        const user = await this.getCurrentUser();
        if (!user) return null;

        try {
            if (this.Auth) {
                const attributes = await this.Auth.userAttributes(user);
                return {
                    username: user.username,
                    email: attributes.find(attr => attr.Name === 'email')?.Value,
                    name: attributes.find(attr => attr.Name === 'name')?.Value,
                    isAuthenticated: true
                };
            } else {
                // モック認証
                return {
                    username: user.username,
                    email: user.attributes?.email,
                    name: user.attributes?.name,
                    isAuthenticated: true
                };
            }
        } catch (error) {
            console.error('ユーザー情報取得エラー:', error);
            return {
                username: user.username,
                isAuthenticated: true
            };
        }
    }

    /**
     * 認証状態を確認
     */
    isAuthenticated() {
        return this.currentUser !== null;
    }

    // モック認証用のヘルパーメソッド
    getMockUser() {
        const userData = localStorage.getItem('mockUser');
        return userData ? JSON.parse(userData) : null;
    }

    setMockUser(user) {
        localStorage.setItem('mockUser', JSON.stringify(user));
    }

    clearMockUser() {
        localStorage.removeItem('mockUser');
    }

    /**
     * パスワードリセット
     */
    async forgotPassword(username) {
        try {
            if (this.Auth) {
                await this.Auth.forgotPassword(username);
                return { success: true };
            } else {
                // モック認証
                console.log('モックパスワードリセット:', username);
                return { success: true };
            }
        } catch (error) {
            console.error('パスワードリセットエラー:', error);
            return {
                success: false,
                error: error.message || 'パスワードリセットに失敗しました'
            };
        }
    }

    /**
     * パスワードリセット確認
     */
    async forgotPasswordSubmit(username, confirmationCode, newPassword) {
        try {
            if (this.Auth) {
                await this.Auth.forgotPasswordSubmit(username, confirmationCode, newPassword);
                return { success: true };
            } else {
                // モック認証
                console.log('モックパスワードリセット確認:', username);
                return { success: true };
            }
        } catch (error) {
            console.error('パスワードリセット確認エラー:', error);
            return {
                success: false,
                error: error.message || 'パスワードリセット確認に失敗しました'
            };
        }
    }
}

// グローバルインスタンスを作成
const authManager = new AuthManager();

// モジュールとしてエクスポート
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { AuthManager, authManager };
}

// グローバルスコープでも利用可能にする
window.authManager = authManager;
window.AuthManager = AuthManager;