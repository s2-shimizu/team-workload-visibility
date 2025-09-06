package com.teamdashboard.service;

import com.teamdashboard.entity.IssueComment;
import com.teamdashboard.entity.IssueStatus;
import com.teamdashboard.entity.TeamIssue;
import com.teamdashboard.entity.User;
import com.teamdashboard.repository.IssueCommentRepository;
import com.teamdashboard.repository.TeamIssueRepository;
import com.teamdashboard.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Optional;

/**
 * TeamIssue（困りごと）に関するビジネスロジックを提供するサービスクラス
 */
@Service
@Transactional
@Profile("!dynamodb")
public class TeamIssueService {

    private final TeamIssueRepository teamIssueRepository;
    private final IssueCommentRepository issueCommentRepository;
    private final UserRepository userRepository;

    @Autowired
    public TeamIssueService(TeamIssueRepository teamIssueRepository,
                           IssueCommentRepository issueCommentRepository,
                           UserRepository userRepository) {
        this.teamIssueRepository = teamIssueRepository;
        this.issueCommentRepository = issueCommentRepository;
        this.userRepository = userRepository;
    }

    /**
     * 全ての困りごとを取得（作成日時の降順）
     * @return 困りごとのリスト
     */
    @Transactional(readOnly = true)
    public List<TeamIssue> getAllIssues() {
        return teamIssueRepository.findAllWithUserOrderByCreatedAtDesc();
    }

    /**
     * 指定されたステータスの困りごとを取得
     * @param status 困りごとのステータス
     * @return 困りごとのリスト
     */
    @Transactional(readOnly = true)
    public List<TeamIssue> getIssuesByStatus(IssueStatus status) {
        return teamIssueRepository.findByStatusWithUserOrderByCreatedAtDesc(status);
    }

    /**
     * 未解決の困りごとを取得
     * @return 未解決の困りごとのリスト
     */
    @Transactional(readOnly = true)
    public List<TeamIssue> getOpenIssues() {
        return getIssuesByStatus(IssueStatus.OPEN);
    }

    /**
     * 解決済みの困りごとを取得
     * @return 解決済みの困りごとのリスト
     */
    @Transactional(readOnly = true)
    public List<TeamIssue> getResolvedIssues() {
        return getIssuesByStatus(IssueStatus.RESOLVED);
    }

    /**
     * 指定されたIDの困りごとを取得
     * @param id 困りごとID
     * @return 困りごと（存在しない場合はOptional.empty()）
     */
    @Transactional(readOnly = true)
    public Optional<TeamIssue> getIssueById(Long id) {
        return teamIssueRepository.findById(id);
    }

    /**
     * 指定されたユーザーの困りごとを取得
     * @param userId ユーザーID
     * @return 困りごとのリスト
     */
    @Transactional(readOnly = true)
    public List<TeamIssue> getIssuesByUserId(Long userId) {
        return teamIssueRepository.findByUserIdWithUserOrderByCreatedAtDesc(userId);
    }

    /**
     * 新しい困りごとを投稿
     * @param userId 投稿者のユーザーID
     * @param content 困りごとの内容
     * @return 作成された困りごと
     * @throws IllegalArgumentException ユーザーが存在しない場合
     */
    public TeamIssue createIssue(Long userId, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません: " + userId));

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("困りごとの内容は必須です");
        }

        if (content.length() > 1000) {
            throw new IllegalArgumentException("困りごとの内容は1000文字以内で入力してください");
        }

        TeamIssue issue = new TeamIssue(user, content.trim());
        return teamIssueRepository.save(issue);
    }

    /**
     * 困りごとを解決済みにマーク
     * @param issueId 困りごとID
     * @return 更新された困りごと
     * @throws IllegalArgumentException 困りごとが存在しない場合
     */
    public TeamIssue resolveIssue(Long issueId) {
        TeamIssue issue = teamIssueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("困りごとが見つかりません: " + issueId));

        if (issue.isResolved()) {
            throw new IllegalStateException("この困りごとは既に解決済みです");
        }

        issue.resolve();
        return teamIssueRepository.save(issue);
    }

    /**
     * 困りごとを未解決に戻す
     * @param issueId 困りごとID
     * @return 更新された困りごと
     * @throws IllegalArgumentException 困りごとが存在しない場合
     */
    public TeamIssue reopenIssue(Long issueId) {
        TeamIssue issue = teamIssueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("困りごとが見つかりません: " + issueId));

        if (!issue.isResolved()) {
            throw new IllegalStateException("この困りごとは既に未解決状態です");
        }

        issue.reopen();
        return teamIssueRepository.save(issue);
    }

    /**
     * 指定された困りごとのコメントを取得
     * @param issueId 困りごとID
     * @return コメントのリスト（作成日時の昇順）
     */
    @Transactional(readOnly = true)
    public List<IssueComment> getCommentsByIssueId(Long issueId) {
        return issueCommentRepository.findByIssueIdWithUserOrderByCreatedAtAsc(issueId);
    }

    /**
     * 困りごとにコメントを投稿
     * @param issueId 困りごとID
     * @param userId コメント投稿者のユーザーID
     * @param content コメント内容
     * @return 作成されたコメント
     * @throws IllegalArgumentException 困りごとまたはユーザーが存在しない場合
     */
    public IssueComment addComment(Long issueId, Long userId, String content) {
        TeamIssue issue = teamIssueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("困りごとが見つかりません: " + issueId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません: " + userId));

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("コメント内容は必須です");
        }

        if (content.length() > 500) {
            throw new IllegalArgumentException("コメントは500文字以内で入力してください");
        }

        IssueComment comment = new IssueComment(issue, user, content.trim());
        return issueCommentRepository.save(comment);
    }

    /**
     * 未解決の困りごとの件数を取得
     * @return 未解決の困りごとの件数
     */
    @Transactional(readOnly = true)
    public long getOpenIssueCount() {
        return teamIssueRepository.countByStatus(IssueStatus.OPEN);
    }

    /**
     * 解決済みの困りごとの件数を取得
     * @return 解決済みの困りごとの件数
     */
    @Transactional(readOnly = true)
    public long getResolvedIssueCount() {
        return teamIssueRepository.countByStatus(IssueStatus.RESOLVED);
    }

    /**
     * 指定された困りごとのコメント数を取得
     * @param issueId 困りごとID
     * @return コメント数
     */
    @Transactional(readOnly = true)
    public long getCommentCount(Long issueId) {
        return issueCommentRepository.countByIssueId(issueId);
    }
}