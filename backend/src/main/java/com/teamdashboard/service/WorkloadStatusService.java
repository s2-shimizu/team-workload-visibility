package com.teamdashboard.service;

import com.teamdashboard.model.WorkloadStatus;
import com.teamdashboard.repository.WorkloadStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WorkloadStatusService {

    private final WorkloadStatusRepository workloadStatusRepository;

    @Autowired
    public WorkloadStatusService(WorkloadStatusRepository workloadStatusRepository) {
        this.workloadStatusRepository = workloadStatusRepository;
    }

    public List<WorkloadStatus> getAllWorkloadStatuses() {
        return workloadStatusRepository.findAll();
    }

    public Optional<WorkloadStatus> getWorkloadStatusByUserId(String userId) {
        return workloadStatusRepository.findByUserId(userId);
    }

    public WorkloadStatus createOrUpdateWorkloadStatus(String userId, String displayName, 
                                                      String workloadLevel, Integer projectCount, 
                                                      Integer taskCount, String comment) {
        WorkloadStatus workloadStatus = workloadStatusRepository.findByUserId(userId)
                .orElse(new WorkloadStatus());
        
        workloadStatus.setUserId(userId);
        workloadStatus.setDisplayName(displayName);
        workloadStatus.setWorkloadLevel(workloadLevel);
        workloadStatus.setProjectCount(projectCount);
        workloadStatus.setTaskCount(taskCount);
        workloadStatus.setComment(comment);
        
        return workloadStatusRepository.save(workloadStatus);
    }

    public WorkloadStatus updateWorkloadStatus(WorkloadStatus workloadStatus) {
        return workloadStatusRepository.save(workloadStatus);
    }

    public void deleteWorkloadStatus(String userId) {
        workloadStatusRepository.deleteByUserId(userId);
    }

    public boolean existsByUserId(String userId) {
        return workloadStatusRepository.existsByUserId(userId);
    }

    public long getTotalCount() {
        return workloadStatusRepository.count();
    }

    // 統計情報の取得
    public WorkloadStatistics getWorkloadStatistics() {
        List<WorkloadStatus> allStatuses = getAllWorkloadStatuses();
        
        long highCount = allStatuses.stream()
                .filter(status -> "HIGH".equals(status.getWorkloadLevel()))
                .count();
        
        long mediumCount = allStatuses.stream()
                .filter(status -> "MEDIUM".equals(status.getWorkloadLevel()))
                .count();
        
        long lowCount = allStatuses.stream()
                .filter(status -> "LOW".equals(status.getWorkloadLevel()))
                .count();
        
        double avgProjectCount = allStatuses.stream()
                .mapToInt(status -> status.getProjectCount() != null ? status.getProjectCount() : 0)
                .average()
                .orElse(0.0);
        
        double avgTaskCount = allStatuses.stream()
                .mapToInt(status -> status.getTaskCount() != null ? status.getTaskCount() : 0)
                .average()
                .orElse(0.0);
        
        return new WorkloadStatistics(
                allStatuses.size(),
                highCount,
                mediumCount,
                lowCount,
                avgProjectCount,
                avgTaskCount
        );
    }

    // 統計情報を格納するための内部クラス
    public static class WorkloadStatistics {
        private final long totalUsers;
        private final long highWorkload;
        private final long mediumWorkload;
        private final long lowWorkload;
        private final double averageProjectCount;
        private final double averageTaskCount;

        public WorkloadStatistics(long totalUsers, long highWorkload, long mediumWorkload, 
                                long lowWorkload, double averageProjectCount, double averageTaskCount) {
            this.totalUsers = totalUsers;
            this.highWorkload = highWorkload;
            this.mediumWorkload = mediumWorkload;
            this.lowWorkload = lowWorkload;
            this.averageProjectCount = averageProjectCount;
            this.averageTaskCount = averageTaskCount;
        }

        // Getters
        public long getTotalUsers() { return totalUsers; }
        public long getHighWorkload() { return highWorkload; }
        public long getMediumWorkload() { return mediumWorkload; }
        public long getLowWorkload() { return lowWorkload; }
        public double getAverageProjectCount() { return averageProjectCount; }
        public double getAverageTaskCount() { return averageTaskCount; }
    }
}