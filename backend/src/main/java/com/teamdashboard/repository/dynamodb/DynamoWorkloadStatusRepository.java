package com.teamdashboard.repository.dynamodb;

import com.teamdashboard.entity.dynamodb.TeamDashboardItem;
import com.teamdashboard.model.WorkloadStatusModel;
import com.teamdashboard.entity.WorkloadLevel;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@Profile({"lambda", "dynamodb"})
public class DynamoWorkloadStatusRepository {
    
    @Autowired
    private DynamoDbEnhancedClient enhancedClient;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${dynamodb.table.name:TeamDashboard}")
    private String tableName;
    
    private DynamoDbTable<TeamDashboardItem> getTable() {
        return enhancedClient.table(tableName, TableSchema.fromBean(TeamDashboardItem.class));
    }
    
    public List<WorkloadStatusModel> findAll() {
        DynamoDbTable<TeamDashboardItem> table = getTable();
        
        // GSI1を使用してWORKLOADタイプのアイテムを検索
        return table.index("GSI1")
            .query(QueryConditional.keyEqualTo(Key.builder()
                .partitionValue("WORKLOAD")
                .build()))
            .stream()
            .flatMap(page -> page.items().stream())
            .map(this::convertToWorkloadStatus)
            .collect(Collectors.toList());
    }
    
    public WorkloadStatusModel findByUserId(String userId) {
        DynamoDbTable<TeamDashboardItem> table = getTable();
        
        Key key = Key.builder()
            .partitionValue("USER#" + userId)
            .sortValue("WORKLOAD")
            .build();
        
        TeamDashboardItem item = table.getItem(key);
        return item != null ? convertToWorkloadStatus(item) : null;
    }
    
    public WorkloadStatusModel save(WorkloadStatusModel workloadStatus) {
        DynamoDbTable<TeamDashboardItem> table = getTable();
        
        TeamDashboardItem item = convertFromWorkloadStatus(workloadStatus);
        table.putItem(item);
        
        return workloadStatus;
    }
    
    public void deleteByUserId(String userId) {
        DynamoDbTable<TeamDashboardItem> table = getTable();
        
        Key key = Key.builder()
            .partitionValue("USER#" + userId)
            .sortValue("WORKLOAD")
            .build();
        
        table.deleteItem(key);
    }
    
    public List<WorkloadStatusModel> findByWorkloadLevel(WorkloadLevel level) {
        DynamoDbTable<TeamDashboardItem> table = getTable();
        
        return table.scan(ScanEnhancedRequest.builder().build())
            .stream()
            .flatMap(page -> page.items().stream())
            .filter(item -> "WORKLOAD".equals(item.getItemType()))
            .map(this::convertToWorkloadStatus)
            .filter(ws -> ws.getWorkloadLevel() == level)
            .collect(Collectors.toList());
    }
    
    private WorkloadStatusModel convertToWorkloadStatus(TeamDashboardItem item) {
        try {
            Map<String, Object> data = item.getData();
            WorkloadStatusModel workloadStatus = objectMapper.convertValue(data, WorkloadStatusModel.class);
            
            // Instantから LocalDateTimeに変換
            if (item.getUpdatedAt() != null) {
                workloadStatus.setUpdatedAt(
                    LocalDateTime.ofInstant(item.getUpdatedAt(), ZoneOffset.UTC)
                );
            }
            
            return workloadStatus;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert TeamDashboardItem to WorkloadStatusModel", e);
        }
    }
    
    private TeamDashboardItem convertFromWorkloadStatus(WorkloadStatusModel workloadStatus) {
        TeamDashboardItem item = new TeamDashboardItem();
        
        item.setPk("USER#" + workloadStatus.getUserId());
        item.setSk("WORKLOAD");
        item.setGsi1pk("WORKLOAD");
        item.setGsi1sk(workloadStatus.getWorkloadLevel().name() + "#" + workloadStatus.getUserId());
        item.setItemType("WORKLOAD");
        
        // WorkloadStatusをMapに変換
        @SuppressWarnings("unchecked")
        Map<String, Object> data = objectMapper.convertValue(workloadStatus, Map.class);
        item.setData(data);
        
        Instant now = Instant.now();
        item.setUpdatedAt(now);
        if (item.getCreatedAt() == null) {
            item.setCreatedAt(now);
        }
        
        return item;
    }
}