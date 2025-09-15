package com.teamdashboard;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.*;

/**
 * 純粋なJavaで実装されたAPIハンドラー
 * Spring Bootの依存関係を使用せず、確実に動作する
 */
public class PureApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            context.getLogger().log("PureApiHandler - Received request: " + input.getPath() + " " + input.getHttpMethod());
            
            String path = input.getPath();
            String method = input.getHttpMethod();
            
            // CORS対応
            if ("OPTIONS".equals(method)) {
                return createCorsResponse();
            }
            
            // 認証が必要なエンドポイントをチェック
            if (requiresAuthentication(path)) {
                String authResult = validateAuthentication(input, context);
                if (authResult != null) {
                    return createErrorResponse(401, authResult);
                }
            }
            
            // ルーティング
            switch (path) {
                case "/health":
                case "/actuator/health":
                    return handleHealth();
                case "/api/status":
                    return handleApiStatus();
                case "/api/workload-status":
                case "/workload-status":
                    return handleWorkloadStatus(method);
                case "/api/workload-status/my":
                case "/workload-status/my":
                    return handleMyWorkloadStatus();
                case "/api/team-issues":
                case "/team-issues":
                    return handleTeamIssues(method);
                case "/api/team-issues/open":
                case "/team-issues/open":
                    return handleOpenTeamIssues();
                case "/api/team-issues/statistics":
                case "/team-issues/statistics":
                    return handleIssueStatistics();
                default:
                    return createErrorResponse(404, "Not Found: " + path);
            }
            
        } catch (Exception e) {
            context.getLogger().log("PureApiHandler - Error: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse(500, "Internal Server Error: " + e.getMessage());
        }
    }
    
    private APIGatewayProxyResponseEvent createCorsResponse() {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setHeaders(getCorsHeaders());
        response.setBody("");
        return response;
    }
    
    private APIGatewayProxyResponseEvent handleHealth() {
        String json = "{" +
            "\"status\":\"OK\"," +
            "\"message\":\"Team Dashboard API is running\"," +
            "\"timestamp\":" + System.currentTimeMillis() +
            "}";
        return createSuccessResponse(json);
    }
    
    private APIGatewayProxyResponseEvent handleApiStatus() {
        String json = "{" +
            "\"status\":\"OK\"," +
            "\"message\":\"Team Dashboard Pure API is running\"," +
            "\"timestamp\":" + System.currentTimeMillis() + "," +
            "\"version\":\"1.0.0\"" +
            "}";
        return createSuccessResponse(json);
    }
    
    private APIGatewayProxyResponseEvent handleWorkloadStatus(String method) {
        if ("GET".equals(method)) {
            String json = "[" +
                "{" +
                    "\"userId\":\"user1\"," +
                    "\"displayName\":\"田中太郎\"," +
                    "\"workloadLevel\":\"MEDIUM\"," +
                    "\"projectCount\":3," +
                    "\"taskCount\":15," +
                    "\"updatedAt\":" + System.currentTimeMillis() +
                "}," +
                "{" +
                    "\"userId\":\"user2\"," +
                    "\"displayName\":\"佐藤花子\"," +
                    "\"workloadLevel\":\"HIGH\"," +
                    "\"projectCount\":5," +
                    "\"taskCount\":25," +
                    "\"updatedAt\":" + (System.currentTimeMillis() - 3600000) +
                "}," +
                "{" +
                    "\"userId\":\"user3\"," +
                    "\"displayName\":\"鈴木一郎\"," +
                    "\"workloadLevel\":\"LOW\"," +
                    "\"projectCount\":1," +
                    "\"taskCount\":5," +
                    "\"updatedAt\":" + (System.currentTimeMillis() - 7200000) +
                "}" +
                "]";
            return createSuccessResponse(json);
        } else if ("POST".equals(method)) {
            String json = "{" +
                "\"userId\":\"current-user\"," +
                "\"displayName\":\"現在のユーザー\"," +
                "\"workloadLevel\":\"MEDIUM\"," +
                "\"projectCount\":2," +
                "\"taskCount\":8," +
                "\"message\":\"負荷状況を更新しました\"," +
                "\"updatedAt\":" + System.currentTimeMillis() +
                "}";
            return createSuccessResponse(json);
        }
        return createErrorResponse(405, "Method Not Allowed");
    }
    
    private APIGatewayProxyResponseEvent handleMyWorkloadStatus() {
        String json = "{" +
            "\"userId\":\"current-user\"," +
            "\"displayName\":\"現在のユーザー\"," +
            "\"workloadLevel\":\"LOW\"," +
            "\"projectCount\":2," +
            "\"taskCount\":8," +
            "\"updatedAt\":" + System.currentTimeMillis() +
            "}";
        return createSuccessResponse(json);
    }
    
    private APIGatewayProxyResponseEvent handleTeamIssues(String method) {
        if ("GET".equals(method)) {
            String json = "[" +
                "{" +
                    "\"issueId\":\"issue-1\"," +
                    "\"userId\":\"user1\"," +
                    "\"displayName\":\"田中太郎\"," +
                    "\"content\":\"新しい技術の学習で詰まっています。React Hooksの使い方がよくわからず、コンポーネントの状態管理で困っています。\"," +
                    "\"status\":\"OPEN\"," +
                    "\"priority\":\"HIGH\"," +
                    "\"createdAt\":" + (System.currentTimeMillis() - 86400000) +
                "}," +
                "{" +
                    "\"issueId\":\"issue-2\"," +
                    "\"userId\":\"user2\"," +
                    "\"displayName\":\"佐藤花子\"," +
                    "\"content\":\"プロジェクトの進め方で悩んでいます。タスクの優先順位をどう決めればよいかアドバイスをください。\"," +
                    "\"status\":\"RESOLVED\"," +
                    "\"priority\":\"MEDIUM\"," +
                    "\"createdAt\":" + (System.currentTimeMillis() - 172800000) +
                "}" +
                "]";
            return createSuccessResponse(json);
        } else if ("POST".equals(method)) {
            String json = "{" +
                "\"issueId\":\"issue-" + System.currentTimeMillis() + "\"," +
                "\"userId\":\"current-user\"," +
                "\"displayName\":\"現在のユーザー\"," +
                "\"content\":\"新しい困りごとが投稿されました\"," +
                "\"status\":\"OPEN\"," +
                "\"priority\":\"MEDIUM\"," +
                "\"createdAt\":" + System.currentTimeMillis() + "," +
                "\"message\":\"新しい困りごとが投稿されました\"" +
                "}";
            return createSuccessResponse(json);
        }
        return createErrorResponse(405, "Method Not Allowed");
    }
    
    private APIGatewayProxyResponseEvent handleOpenTeamIssues() {
        String json = "[" +
            "{" +
                "\"issueId\":\"issue-1\"," +
                "\"userId\":\"user1\"," +
                "\"displayName\":\"田中太郎\"," +
                "\"content\":\"新しい技術の学習で詰まっています。React Hooksの使い方がよくわからず、コンポーネントの状態管理で困っています。\"," +
                "\"status\":\"OPEN\"," +
                "\"priority\":\"HIGH\"," +
                "\"createdAt\":" + (System.currentTimeMillis() - 86400000) +
            "}" +
            "]";
        return createSuccessResponse(json);
    }
    
    private APIGatewayProxyResponseEvent handleIssueStatistics() {
        String json = "{" +
            "\"open\":8," +
            "\"resolved\":15," +
            "\"total\":23," +
            "\"highPriority\":3," +
            "\"mediumPriority\":12," +
            "\"lowPriority\":8" +
            "}";
        return createSuccessResponse(json);
    }
    
    private APIGatewayProxyResponseEvent createSuccessResponse(String json) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setHeaders(getCorsHeaders());
        response.setBody(json);
        return response;
    }
    
    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setHeaders(getCorsHeaders());
        
        String json = "{" +
            "\"error\":\"" + message + "\"," +
            "\"timestamp\":" + System.currentTimeMillis() +
            "}";
        response.setBody(json);
        
        return response;
    }
    
    /**
     * 認証が必要なエンドポイントかどうかを判定
     */
    private boolean requiresAuthentication(String path) {
        // 認証が不要なエンドポイント
        if (path.equals("/health") || path.equals("/actuator/health") || path.equals("/api/status")) {
            return false;
        }
        
        // その他のAPIエンドポイントは認証が必要
        return path.startsWith("/api/") || path.startsWith("/workload-status") || path.startsWith("/team-issues");
    }
    
    /**
     * 認証を検証
     */
    private String validateAuthentication(APIGatewayProxyRequestEvent input, Context context) {
        Map<String, String> headers = input.getHeaders();
        if (headers == null) {
            return "認証ヘッダーがありません";
        }
        
        // Authorizationヘッダーを取得（大文字小文字を考慮）
        String authHeader = null;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if ("authorization".equalsIgnoreCase(entry.getKey())) {
                authHeader = entry.getValue();
                break;
            }
        }
        
        if (authHeader == null || authHeader.isEmpty()) {
            context.getLogger().log("認証ヘッダーが見つかりません");
            // 開発環境では認証をスキップ（本番環境では削除）
            return null;
        }
        
        if (!authHeader.startsWith("Bearer ")) {
            return "無効な認証ヘッダー形式";
        }
        
        String token = authHeader.substring(7);
        
        // モックトークンの場合は認証成功
        if (token.startsWith("mock-jwt-token-")) {
            context.getLogger().log("モック認証トークンを検出: " + token);
            return null;
        }
        
        // JWT検証（簡易版 - 本番環境では適切なJWT検証を実装）
        if (token.length() < 10) {
            return "無効なトークン";
        }
        
        context.getLogger().log("JWT認証成功: " + token.substring(0, Math.min(20, token.length())) + "...");
        return null; // 認証成功
    }
    
    /**
     * 認証されたユーザー情報を取得
     */
    private String getCurrentUserId(APIGatewayProxyRequestEvent input) {
        Map<String, String> headers = input.getHeaders();
        if (headers == null) {
            return "anonymous";
        }
        
        String authHeader = null;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if ("authorization".equalsIgnoreCase(entry.getKey())) {
                authHeader = entry.getValue();
                break;
            }
        }
        
        if (authHeader != null && authHeader.startsWith("Bearer mock-jwt-token-")) {
            return authHeader.substring("Bearer mock-jwt-token-".length());
        }
        
        // 実際のJWTからユーザーIDを抽出（簡易版）
        return "current-user";
    }
    
    private Map<String, String> getCorsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
        headers.put("Content-Type", "application/json");
        return headers;
    }
}