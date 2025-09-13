# CloudWatch ログ分析ガイド

## 概要

AWS CloudWatchログを効果的に分析し、Amplifyデプロイメントの問題を迅速に特定するための詳細ガイドです。

## ログの種類と場所

### 1. Amplifyビルドログ
**場所**: `/aws/amplify/[app-id]`
**内容**: ビルドプロセス、デプロイメント、設定エラー

### 2. Lambda関数ログ
**場所**: `/aws/lambda/[function-name]`
**内容**: 関数実行、エラー、パフォーマンスメトリクス

### 3. API Gatewayログ
**場所**: `API-Gateway-Execution-Logs_[api-id]/[stage]`
**内容**: APIリクエスト、レスポンス、認証エラー

### 4. CloudFrontログ
**場所**: S3バケット（設定による）
**内容**: アクセスログ、キャッシュヒット/ミス、エラー

## 基本的なログ検索コマンド

### ログストリームの一覧取得
```bash
# Amplifyアプリのログストリーム
aws logs describe-log-streams \
  --log-group-name "/aws/amplify/[app-id]" \
  --order-by LastEventTime \
  --descending

# Lambda関数のログストリーム
aws logs describe-log-streams \
  --log-group-name "/aws/lambda/[function-name]" \
  --order-by LastEventTime \
  --descending \
  --max-items 10
```

### 時間範囲指定でのログ取得
```bash
# 過去1時間のログ
aws logs filter-log-events \
  --log-group-name "/aws/amplify/[app-id]" \
  --start-time $(date -d '1 hour ago' +%s)000 \
  --end-time $(date +%s)000

# 特定の日時範囲のログ
aws logs filter-log-events \
  --log-group-name "/aws/lambda/[function-name]" \
  --start-time 1640995200000 \
  --end-time 1640998800000
```

## エラーパターン別の検索方法

### 1. ビルドエラーの検索

#### YAML構文エラー
```bash
aws logs filter-log-events \
  --log-group-name "/aws/amplify/[app-id]" \
  --filter-pattern "{ $.message = \"*YAML*\" || $.message = \"*syntax*\" }"
```

#### ファイル不足エラー
```bash
aws logs filter-log-events \
  --log-group-name "/aws/amplify/[app-id]" \
  --filter-pattern "{ $.message = \"*not found*\" || $.message = \"*No such file*\" }"
```

#### 依存関係エラー
```bash
aws logs filter-log-events \
  --log-group-name "/aws/amplify/[app-id]" \
  --filter-pattern "{ $.message = \"*dependency*\" || $.message = \"*npm ERR*\" || $.message = \"*Maven*\" }"
```

### 2. Lambda実行エラーの検索

#### タイムアウトエラー
```bash
aws logs filter-log-events \
  --log-group-name "/aws/lambda/[function-name]" \
  --filter-pattern "Task timed out"
```

#### メモリ不足エラー
```bash
aws logs filter-log-events \
  --log-group-name "/aws/lambda/[function-name]" \
  --filter-pattern "{ $.message = \"*OutOfMemoryError*\" || $.message = \"*Memory*\" }"
```

#### 権限エラー
```bash
aws logs filter-log-events \
  --log-group-name "/aws/lambda/[function-name]" \
  --filter-pattern "{ $.message = \"*AccessDenied*\" || $.message = \"*Forbidden*\" || $.message = \"*Unauthorized*\" }"
```

### 3. API Gatewayエラーの検索

#### 4xxエラー（クライアントエラー）
```bash
aws logs filter-log-events \
  --log-group-name "API-Gateway-Execution-Logs_[api-id]/[stage]" \
  --filter-pattern "{ $.status = 4* }"
```

#### 5xxエラー（サーバーエラー）
```bash
aws logs filter-log-events \
  --log-group-name "API-Gateway-Execution-Logs_[api-id]/[stage]" \
  --filter-pattern "{ $.status = 5* }"
```

## CloudWatch Insightsクエリ

### 1. エラー分析クエリ

