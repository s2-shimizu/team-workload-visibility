package com.teamdashboard.service;

import com.teamdashboard.model.TeamIssueModel;
import com.teamdashboard.model.IssueCommentModel;
import com.teamdashboard.entity.IssueStatus;
import com.teamdashboard.repository.dynamodb.DynamoTeamIssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Profile({"lambda", "dynamodb"})
public class DynamoTeamIssueService {
    
    @Autowired
    private DynamoTeamIssueRepository repository;
    
    public List<TeamIssueModel> getAllTeamIssues() {
        return repository.findAll();
    }
    
    public Optional<TeamIssueModel> getTeamIssueById(String issueId) {
        return repository.findById(issueId);
    }
    
    public TeamIssueModel createTeamIssue(String userId, String displayName, String content) {
        // バリデーション
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new IllegalArgumentException("Display name is required");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content is required");
        }
        if (content.length() > 1000) {
            throw new IllegalArgumentException("Content must be 1000 characters or less");
        }
        
        TeamIssueModel teamIssue = new TeamIssueModel(userId, displayName, content);
        return repository.save(teamIssue);
    }
    
    public TeamIssueModel updateTeamIssue(TeamIssueModel teamIssue) {
        return repository.save(teamIssue);
    }
    
    public void deleteTeamIssue(String issueId) {
        repository.deleteById(issueId);
    }
    
    public List<TeamIssueModel> getOpenIssues() {
        return repository.findByStatus(IssueStatus.OPEN);
    }
    
    public List<TeamIssueModel> getResolvedIssues() {
        return repository.findByStatus(IssueStatus.RESOLVED);
    }
    
    public List<TeamIssueModel> getIssuesByUserId(String userId) {
        return repository.findByUserId(userId);
    }
    
    public TeamIssueModel resolveIssue(String issueId) {
        // バリデーション
        if (issueId == null || issueId.trim().isEmpty()) {
            throw new IllegalArgumentException("Issue ID is required");
        }
        
        Optional<TeamIssueModel> optionalIssue = repository.findById(issueId);
        if (optionalIssue.isPresent()) {
            TeamIssueModel issue = optionalIssue.get();
            if (issue.getStatus() == IssueStatus.RESOLVED) {
                throw new IllegalStateException("Issue is already resolved");
            }
            issue.resolve();
            return repository.save(issue);
        }
        throw new RuntimeException("Issue not found: " + issueId);
    }
    
    public TeamIssueModel addComment(String issueId, String userId, String displayName, String content) {
        // バリデーション
        if (issueId == null || issueId.trim().isEmpty()) {
            throw new IllegalArgumentException("Issue ID is required");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new IllegalArgumentException("Display name is required");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment content is required");
        }
        if (content.length() > 500) {
            throw new IllegalArgumentException("Comment must be 500 characters or less");
        }
        
        Optional<TeamIssueModel> optionalIssue = repository.findById(issueId);
        if (optionalIssue.isPresent()) {
            TeamIssueModel issue = optionalIssue.get();
            IssueCommentModel comment = new IssueCommentModel(userId, displayName, content);
            issue.addComment(comment);
            return repository.save(issue);
        }
        throw new RuntimeException("Issue not found: " + issueId);
    }
    
    public long countOpenIssues() {
        return getOpenIssues().size();
    }
    
    public long countResolvedIssues() {
        return getResolvedIssues().size();
    }
}