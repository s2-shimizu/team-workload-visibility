package com.teamdashboard.dto;

import com.teamdashboard.entity.WorkloadLevel;
import com.teamdashboard.entity.WorkloadStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 負荷状況レスポンス用DTO
 */
public class WorkloadStatusResponseDTO {

    private Long id;
    private Long userId;
    private String username;
    private String displayName;
    private String department;
    private WorkloadLevel workloadLevel;
    private String workloadLevelDisplay;
    private Integer projectCount;
    private Integer taskCount;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Constructors
    public WorkloadStatusResponseDTO() {}

    public WorkloadStatusResponseDTO(Long id, Long userId, String username, String displayName, 
                                   String department, WorkloadLevel workloadLevel, 
                                   Integer projectCount, Integer taskCount, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.department = department;
        this.workloadLevel = workloadLevel;
        this.workloadLevelDisplay = workloadLevel != null ? workloadLevel.getDisplayName() : null;
        this.projectCount = projectCount;
        this.taskCount = taskCount;
        this.updatedAt = updatedAt;
    }

    /**
     * WorkloadStatusエンティティからDTOを作成
     * @param workloadStatus 負荷状況エンティティ
     * @return WorkloadStatusResponseDTO
     */
    public static WorkloadStatusResponseDTO fromEntity(WorkloadStatus workloadStatus) {
        if (workloadStatus == null) {
            return null;
        }

        WorkloadStatusResponseDTO dto = new WorkloadStatusResponseDTO();
        dto.setId(workloadStatus.getId());
        dto.setUserId(workloadStatus.getUser().getId());
        dto.setUsername(workloadStatus.getUser().getUsername());
        dto.setDisplayName(workloadStatus.getUser().getDisplayName());
        dto.setDepartment(workloadStatus.getUser().getDepartment());
        dto.setWorkloadLevel(workloadStatus.getWorkloadLevel());
        dto.setWorkloadLevelDisplay(workloadStatus.getWorkloadLevel().getDisplayName());
        dto.setProjectCount(workloadStatus.getProjectCount());
        dto.setTaskCount(workloadStatus.getTaskCount());
        dto.setUpdatedAt(workloadStatus.getUpdatedAt());

        return dto;
    }

    /**
     * DTOからWorkloadStatusエンティティを作成（更新用）
     * 注意: Userエンティティは別途設定する必要があります
     * @return WorkloadStatus
     */
    public WorkloadStatus toEntity() {
        WorkloadStatus workloadStatus = new WorkloadStatus();
        workloadStatus.setId(this.id);
        workloadStatus.setWorkloadLevel(this.workloadLevel);
        workloadStatus.setProjectCount(this.projectCount);
        workloadStatus.setTaskCount(this.taskCount);
        workloadStatus.setUpdatedAt(this.updatedAt);
        return workloadStatus;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public WorkloadLevel getWorkloadLevel() {
        return workloadLevel;
    }

    public void setWorkloadLevel(WorkloadLevel workloadLevel) {
        this.workloadLevel = workloadLevel;
        this.workloadLevelDisplay = workloadLevel != null ? workloadLevel.getDisplayName() : null;
    }

    public String getWorkloadLevelDisplay() {
        return workloadLevelDisplay;
    }

    public void setWorkloadLevelDisplay(String workloadLevelDisplay) {
        this.workloadLevelDisplay = workloadLevelDisplay;
    }

    public Integer getProjectCount() {
        return projectCount;
    }

    public void setProjectCount(Integer projectCount) {
        this.projectCount = projectCount;
    }

    public Integer getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(Integer taskCount) {
        this.taskCount = taskCount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "WorkloadStatusResponseDTO{" +
                "id=" + id +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", displayName='" + displayName + '\'' +
                ", department='" + department + '\'' +
                ", workloadLevel=" + workloadLevel +
                ", workloadLevelDisplay='" + workloadLevelDisplay + '\'' +
                ", projectCount=" + projectCount +
                ", taskCount=" + taskCount +
                ", updatedAt=" + updatedAt +
                '}';
    }
}