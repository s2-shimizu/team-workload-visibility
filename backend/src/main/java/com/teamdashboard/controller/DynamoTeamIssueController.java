package com.teamdashboard.controller;

import com.teamdashboard.model.TeamIssueModel;
import com.teamdashboard.model.IssueCommentModel;
import com.teamdashboard.service.DynamoTeamIssueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Profile;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/team-issues")
@CrossOrigin(origins = "*")
@Profile({"lambda", "dynamodb"})
public class DynamoTeamIssueController {
    
    @Autowired
    private DynamoTeamIssueService teamIssueService;
    
    @GetMapping
    public ResponseEntity<List<TeamIssueModel>> getAllTeamIssues() {
        try {
            List<TeamIssueModel> issues = teamIssueService.getAllTeamIssues();
            return ResponseEntity.ok(issues);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{issueId}")
    public ResponseEntity<TeamIssueModel> getTeamIssueById(@PathVariable String issueId) {
        try {
            Optional<TeamIssueModel> issue = teamIssueService.getTeamIssueById(issueId);
            return issue.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping
    public ResponseEntity<TeamIssueModel> createTeamIssue(@Valid @RequestBody CreateIssueRequest request) {
        try {
            // デフォルトユーザー情報を設定（認証機能がない場合）
            String userId = request.getUserId() != null ? request.getUserId() : "current-user";
            String displayName = request.getDisplayName() != null ? request.getDisplayName() : "現在のユーザー";
            
            TeamIssueModel created = teamIssueService.createTeamIssue(userId, displayName, request.getContent());
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    @PutMapping("/{issueId}/resolve")
    public ResponseEntity<TeamIssueModel> resolveTeamIssue(@PathVariable String issueId) {
        try {
            TeamIssueModel resolved = teamIssueService.resolveIssue(issueId);
            return ResponseEntity.ok(resolved);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/{issueId}/comments")
    public ResponseEntity<TeamIssueModel> addComment(
            @PathVariable String issueId, 
            @Valid @RequestBody AddCommentRequest request) {
        try {
            // デフォルトユーザー情報を設定（認証機能がない場合）
            String userId = request.getUserId() != null ? request.getUserId() : "current-user";
            String displayName = request.getDisplayName() != null ? request.getDisplayName() : "現在のユーザー";
            
            TeamIssueModel updated = teamIssueService.addComment(issueId, userId, displayName, request.getContent());
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    @GetMapping("/{issueId}/comments")
    public ResponseEntity<List<IssueCommentModel>> getComments(@PathVariable String issueId) {
        try {
            Optional<TeamIssueModel> issue = teamIssueService.getTeamIssueById(issueId);
            return issue.map(i -> ResponseEntity.ok(i.getComments()))
                       .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/open")
    public ResponseEntity<List<TeamIssueModel>> getOpenIssues() {
        try {
            List<TeamIssueModel> openIssues = teamIssueService.getOpenIssues();
            return ResponseEntity.ok(openIssues);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/resolved")
    public ResponseEntity<List<TeamIssueModel>> getResolvedIssues() {
        try {
            List<TeamIssueModel> resolvedIssues = teamIssueService.getResolvedIssues();
            return ResponseEntity.ok(resolvedIssues);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getIssueStatistics() {
        try {
            Map<String, Long> statistics = Map.of(
                "open", teamIssueService.countOpenIssues(),
                "resolved", teamIssueService.countResolvedIssues()
            );
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @DeleteMapping("/{issueId}")
    public ResponseEntity<Void> deleteTeamIssue(@PathVariable String issueId) {
        try {
            teamIssueService.deleteTeamIssue(issueId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // DTOクラス
    public static class CreateIssueRequest {
        private String userId;
        private String displayName;
        private String content;
        
        // ゲッター・セッター
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
    
    public static class AddCommentRequest {
        private String userId;
        private String displayName;
        private String content;
        
        // ゲッター・セッター
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
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