package com.teamdashboard.controller;

import com.teamdashboard.model.WorkloadStatusModel;
import com.teamdashboard.entity.WorkloadLevel;
import com.teamdashboard.service.DynamoWorkloadStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Profile;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workload-status")
@CrossOrigin(origins = "*")
@Profile({"lambda", "dynamodb"})
public class DynamoWorkloadStatusController {
    
    @Autowired
    private DynamoWorkloadStatusService workloadStatusService;
    
    @GetMapping
    public ResponseEntity<List<WorkloadStatusModel>> getAllWorkloadStatuses() {
        try {
            List<WorkloadStatusModel> statuses = workloadStatusService.getAllWorkloadStatuses();
            return ResponseEntity.ok(statuses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/my")
    public ResponseEntity<WorkloadStatusModel> getMyWorkloadStatus(@RequestParam String userId) {
        try {
            WorkloadStatusModel status = workloadStatusService.getWorkloadStatusByUserId(userId);
            if (status != null) {
                return ResponseEntity.ok(status);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping
    public ResponseEntity<WorkloadStatusModel> updateWorkloadStatus(@Valid @RequestBody WorkloadStatusModel request) {
        try {
            WorkloadStatusModel updated = workloadStatusService.updateWorkloadStatus(request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PutMapping("/{userId}")
    public ResponseEntity<WorkloadStatusModel> updateWorkloadStatusByUserId(
            @PathVariable String userId, 
            @Valid @RequestBody WorkloadStatusModel request) {
        try {
            request.setUserId(userId);
            WorkloadStatusModel updated = workloadStatusService.updateWorkloadStatus(request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteWorkloadStatus(@PathVariable String userId) {
        try {
            workloadStatusService.deleteWorkloadStatus(userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/high-workload")
    public ResponseEntity<List<WorkloadStatusModel>> getHighWorkloadUsers() {
        try {
            List<WorkloadStatusModel> highWorkloadUsers = workloadStatusService.getHighWorkloadUsers();
            return ResponseEntity.ok(highWorkloadUsers);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getWorkloadStatistics() {
        try {
            Map<String, Long> statistics = Map.of(
                "high", workloadStatusService.countByWorkloadLevel(WorkloadLevel.HIGH),
                "medium", workloadStatusService.countByWorkloadLevel(WorkloadLevel.MEDIUM),
                "low", workloadStatusService.countByWorkloadLevel(WorkloadLevel.LOW)
            );
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}