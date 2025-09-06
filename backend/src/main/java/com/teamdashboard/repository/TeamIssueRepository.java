package com.teamdashboard.repository;

import com.teamdashboard.entity.TeamIssue;
import com.teamdashboard.entity.IssueStatus;
import com.teamdashboard.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TeamIssueエンティティのリポジトリインターフェース
 */
@Repository
public interface TeamIssueRepository extends JpaRepository<TeamIssue, Long> {
    
    /**
     * 全ての困りごとを作成日時の降順で取得（ユーザー情報も含む）
     * @return 困りごとのリスト
     */
    @Query("SELECT ti FROM TeamIssue ti JOIN FETCH ti.user ORDER BY ti.createdAt DESC")
    List<TeamIssue> findAllWithUserOrderByCreatedAtDesc();
    
    /**
     * 指定されたステータスの困りごとを作成日時の降順で取得
     * @param status 困りごとのステータス
     * @return 困りごとのリスト
     */
    @Query("SELECT ti FROM TeamIssue ti JOIN FETCH ti.user WHERE ti.status = :status ORDER BY ti.createdAt DESC")
    List<TeamIssue> findByStatusWithUserOrderByCreatedAtDesc(@Param("status") IssueStatus status);
    
    /**
     * 指定されたユーザーの困りごとを作成日時の降順で取得
     * @param user ユーザー
     * @return 困りごとのリスト
     */
    List<TeamIssue> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * 指定されたユーザーIDの困りごとを作成日時の降順で取得
     * @param userId ユーザーID
     * @return 困りごとのリスト
     */
    @Query("SELECT ti FROM TeamIssue ti JOIN FETCH ti.user WHERE ti.user.id = :userId ORDER BY ti.createdAt DESC")
    List<TeamIssue> findByUserIdWithUserOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    /**
     * 指定された期間内に作成された困りごとを取得
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @return 困りごとのリスト
     */
    @Query("SELECT ti FROM TeamIssue ti JOIN FETCH ti.user WHERE ti.createdAt BETWEEN :startDate AND :endDate ORDER BY ti.createdAt DESC")
    List<TeamIssue> findByCreatedAtBetweenWithUserOrderByCreatedAtDesc(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * 未解決の困りごとの件数を取得
     * @return 未解決の困りごとの件数
     */
    long countByStatus(IssueStatus status);
}