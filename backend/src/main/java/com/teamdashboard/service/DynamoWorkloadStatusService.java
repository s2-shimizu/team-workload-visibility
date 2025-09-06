package com.teamdashboard.service;

import com.teamdashboard.model.WorkloadStatusModel;
import com.teamdashboard.entity.WorkloadLevel;
import com.teamdashboard.repository.dynamodb.DynamoWorkloadStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Profile({"lambda", "dynamodb"})
public class DynamoWorkloadStatusService {
    
    @Autowired
    private DynamoWorkloadStatusRepository repository;
    
    public List<WorkloadStatusModel> getAllWorkloadStatuses() {
        return repository.findAll();
    }
    
    public WorkloadStatusModel getWorkloadStatusByUserId(String userId) {
        return repository.findByUserId(userId);
    }
    
    public WorkloadStatusModel updateWorkloadStatus(WorkloadStatusModel workloadStatus) {
        workloadStatus.setUpdatedAt(LocalDateTime.now());
        return repository.save(workloadStatus);
    }
    
    public WorkloadStatusModel createWorkloadStatus(String userId, String displayName, WorkloadLevel level) {
        WorkloadStatusModel workloadStatus = new WorkloadStatusModel(userId, displayName, level);
        return repository.save(workloadStatus);
    }
    
    public void deleteWorkloadStatus(String userId) {
        repository.deleteByUserId(userId);
    }
    
    public List<WorkloadStatusModel> getHighWorkloadUsers() {
        return repository.findByWorkloadLevel(WorkloadLevel.HIGH);
    }
    
    public long countByWorkloadLevel(WorkloadLevel level) {
        return repository.findByWorkloadLevel(level).size();
    }
    
    public boolean existsByUserId(String userId) {
        return repository.findByUserId(userId) != null;
    }
}