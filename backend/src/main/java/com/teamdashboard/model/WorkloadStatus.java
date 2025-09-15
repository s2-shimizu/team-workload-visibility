package com.teamdashboard.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import java.time.Instant;

@DynamoDbBean
public class WorkloadStatus {
    
    private String userId;
    private String displayName;
    private String workloadLevel;
    private Integer projectCount;
    private Integer taskCount;
    private String comment;
    private Instant updatedAt;
    private Instant createdAt;

    public WorkloadStatus() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @DynamoDbPartitionKey
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

    @DynamoDbAttribute("workloadLevel")
    public String getWorkloadLevel() {
        return workloadLevel;
    }

    public void setWorkloadLevel(String workloadLevel) {
        this.workloadLevel = workloadLevel;
    }

    @DynamoDbAttribute("projectCount")
    public Integer getProjectCount() {
        return projectCount;
    }

    public void setProjectCount(Integer projectCount) {
        this.projectCount = projectCount;
    }

    @DynamoDbAttribute("taskCount")
    public Integer getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(Integer taskCount) {
        this.taskCount = taskCount;
    }

    @DynamoDbAttribute("comment")
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @DynamoDbAttribute("updatedAt")
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @DynamoDbAttribute("createdAt")
    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // Helper method to update timestamp
    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "WorkloadStatus{" +
                "userId='" + userId + '\'' +
                ", displayName='" + displayName + '\'' +
                ", workloadLevel='" + workloadLevel + '\'' +
                ", projectCount=" + projectCount +
                ", taskCount=" + taskCount +
                ", comment='" + comment + '\'' +
                ", updatedAt=" + updatedAt +
                ", createdAt=" + createdAt +
                '}';
    }
}