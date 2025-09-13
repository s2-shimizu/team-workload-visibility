#!/usr/bin/env node

/**
 * 環境設定セットアップスクリプト
 * Team Dashboard アプリケーションの環境変数とAWS設定をセットアップします
 */

const fs = require('fs');
const path = require('path');
const readline = require('readline');

class EnvironmentSetup {
    constructor() {
        this.rl = readline.createInterface({
            input: process.stdin,
            output: process.stdout
        });
        
        this.config = {
            environment: 'dev',
            region: 'ap-northeast-1',
            tableName: '',
            functionName: '',
            apiName: '',
            jwtSecret: '',
            corsOrigins: '*'
        };
    }

    // ユーザー入力を取得
    async prompt(question, defaultValue = '') {
        return new Promise((resolve) => {
            const displayDefault = defaultValue ? ` (デフォルト: ${defaultValue})` : '';
            this.rl.question(`${question}${displayDefault}: `, (answer) => {
                resolve(answer.trim() || defaultValue);
            });
        });
    }

    // 強力なJWTシークレットを生成
    generateJwtSecret() {
        const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*';
        let result = '';
        for (let i = 0; i < 64; i++) {
            result += chars.charAt(Math.floor(Math.random() * chars.length));
        }
        return result;
    }

    // 環境設定の収集
    async collectConfiguration() {
        console.log('🚀 Team Dashboard 環境設定セットアップ\n');
        
        // 基本設定
        this.config.environment = await this.prompt('デプロイメント環境を選択してください (dev/staging/prod)', 'dev');
        this.config.region = await this.prompt('AWSリージョンを入力してください', 'ap-northeast-1');
        
        // DynamoDB設定
        const defaultTableName = `TeamDashboard-${this.config.environment}`;
        this.config.tableName = await this.prompt('DynamoDBテーブル名を入力してください', defaultTableName);
        
        // Lambda設定
        const defaultFunctionName = `team-dashboard-api-${this.config.environment}`;
        this.config.functionName = await this.prompt('Lambda関数名を入力してください', defaultFunctionName);
        
        // API Gateway設定
        const defaultApiName = `team-dashboard-api-${this.config.environment}`;
        this.config.apiName = await this.prompt('API Gateway名を入力してください', defaultApiName);
        
        // セキュリティ設定
        const generateSecret = await this.prompt('JWTシークレットを自動生成しますか？ (y/n)', 'y');
        if (generateSecret.toLowerCase() === 'y') {
            this.config.jwtSecret = this.generateJwtSecret();
            console.log('✅ JWTシークレットを自動生成しました');
        } else {
            this.config.jwtSecret = await this.prompt('JWTシークレットを入力してください（32文字以上推奨）');
        }
        
        // CORS設定
        if (this.config.environment === 'prod') {
            this.config.corsOrigins = await this.prompt('本番環境のCORS許可オリジンを入力してください', 'https://yourdomain.com');
        } else {
            this.config.corsOrigins = await this.prompt('CORS許可オリジンを入力してください', '*');
        }
    }

    // .env ファイルの生成
    generateEnvFile() {
        const envContent = `# Team Dashboard Environment Configuration
# Generated on ${new Date().toISOString()}

# Basic Configuration
ENVIRONMENT=${this.config.environment}
SPRING_PROFILES_ACTIVE=lambda,dynamodb,${this.config.environment}
AWS_REGION=${this.config.region}

# DynamoDB Configuration
DYNAMODB_TABLE_NAME=${this.config.tableName}
${this.config.environment === 'local' ? 'DYNAMODB_ENDPOINT=http://localhost:8000' : '# DYNAMODB_ENDPOINT='}

# Lambda Configuration
JAVA_TOOL_OPTIONS=-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Djava.awt.headless=true

# API Gateway Configuration
API_GATEWAY_STAGE=${this.config.environment}
CORS_ALLOWED_ORIGINS=${this.config.corsOrigins}

# Security Configuration
JWT_SECRET=${this.config.jwtSecret}
JWT_EXPIRATION=86400

# Logging Configuration
LOG_LEVEL=${this.config.environment === 'prod' ? 'WARN' : 'INFO'}
LOG_FORMAT=JSON

# Optional Cognito Configuration (uncomment if using)
# COGNITO_USER_POOL_ID=
# COGNITO_CLIENT_ID=

# Local Development (uncomment for local development)
# AWS_ACCESS_KEY_ID=your-access-key
# AWS_SECRET_ACCESS_KEY=your-secret-key
`;

        fs.writeFileSync('.env', envContent);
        console.log('✅ .env ファイルを生成しました');
    }

