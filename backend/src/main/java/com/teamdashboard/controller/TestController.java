package com.teamdashboard.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TestController {
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Spring Boot server is running");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/workload-status")
    public ResponseEntity<List<Map<String, Object>>> getWorkloadStatuses() {
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
        
        return ResponseEntity.ok(statuses);
    }
    
    @GetMapping("/workload-status/my")
    public ResponseEntity<Map<String, Object>> getMyWorkloadStatus() {
        Map<String, Object> myStatus = new HashMap<>();
        myStatus.put("userId", "current-user");
        myStatus.put("displayName", "現在のユーザー");
        myStatus.put("workloadLevel", "LOW");
        myStatus.put("projectCount", 2);
        myStatus.put("taskCount", 8);
        myStatus.put("updatedAt", System.currentTimeMillis());
        
        return ResponseEntity.ok(myStatus);
    }
    
    @PostMapping("/workload-status")
    public ResponseEntity<Map<String, Object>> updateWorkloadStatus(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("userId", request.getOrDefault("userId", "current-user"));
        response.put("displayName", request.getOrDefault("displayName", "現在のユーザー"));
        response.put("workloadLevel", request.getOrDefault("workloadLevel", "MEDIUM"));
        response.put("projectCount", request.getOrDefault("projectCount", 0));
        response.put("taskCount", request.getOrDefault("taskCount", 0));
        response.put("updatedAt", System.currentTimeMillis());
        response.put("message", "負荷状況を更新しました");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/team-issues")
    public ResponseEntity<List<Map<String, Object>>> getTeamIssues() {
        List<Map<String, Object>> issues = new ArrayList<>();
        
        Map<String, Object> issue1 = new HashMap<>();
        issue1.put("issueId", "1");
        issue1.put("userId", "user1");
        issue1.put("displayName", "田中太郎");
        issue1.put("content", "新しい技術の学習で詰まっています。React Hooksの使い方がよくわからず、コンポーネントの状態管理で困っています。");
        issue1.put("status", "OPEN");
        issue1.put("createdAt", System.currentTimeMillis() - 7200000);
        issues.add(issue1);
        
        Map<String, Object> issue2 = new HashMap<>();
        issue2.put("issueId", "2");
        issue2.put("userId", "user2");
        issue2.put("displayName", "佐藤花子");
        issue2.put("content", "プロジェクトの進め方で悩んでいます。タスクの優先順位をどう決めればよいかアドバイスをください。");
        issue2.put("status", "RESOLVED");
        issue2.put("createdAt", System.currentTimeMillis() - 86400000);
        issue2.put("resolvedAt", System.currentTimeMillis() - 3600000);
        issues.add(issue2);
        
        return ResponseEntity.ok(issues);
    }
    
    @PostMapping("/team-issues")
    public ResponseEntity<Map<String, Object>> createTeamIssue(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("issueId", "3");
        response.put("userId", request.getOrDefault("userId", "current-user"));
        response.put("displayName", request.getOrDefault("displayName", "現在のユーザー"));
        response.put("content", request.getOrDefault("content", "新しい困りごとが投稿されました"));
        response.put("status", "OPEN");
        response.put("createdAt", System.currentTimeMillis());
        response.put("message", "困りごとを投稿しました");
        
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/team-issues/{issueId}/resolve")
    public ResponseEntity<Map<String, Object>> resolveTeamIssue(@PathVariable String issueId) {
        Map<String, Object> response = new HashMap<>();
        response.put("issueId", issueId);
        response.put("status", "RESOLVED");
        response.put("resolvedAt", System.currentTimeMillis());
        response.put("message", "困りごとを解決済みにしました");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/team-issues/{issueId}/comments")
    public ResponseEntity<List<Map<String, Object>>> getIssueComments(@PathVariable String issueId) {
        List<Map<String, Object>> comments = new ArrayList<>();
        
        Map<String, Object> comment1 = new HashMap<>();
        comment1.put("id", "1");
        comment1.put("issueId", issueId);
        comment1.put("userId", "user2");
        comment1.put("displayName", "佐藤花子");
        comment1.put("content", "React Hooksについては公式ドキュメントを読むのがおすすめです。useStateとuseEffectから始めてみてください。");
        comment1.put("createdAt", System.currentTimeMillis() - 3600000);
        comments.add(comment1);
        
        return ResponseEntity.ok(comments);
    }
    
    @PostMapping("/team-issues/{issueId}/comments")
    public ResponseEntity<Map<String, Object>> addIssueComment(@PathVariable String issueId, @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", "2");
        response.put("issueId", issueId);
        response.put("userId", request.getOrDefault("userId", "current-user"));
        response.put("displayName", request.getOrDefault("displayName", "現在のユーザー"));
        response.put("content", request.getOrDefault("content", "コメントが投稿されました"));
        response.put("createdAt", System.currentTimeMillis());
        response.put("message", "コメントを投稿しました");
        
        return ResponseEntity.ok(response);
    }
}