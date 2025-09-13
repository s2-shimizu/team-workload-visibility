package com.teamdashboard.config;

import com.teamdashboard.model.WorkloadStatusModel;
import com.teamdashboard.model.TeamIssueModel;
import com.teamdashboard.entity.WorkloadLevel;
import com.teamdashboard.entity.IssueStatus;
import com.teamdashboard.repository.dynamodb.DynamoWorkloadStatusRepository;
import com.teamdashboard.repository.dynamodb.DynamoTeamIssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Profile({"dynamodb", "lambda"})
@Order(2) // DynamoDBTableCreatorの後に実行
public class DynamoDBTestDataInitializer implements CommandLineRunner {
    
    @Autowired
    private DynamoWorkloadStatusRepository workloadStatusRepository;
    
    @Autowired
    private DynamoTeamIssueRepository teamIssueRepository;
    
    @Override
    public void run(String... args) throws Exception {
        try {
            initializeTestData();
        } catch (Exception e) {
            System.err.println("DynamoDBテストデータ初期化をスキップしました（DynamoDBローカルが利用できません）: " + e.getMessage());
        }
    }
    
    private void initializeTestData() {
        System.out.println("DynamoDBにテストデータを初期化中...");
        
        try {
            // 既存データをチェック
            if (!workloadStatusRepository.findAll().isEmpty()) {
                System.out.println("テストデータは既に存在します。初期化をスキップします。");
                return;
            }
            
            // 負荷状況テストデータ
            createWorkloadStatusTestData();
            
            // 困りごとテストデータ
            createTeamIssueTestData();
            
            System.out.println("DynamoDBテストデータの初期化が完了しました");
            
        } catch (Exception e) {
            System.err.println("テストデータの初期化に失敗しました: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createWorkloadStatusTestData() {
        // ユーザー1: 田中太郎 - 中程度の負荷
        WorkloadStatusModel workload1 = new WorkloadStatusModel();
        workload1.setUserId("user1");
        workload1.setDisplayName("田中太郎");
        workload1.setWorkloadLevel(WorkloadLevel.MEDIUM);
        workload1.setProjectCount(3);
        workload1.setTaskCount(15);
        workload1.setUpdatedAt(LocalDateTime.now().minusHours(2));
        workloadStatusRepository.save(workload1);
        
        // ユーザー2: 佐藤花子 - 高負荷
        WorkloadStatusModel workload2 = new WorkloadStatusModel();
        workload2.setUserId("user2");
        workload2.setDisplayName("佐藤花子");
        workload2.setWorkloadLevel(WorkloadLevel.HIGH);
        workload2.setProjectCount(5);
        workload2.setTaskCount(25);
        workload2.setUpdatedAt(LocalDateTime.now().minusHours(1));
        workloadStatusRepository.save(workload2);
        
        // ユーザー3: 鈴木一郎 - 低負荷
        WorkloadStatusModel workload3 = new WorkloadStatusModel();
        workload3.setUserId("user3");
        workload3.setDisplayName("鈴木一郎");
        workload3.setWorkloadLevel(WorkloadLevel.LOW);
        workload3.setProjectCount(1);
        workload3.setTaskCount(5);
        workload3.setUpdatedAt(LocalDateTime.now().minusMinutes(30));
        workloadStatusRepository.save(workload3);
        
        // ユーザー4: 高橋美咲 - 中程度の負荷
        WorkloadStatusModel workload4 = new WorkloadStatusModel();
        workload4.setUserId("user4");
        workload4.setDisplayName("高橋美咲");
        workload4.setWorkloadLevel(WorkloadLevel.MEDIUM);
        workload4.setProjectCount(2);
        workload4.setTaskCount(12);
        workload4.setUpdatedAt(LocalDateTime.now().minusMinutes(15));
        workloadStatusRepository.save(workload4);
        
        System.out.println("負荷状況テストデータを4件作成しました");
    }
    
    private void createTeamIssueTestData() {
        // 困りごと1: 技術的な問題（未解決）
        TeamIssueModel issue1 = new TeamIssueModel();
        issue1.setIssueId(UUID.randomUUID().toString());
        issue1.setUserId("user1");
        issue1.setDisplayName("田中太郎");
        issue1.setContent("新しい技術の学習で詰まっています。React Hooksの使い方がよくわからず、コンポーネントの状態管理で困っています。特にuseEffectの依存配列の設定で無限ループが発生してしまいます。");
        issue1.setStatus(IssueStatus.OPEN);
        issue1.setCreatedAt(LocalDateTime.now().minusHours(6));
        teamIssueRepository.save(issue1);
        
        // 困りごと2: プロジェクト管理の問題（解決済み）
        TeamIssueModel issue2 = new TeamIssueModel();
        issue2.setIssueId(UUID.randomUUID().toString());
        issue2.setUserId("user2");
        issue2.setDisplayName("佐藤花子");
        issue2.setContent("プロジェクトの進め方で悩んでいます。複数の案件を並行して進める中で、タスクの優先順位をどう決めればよいかアドバイスをください。");
        issue2.setStatus(IssueStatus.RESOLVED);
        issue2.setCreatedAt(LocalDateTime.now().minusDays(1));
        issue2.setResolvedAt(LocalDateTime.now().minusHours(3));
        teamIssueRepository.save(issue2);
        
        // 困りごと3: コミュニケーションの問題（未解決）
        TeamIssueModel issue3 = new TeamIssueModel();
        issue3.setIssueId(UUID.randomUUID().toString());
        issue3.setUserId("user3");
        issue3.setDisplayName("鈴木一郎");
        issue3.setContent("リモートワークでのコミュニケーションに課題を感じています。チームメンバーとの連携がうまくいかず、情報共有のタイミングが難しいです。");
        issue3.setStatus(IssueStatus.OPEN);
        issue3.setCreatedAt(LocalDateTime.now().minusHours(4));
        teamIssueRepository.save(issue3);
        
        // 困りごと4: 技術選定の問題（未解決）
        TeamIssueModel issue4 = new TeamIssueModel();
        issue4.setIssueId(UUID.randomUUID().toString());
        issue4.setUserId("user4");
        issue4.setDisplayName("高橋美咲");
        issue4.setContent("新機能の実装で使用する技術スタックの選定で迷っています。パフォーマンスと開発効率のバランスを考慮した最適な選択肢について相談したいです。");
        issue4.setStatus(IssueStatus.OPEN);
        issue4.setCreatedAt(LocalDateTime.now().minusHours(2));
        teamIssueRepository.save(issue4);
        
        System.out.println("困りごとテストデータを4件作成しました");
    }
}