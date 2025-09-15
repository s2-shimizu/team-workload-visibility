package com.teamdashboard.service;

import com.teamdashboard.model.TeamIssue;
import com.teamdashboard.repository.TeamIssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TeamIssueService {

    private final TeamIssueRepository teamIssueRepository;

    @Autowired
    public TeamIssueService(TeamIssueRepository teamIssueRepository) {
        this.teamIssueRepository = teamIssueRepository;
    }

    public List<TeamIssue> getAllTeamIssues() {
        return teamIssueRepository.findAll();
    }

    public List<TeamIssue> getOpenTeamIssues() {
        return teamIssueRepository.findByStatus("OPEN");
    }

    public List<TeamIssue> getTeamIssuesByUserId(String userId) {
        return teamIssueRepository.findByUserId(userId);
    }

    public List<TeamIssue> getTeamIssuesByPriority(String priority) {
        return teamIssueRepository.findByPriority(priority);
    }

    public Optional<TeamIssue> getTeamIssueById(String issueId) {
        return teamIssueRepository.findByIssueId(issueId);
    }

    public TeamIssue createTeamIssue(String userId, String displayName, String content, String priority) {
        TeamIssue teamIssue = new TeamIssue();
        teamIssue.setUserId(userId);
        teamIssue.setDisplayName(displayName);
        teamIssue.setContent(content);
        teamIssue.setPriority(priority != null ? priority : "MEDIUM");
        teamIssue.setStatus("OPEN");
        
        return teamIssueRepository.save(teamIssue);
    }

    public TeamIssue updateTeamIssue(TeamIssue teamIssue) {
        return teamIssueRepository.save(teamIssue);
    }

    public Optional<TeamIssue> resolveTeamIssue(String issueId) {
        Optional<TeamIssue> optionalIssue = teamIssueRepository.findByIssueId(issueId);
        if (optionalIssue.isPresent()) {
            TeamIssue issue = optionalIssue.get();
            issue.setStatus("RESOLVED");
            return Optional.of(teamIssueRepository.save(issue));
        }
        return Optional.empty();
    }

    public Optional<TeamIssue> reopenTeamIssue(String issueId) {
        Optional<TeamIssue> optionalIssue = teamIssueRepository.findByIssueId(issueId);
        if (optionalIssue.isPresent()) {
            TeamIssue issue = optionalIssue.get();
            issue.setStatus("OPEN");
            issue.setResolvedAt(null);
            return Optional.of(teamIssueRepository.save(issue));
        }
        return Optional.empty();
    }

    public void deleteTeamIssue(String issueId) {
        teamIssueRepository.deleteByIssueId(issueId);
    }

    public boolean existsByIssueId(String issueId) {
        return teamIssueRepository.existsByIssueId(issueId);
    }

    public long getTotalCount() {
        return teamIssueRepository.count();
    }

    // 統計情報の取得
    public IssueStatistics getIssueStatistics() {
        long totalCount = teamIssueRepository.count();
        long openCount = teamIssueRepository.countByStatus("OPEN");
        long resolvedCount = teamIssueRepository.countByStatus("RESOLVED");
        long highPriorityCount = teamIssueRepository.countByPriority("HIGH");
        long mediumPriorityCount = teamIssueRepository.countByPriority("MEDIUM");
        long lowPriorityCount = teamIssueRepository.countByPriority("LOW");
        
        return new IssueStatistics(
                totalCount,
                openCount,
                resolvedCount,
                highPriorityCount,
                mediumPriorityCount,
                lowPriorityCount
        );
    }

    // 統計情報を格納するための内部クラス
    public static class IssueStatistics {
        private final long total;
        private final long open;
        private final long resolved;
        private final long highPriority;
        private final long mediumPriority;
        private final long lowPriority;

        public IssueStatistics(long total, long open, long resolved, 
                             long highPriority, long mediumPriority, long lowPriority) {
            this.total = total;
            this.open = open;
            this.resolved = resolved;
            this.highPriority = highPriority;
            this.mediumPriority = mediumPriority;
            this.lowPriority = lowPriority;
        }

        // Getters
        public long getTotal() { return total; }
        public long getOpen() { return open; }
        public long getResolved() { return resolved; }
        public long getHighPriority() { return highPriority; }
        public long getMediumPriority() { return mediumPriority; }
        public long getLowPriority() { return lowPriority; }
    }
}