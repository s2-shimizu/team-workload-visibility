package com.teamdashboard.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import java.time.Instant;

@DynamoDbBean
public class TeamIssue {
    
    private String issueId;
    private String userId;
    private String displayName;
    private String content;
    private String status;
    private String priority;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant resolvedAt;

    public TeamIssue() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.status = "OPEN";
        this.priority = "MEDIUM";
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("issueId")
    public String getIssueId() {
        return issueId;
    }

    public void setIssueId(String issueId) {
        this.issueId = issueId;
    }

    @DynamoDbAttribute("userId")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @DynamoDbAttribute("displayName")
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @DynamoDbAttribute("content")
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @DynamoDbAttribute("status")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        if ("RESOLVED".equals(status) && this.resolvedAt == null) {
            this.resolvedAt = Instant.now();
        }
        this.updatedAt = Instant.now();
    }

    @DynamoDbAttribute("priority")
    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    @DynamoDbAttribute("createdAt")
    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @DynamoDbAttribute("updatedAt")
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @DynamoDbAttribute("resolvedAt")
    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    // Helper method to generate issue ID
    public void generateIssueId() {
        if (this.issueId == null) {
            this.issueId = "issue-" + System.currentTimeMillis() + "-" + 
                          (userId != null ? userId.hashCode() : "unknown");
        }
    }

    // Helper method to update timestamp
    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "TeamIssue{" +
                "issueId='" + issueId + '\'' +
                ", userId='" + userId + '\'' +
                ", displayName='" + displayName + '\'' +
                ", content='" + content + '\'' +
                ", status='" + status + '\'' +
                ", priority='" + priority + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", resolvedAt=" + resolvedAt +
                '}';
    }
}