# 設計書

## 概要

既存のチームダッシュボードアプリケーションを拡張し、チームメンバーの負荷状況と困りごとの可視化機能を追加する。既存のDailyReportエンティティのworkloadLevelを活用し、新たにWorkloadStatusとTeamIssueエンティティを追加してリアルタイムな状況共有を実現する。

## アーキテクチャ

### システム構成
- **フロントエンド**: 既存のHTML/CSS/JavaScriptベースのSPA
- **バックエンド**: Spring Boot REST API
- **データベース**: PostgreSQL（H2での開発も対応）
- **認証**: 既存のSpring Securityベース認証システム

### 新機能の統合方針
既存のダッシュボード画面に新しいセクションを追加し、日報投稿フォームを拡張する形で実装する。

## コンポーネントとインターフェース

### 1. データモデル

#### WorkloadStatus エンティティ
```java
@Entity
@Table(name = "workload_status")
public class WorkloadStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    private WorkloadLevel workloadLevel; // HIGH, MEDIUM, LOW
    
    private Integer projectCount; // 任意入力
    private Integer taskCount;    // 任意入力
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

#### TeamIssue エンティティ
```java
@Entity
@Table(name = "team_issues")
public class TeamIssue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Enumerated(EnumType.STRING)
    private IssueStatus status; // OPEN, RESOLVED
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
```

#### IssueComment エンティティ
```java
@Entity
@Table(name = "issue_comments")
public class IssueComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private TeamIssue issue;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

### 2. REST APIエンドポイント

#### WorkloadStatus API
- `GET /api/workload-status` - 全メンバーの負荷状況取得
- `GET /api/workload-status/my` - 自分の負荷状況取得
- `POST /api/workload-status` - 負荷状況更新
- `PUT /api/workload-status/{id}` - 負荷状況編集

#### TeamIssue API
- `GET /api/team-issues` - チーム困りごと一覧取得
- `POST /api/team-issues` - 困りごと投稿
- `PUT /api/team-issues/{id}/resolve` - 困りごと解決マーク
- `POST /api/team-issues/{id}/comments` - コメント投稿
- `GET /api/team-issues/{id}/comments` - コメント一覧取得

### 3. フロントエンド構成

#### 新規画面・セクション
1. **負荷状況セクション** (ダッシュボードに追加)
   - チーム全体の負荷状況一覧表示
   - 色分けによる視覚的表現（赤：高、黄：中、緑：低）
   - 最終更新日時表示

2. **困りごとセクション** (ダッシュボードに追加)
   - 未解決の困りごと一覧
   - 簡単なコメント機能
   - 解決マーク機能

3. **負荷状況更新フォーム** (新規モーダル)
   - 負荷レベル選択（必須）
   - 案件数・タスク数入力（任意）

4. **困りごと投稿フォーム** (新規モーダル)
   - 困りごと内容入力（必須）

#### 既存画面の拡張
- ダッシュボードのナビゲーションに「負荷状況」「困りごと」タブを追加
- 日報投稿時に負荷状況も同時更新するオプション

## データモデル

### エンティティ関係図
```
User (既存)
├── DailyReport (既存) - workloadLevel活用
├── WorkloadStatus (新規) - リアルタイム負荷状況
└── TeamIssue (新規) - 困りごと
    └── IssueComment (新規) - コメント
```

### 列挙型定義
```java
public enum WorkloadLevel {
    LOW("低"),
    MEDIUM("中"),
    HIGH("高");
}

public enum IssueStatus {
    OPEN("未解決"),
    RESOLVED("解決済み");
}
```

## エラーハンドリング

### バリデーション
- 負荷状況更新時の必須項目チェック
- 困りごと投稿時の内容長制限（1000文字以内）
- 不正なユーザーIDでのアクセス制御

### エラーレスポンス
```json
{
  "error": "VALIDATION_ERROR",
  "message": "入力内容に不備があります",
  "details": [
    {
      "field": "workloadLevel",
      "message": "負荷レベルは必須です"
    }
  ]
}
```

