package com.teamdashboard.repository;

import com.teamdashboard.entity.IssueComment;
import com.teamdashboard.entity.TeamIssue;
import com.teamdashboard.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * IssueCommentエンティティのリポジトリインターフェース
 */
@Repository
public interface IssueCommentRepository extends JpaRepository<IssueComment, Long> {
    
    /**
     * 指定された困りごとのコメントを作成日時の昇順で取得（ユーザー情報も含む）
     * @param issue 困りごと
     * @return コメントのリスト
     */
    @Query("SELECT ic FROM IssueComment ic JOIN FETCH ic.user WHERE ic.issue = :issue ORDER BY ic.createdAt ASC")
    List<IssueComment> findByIssueWithUserOrderByCreatedAtAsc(@Param("issue") TeamIssue issue);
    
    /**
     * 指定された困りごとIDのコメントを作成日時の昇順で取得（ユーザー情報も含む）
     * @param issueId 困りごとID
     * @return コメントのリスト
     */
    @Query("SELECT ic FROM IssueComment ic JOIN FETCH ic.user WHERE ic.issue.id = :issueId ORDER BY ic.createdAt ASC")
    List<IssueComment> findByIssueIdWithUserOrderByCreatedAtAsc(@Param("issueId") Long issueId);
    
    /**
     * 指定されたユーザーのコメントを作成日時の降順で取得
     * @param user ユーザー
     * @return コメントのリスト
     */
    List<IssueComment> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * 指定された困りごとのコメント数を取得
     * @param issue 困りごと
     * @return コメント数
     */
    long countByIssue(TeamIssue issue);
    
    /**
     * 指定された困りごとIDのコメント数を取得
     * @param issueId 困りごとID
     * @return コメント数
     */
    long countByIssueId(Long issueId);
}