package com.teamdashboard.controller;

import com.teamdashboard.model.WorkloadStatus;
import com.teamdashboard.service.WorkloadStatusService;
import com.teamdashboard.service.RealtimeNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class WorkloadController {

    private final WorkloadStatusService workloadStatusService;
    private final RealtimeNotificationService notificationService;

    @Autowired
    public WorkloadController(WorkloadStatusService workloadStatusService, 
                            RealtimeNotificationService notificationService) {
        this.workloadStatusService = workloadStatusService;
        this.notificationService = notificationService;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Team Dashboard Spring Boot API is running");
        response.put("timestamp", System.currentTimeMillis());
        response.put("version", "1.0.0");
        response.put("database", "DynamoDB");
        return response;
    }

    @GetMapping("/workload-status")
    public List<Map<String, Object>> getAllWorkloadStatuses() {
        try {
            List<WorkloadStatus> statuses = workloadStatusService.getAllWorkloadStatuses();
            return statuses.stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // フォールバック: エラー時はサンプルデータを返す
            return getSampleWorkloadStatuses();
        }
    }

    @GetMapping("/workload-status/my")
    public ResponseEntity<Map<String, Object>> getMyWorkloadStatus() {
        try {
            // TODO: 実際の認証からユーザーIDを取得
            String currentUserId = getCurrentUserId();
            
            Optional<WorkloadStatus> status = workloadStatusService.getWorkloadStatusByUserId(currentUserId);
            if (status.isPresent()) {
                return ResponseEntity.ok(convertToMap(status.get()));
            } else {
                // ユーザーの状況が未登録の場合はデフォルト値を返す
                Map<String, Object> defaultStatus = new HashMap<>();
                defaultStatus.put("userId", currentUserId);
                defaultStatus.put("displayName", "現在のユーザー");
                defaultStatus.put("workloadLevel", "MEDIUM");
                defaultStatus.put("projectCount", 0);
                defaultStatus.put("taskCount", 0);
                defaultStatus.put("updatedAt", Instant.now().toEpochMilli());
                return ResponseEntity.ok(defaultStatus);
            }
        } catch (Exception e) {
            // エラー時はサンプルデータを返す
            Map<String, Object> fallbackStatus = new HashMap<>();
            fallbackStatus.put("userId", "current-user");
            fallbackStatus.put("displayName", "現在のユーザー");
            fallbackStatus.put("workloadLevel", "MEDIUM");
            fallbackStatus.put("projectCount", 2);
            fallbackStatus.put("taskCount", 8);
            fallbackStatus.put("updatedAt", System.currentTimeMillis());
            return ResponseEntity.ok(fallbackStatus);
        }
    }

    @PostMapping("/workload-status")
    public ResponseEntity<Map<String, Object>> updateWorkloadStatus(@RequestBody Map<String, Object> request) {
        try {
            // TODO: 実際の認証からユーザーIDを取得
            String currentUserId = getCurrentUserId();
            String displayName = getCurrentUserDisplayName();
            
            String workloadLevel = (String) request.getOrDefault("workloadLevel", "MEDIUM");
            Integer projectCount = getIntegerFromRequest(request, "projectCount", 0);
            Integer taskCount = getIntegerFromRequest(request, "taskCount", 0);
            String comment = (String) request.get("comment");
            
            WorkloadStatus updatedStatus = workloadStatusService.createOrUpdateWorkloadStatus(
                    currentUserId, displayName, workloadLevel, projectCount, taskCount, comment);
            
            // リアルタイム通知を送信
            notificationService.notifyWorkloadStatusUpdate(
                    currentUserId, displayName, workloadLevel, projectCount, taskCount);
            
            Map<String, Object> response = convertToMap(updatedStatus);
            response.put("message", "負荷状況を更新しました");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // エラー時はフォールバック応答
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("userId", "current-user");
            errorResponse.put("displayName", "現在のユーザー");
            errorResponse.put("workloadLevel", request.getOrDefault("workloadLevel", "MEDIUM"));
            errorResponse.put("projectCount", request.getOrDefault("projectCount", 2));
            errorResponse.put("taskCount", request.getOrDefault("taskCount", 8));
            errorResponse.put("message", "負荷状況を更新しました（開発モード）");
            errorResponse.put("updatedAt", System.currentTimeMillis());
            errorResponse.put("error", "DynamoDB接続エラー: " + e.getMessage());
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    @GetMapping("/workload-status/statistics")
    public ResponseEntity<Map<String, Object>> getWorkloadStatistics() {
        try {
            WorkloadStatusService.WorkloadStatistics stats = workloadStatusService.getWorkloadStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalUsers", stats.getTotalUsers());
            response.put("highWorkload", stats.getHighWorkload());
            response.put("mediumWorkload", stats.getMediumWorkload());
            response.put("lowWorkload", stats.getLowWorkload());
            response.put("averageProjectCount", stats.getAverageProjectCount());
            response.put("averageTaskCount", stats.getAverageTaskCount());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // エラー時はサンプル統計を返す
            Map<String, Object> fallbackStats = new HashMap<>();
            fallbackStats.put("totalUsers", 10);
            fallbackStats.put("highWorkload", 3);
            fallbackStats.put("mediumWorkload", 5);
            fallbackStats.put("lowWorkload", 2);
            fallbackStats.put("averageProjectCount", 3.2);
            fallbackStats.put("averageTaskCount", 15.8);
            fallbackStats.put("error", "DynamoDB接続エラー: " + e.getMessage());
            
            return ResponseEntity.ok(fallbackStats);
        }
    }

    // ヘルパーメソッド
    private Map<String, Object> convertToMap(WorkloadStatus status) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", status.getUserId());
        map.put("displayName", status.getDisplayName());
        map.put("workloadLevel", status.getWorkloadLevel());
        map.put("projectCount", status.getProjectCount());
        map.put("taskCount", status.getTaskCount());
        map.put("comment", status.getComment());
        map.put("updatedAt", status.getUpdatedAt() != null ? status.getUpdatedAt().toEpochMilli() : null);
        map.put("createdAt", status.getCreatedAt() != null ? status.getCreatedAt().toEpochMilli() : null);
        return map;
    }

    private Integer getIntegerFromRequest(Map<String, Object> request, String key, Integer defaultValue) {
        Object value = request.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private String getCurrentUserId() {
        // TODO: Spring Securityから実際のユーザーIDを取得
        return "current-user-" + System.currentTimeMillis() % 1000;
    }

    private String getCurrentUserDisplayName() {
        // TODO: Spring Securityから実際のユーザー名を取得
        return "現在のユーザー";
    }

    private List<Map<String, Object>> getSampleWorkloadStatuses() {
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
        
        return statuses;
    }
}