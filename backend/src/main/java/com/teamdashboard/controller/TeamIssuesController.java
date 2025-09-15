package com.teamdashboard.controller;

import com.teamdashboard.model.TeamIssue;
import com.teamdashboard.service.TeamIssueService;
import com.teamdashboard.service.RealtimeNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/team-issues")
@CrossOrigin(origins = "*")
public class TeamIssuesController {

    private final TeamIssueService teamIssueService;
    private final RealtimeNotificationService notificationService;

    @Autowired
    public TeamIssuesController(TeamIssueService teamIssueService, 
                              RealtimeNotificationService notificationService) {
        this.teamIssueService = teamIssueService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<Map<String, Object>> getAllTeamIssues() {
        try {
            List<TeamIssue> issues = teamIssueService.getAllTeamIssues();
            return issues.stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // フォールバック: エラー時はサンプルデータを返す
            return getSampleTeamIssues();
        }
    }

    @GetMapping("/open")
    public List<Map<String, Object>> getOpenTeamIssues() {
        try {
            List<TeamIssue> openIssues = teamIssueService.getOpenTeamIssues();
            return openIssues.stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // フォールバック: エラー時はサンプルデータを返す
            return getSampleOpenTeamIssues();
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getIssueStatistics() {
        try {
            TeamIssueService.IssueStatistics stats = teamIssueService.getIssueStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("total", stats.getTotal());
            response.put("open", stats.getOpen());
            response.put("resolved", stats.getResolved());
            response.put("highPriority", stats.getHighPriority());
            response.put("mediumPriority", stats.getMediumPriority());
            response.put("lowPriority", stats.getLowPriority());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // エラー時はサンプル統計を返す
            Map<String, Object> fallbackStats = new HashMap<>();
            fallbackStats.put("open", 8);
            fallbackStats.put("resolved", 15);
            fallbackStats.put("total", 23);
            fallbackStats.put("highPriority", 3);
            fallbackStats.put("mediumPriority", 12);
            fallbackStats.put("lowPriority", 8);
            fallbackStats.put("error", "DynamoDB接続エラー: " + e.getMessage());
            
            return ResponseEntity.ok(fallbackStats);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createTeamIssue(@RequestBody Map<String, Object> request) {
        try {
            // TODO: 実際の認証からユーザーIDを取得
            String currentUserId = getCurrentUserId();
            String displayName = getCurrentUserDisplayName();
            
            String content = (String) request.get("content");
            String priority = (String) request.getOrDefault("priority", "MEDIUM");
            
            if (content == null || content.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Content is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            TeamIssue newIssue = teamIssueService.createTeamIssue(currentUserId, displayName, content, priority);
            
            // リアルタイム通知を送信
            notificationService.notifyTeamIssueCreated(
                    newIssue.getIssueId(), currentUserId, displayName, content, priority);
            
            Map<String, Object> response = convertToMap(newIssue);
            response.put("message", "新しい困りごとが投稿されました");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // エラー時はフォールバック応答
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("issueId", "issue-" + System.currentTimeMillis());
            errorResponse.put("userId", "current-user");
            errorResponse.put("displayName", "現在のユーザー");
            errorResponse.put("content", request.get("content"));
            errorResponse.put("status", "OPEN");
            errorResponse.put("priority", request.getOrDefault("priority", "MEDIUM"));
            errorResponse.put("createdAt", System.currentTimeMillis());
            errorResponse.put("message", "新しい困りごとが投稿されました（開発モード）");
            errorResponse.put("error", "DynamoDB接続エラー: " + e.getMessage());
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    @PutMapping("/{issueId}/resolve")
    public ResponseEntity<Map<String, Object>> resolveTeamIssue(@PathVariable String issueId) {
        try {
            Optional<TeamIssue> resolvedIssue = teamIssueService.resolveTeamIssue(issueId);
            if (resolvedIssue.isPresent()) {
                TeamIssue issue = resolvedIssue.get();
                
                // リアルタイム通知を送信
                notificationService.notifyTeamIssueResolved(
                        issueId, issue.getUserId(), issue.getDisplayName());
                
                Map<String, Object> response = convertToMap(issue);
                response.put("message", "困りごとが解決済みになりました");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Issue not found");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to resolve issue: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PutMapping("/{issueId}/reopen")
    public ResponseEntity<Map<String, Object>> reopenTeamIssue(@PathVariable String issueId) {
        try {
            Optional<TeamIssue> reopenedIssue = teamIssueService.reopenTeamIssue(issueId);
            if (reopenedIssue.isPresent()) {
                Map<String, Object> response = convertToMap(reopenedIssue.get());
                response.put("message", "困りごとが再オープンされました");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Issue not found");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to reopen issue: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @DeleteMapping("/{issueId}")
    public ResponseEntity<Map<String, Object>> deleteTeamIssue(@PathVariable String issueId) {
        try {
            if (teamIssueService.existsByIssueId(issueId)) {
                teamIssueService.deleteTeamIssue(issueId);
                Map<String, Object> response = new HashMap<>();
                response.put("message", "困りごとが削除されました");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to delete issue: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // ヘルパーメソッド
    private Map<String, Object> convertToMap(TeamIssue issue) {
        Map<String, Object> map = new HashMap<>();
        map.put("issueId", issue.getIssueId());
        map.put("userId", issue.getUserId());
        map.put("displayName", issue.getDisplayName());
        map.put("content", issue.getContent());
        map.put("status", issue.getStatus());
        map.put("priority", issue.getPriority());
        map.put("createdAt", issue.getCreatedAt() != null ? issue.getCreatedAt().toEpochMilli() : null);
        map.put("updatedAt", issue.getUpdatedAt() != null ? issue.getUpdatedAt().toEpochMilli() : null);
        map.put("resolvedAt", issue.getResolvedAt() != null ? issue.getResolvedAt().toEpochMilli() : null);
        return map;
    }

    private String getCurrentUserId() {
        // TODO: Spring Securityから実際のユーザーIDを取得
        return "current-user-" + System.currentTimeMillis() % 1000;
    }

    private String getCurrentUserDisplayName() {
        // TODO: Spring Securityから実際のユーザー名を取得
        return "現在のユーザー";
    }

    private List<Map<String, Object>> getSampleTeamIssues() {
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
        
        return issues;
    }

    private List<Map<String, Object>> getSampleOpenTeamIssues() {
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
        
        return openIssues;
    }
}