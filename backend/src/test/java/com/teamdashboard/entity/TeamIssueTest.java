package com.teamdashboard.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TeamIssueエンティティの単体テスト
 */
@DisplayName("TeamIssue エンティティテスト")
class TeamIssueTest {

    private Validator validator;
    private User testUser;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        testUser = new User("testuser", "password", "test@example.com", "テストユーザー");
        testUser.setId(1L);
    }

    @Test
    @DisplayName("正常なTeamIssueの作成")
    void createValidTeamIssue() {
        // Given
        String content = "データベース接続でエラーが発生しています";
        TeamIssue issue = new TeamIssue(testUser, content);
        
        // When
        Set<ConstraintViolation<TeamIssue>> violations = validator.validate(issue);
        
        // Then
        assertTrue(violations.isEmpty());
        assertEquals(testUser, issue.getUser());
        assertEquals(content, issue.getContent());
        assertEquals(IssueStatus.OPEN, issue.getStatus());
        assertNotNull(issue.getCreatedAt());
        assertNull(issue.getResolvedAt());
        assertFalse(issue.isResolved());
    }

    @Test
    @DisplayName("空のコンテンツでバリデーションエラー")
    void invalidEmptyContent() {
        // Given
        TeamIssue issue = new TeamIssue(testUser, "");
        
        // When
        Set<ConstraintViolation<TeamIssue>> violations = validator.validate(issue);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("content")));
    }

    @Test
    @DisplayName("nullのコンテンツでバリデーションエラー")
    void invalidNullContent() {
        // Given
        TeamIssue issue = new TeamIssue();
        issue.setUser(testUser);
        issue.setContent(null);
        
        // When
        Set<ConstraintViolation<TeamIssue>> violations = validator.validate(issue);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("content")));
    }

    @Test
    @DisplayName("1000文字を超えるコンテンツでバリデーションエラー")
    void invalidTooLongContent() {
        // Given
        String longContent = "a".repeat(1001); // 1001文字
        TeamIssue issue = new TeamIssue(testUser, longContent);
        
        // When
        Set<ConstraintViolation<TeamIssue>> violations = validator.validate(issue);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("content")));
    }

    @Test
    @DisplayName("1000文字ちょうどのコンテンツは有効")
    void validMaxLengthContent() {
        // Given
        String maxContent = "a".repeat(1000); // 1000文字
        TeamIssue issue = new TeamIssue(testUser, maxContent);
        
        // When
        Set<ConstraintViolation<TeamIssue>> violations = validator.validate(issue);
        
        // Then
        assertTrue(violations.isEmpty());
        assertEquals(maxContent, issue.getContent());
    }

    @Test
    @DisplayName("困りごとの解決")
    void resolveIssue() {
        // Given
        TeamIssue issue = new TeamIssue(testUser, "テスト問題");
        LocalDateTime beforeResolve = LocalDateTime.now();
        
        // When
        issue.resolve();
        
        // Then
        assertEquals(IssueStatus.RESOLVED, issue.getStatus());
        assertNotNull(issue.getResolvedAt());
        assertTrue(issue.getResolvedAt().isAfter(beforeResolve) || 
                  issue.getResolvedAt().isEqual(beforeResolve));
        assertTrue(issue.isResolved());
    }

    @Test
    @DisplayName("困りごとの再オープン")
    void reopenIssue() {
        // Given
        TeamIssue issue = new TeamIssue(testUser, "テスト問題");
        issue.resolve(); // 一度解決
        
        // When
        issue.reopen();
        
        // Then
        assertEquals(IssueStatus.OPEN, issue.getStatus());
        assertNull(issue.getResolvedAt());
        assertFalse(issue.isResolved());
    }

    @Test
    @DisplayName("@PrePersistで作成日時が設定される")
    void prePersistSetsCreatedAt() {
        // Given
        TeamIssue issue = new TeamIssue();
        issue.setUser(testUser);
        issue.setContent("テスト問題");
        LocalDateTime beforeCreate = LocalDateTime.now();
        
        // When
        issue.onCreate(); // @PrePersistメソッドを直接呼び出し
        
        // Then
        assertNotNull(issue.getCreatedAt());
        assertTrue(issue.getCreatedAt().isAfter(beforeCreate) || 
                  issue.getCreatedAt().isEqual(beforeCreate));
    }

    @Test
    @DisplayName("デフォルトステータスはOPEN")
    void defaultStatusIsOpen() {
        // Given
        TeamIssue issue = new TeamIssue();
        
        // When & Then
        assertEquals(IssueStatus.OPEN, issue.getStatus());
        assertFalse(issue.isResolved());
    }

    @Test
    @DisplayName("コメントリストの初期化")
    void commentsListInitialization() {
        // Given
        TeamIssue issue = new TeamIssue(testUser, "テスト問題");
        
        // When & Then
        assertNotNull(issue.getComments());
        assertTrue(issue.getComments().isEmpty());
    }

    @Test
    @DisplayName("ステータスの直接設定")
    void directStatusSetting() {
        // Given
        TeamIssue issue = new TeamIssue(testUser, "テスト問題");
        
        // When
        issue.setStatus(IssueStatus.RESOLVED);
        
        // Then
        assertEquals(IssueStatus.RESOLVED, issue.getStatus());
        assertTrue(issue.isResolved());
    }

    @Test
    @DisplayName("解決日時の直接設定")
    void directResolvedAtSetting() {
        // Given
        TeamIssue issue = new TeamIssue(testUser, "テスト問題");
        LocalDateTime resolvedTime = LocalDateTime.now();
        
        // When
        issue.setResolvedAt(resolvedTime);
        
        // Then
        assertEquals(resolvedTime, issue.getResolvedAt());
    }
}