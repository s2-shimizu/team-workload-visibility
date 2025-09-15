# EC2 デプロイスクリプト（WebSocket対応）
param(
    [string]$Environment = "dev",
    [string]$AppName = "team-dashboard",
    [string]$Region = "ap-northeast-1",
    [string]$InstanceType = "t3.medium",
    [string]$KeyName = "",
    [switch]$CreateInstance = $false
)

Write-Host "=== EC2 デプロイ（WebSocket対応） ===" -ForegroundColor Green
Write-Host "Environment: $Environment" -ForegroundColor Yellow
Write-Host "Instance Type: $InstanceType" -ForegroundColor Yellow
Write-Host "Region: $Region" -ForegroundColor Yellow
Write-Host ""

# 前提条件チェック
Write-Host "1. 前提条件チェック" -ForegroundColor Cyan

# AWS CLI確認
try {
    $awsVersion = aws --version
    Write-Host "✅ AWS CLI: $awsVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ AWS CLIが見つかりません。" -ForegroundColor Red
    exit 1
}

# AWS認証確認
try {
    $identity = aws sts get-caller-identity --output json | ConvertFrom-Json
    Write-Host "✅ AWS認証: $($identity.Arn)" -ForegroundColor Green
    $accountId = $identity.Account
} catch {
    Write-Host "❌ AWS認証が設定されていません。" -ForegroundColor Red
    exit 1
}

