#!/usr/bin/env node

/**
 * AWS設定検証スクリプト
 * Team Dashboard アプリケーションのAWS環境設定を検証します
 */

const { DynamoDBClient, DescribeTableCommand } = require('@aws-sdk/client-dynamodb');
const { LambdaClient, GetFunctionCommand } = require('@aws-sdk/client-lambda');
const { APIGatewayClient, GetRestApiCommand } = require('@aws-sdk/client-api-gateway');
const { SSMClient, GetParameterCommand } = require('@aws-sdk/client-ssm');

// 必須環境変数の定義
const REQUIRED_ENV_VARS = [
    'SPRING_PROFILES_ACTIVE',
    'AWS_REGION',
    'DYNAMODB_TABLE_NAME'
];

// オプション環境変数の定義
const OPTIONAL_ENV_VARS = [
    'JAVA_TOOL_OPTIONS',
    'API_GATEWAY_STAGE',
    'CORS_ALLOWED_ORIGINS',
    'JWT_SECRET',
    'JWT_EXPIRATION',
    'LOG_LEVEL',
    'LOG_FORMAT'
];

// セキュリティ関連環境変数
const SECURITY_ENV_VARS = [
    'JWT_SECRET',
    'COGNITO_USER_POOL_ID',
    'COGNITO_CLIENT_ID'
];

class AWSConfigValidator {
    constructor() {
        this.region = process.env.AWS_REGION || 'ap-northeast-1';
        this.environment = process.env.ENVIRONMENT || 'dev';
        this.errors = [];
        this.warnings = [];
        this.successes = [];
        
        // AWS クライアント初期化
        this.dynamoClient = new DynamoDBClient({ region: this.region });
        this.lambdaClient = new LambdaClient({ region: this.region });
        this.apiGatewayClient = new APIGatewayClient({ region: this.region });
        this.ssmClient = new SSMClient({ region: this.region });
    }

    log(level, message, details = null) {
        const timestamp = new Date().toISOString();
        const prefix = {
            'SUCCESS': '✅',
            'WARNING': '⚠️',
            'ERROR': '❌',
            'INFO': 'ℹ️'
        }[level] || 'ℹ️';
        
        console.log(`${prefix} [${timestamp}] ${message}`);
        if (details) {
            console.log(`   詳細: ${JSON.stringify(details, null, 2)}`);
        }
        
        // 結果を分類して保存
        switch (level) {
            case 'SUCCESS':
                this.successes.push({ message, details });
                break;
            case 'WARNING':
                this.warnings.push({ message, details });
                break;
            case 'ERROR':
                this.errors.push({ message, details });
                break;
        }
    }

    // 環境変数の検証
    validateEnvironmentVariables() {
        this.log('INFO', '=== 環境変数検証開始 ===');
        
        // 必須環境変数のチェック
        for (const envVar of REQUIRED_ENV_VARS) {
            const value = process.env[envVar];
            if (!value) {
                this.log('ERROR', `必須環境変数 ${envVar} が設定されていません`);
            } else {
                this.log('SUCCESS', `必須環境変数 ${envVar} = ${value}`);
            }
        }
        
        // オプション環境変数のチェック
        for (const envVar of OPTIONAL_ENV_VARS) {
            const value = process.env[envVar];
            if (value) {
                this.log('SUCCESS', `オプション環境変数 ${envVar} = ${value}`);
            } else {
                this.log('WARNING', `オプション環境変数 ${envVar} が設定されていません（デフォルト値を使用）`);
            }
        }
        
        // セキュリティ関連環境変数のチェック
        for (const envVar of SECURITY_ENV_VARS) {
            const value = process.env[envVar];
            if (value) {
                // セキュリティ上、値は表示しない
                this.log('SUCCESS', `セキュリティ環境変数 ${envVar} が設定されています`);
                
                // JWT_SECRETの強度チェック
                if (envVar === 'JWT_SECRET' && value.length < 32) {
                    this.log('WARNING', 'JWT_SECRETは32文字以上の強力な文字列を推奨します');
                }
            } else {
                this.log('WARNING', `セキュリティ環境変数 ${envVar} が設定されていません`);
            }
        }
    }

