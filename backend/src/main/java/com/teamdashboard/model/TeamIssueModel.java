package com.teamdashboard.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.teamdashboard.entity.IssueStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TeamIssueModel {
    private String issueId;
    private String userId;
    private String displayName;
    private String content;
    private IssueStatus status;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime resolvedAt;
    
    private List<IssueCommentModel> comments = new ArrayList<>();
    
    // コンストラクタ
    public TeamIssueModel() {}
    
    public TeamIssueModel(String userId, String displayName, String content) {
        this.issueId = java.util.UUID.randomUUID().toString();
        this.userId = userId;
        this.displayName = displayName;
        this.content = content;
        this.status = IssueStatus.OPEN;
        this.createdAt = LocalDateTime.now();
    }
    
    // ゲッター・セッター
    public String getIssueId() { return issueId; }
    public void setIssueId(String issueId) { this.issueId = issueId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public IssueStatus getStatus() { return status; }
    public void setStatus(IssueStatus status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    
    public List<IssueCommentModel> getComments() { return comments; }
    public void setComments(List<IssueCommentModel> comments) { this.comments = comments; }
    
    public void addComment(IssueCommentModel comment) {
        this.comments.add(comment);
    }
    
    public void resolve() {
        this.status = IssueStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
    }
}

// IssueStatusは既存のenumを使用