package com.teamdashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamdashboard.dto.WorkloadStatusRequestDTO;
import com.teamdashboard.entity.User;
import com.teamdashboard.entity.WorkloadLevel;
import com.teamdashboard.entity.WorkloadStatus;
import com.teamdashboard.service.WorkloadStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WorkloadStatusControllerの単体テスト
 */
@WebMvcTest(controllers = WorkloadStatusController.class, 
           excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
@DisplayName("WorkloadStatusController テスト")
class WorkloadStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkloadStatusService workloadStatusService;

    @Autowired
    private ObjectMapper objectMapper;

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
    @DisplayName("全メンバーの負荷状況取得 - 正常ケース")
    void getAllWorkloadStatuses_Success() throws Exception {
        // Given
        List<WorkloadStatus> workloadStatuses = Arrays.asList(workloadStatus2, workloadStatus1);
        when(workloadStatusService.getAllWorkloadStatuses()).thenReturn(workloadStatuses);

        // When & Then
        mockMvc.perform(get("/api/workload-status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].userId").value(2))
                .andExpect(jsonPath("$[0].username").value("user2"))
                .andExpect(jsonPath("$[0].displayName").value("ユーザー2"))
                .andExpect(jsonPath("$[0].workloadLevel").value("LOW"))
                .andExpect(jsonPath("$[0].workloadLevelDisplay").value("低"))
                .andExpect(jsonPath("$[0].projectCount").value(1))
                .andExpect(jsonPath("$[0].taskCount").value(2))
                .andExpect(jsonPath("$[1].id").value(1))
                .andExpect(jsonPath("$[1].workloadLevel").value("HIGH"));

        verify(workloadStatusService).getAllWorkloadStatuses();
    }

    @Test
    @DisplayName("全メンバーの負荷状況取得 - 空のリスト")
    void getAllWorkloadStatuses_EmptyList() throws Exception {
        // Given
        when(workloadStatusService.getAllWorkloadStatuses()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/workload-status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(workloadStatusService).getAllWorkloadStatuses();
    }

    @Test
    @DisplayName("全メンバーの負荷状況取得 - サービス例外")
    void getAllWorkloadStatuses_ServiceException() throws Exception {
        // Given
        when(workloadStatusService.getAllWorkloadStatuses()).thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/api/workload-status"))
                .andExpect(status().isInternalServerError());

        verify(workloadStatusService).getAllWorkloadStatuses();
    }

    @Test
    @DisplayName("自分の負荷状況取得 - 存在する場合")
    void getMyWorkloadStatus_Found() throws Exception {
        // Given
        when(workloadStatusService.getWorkloadStatusByUsername("testuser"))
                .thenReturn(Optional.of(workloadStatus1));

        // When & Then
        mockMvc.perform(get("/api/workload-status/my"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.workloadLevel").value("HIGH"))
                .andExpect(jsonPath("$.projectCount").value(3))
                .andExpect(jsonPath("$.taskCount").value(5));

        verify(workloadStatusService).getWorkloadStatusByUsername("testuser");
    }

    @Test
    @DisplayName("自分の負荷状況取得 - 存在しない場合")
    void getMyWorkloadStatus_NotFound() throws Exception {
        // Given
        when(workloadStatusService.getWorkloadStatusByUsername("testuser"))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/workload-status/my"))
                .andExpect(status().isNotFound());

        verify(workloadStatusService).getWorkloadStatusByUsername("testuser");
    }

    @Test
    @DisplayName("自分の負荷状況取得 - サービス例外")
    void getMyWorkloadStatus_ServiceException() throws Exception {
        // Given
        when(workloadStatusService.getWorkloadStatusByUsername("testuser"))
                .thenThrow(new IllegalArgumentException("Invalid username"));

        // When & Then
        mockMvc.perform(get("/api/workload-status/my"))
                .andExpect(status().isBadRequest());

        verify(workloadStatusService).getWorkloadStatusByUsername("testuser");
    }

    @Test
    @DisplayName("ユーザーID別負荷状況取得 - 存在する場合")
    void getWorkloadStatusByUserId_Found() throws Exception {
        // Given
        when(workloadStatusService.getWorkloadStatusByUserId(1L))
                .thenReturn(Optional.of(workloadStatus1));

        // When & Then
        mockMvc.perform(get("/api/workload-status/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.workloadLevel").value("HIGH"));

        verify(workloadStatusService).getWorkloadStatusByUserId(1L);
    }

    @Test
    @DisplayName("ユーザーID別負荷状況取得 - 存在しない場合")
    void getWorkloadStatusByUserId_NotFound() throws Exception {
        // Given
        when(workloadStatusService.getWorkloadStatusByUserId(999L))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/workload-status/999"))
                .andExpect(status().isNotFound());

        verify(workloadStatusService).getWorkloadStatusByUserId(999L);
    }

    @Test
    @DisplayName("負荷状況更新 - 正常ケース")
    void updateWorkloadStatus_Success() throws Exception {
        // Given
        WorkloadStatusRequestDTO request = new WorkloadStatusRequestDTO(WorkloadLevel.HIGH, 3, 5);
        when(workloadStatusService.updateWorkloadStatusByUsername("testuser", WorkloadLevel.HIGH, 3, 5))
                .thenReturn(workloadStatus1);

        // When & Then
        mockMvc.perform(post("/api/workload-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.workloadLevel").value("HIGH"))
                .andExpect(jsonPath("$.projectCount").value(3))
                .andExpect(jsonPath("$.taskCount").value(5));

        verify(workloadStatusService).updateWorkloadStatusByUsername("testuser", WorkloadLevel.HIGH, 3, 5);
    }

    @Test
    @DisplayName("負荷状況更新 - バリデーションエラー（null負荷レベル）")
    void updateWorkloadStatus_ValidationError() throws Exception {
        // Given
        WorkloadStatusRequestDTO request = new WorkloadStatusRequestDTO(null, 3, 5);

        // When & Then
        mockMvc.perform(post("/api/workload-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(workloadStatusService, never()).updateWorkloadStatusByUsername(any(), any(), any(), any());
    }

    @Test
    @DisplayName("負荷状況更新 - バリデーションエラー（負の案件数）")
    void updateWorkloadStatus_NegativeProjectCount() throws Exception {
        // Given
        WorkloadStatusRequestDTO request = new WorkloadStatusRequestDTO(WorkloadLevel.HIGH, -1, 5);

        // When & Then
        mockMvc.perform(post("/api/workload-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(workloadStatusService, never()).updateWorkloadStatusByUsername(any(), any(), any(), any());
    }

    @Test
    @DisplayName("負荷状況更新 - サービス例外（ユーザーが見つからない）")
    void updateWorkloadStatus_UserNotFound() throws Exception {
        // Given
        WorkloadStatusRequestDTO request = new WorkloadStatusRequestDTO(WorkloadLevel.HIGH, 3, 5);
        when(workloadStatusService.updateWorkloadStatusByUsername("testuser", WorkloadLevel.HIGH, 3, 5))
                .thenThrow(new RuntimeException("ユーザーが見つかりません"));

        // When & Then
        mockMvc.perform(post("/api/workload-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(workloadStatusService).updateWorkloadStatusByUsername("testuser", WorkloadLevel.HIGH, 3, 5);
    }

    @Test
    @DisplayName("負荷状況更新 - サービス例外（不正な引数）")
    void updateWorkloadStatus_IllegalArgument() throws Exception {
        // Given
        WorkloadStatusRequestDTO request = new WorkloadStatusRequestDTO(WorkloadLevel.HIGH, 3, 5);
        when(workloadStatusService.updateWorkloadStatusByUsername("testuser", WorkloadLevel.HIGH, 3, 5))
                .thenThrow(new IllegalArgumentException("Invalid argument"));

        // When & Then
        mockMvc.perform(post("/api/workload-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(workloadStatusService).updateWorkloadStatusByUsername("testuser", WorkloadLevel.HIGH, 3, 5);
    }

    @Test
    @DisplayName("ユーザーID別負荷状況更新 - 正常ケース")
    void updateWorkloadStatusByUserId_Success() throws Exception {
        // Given
        WorkloadStatusRequestDTO request = new WorkloadStatusRequestDTO(WorkloadLevel.MEDIUM, 2, 4);
        when(workloadStatusService.updateWorkloadStatus(1L, WorkloadLevel.MEDIUM, 2, 4))
                .thenReturn(workloadStatus1);

        // When & Then
        mockMvc.perform(put("/api/workload-status/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1));

        verify(workloadStatusService).updateWorkloadStatus(1L, WorkloadLevel.MEDIUM, 2, 4);
    }

    @Test
    @DisplayName("ユーザーID別負荷状況更新 - ユーザーが見つからない")
    void updateWorkloadStatusByUserId_UserNotFound() throws Exception {
        // Given
        WorkloadStatusRequestDTO request = new WorkloadStatusRequestDTO(WorkloadLevel.HIGH, 3, 5);
        when(workloadStatusService.updateWorkloadStatus(999L, WorkloadLevel.HIGH, 3, 5))
                .thenThrow(new RuntimeException("ユーザーが見つかりません"));

        // When & Then
        mockMvc.perform(put("/api/workload-status/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(workloadStatusService).updateWorkloadStatus(999L, WorkloadLevel.HIGH, 3, 5);
    }

    @Test
    @DisplayName("負荷状況更新 - null値の案件数・タスク数")
    void updateWorkloadStatus_NullCounts() throws Exception {
        // Given
        WorkloadStatusRequestDTO request = new WorkloadStatusRequestDTO(WorkloadLevel.LOW, null, null);
        when(workloadStatusService.updateWorkloadStatusByUsername("testuser", WorkloadLevel.LOW, null, null))
                .thenReturn(workloadStatus1);

        // When & Then
        mockMvc.perform(post("/api/workload-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(workloadStatusService).updateWorkloadStatusByUsername("testuser", WorkloadLevel.LOW, null, null);
    }

    @Test
    @DisplayName("負荷状況更新 - ゼロ値の案件数・タスク数")
    void updateWorkloadStatus_ZeroCounts() throws Exception {
        // Given
        WorkloadStatusRequestDTO request = new WorkloadStatusRequestDTO(WorkloadLevel.LOW, 0, 0);
        when(workloadStatusService.updateWorkloadStatusByUsername("testuser", WorkloadLevel.LOW, 0, 0))
                .thenReturn(workloadStatus1);

        // When & Then
        mockMvc.perform(post("/api/workload-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(workloadStatusService).updateWorkloadStatusByUsername("testuser", WorkloadLevel.LOW, 0, 0);
    }

    @Test
    @DisplayName("負荷状況更新 - 不正なJSON")
    void updateWorkloadStatus_InvalidJson() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/workload-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isInternalServerError());

        verify(workloadStatusService, never()).updateWorkloadStatusByUsername(any(), any(), any(), any());
    }

    @Test
    @DisplayName("負荷状況更新 - 空のリクエストボディ")
    void updateWorkloadStatus_EmptyBody() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/workload-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(workloadStatusService, never()).updateWorkloadStatusByUsername(any(), any(), any(), any());
    }

    @Test
    @DisplayName("CORS設定の確認")
    void corsConfiguration() throws Exception {
        // Given
        when(workloadStatusService.getAllWorkloadStatuses()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/workload-status")
                        .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }

    @Test
    @DisplayName("全ての負荷レベルでの更新テスト")
    void updateWorkloadStatus_AllLevels() throws Exception {
        // Given & When & Then - HIGH
        WorkloadStatusRequestDTO highRequest = new WorkloadStatusRequestDTO(WorkloadLevel.HIGH, 5, 10);
        when(workloadStatusService.updateWorkloadStatusByUsername("testuser", WorkloadLevel.HIGH, 5, 10))
                .thenReturn(workloadStatus1);

        mockMvc.perform(post("/api/workload-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(highRequest)))
                .andExpect(status().isOk());

        // Given & When & Then - MEDIUM
        WorkloadStatusRequestDTO mediumRequest = new WorkloadStatusRequestDTO(WorkloadLevel.MEDIUM, 3, 6);
        when(workloadStatusService.updateWorkloadStatusByUsername("testuser", WorkloadLevel.MEDIUM, 3, 6))
                .thenReturn(workloadStatus1);

        mockMvc.perform(post("/api/workload-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mediumRequest)))
                .andExpect(status().isOk());

        // Given & When & Then - LOW
        WorkloadStatusRequestDTO lowRequest = new WorkloadStatusRequestDTO(WorkloadLevel.LOW, 1, 2);
        when(workloadStatusService.updateWorkloadStatusByUsername("testuser", WorkloadLevel.LOW, 1, 2))
                .thenReturn(workloadStatus1);

        mockMvc.perform(post("/api/workload-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lowRequest)))
                .andExpect(status().isOk());

        verify(workloadStatusService).updateWorkloadStatusByUsername("testuser", WorkloadLevel.HIGH, 5, 10);
        verify(workloadStatusService).updateWorkloadStatusByUsername("testuser", WorkloadLevel.MEDIUM, 3, 6);
        verify(workloadStatusService).updateWorkloadStatusByUsername("testuser", WorkloadLevel.LOW, 1, 2);
    }
}