    // DynamoDBテーブルの検証
    async validateDynamoDBTable() {
        this.log('INFO', '=== DynamoDBテーブル検証開始 ===');
        
        const tableName = process.env.DYNAMODB_TABLE_NAME;
        if (!tableName) {
            this.log('ERROR', 'DYNAMODB_TABLE_NAME が設定されていません');
            return;
        }
        
        try {
            const command = new DescribeTableCommand({ TableName: tableName });
            const response = await this.dynamoClient.send(command);
            
            this.log('SUCCESS', `DynamoDBテーブル '${tableName}' が存在します`);
            
            // テーブル設定の詳細チェック
            const table = response.Table;
            
            // キースキーマの確認
            const expectedKeys = ['PK', 'SK'];
            const actualKeys = table.KeySchema.map(key => key.AttributeName);
            
            for (const expectedKey of expectedKeys) {
                if (actualKeys.includes(expectedKey)) {
                    this.log('SUCCESS', `キー属性 '${expectedKey}' が正しく設定されています`);
                } else {
                    this.log('ERROR', `キー属性 '${expectedKey}' が見つかりません`);
                }
            }
            
            // GSIの確認
            if (table.GlobalSecondaryIndexes && table.GlobalSecondaryIndexes.length > 0) {
                this.log('SUCCESS', `グローバルセカンダリインデックス: ${table.GlobalSecondaryIndexes.length}個`);
                
                for (const gsi of table.GlobalSecondaryIndexes) {
                    this.log('INFO', `GSI: ${gsi.IndexName}`, {
                        keys: gsi.KeySchema.map(k => k.AttributeName),
                        status: gsi.IndexStatus
                    });
                }
            } else {
                this.log('WARNING', 'グローバルセカンダリインデックスが設定されていません');
            }
            
            // 課金モードの確認
            this.log('INFO', `課金モード: ${table.BillingModeSummary?.BillingMode || 'PROVISIONED'}`);
            
            // 暗号化設定の確認
            if (table.SSEDescription?.Status === 'ENABLED') {
                this.log('SUCCESS', 'テーブル暗号化が有効です');
            } else {
                this.log('WARNING', 'テーブル暗号化が無効です（本番環境では有効化を推奨）');
            }
            
        } catch (error) {
            this.log('ERROR', `DynamoDBテーブル検証エラー: ${error.message}`, {
                errorCode: error.name,
                tableName: tableName
            });
        }
    }

    // Lambda関数の検証
    async validateLambdaFunction() {
        this.log('INFO', '=== Lambda関数検証開始 ===');
        
        const functionName = `team-dashboard-api-${this.environment}`;
        
        try {
            const command = new GetFunctionCommand({ FunctionName: functionName });
            const response = await this.lambdaClient.send(command);
            
            this.log('SUCCESS', `Lambda関数 '${functionName}' が存在します`);
            
            const config = response.Configuration;
            
            // ランタイム確認
            this.log('INFO', `ランタイム: ${config.Runtime}`);
            if (!config.Runtime.startsWith('java')) {
                this.log('WARNING', 'Javaランタイムではありません');
            }
            
            // メモリとタイムアウト確認
            this.log('INFO', `メモリサイズ: ${config.MemorySize}MB`);
            this.log('INFO', `タイムアウト: ${config.Timeout}秒`);
            
            if (config.MemorySize < 512) {
                this.log('WARNING', 'メモリサイズが小さすぎる可能性があります（512MB以上を推奨）');
            }
            
            if (config.Timeout < 30) {
                this.log('WARNING', 'タイムアウトが短すぎる可能性があります（30秒以上を推奨）');
            }
            
            // 環境変数確認
            if (config.Environment?.Variables) {
                const envVars = config.Environment.Variables;
                this.log('SUCCESS', `Lambda環境変数: ${Object.keys(envVars).length}個設定済み`);
                
                // 重要な環境変数の確認
                const importantVars = ['SPRING_PROFILES_ACTIVE', 'DYNAMODB_TABLE_NAME', 'AWS_REGION'];
                for (const varName of importantVars) {
                    if (envVars[varName]) {
                        this.log('SUCCESS', `Lambda環境変数 ${varName} が設定されています`);
                    } else {
                        this.log('WARNING', `Lambda環境変数 ${varName} が設定されていません`);
                    }
                }
            }
            
            // デッドレターキューの確認
            if (config.DeadLetterConfig?.TargetArn) {
                this.log('SUCCESS', 'デッドレターキューが設定されています');
            } else {
                this.log('WARNING', 'デッドレターキューが設定されていません（本番環境では設定を推奨）');
            }
            
            // VPC設定の確認
            if (config.VpcConfig?.VpcId) {
                this.log('INFO', `VPC設定: ${config.VpcConfig.VpcId}`);
            } else {
                this.log('INFO', 'VPC設定なし（パブリックアクセス）');
            }
            
        } catch (error) {
            if (error.name === 'ResourceNotFoundException') {
                this.log('WARNING', `Lambda関数 '${functionName}' が見つかりません（まだデプロイされていない可能性があります）`);
            } else {
                this.log('ERROR', `Lambda関数検証エラー: ${error.message}`, {
                    errorCode: error.name,
                    functionName: functionName
                });
            }
        }
    }

    // Systems Manager Parameter Storeの検証
    async validateParameterStore() {
        this.log('INFO', '=== Parameter Store検証開始 ===');
        
        const parameterPaths = [
            `/team-dashboard/${this.environment}/jwt-secret`,
            `/team-dashboard/${this.environment}/database-url`,
            `/team-dashboard/${this.environment}/api-key`
        ];
        
        for (const paramPath of parameterPaths) {
            try {
                const command = new GetParameterCommand({
                    Name: paramPath,
                    WithDecryption: false // セキュリティのため値は取得しない
                });
                
                await this.ssmClient.send(command);
                this.log('SUCCESS', `Parameter Store パラメータ '${paramPath}' が存在します`);
                
            } catch (error) {
                if (error.name === 'ParameterNotFound') {
                    this.log('WARNING', `Parameter Store パラメータ '${paramPath}' が見つかりません`);
                } else {
                    this.log('ERROR', `Parameter Store検証エラー: ${error.message}`, {
                        errorCode: error.name,
                        parameter: paramPath
                    });
                }
            }
        }
    }