### 例外処理
- データベース接続エラー時の適切なメッセージ表示
- 認証エラー時のログイン画面リダイレクト
- 権限不足時の403エラー返却

## テスト戦略

### 単体テスト
- **エンティティテスト**: バリデーション、関係性の確認
- **リポジトリテスト**: CRUD操作、カスタムクエリの動作確認
- **サービステスト**: ビジネスロジック、例外処理の確認
- **コントローラテスト**: APIエンドポイント、レスポンス形式の確認

### 統合テスト
- **API統合テスト**: フロントエンドからバックエンドまでの一連の流れ
- **データベーステスト**: トランザクション、データ整合性の確認
- **認証テスト**: セキュリティ機能の動作確認

### フロントエンドテスト
- **UI操作テスト**: フォーム入力、画面遷移の確認
- **データ表示テスト**: API連携、データ表示の確認
- **レスポンシブテスト**: 各デバイスでの表示確認

### テストデータ
- 複数ユーザーでの負荷状況パターン
- 様々な困りごととコメントのパターン
- 日時による状況変化のパターン

## AWSデプロイメント戦略

### 推奨アーキテクチャ

#### オプション1: シンプル構成（小規模チーム向け）
- **EC2**: t3.micro インスタンスでSpring Bootアプリケーション実行
- **RDS**: PostgreSQL（db.t3.micro）
- **S3**: 静的ファイル（CSS/JS）配信
- **CloudFront**: CDN配信（オプション）
- **Route 53**: ドメイン管理

#### オプション2: コンテナ構成（スケーラブル）
- **ECS Fargate**: Spring Bootアプリケーションのコンテナ実行
- **Application Load Balancer**: 負荷分散
- **RDS**: PostgreSQL（Multi-AZ構成）
- **S3 + CloudFront**: 静的コンテンツ配信
- **ECR**: Dockerイメージ管理

#### オプション3: サーバーレス構成（コスト最適化）
- **Lambda**: Spring Boot Native（GraalVM）
- **API Gateway**: REST APIエンドポイント
- **RDS Proxy**: データベース接続プール
- **S3 + CloudFront**: フロントエンド配信

#### オプション4: AWS Amplify構成（フルマネージド）
- **Amplify Hosting**: フロントエンド（HTML/CSS/JS）の自動デプロイ
- **Lambda**: Spring Boot APIをサーバーレス化
- **API Gateway**: REST APIエンドポイント
- **RDS**: PostgreSQL（または DynamoDB）
- **Cognito**: ユーザー認証（既存認証から移行）

### デプロイメント設定

#### 環境変数設定
```yaml
# application-aws.yml
spring:
  datasource:
    url: ${RDS_ENDPOINT}
    username: ${RDS_USERNAME}
    password: ${RDS_PASSWORD}
  
aws:
  region: ${AWS_REGION:ap-northeast-1}
  
logging:
  level:
    com.teamdashboard: INFO
```

#### Docker設定
```dockerfile
# マルチステージビルド
FROM openjdk:17-jdk-slim as builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM openjdk:17-jre-slim
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Infrastructure as Code（Terraform推奨）
```hcl
# main.tf
resource "aws_ecs_cluster" "team_dashboard" {
  name = "team-dashboard"
}

