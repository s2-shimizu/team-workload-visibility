# Simple DynamoDB Table Check
param(
    [string]$Region = "ap-northeast-1"
)

Write-Host "=== DynamoDB Table Check ===" -ForegroundColor Green
Write-Host "Region: $Region" -ForegroundColor Yellow
Write-Host ""

# Check AWS CLI
try {
    $awsVersion = aws --version
    Write-Host "AWS CLI: $awsVersion" -ForegroundColor Green
} catch {
    Write-Host "AWS CLI not found" -ForegroundColor Red
    exit 1
}

# Check AWS credentials
try {
    $identity = aws sts get-caller-identity --output json | ConvertFrom-Json
    Write-Host "AWS Account: $($identity.Account)" -ForegroundColor Green
} catch {
    Write-Host "AWS credentials not configured" -ForegroundColor Red
    exit 1
}

Write-Host ""

# List all DynamoDB tables
Write-Host "DynamoDB Tables:" -ForegroundColor Cyan
try {
    $tables = aws dynamodb list-tables --region $Region --output json | ConvertFrom-Json
    Write-Host "Total tables: $($tables.TableNames.Count)" -ForegroundColor Gray
    
    foreach ($tableName in $tables.TableNames) {
        Write-Host "  - $tableName" -ForegroundColor Gray
        
        # Get basic table info
        try {
            $tableInfo = aws dynamodb describe-table --table-name $tableName --region $Region --output json | ConvertFrom-Json
            Write-Host "    Status: $($tableInfo.Table.TableStatus)" -ForegroundColor Gray
            Write-Host "    Items: $($tableInfo.Table.ItemCount)" -ForegroundColor Gray
            
            # Show key schema
            $keys = $tableInfo.Table.KeySchema | ForEach-Object { "$($_.AttributeName) ($($_.KeyType))" }
            Write-Host "    Keys: $($keys -join ', ')" -ForegroundColor Gray
            
        } catch {
            Write-Host "    Error getting table info" -ForegroundColor Yellow
        }
        Write-Host ""
    }
} catch {
    Write-Host "Failed to list tables: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "=== Check Complete ===" -ForegroundColor Green