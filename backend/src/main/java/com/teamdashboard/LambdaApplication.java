package com.teamdashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.HttpEncodingAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {
    HttpEncodingAutoConfiguration.class,
    MultipartAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    SecurityAutoConfiguration.class
})
@ComponentScan(basePackages = {
    "com.teamdashboard.controller",
    "com.teamdashboard.service", 
    "com.teamdashboard.repository",
    "com.teamdashboard.config"
})
public class LambdaApplication {
    
    public static void main(String[] args) {
        // Lambda環境での起動最適化
        System.setProperty("spring.main.lazy-initialization", "true");
        System.setProperty("spring.jpa.open-in-view", "false");
        System.setProperty("java.awt.headless", "true");
        
        SpringApplication app = new SpringApplication(LambdaApplication.class);
        app.setLazyInitialization(true);
        app.run(args);
    }
}