resource "aws_db_instance" "postgres" {
  identifier = "team-dashboard-db"
  engine     = "postgres"
  engine_version = "15.4"
  instance_class = "db.t3.micro"
  allocated_storage = 20
  
  db_name  = "teamdashboard"
  username = var.db_username
  password = var.db_password
  
  skip_final_snapshot = true
}
```

### セキュリティ考慮事項

#### ネットワークセキュリティ
- **VPC**: プライベートサブネットでRDS配置
- **Security Groups**: 必要最小限のポート開放
- **WAF**: Web Application Firewall設定

#### データ保護
- **RDS暗号化**: 保存時暗号化有効
- **SSL/TLS**: HTTPS通信強制
- **IAM Roles**: 最小権限の原則

#### 監視・ログ
- **CloudWatch**: アプリケーションログ、メトリクス監視
- **X-Ray**: 分散トレーシング（オプション）
- **CloudTrail**: API呼び出し監査

### コスト最適化

#### 推定月額コスト（東京リージョン）
- **シンプル構成**: 約$15-25/月
  - EC2 t3.micro: $8.5
  - RDS db.t3.micro: $12.5
  - その他（S3、データ転送）: $2-4

- **コンテナ構成**: 約$30-50/月
  - ECS Fargate: $15-25
  - ALB: $16
  - RDS: $12.5
  - その他: $5-10

#### コスト削減策
- **Reserved Instances**: 1年契約で30-40%削減
- **Spot Instances**: 開発環境で70%削減
- **Auto Scaling**: 負荷に応じた自動スケーリング
- **CloudWatch**: 不要リソースの監視・削除

### デプロイメントパイプライン

#### CI/CD推奨構成
1. **GitHub Actions** または **AWS CodePipeline**
2. **CodeBuild**: ビルド・テスト実行
3. **ECR**: Dockerイメージ保存
4. **ECS**: 本番デプロイ

#### デプロイメント戦略
- **Blue-Green Deployment**: ダウンタイムゼロ
- **Rolling Update**: 段階的更新
- **Canary Release**: 一部トラフィックでテスト

### AWS Amplifyでのデプロイメント詳細

#### Amplify構成の利点
- **フロントエンド**: Git連携による自動デプロイ
- **バックエンド**: Amplify CLIでのインフラ管理
- **認証**: Cognito統合による簡単なユーザー管理
- **API**: GraphQLまたはREST APIの自動生成
- **ホスティング**: CDN、SSL証明書が自動設定

#### 現在のアプリケーションのAmplify対応

**フロントエンド対応**
```yaml
# amplify.yml
version: 1
frontend:
  phases:
    build:
      commands:
        - echo "フロントエンドビルド（現在は静的ファイルのため不要）"
  artifacts:
    baseDirectory: frontend
    files:
      - '**/*'
  cache:
    paths: []
```

**バックエンドAPI対応**
現在のSpring BootアプリケーションをAmplifyで使用する場合：

1. **Lambda化**: Spring Boot Native（GraalVM）でコールドスタート最適化
2. **API Gateway統合**: 既存のREST APIエンドポイントをそのまま利用
3. **環境変数**: Amplify環境設定でデータベース接続情報管理

**認証システム移行**
```javascript
// 既存のSpring Security → Cognito移行
import { Auth } from 'aws-amplify';

// ログイン
const signIn = async (username, password) => {
  try {
    const user = await Auth.signIn(username, password);
    return user;
  } catch (error) {
    console.error('ログインエラー:', error);
  }
};
```

#### Amplify CLI設定例
```bash
# Amplify初期化
amplify init

# API追加（既存Spring Boot APIをLambdaとして）
amplify add api

# 認証追加
amplify add auth

# ホスティング追加
amplify add hosting

