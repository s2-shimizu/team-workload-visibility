package com.teamdashboard;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class SimpleApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
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
                return handleHealth();
            } else if (path.equals("/api/status")) {
                return handleApiStatus();
            } else if (path.equals("/api/workload-status")) {
                return handleWorkloadStatus(method, input);
            } else if (path.equals("/api/workload-status/my")) {
                return handleMyWorkloadStatus();
            } else if (path.startsWith("/api/team-issues")) {
                return handleTeamIssues(path, method, input);
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
        response.setBody("");
        return response;
    }
    
    private APIGatewayProxyResponseEvent handleHealth() {
        Map<String, Object> healthData = new HashMap<>();
        healthData.put("status", "OK");
        healthData.put("message", "Team Dashboard API is running");
        healthData.put("timestamp", System.currentTimeMillis());
        return createSuccessResponse(healthData);
    }
    
    private APIGatewayProxyResponseEvent handleApiStatus() {
        Map<String, Object> statusData = new HashMap<>();
        statusData.put("status", "OK");
        statusData.put("message", "Team Dashboard Spring Boot API is running");
        statusData.put("timestamp", System.currentTimeMillis());
        statusData.put("version", "1.0.0");
        return createSuccessResponse(statusData);
    }
    
    private APIGatewayProxyResponseEvent handleWorkloadStatus(String method, APIGatewayProxyRequestEvent input) {
        if ("GET".equals(method)) {
            List<Map<String, Object>> statuses = new ArrayList<>();
            
            Map<String, Object> status1 = new HashMap<>();
            status1.put("userId", "user1");
            status1.put("displayName", "田中太郎");
            status1.put("workloadLevel", "MEDIUM");
            status1.put("projectCount", 3);
            status1.put("taskCount", 15);
            status1.put("updatedAt", System.currentTimeMillis());
            statuses.add(status1);
            
            Map<String, Object> status2 = new HashMap<>();
            status2.put("userId", "user2");
            status2.put("displayName", "佐藤花子");
            status2.put("workloadLevel", "HIGH");
            status2.put("projectCount", 5);
            status2.put("taskCount", 25);
            status2.put("updatedAt", System.currentTimeMillis() - 3600000);
            statuses.add(status2);
            
            return createSuccessResponse(statuses);
        } else if ("POST".equals(method)) {
            Map<String, Object> updatedStatus = new HashMap<>();
            updatedStatus.put("userId", "current-user");
            updatedStatus.put("displayName", "現在のユーザー");
            updatedStatus.put("workloadLevel", "MEDIUM");
            updatedStatus.put("projectCount", 2);
            updatedStatus.put("taskCount", 8);
            updatedStatus.put("message", "負荷状況を更新しました");
            updatedStatus.put("updatedAt", System.currentTimeMillis());
            return createSuccessResponse(updatedStatus);
        }
        return createErrorResponse(405, "Method Not Allowed");
    }
    
    private APIGatewayProxyResponseEvent handleMyWorkloadStatus() {
        Map<String, Object> myStatus = new HashMap<>();
        myStatus.put("userId", "current-user");
        myStatus.put("displayName", "現在のユーザー");
        myStatus.put("workloadLevel", "LOW");
        myStatus.put("projectCount", 2);
        myStatus.put("taskCount", 8);
        myStatus.put("updatedAt", System.currentTimeMillis());
        return createSuccessResponse(myStatus);
    }
    
    private APIGatewayProxyResponseEvent handleTeamIssues(String path, String method, APIGatewayProxyRequestEvent input) {
        if (path.equals("/api/team-issues")) {
            if ("GET".equals(method)) {
                List<Map<String, Object>> issues = new ArrayList<>();
                
                Map<String, Object> issue1 = new HashMap<>();
                issue1.put("issueId", "issue-1");
                issue1.put("userId", "user1");
                issue1.put("displayName", "田中太郎");
                issue1.put("content", "新しい技術の学習で詰まっています。React Hooksの使い方がよくわからず、コンポーネントの状態管理で困っています。");
                issue1.put("status", "OPEN");
                issue1.put("priority", "HIGH");
                issue1.put("createdAt", System.currentTimeMillis() - 86400000);
                issues.add(issue1);
                
                Map<String, Object> issue2 = new HashMap<>();
                issue2.put("issueId", "issue-2");
                issue2.put("userId", "user2");
                issue2.put("displayName", "佐藤花子");
                issue2.put("content", "プロジェクトの進め方で悩んでいます。タスクの優先順位をどう決めればよいかアドバイスをください。");
                issue2.put("status", "RESOLVED");
                issue2.put("priority", "MEDIUM");
                issue2.put("createdAt", System.currentTimeMillis() - 172800000);
                issues.add(issue2);
                
                return createSuccessResponse(issues);
            } else if ("POST".equals(method)) {
                Map<String, Object> newIssue = new HashMap<>();
                newIssue.put("issueId", "issue-" + System.currentTimeMillis());
                newIssue.put("userId", "current-user");
                newIssue.put("displayName", "現在のユーザー");
                newIssue.put("content", "新しい困りごとが投稿されました");
                newIssue.put("status", "OPEN");
                newIssue.put("priority", "MEDIUM");
                newIssue.put("createdAt", System.currentTimeMillis());
                newIssue.put("message", "新しい困りごとが投稿されました");
                return createSuccessResponse(newIssue);
            }
        } else if (path.equals("/api/team-issues/open")) {
            List<Map<String, Object>> openIssues = new ArrayList<>();
            
            Map<String, Object> issue1 = new HashMap<>();
            issue1.put("issueId", "issue-1");
            issue1.put("userId", "user1");
            issue1.put("displayName", "田中太郎");
            issue1.put("content", "新しい技術の学習で詰まっています。React Hooksの使い方がよくわからず、コンポーネントの状態管理で困っています。");
            issue1.put("status", "OPEN");
            issue1.put("priority", "HIGH");
            issue1.put("createdAt", System.currentTimeMillis() - 86400000);
            openIssues.add(issue1);
            
            return createSuccessResponse(openIssues);
        } else if (path.equals("/api/team-issues/statistics")) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("open", 8);
            stats.put("resolved", 15);
            stats.put("total", 23);
            stats.put("highPriority", 3);
            stats.put("mediumPriority", 12);
            stats.put("lowPriority", 8);
            return createSuccessResponse(stats);
        }
        
        return createErrorResponse(404, "Team issues endpoint not found");
    }
    
    private APIGatewayProxyResponseEvent createSuccessResponse(Object data) {
        try {
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(200);
            response.setHeaders(getCorsHeaders());
            response.setBody(objectMapper.writeValueAsString(data));
            return response;
        } catch (Exception e) {
            return createErrorResponse(500, "JSON serialization error: " + e.getMessage());
        }
    }
    
    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setHeaders(getCorsHeaders());
        
        Map<String, String> errorBody = new HashMap<>();
        errorBody.put("error", message);
        errorBody.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        try {
            response.setBody(objectMapper.writeValueAsString(errorBody));
        } catch (Exception e) {
            response.setBody("{\"error\":\"" + message + "\"}");
        }
        
        return response;
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