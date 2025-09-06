package com.teamdashboard.service;

import com.teamdashboard.entity.User;
import com.teamdashboard.entity.WorkloadLevel;
import com.teamdashboard.entity.WorkloadStatus;
import com.teamdashboard.repository.UserRepository;
import com.teamdashboard.repository.WorkloadStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Optional;

/**
 * 負荷状況管理のビジネスロジックを提供するサービスクラス
 */
@Service
@Transactional
@Profile("!dynamodb")
public class WorkloadStatusService {

    private final WorkloadStatusRepository workloadStatusRepository;
    private final UserRepository userRepository;

    @Autowired
    public WorkloadStatusService(WorkloadStatusRepository workloadStatusRepository, 
                               UserRepository userRepository) {
        this.workloadStatusRepository = workloadStatusRepository;
        this.userRepository = userRepository;
    }

    /**
     * 全メンバーの負荷状況を取得
     * @return 全メンバーの負荷状況リスト（更新日時降順）
     */
    @Transactional(readOnly = true)
    public List<WorkloadStatus> getAllWorkloadStatuses() {
        return workloadStatusRepository.findAllWithUserOrderByUpdatedAtDesc();
    }

    /**
     * 指定されたユーザーIDの負荷状況を取得
     * @param userId ユーザーID
     * @return 負荷状況（存在しない場合はOptional.empty()）
     */
    @Transactional(readOnly = true)
    public Optional<WorkloadStatus> getWorkloadStatusByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("ユーザーIDは必須です");
        }
        return workloadStatusRepository.findByUserId(userId);
    }

    /**
     * 指定されたユーザー名の負荷状況を取得
     * @param username ユーザー名
     * @return 負荷状況（存在しない場合はOptional.empty()）
     */
    @Transactional(readOnly = true)
    public Optional<WorkloadStatus> getWorkloadStatusByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("ユーザー名は必須です");
        }
        
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            return Optional.empty();
        }
        
        return workloadStatusRepository.findByUser(user.get());
    }

    /**
     * 負荷状況を更新または作成
     * @param userId ユーザーID
     * @param workloadLevel 負荷レベル（必須）
     * @param projectCount 案件数（任意）
     * @param taskCount タスク数（任意）
     * @return 更新された負荷状況
     * @throws IllegalArgumentException バリデーションエラー時
     * @throws RuntimeException ユーザーが存在しない場合
     */
    public WorkloadStatus updateWorkloadStatus(Long userId, WorkloadLevel workloadLevel, 
                                             Integer projectCount, Integer taskCount) {
        // バリデーション
        validateUpdateRequest(userId, workloadLevel, projectCount, taskCount);
        
        // ユーザー存在確認
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません: " + userId));
        
        // 既存の負荷状況を取得または新規作成
        WorkloadStatus workloadStatus = workloadStatusRepository.findByUser(user)
            .orElse(new WorkloadStatus(user, workloadLevel));
        
        // 値を更新
        workloadStatus.setWorkloadLevel(workloadLevel);
        workloadStatus.setProjectCount(projectCount);
        workloadStatus.setTaskCount(taskCount);
        
        return workloadStatusRepository.save(workloadStatus);
    }

    /**
     * ユーザー名による負荷状況の更新または作成
     * @param username ユーザー名
     * @param workloadLevel 負荷レベル（必須）
     * @param projectCount 案件数（任意）
     * @param taskCount タスク数（任意）
     * @return 更新された負荷状況
     * @throws IllegalArgumentException バリデーションエラー時
     * @throws RuntimeException ユーザーが存在しない場合
     */
    public WorkloadStatus updateWorkloadStatusByUsername(String username, WorkloadLevel workloadLevel,
                                                       Integer projectCount, Integer taskCount) {
        // バリデーション
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("ユーザー名は必須です");
        }
        validateWorkloadLevel(workloadLevel);
        validateOptionalCounts(projectCount, taskCount);
        
        // ユーザー存在確認
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません: " + username));
        
        return updateWorkloadStatus(user.getId(), workloadLevel, projectCount, taskCount);
    }

    /**
     * 指定された負荷レベルの負荷状況を取得
     * @param workloadLevel 負荷レベル
     * @return 指定された負荷レベルの負荷状況リスト
     */
    @Transactional(readOnly = true)
    public List<WorkloadStatus> getWorkloadStatusesByLevel(WorkloadLevel workloadLevel) {
        if (workloadLevel == null) {
            throw new IllegalArgumentException("負荷レベルは必須です");
        }
        return workloadStatusRepository.findByWorkloadLevelWithUser(workloadLevel);
    }

    /**
     * 負荷状況更新リクエストのバリデーション
     */
    private void validateUpdateRequest(Long userId, WorkloadLevel workloadLevel, 
                                     Integer projectCount, Integer taskCount) {
        if (userId == null) {
            throw new IllegalArgumentException("ユーザーIDは必須です");
        }
        validateWorkloadLevel(workloadLevel);
        validateOptionalCounts(projectCount, taskCount);
    }

    /**
     * 負荷レベルのバリデーション
     */
    private void validateWorkloadLevel(WorkloadLevel workloadLevel) {
        if (workloadLevel == null) {
            throw new IllegalArgumentException("負荷レベルは必須です");
        }
    }

    /**
     * 任意入力項目（案件数、タスク数）のバリデーション
     */
    private void validateOptionalCounts(Integer projectCount, Integer taskCount) {
        if (projectCount != null && projectCount < 0) {
            throw new IllegalArgumentException("案件数は0以上である必要があります");
        }
        if (taskCount != null && taskCount < 0) {
            throw new IllegalArgumentException("タスク数は0以上である必要があります");
        }
    }
}