# デプロイ
amplify push
```

#### データベース選択肢

**PostgreSQL継続の場合**
- RDS PostgreSQLをAmplifyから接続
- VPC設定でLambdaからRDSアクセス
- 接続プールとしてRDS Proxyを使用

**DynamoDB移行の場合**
- NoSQLへのデータモデル変更が必要
- Amplify DataStoreでオフライン同期対応
- GraphQL APIの自動生成

#### コスト比較（Amplify vs 他オプション）

**Amplify構成の月額コスト**
- Amplify Hosting: $1-5（トラフィック依存）
- Lambda: $5-15（実行時間依存）
- RDS: $12.5（t3.micro）
- API Gateway: $3-10（リクエスト数依存）
- **合計**: 約$20-45/月

#### Amplify採用時の制約事項
- **Lambda制限**: 15分実行時間制限、メモリ上限
- **コールドスタート**: 初回リクエストの遅延
- **デバッグ**: ローカル開発環境の複雑化
- **ベンダーロックイン**: AWS特化の構成

#### 推奨移行戦略
1. **フェーズ1**: フロントエンドのみAmplify Hostingに移行
2. **フェーズ2**: 認証をCognitoに移行
3. **フェーズ3**: バックエンドAPIをLambda化
4. **フェーズ4**: 必要に応じてDynamoDBに移行

## React フロントエンド移行戦略

### React移行の利点

#### 技術的メリット
- **コンポーネント化**: 再利用可能なUIコンポーネント
- **状態管理**: React Context/Redux による効率的な状態管理
- **TypeScript対応**: 型安全性の向上
- **豊富なエコシステム**: ライブラリ・ツールの充実
- **テスト**: Jest/React Testing Libraryでの単体テスト

#### 開発効率の向上
- **Hot Reload**: 開発時の即座な反映
- **デバッグツール**: React DevToolsでの状態確認
- **モダンな開発体験**: ES6+、JSX、モジュールシステム

### React アプリケーション設計

#### プロジェクト構成
```
frontend-react/
├── public/
├── src/
│   ├── components/          # 再利用可能コンポーネント
│   │   ├── common/         # 共通コンポーネント
│   │   ├── workload/       # 負荷状況関連
│   │   └── issues/         # 困りごと関連
│   ├── pages/              # ページコンポーネント
│   │   ├── Dashboard.jsx
│   │   ├── Reports.jsx
│   │   └── Calendar.jsx
│   ├── hooks/              # カスタムフック
│   ├── services/           # API通信
│   ├── context/            # React Context
│   ├── utils/              # ユーティリティ
│   └── styles/             # CSS/Styled Components
├── package.json
└── vite.config.js          # Vite設定
```

#### 主要コンポーネント設計

**WorkloadStatusCard コンポーネント**
```jsx
import React from 'react';
import { WorkloadLevel } from '../types';

interface WorkloadStatusCardProps {
  user: User;
  workloadStatus: WorkloadStatus;
  onUpdate: () => void;
}

const WorkloadStatusCard: React.FC<WorkloadStatusCardProps> = ({
  user,
  workloadStatus,
  onUpdate
}) => {
  const getStatusColor = (level: WorkloadLevel) => {
    switch (level) {
      case 'HIGH': return 'bg-red-500';
      case 'MEDIUM': return 'bg-yellow-500';
      case 'LOW': return 'bg-green-500';
      default: return 'bg-gray-500';
    }
  };

  return (
    <div className="workload-card">
      <div className={`status-indicator ${getStatusColor(workloadStatus.level)}`} />
      <h3>{user.displayName}</h3>
      <p>負荷レベル: {workloadStatus.level}</p>
      <p>更新: {formatDate(workloadStatus.updatedAt)}</p>
      {workloadStatus.projectCount && (
        <p>案件数: {workloadStatus.projectCount}</p>
      )}
    </div>
  );
};
```

**TeamIssueList コンポーネント**
```jsx
import React, { useState, useEffect } from 'react';
import { useTeamIssues } from '../hooks/useTeamIssues';

const TeamIssueList: React.FC = () => {
  const { issues, loading, addComment, resolveIssue } = useTeamIssues();

  if (loading) return <div>読み込み中...</div>;

  return (
    <div className="issues-list">
      {issues.map(issue => (
        <IssueCard
          key={issue.id}
          issue={issue}
          onAddComment={addComment}
          onResolve={resolveIssue}
        />
      ))}
    </div>
  );
};
```

#### 状態管理戦略

**React Context による状態管理**
```jsx
// AuthContext.jsx
const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const login = async (credentials) => {
    // ログイン処理
  };

  const logout = () => {
    // ログアウト処理
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
};

