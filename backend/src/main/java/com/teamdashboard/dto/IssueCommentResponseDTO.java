package com.teamdashboard.dto;

import com.teamdashboard.entity.IssueComment;
import java.time.LocalDateTime;

/**
 * コメントレスポンス用DTO
 */
public class IssueCommentResponseDTO {
    
    private Long id;
    private Long issueId;
    private Long userId;
    private String username;
    private String displayName;
    private String content;
    private LocalDateTime createdAt;

    // Constructors
    public IssueCommentResponseDTO() {}

    public IssueCommentResponseDTO(Long id, Long issueId, Long userId, String username, 
                                  String displayName, String content, LocalDateTime createdAt) {
        this.id = id;
        this.issueId = issueId;
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.content = content;
        this.createdAt = createdAt;
    }

    /**
     * IssueCommentエンティティからDTOを作成
     * @param comment IssueCommentエンティティ
     * @return IssueCommentResponseDTO
     */
    public static IssueCommentResponseDTO fromEntity(IssueComment comment) {
        return new IssueCommentResponseDTO(
            comment.getId(),
            comment.getIssue().getId(),
            comment.getUser().getId(),
            comment.getUser().getUsername(),
            comment.getUser().getDisplayName(),
            comment.getContent(),
            comment.getCreatedAt()
        );
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIssueId() {
        return issueId;
    }

    public void setIssueId(Long issueId) {
        this.issueId = issueId;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}