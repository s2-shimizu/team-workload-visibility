package com.teamdashboard.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

/**
 * チームメンバーの負荷状況を管理するエンティティ
 */
@Entity
@Table(name = "workload_status")
public class WorkloadStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "workload_level", nullable = false)
    private WorkloadLevel workloadLevel;

    @Min(0)
    @Column(name = "project_count")
    private Integer projectCount; // 任意入力

    @Min(0)
    @Column(name = "task_count")
    private Integer taskCount; // 任意入力

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public WorkloadStatus() {}

    public WorkloadStatus(User user, WorkloadLevel workloadLevel) {
        this.user = user;
        this.workloadLevel = workloadLevel;
        this.updatedAt = LocalDateTime.now();
    }

    public WorkloadStatus(User user, WorkloadLevel workloadLevel, Integer projectCount, Integer taskCount) {
        this.user = user;
        this.workloadLevel = workloadLevel;
        this.projectCount = projectCount;
        this.taskCount = taskCount;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}