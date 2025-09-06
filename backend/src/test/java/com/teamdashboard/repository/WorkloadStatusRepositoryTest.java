package com.teamdashboard.repository;

import com.teamdashboard.entity.User;
import com.teamdashboard.entity.WorkloadStatus;
import com.teamdashboard.entity.WorkloadLevel;
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
 * WorkloadStatusRepositoryの単体テスト
 */
@DataJpaTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("WorkloadStatusRepository テスト")
class WorkloadStatusRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private WorkloadStatusRepository workloadStatusRepository;

    private User testUser1;
    private User testUser2;
    private WorkloadStatus workloadStatus1;
    private WorkloadStatus workloadStatus2;

    @BeforeEach
    void setUp() {
        // テストユーザーの作成
        testUser1 = new User("user1", "password1", "user1@example.com", "ユーザー1");
        testUser2 = new User("user2", "password2", "user2@example.com", "ユーザー2");
        
        entityManager.persistAndFlush(testUser1);
        entityManager.persistAndFlush(testUser2);

        // テスト用WorkloadStatusの作成
        workloadStatus1 = new WorkloadStatus(testUser1, WorkloadLevel.HIGH, 3, 5);
        workloadStatus2 = new WorkloadStatus(testUser2, WorkloadLevel.LOW, 1, 2);
        
        entityManager.persistAndFlush(workloadStatus1);
        entityManager.persistAndFlush(workloadStatus2);
        
        entityManager.clear();
    }

    @Test
    @DisplayName("ユーザーによる負荷状況の検索")
    void findByUser() {
        // When
        Optional<WorkloadStatus> found = workloadStatusRepository.findByUser(testUser1);
        
        // Then
        assertTrue(found.isPresent());
        assertEquals(testUser1.getId(), found.get().getUser().getId());
        assertEquals(WorkloadLevel.HIGH, found.get().getWorkloadLevel());
        assertEquals(3, found.get().getProjectCount());
        assertEquals(5, found.get().getTaskCount());
    }

    @Test
    @DisplayName("存在しないユーザーによる負荷状況の検索")
    void findByUserNotFound() {
        // Given
        User nonExistentUser = new User("nonexistent", "password", "none@example.com", "存在しないユーザー");
        entityManager.persistAndFlush(nonExistentUser);
        
        // When
        Optional<WorkloadStatus> found = workloadStatusRepository.findByUser(nonExistentUser);
        
        // Then
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("ユーザーIDによる負荷状況の検索")
    void findByUserId() {
        // When
        Optional<WorkloadStatus> found = workloadStatusRepository.findByUserId(testUser1.getId());
        
        // Then
        assertTrue(found.isPresent());
        assertEquals(testUser1.getId(), found.get().getUser().getId());
        assertEquals(WorkloadLevel.HIGH, found.get().getWorkloadLevel());
    }

    @Test
    @DisplayName("存在しないユーザーIDによる負荷状況の検索")
    void findByUserIdNotFound() {
        // When
        Optional<WorkloadStatus> found = workloadStatusRepository.findByUserId(999L);
        
        // Then
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("全負荷状況を更新日時降順で取得")
    void findAllWithUserOrderByUpdatedAtDesc() {
        // Given - 更新日時を明確に異なるものにする
        workloadStatus1.setUpdatedAt(LocalDateTime.now().minusHours(1));
        workloadStatus2.setUpdatedAt(LocalDateTime.now());
        entityManager.merge(workloadStatus1);
        entityManager.merge(workloadStatus2);
        entityManager.flush();
        
        // When
        List<WorkloadStatus> results = workloadStatusRepository.findAllWithUserOrderByUpdatedAtDesc();
        
        // Then
        assertEquals(2, results.size());
        // 最新のものが最初に来る
        assertEquals(testUser2.getId(), results.get(0).getUser().getId());
        assertEquals(testUser1.getId(), results.get(1).getUser().getId());
        
        // ユーザー情報がフェッチされていることを確認
        assertNotNull(results.get(0).getUser().getDisplayName());
        assertNotNull(results.get(1).getUser().getDisplayName());
    }

    @Test
    @DisplayName("負荷レベル別の負荷状況検索")
    void findByWorkloadLevelWithUser() {
        // Given - 同じ負荷レベルのデータを追加
        User testUser3 = new User("user3", "password3", "user3@example.com", "ユーザー3");
        entityManager.persistAndFlush(testUser3);
        
        WorkloadStatus workloadStatus3 = new WorkloadStatus(testUser3, WorkloadLevel.HIGH, 2, 4);
        entityManager.persistAndFlush(workloadStatus3);
        
        // When
        List<WorkloadStatus> highWorkloadResults = workloadStatusRepository.findByWorkloadLevelWithUser(WorkloadLevel.HIGH);
        List<WorkloadStatus> lowWorkloadResults = workloadStatusRepository.findByWorkloadLevelWithUser(WorkloadLevel.LOW);
        List<WorkloadStatus> mediumWorkloadResults = workloadStatusRepository.findByWorkloadLevelWithUser(WorkloadLevel.MEDIUM);
        
        // Then
        assertEquals(2, highWorkloadResults.size());
        assertEquals(1, lowWorkloadResults.size());
        assertEquals(0, mediumWorkloadResults.size());
        
        // HIGH負荷のユーザーを確認
        assertTrue(highWorkloadResults.stream()
            .anyMatch(ws -> ws.getUser().getId().equals(testUser1.getId())));
        assertTrue(highWorkloadResults.stream()
            .anyMatch(ws -> ws.getUser().getId().equals(testUser3.getId())));
        
        // LOW負荷のユーザーを確認
        assertEquals(testUser2.getId(), lowWorkloadResults.get(0).getUser().getId());
        
        // ユーザー情報がフェッチされていることを確認
        assertNotNull(highWorkloadResults.get(0).getUser().getDisplayName());
    }

    @Test
    @DisplayName("基本的なCRUD操作")
    void basicCrudOperations() {
        // Create
        User newUser = new User("newuser", "password", "new@example.com", "新規ユーザー");
        entityManager.persistAndFlush(newUser);
        
        WorkloadStatus newWorkloadStatus = new WorkloadStatus(newUser, WorkloadLevel.MEDIUM, 2, 3);
        WorkloadStatus saved = workloadStatusRepository.save(newWorkloadStatus);
        
        assertNotNull(saved.getId());
        assertEquals(WorkloadLevel.MEDIUM, saved.getWorkloadLevel());
        
        // Read
        Optional<WorkloadStatus> found = workloadStatusRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(newUser.getId(), found.get().getUser().getId());
        
        // Update
        found.get().setWorkloadLevel(WorkloadLevel.HIGH);
        found.get().setProjectCount(4);
        WorkloadStatus updated = workloadStatusRepository.save(found.get());
        
        assertEquals(WorkloadLevel.HIGH, updated.getWorkloadLevel());
        assertEquals(4, updated.getProjectCount());
        
        // Delete
        workloadStatusRepository.delete(updated);
        Optional<WorkloadStatus> deleted = workloadStatusRepository.findById(saved.getId());
        assertFalse(deleted.isPresent());
    }

    @Test
    @DisplayName("全件取得")
    void findAll() {
        // When
        List<WorkloadStatus> all = workloadStatusRepository.findAll();
        
        // Then
        assertEquals(2, all.size());
    }

    @Test
    @DisplayName("件数取得")
    void count() {
        // When
        long count = workloadStatusRepository.count();
        
        // Then
        assertEquals(2, count);
    }

    @Test
    @DisplayName("存在確認")
    void existsById() {
        // When & Then
        assertTrue(workloadStatusRepository.existsById(workloadStatus1.getId()));
        assertFalse(workloadStatusRepository.existsById(999L));
    }

    @Test
    @DisplayName("複数件削除")
    void deleteAll() {
        // Given
        assertEquals(2, workloadStatusRepository.count());
        
        // When
        workloadStatusRepository.deleteAll();
        
        // Then
        assertEquals(0, workloadStatusRepository.count());
    }

    @Test
    @DisplayName("null値を含む負荷状況の保存と取得")
    void saveAndFindWithNullValues() {
        // Given
        User newUser = new User("nulluser", "password", "null@example.com", "Nullユーザー");
        entityManager.persistAndFlush(newUser);
        
        WorkloadStatus workloadStatusWithNulls = new WorkloadStatus(newUser, WorkloadLevel.MEDIUM);
        // projectCountとtaskCountはnull
        
        // When
        WorkloadStatus saved = workloadStatusRepository.save(workloadStatusWithNulls);
        Optional<WorkloadStatus> found = workloadStatusRepository.findById(saved.getId());
        
        // Then
        assertTrue(found.isPresent());
        assertEquals(WorkloadLevel.MEDIUM, found.get().getWorkloadLevel());
        assertNull(found.get().getProjectCount());
        assertNull(found.get().getTaskCount());
    }
}