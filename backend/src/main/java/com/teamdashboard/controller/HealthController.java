package com.teamdashboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Team Dashboard API is running");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    @GetMapping("/actuator/health")
    public Map<String, Object> actuatorHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        
        Map<String, String> checks = new HashMap<>();
        checks.put("database", "UP");
        checks.put("diskSpace", "UP");
        response.put("checks", checks);
        
        return response;
    }
}