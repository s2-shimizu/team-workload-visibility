package com.teamdashboard.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.teamdashboard.entity.WorkloadLevel;
import java.time.LocalDateTime;

public class WorkloadStatusModel {
    private String userId;
    private String displayName;
    private String department;
    private WorkloadLevel workloadLevel;
    private Integer projectCount;
    private Integer taskCount;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // コンストラクタ
    public WorkloadStatusModel() {}
    
    public WorkloadStatusModel(String userId, String displayName, WorkloadLevel workloadLevel) {
        this.userId = userId;
        this.displayName = displayName;
        this.workloadLevel = workloadLevel;
        this.updatedAt = LocalDateTime.now();
    }
    
    // ゲッター・セッター
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public WorkloadLevel getWorkloadLevel() { return workloadLevel; }
    public void setWorkloadLevel(WorkloadLevel workloadLevel) { this.workloadLevel = workloadLevel; }
    
    public Integer getProjectCount() { return projectCount; }
    public void setProjectCount(Integer projectCount) { this.projectCount = projectCount; }
    
    public Integer getTaskCount() { return taskCount; }
    public void setTaskCount(Integer taskCount) { this.taskCount = taskCount; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

// WorkloadLevelは既存のenumを使用