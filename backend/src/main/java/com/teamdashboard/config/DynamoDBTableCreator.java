package com.teamdashboard.config;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dynamodb", "lambda"})
public class DynamoDBTableCreator implements CommandLineRunner {
    
    @Autowired
    private DynamoDbClient dynamoDbClient;
    
    @Value("${dynamodb.table.name:TeamDashboard}")
    private String tableName;
    
    @Override
    public void run(String... args) throws Exception {
        try {
            createTableIfNotExists();
        } catch (Exception e) {
            System.err.println("DynamoDBテーブル作成をスキップしました（DynamoDBローカルが利用できません）: " + e.getMessage());
        }
    }
    
    private void createTableIfNotExists() {
        try {
            // テーブルが存在するかチェック
            DescribeTableRequest describeRequest = DescribeTableRequest.builder()
                .tableName(tableName)
                .build();
            
            dynamoDbClient.describeTable(describeRequest);
            System.out.println("テーブル " + tableName + " は既に存在します");
            
        } catch (ResourceNotFoundException e) {
            // テーブルが存在しない場合は作成
            System.out.println("テーブル " + tableName + " を作成中...");
            createTable();
        }
    }
    
    private void createTable() {
        CreateTableRequest createTableRequest = CreateTableRequest.builder()
            .tableName(tableName)
            .keySchema(
                KeySchemaElement.builder()
                    .attributeName("PK")
                    .keyType(KeyType.HASH)
                    .build(),
                KeySchemaElement.builder()
                    .attributeName("SK")
                    .keyType(KeyType.RANGE)
                    .build()
            )
            .attributeDefinitions(
                AttributeDefinition.builder()
                    .attributeName("PK")
                    .attributeType(ScalarAttributeType.S)
                    .build(),
                AttributeDefinition.builder()
                    .attributeName("SK")
                    .attributeType(ScalarAttributeType.S)
                    .build(),
                AttributeDefinition.builder()
                    .attributeName("GSI1PK")
                    .attributeType(ScalarAttributeType.S)
                    .build(),
                AttributeDefinition.builder()
                    .attributeName("GSI1SK")
                    .attributeType(ScalarAttributeType.S)
                    .build()
            )
            .globalSecondaryIndexes(
                GlobalSecondaryIndex.builder()
                    .indexName("GSI1")
                    .keySchema(
                        KeySchemaElement.builder()
                            .attributeName("GSI1PK")
                            .keyType(KeyType.HASH)
                            .build(),
                        KeySchemaElement.builder()
                            .attributeName("GSI1SK")
                            .keyType(KeyType.RANGE)
                            .build()
                    )
                    .projection(Projection.builder()
                        .projectionType(ProjectionType.ALL)
                        .build())
                    .build()
            )
            .billingMode(BillingMode.PAY_PER_REQUEST)
            .build();
        
        try {
            CreateTableResponse response = dynamoDbClient.createTable(createTableRequest);
            System.out.println("テーブル作成リクエストを送信しました: " + response.tableDescription().tableName());
            
            // テーブルがアクティブになるまで待機
            waitForTableToBeActive();
            
        } catch (Exception e) {
            System.err.println("テーブル作成に失敗しました: " + e.getMessage());
            throw new RuntimeException("DynamoDBテーブルの作成に失敗しました", e);
        }
    }
    
    private void waitForTableToBeActive() {
        System.out.println("テーブルがアクティブになるまで待機中...");
        
        try (DynamoDbWaiter waiter = dynamoDbClient.waiter()) {
            WaiterResponse<DescribeTableResponse> waiterResponse = waiter
                .waitUntilTableExists(DescribeTableRequest.builder()
                    .tableName(tableName)
                    .build());
            
            waiterResponse.matched().response().ifPresent(response -> {
                System.out.println("テーブル " + tableName + " の作成が完了しました");
            });
        }
    }
}