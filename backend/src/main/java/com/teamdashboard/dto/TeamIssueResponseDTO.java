package com.teamdashboard.dto;

import com.teamdashboard.entity.IssueStatus;
import com.teamdashboard.entity.TeamIssue;
import java.time.LocalDateTime;

/**
 * 困りごとレスポンス用DTO
 */
public class TeamIssueResponseDTO {
    
    private Long id;
    private Long userId;
    private String username;
    private String displayName;
    private String content;
    private IssueStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private long commentCount;

    // Constructors
    public TeamIssueResponseDTO() {}

    public TeamIssueResponseDTO(Long id, Long userId, String username, String displayName, 
                               String content, IssueStatus status, LocalDateTime createdAt, 
                               LocalDateTime resolvedAt, long commentCount) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.content = content;
        this.status = status;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
        this.commentCount = commentCount;
    }

    /**
     * TeamIssueエンティティからDTOを作成
     * @param issue TeamIssueエンティティ
     * @param commentCount コメント数
     * @return TeamIssueResponseDTO
     */
    public static TeamIssueResponseDTO fromEntity(TeamIssue issue, long commentCount) {
        return new TeamIssueResponseDTO(
            issue.getId(),
            issue.getUser().getId(),
            issue.getUser().getUsername(),
            issue.getUser().getDisplayName(),
            issue.getContent(),
            issue.getStatus(),
            issue.getCreatedAt(),
            issue.getResolvedAt(),
            commentCount
        );
    }

    /**
     * TeamIssueエンティティからDTOを作成（コメント数は0）
     * @param issue TeamIssueエンティティ
     * @return TeamIssueResponseDTO
     */
    public static TeamIssueResponseDTO fromEntity(TeamIssue issue) {
        return fromEntity(issue, 0);
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public void setStatus(IssueStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public long getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(long commentCount) {
        this.commentCount = commentCount;
    }
}