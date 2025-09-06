package com.teamdashboard.controller;

import com.teamdashboard.dto.DailyReportRequest;
import com.teamdashboard.dto.DailyReportResponse;
import com.teamdashboard.service.DailyReportService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/reports")
@CrossOrigin(origins = "*")
public class DailyReportController {

    @Autowired
    private DailyReportService dailyReportService;

    @GetMapping
    public ResponseEntity<List<DailyReportResponse>> getAllReports() {
        List<DailyReportResponse> reports = dailyReportService.getAllReports();
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<DailyReportResponse>> getReportsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<DailyReportResponse> reports = dailyReportService.getReportsByDate(date);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<List<DailyReportResponse>> getReportsByUser(@PathVariable String username) {
        List<DailyReportResponse> reports = dailyReportService.getReportsByUser(username);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<DailyReportResponse>> getRecentReports(
            @RequestParam(defaultValue = "7") int days) {
        List<DailyReportResponse> reports = dailyReportService.getRecentReports(days);
        return ResponseEntity.ok(reports);
    }

    @PostMapping
    public ResponseEntity<DailyReportResponse> createReport(
            @Valid @RequestBody DailyReportRequest request,
            Principal principal) {
        String username = principal != null ? principal.getName() : "testuser";
        DailyReportResponse response = dailyReportService.createReport(username, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DailyReportResponse> updateReport(
            @PathVariable Long id,
            @Valid @RequestBody DailyReportRequest request,
            Principal principal) {
        String username = principal != null ? principal.getName() : "testuser";
        DailyReportResponse response = dailyReportService.updateReport(id, username, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long id, Principal principal) {
        String username = principal != null ? principal.getName() : "testuser";
        dailyReportService.deleteReport(id, username);
        return ResponseEntity.noContent().build();
    }
}