# ECS Fargate デプロイスクリプト（WebSocket対応）
param(
    [string]$Environment = "dev",
    [string]$AppName = "team-dashboard",
    [string]$Region = "ap-northeast-1",
    [switch]$BuildOnly = $false,
    [switch]$DeployOnly = $false
)

Write-Host "=== ECS Fargate デプロイ（WebSocket対応） ===" -ForegroundColor Green
Write-Host "Environment: $Environment" -ForegroundColor Yellow
Write-Host "App Name: $AppName" -ForegroundColor Yellow
Write-Host "Region: $Region" -ForegroundColor Yellow
Write-Host ""

# 前提条件チェック
Write-Host "1. 前提条件チェック" -ForegroundColor Cyan

# Docker確認
try {
    $dockerVersion = docker --version
    Write-Host "✅ Docker: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Dockerが見つかりません。Docker Desktopをインストールしてください。" -ForegroundColor Red
    exit 1
}

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

Write-Host ""

# 2. Dockerイメージビルド
if (-not $DeployOnly) {
    Write-Host "2. Dockerイメージビルド" -ForegroundColor Cyan
    
    # Dockerfileを作成
    Write-Host "Dockerfileを作成中..." -ForegroundColor Gray
    $dockerfileContent = @"
FROM openjdk:17-jre-slim

# 作業ディレクトリ
WORKDIR /app

# アプリケーションJARをコピー
COPY backend/target/team-dashboard-backend-*-lambda.jar app.jar

# ポート公開
EXPOSE 8080

# ヘルスチェック
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/api/status || exit 1

# アプリケーション実行
ENTRYPOINT ["java", "-Dspring.profiles.active=prod,dynamodb", "-jar", "app.jar"]
"@
    
    $dockerfileContent | Out-File -FilePath "Dockerfile" -Encoding UTF8
    
    # Mavenビルド
    Write-Host "Mavenビルドを実行中..." -ForegroundColor Gray
    try {
        Set-Location backend
        mvn clean package -Plambda -DskipTests -q
        if ($LASTEXITCODE -ne 0) {
            throw "Maven build failed"
        }
        Set-Location ..
        Write-Host "✅ Mavenビルド完了" -ForegroundColor Green
    } catch {
        Write-Host "❌ Mavenビルドエラー: $($_.Exception.Message)" -ForegroundColor Red
        Set-Location ..
        exit 1
    }
    
    # Dockerイメージビルド
    Write-Host "Dockerイメージをビルド中..." -ForegroundColor Gray
    try {
        docker build -t "$AppName-$Environment" .
        if ($LASTEXITCODE -ne 0) {
            throw "Docker build failed"
        }
        Write-Host "✅ Dockerイメージビルド完了" -ForegroundColor Green
    } catch {
        Write-Host "❌ Dockerビルドエラー: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
    
    Write-Host ""
}

# 3. ECRリポジトリ作成・プッシュ
if (-not $DeployOnly) {
    Write-Host "3. ECRリポジトリ作成・プッシュ" -ForegroundColor Cyan
    
    $ecrRepo = "$accountId.dkr.ecr.$Region.amazonaws.com/$AppName-$Environment"
    
    # ECRリポジトリ作成
    Write-Host "ECRリポジトリを作成中..." -ForegroundColor Gray
    try {
        aws ecr create-repository --repository-name "$AppName-$Environment" --region $Region 2>$null
        Write-Host "✅ ECRリポジトリ作成完了（または既存）" -ForegroundColor Green
    } catch {
        Write-Host "⚠️ ECRリポジトリは既に存在します" -ForegroundColor Yellow
    }
    
    # ECRログイン
    Write-Host "ECRにログイン中..." -ForegroundColor Gray
    try {
        aws ecr get-login-password --region $Region | docker login --username AWS --password-stdin $ecrRepo
        if ($LASTEXITCODE -ne 0) {
            throw "ECR login failed"
        }
        Write-Host "✅ ECRログイン完了" -ForegroundColor Green
    } catch {
        Write-Host "❌ ECRログインエラー: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
    
    # イメージタグ付け・プッシュ
    Write-Host "イメージをプッシュ中..." -ForegroundColor Gray
    try {
        docker tag "$AppName-$Environment:latest" "$ecrRepo:latest"
        docker push "$ecrRepo:latest"
        if ($LASTEXITCODE -ne 0) {
            throw "Docker push failed"
        }
        Write-Host "✅ イメージプッシュ完了" -ForegroundColor Green
    } catch {
        Write-Host "❌ イメージプッシュエラー: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
    
    Write-Host ""
}

if ($BuildOnly) {
    Write-Host "=== ビルド完了 ===" -ForegroundColor Green
    Write-Host "次のステップ: .\deploy-ecs-fargate.ps1 -Environment $Environment -DeployOnly" -ForegroundColor Yellow
    exit 0
}

# 4. ECSインフラストラクチャ作成
if (-not $BuildOnly) {
    Write-Host "4. ECSインフラストラクチャ作成" -ForegroundColor Cyan
    
    # CloudFormationテンプレート作成
    Write-Host "CloudFormationテンプレートを作成中..." -ForegroundColor Gray
    
    $cfTemplate = @"
AWSTemplateFormatVersion: '2010-09-09'
Description: 'Team Dashboard ECS Fargate Infrastructure'

Parameters:
  Environment:
    Type: String
    Default: dev
  AppName:
    Type: String
    Default: team-dashboard

Resources:
  # VPC
  VPC:
    Type: AWS::EC2::VPC
    Properties:
      CidrBlock: 10.0.0.0/16
      EnableDnsHostnames: true
      EnableDnsSupport: true
      Tags:
        - Key: Name
          Value: !Sub '\${AppName}-\${Environment}-vpc'

  # Internet Gateway
  InternetGateway:
    Type: AWS::EC2::InternetGateway
    Properties:
      Tags:
        - Key: Name
          Value: !Sub '\${AppName}-\${Environment}-igw'

  InternetGatewayAttachment:
    Type: AWS::EC2::VPCGatewayAttachment
    Properties:
      InternetGatewayId: !Ref InternetGateway
      VpcId: !Ref VPC

  # Subnets
  PublicSubnet1:
    Type: AWS::EC2::Subnet
    Properties:
      VpcId: !Ref VPC
      AvailabilityZone: !Select [0, !GetAZs '']
      CidrBlock: 10.0.1.0/24
      MapPublicIpOnLaunch: true
      Tags:
        - Key: Name
          Value: !Sub '\${AppName}-\${Environment}-public-subnet-1'

  PublicSubnet2:
    Type: AWS::EC2::Subnet
    Properties:
      VpcId: !Ref VPC
      AvailabilityZone: !Select [1, !GetAZs '']
      CidrBlock: 10.0.2.0/24
      MapPublicIpOnLaunch: true
      Tags:
        - Key: Name
          Value: !Sub '\${AppName}-\${Environment}-public-subnet-2'

  # Route Table
  PublicRouteTable:
    Type: AWS::EC2::RouteTable
    Properties:
      VpcId: !Ref VPC
      Tags:
        - Key: Name
          Value: !Sub '\${AppName}-\${Environment}-public-routes'

  DefaultPublicRoute:
    Type: AWS::EC2::Route
    DependsOn: InternetGatewayAttachment
    Properties:
      RouteTableId: !Ref PublicRouteTable
      DestinationCidrBlock: 0.0.0.0/0
      GatewayId: !Ref InternetGateway

  PublicSubnet1RouteTableAssociation:
    Type: AWS::EC2::SubnetRouteTableAssociation
    Properties:
      RouteTableId: !Ref PublicRouteTable
      SubnetId: !Ref PublicSubnet1

  PublicSubnet2RouteTableAssociation:
    Type: AWS::EC2::SubnetRouteTableAssociation
    Properties:
      RouteTableId: !Ref PublicRouteTable
      SubnetId: !Ref PublicSubnet2

  # Security Groups
  LoadBalancerSecurityGroup:
    Type: AWS::EC2::SecurityGroup
    Properties:
      GroupDescription: Access to the public facing load balancer
      VpcId: !Ref VPC
      SecurityGroupIngress:
        - IpProtocol: tcp
          FromPort: 80
          ToPort: 80
          CidrIp: 0.0.0.0/0
        - IpProtocol: tcp
          FromPort: 443
          ToPort: 443
          CidrIp: 0.0.0.0/0
      Tags:
        - Key: Name
          Value: !Sub '\${AppName}-\${Environment}-alb-sg'

  ECSSecurityGroup:
    Type: AWS::EC2::SecurityGroup
    Properties:
      GroupDescription: Access to the ECS containers
      VpcId: !Ref VPC
      SecurityGroupIngress:
        - IpProtocol: tcp
          FromPort: 8080
          ToPort: 8080
          SourceSecurityGroupId: !Ref LoadBalancerSecurityGroup
      Tags:
        - Key: Name
          Value: !Sub '\${AppName}-\${Environment}-ecs-sg'

  # Application Load Balancer
  LoadBalancer:
    Type: AWS::ElasticLoadBalancingV2::LoadBalancer
    Properties:
      Name: !Sub '\${AppName}-\${Environment}-alb'
      Subnets:
        - !Ref PublicSubnet1
        - !Ref PublicSubnet2
      SecurityGroups:
        - !Ref LoadBalancerSecurityGroup
      Tags:
        - Key: Name
          Value: !Sub '\${AppName}-\${Environment}-alb'

  TargetGroup:
    Type: AWS::ElasticLoadBalancingV2::TargetGroup
    Properties:
      Name: !Sub '\${AppName}-\${Environment}-tg'
      Port: 8080
      Protocol: HTTP
      VpcId: !Ref VPC
      TargetType: ip
      HealthCheckPath: /api/status
      HealthCheckProtocol: HTTP
      HealthCheckIntervalSeconds: 30
      HealthCheckTimeoutSeconds: 5
      HealthyThresholdCount: 2
      UnhealthyThresholdCount: 3

  LoadBalancerListener:
    Type: AWS::ElasticLoadBalancingV2::Listener
    Properties:
      DefaultActions:
        - Type: forward
          TargetGroupArn: !Ref TargetGroup
      LoadBalancerArn: !Ref LoadBalancer
      Port: 80
      Protocol: HTTP

  # ECS Cluster
  ECSCluster:
    Type: AWS::ECS::Cluster
    Properties:
      ClusterName: !Sub '\${AppName}-\${Environment}-cluster'
      CapacityProviders:
        - FARGATE
        - FARGATE_SPOT
      DefaultCapacityProviderStrategy:
        - CapacityProvider: FARGATE
          Weight: 1

  # ECS Task Definition
  TaskDefinition:
    Type: AWS::ECS::TaskDefinition
    Properties:
      Family: !Sub '\${AppName}-\${Environment}-task'
      Cpu: 512
      Memory: 1024
      NetworkMode: awsvpc
      RequiresCompatibilities:
        - FARGATE
      ExecutionRoleArn: !Ref ECSTaskExecutionRole
      TaskRoleArn: !Ref ECSTaskRole
      ContainerDefinitions:
        - Name: !Sub '\${AppName}-container'
          Image: !Sub '\${AWS::AccountId}.dkr.ecr.\${AWS::Region}.amazonaws.com/\${AppName}-\${Environment}:latest'
          PortMappings:
            - ContainerPort: 8080
              Protocol: tcp
          LogConfiguration:
            LogDriver: awslogs
            Options:
              awslogs-group: !Ref CloudWatchLogsGroup
              awslogs-region: !Ref AWS::Region
              awslogs-stream-prefix: ecs
          Environment:
            - Name: SPRING_PROFILES_ACTIVE
              Value: prod,dynamodb
            - Name: AWS_REGION
              Value: !Ref AWS::Region
            - Name: WORKLOAD_STATUS_TABLE
              Value: !Sub 'WorkloadStatus-\${Environment}'
            - Name: TEAM_ISSUE_TABLE
              Value: !Sub 'TeamIssue-\${Environment}'

  # ECS Service
  ECSService:
    Type: AWS::ECS::Service
    DependsOn: LoadBalancerListener
    Properties:
      ServiceName: !Sub '\${AppName}-\${Environment}-service'
      Cluster: !Ref ECSCluster
      LaunchType: FARGATE
      DeploymentConfiguration:
        MaximumPercent: 200
        MinimumHealthyPercent: 75
      DesiredCount: 2
      NetworkConfiguration:
        AwsvpcConfiguration:
          AssignPublicIp: ENABLED
          SecurityGroups:
            - !Ref ECSSecurityGroup
          Subnets:
            - !Ref PublicSubnet1
            - !Ref PublicSubnet2
      TaskDefinition: !Ref TaskDefinition
      LoadBalancers:
        - ContainerName: !Sub '\${AppName}-container'
          ContainerPort: 8080
          TargetGroupArn: !Ref TargetGroup

  # IAM Roles
  ECSTaskExecutionRole:
    Type: AWS::IAM::Role
    Properties:
      AssumeRolePolicyDocument:
        Statement:
          - Effect: Allow
            Principal:
              Service: ecs-tasks.amazonaws.com
            Action: sts:AssumeRole
      ManagedPolicyArns:
        - arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy

  ECSTaskRole:
    Type: AWS::IAM::Role
    Properties:
      AssumeRolePolicyDocument:
        Statement:
          - Effect: Allow
            Principal:
              Service: ecs-tasks.amazonaws.com
            Action: sts:AssumeRole
      Policies:
        - PolicyName: DynamoDBAccess
          PolicyDocument:
            Statement:
              - Effect: Allow
                Action:
                  - dynamodb:GetItem
                  - dynamodb:PutItem
                  - dynamodb:UpdateItem
                  - dynamodb:DeleteItem
                  - dynamodb:Query
                  - dynamodb:Scan
                Resource:
                  - !Sub 'arn:aws:dynamodb:\${AWS::Region}:\${AWS::AccountId}:table/WorkloadStatus-\${Environment}'
                  - !Sub 'arn:aws:dynamodb:\${AWS::Region}:\${AWS::AccountId}:table/TeamIssue-\${Environment}'

  # CloudWatch Logs
  CloudWatchLogsGroup:
    Type: AWS::Logs::LogGroup
    Properties:
      LogGroupName: !Sub '/ecs/\${AppName}-\${Environment}'
      RetentionInDays: 7

Outputs:
  LoadBalancerURL:
    Description: URL of the load balancer
    Value: !Sub 'http://\${LoadBalancer.DNSName}'
    Export:
      Name: !Sub '\${AppName}-\${Environment}-LoadBalancerURL'
  
  ECSCluster:
    Description: ECS Cluster Name
    Value: !Ref ECSCluster
    Export:
      Name: !Sub '\${AppName}-\${Environment}-ECSCluster'
"@
    
    $cfTemplate | Out-File -FilePath "ecs-infrastructure.yaml" -Encoding UTF8
    
    # CloudFormationスタックデプロイ
    Write-Host "ECSインフラストラクチャをデプロイ中..." -ForegroundColor Gray
    try {
        aws cloudformation deploy `
            --template-file ecs-infrastructure.yaml `
            --stack-name "$AppName-$Environment-ecs" `
            --parameter-overrides Environment=$Environment AppName=$AppName `
            --capabilities CAPABILITY_IAM `
            --region $Region
        
        if ($LASTEXITCODE -ne 0) {
            throw "CloudFormation deploy failed"
        }
        
        Write-Host "✅ ECSインフラストラクチャデプロイ完了" -ForegroundColor Green
    } catch {
        Write-Host "❌ ECSデプロイエラー: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
    
    Write-Host ""
}

# 5. デプロイ結果確認
Write-Host "5. デプロイ結果確認" -ForegroundColor Cyan

try {
    $stackInfo = aws cloudformation describe-stacks --stack-name "$AppName-$Environment-ecs" --region $Region --output json | ConvertFrom-Json
    
    if ($stackInfo.Stacks.Count -gt 0) {
        $stack = $stackInfo.Stacks[0]
        Write-Host "✅ スタックステータス: $($stack.StackStatus)" -ForegroundColor Green
        
        if ($stack.Outputs) {
            Write-Host ""
            Write-Host "デプロイ結果:" -ForegroundColor Yellow
            foreach ($output in $stack.Outputs) {
                Write-Host "  $($output.OutputKey): $($output.OutputValue)" -ForegroundColor Gray
                
                if ($output.OutputKey -eq "LoadBalancerURL") {
                    $script:LoadBalancerURL = $output.OutputValue
                }
            }
        }
    }
} catch {
    Write-Host "⚠️ スタック情報取得エラー: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== ECS Fargateデプロイ完了 ===" -ForegroundColor Green

if ($script:LoadBalancerURL) {
    Write-Host ""
    Write-Host "🎉 デプロイ成功！" -ForegroundColor Green
    Write-Host "アプリケーションURL: $($script:LoadBalancerURL)" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "次のステップ:" -ForegroundColor Cyan
    Write-Host "1. ヘルスチェック:" -ForegroundColor Gray
    Write-Host "   curl $($script:LoadBalancerURL)/api/status" -ForegroundColor Gray
    Write-Host ""
    Write-Host "2. リアルタイム機能テスト:" -ForegroundColor Gray
    Write-Host "   .\test-realtime-updates.ps1 -BaseUrl '$($script:LoadBalancerURL)'" -ForegroundColor Gray
    Write-Host ""
    Write-Host "3. フロントエンド設定更新:" -ForegroundColor Gray
    Write-Host "   frontend/js/aws-config.js のendpointを '$($script:LoadBalancerURL)' に更新" -ForegroundColor Gray
    Write-Host ""
    Write-Host "4. WebSocket接続確認:" -ForegroundColor Gray
    Write-Host "   ブラウザでダッシュボードを開いて接続状態を確認" -ForegroundColor Gray
}

Write-Host ""
Write-Host "使用方法:" -ForegroundColor Yellow
Write-Host "  フルデプロイ: .\deploy-ecs-fargate.ps1 -Environment dev" -ForegroundColor Gray
Write-Host "  ビルドのみ: .\deploy-ecs-fargate.ps1 -Environment dev -BuildOnly" -ForegroundColor Gray
Write-Host "  デプロイのみ: .\deploy-ecs-fargate.ps1 -Environment dev -DeployOnly" -ForegroundColor Gray
Write-Host "  本番環境: .\deploy-ecs-fargate.ps1 -Environment prod" -ForegroundColor Gray