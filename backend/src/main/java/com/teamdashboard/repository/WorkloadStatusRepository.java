package com.teamdashboard.repository;

import com.teamdashboard.model.WorkloadStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class WorkloadStatusRepository {

    private final DynamoDbTable<WorkloadStatus> workloadStatusTable;

    @Autowired
    public WorkloadStatusRepository(DynamoDbEnhancedClient enhancedClient,
                                   @Value("${aws.dynamodb.tables.workload-status:WorkloadStatus}") String tableName) {
        this.workloadStatusTable = enhancedClient.table(tableName, 
                                                        TableSchema.fromBean(WorkloadStatus.class));
    }

    public WorkloadStatus save(WorkloadStatus workloadStatus) {
        try {
            workloadStatus.updateTimestamp();
            workloadStatusTable.putItem(workloadStatus);
            return workloadStatus;
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to save workload status: " + e.getMessage(), e);
        }
    }

    public Optional<WorkloadStatus> findByUserId(String userId) {
        try {
            Key key = Key.builder()
                    .partitionValue(userId)
                    .build();
            
            WorkloadStatus item = workloadStatusTable.getItem(key);
            return Optional.ofNullable(item);
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to find workload status by userId: " + e.getMessage(), e);
        }
    }

    public List<WorkloadStatus> findAll() {
        try {
            return workloadStatusTable.scan(ScanEnhancedRequest.builder().build())
                    .items()
                    .stream()
                    .collect(Collectors.toList());
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to scan workload statuses: " + e.getMessage(), e);
        }
    }

    public void deleteByUserId(String userId) {
        try {
            Key key = Key.builder()
                    .partitionValue(userId)
                    .build();
            
            workloadStatusTable.deleteItem(key);
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to delete workload status: " + e.getMessage(), e);
        }
    }

    public boolean existsByUserId(String userId) {
        return findByUserId(userId).isPresent();
    }

    public long count() {
        try {
            return workloadStatusTable.scan(ScanEnhancedRequest.builder().build())
                    .items()
                    .stream()
                    .count();
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to count workload statuses: " + e.getMessage(), e);
        }
    }

    // テーブル作成用のヘルパーメソッド（開発・テスト用）
    public void createTableIfNotExists() {
        try {
            workloadStatusTable.createTable();
        } catch (Exception e) {
            // テーブルが既に存在する場合は無視
            System.out.println("WorkloadStatus table may already exist: " + e.getMessage());
        }
    }
}