package com.teamdashboard.repository;

import com.teamdashboard.entity.DailyReport;
import com.teamdashboard.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {
    List<DailyReport> findByUserOrderByReportDateDesc(User user);
    
    List<DailyReport> findByReportDateOrderByCreatedAtDesc(LocalDate reportDate);
    
    Optional<DailyReport> findByUserAndReportDate(User user, LocalDate reportDate);
    
    @Query("SELECT dr FROM DailyReport dr WHERE dr.reportDate BETWEEN :startDate AND :endDate ORDER BY dr.reportDate DESC, dr.createdAt DESC")
    List<DailyReport> findByReportDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT dr FROM DailyReport dr WHERE dr.user = :user AND dr.reportDate BETWEEN :startDate AND :endDate ORDER BY dr.reportDate DESC")
    List<DailyReport> findByUserAndReportDateBetween(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}