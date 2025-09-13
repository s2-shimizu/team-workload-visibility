package com.teamdashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamdashboard.dto.IssueCommentRequestDTO;
import com.teamdashboard.dto.TeamIssueRequestDTO;
import com.teamdashboard.entity.IssueComment;
import com.teamdashboard.entity.IssueStatus;
import com.teamdashboard.entity.TeamIssue;
import com.teamdashboard.entity.User;
import com.teamdashboard.repository.UserRepository;
import com.teamdashboard.service.TeamIssueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
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
 * TeamIssueControllerの単体テスト
 */
@WebMvcTest(controllers = TeamIssueController_Disabled.class,
           excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
@DisplayName("TeamIssueController テスト")
@Disabled("Controller is disabled - using DynamoDB version instead")
class TeamIssueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeamIssueService teamIssueService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser1;
    private User testUser2;
    private TeamIssue openIssue;
    private TeamIssue resolvedIssue;
    private IssueComment comment1;
    private IssueComment comment2;

    @BeforeEach
    void setUp() {
        testUser1 = new User("user1", "password1", "user1@example.com", "ユーザー1");
        testUser1.setId(1L);
        
        testUser2 = new User("user2", "password2", "user2@example.com", "ユーザー2");
        testUser2.setId(2L);

        openIssue = new TeamIssue(testUser1, "データベース接続エラーが発生しています");
        openIssue.setId(1L);
        openIssue.setCreatedAt(LocalDateTime.now().minusHours(1));

        resolvedIssue = new TeamIssue(testUser2, "APIのレスポンスが遅い問題");
        resolvedIssue.setId(2L);
        resolvedIssue.setCreatedAt(LocalDateTime.now().minusHours(2));
        resolvedIssue.resolve();

        comment1 = new IssueComment(openIssue, testUser1, "設定ファイルを確認してください");
        comment1.setId(1L);
        comment1.setCreatedAt(LocalDateTime.now().minusMinutes(30));

        comment2 = new IssueComment(openIssue, testUser2, "データベースの接続設定が間違っているかもしれません");
        comment2.setId(2L);
        comment2.setCreatedAt(LocalDateTime.now().minusMinutes(15));
    }

    @Test
    @DisplayName("全困りごと取得 - 正常ケース")
    void getAllIssues_Success() throws Exception {
        // Given
        List<TeamIssue> issues = Arrays.asList(openIssue, resolvedIssue);
        when(teamIssueService.getAllIssues()).thenReturn(issues);
        when(teamIssueService.getCommentCount(1L)).thenReturn(2L);
        when(teamIssueService.getCommentCount(2L)).thenReturn(0L);

        // When & Then
        mockMvc.perform(get("/api/team-issues"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].username").value("user1"))
                .andExpect(jsonPath("$[0].displayName").value("ユーザー1"))
                .andExpect(jsonPath("$[0].content").value("データベース接続エラーが発生しています"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].commentCount").value(2))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].status").value("RESOLVED"))
                .andExpect(jsonPath("$[1].commentCount").value(0));

        verify(teamIssueService).getAllIssues();
        verify(teamIssueService).getCommentCount(1L);
        verify(teamIssueService).getCommentCount(2L);
    }

    @Test
    @DisplayName("困りごと取得 - ステータス指定（OPEN）")
    void getAllIssues_FilterByOpenStatus() throws Exception {
        // Given
        List<TeamIssue> openIssues = Arrays.asList(openIssue);
        when(teamIssueService.getOpenIssues()).thenReturn(openIssues);
        when(teamIssueService.getCommentCount(1L)).thenReturn(2L);

        // When & Then
        mockMvc.perform(get("/api/team-issues?status=OPEN"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("OPEN"));

        verify(teamIssueService).getOpenIssues();
        verify(teamIssueService, never()).getAllIssues();
    }

    @Test
    @DisplayName("困りごと取得 - ステータス指定（RESOLVED）")
    void getAllIssues_FilterByResolvedStatus() throws Exception {
        // Given
        List<TeamIssue> resolvedIssues = Arrays.asList(resolvedIssue);
        when(teamIssueService.getResolvedIssues()).thenReturn(resolvedIssues);
        when(teamIssueService.getCommentCount(2L)).thenReturn(0L);

        // When & Then
        mockMvc.perform(get("/api/team-issues?status=RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].status").value("RESOLVED"));

        verify(teamIssueService).getResolvedIssues();
        verify(teamIssueService, never()).getAllIssues();
    }

    @Test
    @DisplayName("困りごと取得 - 不正なステータス指定")
    void getAllIssues_InvalidStatus() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/team-issues?status=INVALID"))
                .andExpect(status().isBadRequest());

        verify(teamIssueService, never()).getAllIssues();
        verify(teamIssueService, never()).getOpenIssues();
        verify(teamIssueService, never()).getResolvedIssues();
    }

    @Test
    @DisplayName("困りごと取得 - 空のリスト")
    void getAllIssues_EmptyList() throws Exception {
        // Given
        when(teamIssueService.getAllIssues()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/team-issues"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(teamIssueService).getAllIssues();
    }

    @Test
    @DisplayName("ID別困りごと取得 - 存在する場合")
    void getIssueById_Found() throws Exception {
        // Given
        when(teamIssueService.getIssueById(1L)).thenReturn(Optional.of(openIssue));
        when(teamIssueService.getCommentCount(1L)).thenReturn(2L);

        // When & Then
        mockMvc.perform(get("/api/team-issues/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.content").value("データベース接続エラーが発生しています"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.commentCount").value(2));

        verify(teamIssueService).getIssueById(1L);
        verify(teamIssueService).getCommentCount(1L);
    }

    @Test
    @DisplayName("ID別困りごと取得 - 存在しない場合")
    void getIssueById_NotFound() throws Exception {
        // Given
        when(teamIssueService.getIssueById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/team-issues/999"))
                .andExpect(status().isNotFound());

        verify(teamIssueService).getIssueById(999L);
        verify(teamIssueService, never()).getCommentCount(any());
    }

    @Test
    @DisplayName("困りごと作成 - 正常ケース（認証なし）")
    void createIssue_Success() throws Exception {
        // Given
        TeamIssueRequestDTO request = new TeamIssueRequestDTO("新しい困りごとです");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser1));
        when(teamIssueService.createIssue(1L, "新しい困りごとです")).thenReturn(openIssue);

        // When & Then
        mockMvc.perform(post("/api/team-issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.content").value("データベース接続エラーが発生しています"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.commentCount").value(0));

        verify(userRepository).findByUsername("testuser");
        verify(teamIssueService).createIssue(1L, "新しい困りごとです");
    }

    @Test
    @DisplayName("困りごと作成 - ユーザーが存在しない場合")
    void createIssue_UserNotFound() throws Exception {
        // Given
        TeamIssueRequestDTO request = new TeamIssueRequestDTO("困りごと内容");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/api/team-issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(userRepository).findByUsername("testuser");
        verify(teamIssueService, never()).createIssue(any(), any());
    }

    @Test
    @DisplayName("困りごと作成 - バリデーションエラー（空のコンテンツ）")
    void createIssue_EmptyContent() throws Exception {
        // Given
        TeamIssueRequestDTO request = new TeamIssueRequestDTO("");

        // When & Then
        mockMvc.perform(post("/api/team-issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(teamIssueService, never()).createIssue(any(), any());
    }

    @Test
    @DisplayName("困りごと作成 - バリデーションエラー（長すぎるコンテンツ）")
    void createIssue_TooLongContent() throws Exception {
        // Given
        String longContent = "a".repeat(1001); // 1001文字
        TeamIssueRequestDTO request = new TeamIssueRequestDTO(longContent);

        // When & Then
        mockMvc.perform(post("/api/team-issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(teamIssueService, never()).createIssue(any(), any());
    }

    @Test
    @DisplayName("困りごと作成 - サービス例外")
    void createIssue_ServiceException() throws Exception {
        // Given
        TeamIssueRequestDTO request = new TeamIssueRequestDTO("困りごと内容");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser1));
        when(teamIssueService.createIssue(1L, "困りごと内容"))
                .thenThrow(new IllegalArgumentException("Invalid content"));

        // When & Then
        mockMvc.perform(post("/api/team-issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(teamIssueService).createIssue(1L, "困りごと内容");
    }

    @Test
    @DisplayName("困りごと解決 - 正常ケース")
    void resolveIssue_Success() throws Exception {
        // Given
        when(teamIssueService.resolveIssue(1L)).thenReturn(resolvedIssue);
        when(teamIssueService.getCommentCount(1L)).thenReturn(0L);

        // When & Then
        mockMvc.perform(put("/api/team-issues/1/resolve"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        verify(teamIssueService).resolveIssue(1L);
        verify(teamIssueService).getCommentCount(1L);
    }

    @Test
    @DisplayName("困りごと解決 - 困りごとが存在しない場合")
    void resolveIssue_IssueNotFound() throws Exception {
        // Given
        when(teamIssueService.resolveIssue(999L))
                .thenThrow(new IllegalArgumentException("困りごとが見つかりません"));

        // When & Then
        mockMvc.perform(put("/api/team-issues/999/resolve"))
                .andExpect(status().isNotFound());

        verify(teamIssueService).resolveIssue(999L);
    }

    @Test
    @DisplayName("困りごと解決 - 既に解決済みの場合")
    void resolveIssue_AlreadyResolved() throws Exception {
        // Given
        when(teamIssueService.resolveIssue(2L))
                .thenThrow(new IllegalStateException("この困りごとは既に解決済みです"));

        // When & Then
        mockMvc.perform(put("/api/team-issues/2/resolve"))
                .andExpect(status().isBadRequest());

        verify(teamIssueService).resolveIssue(2L);
    }

    @Test
    @DisplayName("困りごと再オープン - 正常ケース")
    void reopenIssue_Success() throws Exception {
        // Given
        when(teamIssueService.reopenIssue(2L)).thenReturn(openIssue);
        when(teamIssueService.getCommentCount(2L)).thenReturn(1L);

        // When & Then
        mockMvc.perform(put("/api/team-issues/2/reopen"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("OPEN"));

        verify(teamIssueService).reopenIssue(2L);
        verify(teamIssueService).getCommentCount(2L);
    }

    @Test
    @DisplayName("困りごと再オープン - 困りごとが存在しない場合")
    void reopenIssue_IssueNotFound() throws Exception {
        // Given
        when(teamIssueService.reopenIssue(999L))
                .thenThrow(new IllegalArgumentException("困りごとが見つかりません"));

        // When & Then
        mockMvc.perform(put("/api/team-issues/999/reopen"))
                .andExpect(status().isNotFound());

        verify(teamIssueService).reopenIssue(999L);
    }

    @Test
    @DisplayName("困りごと再オープン - 既に未解決の場合")
    void reopenIssue_AlreadyOpen() throws Exception {
        // Given
        when(teamIssueService.reopenIssue(1L))
                .thenThrow(new IllegalStateException("この困りごとは既に未解決状態です"));

        // When & Then
        mockMvc.perform(put("/api/team-issues/1/reopen"))
                .andExpect(status().isBadRequest());

        verify(teamIssueService).reopenIssue(1L);
    }

    @Test
    @DisplayName("困りごと別コメント取得 - 正常ケース")
    void getCommentsByIssueId_Success() throws Exception {
        // Given
        List<IssueComment> comments = Arrays.asList(comment1, comment2);
        when(teamIssueService.getIssueById(1L)).thenReturn(Optional.of(openIssue));
        when(teamIssueService.getCommentsByIssueId(1L)).thenReturn(comments);

        // When & Then
        mockMvc.perform(get("/api/team-issues/1/comments"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].issueId").value(1))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].username").value("user1"))
                .andExpect(jsonPath("$[0].content").value("設定ファイルを確認してください"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].userId").value(2))
                .andExpect(jsonPath("$[1].content").value("データベースの接続設定が間違っているかもしれません"));

        verify(teamIssueService).getIssueById(1L);
        verify(teamIssueService).getCommentsByIssueId(1L);
    }

    @Test
    @DisplayName("困りごと別コメント取得 - 困りごとが存在しない場合")
    void getCommentsByIssueId_IssueNotFound() throws Exception {
        // Given
        when(teamIssueService.getIssueById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/team-issues/999/comments"))
                .andExpect(status().isNotFound());

        verify(teamIssueService).getIssueById(999L);
        verify(teamIssueService, never()).getCommentsByIssueId(any());
    }

    @Test
    @DisplayName("コメント追加 - 正常ケース（認証なし）")
    void addComment_Success() throws Exception {
        // Given
        IssueCommentRequestDTO request = new IssueCommentRequestDTO("新しいコメントです");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser1));
        when(teamIssueService.addComment(1L, 1L, "新しいコメントです")).thenReturn(comment1);

        // When & Then
        mockMvc.perform(post("/api/team-issues/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.issueId").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.content").value("設定ファイルを確認してください"));

        verify(userRepository).findByUsername("testuser");
        verify(teamIssueService).addComment(1L, 1L, "新しいコメントです");
    }

    @Test
    @DisplayName("コメント追加 - ユーザーが存在しない場合")
    void addComment_UserNotFound() throws Exception {
        // Given
        IssueCommentRequestDTO request = new IssueCommentRequestDTO("コメント内容");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/api/team-issues/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(userRepository).findByUsername("testuser");
        verify(teamIssueService, never()).addComment(any(), any(), any());
    }

    @Test
    @DisplayName("コメント追加 - バリデーションエラー（空のコンテンツ）")
    void addComment_EmptyContent() throws Exception {
        // Given
        IssueCommentRequestDTO request = new IssueCommentRequestDTO("");

        // When & Then
        mockMvc.perform(post("/api/team-issues/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(teamIssueService, never()).addComment(any(), any(), any());
    }

    @Test
    @DisplayName("コメント追加 - バリデーションエラー（長すぎるコンテンツ）")
    void addComment_TooLongContent() throws Exception {
        // Given
        String longContent = "a".repeat(501); // 501文字
        IssueCommentRequestDTO request = new IssueCommentRequestDTO(longContent);

        // When & Then
        mockMvc.perform(post("/api/team-issues/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(teamIssueService, never()).addComment(any(), any(), any());
    }

    @Test
    @DisplayName("コメント追加 - サービス例外（困りごとが存在しない）")
    void addComment_IssueNotFound() throws Exception {
        // Given
        IssueCommentRequestDTO request = new IssueCommentRequestDTO("コメント内容");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser1));
        when(teamIssueService.addComment(999L, 1L, "コメント内容"))
                .thenThrow(new IllegalArgumentException("困りごとが見つかりません"));

        // When & Then
        mockMvc.perform(post("/api/team-issues/999/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(teamIssueService).addComment(999L, 1L, "コメント内容");
    }

    @Test
    @DisplayName("CORS設定の確認")
    void corsConfiguration() throws Exception {
        // Given
        when(teamIssueService.getAllIssues()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/team-issues")
                        .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }

    @Test
    @DisplayName("困りごと作成 - 1000文字ちょうどは有効")
    void createIssue_MaxLengthContent() throws Exception {
        // Given
        String maxContent = "a".repeat(1000); // 1000文字
        TeamIssueRequestDTO request = new TeamIssueRequestDTO(maxContent);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser1));
        when(teamIssueService.createIssue(1L, maxContent)).thenReturn(openIssue);

        // When & Then
        mockMvc.perform(post("/api/team-issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(teamIssueService).createIssue(1L, maxContent);
    }

    @Test
    @DisplayName("コメント追加 - 500文字ちょうどは有効")
    void addComment_MaxLengthContent() throws Exception {
        // Given
        String maxContent = "a".repeat(500); // 500文字
        IssueCommentRequestDTO request = new IssueCommentRequestDTO(maxContent);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser1));
        when(teamIssueService.addComment(1L, 1L, maxContent)).thenReturn(comment1);

        // When & Then
        mockMvc.perform(post("/api/team-issues/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(teamIssueService).addComment(1L, 1L, maxContent);
    }

    @Test
    @DisplayName("困りごと取得 - サービス例外")
    void getAllIssues_ServiceException() throws Exception {
        // Given
        when(teamIssueService.getAllIssues()).thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/api/team-issues"))
                .andExpect(status().isInternalServerError());

        verify(teamIssueService).getAllIssues();
    }

    @Test
    @DisplayName("困りごと別コメント取得 - 空のリスト")
    void getCommentsByIssueId_EmptyList() throws Exception {
        // Given
        when(teamIssueService.getIssueById(1L)).thenReturn(Optional.of(openIssue));
        when(teamIssueService.getCommentsByIssueId(1L)).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/team-issues/1/comments"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(teamIssueService).getIssueById(1L);
        verify(teamIssueService).getCommentsByIssueId(1L);
    }
}