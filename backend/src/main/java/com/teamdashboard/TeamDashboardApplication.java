package com.teamdashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * ローカル開発用のSpring Bootアプリケーション
 */
@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
@ComponentScan(basePackages = {
    "com.teamdashboard.controller"
})
public class TeamDashboardApplication {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Team Dashboard - ローカル開発サーバー起動");
        System.out.println("========================================");
        
        SpringApplication.run(TeamDashboardApplication.class, args);
        
        System.out.println("========================================");
        System.out.println("サーバーが起動しました");
        System.out.println("URL: http://localhost:8081");
        System.out.println("API: http://localhost:8081/api");
        System.out.println("テストエンドポイント: http://localhost:8081/api/status");
        System.out.println("========================================");
    }
}