package com.teamdashboard.service;

import com.teamdashboard.entity.User;
import com.teamdashboard.entity.WorkloadLevel;
import com.teamdashboard.entity.WorkloadStatus;
import com.teamdashboard.repository.UserRepository;
import com.teamdashboard.repository.WorkloadStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WorkloadStatusServiceの単体テスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkloadStatusService テスト")
class WorkloadStatusServiceTest {

    @Mock
    private WorkloadStatusRepository workloadStatusRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkloadStatusService workloadStatusService;

    private User testUser1;
    private User testUser2;
    private WorkloadStatus workloadStatus1;
    private WorkloadStatus workloadStatus2;

    @BeforeEach
    void setUp() {
        testUser1 = new User("user1", "password1", "user1@example.com", "ユーザー1");
        testUser1.setId(1L);
        
        testUser2 = new User("user2", "password2", "user2@example.com", "ユーザー2");
        testUser2.setId(2L);

        workloadStatus1 = new WorkloadStatus(testUser1, WorkloadLevel.HIGH, 3, 5);
        workloadStatus1.setId(1L);
        workloadStatus1.setUpdatedAt(LocalDateTime.now().minusHours(1));

        workloadStatus2 = new WorkloadStatus(testUser2, WorkloadLevel.LOW, 1, 2);
        workloadStatus2.setId(2L);
        workloadStatus2.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("全メンバーの負荷状況取得")
    void getAllWorkloadStatuses() {
        // Given
        List<WorkloadStatus> expectedList = Arrays.asList(workloadStatus2, workloadStatus1);
        when(workloadStatusRepository.findAllWithUserOrderByUpdatedAtDesc()).thenReturn(expectedList);

        // When
        List<WorkloadStatus> result = workloadStatusService.getAllWorkloadStatuses();

        // Then
        assertEquals(2, result.size());
        assertEquals(workloadStatus2.getId(), result.get(0).getId()); // 最新が最初
        assertEquals(workloadStatus1.getId(), result.get(1).getId());
        verify(workloadStatusRepository).findAllWithUserOrderByUpdatedAtDesc();
    }

    @Test
    @DisplayName("ユーザーIDによる負荷状況取得 - 存在する場合")
    void getWorkloadStatusByUserId_Found() {
        // Given
        when(workloadStatusRepository.findByUserId(1L)).thenReturn(Optional.of(workloadStatus1));

        // When
        Optional<WorkloadStatus> result = workloadStatusService.getWorkloadStatusByUserId(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(workloadStatus1.getId(), result.get().getId());
        assertEquals(WorkloadLevel.HIGH, result.get().getWorkloadLevel());
        verify(workloadStatusRepository).findByUserId(1L);
    }

    @Test
    @DisplayName("ユーザーIDによる負荷状況取得 - 存在しない場合")
    void getWorkloadStatusByUserId_NotFound() {
        // Given
        when(workloadStatusRepository.findByUserId(999L)).thenReturn(Optional.empty());

        // When
        Optional<WorkloadStatus> result = workloadStatusService.getWorkloadStatusByUserId(999L);

        // Then
        assertFalse(result.isPresent());
        verify(workloadStatusRepository).findByUserId(999L);
    }

    @Test
    @DisplayName("ユーザーIDによる負荷状況取得 - nullの場合")
    void getWorkloadStatusByUserId_NullUserId() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> workloadStatusService.getWorkloadStatusByUserId(null)
        );
        assertEquals("ユーザーIDは必須です", exception.getMessage());
        verify(workloadStatusRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("ユーザー名による負荷状況取得 - 存在する場合")
    void getWorkloadStatusByUsername_Found() {
        // Given
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(testUser1));
        when(workloadStatusRepository.findByUser(testUser1)).thenReturn(Optional.of(workloadStatus1));

        // When
        Optional<WorkloadStatus> result = workloadStatusService.getWorkloadStatusByUsername("user1");

        // Then
        assertTrue(result.isPresent());
        assertEquals(workloadStatus1.getId(), result.get().getId());
        verify(userRepository).findByUsername("user1");
        verify(workloadStatusRepository).findByUser(testUser1);
    }

    @Test
    @DisplayName("ユーザー名による負荷状況取得 - ユーザーが存在しない場合")
    void getWorkloadStatusByUsername_UserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When
        Optional<WorkloadStatus> result = workloadStatusService.getWorkloadStatusByUsername("nonexistent");

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findByUsername("nonexistent");
        verify(workloadStatusRepository, never()).findByUser(any());
    }

