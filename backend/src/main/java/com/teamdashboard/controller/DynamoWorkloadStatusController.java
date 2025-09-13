package com.teamdashboard.controller;

import com.teamdashboard.model.WorkloadStatusModel;
import com.teamdashboard.entity.WorkloadLevel;
import com.teamdashboard.service.DynamoWorkloadStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Profile;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workload-status")
@CrossOrigin(origins = "*")
@Profile({"lambda", "dynamodb"})
public class DynamoWorkloadStatusController {
    
    @Autowired
    private DynamoWorkloadStatusService workloadStatusService;
    
    @GetMapping
    public ResponseEntity<List<WorkloadStatusModel>> getAllWorkloadStatuses() {
        try {
            List<WorkloadStatusModel> statuses = workloadStatusService.getAllWorkloadStatuses();
            return ResponseEntity.ok(statuses);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(null);
        }
    }
    
    @GetMapping("/my")
    public ResponseEntity<WorkloadStatusModel> getMyWorkloadStatus(@RequestParam(required = false) String userId) {
        try {
            // デフォルトユーザーIDを設定（認証機能がない場合）
            String targetUserId = userId != null ? userId : "current-user";
            WorkloadStatusModel status = workloadStatusService.getWorkloadStatusByUserId(targetUserId);
            if (status != null) {
                return ResponseEntity.ok(status);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    @PostMapping
    public ResponseEntity<WorkloadStatusModel> updateWorkloadStatus(@Valid @RequestBody WorkloadStatusModel request) {
        try {
            // デフォルトユーザーIDを設定（認証機能がない場合）
            if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
                request.setUserId("current-user");
            }
            if (request.getDisplayName() == null || request.getDisplayName().trim().isEmpty()) {
                request.setDisplayName("現在のユーザー");
            }
            
            WorkloadStatusModel updated = workloadStatusService.updateWorkloadStatus(request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    @PutMapping("/{userId}")
    public ResponseEntity<WorkloadStatusModel> updateWorkloadStatusByUserId(
            @PathVariable String userId, 
            @Valid @RequestBody WorkloadStatusModel request) {
        try {
            request.setUserId(userId);
            WorkloadStatusModel updated = workloadStatusService.updateWorkloadStatus(request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteWorkloadStatus(@PathVariable String userId) {
        try {
            workloadStatusService.deleteWorkloadStatus(userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/high-workload")
    public ResponseEntity<List<WorkloadStatusModel>> getHighWorkloadUsers() {
        try {
            List<WorkloadStatusModel> highWorkloadUsers = workloadStatusService.getHighWorkloadUsers();
            return ResponseEntity.ok(highWorkloadUsers);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getWorkloadStatistics() {
        try {
            Map<String, Long> statistics = Map.of(
                "high", workloadStatusService.countByWorkloadLevel(WorkloadLevel.HIGH),
                "medium", workloadStatusService.countByWorkloadLevel(WorkloadLevel.MEDIUM),
                "low", workloadStatusService.countByWorkloadLevel(WorkloadLevel.LOW)
            );
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * エラーハンドリング用の例外ハンドラー
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        ErrorResponse error = new ErrorResponse("RUNTIME_ERROR", e.getMessage());
        return ResponseEntity.status(500).body(error);
    }

    /**
     * エラーレスポンス用のクラス
     */
    public static class ErrorResponse {
        private String error;
        private String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }

        // Getters and Setters
        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}