package com.teamdashboard;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class LambdaHandler implements RequestHandler<AwsProxyRequest, AwsProxyResponse> {
    
    private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
    
    static {
        try {
            // Lambda環境用のSpring Boot設定
            System.setProperty("spring.main.web-application-type", "servlet");
            System.setProperty("spring.servlet.multipart.enabled", "false");
            System.setProperty("spring.http.encoding.enabled", "false");
            System.setProperty("spring.main.lazy-initialization", "true");
            System.setProperty("spring.jpa.open-in-view", "false");
            
            // コールドスタート最適化のためのJVM設定
            System.setProperty("java.awt.headless", "true");
            System.setProperty("spring.main.allow-bean-definition-overriding", "true");
            
            handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(LambdaApplication.class);
            
            // プロファイル設定 - lambda と dynamodb を有効化
            handler.activateSpringProfiles("lambda", "dynamodb");
            
            // フィルター重複を防ぐ設定
            handler.stripBasePath("/");
            
            // 初期化完了ログ
            System.out.println("Spring Boot Lambda handler initialized successfully");
            
        } catch (ContainerInitializationException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not initialize Spring Boot application", e);
        }
    }

    @Override
    public AwsProxyResponse handleRequest(AwsProxyRequest input, Context context) {
        try {
            // リクエストログ
            context.getLogger().log("Processing request: " + input.getHttpMethod() + " " + input.getPath());
            
            // Spring Boot アプリケーションにリクエストを転送
            AwsProxyResponse response = handler.proxy(input, context);
            
            // レスポンスログ
            context.getLogger().log("Response status: " + response.getStatusCode());
            
            return response;
            
        } catch (Exception e) {
            context.getLogger().log("Error processing request: " + e.getMessage());
            e.printStackTrace();
            
            // エラーレスポンスを返す
            AwsProxyResponse errorResponse = new AwsProxyResponse();
            errorResponse.setStatusCode(500);
            errorResponse.setBody("{\"error\":\"Internal Server Error\",\"message\":\"" + e.getMessage() + "\"}");
            errorResponse.getHeaders().put("Content-Type", "application/json");
            errorResponse.getHeaders().put("Access-Control-Allow-Origin", "*");
            
            return errorResponse;
        }
    }
}