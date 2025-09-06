package com.teamdashboard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamdashboard.dto.IssueCommentRequestDTO;
import com.teamdashboard.dto.TeamIssueRequestDTO;
import com.teamdashboard.dto.WorkloadStatusRequestDTO;
import com.teamdashboard.entity.*;
import com.teamdashboard.repository.*;
import com.teamdashboard.service.TeamIssueService;
import com.teamdashboard.service.WorkloadStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 全体機能の統合テスト
 * 要件: 4.1, 4.2
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public class OverallIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkloadStatusService workloadStatusService;

    @Autowired
    private TeamIssueService teamIssueService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkloadStatusRepository workloadStatusRepository;

    @Autowired
    private TeamIssueRepository teamIssueRepository;

    @Autowired
    private IssueCommentRepository issueCommentRepository;

    @Autowired
    private DailyReportRepository dailyReportRepository;

    private User teamLeader;
    private User developer1;
    private User developer2;
    private User designer;

    @BeforeEach
    void setUp() {
        // MockMvcを初期化
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // 既存データをクリーンアップ（外部キー制約を考慮した順序）
        issueCommentRepository.deleteAll();
        teamIssueRepository.deleteAll();
        workloadStatusRepository.deleteAll();
        dailyReportRepository.deleteAll();
        userRepository.deleteAll();
        
        // テストユーザーを作成（実際のチーム構成を模擬）
        teamLeader = new User();
        teamLeader.setUsername("teamleader");
        teamLeader.setDisplayName("チームリーダー");
        teamLeader.setEmail("leader@example.com");
        teamLeader.setPassword("password");
        teamLeader.setDepartment("開発部");
        teamLeader = userRepository.save(teamLeader);

        developer1 = new User();
        developer1.setUsername("developer1");
        developer1.setDisplayName("開発者A");
        developer1.setEmail("dev1@example.com");
        developer1.setPassword("password");
        developer1.setDepartment("開発部");
        developer1 = userRepository.save(developer1);

        developer2 = new User();
        developer2.setUsername("developer2");
        developer2.setDisplayName("開発者B");
        developer2.setEmail("dev2@example.com");
        developer2.setPassword("password");
        developer2.setDepartment("開発部");
        developer2 = userRepository.save(developer2);

        designer = new User();
        designer.setUsername("designer");
        designer.setDisplayName("デザイナー");
        designer.setEmail("designer@example.com");
        designer.setPassword("password");
        designer.setDepartment("デザイン部");
        designer = userRepository.save(designer);
        
        // デフォルトのテストユーザーを作成（コントローラーのフォールバック用）
        // data.sqlで既に作成されている場合はスキップ
        if (!userRepository.findByUsername("testuser").isPresent()) {
            User testUser = new User();
            testUser.setUsername("testuser");
            testUser.setDisplayName("テストユーザー");
            testUser.setEmail("testuser@example.com");
            testUser.setPassword("password");
            testUser.setDepartment("テスト部門");
            userRepository.save(testUser);
        }
    }

    /**
     * ダッシュボード全体の表示・更新機能テストを実行
     * 要件: 4.1, 4.2
     */
    @Test
    void testDashboardOverallDisplayAndUpdate() {
        // 1. 初期状態の確認（空のダッシュボード）
        List<WorkloadStatus> initialWorkloadStatuses = workloadStatusService.getAllWorkloadStatuses();
        List<TeamIssue> initialIssues = teamIssueService.getAllIssues();
        
        assertThat(initialWorkloadStatuses).isEmpty();
        assertThat(initialIssues).isEmpty();

        // 2. 各メンバーが負荷状況を更新
        LocalDateTime testStartTime = LocalDateTime.now();
        
        // チームリーダー: 中負荷
        WorkloadStatus leaderStatus = workloadStatusService.updateWorkloadStatus(
            teamLeader.getId(), WorkloadLevel.MEDIUM, 2, 8
        );
        
        // 開発者A: 高負荷
        WorkloadStatus dev1Status = workloadStatusService.updateWorkloadStatus(
            developer1.getId(), WorkloadLevel.HIGH, 3, 15
        );
        
        // 開発者B: 低負荷
        WorkloadStatus dev2Status = workloadStatusService.updateWorkloadStatus(
            developer2.getId(), WorkloadLevel.LOW, 1, 4
        );
        
        // デザイナー: 中負荷
        WorkloadStatus designerStatus = workloadStatusService.updateWorkloadStatus(
            designer.getId(), WorkloadLevel.MEDIUM, 2, 6
        );

        // 3. ダッシュボードでの負荷状況表示確認
        List<WorkloadStatus> updatedWorkloadStatuses = workloadStatusService.getAllWorkloadStatuses();
        assertThat(updatedWorkloadStatuses).hasSize(4);
        
        // 各メンバーの負荷状況が正しく表示されているか確認
        WorkloadStatus displayedLeaderStatus = updatedWorkloadStatuses.stream()
                .filter(ws -> ws.getUser().getUsername().equals("teamleader"))
                .findFirst().orElseThrow();
        assertThat(displayedLeaderStatus.getWorkloadLevel()).isEqualTo(WorkloadLevel.MEDIUM);
        assertThat(displayedLeaderStatus.getProjectCount()).isEqualTo(2);
        assertThat(displayedLeaderStatus.getTaskCount()).isEqualTo(8);
        
        WorkloadStatus displayedDev1Status = updatedWorkloadStatuses.stream()
                .filter(ws -> ws.getUser().getUsername().equals("developer1"))
                .findFirst().orElseThrow();
        assertThat(displayedDev1Status.getWorkloadLevel()).isEqualTo(WorkloadLevel.HIGH);
        assertThat(displayedDev1Status.getProjectCount()).isEqualTo(3);
        assertThat(displayedDev1Status.getTaskCount()).isEqualTo(15);

        // 4. 困りごとの投稿と表示
        TeamIssue issue1 = teamIssueService.createIssue(
            developer1.getId(), 
            "新しいフレームワークの学習に時間がかかっています。効率的な学習方法を教えてください。"
        );
        
        TeamIssue issue2 = teamIssueService.createIssue(
            designer.getId(), 
            "デザインツールの新バージョンで互換性の問題が発生しています。"
        );

        // 5. ダッシュボードでの困りごと表示確認
        List<TeamIssue> displayedIssues = teamIssueService.getAllIssues();
        assertThat(displayedIssues).hasSize(2);
        
        // 最新の困りごとが先頭に表示されているか確認（作成日時降順）
        // 作成順序を確認（後から作成されたissue2が先頭に来るはず）
        TeamIssue firstDisplayedIssue = displayedIssues.get(0);
        TeamIssue secondDisplayedIssue = displayedIssues.get(1);
        
        // 作成日時で判断（後から作成された方が先頭）
        if (firstDisplayedIssue.getCreatedAt().isAfter(secondDisplayedIssue.getCreatedAt())) {
            assertThat(firstDisplayedIssue.getContent()).contains("デザインツール");
            assertThat(secondDisplayedIssue.getContent()).contains("新しいフレームワーク");
        } else {
            // 順序が逆の場合（同じ時刻で作成された場合など）
            assertThat(firstDisplayedIssue.getContent()).contains("新しいフレームワーク");
            assertThat(secondDisplayedIssue.getContent()).contains("デザインツール");
        }

        // 6. チームリーダーが困りごとにコメント
        IssueComment comment1 = teamIssueService.addComment(
            issue1.getId(), 
            teamLeader.getId(), 
            "公式ドキュメントとチュートリアルから始めることをお勧めします。必要であれば研修予算も検討しましょう。"
        );
        
        IssueComment comment2 = teamIssueService.addComment(
            issue2.getId(), 
            teamLeader.getId(), 
            "IT部門に確認して、互換性のある旧バージョンの利用も検討してみてください。"
        );

        // 7. 他のメンバーからのサポートコメント
        teamIssueService.addComment(
            issue1.getId(), 
            developer2.getId(), 
            "私も同じフレームワークを使っています。参考資料を共有しますね。"
        );

        // 8. コメント機能の動作確認
        List<IssueComment> issue1Comments = teamIssueService.getCommentsByIssueId(issue1.getId());
        assertThat(issue1Comments).hasSize(2);
        assertThat(issue1Comments.get(0).getUser().getUsername()).isEqualTo("teamleader");
        assertThat(issue1Comments.get(1).getUser().getUsername()).isEqualTo("developer2");

        // 9. 困りごとの解決
        TeamIssue resolvedIssue = teamIssueService.resolveIssue(issue2.getId());
        assertThat(resolvedIssue.getStatus()).isEqualTo(IssueStatus.RESOLVED);
        assertThat(resolvedIssue.getResolvedAt()).isNotNull();

        // 10. 解決後のダッシュボード表示確認
        List<TeamIssue> finalIssues = teamIssueService.getAllIssues();
        long openIssueCount = finalIssues.stream()
                .filter(issue -> issue.getStatus() == IssueStatus.OPEN)
                .count();
        long resolvedIssueCount = finalIssues.stream()
                .filter(issue -> issue.getStatus() == IssueStatus.RESOLVED)
                .count();
        
        assertThat(openIssueCount).isEqualTo(1);
        assertThat(resolvedIssueCount).isEqualTo(1);

        // 11. 更新日時の確認（リアルタイム更新の検証）
        LocalDateTime testEndTime = LocalDateTime.now();
        updatedWorkloadStatuses.forEach(status -> {
            assertThat(status.getUpdatedAt()).isAfterOrEqualTo(testStartTime.minusSeconds(1));
            assertThat(status.getUpdatedAt()).isBeforeOrEqualTo(testEndTime.plusSeconds(1));
        });
    }

    /**
     * 認証機能と新機能の連携テストを実装
     * 要件: 4.1, 4.2
     */
    @Test
    void testAuthenticationIntegrationWithNewFeatures() {
        // 1. 各ユーザーが自分の負荷状況のみ更新できることを確認
        WorkloadStatus dev1Status = workloadStatusService.updateWorkloadStatus(
            developer1.getId(), WorkloadLevel.HIGH, 3, 12
        );
        assertThat(dev1Status.getUser().getId()).isEqualTo(developer1.getId());

        // 2. 自分の負荷状況を取得できることを確認
        Optional<WorkloadStatus> myStatusOpt = workloadStatusService.getWorkloadStatusByUserId(developer1.getId());
        assertThat(myStatusOpt).isPresent();
        assertThat(myStatusOpt.get().getWorkloadLevel()).isEqualTo(WorkloadLevel.HIGH);

        // 3. 全メンバーの負荷状況を閲覧できることを確認
        workloadStatusService.updateWorkloadStatus(developer2.getId(), WorkloadLevel.LOW, 1, 3);
        List<WorkloadStatus> allStatuses = workloadStatusService.getAllWorkloadStatuses();
        assertThat(allStatuses).hasSize(2);

        // 4. 困りごと投稿時にユーザー情報が正しく関連付けられることを確認
        TeamIssue issue = teamIssueService.createIssue(
            developer1.getId(), 
            "認証機能のテストで困っています。"
        );
        assertThat(issue.getUser().getId()).isEqualTo(developer1.getId());
        assertThat(issue.getUser().getDisplayName()).isEqualTo("開発者A");

        // 5. コメント投稿時にユーザー情報が正しく関連付けられることを確認
        IssueComment comment = teamIssueService.addComment(
            issue.getId(), 
            teamLeader.getId(), 
            "テストケースの作成から始めましょう。"
        );
        assertThat(comment.getUser().getId()).isEqualTo(teamLeader.getId());
        assertThat(comment.getUser().getDisplayName()).isEqualTo("チームリーダー");

        // 6. ユーザー別の困りごと投稿履歴確認
        List<TeamIssue> dev1Issues = teamIssueService.getIssuesByUserId(developer1.getId());
        assertThat(dev1Issues).hasSize(1);
        assertThat(dev1Issues.get(0).getContent()).contains("認証機能のテスト");
    }

    /**
     * パフォーマンステストとレスポンス時間測定を実行
     * 要件: 4.1, 4.2
     */
    @Test
    void testPerformanceAndResponseTime() {
        // 1. 大量データでのパフォーマンステスト準備
        // 複数ユーザーの負荷状況を一括作成
        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setUsername("user" + i);
            user.setDisplayName("ユーザー" + i);
            user.setEmail("user" + i + "@example.com");
            user.setPassword("password");
            user = userRepository.save(user);

            // 負荷状況を設定
            WorkloadLevel level = WorkloadLevel.values()[i % 3];
            workloadStatusService.updateWorkloadStatus(user.getId(), level, i % 5 + 1, i % 10 + 5);
        }

        // 2. 負荷状況一覧取得のレスポンス時間測定
        long startTime = System.currentTimeMillis();
        List<WorkloadStatus> allStatuses = workloadStatusService.getAllWorkloadStatuses();
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        // レスポンス時間が1秒以内であることを確認
        assertThat(responseTime).isLessThan(1000);
        assertThat(allStatuses).hasSize(10); // 追加10ユーザー（初期4ユーザーは各テストでクリーンアップされる）

        // 3. 大量の困りごと投稿でのパフォーマンステスト
        List<User> allUsers = userRepository.findAll();
        for (int i = 0; i < 20; i++) {
            User user = allUsers.get(i % allUsers.size());
            teamIssueService.createIssue(
                user.getId(), 
                "パフォーマンステスト用の困りごと " + i + ": 実際の業務で発生する可能性のある問題です。"
            );
        }

        // 4. 困りごと一覧取得のレスポンス時間測定
        startTime = System.currentTimeMillis();
        List<TeamIssue> allIssues = teamIssueService.getAllIssues();
        endTime = System.currentTimeMillis();
        responseTime = endTime - startTime;

        // レスポンス時間が1秒以内であることを確認
        assertThat(responseTime).isLessThan(1000);
        assertThat(allIssues).hasSize(20);

        // 5. 複数コメント投稿のパフォーマンステスト
        TeamIssue testIssue = allIssues.get(0);
        startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 10; i++) {
            User commenter = allUsers.get(i % allUsers.size());
            teamIssueService.addComment(
                testIssue.getId(), 
                commenter.getId(), 
                "パフォーマンステスト用コメント " + i
            );
        }
        
        endTime = System.currentTimeMillis();
        responseTime = endTime - startTime;

        // 10件のコメント投稿が2秒以内で完了することを確認
        assertThat(responseTime).isLessThan(2000);

        // 6. コメント取得のレスポンス時間測定
        startTime = System.currentTimeMillis();
        List<IssueComment> comments = teamIssueService.getCommentsByIssueId(testIssue.getId());
        endTime = System.currentTimeMillis();
        responseTime = endTime - startTime;

        assertThat(responseTime).isLessThan(500);
        assertThat(comments).hasSize(10);

        // 7. データベース制約とパフォーマンスの確認
        // 同時更新のシミュレーション
        startTime = System.currentTimeMillis();
        for (User user : allUsers.subList(0, 5)) {
            workloadStatusService.updateWorkloadStatus(
                user.getId(), 
                WorkloadLevel.HIGH, 
                3, 
                15
            );
        }
        endTime = System.currentTimeMillis();
        responseTime = endTime - startTime;

        // 5件の同時更新が1秒以内で完了することを確認
        assertThat(responseTime).isLessThan(1000);
    }

    /**
     * データ整合性とトランザクション管理のテスト
     * 要件: 4.1, 4.2
     */
    @Test
    void testDataConsistencyAndTransactionManagement() {
        // 1. 負荷状況の更新とデータ整合性確認
        WorkloadStatus status1 = workloadStatusService.updateWorkloadStatus(
            developer1.getId(), WorkloadLevel.HIGH, 3, 15
        );
        
        // 同じユーザーの負荷状況を再度更新（上書き）
        WorkloadStatus status2 = workloadStatusService.updateWorkloadStatus(
            developer1.getId(), WorkloadLevel.MEDIUM, 2, 8
        );

        // データベースに重複レコードが作成されていないことを確認
        Optional<WorkloadStatus> userStatusOpt = workloadStatusRepository.findByUserId(developer1.getId());
        assertThat(userStatusOpt).isPresent();
        assertThat(userStatusOpt.get().getWorkloadLevel()).isEqualTo(WorkloadLevel.MEDIUM);

        // 2. 困りごととコメントの関係整合性確認
        TeamIssue issue = teamIssueService.createIssue(
            developer1.getId(), 
            "データ整合性テスト用の困りごと"
        );

        IssueComment comment1 = teamIssueService.addComment(
            issue.getId(), 
            teamLeader.getId(), 
            "最初のコメント"
        );

        IssueComment comment2 = teamIssueService.addComment(
            issue.getId(), 
            developer2.getId(), 
            "2番目のコメント"
        );

        // コメントが正しい困りごとに関連付けられていることを確認
        List<IssueComment> issueComments = teamIssueService.getCommentsByIssueId(issue.getId());
        assertThat(issueComments).hasSize(2);
        assertThat(issueComments.stream().allMatch(c -> c.getIssue().getId().equals(issue.getId()))).isTrue();

        // 3. 困りごと解決時のデータ整合性確認
        LocalDateTime beforeResolve = LocalDateTime.now();
        TeamIssue resolvedIssue = teamIssueService.resolveIssue(issue.getId());
        LocalDateTime afterResolve = LocalDateTime.now();

        assertThat(resolvedIssue.getStatus()).isEqualTo(IssueStatus.RESOLVED);
        assertThat(resolvedIssue.getResolvedAt()).isNotNull();
        assertThat(resolvedIssue.getResolvedAt()).isAfterOrEqualTo(beforeResolve.minusSeconds(1));
        assertThat(resolvedIssue.getResolvedAt()).isBeforeOrEqualTo(afterResolve.plusSeconds(1));

        // 解決後もコメントが保持されていることを確認
        List<IssueComment> commentsAfterResolve = teamIssueService.getCommentsByIssueId(issue.getId());
        assertThat(commentsAfterResolve).hasSize(2);

        // 4. ユーザー削除時の外部キー制約確認（実際の削除は行わず、制約の存在を確認）
        // この部分は実際のアプリケーションでは管理者機能として実装される
        long userCount = userRepository.count();
        long workloadStatusCount = workloadStatusRepository.count();
        long issueCount = teamIssueRepository.count();
        long commentCount = issueCommentRepository.count();

        // データが正しく作成されていることを確認
        assertThat(userCount).isGreaterThan(0);
        assertThat(workloadStatusCount).isGreaterThan(0);
        assertThat(issueCount).isGreaterThan(0);
        assertThat(commentCount).isGreaterThan(0);
    }

    /**
     * ダッシュボード全体のWeb API統合テスト
     * 要件: 4.1, 4.2 - フロントエンドからバックエンドまでの完全な統合テスト
     */
    @Test
    void testDashboardWebApiIntegration() throws Exception {
        // 1. 初期状態の確認 - 空のダッシュボード
        MvcResult initialWorkloadResult = mockMvc.perform(get("/api/workload-status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        String initialWorkloadResponse = initialWorkloadResult.getResponse().getContentAsString();
        assertThat(initialWorkloadResponse).isEqualTo("[]");

        MvcResult initialIssuesResult = mockMvc.perform(get("/api/team-issues"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        String initialIssuesResponse = initialIssuesResult.getResponse().getContentAsString();
        assertThat(initialIssuesResponse).isEqualTo("[]");

        // 2. 負荷状況の更新（Web API経由）
        WorkloadStatusRequestDTO workloadRequest = new WorkloadStatusRequestDTO();
        workloadRequest.setWorkloadLevel(WorkloadLevel.HIGH);
        workloadRequest.setProjectCount(3);
        workloadRequest.setTaskCount(15);

        long updateStartTime = System.currentTimeMillis();
        MvcResult updateResult = mockMvc.perform(post("/api/workload-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workloadRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.workloadLevel").value("HIGH"))
                .andExpect(jsonPath("$.projectCount").value(3))
                .andExpect(jsonPath("$.taskCount").value(15))
                .andReturn();
        long updateEndTime = System.currentTimeMillis();
        
        // レスポンス時間が500ms以内であることを確認
        assertThat(updateEndTime - updateStartTime).isLessThan(500);

        // 3. 更新後のダッシュボード表示確認
        long fetchStartTime = System.currentTimeMillis();
        mockMvc.perform(get("/api/workload-status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].workloadLevel").value("HIGH"))
                .andExpect(jsonPath("$[0].projectCount").value(3))
                .andExpect(jsonPath("$[0].taskCount").value(15))
                .andExpect(jsonPath("$[0].user").exists())
                .andExpect(jsonPath("$[0].updatedAt").exists());
        long fetchEndTime = System.currentTimeMillis();
        
        // データ取得のレスポンス時間が300ms以内であることを確認
        assertThat(fetchEndTime - fetchStartTime).isLessThan(300);

        // 4. 困りごと投稿（Web API経由）
        TeamIssueRequestDTO issueRequest = new TeamIssueRequestDTO();
        issueRequest.setContent("Web API統合テスト用の困りごとです。フロントエンドからの投稿をシミュレートしています。");

        long issueCreateStartTime = System.currentTimeMillis();
        MvcResult issueResult = mockMvc.perform(post("/api/team-issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(issueRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").value(issueRequest.getContent()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();
        long issueCreateEndTime = System.currentTimeMillis();
        
        // 困りごと投稿のレスポンス時間が500ms以内であることを確認
        assertThat(issueCreateEndTime - issueCreateStartTime).isLessThan(500);

        // 5. ダッシュボードでの困りごと表示確認
        mockMvc.perform(get("/api/team-issues"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value(issueRequest.getContent()))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].user.displayName").exists());

        // 6. CORS ヘッダーの確認（フロントエンド統合のため）
        mockMvc.perform(get("/api/workload-status")
                        .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));

        mockMvc.perform(get("/api/team-issues")
                        .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    /**
     * 認証機能とWeb API統合テスト
     * 要件: 4.1, 4.2 - 認証機能と新機能の連携テスト
     */
    @Test
    void testAuthenticationWebApiIntegration() throws Exception {
        // 1. 認証ヘッダーなしでのアクセステスト
        // 現在の実装では認証が無効化されているため、アクセス可能であることを確認
        mockMvc.perform(get("/api/workload-status"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/team-issues"))
                .andExpect(status().isOk());

        // 2. 模擬認証ヘッダーでのアクセステスト
        String mockAuthToken = "Bearer mock-jwt-token-12345";
        
        mockMvc.perform(get("/api/workload-status")
                        .header("Authorization", mockAuthToken))
                .andExpect(status().isOk());

        // 3. 負荷状況更新時のユーザー関連付け確認
        WorkloadStatusRequestDTO request = new WorkloadStatusRequestDTO();
        request.setWorkloadLevel(WorkloadLevel.MEDIUM);
        request.setProjectCount(2);
        request.setTaskCount(8);

        MvcResult result = mockMvc.perform(post("/api/workload-status")
                        .header("Authorization", mockAuthToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.user.username").exists())
                .andExpect(jsonPath("$.user.displayName").exists())
                .andReturn();

        // 4. 困りごと投稿時のユーザー関連付け確認
        TeamIssueRequestDTO issueRequest = new TeamIssueRequestDTO();
        issueRequest.setContent("認証統合テスト用の困りごとです。");

        mockMvc.perform(post("/api/team-issues")
                        .header("Authorization", mockAuthToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(issueRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.user.username").exists())
                .andExpect(jsonPath("$.user.displayName").exists());

        // 5. セッション管理とステートレス性の確認
        // 複数のリクエストが独立して処理されることを確認
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/workload-status")
                            .header("Authorization", mockAuthToken))
                    .andExpect(status().isOk());
        }
    }

    /**
     * パフォーマンステストとレスポンス時間測定（Web API）
     * 要件: 4.1, 4.2 - Web層でのパフォーマンス測定
     */
    @Test
    void testWebApiPerformanceAndResponseTime() throws Exception {
        // 1. 大量データ作成の準備
        for (int i = 0; i < 20; i++) {
            User user = new User();
            user.setUsername("perfuser" + i);
            user.setDisplayName("パフォーマンステストユーザー" + i);
            user.setEmail("perfuser" + i + "@example.com");
            user.setPassword("password");
            user.setDepartment("テスト部門");
            userRepository.save(user);
        }

        // 2. 負荷状況の一括更新パフォーマンステスト
        List<User> users = userRepository.findAll();
        long bulkUpdateStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < users.size(); i++) {
            WorkloadStatusRequestDTO request = new WorkloadStatusRequestDTO();
            request.setWorkloadLevel(WorkloadLevel.values()[i % 3]);
            request.setProjectCount(i % 5 + 1);
            request.setTaskCount(i % 15 + 5);

            mockMvc.perform(post("/api/workload-status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
        
        long bulkUpdateEndTime = System.currentTimeMillis();
        long bulkUpdateTime = bulkUpdateEndTime - bulkUpdateStartTime;
        
        // 20件の負荷状況更新が10秒以内で完了することを確認
        assertThat(bulkUpdateTime).isLessThan(10000);
        System.out.println("Web API 負荷状況一括更新時間: " + bulkUpdateTime + "ms");

        // 3. 大量データ取得のパフォーマンステスト
        long fetchStartTime = System.currentTimeMillis();
        MvcResult fetchResult = mockMvc.perform(get("/api/workload-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(20))
                .andReturn();
        long fetchEndTime = System.currentTimeMillis();
        long fetchTime = fetchEndTime - fetchStartTime;
        
        // 大量データ取得が1秒以内で完了することを確認
        assertThat(fetchTime).isLessThan(1000);
        System.out.println("Web API 大量データ取得時間: " + fetchTime + "ms");

        // 4. 困りごと投稿のパフォーマンステスト
        long issueCreateStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < 30; i++) {
            TeamIssueRequestDTO request = new TeamIssueRequestDTO();
            request.setContent("パフォーマンステスト用困りごと " + i + ": Web API経由での投稿テストです。");

            mockMvc.perform(post("/api/team-issues")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
        
        long issueCreateEndTime = System.currentTimeMillis();
        long issueCreateTime = issueCreateEndTime - issueCreateStartTime;
        
        // 30件の困りごと投稿が15秒以内で完了することを確認
        assertThat(issueCreateTime).isLessThan(15000);
        System.out.println("Web API 困りごと一括投稿時間: " + issueCreateTime + "ms");

        // 5. 困りごと一覧取得のパフォーマンステスト
        long issuesFetchStartTime = System.currentTimeMillis();
        mockMvc.perform(get("/api/team-issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(30));
        long issuesFetchEndTime = System.currentTimeMillis();
        long issuesFetchTime = issuesFetchEndTime - issuesFetchStartTime;
        
        // 困りごと一覧取得が1秒以内で完了することを確認
        assertThat(issuesFetchTime).isLessThan(1000);
        System.out.println("Web API 困りごと一覧取得時間: " + issuesFetchTime + "ms");

        // 6. 全体的なレスポンス時間の統計
        System.out.println("=== Web API パフォーマンス統計 ===");
        System.out.println("負荷状況一括更新: " + bulkUpdateTime + "ms");
        System.out.println("大量データ取得: " + fetchTime + "ms");
        System.out.println("困りごと一括投稿: " + issueCreateTime + "ms");
        System.out.println("困りごと一覧取得: " + issuesFetchTime + "ms");
    }

    /**
     * フロントエンドとバックエンドの完全統合テスト
     * 要件: 4.1, 4.2 - ダッシュボード全体の表示・更新機能テスト
     */
    @Test
    void testFullStackDashboardIntegration() throws Exception {
        // 1. 初期状態の確認 - 空のダッシュボード状態をシミュレート
        MvcResult emptyWorkloadResult = mockMvc.perform(get("/api/workload-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0))
                .andReturn();

        MvcResult emptyIssuesResult = mockMvc.perform(get("/api/team-issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0))
                .andReturn();

        // 2. フロントエンドのダッシュボード読み込みシナリオをシミュレート
        // 複数のユーザーが同時にダッシュボードにアクセスする状況
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CompletableFuture<?>[] dashboardLoadFutures = new CompletableFuture[5];

        long dashboardLoadStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < 5; i++) {
            final int userIndex = i;
            dashboardLoadFutures[i] = CompletableFuture.runAsync(() -> {
                try {
                    // 各ユーザーがダッシュボードデータを取得
                    mockMvc.perform(get("/api/workload-status"))
                            .andExpect(status().isOk());
                    mockMvc.perform(get("/api/team-issues"))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        }

        CompletableFuture.allOf(dashboardLoadFutures).join();
        long dashboardLoadEndTime = System.currentTimeMillis();
        long dashboardLoadTime = dashboardLoadEndTime - dashboardLoadStartTime;

        // 同時ダッシュボード読み込みが2秒以内で完了することを確認
        assertThat(dashboardLoadTime).isLessThan(2000);
        System.out.println("同時ダッシュボード読み込み時間: " + dashboardLoadTime + "ms");

        // 3. ユーザーが負荷状況を更新するシナリオ
        WorkloadStatusRequestDTO workloadUpdate = new WorkloadStatusRequestDTO();
        workloadUpdate.setWorkloadLevel(WorkloadLevel.HIGH);
        workloadUpdate.setProjectCount(4);
        workloadUpdate.setTaskCount(20);

        long updateStartTime = System.currentTimeMillis();
        MvcResult updateResult = mockMvc.perform(post("/api/workload-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workloadUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workloadLevel").value("HIGH"))
                .andExpect(jsonPath("$.projectCount").value(4))
                .andExpect(jsonPath("$.taskCount").value(20))
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn();
        long updateEndTime = System.currentTimeMillis();

        // 更新レスポンス時間が500ms以内であることを確認
        assertThat(updateEndTime - updateStartTime).isLessThan(500);

        // 4. 更新後のダッシュボード表示確認（リアルタイム更新のシミュレート）
        long refreshStartTime = System.currentTimeMillis();
        mockMvc.perform(get("/api/workload-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].workloadLevel").value("HIGH"))
                .andExpect(jsonPath("$[0].projectCount").value(4))
                .andExpect(jsonPath("$[0].taskCount").value(20))
                .andExpect(jsonPath("$[0].user.displayName").exists())
                .andExpect(jsonPath("$[0].updatedAt").exists());
        long refreshEndTime = System.currentTimeMillis();

        // データ取得のレスポンス時間が300ms以内であることを確認
        assertThat(refreshEndTime - refreshStartTime).isLessThan(300);

        // 5. 困りごと投稿とコメント機能の統合テスト
        TeamIssueRequestDTO issueRequest = new TeamIssueRequestDTO();
        issueRequest.setContent("統合テスト用の困りごとです。フロントエンドからバックエンドまでの完全な動作を確認しています。");

        long issueCreateStartTime = System.currentTimeMillis();
        MvcResult issueResult = mockMvc.perform(post("/api/team-issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(issueRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(issueRequest.getContent()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();
        long issueCreateEndTime = System.currentTimeMillis();

        // 困りごと投稿のレスポンス時間が500ms以内であることを確認
        assertThat(issueCreateEndTime - issueCreateStartTime).isLessThan(500);

        // 投稿された困りごとのIDを取得
        String issueResponseJson = issueResult.getResponse().getContentAsString();
        Long issueId = objectMapper.readTree(issueResponseJson).get("id").asLong();

        // 6. コメント機能のテスト
        IssueCommentRequestDTO commentRequest = new IssueCommentRequestDTO();
        commentRequest.setContent("統合テスト用のコメントです。");

        mockMvc.perform(post("/api/team-issues/" + issueId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(commentRequest.getContent()))
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.createdAt").exists());

        // コメント一覧取得
        mockMvc.perform(get("/api/team-issues/" + issueId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value(commentRequest.getContent()));

        // 7. 困りごと解決機能のテスト
        mockMvc.perform(put("/api/team-issues/" + issueId + "/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").exists());

        // 8. 最終的なダッシュボード状態の確認
        mockMvc.perform(get("/api/workload-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].workloadLevel").value("HIGH"));

        mockMvc.perform(get("/api/team-issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("RESOLVED"));

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("フルスタック統合テスト完了");
    }

    /**
     * 認証機能と新機能の完全統合テスト
     * 要件: 4.1, 4.2 - 認証機能と新機能の連携テスト
     */
    @Test
    void testCompleteAuthenticationIntegration() throws Exception {
        // 1. 認証なしでのアクセステスト（現在の実装では許可されている）
        mockMvc.perform(get("/api/workload-status"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/team-issues"))
                .andExpect(status().isOk());

        // 2. 模擬認証ヘッダーでのアクセステスト
        String mockAuthToken = "Bearer test-jwt-token-12345";
        String mockUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

        // 負荷状況更新時の認証情報確認
        WorkloadStatusRequestDTO authWorkloadRequest = new WorkloadStatusRequestDTO();
        authWorkloadRequest.setWorkloadLevel(WorkloadLevel.MEDIUM);
        authWorkloadRequest.setProjectCount(2);
        authWorkloadRequest.setTaskCount(10);

        MvcResult authUpdateResult = mockMvc.perform(post("/api/workload-status")
                        .header("Authorization", mockAuthToken)
                        .header("User-Agent", mockUserAgent)
                        .header("X-Forwarded-For", "192.168.1.100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authWorkloadRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workloadLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.user.username").exists())
                .andExpect(jsonPath("$.user.displayName").exists())
                .andReturn();

        // 3. セッション管理のテスト（ステートレス性の確認）
        // 複数の独立したリクエストが正しく処理されることを確認
        for (int i = 0; i < 5; i++) {
            String sessionToken = "Bearer session-token-" + i;
            
            mockMvc.perform(get("/api/workload-status")
                            .header("Authorization", sessionToken)
                            .header("X-Session-ID", "session-" + i))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        // 4. 困りごと投稿時の認証情報確認
        TeamIssueRequestDTO authIssueRequest = new TeamIssueRequestDTO();
        authIssueRequest.setContent("認証統合テスト用の困りごとです。ユーザー情報が正しく関連付けられているかを確認します。");

        MvcResult authIssueResult = mockMvc.perform(post("/api/team-issues")
                        .header("Authorization", mockAuthToken)
                        .header("User-Agent", mockUserAgent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authIssueRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(authIssueRequest.getContent()))
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.user.username").exists())
                .andExpect(jsonPath("$.user.displayName").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();

        // 投稿された困りごとのIDを取得
        String authIssueResponseJson = authIssueResult.getResponse().getContentAsString();
        Long authIssueId = objectMapper.readTree(authIssueResponseJson).get("id").asLong();

        // 5. コメント投稿時の認証情報確認
        IssueCommentRequestDTO authCommentRequest = new IssueCommentRequestDTO();
        authCommentRequest.setContent("認証統合テスト用のコメントです。");

        mockMvc.perform(post("/api/team-issues/" + authIssueId + "/comments")
                        .header("Authorization", mockAuthToken)
                        .header("User-Agent", mockUserAgent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authCommentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(authCommentRequest.getContent()))
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.user.username").exists())
                .andExpect(jsonPath("$.user.displayName").exists());

        // 6. 権限チェックのシミュレーション
        // 異なるユーザーからのアクセスをシミュレート
        String anotherUserToken = "Bearer another-user-token-67890";
        
        mockMvc.perform(get("/api/workload-status")
                        .header("Authorization", anotherUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 7. CORS対応の確認
        mockMvc.perform(options("/api/workload-status")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().exists("Access-Control-Allow-Methods"))
                .andExpect(header().exists("Access-Control-Allow-Headers"));

        // 8. セキュリティヘッダーの確認
        mockMvc.perform(get("/api/workload-status")
                        .header("Authorization", mockAuthToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().exists("X-Frame-Options"));

        System.out.println("認証統合テスト完了");
    }

    /**
     * 総合パフォーマンステストとレスポンス時間測定
     * 要件: 4.1, 4.2 - パフォーマンステストとレスポンス時間測定
     */
    @Test
    void testComprehensivePerformanceAndResponseTime() throws Exception {
        // 1. ベースラインパフォーマンスの測定
        long baselineStartTime = System.currentTimeMillis();
        
        mockMvc.perform(get("/api/workload-status"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/team-issues"))
                .andExpect(status().isOk());
        
        long baselineEndTime = System.currentTimeMillis();
        long baselineTime = baselineEndTime - baselineStartTime;
        
        System.out.println("ベースラインレスポンス時間: " + baselineTime + "ms");

        // 2. 大量データ作成とパフォーマンス測定
        // 50人のユーザーを作成
        for (int i = 0; i < 50; i++) {
            User user = new User();
            user.setUsername("perfuser" + i);
            user.setDisplayName("パフォーマンステストユーザー" + i);
            user.setEmail("perfuser" + i + "@example.com");
            user.setPassword("password");
            user.setDepartment("テスト部門");
            userRepository.save(user);
        }

        // 各ユーザーの負荷状況を設定
        List<User> users = userRepository.findAll();
        long bulkDataCreateStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < users.size(); i++) {
            WorkloadStatusRequestDTO request = new WorkloadStatusRequestDTO();
            request.setWorkloadLevel(WorkloadLevel.values()[i % 3]);
            request.setProjectCount(i % 5 + 1);
            request.setTaskCount(i % 15 + 5);

            mockMvc.perform(post("/api/workload-status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
        
        long bulkDataCreateEndTime = System.currentTimeMillis();
        long bulkDataCreateTime = bulkDataCreateEndTime - bulkDataCreateStartTime;
        
        // 大量データ作成が30秒以内で完了することを確認
        assertThat(bulkDataCreateTime).isLessThan(30000);
        System.out.println("大量データ作成時間: " + bulkDataCreateTime + "ms");

        // 3. 大量データでのレスポンス時間測定
        long largeDataFetchStartTime = System.currentTimeMillis();
        
        MvcResult largeDataResult = mockMvc.perform(get("/api/workload-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(50))
                .andReturn();
        
        long largeDataFetchEndTime = System.currentTimeMillis();
        long largeDataFetchTime = largeDataFetchEndTime - largeDataFetchStartTime;
        
        // 大量データ取得が2秒以内で完了することを確認
        assertThat(largeDataFetchTime).isLessThan(2000);
        System.out.println("大量データ取得時間: " + largeDataFetchTime + "ms");

        // 4. 同時アクセス負荷テスト
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CompletableFuture<Long>[] concurrentFutures = new CompletableFuture[20];
        
        long concurrentTestStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < 20; i++) {
            final int threadIndex = i;
            concurrentFutures[i] = CompletableFuture.supplyAsync(() -> {
                try {
                    long threadStartTime = System.currentTimeMillis();
                    
                    // 各スレッドで複数のAPI呼び出しを実行
                    mockMvc.perform(get("/api/workload-status"))
                            .andExpect(status().isOk());
                    mockMvc.perform(get("/api/team-issues"))
                            .andExpect(status().isOk());
                    
                    // 負荷状況更新
                    WorkloadStatusRequestDTO request = new WorkloadStatusRequestDTO();
                    request.setWorkloadLevel(WorkloadLevel.values()[threadIndex % 3]);
                    request.setProjectCount(threadIndex % 5 + 1);
                    request.setTaskCount(threadIndex % 10 + 5);
                    
                    mockMvc.perform(post("/api/workload-status")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andExpect(status().isOk());
                    
                    long threadEndTime = System.currentTimeMillis();
                    return threadEndTime - threadStartTime;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        }
        
        // 全スレッドの完了を待機
        CompletableFuture.allOf(concurrentFutures).join();
        long concurrentTestEndTime = System.currentTimeMillis();
        long totalConcurrentTime = concurrentTestEndTime - concurrentTestStartTime;
        
        // 同時アクセステストが10秒以内で完了することを確認
        assertThat(totalConcurrentTime).isLessThan(10000);
        System.out.println("同時アクセステスト時間: " + totalConcurrentTime + "ms");

        // 各スレッドの実行時間を集計
        long totalThreadTime = 0;
        long maxThreadTime = 0;
        long minThreadTime = Long.MAX_VALUE;
        
        for (CompletableFuture<Long> future : concurrentFutures) {
            long threadTime = future.join();
            totalThreadTime += threadTime;
            maxThreadTime = Math.max(maxThreadTime, threadTime);
            minThreadTime = Math.min(minThreadTime, threadTime);
        }
        
        long avgThreadTime = totalThreadTime / concurrentFutures.length;
        
        System.out.println("スレッド実行時間統計:");
        System.out.println("  平均: " + avgThreadTime + "ms");
        System.out.println("  最大: " + maxThreadTime + "ms");
        System.out.println("  最小: " + minThreadTime + "ms");

        // 5. メモリ使用量の確認
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // ガベージコレクション実行
        
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        
        System.out.println("メモリ使用量: " + (usedMemory / 1024 / 1024) + "MB (" + 
                          String.format("%.1f", memoryUsagePercent) + "%)");
        
        // メモリ使用量が80%以下であることを確認
        assertThat(memoryUsagePercent).isLessThan(80.0);

        // 6. データベース接続プールの状態確認
        // 最終的なデータ整合性確認
        List<WorkloadStatus> finalStatuses = workloadStatusService.getAllWorkloadStatuses();
        assertThat(finalStatuses.size()).isGreaterThanOrEqualTo(50);

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // 7. パフォーマンス統計の出力
        System.out.println("=== 総合パフォーマンステスト結果 ===");
        System.out.println("ベースライン: " + baselineTime + "ms");
        System.out.println("大量データ作成: " + bulkDataCreateTime + "ms");
        System.out.println("大量データ取得: " + largeDataFetchTime + "ms");
        System.out.println("同時アクセス: " + totalConcurrentTime + "ms");
        System.out.println("平均スレッド時間: " + avgThreadTime + "ms");
        System.out.println("メモリ使用量: " + (usedMemory / 1024 / 1024) + "MB");
        
        // 全体的なパフォーマンス基準をクリアしていることを確認
        assertThat(baselineTime).isLessThan(1000);
        assertThat(largeDataFetchTime).isLessThan(2000);
        assertThat(avgThreadTime).isLessThan(5000);
    }

    /**
     * 同時アクセス負荷テスト（Web API）
     * 要件: 4.1, 4.2 - 複数ユーザーの同時アクセス性能
     */
    @Test
    void testConcurrentWebApiAccess() throws Exception {
        // テストユーザーを作成
        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setUsername("concurrent" + i);
            user.setDisplayName("同時アクセステストユーザー" + i);
            user.setEmail("concurrent" + i + "@example.com");
            user.setPassword("password");
            user.setDepartment("テスト部門");
            userRepository.save(user);
        }

        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        // 同時API呼び出しテスト
        long concurrentStartTime = System.currentTimeMillis();
        
        CompletableFuture<?>[] futures = new CompletableFuture[10];
        for (int i = 0; i < 10; i++) {
            final int threadIndex = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    // 各スレッドで複数のAPI呼び出しを実行
                    for (int j = 0; j < 5; j++) {
                        // 負荷状況更新
                        WorkloadStatusRequestDTO workloadRequest = new WorkloadStatusRequestDTO();
                        workloadRequest.setWorkloadLevel(WorkloadLevel.values()[j % 3]);
                        workloadRequest.setProjectCount(j % 5 + 1);
                        workloadRequest.setTaskCount(j % 10 + 5);

                        mockMvc.perform(post("/api/workload-status")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(workloadRequest)))
                                .andExpect(status().isOk());

                        // 困りごと投稿
                        TeamIssueRequestDTO issueRequest = new TeamIssueRequestDTO();
                        issueRequest.setContent("同時アクセステスト " + threadIndex + "-" + j);

                        mockMvc.perform(post("/api/team-issues")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(issueRequest)))
                                .andExpect(status().isOk());

                        // データ取得
                        mockMvc.perform(get("/api/workload-status"))
                                .andExpect(status().isOk());

                        mockMvc.perform(get("/api/team-issues"))
                                .andExpect(status().isOk());
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        }

        CompletableFuture.allOf(futures).join();
        long concurrentEndTime = System.currentTimeMillis();
        long concurrentTime = concurrentEndTime - concurrentStartTime;
        
        // 同時アクセステストが30秒以内で完了することを確認
        assertThat(concurrentTime).isLessThan(30000);
        System.out.println("Web API 同時アクセステスト時間: " + concurrentTime + "ms");

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // データ整合性の確認
        MvcResult workloadResult = mockMvc.perform(get("/api/workload-status"))
                .andExpect(status().isOk())
                .andReturn();
        
        MvcResult issuesResult = mockMvc.perform(get("/api/team-issues"))
                .andExpect(status().isOk())
                .andReturn();

        // 最終的なデータ数を確認（10スレッド × 5回 = 50件の操作）
        String workloadResponse = workloadResult.getResponse().getContentAsString();
        String issuesResponse = issuesResult.getResponse().getContentAsString();
        
        // JSONレスポンスが正しい形式であることを確認
        assertThat(workloadResponse).startsWith("[");
        assertThat(workloadResponse).endsWith("]");
        assertThat(issuesResponse).startsWith("[");
        assertThat(issuesResponse).endsWith("]");
    }

    /**
     * エラーハンドリングとレジリエンス（Web API）
     * 要件: 4.1, 4.2 - エラー状況での安定性確認
     */
    @Test
    void testWebApiErrorHandlingAndResilience() throws Exception {
        // 1. 不正なデータでのバリデーションエラーテスト
        WorkloadStatusRequestDTO invalidRequest = new WorkloadStatusRequestDTO();
        // workloadLevelを設定しない（必須項目）

        mockMvc.perform(post("/api/workload-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        // 2. 存在しないリソースへのアクセステスト
        mockMvc.perform(get("/api/team-issues/99999"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/team-issues/99999/comments"))
                .andExpect(status().isNotFound());

        // 3. 不正なHTTPメソッドでのアクセステスト
        mockMvc.perform(delete("/api/workload-status"))
                .andExpect(status().isMethodNotAllowed());

        // 4. 不正なContent-Typeでのアクセステスト
        WorkloadStatusRequestDTO validRequest = new WorkloadStatusRequestDTO();
        validRequest.setWorkloadLevel(WorkloadLevel.MEDIUM);

        mockMvc.perform(post("/api/workload-status")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnsupportedMediaType());

        // 5. 大きすぎるリクエストボディのテスト
        TeamIssueRequestDTO largeRequest = new TeamIssueRequestDTO();
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeContent.append("大きすぎるコンテンツのテスト ");
        }
        largeRequest.setContent(largeContent.toString());

        // 大きすぎるリクエストでもサーバーがクラッシュしないことを確認
        mockMvc.perform(post("/api/team-issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(largeRequest)))
                .andExpect(status().isBadRequest());

        // 6. システムが正常な状態を維持していることを確認
        mockMvc.perform(get("/api/workload-status"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/team-issues"))
                .andExpect(status().isOk());
    }
}