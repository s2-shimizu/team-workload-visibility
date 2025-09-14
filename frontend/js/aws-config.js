// AWS Amplify設定
// import { Amplify } from 'aws-amplify'; // CDN版を使用するためコメントアウト

const awsConfig = {
    Auth: {
        region: 'ap-northeast-1',
        userPoolId: 'ap-northeast-1_S0zRV4ais',
        userPoolWebClientId: '7nue9hv9e54sdrcvorl990q1t6',
        identityPoolId: 'ap-northeast-1:633a8ab9-3889-4894-adef-31671acb45e8'
    },
    API: {
        endpoints: [
            {
                name: "teamDashboardApi",
                endpoint: "https://bn6xwu62qd.execute-api.ap-northeast-1.amazonaws.com/dev",
                region: 'ap-northeast-1'
            }
        ]
    },
    Storage: {
        AWSS3: {
            bucket: 'team-dashboard-storage', // 必要に応じて
            region: 'ap-northeast-1'
        }
    }
};

// 開発環境用の設定
if (window.location.hostname === 'localhost') {
    awsConfig.API.endpoints[0].endpoint = 'http://localhost:8081';
}

// CDN版のAmplifyを使用
if (typeof window.aws_amplify_core !== 'undefined') {
    window.aws_amplify_core.Amplify.configure(awsConfig);
} else if (typeof Amplify !== 'undefined') {
    Amplify.configure(awsConfig);
}
// export default awsConfig; // 通常のスクリプトとして読み込むためコメントアウト
window.awsConfig = awsConfig;