#### エラー発生頻度の分析
```sql
fields @timestamp, @message
| filter @message like /ERROR/
| stats count() as error_count by bin(5m)
| sort @timestamp desc
```

#### 特定エラーの詳細分析
```sql
fields @timestamp, @message, @requestId
| filter @message like /NullPointerException/
| sort @timestamp desc
| limit 50
```

#### エラー率の計算
```sql
fields @timestamp, @type, @message
| filter @type = "REPORT"
| stats count() as total_invocations, 
        sum(strcontains(@message, "ERROR")) as error_count
| extend error_rate = error_count / total_invocations * 100
```

### 2. パフォーマンス分析クエリ

#### レスポンス時間の分析
```sql
fields @timestamp, @duration, @billedDuration, @maxMemoryUsed
| filter @type = "REPORT"
| stats avg(@duration) as avg_duration,
        max(@duration) as max_duration,
        min(@duration) as min_duration,
        percentile(@duration, 95) as p95_duration
        by bin(5m)
| sort @timestamp desc
```

#### メモリ使用量の分析
```sql
fields @timestamp, @maxMemoryUsed, @memorySize
| filter @type = "REPORT"
| stats avg(@maxMemoryUsed) as avg_memory,
        max(@maxMemoryUsed) as max_memory,
        avg(@memorySize) as allocated_memory
        by bin(5m)
| extend memory_utilization = avg_memory / allocated_memory * 100
| sort @timestamp desc
```

#### 冷起動の分析
```sql
fields @timestamp, @duration, @initDuration
| filter @type = "REPORT" and ispresent(@initDuration)
| stats count() as cold_starts,
        avg(@initDuration) as avg_init_duration,
        max(@initDuration) as max_init_duration
        by bin(1h)
| sort @timestamp desc
```

### 3. ビジネスロジック分析クエリ

#### ユーザーアクション分析
```sql
fields @timestamp, @message
| filter @message like /userId/
| parse @message "userId: * action: *" as userId, action
| stats count() as action_count by action, bin(1h)
| sort @timestamp desc
```

#### API エンドポイント使用状況
```sql
fields @timestamp, @message
| filter @message like /API request/
| parse @message "method: * path: * status: *" as method, path, status
| stats count() as request_count by method, path, status
| sort request_count desc
```

## ログ分析の自動化

### 1. 定期的なエラー監視スクリプト
```bash
#!/bin/bash
# error-monitor.sh

LOG_GROUP="/aws/lambda/your-function-name"
TIME_RANGE=3600  # 1時間

# エラーログの取得
ERRORS=$(aws logs filter-log-events \
  --log-group-name "$LOG_GROUP" \
  --start-time $(($(date +%s) - $TIME_RANGE))000 \
  --filter-pattern "ERROR" \
  --query 'events[].message' \
  --output text)

if [ -n "$ERRORS" ]; then
    echo "エラーが検出されました:"
    echo "$ERRORS"
    # Slack通知などの処理
    curl -X POST -H 'Content-type: application/json' \
      --data "{\"text\":\"Lambda関数でエラーが発生しました\"}" \
      "$SLACK_WEBHOOK_URL"
fi
```

### 2. パフォーマンス監視スクリプト
```bash
#!/bin/bash
# performance-monitor.sh

LOG_GROUP="/aws/lambda/your-function-name"

# 平均実行時間の取得
AVG_DURATION=$(aws logs start-query \
  --log-group-name "$LOG_GROUP" \
  --start-time $(($(date +%s) - 3600))000 \
  --end-time $(date +%s)000 \
  --query-string 'fields @duration | filter @type = "REPORT" | stats avg(@duration)' \
  --query 'queryId' --output text)

# クエリ結果の取得（少し待機が必要）
sleep 10
RESULT=$(aws logs get-query-results --query-id "$AVG_DURATION")

echo "平均実行時間: $RESULT"
```

## アラートの設定

### 1. CloudWatchアラームの作成

