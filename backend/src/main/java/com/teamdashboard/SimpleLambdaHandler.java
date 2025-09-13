package com.teamdashboard;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class SimpleLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Context context;
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        this.context = context;
        try {
            context.getLogger().log("Received request: " + input.getPath() + " " + input.getHttpMethod());
            
            String path = input.getPath();
            String method = input.getHttpMethod();
            
            // CORS対応
            if ("OPTIONS".equals(method)) {
                return createCorsResponse();
            }
            
            // ルーティング
            if (path.equals("/health") || path.equals("/actuator/health")) {
                return handleHealth(input, context);
            } else if (path.startsWith("/workload-status")) {
                return handleWorkloadStatus(input, context);
            } else if (path.startsWith("/team-issues")) {
                return handleTeamIssues(input, context);
            } else {
                return createErrorResponse(404, "Not Found: " + path);
            }
            
        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse(500, "Internal Server Error: " + e.getMessage());
        }
    }
    
    private APIGatewayProxyResponseEvent createCorsResponse() {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setHeaders(getCorsHeaders());
        return response;
    }
    
    private APIGatewayProxyResponseEvent handleHealth(APIGatewayProxyRequestEvent input, Context context) throws Exception {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", "OK");
        responseBody.put("message", "Lambda function is working");
        responseBody.put("path", input.getPath());
        responseBody.put("method", input.getHttpMethod());
        responseBody.put("timestamp", System.currentTimeMillis());
        
        return createSuccessResponse(responseBody);
    }
    
    private APIGatewayProxyResponseEvent handleWorkloadStatus(APIGatewayProxyRequestEvent input, Context context) throws Exception {
        String method = input.getHttpMethod();
        String path = input.getPath();
        
        if ("GET".equals(method) && "/workload-status".equals(path)) {
            // 全メンバーの負荷状況を取得
            return getAllWorkloadStatuses();
        } else if ("GET".equals(method) && "/workload-status/my".equals(path)) {
            // 自分の負荷状況を取得
            return getMyWorkloadStatus();
        } else if ("POST".equals(method) && "/workload-status".equals(path)) {
            // 負荷状況を更新
            return updateWorkloadStatus(input);
        } else {
            return createErrorResponse(404, "Workload status endpoint not found");
        }
    }
    
    private APIGatewayProxyResponseEvent handleTeamIssues(APIGatewayProxyRequestEvent input, Context context) throws Exception {
        String method = input.getHttpMethod();
        String path = input.getPath();
        
        if ("GET".equals(method) && "/team-issues".equals(path)) {
            // 困りごと一覧を取得
            return getAllTeamIssues();
        } else if ("POST".equals(method) && "/team-issues".equals(path)) {
            // 困りごとを投稿
            return createTeamIssue(input);
        } else if (path.matches("/team-issues/\\d+/resolve") && "PUT".equals(method)) {
            // 困りごとを解決
            String issueId = path.split("/")[2];
            return resolveTeamIssue(issueId);
        } else if (path.matches("/team-issues/\\d+/comments")) {
            String issueId = path.split("/")[2];
            if ("GET".equals(method)) {
                return getIssueComments(issueId);
            } else if ("POST".equals(method)) {
                return addIssueComment(issueId, input);
            }
        }
        
        return createErrorResponse(404, "Team issues endpoint not found");
    }
    
    // 負荷状況関連のメソッド
    private APIGatewayProxyResponseEvent getAllWorkloadStatuses() throws Exception {
        // モックデータを返す（実際の実装ではDynamoDBから取得）
        Map<String, Object> status1 = new HashMap<>();
        status1.put("userId", "user1");
        status1.put("displayName", "田中太郎");
        status1.put("workloadLevel", "MEDIUM");
        status1.put("projectCount", 3);
        status1.put("taskCount", 15);
        status1.put("updatedAt", System.currentTimeMillis());
        
        Map<String, Object> status2 = new HashMap<>();
        status2.put("userId", "user2");
        status2.put("displayName", "佐藤花子");
        status2.put("workloadLevel", "HIGH");
        status2.put("projectCount", 5);
        status2.put("taskCount", 25);
        status2.put("updatedAt", System.currentTimeMillis() - 3600000);
        
        return createSuccessResponse(java.util.Arrays.asList(status1, status2));
    }
    
    private APIGatewayProxyResponseEvent getMyWorkloadStatus() throws Exception {
        // モックデータを返す
        Map<String, Object> myStatus = new HashMap<>();
        myStatus.put("userId", "current-user");
        myStatus.put("displayName", "現在のユーザー");
        myStatus.put("workloadLevel", "LOW");
        myStatus.put("projectCount", 2);
        myStatus.put("taskCount", 8);
        myStatus.put("updatedAt", System.currentTimeMillis());
        
        return createSuccessResponse(myStatus);
    }
    
    private APIGatewayProxyResponseEvent updateWorkloadStatus(APIGatewayProxyRequestEvent input) throws Exception {
        String body = input.getBody();
        context.getLogger().log("Updating workload status: " + body);
        
        // 更新されたデータを返す（実際の実装ではDynamoDBに保存）
        Map<String, Object> updatedStatus = new HashMap<>();
        updatedStatus.put("userId", "current-user");
        updatedStatus.put("displayName", "現在のユーザー");
        updatedStatus.put("message", "負荷状況を更新しました");
        updatedStatus.put("updatedAt", System.currentTimeMillis());
        
        return createSuccessResponse(updatedStatus);
    }
    
    // 困りごと関連のメソッド
    private APIGatewayProxyResponseEvent getAllTeamIssues() throws Exception {
        // モックデータを返す
        Map<String, Object> issue1 = new HashMap<>();
        issue1.put("id", 1);
        issue1.put("userId", "user1");
        issue1.put("displayName", "田中太郎");
        issue1.put("content", "新しい技術の学習で詰まっています。React Hooksの使い方がよくわからず、コンポーネントの状態管理で困っています。");
        issue1.put("status", "OPEN");
        issue1.put("createdAt", System.currentTimeMillis() - 7200000);
        
        Map<String, Object> issue2 = new HashMap<>();
        issue2.put("id", 2);
        issue2.put("userId", "user2");
        issue2.put("displayName", "佐藤花子");
        issue2.put("content", "プロジェクトの進め方で悩んでいます。タスクの優先順位をどう決めればよいかアドバイスをください。");
        issue2.put("status", "RESOLVED");
        issue2.put("createdAt", System.currentTimeMillis() - 86400000);
        issue2.put("resolvedAt", System.currentTimeMillis() - 3600000);
        
        return createSuccessResponse(java.util.Arrays.asList(issue1, issue2));
    }
    
    private APIGatewayProxyResponseEvent createTeamIssue(APIGatewayProxyRequestEvent input) throws Exception {
        String body = input.getBody();
        context.getLogger().log("Creating team issue: " + body);
        
        // 新しい困りごとを返す（実際の実装ではDynamoDBに保存）
        Map<String, Object> newIssue = new HashMap<>();
        newIssue.put("id", 3);
        newIssue.put("userId", "current-user");
        newIssue.put("displayName", "現在のユーザー");
        newIssue.put("content", "新しい困りごとが投稿されました");
        newIssue.put("status", "OPEN");
        newIssue.put("createdAt", System.currentTimeMillis());
        
        return createSuccessResponse(newIssue);
    }
    
    private APIGatewayProxyResponseEvent resolveTeamIssue(String issueId) throws Exception {
        context.getLogger().log("Resolving team issue: " + issueId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", issueId);
        result.put("status", "RESOLVED");
        result.put("resolvedAt", System.currentTimeMillis());
        result.put("message", "困りごとを解決済みにしました");
        
        return createSuccessResponse(result);
    }
    
    private APIGatewayProxyResponseEvent getIssueComments(String issueId) throws Exception {
        // モックコメントデータを返す
        Map<String, Object> comment1 = new HashMap<>();
        comment1.put("id", 1);
        comment1.put("issueId", issueId);
        comment1.put("userId", "user2");
        comment1.put("displayName", "佐藤花子");
        comment1.put("content", "React Hooksについては公式ドキュメントを読むのがおすすめです。useStateとuseEffectから始めてみてください。");
        comment1.put("createdAt", System.currentTimeMillis() - 3600000);
        
        return createSuccessResponse(java.util.Arrays.asList(comment1));
    }
    
    private APIGatewayProxyResponseEvent addIssueComment(String issueId, APIGatewayProxyRequestEvent input) throws Exception {
        String body = input.getBody();
        context.getLogger().log("Adding comment to issue " + issueId + ": " + body);
        
        Map<String, Object> newComment = new HashMap<>();
        newComment.put("id", 2);
        newComment.put("issueId", issueId);
        newComment.put("userId", "current-user");
        newComment.put("displayName", "現在のユーザー");
        newComment.put("content", "コメントが投稿されました");
        newComment.put("createdAt", System.currentTimeMillis());
        
        return createSuccessResponse(newComment);
    }
    
    // ヘルパーメソッド
    private APIGatewayProxyResponseEvent createSuccessResponse(Object data) throws Exception {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setBody(objectMapper.writeValueAsString(data));
        response.setHeaders(getCorsHeaders());
        return response;
    }
    
    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setBody("{\"error\":\"" + message + "\"}");
        response.setHeaders(getCorsHeaders());
        return response;
    }
    
    private Map<String, String> getCorsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
        return headers;
    }
}