// WorkloadContext.jsx
const WorkloadContext = createContext();

export const WorkloadProvider = ({ children }) => {
  const [workloadStatuses, setWorkloadStatuses] = useState([]);
  const [teamIssues, setTeamIssues] = useState([]);

  // 状態更新ロジック

  return (
    <WorkloadContext.Provider value={{ workloadStatuses, teamIssues, ... }}>
      {children}
    </WorkloadContext.Provider>
  );
};
```

#### API通信サービス

**APIクライアント**
```jsx
// services/api.js
import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// リクエストインターセプター（認証トークン付与）
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('authToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const workloadAPI = {
  getAll: () => apiClient.get('/workload-status'),
  getMy: () => apiClient.get('/workload-status/my'),
  update: (data) => apiClient.post('/workload-status', data),
};

export const issuesAPI = {
  getAll: () => apiClient.get('/team-issues'),
  create: (data) => apiClient.post('/team-issues', data),
  resolve: (id) => apiClient.put(`/team-issues/${id}/resolve`),
  addComment: (id, comment) => apiClient.post(`/team-issues/${id}/comments`, comment),
};
```

### 技術スタック

#### 推奨ライブラリ
```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.8.0",
    "axios": "^1.3.0",
    "date-fns": "^2.29.0",
    "react-hook-form": "^7.43.0",
    "react-query": "^3.39.0"
  },
  "devDependencies": {
    "@vitejs/plugin-react": "^3.1.0",
    "vite": "^4.1.0",
    "tailwindcss": "^3.2.0",
    "typescript": "^4.9.0",
    "@types/react": "^18.0.0",
    "jest": "^29.0.0",
    "@testing-library/react": "^14.0.0"
  }
}
```

#### スタイリング戦略
- **Tailwind CSS**: ユーティリティファーストCSS
- **CSS Modules**: コンポーネント固有スタイル
- **Styled Components**: CSS-in-JS（オプション）

### 移行戦略

#### 段階的移行アプローチ

**フェーズ1: 環境構築**
- React開発環境セットアップ
- 既存APIとの接続確認
- 基本的なルーティング実装

**フェーズ2: 既存機能の移植**
- ダッシュボード画面のReact化
- 日報投稿機能の移植
- カレンダー機能の移植

**フェーズ3: 新機能実装**
- 負荷状況管理機能
- 困りごと共有機能
- リアルタイム更新機能

**フェーズ4: 最適化・テスト**
- パフォーマンス最適化
- 単体テスト・統合テスト
- アクセシビリティ対応

#### 並行開発戦略
```
現在のHTML/JS版 → 本番運用継続
     ↓
React版開発 → 開発・テスト環境
     ↓
段階的切り替え → 機能単位でReact版に移行
```

### AWS Amplify + React構成

#### Amplify React統合
```bash
# Create React App with Amplify
npx create-react-app team-dashboard --template typescript
cd team-dashboard
npm install aws-amplify @aws-amplify/ui-react

# Amplify初期化
amplify init
amplify add auth
amplify add api
amplify push
```

#### Amplify設定
```jsx
// src/index.js
import { Amplify } from 'aws-amplify';
import awsExports from './aws-exports';

Amplify.configure(awsExports);
```

#### 認証統合
```jsx
import { withAuthenticator } from '@aws-amplify/ui-react';

function App() {
  return (
    <div className="App">
      <Dashboard />
    </div>
  );
}

