# CI/CD パイプライン統合セットアップガイド

## GitHub Actions 統合

### 1. ワークフローファイルの作成

`.github/workflows/amplify-deployment.yml` を作成：

```yaml
name: Amplify Deployment with Validation

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

env:
  FRONTEND_URL: ${{ secrets.FRONTEND_URL }}
  API_URL: ${{ secrets.API_URL }}
  AMPLIFY_APP_ID: ${{ secrets.AMPLIFY_APP_ID }}

jobs:
  pre-deployment-check:
    runs-on: windows-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '16'
          
      - name: Install dependencies
        run: npm install
        
      - name: Run pre-deployment checks
        run: node pre-deployment-checker.js --verbose
        
      - name: Upload pre-deployment report
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: pre-deployment-report
          path: pre-deployment-check-report.json

  deploy:
    needs: pre-deployment-check
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v3
      
      - name: Deploy to Amplify
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ap-northeast-1
          
      - name: Trigger Amplify deployment
        run: |
          aws amplify start-job --app-id ${{ env.AMPLIFY_APP_ID }} --branch-name main --job-type RELEASE

  post-deployment-verification:
    needs: deploy
    runs-on: windows-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '16'
          
      - name: Wait for deployment completion
        run: |
          Start-Sleep -Seconds 300  # 5分待機
          
      - name: Run deployment verification
        run: |
          node deployment-verification.js --frontend-url ${{ env.FRONTEND_URL }} --api-url ${{ env.API_URL }}
          
      - name: Run integration tests
        run: |
          node integration-test-suite.js --frontend-url ${{ env.FRONTEND_URL }} --api-url ${{ env.API_URL }} --skip-deployment
          
      - name: Upload verification reports
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: verification-reports
          path: |
            deployment-verification-report.json
            integration-test-report.json
            
      - name: Notify Slack on failure
        if: failure()
        uses: 8398a7/action-slack@v3
        with:
          status: failure
          channel: '#deployment-alerts'
          webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

### 2. 必要なシークレットの設定

GitHub リポジトリの Settings > Secrets で以下を設定：

```
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
FRONTEND_URL=https://main.d1234567890.amplifyapp.com
API_URL=https://api.example.com
AMPLIFY_APP_ID=d1234567890
SLACK_WEBHOOK=https://hooks.slack.com/services/...
```

## Azure DevOps 統合

### パイプライン設定 (`azure-pipelines.yml`)

```yaml
trigger:
- main
- develop

pool:
  vmImage: 'windows-latest'

variables:
  FRONTEND_URL: $(frontendUrl)
  API_URL: $(apiUrl)
  AMPLIFY_APP_ID: $(amplifyAppId)

stages:
- stage: PreDeploymentCheck
  displayName: 'Pre-deployment Validation'
  jobs:
  - job: ValidationJob
    steps:
    - task: NodeTool@0
      inputs:
        versionSpec: '16.x'
        
    - script: |
        node pre-deployment-checker.js --verbose
      displayName: 'Run pre-deployment checks'
      
    - task: PublishTestResults@2
      condition: always()
      inputs:
        testResultsFiles: 'pre-deployment-check-report.json'
        testRunTitle: 'Pre-deployment Check Results'

- stage: Deploy
  displayName: 'Deploy to Amplify'
  dependsOn: PreDeploymentCheck
  condition: and(succeeded(), eq(variables['Build.SourceBranch'], 'refs/heads/main'))
  jobs:
  - deployment: DeployJob
    environment: 'production'
    strategy:
      runOnce:
        deploy:
          steps:
          - task: AWSShellScript@1
            inputs:
              awsCredentials: 'AWS-Connection'
              regionName: 'ap-northeast-1'
              scriptType: 'inline'
              inlineScript: |
                aws amplify start-job --app-id $(AMPLIFY_APP_ID) --branch-name main --job-type RELEASE

- stage: PostDeploymentVerification
  displayName: 'Post-deployment Verification'
  dependsOn: Deploy
  jobs:
  - job: VerificationJob
    steps:
    - task: NodeTool@0
      inputs:
        versionSpec: '16.x'
        
    - script: |
        timeout /t 300 /nobreak
      displayName: 'Wait for deployment completion'
      
    - script: |
        node deployment-verification.js --frontend-url $(FRONTEND_URL) --api-url $(API_URL)
      displayName: 'Run deployment verification'
      
    - script: |
        node integration-test-suite.js --frontend-url $(FRONTEND_URL) --api-url $(API_URL) --skip-deployment
      displayName: 'Run integration tests'
      
    - task: PublishTestResults@2
      condition: always()
      inputs:
        testResultsFiles: |
          deployment-verification-report.json
          integration-test-report.json
        testRunTitle: 'Post-deployment Verification Results'
