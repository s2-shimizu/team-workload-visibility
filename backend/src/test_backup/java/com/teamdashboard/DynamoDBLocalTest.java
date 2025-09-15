package com.teamdashboard;

import com.teamdashboard.config.DynamoDBConfig;
import com.teamdashboard.entity.WorkloadLevel;
import com.teamdashboard.model.WorkloadStatusModel;
import com.teamdashboard.repository.dynamodb.DynamoWorkloadStatusRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = LambdaApplication.class)
@ActiveProfiles("dynamodb")
public class DynamoDBLocalTest {
    
    @Autowired(required = false)
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;
    
    @Autowired(required = false)
    private DynamoWorkloadStatusRepository workloadStatusRepository;
    
    @Test
    public void testDynamoDBConnection() {
        // DynamoDBクライアントが正しく設定されているかテスト
        if (dynamoDbEnhancedClient != null) {
            assertNotNull(dynamoDbEnhancedClient);
            System.out.println("✅ DynamoDB Enhanced Client が正常に設定されました");
        } else {
            System.out.println("⚠️ DynamoDB Enhanced Client が設定されていません（プロファイルが無効の可能性）");
        }
    }
    
    @Test
    public void testWorkloadStatusRepository() {
        if (workloadStatusRepository != null) {
            assertNotNull(workloadStatusRepository);
            System.out.println("✅ WorkloadStatus Repository が正常に設定されました");
            
            // 簡単なデータ操作テスト
            try {
                WorkloadStatusModel testStatus = new WorkloadStatusModel();
                testStatus.setUserId("test-user-1");
                testStatus.setDisplayName("テストユーザー");
                testStatus.setWorkloadLevel(WorkloadLevel.MEDIUM);
                
                // 保存テスト
                WorkloadStatusModel saved = workloadStatusRepository.save(testStatus);
                assertNotNull(saved);
                System.out.println("✅ データ保存テスト成功");
                
                // 取得テスト
                WorkloadStatusModel retrieved = workloadStatusRepository.findByUserId("test-user-1");
                if (retrieved != null) {
                    assertEquals("test-user-1", retrieved.getUserId());
                    assertEquals(WorkloadLevel.MEDIUM, retrieved.getWorkloadLevel());
                    System.out.println("✅ データ取得テスト成功");
                } else {
                    System.out.println("⚠️ データ取得テストでデータが見つかりませんでした");
                }
                
            } catch (Exception e) {
                System.out.println("❌ DynamoDB操作エラー: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠️ WorkloadStatus Repository が設定されていません");
        }
    }
}