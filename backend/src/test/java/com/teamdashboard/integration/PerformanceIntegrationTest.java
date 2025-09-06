package com.teamdashboard.integration;

import com.teamdashboard.entity.*;
import com.teamdashboard.repository.*;
import com.teamdashboard.service.TeamIssueService;
import com.teamdashboard.service.WorkloadStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * パフォーマンステストと負荷テスト
 * 要件: 4.1, 4.2 - レスポンス時間とスループットの検証
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PerformanceIntegrationTest {

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

    private static final int PERFORMANCE_USER_COUNT = 50;
    private static final int PERFORMANCE_ISSUE_COUNT = 100;
    private static final int CONCURRENT_THREADS = 10;

    @BeforeEach
    void setUp() {
        // 既存データをクリーンアップ
        issueCommentRepository.deleteAll();
        teamIssueRepository.deleteAll();
        workloadStatusRepository.deleteAll();
        dailyReportRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * 大量データでのレスポンス時間テスト
     * 要件: 4.1, 4.2
     */
    @Test
    void testLargeDatasetPerformance() {
        // 1. 大量のテストユーザーを作成
        for (int i = 0; i < PERFORMANCE_USER_COUNT; i++) {
            User user = new User();
            user.setUsername("perfuser" + i);
            user.setDisplayName("パフォーマンステストユーザー" + i);
            user.setEmail("perfuser" + i + "@example.com");
            user.setPassword("password");
            user.setDepartment("テスト部門");
            userRepository.save(user);
        }

        List<User> users = userRepository.findAll();
        assertThat(users).hasSize(PERFORMANCE_USER_COUNT);

        // 2. 各ユーザーの負荷状況を設定
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            WorkloadLevel level = WorkloadLevel.values()[i % 3];
            workloadStatusService.updateWorkloadStatus(user.getId(), level, i % 5 + 1, i % 15 + 5);
        }
        long endTime = System.currentTimeMillis();
        long bulkUpdateTime = endTime - startTime;

        // 大量データの一括更新が5秒以内で完了することを確認
        assertThat(bulkUpdateTime).isLessThan(5000);
        System.out.println("大量負荷状況更新時間: " + bulkUpdateTime + "ms");

        // 3. 負荷状況一覧取得のパフォーマンステスト
        startTime = System.currentTimeMillis();
        List<WorkloadStatus> allStatuses = workloadStatusService.getAllWorkloadStatuses();
        endTime = System.currentTimeMillis();
        long fetchTime = endTime - startTime;

        assertThat(allStatuses).hasSize(PERFORMANCE_USER_COUNT);
        assertThat(fetchTime).isLessThan(1000); // 1秒以内
        System.out.println("大量負荷状況取得時間: " + fetchTime + "ms");

        // 4. 大量の困りごとを作成
        startTime = System.currentTimeMillis();
        for (int i = 0; i < PERFORMANCE_ISSUE_COUNT; i++) {
            User user = users.get(i % users.size());
            teamIssueService.createIssue(
                user.getId(), 
                "パフォーマンステスト用困りごと " + i + ": 大量データでの動作確認を行っています。"
            );
        }
        endTime = System.currentTimeMillis();
        long issueCreationTime = endTime - startTime;

        assertThat(issueCreationTime).isLessThan(10000); // 10秒以内
        System.out.println("大量困りごと作成時間: " + issueCreationTime + "ms");

        // 5. 困りごと一覧取得のパフォーマンステスト
        startTime = System.currentTimeMillis();
        List<TeamIssue> allIssues = teamIssueService.getAllIssues();
        endTime = System.currentTimeMillis();
        long issuesFetchTime = endTime - startTime;

        assertThat(allIssues).hasSize(PERFORMANCE_ISSUE_COUNT);
        assertThat(issuesFetchTime).isLessThan(2000); // 2秒以内
        System.out.println("大量困りごと取得時間: " + issuesFetchTime + "ms");
    }

    /**
     * 同時アクセス負荷テスト
     * 要件: 4.1, 4.2
     */
    @Test
    void testConcurrentAccessPerformance() throws InterruptedException {
        // テストユーザーを作成
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            User user = new User();
            user.setUsername("concurrent" + i);
            user.setDisplayName("同時アクセステストユーザー" + i);
            user.setEmail("concurrent" + i + "@example.com");
            user.setPassword("password");
            user.setDepartment("テスト部門");
            userRepository.save(user);
        }

        List<User> users = userRepository.findAll();
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);

        // 1. 同時負荷状況更新テスト
        long startTime = System.currentTimeMillis();
        
        CompletableFuture<?>[] updateFutures = new CompletableFuture[CONCURRENT_THREADS];
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int index = i;
            updateFutures[i] = CompletableFuture.runAsync(() -> {
                User user = users.get(index);
                for (int j = 0; j < 10; j++) {
                    WorkloadLevel level = WorkloadLevel.values()[j % 3];
                    workloadStatusService.updateWorkloadStatus(user.getId(), level, j % 5 + 1, j % 10 + 5);
                }
            }, executor);
        }

        CompletableFuture.allOf(updateFutures).join();
        long endTime = System.currentTimeMillis();
        long concurrentUpdateTime = endTime - startTime;

        // 同時更新が5秒以内で完了することを確認
        assertThat(concurrentUpdateTime).isLessThan(5000);
        System.out.println("同時負荷状況更新時間: " + concurrentUpdateTime + "ms");

        // 2. 同時困りごと投稿テスト
        startTime = System.currentTimeMillis();
        
        CompletableFuture<?>[] issueFutures = new CompletableFuture[CONCURRENT_THREADS];
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int index = i;
            issueFutures[i] = CompletableFuture.runAsync(() -> {
                User user = users.get(index);
                for (int j = 0; j < 5; j++) {
                    teamIssueService.createIssue(
                        user.getId(), 
                        "同時アクセステスト用困りごと " + index + "-" + j
                    );
                }
            }, executor);
        }

        CompletableFuture.allOf(issueFutures).join();
        endTime = System.currentTimeMillis();
        long concurrentIssueTime = endTime - startTime;

        assertThat(concurrentIssueTime).isLessThan(5000);
        System.out.println("同時困りごと投稿時間: " + concurrentIssueTime + "ms");

        // 3. 同時データ取得テスト
        startTime = System.currentTimeMillis();
        
        CompletableFuture<?>[] fetchFutures = new CompletableFuture[CONCURRENT_THREADS];
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            fetchFutures[i] = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < 10; j++) {
                    workloadStatusService.getAllWorkloadStatuses();
                    teamIssueService.getAllIssues();
                }
            }, executor);
        }

        CompletableFuture.allOf(fetchFutures).join();
        endTime = System.currentTimeMillis();
        long concurrentFetchTime = endTime - startTime;

        assertThat(concurrentFetchTime).isLessThan(3000);
        System.out.println("同時データ取得時間: " + concurrentFetchTime + "ms");

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // データ整合性の確認
        List<WorkloadStatus> finalStatuses = workloadStatusService.getAllWorkloadStatuses();
        List<TeamIssue> finalIssues = teamIssueService.getAllIssues();
        
        assertThat(finalStatuses).hasSize(CONCURRENT_THREADS);
        assertThat(finalIssues).hasSize(CONCURRENT_THREADS * 5);
    }

    /**
     * メモリ使用量とガベージコレクション影響テスト
     * 要件: 4.1, 4.2
     */
    @Test
    void testMemoryUsageAndGarbageCollection() {
        Runtime runtime = Runtime.getRuntime();
        
        // 初期メモリ状態を記録
        runtime.gc();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // 大量のユーザーとデータを作成
        for (int i = 0; i < 100; i++) {
            User user = new User();
            user.setUsername("memtest" + i);
            user.setDisplayName("メモリテストユーザー" + i);
            user.setEmail("memtest" + i + "@example.com");
            user.setPassword("password");
            user.setDepartment("テスト部門");
            userRepository.save(user);
        }

        List<User> users = userRepository.findAll();
        
        // 大量の操作を実行
        for (int cycle = 0; cycle < 10; cycle++) {
            for (User user : users) {
                workloadStatusService.updateWorkloadStatus(
                    user.getId(), 
                    WorkloadLevel.values()[cycle % 3], 
                    cycle % 5 + 1, 
                    cycle % 10 + 5
                );
                
                teamIssueService.createIssue(
                    user.getId(), 
                    "メモリテスト用困りごと " + cycle + " - " + user.getUsername()
                );
            }
            
            // 定期的にデータを取得
            workloadStatusService.getAllWorkloadStatuses();
            teamIssueService.getAllIssues();
        }

        // ガベージコレクションを実行
        runtime.gc();
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        // メモリ使用量の増加が100MB以内であることを確認
        assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024);
        System.out.println("メモリ使用量増加: " + (memoryIncrease / 1024 / 1024) + "MB");
        
        // データが正しく作成されていることを確認
        List<WorkloadStatus> statuses = workloadStatusService.getAllWorkloadStatuses();
        List<TeamIssue> issues = teamIssueService.getAllIssues();
        
        assertThat(statuses).hasSize(100);
        assertThat(issues).hasSize(1000); // 100 users * 10 cycles
    }

    /**
     * データベース接続プールとトランザクション性能テスト
     * 要件: 4.1, 4.2
     */
    @Test
    void testDatabaseConnectionPoolPerformance() throws InterruptedException {
        // テストユーザーを作成
        User testUser = new User();
        testUser.setUsername("dbpooltest");
        testUser.setDisplayName("DB接続プールテストユーザー");
        testUser.setEmail("dbpooltest@example.com");
        testUser.setPassword("password");
        testUser.setDepartment("テスト部門");
        testUser = userRepository.save(testUser);

        ExecutorService executor = Executors.newFixedThreadPool(20);
        final User finalUser = testUser;

        // 大量の同時データベース操作
        long startTime = System.currentTimeMillis();
        
        CompletableFuture<?>[] dbFutures = new CompletableFuture[20];
        for (int i = 0; i < 20; i++) {
            final int threadIndex = i;
            dbFutures[i] = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < 50; j++) {
                    // 負荷状況の更新
                    workloadStatusService.updateWorkloadStatus(
                        finalUser.getId(), 
                        WorkloadLevel.values()[j % 3], 
                        j % 5 + 1, 
                        j % 10 + 5
                    );
                    
                    // 困りごとの作成
                    TeamIssue issue = teamIssueService.createIssue(
                        finalUser.getId(), 
                        "DB接続プールテスト " + threadIndex + "-" + j
                    );
                    
                    // コメントの追加
                    teamIssueService.addComment(
                        issue.getId(), 
                        finalUser.getId(), 
                        "テストコメント " + threadIndex + "-" + j
                    );
                    
                    // データの取得
                    workloadStatusService.getWorkloadStatusByUserId(finalUser.getId());
                    teamIssueService.getCommentsByIssueId(issue.getId());
                }
            }, executor);
        }

        CompletableFuture.allOf(dbFutures).join();
        long endTime = System.currentTimeMillis();
        long dbOperationTime = endTime - startTime;

        // 大量のDB操作が30秒以内で完了することを確認
        assertThat(dbOperationTime).isLessThan(30000);
        System.out.println("DB接続プール性能テスト時間: " + dbOperationTime + "ms");

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // データ整合性の確認
        List<TeamIssue> issues = teamIssueService.getAllIssues();
        assertThat(issues).hasSize(1000); // 20 threads * 50 operations

        long totalComments = issues.stream()
                .mapToLong(issue -> teamIssueService.getCommentsByIssueId(issue.getId()).size())
                .sum();
        assertThat(totalComments).isEqualTo(1000);
    }
}