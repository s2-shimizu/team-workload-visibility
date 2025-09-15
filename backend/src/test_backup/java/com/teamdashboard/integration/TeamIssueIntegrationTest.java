package com.teamdashboard.integration;

import com.teamdashboard.LambdaApplication;
import com.teamdashboard.dto.IssueCommentRequestDTO;
import com.teamdashboard.dto.IssueCommentResponseDTO;
import com.teamdashboard.dto.TeamIssueRequestDTO;
import com.teamdashboard.dto.TeamIssueResponseDTO;
import com.teamdashboard.entity.IssueComment;
import com.teamdashboard.entity.IssueStatus;
import com.teamdashboard.entity.TeamIssue;
import com.teamdashboard.entity.User;
import com.teamdashboard.repository.DailyReportRepository;
import com.teamdashboard.repository.IssueCommentRepository;
import com.teamdashboard.repository.TeamIssueRepository;
import com.teamdashboard.repository.UserRepository;
import com.teamdashboard.repository.WorkloadStatusRepository;
import com.teamdashboard.service.TeamIssueService;
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
 * 困りごと共有機能の統合テスト
 * 要件: 3.1, 3.2, 3.3
 */
@SpringBootTest(classes = LambdaApplication.class)
@ActiveProfiles("test")
@Transactional
public class TeamIssueIntegrationTest {

    @Autowired
    private TeamIssueService teamIssueService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamIssueRepository teamIssueRepository;

    @Autowired
    private IssueCommentRepository issueCommentRepository;

    @Autowired
    private WorkloadStatusRepository workloadStatusRepository;

    @Autowired
    private DailyReportRepository dailyReportRepository;

    private User testUser1;
    private User testUser2;
    private User testUser3;

    @BeforeEach
    void setUp() {
        // 既存データをクリーンアップ（外部キー制約を考慮した順序）
        issueCommentRepository.deleteAll();
        teamIssueRepository.deleteAll();
        workloadStatusRepository.deleteAll();
        dailyReportRepository.deleteAll();
        userRepository.deleteAll();
        
        // テストユーザーを作成
        testUser1 = new User();
        testUser1.setUsername("testuser1");
        testUser1.setDisplayName("テストユーザー1");
        testUser1.setEmail("test1@example.com");
        testUser1.setPassword("password");
        testUser1 = userRepository.save(testUser1);

        testUser2 = new User();
        testUser2.setUsername("testuser2");
        testUser2.setDisplayName("テストユーザー2");
        testUser2.setEmail("test2@example.com");
        testUser2.setPassword("password");
        testUser2 = userRepository.save(testUser2);

        testUser3 = new User();
        testUser3.setUsername("testuser3");
        testUser3.setDisplayName("テストユーザー3");
        testUser3.setEmail("test3@example.com");
        testUser3.setPassword("password");
        testUser3 = userRepository.save(testUser3);
    }

    /**
     * 困りごと投稿から表示までの一連の流れをテスト
     * 要件: 3.1, 3.2
     */
    @Test
    void testTeamIssuePostAndDisplay() {
        // 1. 困りごとを投稿
        TeamIssueRequestDTO request = new TeamIssueRequestDTO();
        request.setContent("新しいAPIの実装で困っています。認証部分の実装方法がわからず、進捗が遅れています。");

        TeamIssue createdIssue = teamIssueService.createIssue(testUser1.getId(), request.getContent());

        // 投稿レスポンスの詳細確認
        assertThat(createdIssue).isNotNull();
        assertThat(createdIssue.getContent()).isEqualTo("新しいAPIの実装で困っています。認証部分の実装方法がわからず、進捗が遅れています。");
        assertThat(createdIssue.getStatus()).isEqualTo(IssueStatus.OPEN);
        assertThat(createdIssue.getUser().getUsername()).isEqualTo("testuser1");
        assertThat(createdIssue.getCreatedAt()).isNotNull();
        assertThat(createdIssue.getResolvedAt()).isNull();

        // 2. 困りごと一覧を取得して確認
        List<TeamIssue> allIssues = teamIssueService.getAllIssues();
        assertThat(allIssues).hasSize(1);
        
        TeamIssue displayedIssue = allIssues.get(0);
        assertThat(displayedIssue.getContent()).isEqualTo("新しいAPIの実装で困っています。認証部分の実装方法がわからず、進捗が遅れています。");
        assertThat(displayedIssue.getStatus()).isEqualTo(IssueStatus.OPEN);
        assertThat(displayedIssue.getUser().getUsername()).isEqualTo("testuser1");
        assertThat(displayedIssue.getUser().getDisplayName()).isEqualTo("テストユーザー1");

        // 3. データベースに正しく保存されているか確認
        List<TeamIssue> savedIssues = teamIssueRepository.findAll();
        assertThat(savedIssues).hasSize(1);
        
        TeamIssue savedIssue = savedIssues.get(0);
        assertThat(savedIssue.getContent()).isEqualTo("新しいAPIの実装で困っています。認証部分の実装方法がわからず、進捗が遅れています。");
        assertThat(savedIssue.getStatus()).isEqualTo(IssueStatus.OPEN);
        assertThat(savedIssue.getUser().getUsername()).isEqualTo("testuser1");
        assertThat(savedIssue.getCreatedAt()).isNotNull();
        assertThat(savedIssue.getResolvedAt()).isNull();
    }

