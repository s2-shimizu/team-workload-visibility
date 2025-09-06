package com.teamdashboard.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public class DailyReportRequest {
    @NotBlank(message = "作業内容は必須です")
    private String workContent;
    
    private String insights;
    private String issues;
    
    @Min(value = 1, message = "負荷レベルは1-5の範囲で入力してください")
    @Max(value = 5, message = "負荷レベルは1-5の範囲で入力してください")
    private Integer workloadLevel;
    
    private LocalDate reportDate;

    // Constructors
    public DailyReportRequest() {}

    public DailyReportRequest(String workContent, String insights, String issues, Integer workloadLevel) {
        this.workContent = workContent;
        this.insights = insights;
        this.issues = issues;
        this.workloadLevel = workloadLevel;
    }

    // Getters and Setters
    public String getWorkContent() { return workContent; }
    public void setWorkContent(String workContent) { this.workContent = workContent; }

    public String getInsights() { return insights; }
    public void setInsights(String insights) { this.insights = insights; }

    public String getIssues() { return issues; }
    public void setIssues(String issues) { this.issues = issues; }

    public Integer getWorkloadLevel() { return workloadLevel; }
    public void setWorkloadLevel(Integer workloadLevel) { this.workloadLevel = workloadLevel; }

    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
}