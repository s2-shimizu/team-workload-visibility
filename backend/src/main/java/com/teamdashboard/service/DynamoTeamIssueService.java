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
        Optional<TeamIssueModel> optionalIssue = repository.findById(issueId);
        if (optionalIssue.isPresent()) {
            TeamIssueModel issue = optionalIssue.get();
            issue.resolve();
            return repository.save(issue);
        }
        throw new RuntimeException("Issue not found: " + issueId);
    }
    
    public TeamIssueModel addComment(String issueId, String userId, String displayName, String content) {
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