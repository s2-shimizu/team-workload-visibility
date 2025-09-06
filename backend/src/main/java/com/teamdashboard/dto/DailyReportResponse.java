package com.teamdashboard.dto;

import com.teamdashboard.entity.DailyReport;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DailyReportResponse {
    private Long id;
    private String username;
    private String displayName;
    private LocalDate reportDate;
    private String workContent;
    private String insights;
    private String issues;
    private Integer workloadLevel;
    private String workloadLevelText;
    private LocalDateTime createdAt;

    // Constructors
    public DailyReportResponse() {}

    public DailyReportResponse(DailyReport report) {
        this.id = report.getId();
        this.username = report.getUser().getUsername();
        this.displayName = report.getUser().getDisplayName();
        this.reportDate = report.getReportDate();
        this.workContent = report.getWorkContent();
        this.insights = report.getInsights();
        this.issues = report.getIssues();
        this.workloadLevel = report.getWorkloadLevel();
        this.workloadLevelText = getWorkloadLevelText(report.getWorkloadLevel());
        this.createdAt = report.getCreatedAt();
    }

    private String getWorkloadLevelText(Integer level) {
        if (level == null) return "未設定";
        return switch (level) {
            case 1 -> "軽い";
            case 2 -> "普通";
            case 3 -> "やや重い";
            case 4 -> "重い";
            case 5 -> "非常に重い";
            default -> "未設定";
        };
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

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

    public String getWorkloadLevelText() { return workloadLevelText; }
    public void setWorkloadLevelText(String workloadLevelText) { this.workloadLevelText = workloadLevelText; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}