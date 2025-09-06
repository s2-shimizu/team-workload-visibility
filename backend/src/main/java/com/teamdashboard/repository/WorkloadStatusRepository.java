package com.teamdashboard.repository;

import com.teamdashboard.entity.WorkloadStatus;
import com.teamdashboard.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * WorkloadStatusエンティティのリポジトリインターフェース
 */
@Repository
public interface WorkloadStatusRepository extends JpaRepository<WorkloadStatus, Long> {
    
    /**
     * 指定されたユーザーの負荷状況を取得
     * @param user ユーザー
     * @return 負荷状況（存在しない場合はOptional.empty()）
     */
    Optional<WorkloadStatus> findByUser(User user);
    
    /**
     * 指定されたユーザーIDの負荷状況を取得
     * @param userId ユーザーID
     * @return 負荷状況（存在しない場合はOptional.empty()）
     */
    Optional<WorkloadStatus> findByUserId(Long userId);
    
    /**
     * 全メンバーの負荷状況を更新日時の降順で取得
     * @return 負荷状況のリスト
     */
    @Query("SELECT ws FROM WorkloadStatus ws JOIN FETCH ws.user ORDER BY ws.updatedAt DESC")
    List<WorkloadStatus> findAllWithUserOrderByUpdatedAtDesc();
    
    /**
     * 指定された負荷レベルの負荷状況を取得
     * @param workloadLevel 負荷レベル
     * @return 負荷状況のリスト
     */
    @Query("SELECT ws FROM WorkloadStatus ws JOIN FETCH ws.user WHERE ws.workloadLevel = :workloadLevel ORDER BY ws.updatedAt DESC")
    List<WorkloadStatus> findByWorkloadLevelWithUser(com.teamdashboard.entity.WorkloadLevel workloadLevel);
}