    /**
     * コメント機能と解決マーク機能のテストを実装
     * 要件: 3.3
     */
    @Test
    void testCommentAndResolveFeatures() {
        // 1. 困りごとを投稿
        TeamIssue issue = teamIssueService.createIssue(
            testUser1.getId(), 
            "データベース接続エラーが頻発しています。解決方法を教えてください。"
        );

        // 2. 他のユーザーがコメントを投稿
        IssueComment comment1 = teamIssueService.addComment(
            issue.getId(), 
            testUser2.getId(), 
            "接続プールの設定を確認してみてください。application.ymlの設定を見直すと良いかもしれません。"
        );

        // コメント投稿の確認
        assertThat(comment1).isNotNull();
        assertThat(comment1.getContent()).isEqualTo("接続プールの設定を確認してみてください。application.ymlの設定を見直すと良いかもしれません。");
        assertThat(comment1.getUser().getUsername()).isEqualTo("testuser2");
        assertThat(comment1.getIssue().getId()).isEqualTo(issue.getId());
        assertThat(comment1.getCreatedAt()).isNotNull();

        // 3. さらに別のユーザーがコメントを投稿
        IssueComment comment2 = teamIssueService.addComment(
            issue.getId(), 
            testUser3.getId(), 
            "私も同じ問題に遭遇しました。HikariCPの設定を調整することで解決できました。"
        );

        // 4. コメント一覧を取得して確認
        List<IssueComment> comments = teamIssueService.getCommentsByIssueId(issue.getId());
        assertThat(comments).hasSize(2);

        // コメントが作成日時順に並んでいるか確認
        IssueComment firstComment = comments.get(0);
        IssueComment secondComment = comments.get(1);
        
        assertThat(firstComment.getUser().getUsername()).isEqualTo("testuser2");
        assertThat(firstComment.getContent()).contains("接続プールの設定");
        
        assertThat(secondComment.getUser().getUsername()).isEqualTo("testuser3");
        assertThat(secondComment.getContent()).contains("HikariCPの設定");

        // 5. 困りごとを解決マーク
        LocalDateTime beforeResolve = LocalDateTime.now();
        TeamIssue resolvedIssue = teamIssueService.resolveIssue(issue.getId());
        LocalDateTime afterResolve = LocalDateTime.now();

        // 解決マークの確認
        assertThat(resolvedIssue.getStatus()).isEqualTo(IssueStatus.RESOLVED);
        assertThat(resolvedIssue.getResolvedAt()).isNotNull();
        assertThat(resolvedIssue.getResolvedAt()).isAfterOrEqualTo(beforeResolve.minusSeconds(1));
        assertThat(resolvedIssue.getResolvedAt()).isBeforeOrEqualTo(afterResolve.plusSeconds(1));

        // 6. データベースの状態確認
        Optional<TeamIssue> savedIssueOpt = teamIssueRepository.findById(issue.getId());
        assertThat(savedIssueOpt).isPresent();
        TeamIssue savedIssue = savedIssueOpt.get();
        assertThat(savedIssue.getStatus()).isEqualTo(IssueStatus.RESOLVED);
        assertThat(savedIssue.getResolvedAt()).isNotNull();

        List<IssueComment> savedComments = issueCommentRepository.findByIssueIdWithUserOrderByCreatedAtAsc(issue.getId());
        assertThat(savedComments).hasSize(2);
    }

