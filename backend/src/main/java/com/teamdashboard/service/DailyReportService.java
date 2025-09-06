package com.teamdashboard.service;

import com.teamdashboard.dto.DailyReportRequest;
import com.teamdashboard.dto.DailyReportResponse;
import com.teamdashboard.entity.DailyReport;
import com.teamdashboard.entity.User;
import com.teamdashboard.repository.DailyReportRepository;
import com.teamdashboard.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class DailyReportService {

    @Autowired
    private DailyReportRepository dailyReportRepository;

    @Autowired
    private UserRepository userRepository;

    public List<DailyReportResponse> getAllReports() {
        return dailyReportRepository.findAll().stream()
                .map(DailyReportResponse::new)
                .collect(Collectors.toList());
    }

    public List<DailyReportResponse> getReportsByDate(LocalDate date) {
        return dailyReportRepository.findByReportDateOrderByCreatedAtDesc(date).stream()
                .map(DailyReportResponse::new)
                .collect(Collectors.toList());
    }

    public List<DailyReportResponse> getReportsByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません: " + username));
        
        return dailyReportRepository.findByUserOrderByReportDateDesc(user).stream()
                .map(DailyReportResponse::new)
                .collect(Collectors.toList());
    }

    public DailyReportResponse createReport(String username, DailyReportRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません: " + username));

        LocalDate reportDate = request.getReportDate() != null ? request.getReportDate() : LocalDate.now();
        
        // 同じ日の日報が既に存在するかチェック
        Optional<DailyReport> existingReport = dailyReportRepository.findByUserAndReportDate(user, reportDate);
        if (existingReport.isPresent()) {
            throw new RuntimeException("この日の日報は既に投稿されています");
        }

        DailyReport report = new DailyReport();
        report.setUser(user);
        report.setReportDate(reportDate);
        report.setWorkContent(request.getWorkContent());
        report.setInsights(request.getInsights());
        report.setIssues(request.getIssues());
        report.setWorkloadLevel(request.getWorkloadLevel());

        DailyReport savedReport = dailyReportRepository.save(report);
        return new DailyReportResponse(savedReport);
    }

    public DailyReportResponse updateReport(Long reportId, String username, DailyReportRequest request) {
        DailyReport report = dailyReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("日報が見つかりません"));

        // 投稿者本人かチェック
        if (!report.getUser().getUsername().equals(username)) {
            throw new RuntimeException("他のユーザーの日報は編集できません");
        }

        report.setWorkContent(request.getWorkContent());
        report.setInsights(request.getInsights());
        report.setIssues(request.getIssues());
        report.setWorkloadLevel(request.getWorkloadLevel());

        DailyReport updatedReport = dailyReportRepository.save(report);
        return new DailyReportResponse(updatedReport);
    }

    public void deleteReport(Long reportId, String username) {
        DailyReport report = dailyReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("日報が見つかりません"));

        // 投稿者本人かチェック
        if (!report.getUser().getUsername().equals(username)) {
            throw new RuntimeException("他のユーザーの日報は削除できません");
        }

        dailyReportRepository.delete(report);
    }

    public List<DailyReportResponse> getRecentReports(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        
        return dailyReportRepository.findByReportDateBetween(startDate, endDate).stream()
                .map(DailyReportResponse::new)
                .collect(Collectors.toList());
    }
}