#### エラー率アラーム
```bash
aws cloudwatch put-metric-alarm \
  --alarm-name "Lambda-High-Error-Rate" \
  --alarm-description "Lambda function error rate is high" \
  --metric-name Errors \
  --namespace AWS/Lambda \
  --statistic Sum \
  --period 300 \
  --threshold 10 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=FunctionName,Value=your-function-name \
  --evaluation-periods 2 \
  --alarm-actions arn:aws:sns:region:account:topic-name
```

#### レスポンス時間アラーム
```bash
aws cloudwatch put-metric-alarm \
  --alarm-name "Lambda-High-Duration" \
  --alarm-description "Lambda function duration is high" \
  --metric-name Duration \
  --namespace AWS/Lambda \
  --statistic Average \
  --period 300 \
  --threshold 5000 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=FunctionName,Value=your-function-name \
  --evaluation-periods 3 \
  --alarm-actions arn:aws:sns:region:account:topic-name
```

### 2. カスタムメトリクスの作成

#### アプリケーション固有のメトリクス
```javascript
// Lambda関数内でのカスタムメトリクス送信
const AWS = require('aws-sdk');
const cloudwatch = new AWS.CloudWatch();

async function putCustomMetric(metricName, value, unit = 'Count') {
    const params = {
        Namespace: 'YourApp/Lambda',
        MetricData: [{
            MetricName: metricName,
            Value: value,
            Unit: unit,
            Timestamp: new Date()
        }]
    };
    
    try {
        await cloudwatch.putMetricData(params).promise();
    } catch (error) {
        console.error('Failed to put metric:', error);
    }
}

// 使用例
await putCustomMetric('UserRegistrations', 1);
await putCustomMetric('DatabaseConnections', connectionCount);
await putCustomMetric('ProcessingTime', processingTime, 'Milliseconds');
```

## ログ保持とコスト最適化

### 1. ログ保持期間の設定
```bash
# ログ保持期間を30日に設定
aws logs put-retention-policy \
  --log-group-name "/aws/lambda/your-function-name" \
  --retention-in-days 30

# Amplifyログの保持期間設定
aws logs put-retention-policy \
  --log-group-name "/aws/amplify/your-app-id" \
  --retention-in-days 14
```

### 2. ログのアーカイブ
```bash
# S3へのログエクスポート
aws logs create-export-task \
  --log-group-name "/aws/lambda/your-function-name" \
  --from $(date -d '30 days ago' +%s)000 \
  --to $(date -d '1 day ago' +%s)000 \
  --destination your-log-archive-bucket \
  --destination-prefix lambda-logs/
```

## トラブルシューティングのベストプラクティス

### 1. 構造化ログの活用
```javascript
// 良い例：構造化されたログ
console.log(JSON.stringify({
    timestamp: new Date().toISOString(),
    level: 'INFO',
    requestId: context.awsRequestId,
    userId: event.userId,
    action: 'processPayment',
    amount: event.amount,
    currency: event.currency,
    duration: Date.now() - startTime
}));

// 悪い例：非構造化ログ
console.log(`User ${event.userId} processed payment of ${event.amount}`);
```

### 2. 相関IDの使用
```javascript
// リクエスト全体を通じて同じIDを使用
const correlationId = event.headers['x-correlation-id'] || 
                     context.awsRequestId;

console.log(JSON.stringify({
    correlationId,
    level: 'INFO',
    message: 'Starting request processing'
}));
```

### 3. エラーコンテキストの保持
```javascript
try {
    await processRequest(event);
} catch (error) {
    console.log(JSON.stringify({
        level: 'ERROR',
        error: error.message,
        stack: error.stack,
        requestId: context.awsRequestId,
        event: JSON.stringify(event),
        timestamp: new Date().toISOString()
    }));
    throw error;
}
```

## まとめ

効果的なログ分析のために：

1. **構造化ログ**を使用して検索性を向上
2. **適切なログレベル**を設定して重要な情報を強調
3. **CloudWatch Insights**を活用した高度な分析
4. **自動化されたアラート**で問題の早期発見
5. **コスト最適化**を考慮したログ保持戦略

これらの手法を組み合わせることで、Amplifyデプロイメントの問題を迅速に特定し、解決することができます。