    /**
     * 複数ユーザー間での困りごと共有テストを実行
     * 要件: 3.1, 3.2, 3.3
     */
    @Test
    void testMultipleUsersIssueSharing() {
        LocalDateTime now = LocalDateTime.now();

        // 複数のユーザーが困りごとを投稿
        TeamIssue issue1 = teamIssueService.createIssue(
            testUser1.getId(), 
            "フロントエンドのレスポンシブデザインがうまくいきません。"
        );

        TeamIssue issue2 = teamIssueService.createIssue(
            testUser2.getId(), 
            "テストケースの書き方がわからず、カバレッジが上がりません。"
        );

        TeamIssue issue3 = teamIssueService.createIssue(
            testUser3.getId(), 
            "パフォーマンスの問題でページの読み込みが遅いです。"
        );

        // 全ユーザーが困りごと一覧を確認できるかテスト
        List<TeamIssue> allIssues = teamIssueService.getAllIssues();
        assertThat(allIssues).hasSize(3);

        // 各ユーザーの困りごとが正しく表示されているか確認
        TeamIssue user1Issue = allIssues.stream()
                .filter(issue -> issue.getUser().getUsername().equals("testuser1"))
                .findFirst().orElseThrow();
        assertThat(user1Issue.getContent()).contains("レスポンシブデザイン");
        assertThat(user1Issue.getStatus()).isEqualTo(IssueStatus.OPEN);

        TeamIssue user2Issue = allIssues.stream()
                .filter(issue -> issue.getUser().getUsername().equals("testuser2"))
                .findFirst().orElseThrow();
        assertThat(user2Issue.getContent()).contains("テストケース");
        assertThat(user2Issue.getStatus()).isEqualTo(IssueStatus.OPEN);

        TeamIssue user3Issue = allIssues.stream()
                .filter(issue -> issue.getUser().getUsername().equals("testuser3"))
                .findFirst().orElseThrow();
        assertThat(user3Issue.getContent()).contains("パフォーマンス");
        assertThat(user3Issue.getStatus()).isEqualTo(IssueStatus.OPEN);

        // 相互にコメントを投稿してコミュニケーションをテスト
        // ユーザー2がユーザー1の困りごとにコメント
        teamIssueService.addComment(
            issue1.getId(), 
            testUser2.getId(), 
            "CSS GridやFlexboxを使うと良いかもしれません。"
        );

        // ユーザー3がユーザー2の困りごとにコメント
        teamIssueService.addComment(
            issue2.getId(), 
            testUser3.getId(), 
            "JUnitの@ParameterizedTestを使うとテストケースを効率的に書けます。"
        );

        // ユーザー1がユーザー3の困りごとにコメント
        teamIssueService.addComment(
            issue3.getId(), 
            testUser1.getId(), 
            "データベースのインデックスを確認してみてください。"
        );

        // 各困りごとにコメントが正しく投稿されているか確認
        List<IssueComment> issue1Comments = teamIssueService.getCommentsByIssueId(issue1.getId());
        assertThat(issue1Comments).hasSize(1);
        assertThat(issue1Comments.get(0).getUser().getUsername()).isEqualTo("testuser2");
        assertThat(issue1Comments.get(0).getContent()).contains("CSS Grid");

        List<IssueComment> issue2Comments = teamIssueService.getCommentsByIssueId(issue2.getId());
        assertThat(issue2Comments).hasSize(1);
        assertThat(issue2Comments.get(0).getUser().getUsername()).isEqualTo("testuser3");
        assertThat(issue2Comments.get(0).getContent()).contains("@ParameterizedTest");

        List<IssueComment> issue3Comments = teamIssueService.getCommentsByIssueId(issue3.getId());
        assertThat(issue3Comments).hasSize(1);
        assertThat(issue3Comments.get(0).getUser().getUsername()).isEqualTo("testuser1");
        assertThat(issue3Comments.get(0).getContent()).contains("インデックス");

        // 一部の困りごとを解決
        teamIssueService.resolveIssue(issue1.getId());
        teamIssueService.resolveIssue(issue3.getId());

        // 解決済み・未解決の状態が正しく管理されているか確認
        List<TeamIssue> updatedIssues = teamIssueService.getAllIssues();
        
        long resolvedCount = updatedIssues.stream()
                .filter(issue -> issue.getStatus() == IssueStatus.RESOLVED)
                .count();
        long openCount = updatedIssues.stream()
                .filter(issue -> issue.getStatus() == IssueStatus.OPEN)
                .count();

        assertThat(resolvedCount).isEqualTo(2);
        assertThat(openCount).isEqualTo(1);

        // 未解決の困りごとがユーザー2のものであることを確認
        TeamIssue openIssue = updatedIssues.stream()
                .filter(issue -> issue.getStatus() == IssueStatus.OPEN)
                .findFirst().orElseThrow();
        assertThat(openIssue.getUser().getUsername()).isEqualTo("testuser2");
    }

