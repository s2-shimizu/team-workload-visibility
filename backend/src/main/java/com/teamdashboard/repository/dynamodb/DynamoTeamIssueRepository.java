package com.teamdashboard.repository.dynamodb;

import com.teamdashboard.entity.dynamodb.TeamDashboardItem;
import com.teamdashboard.model.TeamIssueModel;
import com.teamdashboard.entity.IssueStatus;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Profile({"lambda", "dynamodb"})
public class DynamoTeamIssueRepository {
    
    @Autowired
    private DynamoDbEnhancedClient enhancedClient;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${dynamodb.table.name:TeamDashboard}")
    private String tableName;
    
    private DynamoDbTable<TeamDashboardItem> getTable() {
        return enhancedClient.table(tableName, TableSchema.fromBean(TeamDashboardItem.class));
    }
    
    public List<TeamIssueModel> findAll() {
        DynamoDbTable<TeamDashboardItem> table = getTable();
        
        // GSI1を使用してISSUEタイプのアイテムを検索
        return table.index("GSI1")
            .query(QueryConditional.keyEqualTo(Key.builder()
                .partitionValue("ISSUE")
                .build()))
            .stream()
            .flatMap(page -> page.items().stream())
            .map(this::convertToTeamIssue)
            .collect(Collectors.toList());
    }
    
    public Optional<TeamIssueModel> findById(String issueId) {
        DynamoDbTable<TeamDashboardItem> table = getTable();
        
        Key key = Key.builder()
            .partitionValue("ISSUE#" + issueId)
            .sortValue("METADATA")
            .build();
        
        TeamDashboardItem item = table.getItem(key);
        return item != null ? Optional.of(convertToTeamIssue(item)) : Optional.empty();
    }
    
    public TeamIssueModel save(TeamIssueModel teamIssue) {
        DynamoDbTable<TeamDashboardItem> table = getTable();
        
        TeamDashboardItem item = convertFromTeamIssue(teamIssue);
        table.putItem(item);
        
        return teamIssue;
    }
    
    public void deleteById(String issueId) {
        DynamoDbTable<TeamDashboardItem> table = getTable();
        
        Key key = Key.builder()
            .partitionValue("ISSUE#" + issueId)
            .sortValue("METADATA")
            .build();
        
        table.deleteItem(key);
    }
    
    public List<TeamIssueModel> findByStatus(IssueStatus status) {
        return findAll().stream()
            .filter(issue -> issue.getStatus() == status)
            .collect(Collectors.toList());
    }
    
    public List<TeamIssueModel> findByUserId(String userId) {
        return findAll().stream()
            .filter(issue -> userId.equals(issue.getUserId()))
            .collect(Collectors.toList());
    }
    
    private TeamIssueModel convertToTeamIssue(TeamDashboardItem item) {
        try {
            Map<String, Object> data = item.getData();
            TeamIssueModel teamIssue = objectMapper.convertValue(data, TeamIssueModel.class);
            
            // Instantから LocalDateTimeに変換
            if (item.getCreatedAt() != null) {
                teamIssue.setCreatedAt(
                    LocalDateTime.ofInstant(item.getCreatedAt(), ZoneOffset.UTC)
                );
            }
            if (item.getUpdatedAt() != null && teamIssue.getResolvedAt() != null) {
                teamIssue.setResolvedAt(
                    LocalDateTime.ofInstant(item.getUpdatedAt(), ZoneOffset.UTC)
                );
            }
            
            return teamIssue;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert TeamDashboardItem to TeamIssueModel", e);
        }
    }
    
    private TeamDashboardItem convertFromTeamIssue(TeamIssueModel teamIssue) {
        TeamDashboardItem item = new TeamDashboardItem();
        
        item.setPk("ISSUE#" + teamIssue.getIssueId());
        item.setSk("METADATA");
        item.setGsi1pk("ISSUE");
        item.setGsi1sk(teamIssue.getStatus().name() + "#" + teamIssue.getCreatedAt().toInstant(ZoneOffset.UTC).toString());
        item.setItemType("ISSUE");
        
        // TeamIssueをMapに変換
        @SuppressWarnings("unchecked")
        Map<String, Object> data = objectMapper.convertValue(teamIssue, Map.class);
        item.setData(data);
        
        Instant now = Instant.now();
        item.setUpdatedAt(now);
        if (item.getCreatedAt() == null) {
            item.setCreatedAt(teamIssue.getCreatedAt().toInstant(ZoneOffset.UTC));
        }
        
        return item;
    }
}