```

## Jenkins 統合

### Jenkinsfile

```groovy
pipeline {
    agent { label 'windows' }
    
    environment {
        FRONTEND_URL = credentials('frontend-url')
        API_URL = credentials('api-url')
        AMPLIFY_APP_ID = credentials('amplify-app-id')
    }
    
    stages {
        stage('Pre-deployment Check') {
            steps {
                script {
                    bat 'node pre-deployment-checker.js --verbose'
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'pre-deployment-check-report.json', allowEmptyArchive: true
                }
            }
        }
        
        stage('Deploy') {
            when {
                branch 'main'
            }
            steps {
                withAWS(credentials: 'aws-credentials', region: 'ap-northeast-1') {
                    bat """
                        aws amplify start-job --app-id %AMPLIFY_APP_ID% --branch-name main --job-type RELEASE
                    """
                }
            }
        }
        
        stage('Post-deployment Verification') {
            steps {
                script {
                    // 5分待機
                    sleep(time: 300, unit: 'SECONDS')
                    
                    bat """
                        node deployment-verification.js --frontend-url %FRONTEND_URL% --api-url %API_URL%
                        node integration-test-suite.js --frontend-url %FRONTEND_URL% --api-url %API_URL% --skip-deployment
                    """
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: '*.json', allowEmptyArchive: true
                }
                failure {
                    slackSend(
                        channel: '#deployment-alerts',
                        color: 'danger',
                        message: "Deployment verification failed for ${env.JOB_NAME} - ${env.BUILD_NUMBER}"
                    )
                }
            }
        }
    }
}
```

## 監視とアラート設定

### CloudWatch アラーム設定

```bash
# デプロイメント失敗率のアラーム
aws cloudwatch put-metric-alarm \
    --alarm-name "AmplifyDeploymentFailureRate" \
    --alarm-description "Amplify deployment failure rate is high" \
    --metric-name "DeploymentFailures" \
    --namespace "AWS/Amplify" \
    --statistic Sum \
    --period 300 \
    --threshold 1 \
    --comparison-operator GreaterThanOrEqualToThreshold \
    --alarm-actions arn:aws:sns:ap-northeast-1:123456789012:deployment-alerts
```

### Slack 通知設定

Webhook URL を使用した通知スクリプト：

```javascript
// slack-notifier.js
const https = require('https');

function sendSlackNotification(message, isError = false) {
    const payload = {
        text: message,
        color: isError ? 'danger' : 'good',
        channel: '#deployment-alerts'
    };
    
    const options = {
        hostname: 'hooks.slack.com',
        path: '/services/YOUR/SLACK/WEBHOOK',
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    };
    
    const req = https.request(options);
    req.write(JSON.stringify(payload));
    req.end();
}

module.exports = { sendSlackNotification };
```

## 定期実行設定

### Windows タスクスケジューラ

```batch
# 毎日午前2時に統合テストを実行
schtasks /create /tn "AmplifyIntegrationTest" /tr "node C:\path\to\integration-test-suite.js --frontend-url %FRONTEND_URL%" /sc daily /st 02:00
```

### Linux Cron

```bash
# 毎時デプロイメント検証を実行
0 * * * * cd /path/to/project && node deployment-verification.js --frontend-url $FRONTEND_URL
```

## レポート集約とダッシュボード

### レポート集約スクリプト

```javascript
// report-aggregator.js
const fs = require('fs');
const path = require('path');

class ReportAggregator {
    constructor() {
        this.reports = [];
    }
    
    collectReports() {
        const reportFiles = [
            'pre-deployment-check-report.json',
            'deployment-verification-report.json',
            'integration-test-report.json'
        ];
        
        reportFiles.forEach(file => {
            if (fs.existsSync(file)) {
                const report = JSON.parse(fs.readFileSync(file, 'utf8'));
                this.reports.push({
                    type: file.replace('-report.json', ''),
                    timestamp: new Date(),
                    data: report
                });
            }
        });
    }
    
    generateDashboard() {
        const dashboard = {
            summary: {
                totalTests: this.reports.length,
                successRate: this.calculateSuccessRate(),
                lastUpdate: new Date()
            },
            reports: this.reports
        };
        
        fs.writeFileSync('dashboard-data.json', JSON.stringify(dashboard, null, 2));
        console.log('Dashboard data generated: dashboard-data.json');
    }
    
    calculateSuccessRate() {
        const successful = this.reports.filter(r => r.data.success).length;
        return (successful / this.reports.length * 100).toFixed(2);
    }
}

// 使用例
const aggregator = new ReportAggregator();
aggregator.collectReports();
aggregator.generateDashboard();
```

これらの設定により、チーム全体でツールを効果的に活用し、継続的にデプロイメント品質を向上させることができます。