package com.teamdashboard.integration;

import com.teamdashboard.LambdaApplication;
import com.teamdashboard.dto.WorkloadStatusRequestDTO;
import com.teamdashboard.dto.WorkloadStatusResponseDTO;
import com.teamdashboard.entity.User;
import com.teamdashboard.entity.WorkloadLevel;
import com.teamdashboard.entity.WorkloadStatus;
import com.teamdashboard.repository.UserRepository;
import com.teamdashboard.repository.WorkloadStatusRepository;
import com.teamdashboard.service.WorkloadStatusService;
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
 * 負荷状況機能の統合テスト（サービスレイヤー）
 * 要件: 1.1, 1.3, 2.1, 2.3
 */
@SpringBootTest(classes = LambdaApplication.class)
@ActiveProfiles("test")
@Transactional
public class WorkloadStatusServiceIntegrationTest {

    @Autowired
    private WorkloadStatusService workloadStatusService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkloadStatusRepository workloadStatusRepository;

    private User testUser1;
    private User testUser2;
    private User testUser3;

    @BeforeEach
    void setUp() {
        // 既存データをクリーンアップ
        workloadStatusRepository.deleteAll();
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
     * 負荷状況更新から表示までの一連の流れをテスト
     * 要件: 2.1, 2.3
     */
    @Test
    void testWorkloadStatusUpdateAndDisplay() {
        // 1. 負荷状況を更新
        WorkloadStatusRequestDTO request = new WorkloadStatusRequestDTO();
        request.setWorkloadLevel(WorkloadLevel.HIGH);
        request.setProjectCount(3);
        request.setTaskCount(15);

        WorkloadStatus updateResponse = workloadStatusService.updateWorkloadStatus(
            testUser1.getId(), 
            request.getWorkloadLevel(), 
            request.getProjectCount(), 
            request.getTaskCount()
        );

        // レスポンスの詳細確認
        assertThat(updateResponse).isNotNull();
        assertThat(updateResponse.getWorkloadLevel()).isEqualTo(WorkloadLevel.HIGH);
        assertThat(updateResponse.getProjectCount()).isEqualTo(3);
        assertThat(updateResponse.getTaskCount()).isEqualTo(15);
        assertThat(updateResponse.getUser().getUsername()).isEqualTo("testuser1");
        assertThat(updateResponse.getUpdatedAt()).isNotNull();

        // 2. 個人の負荷状況を取得して確認
        Optional<WorkloadStatus> myStatusOpt = workloadStatusService.getWorkloadStatusByUserId(testUser1.getId());
        assertThat(myStatusOpt).isPresent();
        WorkloadStatus myStatus = myStatusOpt.get();
        assertThat(myStatus.getWorkloadLevel()).isEqualTo(WorkloadLevel.HIGH);
        assertThat(myStatus.getProjectCount()).isEqualTo(3);
        assertThat(myStatus.getTaskCount()).isEqualTo(15);
        assertThat(myStatus.getUser().getUsername()).isEqualTo("testuser1");

        // 3. 全体の負荷状況一覧に反映されているか確認
        List<WorkloadStatus> allStatuses = workloadStatusService.getAllWorkloadStatuses();
        assertThat(allStatuses).hasSize(1);
        WorkloadStatus status = allStatuses.get(0);
        assertThat(status.getWorkloadLevel()).isEqualTo(WorkloadLevel.HIGH);
        assertThat(status.getUser().getUsername()).isEqualTo("testuser1");

        // 4. データベースに正しく保存されているか確認
        List<WorkloadStatus> savedStatuses = workloadStatusRepository.findAll();
        assertThat(savedStatuses).hasSize(1);
        WorkloadStatus savedStatus = savedStatuses.get(0);
        assertThat(savedStatus.getWorkloadLevel()).isEqualTo(WorkloadLevel.HIGH);
        assertThat(savedStatus.getProjectCount()).isEqualTo(3);
        assertThat(savedStatus.getTaskCount()).isEqualTo(15);
        assertThat(savedStatus.getUser().getUsername()).isEqualTo("testuser1");
        assertThat(savedStatus.getUpdatedAt()).isNotNull();
    }

    /**
     * 複数ユーザーでの負荷状況表示テストを実装
     * 要件: 1.1, 1.3
     */
    @Test
    void testMultipleUsersWorkloadStatusDisplay() {
        // 複数ユーザーの負荷状況を事前に設定
        LocalDateTime now = LocalDateTime.now();
        
        // ユーザー1: 高負荷
        WorkloadStatus status1 = new WorkloadStatus();
        status1.setUser(testUser1);
        status1.setWorkloadLevel(WorkloadLevel.HIGH);
        status1.setProjectCount(4);
        status1.setTaskCount(20);
        status1.setUpdatedAt(now.minusHours(1));
        workloadStatusRepository.save(status1);

        // ユーザー2: 中負荷
        WorkloadStatus status2 = new WorkloadStatus();
        status2.setUser(testUser2);
        status2.setWorkloadLevel(WorkloadLevel.MEDIUM);
        status2.setProjectCount(2);
        status2.setTaskCount(8);
        status2.setUpdatedAt(now.minusHours(2));
        workloadStatusRepository.save(status2);

        // ユーザー3: 低負荷
        WorkloadStatus status3 = new WorkloadStatus();
        status3.setUser(testUser3);
        status3.setWorkloadLevel(WorkloadLevel.LOW);
        status3.setProjectCount(1);
        status3.setTaskCount(3);
        status3.setUpdatedAt(now.minusHours(3));
        workloadStatusRepository.save(status3);

        // 全ユーザーの負荷状況を取得
        List<WorkloadStatus> responses = workloadStatusService.getAllWorkloadStatuses();

        // 各ユーザーの負荷状況が正しく表示されているか確認
        assertThat(responses).hasSize(3);
        
        // ユーザー1の確認
        WorkloadStatus user1Response = responses.stream()
                .filter(r -> r.getUser().getUsername().equals("testuser1"))
                .findFirst().orElseThrow();
        assertThat(user1Response.getWorkloadLevel()).isEqualTo(WorkloadLevel.HIGH);
        assertThat(user1Response.getProjectCount()).isEqualTo(4);
        assertThat(user1Response.getTaskCount()).isEqualTo(20);

        // ユーザー2の確認
        WorkloadStatus user2Response = responses.stream()
                .filter(r -> r.getUser().getUsername().equals("testuser2"))
                .findFirst().orElseThrow();
        assertThat(user2Response.getWorkloadLevel()).isEqualTo(WorkloadLevel.MEDIUM);
        assertThat(user2Response.getProjectCount()).isEqualTo(2);
        assertThat(user2Response.getTaskCount()).isEqualTo(8);

        // ユーザー3の確認
        WorkloadStatus user3Response = responses.stream()
                .filter(r -> r.getUser().getUsername().equals("testuser3"))
                .findFirst().orElseThrow();
        assertThat(user3Response.getWorkloadLevel()).isEqualTo(WorkloadLevel.LOW);
        assertThat(user3Response.getProjectCount()).isEqualTo(1);
        assertThat(user3Response.getTaskCount()).isEqualTo(3);

        // 更新日時が正しく設定されているか確認
        responses.forEach(response -> {
            assertThat(response.getUpdatedAt()).isNotNull();
        });
    }

    /**
     * 負荷レベル別の色分け表示テストを実行
     * 要件: 1.1, 1.3
     */
    @Test
    void testWorkloadLevelColorCoding() {
        // 各負荷レベルのテストデータを作成
        LocalDateTime now = LocalDateTime.now();

        // 高負荷ユーザー
        WorkloadStatus highStatus = new WorkloadStatus();
        highStatus.setUser(testUser1);
        highStatus.setWorkloadLevel(WorkloadLevel.HIGH);
        highStatus.setProjectCount(5);
        highStatus.setTaskCount(25);
        highStatus.setUpdatedAt(now);
        workloadStatusRepository.save(highStatus);

        // 中負荷ユーザー
        WorkloadStatus mediumStatus = new WorkloadStatus();
        mediumStatus.setUser(testUser2);
        mediumStatus.setWorkloadLevel(WorkloadLevel.MEDIUM);
        mediumStatus.setProjectCount(3);
        mediumStatus.setTaskCount(12);
        mediumStatus.setUpdatedAt(now);
        workloadStatusRepository.save(mediumStatus);

        // 低負荷ユーザー
        WorkloadStatus lowStatus = new WorkloadStatus();
        lowStatus.setUser(testUser3);
        lowStatus.setWorkloadLevel(WorkloadLevel.LOW);
        lowStatus.setProjectCount(1);
        lowStatus.setTaskCount(5);
        lowStatus.setUpdatedAt(now);
        workloadStatusRepository.save(lowStatus);

        // サービスから負荷状況を取得
        List<WorkloadStatus> responses = workloadStatusService.getAllWorkloadStatuses();

        assertThat(responses).hasSize(3);

        // 各負荷レベルが正しく返されているか確認
        long highCount = responses.stream().filter(r -> r.getWorkloadLevel() == WorkloadLevel.HIGH).count();
        long mediumCount = responses.stream().filter(r -> r.getWorkloadLevel() == WorkloadLevel.MEDIUM).count();
        long lowCount = responses.stream().filter(r -> r.getWorkloadLevel() == WorkloadLevel.LOW).count();

        assertThat(highCount).isEqualTo(1);
        assertThat(mediumCount).isEqualTo(1);
        assertThat(lowCount).isEqualTo(1);

        // 高負荷ユーザーの詳細確認（警告表示対象）
        WorkloadStatus highWorkloadUser = responses.stream()
                .filter(r -> r.getWorkloadLevel() == WorkloadLevel.HIGH)
                .findFirst().orElseThrow();
        
        assertThat(highWorkloadUser.getUser().getUsername()).isEqualTo("testuser1");
        assertThat(highWorkloadUser.getProjectCount()).isEqualTo(5);
        assertThat(highWorkloadUser.getTaskCount()).isEqualTo(25);
    }

    /**
     * 負荷状況の更新日時表示テスト
     * 要件: 1.3
     */
    @Test
    void testWorkloadStatusTimestampDisplay() {
        // 負荷状況を更新
        WorkloadStatusRequestDTO request = new WorkloadStatusRequestDTO();
        request.setWorkloadLevel(WorkloadLevel.MEDIUM);
        request.setProjectCount(2);
        request.setTaskCount(10);

        LocalDateTime beforeUpdate = LocalDateTime.now();

        WorkloadStatus response = workloadStatusService.updateWorkloadStatus(
            testUser1.getId(), 
            request.getWorkloadLevel(), 
            request.getProjectCount(), 
            request.getTaskCount()
        );

        LocalDateTime afterUpdate = LocalDateTime.now();

        // 更新日時が適切な範囲内にあるか確認
        LocalDateTime updatedAt = response.getUpdatedAt();
        assertThat(updatedAt).isNotNull();
        assertThat(updatedAt).isAfterOrEqualTo(beforeUpdate.minusSeconds(1));
        assertThat(updatedAt).isBeforeOrEqualTo(afterUpdate.plusSeconds(1));
    }

    /**
     * バリデーションエラーのテスト
     * 要件: 2.1
     */
    @Test
    void testWorkloadStatusValidation() {
        // 必須項目なしでリクエスト
        WorkloadStatusRequestDTO invalidRequest = new WorkloadStatusRequestDTO();
        // workloadLevelを設定しない

        try {
            workloadStatusService.updateWorkloadStatus(testUser1.getId(), null, null, null);
            // バリデーションエラーが発生することを期待
            assertThat(false).as("バリデーションエラーが発生するべき").isTrue();
        } catch (Exception e) {
            // バリデーションエラーが発生することを確認
            assertThat(e).isNotNull();
        }
    }

    /**
     * 存在しないユーザーでのテスト
     */
    @Test
    void testWorkloadStatusWithNonExistentUser() {
        WorkloadStatusRequestDTO request = new WorkloadStatusRequestDTO();
        request.setWorkloadLevel(WorkloadLevel.HIGH);
        request.setProjectCount(3);
        request.setTaskCount(15);

        try {
            workloadStatusService.updateWorkloadStatus(999L, request.getWorkloadLevel(), request.getProjectCount(), request.getTaskCount());
            // ユーザーが存在しないエラーが発生することを期待
            assertThat(false).as("ユーザーが存在しないエラーが発生するべき").isTrue();
        } catch (Exception e) {
            // エラーが発生することを確認
            assertThat(e).isNotNull();
        }
    }
}