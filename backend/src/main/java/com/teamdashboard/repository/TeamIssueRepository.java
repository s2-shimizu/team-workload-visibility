package com.teamdashboard.repository;

import com.teamdashboard.model.TeamIssue;
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
public class TeamIssueRepository {

    private final DynamoDbTable<TeamIssue> teamIssueTable;

    @Autowired
    public TeamIssueRepository(DynamoDbEnhancedClient enhancedClient,
                              @Value("${aws.dynamodb.tables.team-issue:TeamIssue}") String tableName) {
        this.teamIssueTable = enhancedClient.table(tableName, 
                                                   TableSchema.fromBean(TeamIssue.class));
    }

    public TeamIssue save(TeamIssue teamIssue) {
        try {
            if (teamIssue.getIssueId() == null) {
                teamIssue.generateIssueId();
            }
            teamIssue.updateTimestamp();
            teamIssueTable.putItem(teamIssue);
            return teamIssue;
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to save team issue: " + e.getMessage(), e);
        }
    }

    public Optional<TeamIssue> findByIssueId(String issueId) {
        try {
            Key key = Key.builder()
                    .partitionValue(issueId)
                    .build();
            
            TeamIssue item = teamIssueTable.getItem(key);
            return Optional.ofNullable(item);
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to find team issue by issueId: " + e.getMessage(), e);
        }
    }

    public List<TeamIssue> findAll() {
        try {
            return teamIssueTable.scan(ScanEnhancedRequest.builder().build())
                    .items()
                    .stream()
                    .collect(Collectors.toList());
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to scan team issues: " + e.getMessage(), e);
        }
    }

    public List<TeamIssue> findByStatus(String status) {
        try {
            return teamIssueTable.scan(ScanEnhancedRequest.builder().build())
                    .items()
                    .stream()
                    .filter(issue -> status.equals(issue.getStatus()))
                    .collect(Collectors.toList());
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to find team issues by status: " + e.getMessage(), e);
        }
    }

    public List<TeamIssue> findByUserId(String userId) {
        try {
            return teamIssueTable.scan(ScanEnhancedRequest.builder().build())
                    .items()
                    .stream()
                    .filter(issue -> userId.equals(issue.getUserId()))
                    .collect(Collectors.toList());
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to find team issues by userId: " + e.getMessage(), e);
        }
    }

    public List<TeamIssue> findByPriority(String priority) {
        try {
            return teamIssueTable.scan(ScanEnhancedRequest.builder().build())
                    .items()
                    .stream()
                    .filter(issue -> priority.equals(issue.getPriority()))
                    .collect(Collectors.toList());
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to find team issues by priority: " + e.getMessage(), e);
        }
    }

    public void deleteByIssueId(String issueId) {
        try {
            Key key = Key.builder()
                    .partitionValue(issueId)
                    .build();
            
            teamIssueTable.deleteItem(key);
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to delete team issue: " + e.getMessage(), e);
        }
    }

    public boolean existsByIssueId(String issueId) {
        return findByIssueId(issueId).isPresent();
    }

    public long count() {
        try {
            return teamIssueTable.scan(ScanEnhancedRequest.builder().build())
                    .items()
                    .stream()
                    .count();
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to count team issues: " + e.getMessage(), e);
        }
    }

    public long countByStatus(String status) {
        try {
            return teamIssueTable.scan(ScanEnhancedRequest.builder().build())
                    .items()
                    .stream()
                    .filter(issue -> status.equals(issue.getStatus()))
                    .count();
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to count team issues by status: " + e.getMessage(), e);
        }
    }

    public long countByPriority(String priority) {
        try {
            return teamIssueTable.scan(ScanEnhancedRequest.builder().build())
                    .items()
                    .stream()
                    .filter(issue -> priority.equals(issue.getPriority()))
                    .count();
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to count team issues by priority: " + e.getMessage(), e);
        }
    }

    // テーブル作成用のヘルパーメソッド（開発・テスト用）
    public void createTableIfNotExists() {
        try {
            teamIssueTable.createTable();
        } catch (Exception e) {
            // テーブルが既に存在する場合は無視
            System.out.println("TeamIssue table may already exist: " + e.getMessage());
        }
    }
}