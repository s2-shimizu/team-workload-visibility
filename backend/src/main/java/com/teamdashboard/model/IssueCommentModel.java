package com.teamdashboard.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class IssueCommentModel {
    private String commentId;
    private String userId;
    private String displayName;
    private String content;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    // コンストラクタ
    public IssueCommentModel() {}
    
    public IssueCommentModel(String userId, String displayName, String content) {
        this.commentId = java.util.UUID.randomUUID().toString();
        this.userId = userId;
        this.displayName = displayName;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
    
    // ゲッター・セッター
    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}