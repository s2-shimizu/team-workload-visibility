package com.teamdashboard.controller;

import com.teamdashboard.dto.WorkloadStatusRequestDTO;
import com.teamdashboard.dto.WorkloadStatusResponseDTO;
import com.teamdashboard.entity.WorkloadStatus;
import com.teamdashboard.service.WorkloadStatusService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import java.util.stream.Collectors;

/**
 * 負荷状況管理のREST APIコントローラー
 */
@RestController
@RequestMapping("/api/workload-status")
@CrossOrigin(origins = "*")
@Profile("!dynamodb")
public class WorkloadStatusController {

    private final WorkloadStatusService workloadStatusService;

    @Autowired
    public WorkloadStatusController(WorkloadStatusService workloadStatusService) {
        this.workloadStatusService = workloadStatusService;
    }

    /**
     * 全メンバーの負荷状況を取得
     * GET /api/workload-status
     * @return 全メンバーの負荷状況リスト
     */
    @GetMapping
    public ResponseEntity<List<WorkloadStatusResponseDTO>> getAllWorkloadStatuses() {
        try {
            List<WorkloadStatus> workloadStatuses = workloadStatusService.getAllWorkloadStatuses();
            List<WorkloadStatusResponseDTO> response = workloadStatuses.stream()
                    .map(WorkloadStatusResponseDTO::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 自分の負荷状況を取得
     * GET /api/workload-status/my
     * @param principal 認証されたユーザー情報
     * @return 自分の負荷状況
     */
    @GetMapping("/my")
    public ResponseEntity<WorkloadStatusResponseDTO> getMyWorkloadStatus(Principal principal) {
        try {
            String username = principal != null ? principal.getName() : "testuser";
            Optional<WorkloadStatus> workloadStatus = workloadStatusService.getWorkloadStatusByUsername(username);
            
            if (workloadStatus.isPresent()) {
                WorkloadStatusResponseDTO response = WorkloadStatusResponseDTO.fromEntity(workloadStatus.get());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 指定されたユーザーIDの負荷状況を取得
     * GET /api/workload-status/{userId}
     * @param userId ユーザーID
     * @return 指定されたユーザーの負荷状況
     */
    @GetMapping("/{userId}")
    public ResponseEntity<WorkloadStatusResponseDTO> getWorkloadStatusByUserId(@PathVariable Long userId) {
        try {
            Optional<WorkloadStatus> workloadStatus = workloadStatusService.getWorkloadStatusByUserId(userId);
            
            if (workloadStatus.isPresent()) {
                WorkloadStatusResponseDTO response = WorkloadStatusResponseDTO.fromEntity(workloadStatus.get());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 負荷状況を更新
     * POST /api/workload-status
     * @param request 負荷状況更新リクエスト
     * @param principal 認証されたユーザー情報
     * @return 更新された負荷状況
     */
    @PostMapping
    public ResponseEntity<WorkloadStatusResponseDTO> updateWorkloadStatus(
            @Valid @RequestBody WorkloadStatusRequestDTO request,
            Principal principal) {
        try {
            String username = principal != null ? principal.getName() : "testuser";
            
            WorkloadStatus updatedStatus = workloadStatusService.updateWorkloadStatusByUsername(
                    username,
                    request.getWorkloadLevel(),
                    request.getProjectCount(),
                    request.getTaskCount()
            );
            
            WorkloadStatusResponseDTO response = WorkloadStatusResponseDTO.fromEntity(updatedStatus);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            // ユーザーが見つからない場合など
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 指定されたユーザーの負荷状況を更新（管理者用）
     * PUT /api/workload-status/{userId}
     * @param userId ユーザーID
     * @param request 負荷状況更新リクエスト
     * @return 更新された負荷状況
     */
    @PutMapping("/{userId}")
    public ResponseEntity<WorkloadStatusResponseDTO> updateWorkloadStatusByUserId(
            @PathVariable Long userId,
            @Valid @RequestBody WorkloadStatusRequestDTO request) {
        try {
            WorkloadStatus updatedStatus = workloadStatusService.updateWorkloadStatus(
                    userId,
                    request.getWorkloadLevel(),
                    request.getProjectCount(),
                    request.getTaskCount()
            );
            
            WorkloadStatusResponseDTO response = WorkloadStatusResponseDTO.fromEntity(updatedStatus);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            // ユーザーが見つからない場合など
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
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