export default withAuthenticator(App);
```

### 開発・運用メリット

#### 開発効率
- **コンポーネント再利用**: 開発速度向上
- **TypeScript**: バグ削減、保守性向上
- **Hot Reload**: 開発体験向上
- **豊富なツール**: デバッグ・テストツール

#### 運用・保守
- **モジュール化**: 機能追加・修正の容易さ
- **テスト**: 自動テストによる品質保証
- **パフォーマンス**: 仮想DOM、コード分割
- **SEO**: Next.jsでのSSR対応（将来的）

#### チーム開発
- **コンポーネント分担**: 並行開発可能
- **スタイルガイド**: Storybookでのコンポーネント管理
- **コードレビュー**: 構造化されたコード

## 最終推奨デプロイ構成

### 推奨アーキテクチャ: React + AWS Amplify + Spring Boot

#### 最終構成概要
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   React SPA     │    │   Spring Boot    │    │   PostgreSQL    │
│  (Amplify Host) │────│     (Lambda)     │────│      (RDS)      │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                        │                        │
         │              ┌──────────────────┐              │
         └──────────────│   API Gateway    │──────────────┘
                        └──────────────────┘
                                 │
                        ┌──────────────────┐
                        │     Cognito      │
                        │   (認証・認可)    │
                        └──────────────────┘
```

#### 詳細構成

**フロントエンド: React + AWS Amplify**
- **Amplify Hosting**: React SPAの配信
- **CloudFront CDN**: 世界規模での高速配信
- **Route 53**: カスタムドメイン設定
- **SSL証明書**: 自動取得・更新

**バックエンド: Spring Boot + Lambda**
- **AWS Lambda**: Spring Boot Native（GraalVM）
- **API Gateway**: REST APIエンドポイント
- **Lambda Layers**: 共通ライブラリの最適化

**データベース: PostgreSQL**
- **Amazon RDS**: PostgreSQL 15.x
- **Multi-AZ**: 高可用性構成
- **RDS Proxy**: 接続プール管理
- **自動バックアップ**: 7日間保持

**認証: Amazon Cognito**
- **User Pool**: ユーザー管理
- **Identity Pool**: AWS リソースアクセス制御
- **MFA**: 多要素認証（オプション）

**監視・ログ**
- **CloudWatch**: アプリケーションログ・メトリクス
- **X-Ray**: 分散トレーシング
- **CloudTrail**: API監査ログ

### デプロイメント設定

#### Infrastructure as Code (Terraform)
```hcl
# main.tf
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# Cognito User Pool
resource "aws_cognito_user_pool" "team_dashboard" {
  name = "team-dashboard-users"
  
  password_policy {
    minimum_length    = 8
    require_lowercase = true
    require_numbers   = true
    require_symbols   = true
    require_uppercase = true
  }
}

# RDS PostgreSQL
resource "aws_db_instance" "postgres" {
  identifier = "team-dashboard-db"
  
  engine         = "postgres"
  engine_version = "15.4"
  instance_class = "db.t3.micro"
  
  allocated_storage     = 20
  max_allocated_storage = 100
  storage_encrypted     = true
  
  db_name  = "teamdashboard"
  username = var.db_username
  password = var.db_password
  
  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "sun:04:00-sun:05:00"
  
  multi_az               = true
  publicly_accessible    = false
  
  skip_final_snapshot = false
  final_snapshot_identifier = "team-dashboard-final-snapshot"
  
  tags = {
    Name = "team-dashboard-db"
  }
}

# Lambda Function
resource "aws_lambda_function" "api" {
  filename         = "target/team-dashboard-lambda.jar"
  function_name    = "team-dashboard-api"
  role            = aws_iam_role.lambda_role.arn
  handler         = "com.teamdashboard.LambdaHandler"
  runtime         = "java17"
  timeout         = 30
  memory_size     = 512
  
  environment {
    variables = {
      DB_HOST     = aws_db_instance.postgres.endpoint
      DB_NAME     = aws_db_instance.postgres.db_name
      DB_USERNAME = var.db_username
      DB_PASSWORD = var.db_password
    }
  }
}

# API Gateway
resource "aws_api_gateway_rest_api" "api" {
  name = "team-dashboard-api"
  
  endpoint_configuration {
    types = ["REGIONAL"]
  }
}
```

