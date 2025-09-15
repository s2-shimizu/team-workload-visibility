package com.teamdashboard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamdashboard.LambdaApplication;
import com.teamdashboard.dto.*;
import com.teamdashboard.entity.*;
import com.teamdashboard.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ダッシュボード全体機能の統合テスト
 * 要件: 4.1, 4.2 - 全体機能の統合テスト
 */
@SpringBootTest(classes = LambdaApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class DashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkloadStatusRepository workloadStatusRepository;

    @Autowired
    private TeamIssueRepository teamIssueRepository;

    @Autowired
    private IssueCommentRepository issueCommentRepository;

    private User testUser1;
    private User testUser2;
    private User testUser3;

    @BeforeEach
    void setUp() {
        // 既存データをクリーンアップ
        issueCommentRepository.deleteAll();
        teamIssueRepository.deleteAll();
        workloadStatusRepository.deleteAll();
        userRepository.deleteAll();
        
        // テストユーザーを作成
        testUser1 = createTestUser("testuser1", "テストユーザー1", "test1@example.com");
        testUser2 = createTestUser("testuser2", "テストユーザー2", "test2@example.com");
        testUser3 = createTestUser("testuser3", "テストユーザー3", "test3@example.com");
    }

    private User createTestUser(String username, String displayName, String email) {
        User user = new User();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setPassword("password");
        return userRepository.save(user);
    }

    /**
     * ダッシュボード全体の表示・更新機能テストを実行
     * 要件: 4.1, 4.2
     */
    @Test
    @WithMockUser(username = "testuser1")
    void testDashboardOverallFunctionality() throws Exception {
        // 1. 初期状態でのダッシュボードデータ取得テスト
        
        // 負荷状況一覧取得（初期状態では空）
        mockMvc.perform(get("/api/workload-status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        // 困りごと一覧取得（初期状態では空）
        mockMvc.perform(get("/api/team-issues"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        // 2. 負荷状況更新テスト
        WorkloadStatusRequestDTO workloadRequest = new WorkloadStatusRequestDTO();
        workloadRequest.setWorkloadLevel(WorkloadLevel.HIGH);
        workloadRequest.setProjectCount(3);
        workloadRequest.setTaskCount(15);

        mockMvc.perform(post("/api/workload-status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(workloadRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workloadLevel").value("HIGH"))
                .andExpect(jsonPath("$.projectCount").value(3))
                .andExpect(jsonPath("$.taskCount").value(15))
                .andExpect(jsonPath("$.user.username").value("testuser1"));

        // 3. 困りごと投稿テスト
        TeamIssueRequestDTO issueRequest = new TeamIssueRequestDTO();
        issueRequest.setContent("プロジェクトAの進捗が遅れています。リソース不足が原因です。");

        String issueResponse = mockMvc.perform(post("/api/team-issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(issueRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("プロジェクトAの進捗が遅れています。リソース不足が原因です。"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.user.username").value("testuser1"))
                .andReturn().getResponse().getContentAsString();

        TeamIssueResponseDTO createdIssue = objectMapper.readValue(issueResponse, TeamIssueResponseDTO.class);

        // 4. 更新後のダッシュボードデータ確認
        
        // 負荷状況一覧に反映されているか確認
        mockMvc.perform(get("/api/workload-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].workloadLevel").value("HIGH"))
                .andExpect(jsonPath("$[0].user.username").value("testuser1"));

        // 困りごと一覧に反映されているか確認
        mockMvc.perform(get("/api/team-issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("プロジェクトAの進捗が遅れています。リソース不足が原因です。"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));

        // 5. コメント投稿テスト
        IssueCommentRequestDTO commentRequest = new IssueCommentRequestDTO();
        commentRequest.setContent("来週までに追加リソースを確保できる予定です。");

        mockMvc.perform(post("/api/team-issues/" + createdIssue.getId() + "/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("来週までに追加リソースを確保できる予定です。"))
                .andExpect(jsonPath("$.user.username").value("testuser1"));

        // 6. 困りごと解決マークテスト
        mockMvc.perform(put("/api/team-issues/" + createdIssue.getId() + "/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").exists());

        // 7. 最終状態確認
        mockMvc.perform(get("/api/team-issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("RESOLVED"));
    }

    /**
     * 認証機能と新機能の連携テストを実装
     * 要件: 4.1, 4.2
     */
    @Test
    @WithMockUser(username = "testuser2")
    void testAuthenticationIntegrationWithNewFeatures() throws Exception {
        // 1. 認証されたユーザーでの負荷状況更新
        WorkloadStatusRequestDTO request = new WorkloadStatusRequestDTO();
        request.setWorkloadLevel(WorkloadLevel.MEDIUM);
        request.setProjectCount(2);
        request.setTaskCount(8);

        mockMvc.perform(post("/api/workload-status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("testuser2"));

        // 2. 自分の負荷状況取得
        mockMvc.perform(get("/api/workload-status/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workloadLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.user.username").value("testuser2"));

        // 3. 認証されたユーザーでの困りごと投稿
        TeamIssueRequestDTO issueRequest = new TeamIssueRequestDTO();
        issueRequest.setContent("新しい技術の学習時間が不足しています。");

        mockMvc.perform(post("/api/team-issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(issueRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("testuser2"))
                .andExpect(jsonPath("$.content").value("新しい技術の学習時間が不足しています。"));
    }

    /**
     * 複数ユーザーでの同時操作テスト
     * 要件: 4.1, 4.2
     */
    @Test
    void testMultiUserConcurrentOperations() throws Exception {
        // 複数ユーザーの負荷状況を設定
        setupMultipleUserWorkloadStatuses();
        
        // 複数ユーザーの困りごとを設定
        setupMultipleUserIssues();

        // 全体データ取得テスト
        mockMvc.perform(get("/api/workload-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.user.username == 'testuser1')].workloadLevel").value("HIGH"))
                .andExpect(jsonPath("$[?(@.user.username == 'testuser2')].workloadLevel").value("MEDIUM"))
                .andExpect(jsonPath("$[?(@.user.username == 'testuser3')].workloadLevel").value("LOW"));

        mockMvc.perform(get("/api/team-issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    /**
     * パフォーマンステストとレスポンス時間測定を実行
     * 要件: 4.2
     */
    @Test
    @WithMockUser(username = "testuser1")
    void testPerformanceAndResponseTime() throws Exception {
        // 大量データを準備
        setupLargeDataSet();

        // レスポンス時間測定
        long startTime = System.currentTimeMillis();
        
        mockMvc.perform(get("/api/workload-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        
        long workloadResponseTime = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        
        mockMvc.perform(get("/api/team-issues"))
                .andExpect(status().isOk());
        
        long issuesResponseTime = System.currentTimeMillis() - startTime;

        // パフォーマンス要件確認（1秒以内）
        assertThat(workloadResponseTime).isLessThan(1000);
        assertThat(issuesResponseTime).isLessThan(1000);

        // 複数回実行してパフォーマンスの安定性確認
        for (int i = 0; i < 5; i++) {
            startTime = System.currentTimeMillis();
            
            mockMvc.perform(get("/api/workload-status"))
                    .andExpect(status().isOk());
            
            long responseTime = System.currentTimeMillis() - startTime;
            assertThat(responseTime).isLessThan(1000);
        }
    }

    /**
     * エラーハンドリングと例外処理テスト
     * 要件: 4.1, 4.2
     */
    @Test
    @WithMockUser(username = "testuser1")
    void testErrorHandlingAndExceptionProcessing() throws Exception {
        // 1. 不正なデータでの負荷状況更新
        WorkloadStatusRequestDTO invalidRequest = new WorkloadStatusRequestDTO();
        // workloadLevelを設定しない

        mockMvc.perform(post("/api/workload-status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        // 2. 空の内容での困りごと投稿
        TeamIssueRequestDTO emptyIssueRequest = new TeamIssueRequestDTO();
        emptyIssueRequest.setContent("");

        mockMvc.perform(post("/api/team-issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyIssueRequest)))
                .andExpect(status().isBadRequest());

        // 3. 存在しない困りごとへのコメント投稿
        IssueCommentRequestDTO commentRequest = new IssueCommentRequestDTO();
        commentRequest.setContent("コメント");

        mockMvc.perform(post("/api/team-issues/999/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isNotFound());

        // 4. 存在しない困りごとの解決マーク
        mockMvc.perform(put("/api/team-issues/999/resolve"))
                .andExpect(status().isNotFound());
    }

    /**
     * データ整合性テスト
     * 要件: 4.1, 4.2
     */
    @Test
    @WithMockUser(username = "testuser1")
    void testDataConsistency() throws Exception {
        // 1. 負荷状況更新
        WorkloadStatusRequestDTO workloadRequest = new WorkloadStatusRequestDTO();
        workloadRequest.setWorkloadLevel(WorkloadLevel.HIGH);
        workloadRequest.setProjectCount(3);
        workloadRequest.setTaskCount(15);

        mockMvc.perform(post("/api/workload-status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(workloadRequest)))
                .andExpect(status().isOk());

        // 2. データベース直接確認
        List<WorkloadStatus> workloadStatuses = workloadStatusRepository.findAll();
        assertThat(workloadStatuses).hasSize(1);
        WorkloadStatus savedStatus = workloadStatuses.get(0);
        assertThat(savedStatus.getWorkloadLevel()).isEqualTo(WorkloadLevel.HIGH);
        assertThat(savedStatus.getProjectCount()).isEqualTo(3);
        assertThat(savedStatus.getTaskCount()).isEqualTo(15);
        assertThat(savedStatus.getUser().getUsername()).isEqualTo("testuser1");

        // 3. 困りごと投稿
        TeamIssueRequestDTO issueRequest = new TeamIssueRequestDTO();
        issueRequest.setContent("テスト困りごと");

        String response = mockMvc.perform(post("/api/team-issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(issueRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        TeamIssueResponseDTO createdIssue = objectMapper.readValue(response, TeamIssueResponseDTO.class);

        // 4. データベース直接確認
        List<TeamIssue> teamIssues = teamIssueRepository.findAll();
        assertThat(teamIssues).hasSize(1);
        TeamIssue savedIssue = teamIssues.get(0);
        assertThat(savedIssue.getContent()).isEqualTo("テスト困りごと");
        assertThat(savedIssue.getStatus()).isEqualTo(IssueStatus.OPEN);
        assertThat(savedIssue.getUser().getUsername()).isEqualTo("testuser1");
        assertThat(savedIssue.getCreatedAt()).isNotNull();
    }

    private void setupMultipleUserWorkloadStatuses() {
        LocalDateTime now = LocalDateTime.now();

        WorkloadStatus status1 = new WorkloadStatus();
        status1.setUser(testUser1);
        status1.setWorkloadLevel(WorkloadLevel.HIGH);
        status1.setProjectCount(4);
        status1.setTaskCount(20);
        status1.setUpdatedAt(now.minusHours(1));
        workloadStatusRepository.save(status1);

        WorkloadStatus status2 = new WorkloadStatus();
        status2.setUser(testUser2);
        status2.setWorkloadLevel(WorkloadLevel.MEDIUM);
        status2.setProjectCount(2);
        status2.setTaskCount(8);
        status2.setUpdatedAt(now.minusHours(2));
        workloadStatusRepository.save(status2);

        WorkloadStatus status3 = new WorkloadStatus();
        status3.setUser(testUser3);
        status3.setWorkloadLevel(WorkloadLevel.LOW);
        status3.setProjectCount(1);
        status3.setTaskCount(3);
        status3.setUpdatedAt(now.minusHours(3));
        workloadStatusRepository.save(status3);
    }

    private void setupMultipleUserIssues() {
        LocalDateTime now = LocalDateTime.now();

        TeamIssue issue1 = new TeamIssue();
        issue1.setUser(testUser1);
        issue1.setContent("プロジェクトAの進捗遅延");
        issue1.setStatus(IssueStatus.OPEN);
        issue1.setCreatedAt(now.minusHours(1));
        teamIssueRepository.save(issue1);

        TeamIssue issue2 = new TeamIssue();
        issue2.setUser(testUser2);
        issue2.setContent("新技術の学習時間不足");
        issue2.setStatus(IssueStatus.OPEN);
        issue2.setCreatedAt(now.minusHours(2));
        teamIssueRepository.save(issue2);

        TeamIssue issue3 = new TeamIssue();
        issue3.setUser(testUser3);
        issue3.setContent("コードレビューの待ち時間");
        issue3.setStatus(IssueStatus.RESOLVED);
        issue3.setCreatedAt(now.minusHours(3));
        issue3.setResolvedAt(now.minusMinutes(30));
        teamIssueRepository.save(issue3);
    }

    private void setupLargeDataSet() {
        setupMultipleUserWorkloadStatuses();
        setupMultipleUserIssues();
        
        // 追加のテストデータ（パフォーマンステスト用）
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < 10; i++) {
            TeamIssue issue = new TeamIssue();
            issue.setUser(testUser1);
            issue.setContent("パフォーマンステスト用困りごと " + i);
            issue.setStatus(IssueStatus.OPEN);
            issue.setCreatedAt(now.minusHours(i));
            teamIssueRepository.save(issue);
        }
    }
}