    // amplify.yml の環境変数セクション更新
    updateAmplifyConfig() {
        const amplifyConfigPath = 'amplify.yml';
        
        if (!fs.existsSync(amplifyConfigPath)) {
            console.log('⚠️  amplify.yml が見つかりません。スキップします。');
            return;
        }

        try {
            const yaml = require('js-yaml');
            let amplifyConfig = yaml.load(fs.readFileSync(amplifyConfigPath, 'utf8'));
            
            // 環境変数セクションを追加/更新
            if (!amplifyConfig.backend) {
                amplifyConfig.backend = {};
            }
            
            if (!amplifyConfig.backend.phases) {
                amplifyConfig.backend.phases = {};
            }
            
            if (!amplifyConfig.backend.phases.preBuild) {
                amplifyConfig.backend.phases.preBuild = { commands: [] };
            }
            
            // 環境変数設定コマンドを追加
            const envCommands = [
                `echo "Environment: ${this.config.environment}"`,
                `echo "AWS Region: ${this.config.region}"`,
                `echo "DynamoDB Table: ${this.config.tableName}"`,
                'echo "Spring Profile: $SPRING_PROFILES_ACTIVE"'
            ];
            
            // 既存のコマンドと重複しないように追加
            for (const cmd of envCommands) {
                if (!amplifyConfig.backend.phases.preBuild.commands.includes(cmd)) {
                    amplifyConfig.backend.phases.preBuild.commands.push(cmd);
                }
            }
            
            // ファイルに書き戻し
            const updatedYaml = yaml.dump(amplifyConfig, { 
                indent: 2,
                lineWidth: 120,
                noRefs: true 
            });
            
            fs.writeFileSync(amplifyConfigPath, updatedYaml);
            console.log('✅ amplify.yml を更新しました');
            
        } catch (error) {
            console.log(`⚠️  amplify.yml の更新に失敗しました: ${error.message}`);
        }
    }

    // AWS Systems Manager Parameter Store設定スクリプト生成
    generateParameterStoreScript() {
        const scriptContent = `#!/bin/bash
# AWS Systems Manager Parameter Store設定スクリプト
# Generated on ${new Date().toISOString()}

set -e

ENVIRONMENT="${this.config.environment}"
REGION="${this.config.region}"

echo "=== Parameter Store設定開始 ==="
echo "Environment: $ENVIRONMENT"
echo "Region: $REGION"

# JWTシークレットの設定
echo "JWTシークレットを設定中..."
aws ssm put-parameter \\
    --name "/team-dashboard/$ENVIRONMENT/jwt-secret" \\
    --value "${this.config.jwtSecret}" \\
    --type "SecureString" \\
    --region "$REGION" \\
    --overwrite

# DynamoDBテーブル名の設定
echo "DynamoDBテーブル名を設定中..."
aws ssm put-parameter \\
    --name "/team-dashboard/$ENVIRONMENT/dynamodb-table-name" \\
    --value "${this.config.tableName}" \\
    --type "String" \\
    --region "$REGION" \\
    --overwrite

# API Gateway設定
echo "API Gateway設定を設定中..."
aws ssm put-parameter \\
    --name "/team-dashboard/$ENVIRONMENT/api-gateway-stage" \\
    --value "$ENVIRONMENT" \\
    --type "String" \\
    --region "$REGION" \\
    --overwrite

# CORS設定
echo "CORS設定を設定中..."
aws ssm put-parameter \\
    --name "/team-dashboard/$ENVIRONMENT/cors-allowed-origins" \\
    --value "${this.config.corsOrigins}" \\
    --type "String" \\
    --region "$REGION" \\
    --overwrite

echo "✅ Parameter Store設定完了"
echo ""
echo "設定されたパラメータ:"
aws ssm get-parameters-by-path \\
    --path "/team-dashboard/$ENVIRONMENT" \\
    --region "$REGION" \\
    --query "Parameters[].Name" \\
    --output table

echo ""
echo "🔧 次のステップ:"
echo "1. AWS CLIが正しく設定されていることを確認"
echo "2. 適切なIAM権限があることを確認"
echo "3. このスクリプトを実行: chmod +x setup-parameter-store.sh && ./setup-parameter-store.sh"
`;

        fs.writeFileSync('setup-parameter-store.sh', scriptContent);
        fs.chmodSync('setup-parameter-store.sh', '755');
        console.log('✅ Parameter Store設定スクリプトを生成しました: setup-parameter-store.sh');
    }

    // CloudFormation パラメータファイル生成
    generateCloudFormationParams() {
        const paramsContent = `[
  {
    "ParameterKey": "Environment",
    "ParameterValue": "${this.config.environment}"
  },
  {
    "ParameterKey": "FunctionName",
    "ParameterValue": "${this.config.functionName}"
  },
  {
    "ParameterKey": "ApiName",
    "ParameterValue": "${this.config.apiName}"
  },
  {
    "ParameterKey": "MemorySize",
    "ParameterValue": "${this.config.environment === 'prod' ? '1024' : '512'}"
  },
  {
    "ParameterKey": "Timeout",
    "ParameterValue": "30"
  },
  {
    "ParameterKey": "LogRetentionDays",
    "ParameterValue": "${this.config.environment === 'prod' ? '30' : '14'}"
  }
]`;

        fs.writeFileSync(`cloudformation-params-${this.config.environment}.json`, paramsContent);
        console.log(`✅ CloudFormationパラメータファイルを生成しました: cloudformation-params-${this.config.environment}.json`);
    }

