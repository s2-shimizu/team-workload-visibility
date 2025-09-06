package com.teamdashboard.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * チームの困りごとを管理するエンティティ
 */
@Entity
@Table(name = "team_issues")
public class TeamIssue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Size(max = 1000, message = "困りごとの内容は1000文字以内で入力してください")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private IssueStatus status = IssueStatus.OPEN;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<IssueComment> comments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public TeamIssue() {}

    public TeamIssue(User user, String content) {
        this.user = user;
        this.content = content;
        this.status = IssueStatus.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    // Business methods
    public void resolve() {
        this.status = IssueStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
    }

    public void reopen() {
        this.status = IssueStatus.OPEN;
        this.resolvedAt = null;
    }

    public boolean isResolved() {
        return this.status == IssueStatus.RESOLVED;
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

    public List<IssueComment> getComments() {
        return comments;
    }

    public void setComments(List<IssueComment> comments) {
        this.comments = comments;
    }
}