    // 設定ファイルの検証
    validateConfigurationFiles() {
        this.log('INFO', '=== 設定ファイル検証開始 ===');
        
        const fs = require('fs');
        const path = require('path');
        
        // amplify.yml の確認
        const amplifyConfigPath = 'amplify.yml';
        if (fs.existsSync(amplifyConfigPath)) {
            this.log('SUCCESS', 'amplify.yml が存在します');
            
            try {
                const yaml = require('js-yaml');
                const amplifyConfig = yaml.load(fs.readFileSync(amplifyConfigPath, 'utf8'));
                
                // 基本構造の確認
                if (amplifyConfig.version) {
                    this.log('SUCCESS', `amplify.yml バージョン: ${amplifyConfig.version}`);
                }
                
                if (amplifyConfig.frontend) {
                    this.log('SUCCESS', 'フロントエンド設定が存在します');
                }
                
                if (amplifyConfig.backend) {
                    this.log('SUCCESS', 'バックエンド設定が存在します');
                } else {
                    this.log('WARNING', 'バックエンド設定が見つかりません');
                }
                
            } catch (error) {
                this.log('ERROR', `amplify.yml 解析エラー: ${error.message}`);
            }
        } else {
            this.log('ERROR', 'amplify.yml が見つかりません');
        }
        
        // template.yaml の確認
        const templatePath = 'template.yaml';
        if (fs.existsSync(templatePath)) {
            this.log('SUCCESS', 'template.yaml が存在します');
        } else {
            this.log('WARNING', 'template.yaml が見つかりません');
        }
        
        // pom.xml の確認
        const pomPath = 'backend/pom.xml';
        if (fs.existsSync(pomPath)) {
            this.log('SUCCESS', 'backend/pom.xml が存在します');
        } else {
            this.log('ERROR', 'backend/pom.xml が見つかりません');
        }
    }

    // 総合レポートの生成
    generateReport() {
        this.log('INFO', '=== 検証結果サマリー ===');
        
        console.log(`\n📊 検証結果:`);
        console.log(`   ✅ 成功: ${this.successes.length}項目`);
        console.log(`   ⚠️  警告: ${this.warnings.length}項目`);
        console.log(`   ❌ エラー: ${this.errors.length}項目`);
        
        if (this.errors.length > 0) {
            console.log(`\n❌ 修正が必要なエラー:`);
            this.errors.forEach((error, index) => {
                console.log(`   ${index + 1}. ${error.message}`);
            });
        }
        
        if (this.warnings.length > 0) {
            console.log(`\n⚠️  改善推奨項目:`);
            this.warnings.forEach((warning, index) => {
                console.log(`   ${index + 1}. ${warning.message}`);
            });
        }
        
        // 推奨アクション
        console.log(`\n🔧 推奨アクション:`);
        
        if (this.errors.length > 0) {
            console.log(`   1. エラー項目を修正してください`);
            console.log(`   2. 必須環境変数を設定してください`);
            console.log(`   3. AWS リソースが正しく作成されているか確認してください`);
        }
        
        if (this.warnings.length > 0) {
            console.log(`   4. 警告項目を確認し、必要に応じて改善してください`);
            console.log(`   5. セキュリティ設定を強化してください`);
            console.log(`   6. 本番環境では暗号化とモニタリングを有効にしてください`);
        }
        
        if (this.errors.length === 0 && this.warnings.length === 0) {
            console.log(`   🎉 すべての設定が正常です！デプロイの準備ができています。`);
        }
        
        return this.errors.length === 0;
    }

    // メイン検証プロセス
    async validate() {
        console.log('🚀 Team Dashboard AWS設定検証を開始します...\n');
        
        try {
            // 各検証を順次実行
            this.validateEnvironmentVariables();
            this.validateConfigurationFiles();
            await this.validateDynamoDBTable();
            await this.validateLambdaFunction();
            await this.validateParameterStore();
            
            // 結果レポート生成
            const isValid = this.generateReport();
            
            console.log('\n🏁 検証完了');
            process.exit(isValid ? 0 : 1);
            
        } catch (error) {
            this.log('ERROR', `検証プロセスでエラーが発生しました: ${error.message}`);
            process.exit(1);
        }
    }
}

// スクリプト実行
if (require.main === module) {
    // 必要なパッケージの確認
    try {
        require('@aws-sdk/client-dynamodb');
        require('@aws-sdk/client-lambda');
        require('@aws-sdk/client-api-gateway');
        require('@aws-sdk/client-ssm');
        require('js-yaml');
    } catch (error) {
        console.error('❌ 必要なパッケージがインストールされていません:');
        console.error('   npm install @aws-sdk/client-dynamodb @aws-sdk/client-lambda @aws-sdk/client-api-gateway @aws-sdk/client-ssm js-yaml');
        process.exit(1);
    }
    
    const validator = new AWSConfigValidator();
    validator.validate();
}

module.exports = AWSConfigValidator;