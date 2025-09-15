# 本番用ドメイン・SSL設定スクリプト
param(
    [Parameter(Mandatory=$true)]
    [string]$DomainName,
    [string]$Environment = "prod",
    [string]$Region = "ap-northeast-1",
    [string]$CertificateRegion = "us-east-1", # CloudFront用証明書はus-east-1が必要
    [switch]$CreateHostedZone = $false
)

Write-Host "=== 本番用ドメイン・SSL設定 ===" -ForegroundColor Green
Write-Host "Domain: $DomainName" -ForegroundColor Yellow
Write-Host "Environment: $Environment" -ForegroundColor Yellow
Write-Host "Region: $Region" -ForegroundColor Yellow
Write-Host "Certificate Region: $CertificateRegion" -ForegroundColor Yellow
Write-Host ""

# 前提条件チェック
Write-Host "1. 前提条件チェック" -ForegroundColor Cyan

try {
    $identity = aws sts get-caller-identity --output json | ConvertFrom-Json
    Write-Host "✅ AWS認証: $($identity.Arn)" -ForegroundColor Green
} catch {
    Write-Host "❌ AWS認証が設定されていません。" -ForegroundColor Red
    exit 1
}

Write-Host ""

# 2. Route 53 Hosted Zone作成（オプション）
if ($CreateHostedZone) {
    Write-Host "2. Route 53 Hosted Zone作成" -ForegroundColor Cyan
    
    try {
        Write-Host "Hosted Zoneを作成中..." -ForegroundColor Gray
        $hostedZone = aws route53 create-hosted-zone --name $DomainName --caller-reference "$(Get-Date -Format 'yyyyMMddHHmmss')" --output json | ConvertFrom-Json
        
        Write-Host "✅ Hosted Zone作成完了" -ForegroundColor Green
        Write-Host "   Zone ID: $($hostedZone.HostedZone.Id)" -ForegroundColor Gray
        Write-Host "   Name Servers:" -ForegroundColor Gray
        foreach ($ns in $hostedZone.DelegationSet.NameServers) {
            Write-Host "     - $ns" -ForegroundColor Gray
        }
        
        Write-Host ""
        Write-Host "⚠️ 重要: ドメインレジストラでネームサーバーを上記に変更してください" -ForegroundColor Yellow
        
    } catch {
        Write-Host "⚠️ Hosted Zone作成エラー（既存の可能性）: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    
    Write-Host ""
} else {
    Write-Host "2. Route 53 Hosted Zone確認" -ForegroundColor Cyan
    
    try {
        $hostedZones = aws route53 list-hosted-zones-by-name --dns-name $DomainName --output json | ConvertFrom-Json
        $matchingZone = $hostedZones.HostedZones | Where-Object { $_.Name -eq "$DomainName." }
        
        if ($matchingZone) {
            Write-Host "✅ Hosted Zone確認: $($matchingZone.Id)" -ForegroundColor Green
            $script:HostedZoneId = $matchingZone.Id -replace '/hostedzone/', ''
        } else {
            Write-Host "❌ Hosted Zoneが見つかりません。-CreateHostedZone オプションを使用してください。" -ForegroundColor Red
            exit 1
        }
    } catch {
        Write-Host "❌ Hosted Zone確認エラー: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
    
    Write-Host ""
}

# 3. SSL証明書作成
Write-Host "3. SSL証明書作成" -ForegroundColor Cyan

try {
    Write-Host "SSL証明書をリクエスト中..." -ForegroundColor Gray
    
    # 証明書リクエスト
    $certRequest = aws acm request-certificate `
        --domain-name $DomainName `
        --subject-alternative-names "www.$DomainName" `
        --validation-method DNS `
        --region $CertificateRegion `
        --output json | ConvertFrom-Json
    
    $certificateArn = $certRequest.CertificateArn
    Write-Host "✅ 証明書リクエスト完了" -ForegroundColor Green
    Write-Host "   Certificate ARN: $certificateArn" -ForegroundColor Gray
    
    # DNS検証レコード取得を待機
    Write-Host "DNS検証レコード情報を取得中..." -ForegroundColor Gray
    Start-Sleep -Seconds 10
    
    $certDetails = aws acm describe-certificate --certificate-arn $certificateArn --region $CertificateRegion --output json | ConvertFrom-Json
    
    if ($certDetails.Certificate.DomainValidationOptions) {
        Write-Host "✅ DNS検証レコード取得完了" -ForegroundColor Green
        
        foreach ($validation in $certDetails.Certificate.DomainValidationOptions) {
            if ($validation.ResourceRecord) {
                Write-Host "   ドメイン: $($validation.DomainName)" -ForegroundColor Gray
                Write-Host "   レコード名: $($validation.ResourceRecord.Name)" -ForegroundColor Gray
                Write-Host "   レコード値: $($validation.ResourceRecord.Value)" -ForegroundColor Gray
                Write-Host "   レコードタイプ: $($validation.ResourceRecord.Type)" -ForegroundColor Gray
                
                # Route 53に自動でDNS検証レコードを追加
                if ($script:HostedZoneId) {
                    Write-Host "   DNS検証レコードを自動追加中..." -ForegroundColor Gray
                    
                    $changeSet = @{
                        Changes = @(
                            @{
                                Action = "CREATE"
                                ResourceRecordSet = @{
                                    Name = $validation.ResourceRecord.Name
                                    Type = $validation.ResourceRecord.Type
                                    TTL = 300
                                    ResourceRecords = @(
                                        @{
                                            Value = "`"$($validation.ResourceRecord.Value)`""
                                        }
                                    )
                                }
                            }
                        )
                    } | ConvertTo-Json -Depth 10
                    
                    $changeSet | Out-File -FilePath "dns-validation.json" -Encoding UTF8
                    
                    try {
                        aws route53 change-resource-record-sets --hosted-zone-id $script:HostedZoneId --change-batch file://dns-validation.json
                        Write-Host "   ✅ DNS検証レコード追加完了" -ForegroundColor Green
                    } catch {
                        Write-Host "   ⚠️ DNS検証レコード追加エラー: $($_.Exception.Message)" -ForegroundColor Yellow
                    } finally {
                        Remove-Item -Path "dns-validation.json" -Force -ErrorAction SilentlyContinue
                    }
                }
                
                Write-Host ""
            }
        }
    }
    
} catch {
    Write-Host "❌ SSL証明書作成エラー: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# 4. CloudFront Distribution作成
Write-Host "4. CloudFront Distribution作成" -ForegroundColor Cyan

# ECSロードバランサーのDNS名を取得（既存デプロイから）
$stackName = "team-dashboard-$Environment-ecs"
try {
    $stackInfo = aws cloudformation describe-stacks --stack-name $stackName --region $Region --output json | ConvertFrom-Json
    $loadBalancerUrl = ($stackInfo.Stacks[0].Outputs | Where-Object { $_.OutputKey -eq "LoadBalancerURL" }).OutputValue
    
    if ($loadBalancerUrl) {
        $originDomain = $loadBalancerUrl -replace 'https?://', ''
        Write-Host "✅ オリジンドメイン取得: $originDomain" -ForegroundColor Green
    } else {
        Write-Host "⚠️ ロードバランサーURLが見つかりません。先にECSデプロイを実行してください。" -ForegroundColor Yellow
        $originDomain = Read-Host "オリジンドメイン名を入力してください（例: alb-123456789.ap-northeast-1.elb.amazonaws.com）"
    }
} catch {
    Write-Host "⚠️ ECSスタック情報取得エラー。手動でオリジンドメインを入力してください。" -ForegroundColor Yellow
    $originDomain = Read-Host "オリジンドメイン名を入力してください"
}

# CloudFront Distribution設定
$distributionConfig = @{
    CallerReference = "team-dashboard-$Environment-$(Get-Date -Format 'yyyyMMddHHmmss')"
    Comment = "Team Dashboard $Environment Distribution"
    DefaultRootObject = "index.html"
    Enabled = $true
    PriceClass = "PriceClass_100"
    
    Origins = @{
        Quantity = 1
        Items = @(
            @{
                Id = "ECS-Origin"
                DomainName = $originDomain
                CustomOriginConfig = @{
                    HTTPPort = 80
                    HTTPSPort = 443
                    OriginProtocolPolicy = "https-only"
                    OriginSslProtocols = @{
                        Quantity = 1
                        Items = @("TLSv1.2")
                    }
                }
            }
        )
    }
    
    DefaultCacheBehavior = @{
        TargetOriginId = "ECS-Origin"
        ViewerProtocolPolicy = "redirect-to-https"
        MinTTL = 0
        DefaultTTL = 86400
        MaxTTL = 31536000
        Compress = $true
        
        ForwardedValues = @{
            QueryString = $true
            Cookies = @{
                Forward = "all"
            }
            Headers = @{
                Quantity = 3
                Items = @("Authorization", "Origin", "Referer")
            }
        }
        
        TrustedSigners = @{
            Enabled = $false
            Quantity = 0
        }
    }
    
    Aliases = @{
        Quantity = 2
        Items = @($DomainName, "www.$DomainName")
    }
    
    ViewerCertificate = @{
        ACMCertificateArn = $certificateArn
        SSLSupportMethod = "sni-only"
        MinimumProtocolVersion = "TLSv1.2_2021"
    }
} | ConvertTo-Json -Depth 10

Write-Host "CloudFront Distributionを作成中..." -ForegroundColor Gray
$distributionConfig | Out-File -FilePath "cloudfront-config.json" -Encoding UTF8

try {
    $distribution = aws cloudfront create-distribution --distribution-config file://cloudfront-config.json --output json | ConvertFrom-Json
    
    Write-Host "✅ CloudFront Distribution作成完了" -ForegroundColor Green
    Write-Host "   Distribution ID: $($distribution.Distribution.Id)" -ForegroundColor Gray
    Write-Host "   Domain Name: $($distribution.Distribution.DomainName)" -ForegroundColor Gray
    Write-Host "   Status: $($distribution.Distribution.Status)" -ForegroundColor Gray
    
    $script:DistributionDomain = $distribution.Distribution.DomainName
    
} catch {
    Write-Host "❌ CloudFront Distribution作成エラー: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
} finally {
    Remove-Item -Path "cloudfront-config.json" -Force -ErrorAction SilentlyContinue
}

Write-Host ""

# 5. Route 53 DNSレコード作成
Write-Host "5. Route 53 DNSレコード作成" -ForegroundColor Cyan

if ($script:HostedZoneId -and $script:DistributionDomain) {
    # Aレコード（Apex domain）
    $apexRecord = @{
        Changes = @(
            @{
                Action = "CREATE"
                ResourceRecordSet = @{
                    Name = $DomainName
                    Type = "A"
                    AliasTarget = @{
                        DNSName = $script:DistributionDomain
                        EvaluateTargetHealth = $false
                        HostedZoneId = "Z2FDTNDATAQYW2" # CloudFrontのHosted Zone ID
                    }
                }
            }
        )
    } | ConvertTo-Json -Depth 10
    
    # CNAMEレコード（www）
    $wwwRecord = @{
        Changes = @(
            @{
                Action = "CREATE"
                ResourceRecordSet = @{
                    Name = "www.$DomainName"
                    Type = "CNAME"
                    TTL = 300
                    ResourceRecords = @(
                        @{
                            Value = $script:DistributionDomain
                        }
                    )
                }
            }
        )
    } | ConvertTo-Json -Depth 10
    
    try {
        # Apexドメインレコード作成
        $apexRecord | Out-File -FilePath "apex-record.json" -Encoding UTF8
        aws route53 change-resource-record-sets --hosted-zone-id $script:HostedZoneId --change-batch file://apex-record.json
        Write-Host "✅ Apexドメインレコード作成完了" -ForegroundColor Green
        
        # wwwレコード作成
        $wwwRecord | Out-File -FilePath "www-record.json" -Encoding UTF8
        aws route53 change-resource-record-sets --hosted-zone-id $script:HostedZoneId --change-batch file://www-record.json
        Write-Host "✅ wwwレコード作成完了" -ForegroundColor Green
        
    } catch {
        Write-Host "⚠️ DNSレコード作成エラー: $($_.Exception.Message)" -ForegroundColor Yellow
    } finally {
        Remove-Item -Path "apex-record.json" -Force -ErrorAction SilentlyContinue
        Remove-Item -Path "www-record.json" -Force -ErrorAction SilentlyContinue
    }
}

Write-Host ""
Write-Host "=== 本番用ドメイン・SSL設定完了 ===" -ForegroundColor Green

Write-Host ""
Write-Host "設定されたリソース:" -ForegroundColor Cyan
Write-Host "  ドメイン: $DomainName" -ForegroundColor Yellow
Write-Host "  SSL証明書: $certificateArn" -ForegroundColor Yellow
if ($script:DistributionDomain) {
    Write-Host "  CloudFront: $($script:DistributionDomain)" -ForegroundColor Yellow
}
if ($script:HostedZoneId) {
    Write-Host "  Hosted Zone: $($script:HostedZoneId)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "次のステップ:" -ForegroundColor Cyan
Write-Host "1. SSL証明書の検証完了を待つ（数分〜数時間）" -ForegroundColor Gray
Write-Host "2. CloudFront Distributionのデプロイ完了を待つ（15-20分）" -ForegroundColor Gray
Write-Host "3. DNS伝播を待つ（最大48時間）" -ForegroundColor Gray
Write-Host ""
Write-Host "確認方法:" -ForegroundColor Cyan
Write-Host "  SSL証明書: aws acm describe-certificate --certificate-arn $certificateArn --region $CertificateRegion" -ForegroundColor Gray
Write-Host "  CloudFront: aws cloudfront get-distribution --id <distribution-id>" -ForegroundColor Gray
Write-Host "  DNS確認: nslookup $DomainName" -ForegroundColor Gray
Write-Host ""
Write-Host "アクセステスト:" -ForegroundColor Cyan
Write-Host "  https://$DomainName" -ForegroundColor Gray
Write-Host "  https://www.$DomainName" -ForegroundColor Gray

Write-Host ""
Write-Host "使用方法:" -ForegroundColor Yellow
Write-Host "  基本設定: .\setup-production-domain.ps1 -DomainName 'yourdomain.com'" -ForegroundColor Gray
Write-Host "  新規ドメイン: .\setup-production-domain.ps1 -DomainName 'yourdomain.com' -CreateHostedZone" -ForegroundColor Gray