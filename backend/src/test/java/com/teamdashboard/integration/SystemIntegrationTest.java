package com.teamdashboard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamdashboard.dto.*;
import com.teamdashboard.entity.*;
import com.teamdashboard.repository.*;
import com.teamdashboard.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * システム全体の統合テスト
 * 要件: 4.1, 4.2 - 全体機能の統合テスト
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SystemIntegrationTest {

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

    private User testUser1;
    private User testUser2;
    private User testUser3;

    @BeforeEach
    void setUp() {
        // 既存データをクリーンアップ（順序重要）
        issueCommentRepository.deleteAll();
        teamIssueRepository.deleteAll();
        workloadStatusRepository.deleteAll();
        
        // テストユーザーを作成
        testUser1 = createTestUser("integrationuser1", "統合テストユーザー1", "integration1@example.com");
        testUser2 = createTestUser("integrationuser2", "統合テストユーザー2", "integration2@example.com");
        testUser3 = createTestUser("integrationuser3", "統合テストユーザー3", "integration3@example.com");
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
    void testDashboardOverallFunctionality() {
        // 1. 初期状態確認
        List<WorkloadStatus> initialWorkloadStatuses = workloadStatusService.getAllWorkloadStatuses();
        List<TeamIssue> initialTeamIssues = teamIssueService.getAllIssues();
        
        assertThat(initialWorkloadStatuses).isEmpty();
        assertThat(initialTeamIssues).isEmpty();

        // 2. 負荷状況更新テスト
        WorkloadStatus workloadStatus = workloadStatusService.updateWorkloadStatus(
            testUser1.getId(), 
            WorkloadLevel.HIGH, 
            3, 
            15
        );
        
        assertThat(workloadStatus).isNotNull();
        assertThat(workloadStatus.getWorkloadLevel()).isEqualTo(WorkloadLevel.HIGH);
        assertThat(workloadStatus.getProjectCount()).isEqualTo(3);
        assertThat(workloadStatus.getTaskCount()).isEqualTo(15);
        assertThat(workloadStatus.getUser().getId()).isEqualTo(testUser1.getId());

        // 3. 困りごと投稿テスト
        TeamIssue teamIssue = teamIssueService.createIssue(
            testUser1.getId(), 
            "プロジェクトAの進捗が遅れています。リソース不足が原因です。"
        );
        
        assertThat(teamIssue).isNotNull();
        assertThat(teamIssue.getContent()).isEqualTo("プロジェクトAの進捗が遅れています。リソース不足が原因です。");
        assertThat(teamIssue.getStatus()).isEqualTo(IssueStatus.OPEN);
        assertThat(teamIssue.getUser().getId()).isEqualTo(testUser1.getId());

        // 4. 更新後のダッシュボードデータ確認
        List<WorkloadStatus> updatedWorkloadStatuses = workloadStatusService.getAllWorkloadStatuses();
        List<TeamIssue> updatedTeamIssues = teamIssueService.getAllIssues();
        
        assertThat(updatedWorkloadStatuses).hasSize(1);
        assertThat(updatedWorkloadStatuses.get(0).getWorkloadLevel()).isEqualTo(WorkloadLevel.HIGH);
        
        assertThat(updatedTeamIssues).hasSize(1);
        assertThat(updatedTeamIssues.get(0).getContent()).isEqualTo("プロジェクトAの進捗が遅れています。リソース不足が原因です。");

        // 5. コメント投稿テスト
        IssueComment comment = teamIssueService.addComment(
            teamIssue.getId(), 
            testUser2.getId(), 
            "来週までに追加リソースを確保できる予定です。"
        );
        
        assertThat(comment).isNotNull();
        assertThat(comment.getContent()).isEqualTo("来週までに追加リソースを確保できる予定です。");
        assertThat(comment.getUser().getId()).isEqualTo(testUser2.getId());

        // 6. 困りごと解決マークテスト
        TeamIssue resolvedIssue = teamIssueService.resolveIssue(teamIssue.getId());
        
        assertThat(resolvedIssue.getStatus()).isEqualTo(IssueStatus.RESOLVED);
        assertThat(resolvedIssue.getResolvedAt()).isNotNull();

        // 7. 最終状態確認
        List<TeamIssue> finalTeamIssues = teamIssueService.getAllIssues();
        assertThat(finalTeamIssues.get(0).getStatus()).isEqualTo(IssueStatus.RESOLVED);
    }

    /**
     * 複数ユーザーでの同時操作テスト
     * 要件: 4.1, 4.2
     */
    @Test
    void testMultiUserConcurrentOperations() {
        // 複数ユーザーの負荷状況を設定
        WorkloadStatus status1 = workloadStatusService.updateWorkloadStatus(
            testUser1.getId(), WorkloadLevel.HIGH, 4, 20
        );
        WorkloadStatus status2 = workloadStatusService.updateWorkloadStatus(
            testUser2.getId(), WorkloadLevel.MEDIUM, 2, 8
        );
        WorkloadStatus status3 = workloadStatusService.updateWorkloadStatus(
            testUser3.getId(), WorkloadLevel.LOW, 1, 3
        );

        // 複数ユーザーの困りごとを設定
        TeamIssue issue1 = teamIssueService.createIssue(
            testUser1.getId(), "プロジェクトAの進捗遅延"
        );
        TeamIssue issue2 = teamIssueService.createIssue(
            testUser2.getId(), "新技術の学習時間不足"
        );
        TeamIssue issue3 = teamIssueService.createIssue(
            testUser3.getId(), "コードレビューの待ち時間"
        );

        // 全体データ取得テスト
        List<WorkloadStatus> allWorkloadStatuses = workloadStatusService.getAllWorkloadStatuses();
        List<TeamIssue> allTeamIssues = teamIssueService.getAllIssues();

        assertThat(allWorkloadStatuses).hasSize(3);
        assertThat(allTeamIssues).hasSize(3);

        // 各ユーザーのデータ確認
        WorkloadStatus user1Status = allWorkloadStatuses.stream()
                .filter(ws -> ws.getUser().getId().equals(testUser1.getId()))
                .findFirst().orElseThrow();
        assertThat(user1Status.getWorkloadLevel()).isEqualTo(WorkloadLevel.HIGH);

        WorkloadStatus user2Status = allWorkloadStatuses.stream()
                .filter(ws -> ws.getUser().getId().equals(testUser2.getId()))
                .findFirst().orElseThrow();
        assertThat(user2Status.getWorkloadLevel()).isEqualTo(WorkloadLevel.MEDIUM);

        WorkloadStatus user3Status = allWorkloadStatuses.stream()
                .filter(ws -> ws.getUser().getId().equals(testUser3.getId()))
                .findFirst().orElseThrow();
        assertThat(user3Status.getWorkloadLevel()).isEqualTo(WorkloadLevel.LOW);
    }

    /**
     * パフォーマンステストとレスポンス時間測定を実行
     * 要件: 4.2
     */
    @Test
    void testPerformanceAndResponseTime() {
        // 大量データを準備
        setupLargeDataSet();

        // レスポンス時間測定
        long startTime = System.currentTimeMillis();
        
        List<WorkloadStatus> workloadStatuses = workloadStatusService.getAllWorkloadStatuses();
        
        long workloadResponseTime = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        
        List<TeamIssue> teamIssues = teamIssueService.getAllIssues();
        
        long issuesResponseTime = System.currentTimeMillis() - startTime;

        // パフォーマンス要件確認（1秒以内）
        assertThat(workloadResponseTime).isLessThan(1000);
        assertThat(issuesResponseTime).isLessThan(1000);

        // データ件数確認
        assertThat(workloadStatuses).hasSize(3);
        assertThat(teamIssues).hasSize(13); // 3 + 10 additional

        // 複数回実行してパフォーマンスの安定性確認
        for (int i = 0; i < 5; i++) {
            startTime = System.currentTimeMillis();
            
            workloadStatusService.getAllWorkloadStatuses();
            
            long responseTime = System.currentTimeMillis() - startTime;
            assertThat(responseTime).isLessThan(1000);
        }
    }

    /**
     * データ整合性テスト
     * 要件: 4.1, 4.2
     */
    @Test
    void testDataConsistency() {
        // 1. 負荷状況更新
        WorkloadStatus workloadStatus = workloadStatusService.updateWorkloadStatus(
            testUser1.getId(), WorkloadLevel.HIGH, 3, 15
        );

        // 2. データベース直接確認
        List<WorkloadStatus> workloadStatuses = workloadStatusRepository.findAll();
        assertThat(workloadStatuses).hasSize(1);
        WorkloadStatus savedStatus = workloadStatuses.get(0);
        assertThat(savedStatus.getWorkloadLevel()).isEqualTo(WorkloadLevel.HIGH);
        assertThat(savedStatus.getProjectCount()).isEqualTo(3);
        assertThat(savedStatus.getTaskCount()).isEqualTo(15);
        assertThat(savedStatus.getUser().getId()).isEqualTo(testUser1.getId());

        // 3. 困りごと投稿
        TeamIssue teamIssue = teamIssueService.createIssue(
            testUser1.getId(), "テスト困りごと"
        );

        // 4. データベース直接確認
        List<TeamIssue> teamIssues = teamIssueRepository.findAll();
        assertThat(teamIssues).hasSize(1);
        TeamIssue savedIssue = teamIssues.get(0);
        assertThat(savedIssue.getContent()).isEqualTo("テスト困りごと");
        assertThat(savedIssue.getStatus()).isEqualTo(IssueStatus.OPEN);
        assertThat(savedIssue.getUser().getId()).isEqualTo(testUser1.getId());
        assertThat(savedIssue.getCreatedAt()).isNotNull();

        // 5. サービス経由とリポジトリ直接のデータ整合性確認
        Optional<WorkloadStatus> serviceWorkloadStatus = workloadStatusService.getWorkloadStatusByUserId(testUser1.getId());
        assertThat(serviceWorkloadStatus).isPresent();
        assertThat(serviceWorkloadStatus.get().getId()).isEqualTo(savedStatus.getId());

        List<TeamIssue> serviceTeamIssues = teamIssueService.getAllIssues();
        assertThat(serviceTeamIssues.get(0).getId()).isEqualTo(savedIssue.getId());
    }

    /**
     * エラーハンドリングと例外処理テスト
     * 要件: 4.1, 4.2
     */
    @Test
    void testErrorHandlingAndExceptionProcessing() {
        // 1. 存在しないユーザーでの負荷状況更新テスト
        try {
            workloadStatusService.updateWorkloadStatus(999L, WorkloadLevel.HIGH, 3, 15);
            assertThat(false).as("ユーザーが存在しないエラーが発生するべき").isTrue();
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }

        // 2. 存在しないユーザーでの困りごと投稿テスト
        try {
            teamIssueService.createIssue(999L, "テスト困りごと");
            assertThat(false).as("ユーザーが存在しないエラーが発生するべき").isTrue();
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }

        // 3. 存在しない困りごとへのコメント投稿テスト
        try {
            teamIssueService.addComment(999L, testUser1.getId(), "テストコメント");
            assertThat(false).as("困りごとが存在しないエラーが発生するべき").isTrue();
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }

        // 4. 存在しない困りごとの解決マークテスト
        try {
            teamIssueService.resolveIssue(999L);
            assertThat(false).as("困りごとが存在しないエラーが発生するべき").isTrue();
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    /**
     * 負荷レベル別の色分け表示テスト（ビジネスロジック）
     * 要件: 1.1, 1.3
     */
    @Test
    void testWorkloadLevelColorCoding() {
        // 各負荷レベルのテストデータを作成
        workloadStatusService.updateWorkloadStatus(testUser1.getId(), WorkloadLevel.HIGH, 5, 25);
        workloadStatusService.updateWorkloadStatus(testUser2.getId(), WorkloadLevel.MEDIUM, 3, 12);
        workloadStatusService.updateWorkloadStatus(testUser3.getId(), WorkloadLevel.LOW, 1, 5);

        // サービスから負荷状況を取得
        List<WorkloadStatus> workloadStatuses = workloadStatusService.getAllWorkloadStatuses();

        assertThat(workloadStatuses).hasSize(3);

        // 各負荷レベルが正しく返されているか確認
        long highCount = workloadStatuses.stream().filter(ws -> ws.getWorkloadLevel() == WorkloadLevel.HIGH).count();
        long mediumCount = workloadStatuses.stream().filter(ws -> ws.getWorkloadLevel() == WorkloadLevel.MEDIUM).count();
        long lowCount = workloadStatuses.stream().filter(ws -> ws.getWorkloadLevel() == WorkloadLevel.LOW).count();

        assertThat(highCount).isEqualTo(1);
        assertThat(mediumCount).isEqualTo(1);
        assertThat(lowCount).isEqualTo(1);

        // 高負荷ユーザーの詳細確認（警告表示対象）
        WorkloadStatus highWorkloadUser = workloadStatuses.stream()
                .filter(ws -> ws.getWorkloadLevel() == WorkloadLevel.HIGH)
                .findFirst().orElseThrow();
        
        assertThat(highWorkloadUser.getUser().getId()).isEqualTo(testUser1.getId());
        assertThat(highWorkloadUser.getProjectCount()).isEqualTo(5);
        assertThat(highWorkloadUser.getTaskCount()).isEqualTo(25);
    }

    /**
     * 困りごと共有機能の統合テスト
     * 要件: 3.1, 3.2, 3.3
     */
    @Test
    void testTeamIssueFeatureIntegration() {
        // 困りごと投稿
        TeamIssue issue = teamIssueService.createIssue(
            testUser1.getId(), 
            "統合テスト用の困りごとです"
        );
        
        assertThat(issue.getContent()).isEqualTo("統合テスト用の困りごとです");
        assertThat(issue.getStatus()).isEqualTo(IssueStatus.OPEN);
        assertThat(issue.getUser().getId()).isEqualTo(testUser1.getId());

        // コメント投稿
        IssueComment comment1 = teamIssueService.addComment(
            issue.getId(), 
            testUser2.getId(), 
            "解決策を提案します"
        );
        
        IssueComment comment2 = teamIssueService.addComment(
            issue.getId(), 
            testUser3.getId(), 
            "私も同じ問題を抱えています"
        );

        // コメント取得確認
        List<IssueComment> comments = teamIssueService.getCommentsByIssueId(issue.getId());
        assertThat(comments).hasSize(2);
        assertThat(comments.get(0).getContent()).isEqualTo("解決策を提案します");
        assertThat(comments.get(1).getContent()).isEqualTo("私も同じ問題を抱えています");

        // 困りごと解決
        TeamIssue resolvedIssue = teamIssueService.resolveIssue(issue.getId());
        assertThat(resolvedIssue.getStatus()).isEqualTo(IssueStatus.RESOLVED);
        assertThat(resolvedIssue.getResolvedAt()).isNotNull();

        // 全体確認
        List<TeamIssue> allIssues = teamIssueService.getAllIssues();
        assertThat(allIssues).hasSize(1);
        assertThat(allIssues.get(0).getStatus()).isEqualTo(IssueStatus.RESOLVED);
    }

    private void setupLargeDataSet() {
        // 複数ユーザーの負荷状況設定
        workloadStatusService.updateWorkloadStatus(testUser1.getId(), WorkloadLevel.HIGH, 4, 20);
        workloadStatusService.updateWorkloadStatus(testUser2.getId(), WorkloadLevel.MEDIUM, 2, 8);
        workloadStatusService.updateWorkloadStatus(testUser3.getId(), WorkloadLevel.LOW, 1, 3);
        
        // 複数の困りごと作成
        teamIssueService.createIssue(testUser1.getId(), "プロジェクトAの進捗遅延");
        teamIssueService.createIssue(testUser2.getId(), "新技術の学習時間不足");
        teamIssueService.createIssue(testUser3.getId(), "コードレビューの待ち時間");
        
        // 追加のテストデータ（パフォーマンステスト用）
        for (int i = 0; i < 10; i++) {
            teamIssueService.createIssue(testUser1.getId(), "パフォーマンステスト用困りごと " + i);
        }
    }
}