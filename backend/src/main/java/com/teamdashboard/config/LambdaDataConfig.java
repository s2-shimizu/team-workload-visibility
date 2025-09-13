package com.teamdashboard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@Profile("lambda")
public class LambdaDataConfig {
    
    // Lambda環境ではDynamoDBのみを使用
    // JPA/H2データベースは無効化
    
}