    @Test
    @DisplayName("ユーザー名による負荷状況取得 - nullユーザー名")
    void getWorkloadStatusByUsername_NullUsername() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> workloadStatusService.getWorkloadStatusByUsername(null)
        );
        assertEquals("ユーザー名は必須です", exception.getMessage());
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("ユーザー名による負荷状況取得 - 空のユーザー名")
    void getWorkloadStatusByUsername_EmptyUsername() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> workloadStatusService.getWorkloadStatusByUsername("  ")
        );
        assertEquals("ユーザー名は必須です", exception.getMessage());
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("負荷状況更新 - 新規作成")
    void updateWorkloadStatus_NewRecord() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(workloadStatusRepository.findByUser(testUser1)).thenReturn(Optional.empty());
        when(workloadStatusRepository.save(any(WorkloadStatus.class))).thenReturn(workloadStatus1);

        // When
        WorkloadStatus result = workloadStatusService.updateWorkloadStatus(
            1L, WorkloadLevel.HIGH, 3, 5);

        // Then
        assertNotNull(result);
        assertEquals(WorkloadLevel.HIGH, result.getWorkloadLevel());
        assertEquals(3, result.getProjectCount());
        assertEquals(5, result.getTaskCount());
        
        verify(userRepository).findById(1L);
        verify(workloadStatusRepository).findByUser(testUser1);
        verify(workloadStatusRepository).save(any(WorkloadStatus.class));
    }

    @Test
    @DisplayName("負荷状況更新 - 既存レコード更新")
    void updateWorkloadStatus_ExistingRecord() {
        // Given
        WorkloadStatus existingStatus = new WorkloadStatus(testUser1, WorkloadLevel.LOW, 1, 1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(workloadStatusRepository.findByUser(testUser1)).thenReturn(Optional.of(existingStatus));
        when(workloadStatusRepository.save(any(WorkloadStatus.class))).thenReturn(existingStatus);

        // When
        WorkloadStatus result = workloadStatusService.updateWorkloadStatus(
            1L, WorkloadLevel.HIGH, 4, 6);

        // Then
        assertEquals(WorkloadLevel.HIGH, existingStatus.getWorkloadLevel());
        assertEquals(4, existingStatus.getProjectCount());
        assertEquals(6, existingStatus.getTaskCount());
        
        verify(userRepository).findById(1L);
        verify(workloadStatusRepository).findByUser(testUser1);
        verify(workloadStatusRepository).save(existingStatus);
    }

    @Test
    @DisplayName("負荷状況更新 - ユーザーが存在しない場合")
    void updateWorkloadStatus_UserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> workloadStatusService.updateWorkloadStatus(999L, WorkloadLevel.HIGH, 3, 5)
        );
        assertEquals("ユーザーが見つかりません: 999", exception.getMessage());
        
        verify(userRepository).findById(999L);
        verify(workloadStatusRepository, never()).save(any());
    }

    @Test
    @DisplayName("負荷状況更新 - バリデーションエラー（nullユーザーID）")
    void updateWorkloadStatus_NullUserId() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> workloadStatusService.updateWorkloadStatus(null, WorkloadLevel.HIGH, 3, 5)
        );
        assertEquals("ユーザーIDは必須です", exception.getMessage());
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("負荷状況更新 - バリデーションエラー（null負荷レベル）")
    void updateWorkloadStatus_NullWorkloadLevel() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> workloadStatusService.updateWorkloadStatus(1L, null, 3, 5)
        );
        assertEquals("負荷レベルは必須です", exception.getMessage());
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("負荷状況更新 - バリデーションエラー（負の案件数）")
    void updateWorkloadStatus_NegativeProjectCount() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> workloadStatusService.updateWorkloadStatus(1L, WorkloadLevel.HIGH, -1, 5)
        );
        assertEquals("案件数は0以上である必要があります", exception.getMessage());
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("負荷状況更新 - バリデーションエラー（負のタスク数）")
    void updateWorkloadStatus_NegativeTaskCount() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> workloadStatusService.updateWorkloadStatus(1L, WorkloadLevel.HIGH, 3, -1)
        );
        assertEquals("タスク数は0以上である必要があります", exception.getMessage());
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("負荷状況更新 - null値の案件数・タスク数は許可")
    void updateWorkloadStatus_NullCounts() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(workloadStatusRepository.findByUser(testUser1)).thenReturn(Optional.empty());
        when(workloadStatusRepository.save(any(WorkloadStatus.class))).thenReturn(workloadStatus1);

        // When
        WorkloadStatus result = workloadStatusService.updateWorkloadStatus(
            1L, WorkloadLevel.MEDIUM, null, null);

        // Then
        assertNotNull(result);
        verify(workloadStatusRepository).save(any(WorkloadStatus.class));
    }

    @Test
    @DisplayName("ユーザー名による負荷状況更新")
    void updateWorkloadStatusByUsername() {
        // Given
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(testUser1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1)); // 追加
        when(workloadStatusRepository.findByUser(testUser1)).thenReturn(Optional.empty());
        when(workloadStatusRepository.save(any(WorkloadStatus.class))).thenReturn(workloadStatus1);

        // When
        WorkloadStatus result = workloadStatusService.updateWorkloadStatusByUsername(
            "user1", WorkloadLevel.HIGH, 3, 5);

        // Then
        assertNotNull(result);
        verify(userRepository).findByUsername("user1");
        verify(userRepository).findById(1L); // 追加
        verify(workloadStatusRepository).save(any(WorkloadStatus.class));
    }

    @Test
    @DisplayName("ユーザー名による負荷状況更新 - ユーザーが存在しない場合")
    void updateWorkloadStatusByUsername_UserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> workloadStatusService.updateWorkloadStatusByUsername(
                "nonexistent", WorkloadLevel.HIGH, 3, 5)
        );
        assertEquals("ユーザーが見つかりません: nonexistent", exception.getMessage());
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("負荷レベル別負荷状況取得")
    void getWorkloadStatusesByLevel() {
        // Given
        List<WorkloadStatus> highWorkloadList = Arrays.asList(workloadStatus1);
        when(workloadStatusRepository.findByWorkloadLevelWithUser(WorkloadLevel.HIGH))
            .thenReturn(highWorkloadList);

        // When
        List<WorkloadStatus> result = workloadStatusService.getWorkloadStatusesByLevel(WorkloadLevel.HIGH);

        // Then
        assertEquals(1, result.size());
        assertEquals(WorkloadLevel.HIGH, result.get(0).getWorkloadLevel());
        verify(workloadStatusRepository).findByWorkloadLevelWithUser(WorkloadLevel.HIGH);
    }

    @Test
    @DisplayName("負荷レベル別負荷状況取得 - null負荷レベル")
    void getWorkloadStatusesByLevel_NullLevel() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> workloadStatusService.getWorkloadStatusesByLevel(null)
        );
        assertEquals("負荷レベルは必須です", exception.getMessage());
        verify(workloadStatusRepository, never()).findByWorkloadLevelWithUser(any());
    }

    @Test
    @DisplayName("負荷レベル別負荷状況取得 - 空のリスト")
    void getWorkloadStatusesByLevel_EmptyList() {
        // Given
        when(workloadStatusRepository.findByWorkloadLevelWithUser(WorkloadLevel.MEDIUM))
            .thenReturn(Arrays.asList());

        // When
        List<WorkloadStatus> result = workloadStatusService.getWorkloadStatusesByLevel(WorkloadLevel.MEDIUM);

        // Then
        assertTrue(result.isEmpty());
        verify(workloadStatusRepository).findByWorkloadLevelWithUser(WorkloadLevel.MEDIUM);
    }

    @Test
    @DisplayName("ゼロ値の案件数・タスク数は有効")
    void updateWorkloadStatus_ZeroCounts() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(workloadStatusRepository.findByUser(testUser1)).thenReturn(Optional.empty());
        when(workloadStatusRepository.save(any(WorkloadStatus.class))).thenReturn(workloadStatus1);

        // When
        WorkloadStatus result = workloadStatusService.updateWorkloadStatus(
            1L, WorkloadLevel.LOW, 0, 0);

        // Then
        assertNotNull(result);
        verify(workloadStatusRepository).save(any(WorkloadStatus.class));
    }

    @Test
    @DisplayName("全ての負荷レベルでの更新テスト")
    void updateWorkloadStatus_AllLevels() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(workloadStatusRepository.findByUser(testUser1)).thenReturn(Optional.empty());
        when(workloadStatusRepository.save(any(WorkloadStatus.class))).thenReturn(workloadStatus1);

        // When & Then - HIGH
        assertDoesNotThrow(() -> workloadStatusService.updateWorkloadStatus(
            1L, WorkloadLevel.HIGH, 5, 10));

        // When & Then - MEDIUM
        assertDoesNotThrow(() -> workloadStatusService.updateWorkloadStatus(
            1L, WorkloadLevel.MEDIUM, 3, 6));

        // When & Then - LOW
        assertDoesNotThrow(() -> workloadStatusService.updateWorkloadStatus(
            1L, WorkloadLevel.LOW, 1, 2));

        verify(workloadStatusRepository, times(3)).save(any(WorkloadStatus.class));
    }
}