#### Amplify設定
```yaml
# amplify.yml
version: 1
applications:
  - frontend:
      phases:
        preBuild:
          commands:
            - npm ci
        build:
          commands:
            - npm run build
      artifacts:
        baseDirectory: build
        files:
          - '**/*'
      cache:
        paths:
          - node_modules/**/*
    appRoot: frontend-react
```

#### CI/CD パイプライン
```yaml
# .github/workflows/deploy.yml
name: Deploy to AWS

on:
  push:
    branches: [main]

jobs:
  deploy-backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'corretto'
      
      - name: Build with Maven
        run: |
          cd backend
          ./mvnw clean package -Pnative
      
      - name: Deploy to Lambda
        run: |
          aws lambda update-function-code \
            --function-name team-dashboard-api \
            --zip-file fileb://target/team-dashboard-lambda.jar

  deploy-frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      
      - name: Install and Build
        run: |
          cd frontend-react
          npm ci
          npm run build
      
      - name: Deploy to Amplify
        run: |
          # Amplify自動デプロイ（Git連携）
          echo "Amplify will auto-deploy from Git"
```

### 運用・保守

#### 監視設定
```json
{
  "CloudWatchAlarms": {
    "HighErrorRate": {
      "MetricName": "Errors",
      "Threshold": 10,
      "ComparisonOperator": "GreaterThanThreshold"
    },
    "HighLatency": {
      "MetricName": "Duration",
      "Threshold": 5000,
      "ComparisonOperator": "GreaterThanThreshold"
    },
    "DatabaseConnections": {
      "MetricName": "DatabaseConnections",
      "Threshold": 80,
      "ComparisonOperator": "GreaterThanThreshold"
    }
  }
}
```

#### バックアップ戦略
- **RDS自動バックアップ**: 7日間保持
- **スナップショット**: 週次手動スナップショット
- **コードバックアップ**: GitHubリポジトリ
- **設定バックアップ**: Terraformステートファイル

### コスト最適化

#### 月額コスト見積もり（東京リージョン）
```
┌─────────────────────┬──────────┬─────────────┐
│ サービス            │ 構成     │ 月額コスト  │
├─────────────────────┼──────────┼─────────────┤
│ Amplify Hosting     │ 標準     │ $1-5        │
│ Lambda              │ 512MB    │ $5-15       │
│ API Gateway         │ REST API │ $3-10       │
│ RDS PostgreSQL      │ t3.micro │ $12.5       │
│ Cognito             │ 標準     │ $0-5        │
│ CloudWatch          │ 基本     │ $2-5        │
│ Route 53            │ ドメイン │ $0.5        │
├─────────────────────┼──────────┼─────────────┤
│ 合計                │          │ $24-53      │
└─────────────────────┴──────────┴─────────────┘
```

#### コスト削減策
- **Reserved Instances**: RDSで30%削減
- **Lambda Provisioned Concurrency**: 高頻度アクセス時のみ
- **CloudWatch Logs**: 保持期間最適化
- **S3 Intelligent Tiering**: 静的ファイルのコスト最適化

### セキュリティ

#### セキュリティベストプラクティス
- **WAF**: Web Application Firewall設定
- **VPC**: プライベートサブネットでRDS配置
- **IAM**: 最小権限の原則
- **暗号化**: 保存時・転送時暗号化
- **MFA**: 管理者アカウントの多要素認証

#### コンプライアンス
- **GDPR**: 個人データ保護対応
- **SOC 2**: AWS準拠サービス利用
- **監査ログ**: CloudTrailでの完全な監査証跡

### スケーラビリティ

#### 自動スケーリング
- **Lambda**: 自動スケーリング（同時実行数制限設定）
- **RDS**: 必要に応じてRead Replicaで読み取り分散
- **CloudFront**: 世界規模でのCDN配信

#### 将来の拡張性
- **マイクロサービス**: 機能別Lambda分割
- **GraphQL**: Apollo Federationでの統合API
- **リアルタイム**: WebSocket/Server-Sent Events対応