package com.teamdashboard.integration;

import com.teamdashboard.entity.IssueStatus;
import com.teamdashboard.entity.TeamIssue;
import com.teamdashboard.entity.IssueComment;
import com.teamdashboard.entity.User;
import com.teamdashboard.repository.TeamIssueRepository;
import com.teamdashboard.repository.IssueCommentRepository;
import com.teamdashboard.repository.UserRepository;
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
 * 困りごと共有機能の統合テスト（サービスレイヤー）
 * 要件: 3.1, 3.2, 3.3
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TeamIssueServiceIntegrationTest {

    @Autowired
    private TeamIssueService teamIssueService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamIssueRepository teamIssueRepository;

    @Autowired
    private IssueCommentRepository issueCommentRepository;

    private User testUser1;
    private User testUser2;
    private User testUser3;

    @BeforeEach
    void setUp() {
        // 既存データをクリーンアップ（順序重要：外部キー制約のため）
        issueCommentRepository.deleteAll();
        teamIssueRepository.deleteAll();
        
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
        String issueContent = "プロジェクトのデプロイメント手順が分からず困っています。";
        
        TeamIssue createdIssue = teamIssueService.createIssue(testUser1.getId(), issueContent);

        // 投稿された困りごとの確認
        assertThat(createdIssue).isNotNull();
        assertThat(createdIssue.getContent()).isEqualTo(issueContent);
        assertThat(createdIssue.getUser().getUsername()).isEqualTo("testuser1");
        assertThat(createdIssue.getStatus()).isEqualTo(IssueStatus.OPEN);
        assertThat(createdIssue.getCreatedAt()).isNotNull();
        assertThat(createdIssue.getResolvedAt()).isNull();

        // 2. 困りごと一覧を取得して確認
        List<TeamIssue> allIssues = teamIssueService.getAllIssues();
        assertThat(allIssues).hasSize(1);
        
        TeamIssue retrievedIssue = allIssues.get(0);
        assertThat(retrievedIssue.getContent()).isEqualTo(issueContent);
        assertThat(retrievedIssue.getUser().getUsername()).isEqualTo("testuser1");
        assertThat(retrievedIssue.getStatus()).isEqualTo(IssueStatus.OPEN);

        // 3. データベースに正しく保存されているか確認
        List<TeamIssue> savedIssues = teamIssueRepository.findAll();
        assertThat(savedIssues).hasSize(1);
        TeamIssue savedIssue = savedIssues.get(0);
        assertThat(savedIssue.getContent()).isEqualTo(issueContent);
        assertThat(savedIssue.getUser().getUsername()).isEqualTo("testuser1");
        assertThat(savedIssue.getStatus()).isEqualTo(IssueStatus.OPEN);
        assertThat(savedIssue.getCreatedAt()).isNotNull();
    }

    /**
     * コメント機能のテスト
     * 要件: 3.3
     */
    @Test
    void testIssueCommentFunctionality() {
        // 1. 困りごとを投稿
        String issueContent = "新しいライブラリの使い方が分からない";
        TeamIssue issue = teamIssueService.createIssue(testUser1.getId(), issueContent);

        // 2. コメントを投稿
        String commentContent1 = "公式ドキュメントを確認してみてください。";
        IssueComment comment1 = teamIssueService.addComment(issue.getId(), testUser2.getId(), commentContent1);

        // コメントの確認
        assertThat(comment1).isNotNull();
        assertThat(comment1.getContent()).isEqualTo(commentContent1);
        assertThat(comment1.getUser().getUsername()).isEqualTo("testuser2");
        assertThat(comment1.getIssue().getId()).isEqualTo(issue.getId());
        assertThat(comment1.getCreatedAt()).isNotNull();

        // 3. 別のユーザーからもコメント
        String commentContent2 = "サンプルコードも参考になりますよ。";
        IssueComment comment2 = teamIssueService.addComment(issue.getId(), testUser3.getId(), commentContent2);

        assertThat(comment2.getContent()).isEqualTo(commentContent2);
        assertThat(comment2.getUser().getUsername()).isEqualTo("testuser3");

        // 4. コメント一覧を取得
        List<IssueComment> comments = teamIssueService.getCommentsByIssueId(issue.getId());
        assertThat(comments).hasSize(2);

        // コメントが作成日時順に並んでいるか確認
        assertThat(comments.get(0).getContent()).isEqualTo(commentContent1);
        assertThat(comments.get(0).getUser().getUsername()).isEqualTo("testuser2");
        assertThat(comments.get(1).getContent()).isEqualTo(commentContent2);
        assertThat(comments.get(1).getUser().getUsername()).isEqualTo("testuser3");

        // 5. データベースに正しく保存されているか確認
        List<IssueComment> savedComments = issueCommentRepository.findAll();
        assertThat(savedComments).hasSize(2);
    }

    /**
     * 解決マーク機能のテスト
     * 要件: 3.2, 3.3
     */
    @Test
    void testIssueResolutionFunctionality() {
        // 1. 困りごとを投稿
        String issueContent = "テスト環境の設定方法について";
        TeamIssue issue = teamIssueService.createIssue(testUser1.getId(), issueContent);

        // 初期状態の確認
        assertThat(issue.getStatus()).isEqualTo(IssueStatus.OPEN);
        assertThat(issue.getResolvedAt()).isNull();

        // 2. コメントを追加
        teamIssueService.addComment(issue.getId(), testUser2.getId(), "設定手順を共有します。");

        // 3. 困りごとを解決済みにマーク
        LocalDateTime beforeResolve = LocalDateTime.now();
        TeamIssue resolvedIssue = teamIssueService.resolveIssue(issue.getId());
        LocalDateTime afterResolve = LocalDateTime.now();

        // 解決状態の確認
        assertThat(resolvedIssue.getStatus()).isEqualTo(IssueStatus.RESOLVED);
        assertThat(resolvedIssue.getResolvedAt()).isNotNull();
        assertThat(resolvedIssue.getResolvedAt()).isAfterOrEqualTo(beforeResolve.minusSeconds(1));
        assertThat(resolvedIssue.getResolvedAt()).isBeforeOrEqualTo(afterResolve.plusSeconds(1));

        // 4. 解決済み困りごとが一覧に正しく表示されるか確認
        List<TeamIssue> allIssues = teamIssueService.getAllIssues();
        assertThat(allIssues).hasSize(1);
        TeamIssue retrievedIssue = allIssues.get(0);
        assertThat(retrievedIssue.getStatus()).isEqualTo(IssueStatus.RESOLVED);
        assertThat(retrievedIssue.getResolvedAt()).isNotNull();

        // 5. データベースの状態確認
        Optional<TeamIssue> savedIssueOpt = teamIssueRepository.findById(issue.getId());
        assertThat(savedIssueOpt).isPresent();
        TeamIssue savedIssue = savedIssueOpt.get();
        assertThat(savedIssue.getStatus()).isEqualTo(IssueStatus.RESOLVED);
        assertThat(savedIssue.getResolvedAt()).isNotNull();
    }

    /**
     * 複数ユーザー間での困りごと共有テスト
     * 要件: 3.1, 3.2, 3.3
     */
    @Test
    void testMultiUserIssueSharing() {
        // 1. 複数のユーザーが困りごとを投稿
        TeamIssue issue1 = teamIssueService.createIssue(testUser1.getId(), "データベース接続エラーが発生します");
        TeamIssue issue2 = teamIssueService.createIssue(testUser2.getId(), "UIコンポーネントの配置について相談");
        TeamIssue issue3 = teamIssueService.createIssue(testUser3.getId(), "パフォーマンス改善のアドバイスが欲しい");

        // 2. 困りごと一覧の確認
        List<TeamIssue> allIssues = teamIssueService.getAllIssues();
        assertThat(allIssues).hasSize(3);

        // 各困りごとが正しく投稿されているか確認
        assertThat(allIssues.stream().anyMatch(i -> i.getUser().getUsername().equals("testuser1"))).isTrue();
        assertThat(allIssues.stream().anyMatch(i -> i.getUser().getUsername().equals("testuser2"))).isTrue();
        assertThat(allIssues.stream().anyMatch(i -> i.getUser().getUsername().equals("testuser3"))).isTrue();

        // 3. 相互にコメントを投稿
        teamIssueService.addComment(issue1.getId(), testUser2.getId(), "ログファイルを確認してみてください");
        teamIssueService.addComment(issue1.getId(), testUser3.getId(), "設定ファイルの確認も必要です");
        
        teamIssueService.addComment(issue2.getId(), testUser1.getId(), "レスポンシブデザインを考慮しましょう");
        teamIssueService.addComment(issue2.getId(), testUser3.getId(), "ユーザビリティテストも重要です");

        teamIssueService.addComment(issue3.getId(), testUser1.getId(), "プロファイリングツールを使ってみてください");
        teamIssueService.addComment(issue3.getId(), testUser2.getId(), "キャッシュ戦略の見直しも効果的です");

        // 4. コメント数の確認
        List<IssueComment> issue1Comments = teamIssueService.getCommentsByIssueId(issue1.getId());
        List<IssueComment> issue2Comments = teamIssueService.getCommentsByIssueId(issue2.getId());
        List<IssueComment> issue3Comments = teamIssueService.getCommentsByIssueId(issue3.getId());

        assertThat(issue1Comments).hasSize(2);
        assertThat(issue2Comments).hasSize(2);
        assertThat(issue3Comments).hasSize(2);

        // 5. 一部の困りごとを解決
        teamIssueService.resolveIssue(issue1.getId());
        teamIssueService.resolveIssue(issue3.getId());

        // 6. 解決状態の確認
        List<TeamIssue> updatedIssues = teamIssueService.getAllIssues();
        long resolvedCount = updatedIssues.stream().filter(i -> i.getStatus() == IssueStatus.RESOLVED).count();
        long openCount = updatedIssues.stream().filter(i -> i.getStatus() == IssueStatus.OPEN).count();

        assertThat(resolvedCount).isEqualTo(2);
        assertThat(openCount).isEqualTo(1);

        // 7. データベースの整合性確認
        List<TeamIssue> allSavedIssues = teamIssueRepository.findAll();
        List<IssueComment> allSavedComments = issueCommentRepository.findAll();

        assertThat(allSavedIssues).hasSize(3);
        assertThat(allSavedComments).hasSize(6); // 各困りごとに2つずつコメント
    }

    /**
     * 困りごと投稿時のバリデーションテスト
     * 要件: 3.1
     */
    @Test
    void testIssueValidation() {
        // 空の内容での投稿テスト
        try {
            teamIssueService.createIssue(testUser1.getId(), "");
            assertThat(false).as("空の内容でのバリデーションエラーが発生するべき").isTrue();
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }

        // null内容での投稿テスト
        try {
            teamIssueService.createIssue(testUser1.getId(), null);
            assertThat(false).as("null内容でのバリデーションエラーが発生するべき").isTrue();
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }

        // 存在しないユーザーでの投稿テスト
        try {
            teamIssueService.createIssue(999L, "テスト困りごと");
            assertThat(false).as("存在しないユーザーでのエラーが発生するべき").isTrue();
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    /**
     * コメント投稿時のバリデーションテスト
     * 要件: 3.3
     */
    @Test
    void testCommentValidation() {
        // 困りごとを作成
        TeamIssue issue = teamIssueService.createIssue(testUser1.getId(), "テスト困りごと");

        // 空のコメント投稿テスト
        try {
            teamIssueService.addComment(issue.getId(), testUser2.getId(), "");
            assertThat(false).as("空のコメントでのバリデーションエラーが発生するべき").isTrue();
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }

        // 存在しない困りごとへのコメント投稿テスト
        try {
            teamIssueService.addComment(999L, testUser2.getId(), "テストコメント");
            assertThat(false).as("存在しない困りごとへのコメントでエラーが発生するべき").isTrue();
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }

        // 存在しないユーザーでのコメント投稿テスト
        try {
            teamIssueService.addComment(issue.getId(), 999L, "テストコメント");
            assertThat(false).as("存在しないユーザーでのコメントでエラーが発生するべき").isTrue();
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    /**
     * 困りごとの作成日時順表示テスト
     * 要件: 3.2
     */
    @Test
    void testIssueOrderingByCreatedDate() throws InterruptedException {
        // 時間差をつけて困りごとを投稿
        TeamIssue issue1 = teamIssueService.createIssue(testUser1.getId(), "最初の困りごと");
        Thread.sleep(10); // 時間差を作る
        
        TeamIssue issue2 = teamIssueService.createIssue(testUser2.getId(), "2番目の困りごと");
        Thread.sleep(10);
        
        TeamIssue issue3 = teamIssueService.createIssue(testUser3.getId(), "3番目の困りごと");

        // 困りごと一覧を取得
        List<TeamIssue> issues = teamIssueService.getAllIssues();

        // 作成日時の降順（新しい順）で並んでいるか確認
        assertThat(issues).hasSize(3);
        assertThat(issues.get(0).getContent()).isEqualTo("3番目の困りごと");
        assertThat(issues.get(1).getContent()).isEqualTo("2番目の困りごと");
        assertThat(issues.get(2).getContent()).isEqualTo("最初の困りごと");

        // 作成日時が正しく設定されているか確認
        assertThat(issues.get(0).getCreatedAt()).isAfterOrEqualTo(issues.get(1).getCreatedAt());
        assertThat(issues.get(1).getCreatedAt()).isAfterOrEqualTo(issues.get(2).getCreatedAt());
    }
}