    // デプロイメントスクリプト生成
    generateDeploymentScript() {
        const scriptContent = `#!/bin/bash
# Team Dashboard デプロイメントスクリプト
# Generated on ${new Date().toISOString()}

set -e

ENVIRONMENT="${this.config.environment}"
REGION="${this.config.region}"
STACK_NAME="team-dashboard-\$ENVIRONMENT"

echo "=== Team Dashboard デプロイメント開始 ==="
echo "Environment: \$ENVIRONMENT"
echo "Region: \$REGION"
echo "Stack Name: \$STACK_NAME"

# 環境変数の確認
echo "環境変数を確認中..."
if [ -f ".env" ]; then
    echo "✅ .env ファイルが見つかりました"
    source .env
else
    echo "⚠️  .env ファイルが見つかりません"
fi

# AWS CLI設定確認
echo "AWS CLI設定を確認中..."
aws sts get-caller-identity --region "\$REGION"

# SAMビルド
echo "SAMビルドを実行中..."
sam build --region "\$REGION"

# SAMデプロイ
echo "SAMデプロイを実行中..."
sam deploy \\
    --stack-name "\$STACK_NAME" \\
    --region "\$REGION" \\
    --capabilities CAPABILITY_IAM \\
    --parameter-overrides file://cloudformation-params-\$ENVIRONMENT.json \\
    --confirm-changeset \\
    --resolve-s3

# デプロイ後の検証
echo "デプロイ後の検証を実行中..."
node validate-aws-config.js

echo "✅ デプロイメント完了"

# 出力値の表示
echo ""
echo "📋 デプロイ結果:"
aws cloudformation describe-stacks \\
    --stack-name "\$STACK_NAME" \\
    --region "\$REGION" \\
    --query "Stacks[0].Outputs" \\
    --output table

echo ""
echo "🔧 次のステップ:"
echo "1. API Gatewayエンドポイントをテスト"
echo "2. フロントエンドの設定を更新"
echo "3. 継続的デプロイメントを設定"
`;

        fs.writeFileSync(`deploy-${this.config.environment}.sh`, scriptContent);
        fs.chmodSync(`deploy-${this.config.environment}.sh`, '755');
        console.log(`✅ デプロイメントスクリプトを生成しました: deploy-${this.config.environment}.sh`);
    }

    // 設定サマリーの表示
    displaySummary() {
        console.log('\n📋 設定サマリー:');
        console.log('================');
        console.log(`環境: ${this.config.environment}`);
        console.log(`リージョン: ${this.config.region}`);
        console.log(`DynamoDBテーブル: ${this.config.tableName}`);
        console.log(`Lambda関数: ${this.config.functionName}`);
        console.log(`API Gateway: ${this.config.apiName}`);
        console.log(`CORS設定: ${this.config.corsOrigins}`);
        console.log(`JWTシークレット: ${this.config.jwtSecret.substring(0, 8)}...`);
        
        console.log('\n📁 生成されたファイル:');
        console.log('- .env (環境変数設定)');
        console.log('- setup-parameter-store.sh (Parameter Store設定)');
        console.log(`- cloudformation-params-${this.config.environment}.json (CloudFormationパラメータ)`);
        console.log(`- deploy-${this.config.environment}.sh (デプロイメントスクリプト)`);
        
        console.log('\n🔧 次のステップ:');
        console.log('1. Parameter Storeを設定: ./setup-parameter-store.sh');
        console.log('2. 設定を検証: npm run validate');
        console.log(`3. アプリケーションをデプロイ: ./deploy-${this.config.environment}.sh`);
        console.log('4. Amplifyコンソールで環境変数を設定');
    }

    // メインセットアッププロセス
    async setup() {
        try {
            await this.collectConfiguration();
            
            console.log('\n🔧 設定ファイルを生成中...');
            
            this.generateEnvFile();
            this.updateAmplifyConfig();
            this.generateParameterStoreScript();
            this.generateCloudFormationParams();
            this.generateDeploymentScript();
            
            this.displaySummary();
            
            console.log('\n✅ 環境設定セットアップが完了しました！');
            
        } catch (error) {
            console.error(`❌ セットアップエラー: ${error.message}`);
            process.exit(1);
        } finally {
            this.rl.close();
        }
    }
}

// スクリプト実行
if (require.main === module) {
    const setup = new EnvironmentSetup();
    setup.setup();
}

module.exports = EnvironmentSetup;