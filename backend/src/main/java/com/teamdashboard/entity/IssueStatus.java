package com.teamdashboard.entity;

/**
 * 困りごとのステータスを表す列挙型
 */
public enum IssueStatus {
    OPEN("未解決"),
    RESOLVED("解決済み");
    
    private final String displayName;
    
    IssueStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}