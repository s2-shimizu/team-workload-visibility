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
 * WorkloadStatusエンティティの単体テスト
 */
@DisplayName("WorkloadStatus エンティティテスト")
class WorkloadStatusTest {

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
    @DisplayName("正常なWorkloadStatusの作成")
    void createValidWorkloadStatus() {
        // Given
        WorkloadStatus workloadStatus = new WorkloadStatus(testUser, WorkloadLevel.HIGH);
        
        // When
        Set<ConstraintViolation<WorkloadStatus>> violations = validator.validate(workloadStatus);
        
        // Then
        assertTrue(violations.isEmpty());
        assertEquals(testUser, workloadStatus.getUser());
        assertEquals(WorkloadLevel.HIGH, workloadStatus.getWorkloadLevel());
        assertNotNull(workloadStatus.getUpdatedAt());
    }

    @Test
    @DisplayName("プロジェクト数とタスク数を含むWorkloadStatusの作成")
    void createWorkloadStatusWithCounts() {
        // Given
        WorkloadStatus workloadStatus = new WorkloadStatus(testUser, WorkloadLevel.MEDIUM, 3, 5);
        
        // When
        Set<ConstraintViolation<WorkloadStatus>> violations = validator.validate(workloadStatus);
        
        // Then
        assertTrue(violations.isEmpty());
        assertEquals(testUser, workloadStatus.getUser());
        assertEquals(WorkloadLevel.MEDIUM, workloadStatus.getWorkloadLevel());
        assertEquals(3, workloadStatus.getProjectCount());
        assertEquals(5, workloadStatus.getTaskCount());
        assertNotNull(workloadStatus.getUpdatedAt());
    }

    @Test
    @DisplayName("負のプロジェクト数でバリデーションエラー")
    void invalidNegativeProjectCount() {
        // Given
        WorkloadStatus workloadStatus = new WorkloadStatus(testUser, WorkloadLevel.LOW);
        workloadStatus.setProjectCount(-1);
        
        // When
        Set<ConstraintViolation<WorkloadStatus>> violations = validator.validate(workloadStatus);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("projectCount")));
    }

    @Test
    @DisplayName("負のタスク数でバリデーションエラー")
    void invalidNegativeTaskCount() {
        // Given
        WorkloadStatus workloadStatus = new WorkloadStatus(testUser, WorkloadLevel.LOW);
        workloadStatus.setTaskCount(-1);
        
        // When
        Set<ConstraintViolation<WorkloadStatus>> violations = validator.validate(workloadStatus);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("taskCount")));
    }

    @Test
    @DisplayName("@PrePersistで更新日時が設定される")
    void prePersistSetsUpdatedAt() {
        // Given
        WorkloadStatus workloadStatus = new WorkloadStatus();
        workloadStatus.setUser(testUser);
        workloadStatus.setWorkloadLevel(WorkloadLevel.HIGH);
        LocalDateTime beforeUpdate = LocalDateTime.now();
        
        // When
        workloadStatus.onUpdate(); // @PrePersist/@PreUpdateメソッドを直接呼び出し
        
        // Then
        assertNotNull(workloadStatus.getUpdatedAt());
        assertTrue(workloadStatus.getUpdatedAt().isAfter(beforeUpdate) || 
                  workloadStatus.getUpdatedAt().isEqual(beforeUpdate));
    }

    @Test
    @DisplayName("@PreUpdateで更新日時が更新される")
    void preUpdateUpdatesUpdatedAt() {
        // Given
        WorkloadStatus workloadStatus = new WorkloadStatus(testUser, WorkloadLevel.LOW);
        LocalDateTime initialTime = workloadStatus.getUpdatedAt();
        
        // 少し時間を置く
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // When
        workloadStatus.onUpdate(); // @PreUpdateメソッドを直接呼び出し
        
        // Then
        assertNotNull(workloadStatus.getUpdatedAt());
        assertTrue(workloadStatus.getUpdatedAt().isAfter(initialTime));
    }

    @Test
    @DisplayName("WorkloadLevelの設定と取得")
    void workloadLevelSetterGetter() {
        // Given
        WorkloadStatus workloadStatus = new WorkloadStatus();
        
        // When & Then
        workloadStatus.setWorkloadLevel(WorkloadLevel.HIGH);
        assertEquals(WorkloadLevel.HIGH, workloadStatus.getWorkloadLevel());
        
        workloadStatus.setWorkloadLevel(WorkloadLevel.MEDIUM);
        assertEquals(WorkloadLevel.MEDIUM, workloadStatus.getWorkloadLevel());
        
        workloadStatus.setWorkloadLevel(WorkloadLevel.LOW);
        assertEquals(WorkloadLevel.LOW, workloadStatus.getWorkloadLevel());
    }

    @Test
    @DisplayName("プロジェクト数とタスク数のnull値許可")
    void allowNullCounts() {
        // Given
        WorkloadStatus workloadStatus = new WorkloadStatus(testUser, WorkloadLevel.MEDIUM);
        workloadStatus.setProjectCount(null);
        workloadStatus.setTaskCount(null);
        
        // When
        Set<ConstraintViolation<WorkloadStatus>> violations = validator.validate(workloadStatus);
        
        // Then
        assertTrue(violations.isEmpty());
        assertNull(workloadStatus.getProjectCount());
        assertNull(workloadStatus.getTaskCount());
    }

    @Test
    @DisplayName("ゼロのプロジェクト数とタスク数は有効")
    void allowZeroCounts() {
        // Given
        WorkloadStatus workloadStatus = new WorkloadStatus(testUser, WorkloadLevel.LOW);
        workloadStatus.setProjectCount(0);
        workloadStatus.setTaskCount(0);
        
        // When
        Set<ConstraintViolation<WorkloadStatus>> violations = validator.validate(workloadStatus);
        
        // Then
        assertTrue(violations.isEmpty());
        assertEquals(0, workloadStatus.getProjectCount());
        assertEquals(0, workloadStatus.getTaskCount());
    }
}