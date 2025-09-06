package com.teamdashboard.config;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;

@Configuration
@Profile({"lambda", "dynamodb"})
public class DynamoDBConfig {
    
    @Value("${aws.region:ap-northeast-1}")
    private String awsRegion;
    
    @Value("${aws.dynamodb.endpoint:}")
    private String dynamodbEndpoint;
    
    @Bean
    public DynamoDbClient dynamoDbClient() {
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
            .region(Region.of(awsRegion));
        
        // ローカル開発環境用の設定
        if (!dynamodbEndpoint.isEmpty()) {
            builder.endpointOverride(URI.create(dynamodbEndpoint))
                   .credentialsProvider(StaticCredentialsProvider.create(
                       AwsBasicCredentials.create("dummy", "dummy")));
        }
        
        return builder.build();
    }
    
    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
            .dynamoDbClient(dynamoDbClient)
            .build();
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}