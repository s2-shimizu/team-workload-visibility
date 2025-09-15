package com.teamdashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * ローカル開発用のSpring Bootアプリケーション
 * Lambda環境との競合を避けるため@SpringBootApplicationではなく@Configurationを使用
 */
@Configuration
@EnableAutoConfiguration(exclude = {
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