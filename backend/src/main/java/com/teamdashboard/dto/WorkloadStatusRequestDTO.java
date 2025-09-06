package com.teamdashboard.dto;

import com.teamdashboard.entity.WorkloadLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 負荷状況更新リクエスト用DTO
 */
public class WorkloadStatusRequestDTO {

    @NotNull(message = "負荷レベルは必須です")
    private WorkloadLevel workloadLevel;

    @Min(value = 0, message = "案件数は0以上である必要があります")
    private Integer projectCount;

    @Min(value = 0, message = "タスク数は0以上である必要があります")
    private Integer taskCount;

    // Constructors
    public WorkloadStatusRequestDTO() {}

    public WorkloadStatusRequestDTO(WorkloadLevel workloadLevel, Integer projectCount, Integer taskCount) {
        this.workloadLevel = workloadLevel;
        this.projectCount = projectCount;
        this.taskCount = taskCount;
    }

    // Getters and Setters
    public WorkloadLevel getWorkloadLevel() {
        return workloadLevel;
    }

    public void setWorkloadLevel(WorkloadLevel workloadLevel) {
        this.workloadLevel = workloadLevel;
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

    @Override
    public String toString() {
        return "WorkloadStatusRequestDTO{" +
                "workloadLevel=" + workloadLevel +
                ", projectCount=" + projectCount +
                ", taskCount=" + taskCount +
                '}';
    }
}