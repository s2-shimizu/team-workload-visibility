package com.teamdashboard.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class RealtimeNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public RealtimeNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 負荷状況の更新を全クライアントに通知
     */
    public void notifyWorkloadStatusUpdate(String userId, String displayName, String workloadLevel, 
                                         Integer projectCount, Integer taskCount) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "WORKLOAD_STATUS_UPDATE");
        notification.put("userId", userId);
        notification.put("displayName", displayName);
        notification.put("workloadLevel", workloadLevel);
        notification.put("projectCount", projectCount);
        notification.put("taskCount", taskCount);
        notification.put("timestamp", Instant.now().toEpochMilli());

        // 全クライアントに送信
        messagingTemplate.convertAndSend("/topic/workload-updates", notification);
        
        // 特定ユーザーにも送信（自分の更新確認用）
        messagingTemplate.convertAndSendToUser(userId, "/queue/workload-updates", notification);
    }

    /**
     * 困りごとの投稿を全クライアントに通知
     */
    public void notifyTeamIssueCreated(String issueId, String userId, String displayName, 
                                     String content, String priority) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "TEAM_ISSUE_CREATED");
        notification.put("issueId", issueId);
        notification.put("userId", userId);
        notification.put("displayName", displayName);
        notification.put("content", content);
        notification.put("priority", priority);
        notification.put("status", "OPEN");
        notification.put("timestamp", Instant.now().toEpochMilli());

        // 全クライアントに送信
        messagingTemplate.convertAndSend("/topic/issue-updates", notification);
    }

    /**
     * 困りごとの解決を全クライアントに通知
     */
    public void notifyTeamIssueResolved(String issueId, String userId, String displayName) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "TEAM_ISSUE_RESOLVED");
        notification.put("issueId", issueId);
        notification.put("userId", userId);
        notification.put("displayName", displayName);
        notification.put("status", "RESOLVED");
        notification.put("timestamp", Instant.now().toEpochMilli());

        // 全クライアントに送信
        messagingTemplate.convertAndSend("/topic/issue-updates", notification);
    }

    /**
     * 困りごとの再オープンを全クライアントに通知
     */
    public void notifyTeamIssueReopened(String issueId, String userId, String displayName) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "TEAM_ISSUE_REOPENED");
        notification.put("issueId", issueId);
        notification.put("userId", userId);
        notification.put("displayName", displayName);
        notification.put("status", "OPEN");
        notification.put("timestamp", Instant.now().toEpochMilli());

        // 全クライアントに送信
        messagingTemplate.convertAndSend("/topic/issue-updates", notification);
    }

    /**
     * 困りごとの削除を全クライアントに通知
     */
    public void notifyTeamIssueDeleted(String issueId, String userId, String displayName) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "TEAM_ISSUE_DELETED");
        notification.put("issueId", issueId);
        notification.put("userId", userId);
        notification.put("displayName", displayName);
        notification.put("timestamp", Instant.now().toEpochMilli());

        // 全クライアントに送信
        messagingTemplate.convertAndSend("/topic/issue-updates", notification);
    }

    /**
     * システム通知を全クライアントに送信
     */
    public void notifySystemMessage(String message, String type) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "SYSTEM_MESSAGE");
        notification.put("message", message);
        notification.put("messageType", type); // INFO, WARNING, ERROR
        notification.put("timestamp", Instant.now().toEpochMilli());

        // 全クライアントに送信
        messagingTemplate.convertAndSend("/topic/system-messages", notification);
    }

    /**
     * 接続状況の通知（ユーザーのオンライン/オフライン）
     */
    public void notifyUserConnectionStatus(String userId, String displayName, boolean isOnline) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "USER_CONNECTION_STATUS");
        notification.put("userId", userId);
        notification.put("displayName", displayName);
        notification.put("isOnline", isOnline);
        notification.put("timestamp", Instant.now().toEpochMilli());

        // 全クライアントに送信
        messagingTemplate.convertAndSend("/topic/user-status", notification);
    }
}