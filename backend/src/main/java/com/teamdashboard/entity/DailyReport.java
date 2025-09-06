package com.teamdashboard.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_reports")
public class DailyReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String workContent;

    @Column(columnDefinition = "TEXT")
    private String insights;

    @Column(columnDefinition = "TEXT")
    private String issues;

    @Min(1) @Max(5)
    @Column(name = "workload_level")
    private Integer workloadLevel; // 1:軽い 2:普通 3:やや重い 4:重い 5:非常に重い

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (reportDate == null) {
            reportDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public DailyReport() {}

    public DailyReport(User user, String workContent, Integer workloadLevel) {
        this.user = user;
        this.workContent = workContent;
        this.workloadLevel = workloadLevel;
        this.reportDate = LocalDate.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }

    public String getWorkContent() { return workContent; }
    public void setWorkContent(String workContent) { this.workContent = workContent; }

    public String getInsights() { return insights; }
    public void setInsights(String insights) { this.insights = insights; }

    public String getIssues() { return issues; }
    public void setIssues(String issues) { this.issues = issues; }

    public Integer getWorkloadLevel() { return workloadLevel; }
    public void setWorkloadLevel(Integer workloadLevel) { this.workloadLevel = workloadLevel; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}