    /**
     * 困りごと投稿時のバリデーションテスト
     * 要件: 3.1
     */
    @Test
    void testTeamIssueValidation() {
        // 空の内容で投稿を試行
        try {
            teamIssueService.createIssue(testUser1.getId(), "");
            assertThat(false).as("バリデーションエラーが発生するべき").isTrue();
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }

        // nullの内容で投稿を試行
        try {
            teamIssueService.createIssue(testUser1.getId(), null);
            assertThat(false).as("バリデーションエラーが発生するべき").isTrue();
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    /**
     * 存在しないユーザーでのテスト
     */
    @Test
    void testTeamIssueWithNonExistentUser() {
        try {
            teamIssueService.createIssue(999L, "存在しないユーザーからの投稿");
            assertThat(false).as("ユーザーが存在しないエラーが発生するべき").isTrue();
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    /**
     * 存在しない困りごとへのコメント投稿テスト
     */
    @Test
    void testCommentOnNonExistentIssue() {
        try {
            teamIssueService.addComment(999L, testUser1.getId(), "存在しない困りごとへのコメント");
            assertThat(false).as("困りごとが存在しないエラーが発生するべき").isTrue();
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    /**
     * 存在しない困りごとの解決マークテスト
     */
    @Test
    void testResolveNonExistentIssue() {
        try {
            teamIssueService.resolveIssue(999L);
            assertThat(false).as("困りごとが存在しないエラーが発生するべき").isTrue();
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    /**
     * 困りごとの作成日時順ソートテスト
     * 要件: 3.2
     */
    @Test
    void testTeamIssuesSortedByCreatedAt() throws InterruptedException {
        // 時間差をつけて複数の困りごとを投稿
        TeamIssue issue1 = teamIssueService.createIssue(testUser1.getId(), "最初の困りごと");
        Thread.sleep(10); // 時間差を作る
        
        TeamIssue issue2 = teamIssueService.createIssue(testUser2.getId(), "2番目の困りごと");
        Thread.sleep(10);
        
        TeamIssue issue3 = teamIssueService.createIssue(testUser3.getId(), "3番目の困りごと");

        // 困りごと一覧を取得
        List<TeamIssue> issues = teamIssueService.getAllIssues();
        
        assertThat(issues).hasSize(3);
        
        // 作成日時の降順（新しい順）で並んでいることを確認
        assertThat(issues.get(0).getContent()).isEqualTo("3番目の困りごと");
        assertThat(issues.get(1).getContent()).isEqualTo("2番目の困りごと");
        assertThat(issues.get(2).getContent()).isEqualTo("最初の困りごと");
        
        // 作成日時が正しい順序になっているか確認
        assertThat(issues.get(0).getCreatedAt()).isAfterOrEqualTo(issues.get(1).getCreatedAt());
        assertThat(issues.get(1).getCreatedAt()).isAfterOrEqualTo(issues.get(2).getCreatedAt());
    }
}