package com.teamdashboard.repository;

import com.teamdashboard.entity.User;
import com.teamdashboard.entity.TeamIssue;
import com.teamdashboard.entity.IssueStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TeamIssueRepositoryの単体テスト
 */
@DataJpaTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("TeamIssueRepository テスト")
class TeamIssueRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TeamIssueRepository teamIssueRepository;

    private User testUser1;
    private User testUser2;
    private TeamIssue openIssue1;
    private TeamIssue openIssue2;
    private TeamIssue resolvedIssue;

    @BeforeEach
    void setUp() {
        // テストユーザーの作成
        testUser1 = new User("user1", "password1", "user1@example.com", "ユーザー1");
        testUser2 = new User("user2", "password2", "user2@example.com", "ユーザー2");
        
        entityManager.persistAndFlush(testUser1);
        entityManager.persistAndFlush(testUser2);

        // テスト用TeamIssueの作成
        openIssue1 = new TeamIssue(testUser1, "データベース接続エラーが発生しています");
        openIssue1.setCreatedAt(LocalDateTime.now().minusHours(2));
        
        openIssue2 = new TeamIssue(testUser2, "APIのレスポンスが遅いです");
        openIssue2.setCreatedAt(LocalDateTime.now().minusHours(1));
        
        resolvedIssue = new TeamIssue(testUser1, "ログイン機能のバグ");
        resolvedIssue.setCreatedAt(LocalDateTime.now().minusHours(3));
        resolvedIssue.resolve();
        
        entityManager.persistAndFlush(openIssue1);
        entityManager.persistAndFlush(openIssue2);
        entityManager.persistAndFlush(resolvedIssue);
        
        entityManager.clear();
    }

    @Test
    @DisplayName("全困りごとを作成日時降順で取得（ユーザー情報含む）")
    void findAllWithUserOrderByCreatedAtDesc() {
        // When
        List<TeamIssue> results = teamIssueRepository.findAllWithUserOrderByCreatedAtDesc();
        
        // Then
        assertEquals(3, results.size());
        
        // 作成日時の降順で並んでいることを確認
        assertEquals(openIssue2.getId(), results.get(0).getId()); // 最新
        assertEquals(openIssue1.getId(), results.get(1).getId());
        assertEquals(resolvedIssue.getId(), results.get(2).getId()); // 最古
        
        // ユーザー情報がフェッチされていることを確認
        assertNotNull(results.get(0).getUser().getDisplayName());
        assertNotNull(results.get(1).getUser().getDisplayName());
        assertNotNull(results.get(2).getUser().getDisplayName());
    }

    @Test
    @DisplayName("ステータス別困りごと取得（ユーザー情報含む）")
    void findByStatusWithUserOrderByCreatedAtDesc() {
        // When
        List<TeamIssue> openIssues = teamIssueRepository.findByStatusWithUserOrderByCreatedAtDesc(IssueStatus.OPEN);
        List<TeamIssue> resolvedIssues = teamIssueRepository.findByStatusWithUserOrderByCreatedAtDesc(IssueStatus.RESOLVED);
        
        // Then
        assertEquals(2, openIssues.size());
        assertEquals(1, resolvedIssues.size());
        
        // OPEN状態の困りごとを確認
        assertEquals(openIssue2.getId(), openIssues.get(0).getId()); // 最新
        assertEquals(openIssue1.getId(), openIssues.get(1).getId());
        
        // RESOLVED状態の困りごとを確認
        assertEquals(resolvedIssue.getId(), resolvedIssues.get(0).getId());
        
        // ユーザー情報がフェッチされていることを確認
        assertNotNull(openIssues.get(0).getUser().getDisplayName());
        assertNotNull(resolvedIssues.get(0).getUser().getDisplayName());
    }

    @Test
    @DisplayName("ユーザー別困りごと取得")
    void findByUserOrderByCreatedAtDesc() {
        // When
        List<TeamIssue> user1Issues = teamIssueRepository.findByUserOrderByCreatedAtDesc(testUser1);
        List<TeamIssue> user2Issues = teamIssueRepository.findByUserOrderByCreatedAtDesc(testUser2);
        
        // Then
        assertEquals(2, user1Issues.size()); // openIssue1, resolvedIssue
        assertEquals(1, user2Issues.size()); // openIssue2
        
        // user1の困りごとが作成日時降順で並んでいることを確認
        assertEquals(openIssue1.getId(), user1Issues.get(0).getId()); // より新しい
        assertEquals(resolvedIssue.getId(), user1Issues.get(1).getId()); // より古い
        
        // user2の困りごとを確認
        assertEquals(openIssue2.getId(), user2Issues.get(0).getId());
    }

    @Test
    @DisplayName("ユーザーID別困りごと取得（ユーザー情報含む）")
    void findByUserIdWithUserOrderByCreatedAtDesc() {
        // When
        List<TeamIssue> user1Issues = teamIssueRepository.findByUserIdWithUserOrderByCreatedAtDesc(testUser1.getId());
        List<TeamIssue> user2Issues = teamIssueRepository.findByUserIdWithUserOrderByCreatedAtDesc(testUser2.getId());
        List<TeamIssue> nonExistentUserIssues = teamIssueRepository.findByUserIdWithUserOrderByCreatedAtDesc(999L);
        
        // Then
        assertEquals(2, user1Issues.size());
        assertEquals(1, user2Issues.size());
        assertEquals(0, nonExistentUserIssues.size());
        
        // ユーザー情報がフェッチされていることを確認
        assertNotNull(user1Issues.get(0).getUser().getDisplayName());
        assertNotNull(user2Issues.get(0).getUser().getDisplayName());
        
        // 作成日時降順で並んでいることを確認
        assertEquals(openIssue1.getId(), user1Issues.get(0).getId());
        assertEquals(resolvedIssue.getId(), user1Issues.get(1).getId());
    }

    @Test
    @DisplayName("期間指定での困りごと取得（ユーザー情報含む）")
    void findByCreatedAtBetweenWithUserOrderByCreatedAtDesc() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusHours(2).minusMinutes(30);
        LocalDateTime endDate = LocalDateTime.now().minusMinutes(30);
        
        // When
        List<TeamIssue> results = teamIssueRepository.findByCreatedAtBetweenWithUserOrderByCreatedAtDesc(startDate, endDate);
        
        // Then
        assertEquals(2, results.size()); // openIssue1, openIssue2
        
        // 作成日時降順で並んでいることを確認
        assertEquals(openIssue2.getId(), results.get(0).getId());
        assertEquals(openIssue1.getId(), results.get(1).getId());
        
        // ユーザー情報がフェッチされていることを確認
        assertNotNull(results.get(0).getUser().getDisplayName());
        assertNotNull(results.get(1).getUser().getDisplayName());
    }

    @Test
    @DisplayName("ステータス別件数取得")
    void countByStatus() {
        // When
        long openCount = teamIssueRepository.countByStatus(IssueStatus.OPEN);
        long resolvedCount = teamIssueRepository.countByStatus(IssueStatus.RESOLVED);
        
        // Then
        assertEquals(2, openCount);
        assertEquals(1, resolvedCount);
    }

    @Test
    @DisplayName("基本的なCRUD操作")
    void basicCrudOperations() {
        // Create
        TeamIssue newIssue = new TeamIssue(testUser1, "新しい困りごと");
        TeamIssue saved = teamIssueRepository.save(newIssue);
        
        assertNotNull(saved.getId());
        assertEquals("新しい困りごと", saved.getContent());
        assertEquals(IssueStatus.OPEN, saved.getStatus());
        
        // Read
        Optional<TeamIssue> found = teamIssueRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(testUser1.getId(), found.get().getUser().getId());
        
        // Update
        found.get().setContent("更新された困りごと");
        found.get().resolve();
        TeamIssue updated = teamIssueRepository.save(found.get());
        
        assertEquals("更新された困りごと", updated.getContent());
        assertEquals(IssueStatus.RESOLVED, updated.getStatus());
        assertNotNull(updated.getResolvedAt());
        
        // Delete
        teamIssueRepository.delete(updated);
        Optional<TeamIssue> deleted = teamIssueRepository.findById(saved.getId());
        assertFalse(deleted.isPresent());
    }

    @Test
    @DisplayName("全件取得")
    void findAll() {
        // When
        List<TeamIssue> all = teamIssueRepository.findAll();
        
        // Then
        assertEquals(3, all.size());
    }

    @Test
    @DisplayName("件数取得")
    void count() {
        // When
        long count = teamIssueRepository.count();
        
        // Then
        assertEquals(3, count);
    }

    @Test
    @DisplayName("存在確認")
    void existsById() {
        // When & Then
        assertTrue(teamIssueRepository.existsById(openIssue1.getId()));
        assertFalse(teamIssueRepository.existsById(999L));
    }

    @Test
    @DisplayName("複数件削除")
    void deleteAll() {
        // Given
        assertEquals(3, teamIssueRepository.count());
        
        // When
        teamIssueRepository.deleteAll();
        
        // Then
        assertEquals(0, teamIssueRepository.count());
    }

    @Test
    @DisplayName("長いコンテンツの困りごと保存と取得")
    void saveAndFindWithLongContent() {
        // Given
        String longContent = "これは非常に長い困りごとの内容です。".repeat(20); // 約600文字
        TeamIssue longIssue = new TeamIssue(testUser1, longContent);
        
        // When
        TeamIssue saved = teamIssueRepository.save(longIssue);
        Optional<TeamIssue> found = teamIssueRepository.findById(saved.getId());
        
        // Then
        assertTrue(found.isPresent());
        assertEquals(longContent, found.get().getContent());
    }

    @Test
    @DisplayName("困りごとの解決と再オープンのテスト")
    void resolveAndReopenIssue() {
        // Given
        TeamIssue issue = new TeamIssue(testUser1, "テスト困りごと");
        TeamIssue saved = teamIssueRepository.save(issue);
        
        // When - 解決
        saved.resolve();
        TeamIssue resolved = teamIssueRepository.save(saved);
        
        // Then
        assertEquals(IssueStatus.RESOLVED, resolved.getStatus());
        assertNotNull(resolved.getResolvedAt());
        
        // When - 再オープン
        resolved.reopen();
        TeamIssue reopened = teamIssueRepository.save(resolved);
        
        // Then
        assertEquals(IssueStatus.OPEN, reopened.getStatus());
        assertNull(reopened.getResolvedAt());
    }

    @Test
    @DisplayName("空の結果セットのテスト")
    void emptyResultSets() {
        // Given - 全ての困りごとを削除
        teamIssueRepository.deleteAll();
        
        // When & Then
        assertEquals(0, teamIssueRepository.findAllWithUserOrderByCreatedAtDesc().size());
        assertEquals(0, teamIssueRepository.findByStatusWithUserOrderByCreatedAtDesc(IssueStatus.OPEN).size());
        assertEquals(0, teamIssueRepository.findByUserOrderByCreatedAtDesc(testUser1).size());
        assertEquals(0, teamIssueRepository.countByStatus(IssueStatus.OPEN));
    }
}