# キーペア確認
if ($KeyName -eq "") {
    Write-Host "⚠️ キーペア名が指定されていません。" -ForegroundColor Yellow
    Write-Host "   既存のキーペアを使用するか、新しく作成してください。" -ForegroundColor Gray
    
    # 既存キーペア一覧表示
    try {
        $keyPairs = aws ec2 describe-key-pairs --region $Region --output json | ConvertFrom-Json
        if ($keyPairs.KeyPairs.Count -gt 0) {
            Write-Host "   既存のキーペア:" -ForegroundColor Gray
            foreach ($key in $keyPairs.KeyPairs) {
                Write-Host "     - $($key.KeyName)" -ForegroundColor Gray
            }
        }
    } catch {
        Write-Host "   キーペア一覧の取得に失敗しました。" -ForegroundColor Yellow
    }
    
    $KeyName = Read-Host "使用するキーペア名を入力してください"
    if ($KeyName -eq "") {
        Write-Host "❌ キーペア名が必要です。" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""

# 2. Mavenビルド
Write-Host "2. Mavenビルド" -ForegroundColor Cyan

try {
    Set-Location backend
    
    Write-Host "Maven clean..." -ForegroundColor Gray
    mvn clean -q
    if ($LASTEXITCODE -ne 0) {
        throw "Maven clean failed"
    }
    
    Write-Host "Maven package..." -ForegroundColor Gray
    mvn package -Plambda -DskipTests -q
    if ($LASTEXITCODE -ne 0) {
        throw "Maven package failed"
    }
    
    # JARファイル確認
    $jarFiles = Get-ChildItem -Path "target" -Name "*.jar" | Where-Object { $_ -like "*lambda*" }
    if ($jarFiles.Count -gt 0) {
        Write-Host "✅ JAR生成: $($jarFiles[0])" -ForegroundColor Green
        $script:JarFile = "target/$($jarFiles[0])"
    } else {
        throw "JARファイルが見つかりません"
    }
    
} catch {
    Write-Host "❌ Mavenビルドエラー: $($_.Exception.Message)" -ForegroundColor Red
    Set-Location ..
    exit 1
} finally {
    Set-Location ..
}

Write-Host ""

# 3. EC2インスタンス作成（オプション）
if ($CreateInstance) {
    Write-Host "3. EC2インスタンス作成" -ForegroundColor Cyan
    
    # セキュリティグループ作成
    Write-Host "セキュリティグループを作成中..." -ForegroundColor Gray
    try {
        $sgResult = aws ec2 create-security-group `
            --group-name "$AppName-$Environment-sg" `
            --description "Security group for $AppName $Environment" `
            --region $Region --output json | ConvertFrom-Json
        
        $securityGroupId = $sgResult.GroupId
        Write-Host "✅ セキュリティグループ作成: $securityGroupId" -ForegroundColor Green
        
        # セキュリティグループルール追加
        Write-Host "セキュリティグループルールを追加中..." -ForegroundColor Gray
        
        # HTTP (80)
        aws ec2 authorize-security-group-ingress `
            --group-id $securityGroupId `
            --protocol tcp --port 80 --cidr 0.0.0.0/0 `
            --region $Region
        
        # HTTPS (443)
        aws ec2 authorize-security-group-ingress `
            --group-id $securityGroupId `
            --protocol tcp --port 443 --cidr 0.0.0.0/0 `
            --region $Region
        
        # Application (8080)
        aws ec2 authorize-security-group-ingress `
            --group-id $securityGroupId `
            --protocol tcp --port 8080 --cidr 0.0.0.0/0 `
            --region $Region
        
        # SSH (22)
        aws ec2 authorize-security-group-ingress `
            --group-id $securityGroupId `
            --protocol tcp --port 22 --cidr 0.0.0.0/0 `
            --region $Region
        
        Write-Host "✅ セキュリティグループルール追加完了" -ForegroundColor Green
        
    } catch {
        Write-Host "⚠️ セキュリティグループは既に存在する可能性があります" -ForegroundColor Yellow
        # 既存のセキュリティグループを検索
        try {
            $existingSg = aws ec2 describe-security-groups `
                --group-names "$AppName-$Environment-sg" `
                --region $Region --output json | ConvertFrom-Json
            $securityGroupId = $existingSg.SecurityGroups[0].GroupId
            Write-Host "✅ 既存セキュリティグループを使用: $securityGroupId" -ForegroundColor Green
        } catch {
            Write-Host "❌ セキュリティグループの作成/取得に失敗しました" -ForegroundColor Red
            exit 1
        }
    }
    
    # EC2インスタンス起動
    Write-Host "EC2インスタンスを起動中..." -ForegroundColor Gray
    try {
        # Amazon Linux 2 AMI ID (ap-northeast-1)
        $amiId = "ami-0c3fd0f5d33134a76"
        
        $instanceResult = aws ec2 run-instances `
            --image-id $amiId `
            --count 1 `
            --instance-type $InstanceType `
            --key-name $KeyName `
            --security-group-ids $securityGroupId `
            --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$AppName-$Environment},{Key=Environment,Value=$Environment}]" `
            --region $Region --output json | ConvertFrom-Json
        
        $instanceId = $instanceResult.Instances[0].InstanceId
        Write-Host "✅ EC2インスタンス起動: $instanceId" -ForegroundColor Green
        
        # インスタンスの起動完了を待機
        Write-Host "インスタンスの起動完了を待機中..." -ForegroundColor Gray
        aws ec2 wait instance-running --instance-ids $instanceId --region $Region
        
        # パブリックIPアドレス取得
        $instanceInfo = aws ec2 describe-instances --instance-ids $instanceId --region $Region --output json | ConvertFrom-Json
        $publicIp = $instanceInfo.Reservations[0].Instances[0].PublicIpAddress
        
        Write-Host "✅ インスタンス起動完了" -ForegroundColor Green
        Write-Host "   パブリックIP: $publicIp" -ForegroundColor Gray
        
        $script:InstanceId = $instanceId
        $script:PublicIp = $publicIp
        
    } catch {
        Write-Host "❌ EC2インスタンス起動エラー: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
    
    Write-Host ""
} else {
    Write-Host "3. 既存EC2インスタンス使用" -ForegroundColor Cyan
    Write-Host "既存のEC2インスタンスを使用します。" -ForegroundColor Gray
    Write-Host "インスタンスのパブリックIPアドレスを入力してください。" -ForegroundColor Yellow
    $script:PublicIp = Read-Host "パブリックIPアドレス"
    
    if ($script:PublicIp -eq "") {
        Write-Host "❌ パブリックIPアドレスが必要です。" -ForegroundColor Red
        exit 1
    }
    Write-Host ""
}

# 4. アプリケーションデプロイ
Write-Host "4. アプリケーションデプロイ" -ForegroundColor Cyan

# デプロイスクリプト作成
Write-Host "デプロイスクリプトを作成中..." -ForegroundColor Gray
$deployScript = @"
#!/bin/bash
set -e

echo "=== Team Dashboard Deployment ==="

# Java 17インストール
echo "Installing Java 17..."
sudo yum update -y
sudo yum install -y java-17-amazon-corretto-headless

# アプリケーションディレクトリ作成
sudo mkdir -p /opt/team-dashboard
sudo chown ec2-user:ec2-user /opt/team-dashboard

# 既存プロセス停止
echo "Stopping existing application..."
sudo pkill -f "team-dashboard" || true

# アプリケーション配置
echo "Deploying application..."
cp ~/team-dashboard.jar /opt/team-dashboard/

# 環境変数設定
cat > /opt/team-dashboard/app.env << EOF
SPRING_PROFILES_ACTIVE=prod,dynamodb
AWS_REGION=$Region
WORKLOAD_STATUS_TABLE=WorkloadStatus-$Environment
TEAM_ISSUE_TABLE=TeamIssue-$Environment
SERVER_PORT=8080
EOF

# systemdサービス作成
sudo tee /etc/systemd/system/team-dashboard.service > /dev/null << EOF
[Unit]
Description=Team Dashboard Application
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/team-dashboard
EnvironmentFile=/opt/team-dashboard/app.env
ExecStart=/usr/bin/java -jar /opt/team-dashboard/team-dashboard.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# サービス有効化・開始
echo "Starting application service..."
sudo systemctl daemon-reload
sudo systemctl enable team-dashboard
sudo systemctl start team-dashboard

# ステータス確認
echo "Checking application status..."
sleep 10
sudo systemctl status team-dashboard --no-pager

echo "=== Deployment Complete ==="
echo "Application URL: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):8080"
"@

$deployScript | Out-File -FilePath "deploy-script.sh" -Encoding UTF8

# ファイル転送
Write-Host "ファイルを転送中..." -ForegroundColor Gray
try {
    # JARファイル転送
    scp -i "$KeyName.pem" -o StrictHostKeyChecking=no "$($script:JarFile)" "ec2-user@$($script:PublicIp):~/team-dashboard.jar"
    if ($LASTEXITCODE -ne 0) {
        throw "JAR file transfer failed"
    }
    
    # デプロイスクリプト転送
    scp -i "$KeyName.pem" -o StrictHostKeyChecking=no "deploy-script.sh" "ec2-user@$($script:PublicIp):~/deploy-script.sh"
    if ($LASTEXITCODE -ne 0) {
        throw "Deploy script transfer failed"
    }
    
    Write-Host "✅ ファイル転送完了" -ForegroundColor Green
} catch {
    Write-Host "❌ ファイル転送エラー: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   キーファイル '$KeyName.pem' が存在することを確認してください。" -ForegroundColor Yellow
    exit 1
}

# リモートデプロイ実行
Write-Host "リモートデプロイを実行中..." -ForegroundColor Gray
try {
    ssh -i "$KeyName.pem" -o StrictHostKeyChecking=no "ec2-user@$($script:PublicIp)" "chmod +x ~/deploy-script.sh && ~/deploy-script.sh"
    if ($LASTEXITCODE -ne 0) {
        throw "Remote deployment failed"
    }
    
    Write-Host "✅ リモートデプロイ完了" -ForegroundColor Green
} catch {
    Write-Host "❌ リモートデプロイエラー: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""

# 5. デプロイ確認
Write-Host "5. デプロイ確認" -ForegroundColor Cyan

$appUrl = "http://$($script:PublicIp):8080"

Write-Host "アプリケーションの起動を待機中..." -ForegroundColor Gray
Start-Sleep -Seconds 15

try {
    $healthCheck = Invoke-RestMethod -Uri "$appUrl/api/status" -TimeoutSec 10
    Write-Host "✅ ヘルスチェック成功" -ForegroundColor Green
    Write-Host "   ステータス: $($healthCheck.status)" -ForegroundColor Gray
    Write-Host "   バージョン: $($healthCheck.version)" -ForegroundColor Gray
} catch {
    Write-Host "⚠️ ヘルスチェック失敗: $($_.Exception.Message)" -ForegroundColor Yellow
    Write-Host "   アプリケーションの起動に時間がかかっている可能性があります。" -ForegroundColor Gray
}

Write-Host ""
Write-Host "=== EC2デプロイ完了 ===" -ForegroundColor Green

Write-Host ""
Write-Host "🎉 デプロイ成功！" -ForegroundColor Green
Write-Host "アプリケーションURL: $appUrl" -ForegroundColor Yellow
if ($script:InstanceId) {
    Write-Host "EC2インスタンスID: $($script:InstanceId)" -ForegroundColor Gray
}
Write-Host "パブリックIP: $($script:PublicIp)" -ForegroundColor Gray

Write-Host ""
Write-Host "次のステップ:" -ForegroundColor Cyan
Write-Host "1. ヘルスチェック:" -ForegroundColor Gray
Write-Host "   curl $appUrl/api/status" -ForegroundColor Gray
Write-Host ""
Write-Host "2. リアルタイム機能テスト:" -ForegroundColor Gray
Write-Host "   .\test-realtime-updates.ps1 -BaseUrl '$appUrl'" -ForegroundColor Gray
Write-Host ""
Write-Host "3. SSH接続（トラブルシューティング）:" -ForegroundColor Gray
Write-Host "   ssh -i $KeyName.pem ec2-user@$($script:PublicIp)" -ForegroundColor Gray
Write-Host ""
Write-Host "4. ログ確認:" -ForegroundColor Gray
Write-Host "   ssh -i $KeyName.pem ec2-user@$($script:PublicIp) 'sudo journalctl -u team-dashboard -f'" -ForegroundColor Gray
Write-Host ""
Write-Host "5. フロントエンド設定更新:" -ForegroundColor Gray
Write-Host "   frontend/js/aws-config.js のendpointを '$appUrl' に更新" -ForegroundColor Gray

Write-Host ""
Write-Host "使用方法:" -ForegroundColor Yellow
Write-Host "  新規インスタンス: .\deploy-ec2.ps1 -Environment dev -KeyName my-key -CreateInstance" -ForegroundColor Gray
Write-Host "  既存インスタンス: .\deploy-ec2.ps1 -Environment dev -KeyName my-key" -ForegroundColor Gray
Write-Host "  本番環境: .\deploy-ec2.ps1 -Environment prod -KeyName my-key -InstanceType t3.large" -ForegroundColor Gray

# クリーンアップ
Remove-Item -Path "deploy-script.sh" -Force -ErrorAction SilentlyContinue