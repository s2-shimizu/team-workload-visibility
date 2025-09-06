package com.teamdashboard.entity;

/**
 * 負荷レベルを表す列挙型
 */
public enum WorkloadLevel {
    LOW("低"),
    MEDIUM("中"),
    HIGH("高");
    
    private final String displayName;
    
    WorkloadLevel(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}