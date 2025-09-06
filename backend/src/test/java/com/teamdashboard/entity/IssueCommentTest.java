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
 * IssueCommentエンティティの単体テスト
 */
@DisplayName("IssueComment エンティティテスト")
class IssueCommentTest {

    private Validator validator;
    private User testUser;
    private TeamIssue testIssue;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        testUser = new User("testuser", "password", "test@example.com", "テストユーザー");
        testUser.setId(1L);
        
        testIssue = new TeamIssue(testUser, "テスト問題");
        testIssue.setId(1L);
    }

    @Test
    @DisplayName("正常なIssueCommentの作成")
    void createValidIssueComment() {
        // Given
        String content = "この問題は設定ファイルの確認が必要です";
        IssueComment comment = new IssueComment(testIssue, testUser, content);
        
        // When
        Set<ConstraintViolation<IssueComment>> violations = validator.validate(comment);
        
        // Then
        assertTrue(violations.isEmpty());
        assertEquals(testIssue, comment.getIssue());
        assertEquals(testUser, comment.getUser());
        assertEquals(content, comment.getContent());
        assertNotNull(comment.getCreatedAt());
    }

    @Test
    @DisplayName("空のコンテンツでバリデーションエラー")
    void invalidEmptyContent() {
        // Given
        IssueComment comment = new IssueComment(testIssue, testUser, "");
        
        // When
        Set<ConstraintViolation<IssueComment>> violations = validator.validate(comment);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("content")));
    }

    @Test
    @DisplayName("nullのコンテンツでバリデーションエラー")
    void invalidNullContent() {
        // Given
        IssueComment comment = new IssueComment();
        comment.setIssue(testIssue);
        comment.setUser(testUser);
        comment.setContent(null);
        
        // When
        Set<ConstraintViolation<IssueComment>> violations = validator.validate(comment);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("content")));
    }

    @Test
    @DisplayName("500文字を超えるコンテンツでバリデーションエラー")
    void invalidTooLongContent() {
        // Given
        String longContent = "a".repeat(501); // 501文字
        IssueComment comment = new IssueComment(testIssue, testUser, longContent);
        
        // When
        Set<ConstraintViolation<IssueComment>> violations = validator.validate(comment);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("content")));
    }

    @Test
    @DisplayName("500文字ちょうどのコンテンツは有効")
    void validMaxLengthContent() {
        // Given
        String maxContent = "a".repeat(500); // 500文字
        IssueComment comment = new IssueComment(testIssue, testUser, maxContent);
        
        // When
        Set<ConstraintViolation<IssueComment>> violations = validator.validate(comment);
        
        // Then
        assertTrue(violations.isEmpty());
        assertEquals(maxContent, comment.getContent());
    }

    @Test
    @DisplayName("@PrePersistで作成日時が設定される")
    void prePersistSetsCreatedAt() {
        // Given
        IssueComment comment = new IssueComment();
        comment.setIssue(testIssue);
        comment.setUser(testUser);
        comment.setContent("テストコメント");
        LocalDateTime beforeCreate = LocalDateTime.now();
        
        // When
        comment.onCreate(); // @PrePersistメソッドを直接呼び出し
        
        // Then
        assertNotNull(comment.getCreatedAt());
        assertTrue(comment.getCreatedAt().isAfter(beforeCreate) || 
                  comment.getCreatedAt().isEqual(beforeCreate));
    }

    @Test
    @DisplayName("デフォルトコンストラクタでの作成")
    void defaultConstructor() {
        // Given & When
        IssueComment comment = new IssueComment();
        
        // Then
        assertNull(comment.getId());
        assertNull(comment.getIssue());
        assertNull(comment.getUser());
        assertNull(comment.getContent());
        assertNull(comment.getCreatedAt());
    }

    @Test
    @DisplayName("セッターとゲッターの動作確認")
    void settersAndGetters() {
        // Given
        IssueComment comment = new IssueComment();
        String content = "セッターテスト";
        LocalDateTime createdAt = LocalDateTime.now();
        
        // When
        comment.setId(1L);
        comment.setIssue(testIssue);
        comment.setUser(testUser);
        comment.setContent(content);
        comment.setCreatedAt(createdAt);
        
        // Then
        assertEquals(1L, comment.getId());
        assertEquals(testIssue, comment.getIssue());
        assertEquals(testUser, comment.getUser());
        assertEquals(content, comment.getContent());
        assertEquals(createdAt, comment.getCreatedAt());
    }

    @Test
    @DisplayName("日本語コンテンツの処理")
    void japaneseContent() {
        // Given
        String japaneseContent = "この問題は設定ファイルの文字エンコーディングが原因だと思います。UTF-8に変更してみてください。";
        IssueComment comment = new IssueComment(testIssue, testUser, japaneseContent);
        
        // When
        Set<ConstraintViolation<IssueComment>> violations = validator.validate(comment);
        
        // Then
        assertTrue(violations.isEmpty());
        assertEquals(japaneseContent, comment.getContent());
    }

    @Test
    @DisplayName("特殊文字を含むコンテンツの処理")
    void specialCharactersContent() {
        // Given
        String specialContent = "エラーコード: HTTP 500 - Internal Server Error\n詳細: java.lang.NullPointerException at line 42";
        IssueComment comment = new IssueComment(testIssue, testUser, specialContent);
        
        // When
        Set<ConstraintViolation<IssueComment>> violations = validator.validate(comment);
        
        // Then
        assertTrue(violations.isEmpty());
        assertEquals(specialContent, comment.getContent());
    }
}