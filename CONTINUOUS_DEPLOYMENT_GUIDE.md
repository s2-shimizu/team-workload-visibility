# Continuous Deployment Guide

This guide explains how to use the continuous deployment system for AWS Amplify that addresses requirements 5.1, 5.2, and 5.3.

## Overview

The continuous deployment system provides:
- **GitHub Push Trigger Verification** (Requirement 5.1)
- **Deployment Progress Tracking** (Requirement 5.2)  
- **Deployment Failure Notifications** (Requirement 5.3)
- **Automatic Rollback Functionality**

## Components

### 1. ContinuousDeploymentMonitor
Main monitoring class that handles all deployment tracking and failure management.

### 2. ContinuousDeploymentConfig
Configuration management for deployment settings.

### 3. ContinuousDeploymentCLI
Command-line interface for managing and testing the system.

### 4. GitHub Actions Template
Enhanced workflow template for GitHub Actions integration.

## Quick Start

### 1. Configuration Setup

Create or update the configuration:

```bash
# Show current configuration
node continuous-deployment-cli.js config show

# Validate configuration
node continuous-deployment-cli.js config validate

# Generate configuration template
node continuous-deployment-cli.js config template
```

### 2. Environment Variables

Set the following environment variables:

```bash
# Required
export AMPLIFY_APP_ID="your-amplify-app-id"

# Optional but recommended
export NOTIFICATION_EMAIL="your-email@example.com"
export WEBHOOK_URL="https://your-webhook-url.com"
```

### 3. Verify GitHub Trigger

Check if GitHub push triggers are properly configured:

```bash
node continuous-deployment-cli.js verify-trigger
```

### 4. Test the System

Run the complete test suite:

```bash
node test-continuous-deployment.js
```

## Features

### GitHub Push Trigger Verification (Requirement 5.1)

**What it does:**
- Verifies Amplify app configuration
- Checks branch connectivity
- Validates webhook settings
- Confirms auto-deploy is enabled

**Usage:**
```javascript
const monitor = new ContinuousDeploymentMonitor(config);
const result = await monitor.verifyGitHubTrigger();
```

**CLI Usage:**
```bash
node continuous-deployment-cli.js verify-trigger
```

### Deployment Progress Tracking (Requirement 5.2)

**What it does:**
- Tracks deployment phases (provision, build, deploy, verify)
- Records timestamps and duration
- Maintains deployment history
- Provides real-time status updates

**Usage:**
```javascript
const deploymentId = "deploy-123";
const result = await monitor.trackDeploymentProgress(deploymentId);
```

**CLI Usage:**
```bash
node continuous-deployment-cli.js track-deployment deploy-123
```

### Deployment Failure Notifications (Requirement 5.3)

**What it does:**
- Sends email notifications on failure
- Sends webhook notifications
- Includes detailed error information
- Provides troubleshooting context

**Usage:**
```javascript
// Automatic on deployment failure
const result = await monitor.handleDeploymentFailure(deployment);
```

**CLI Usage:**
```bash
node continuous-deployment-cli.js test-notification
```

### Automatic Rollback

**What it does:**
- Automatically rolls back to last successful deployment
- Maintains rollback history
- Configurable rollback settings
- Prevents cascading failures

**Usage:**
```javascript
const result = await monitor.triggerAutomaticRollback(failedDeployment);
```

**CLI Usage:**
```bash
node continuous-deployment-cli.js test-rollback
```

## Configuration Options

### Basic Configuration

```json
{
  "amplifyAppId": "your-app-id",
  "branchName": "main",
  "notificationEmail": "alerts@yourcompany.com",
  "webhookUrl": "https://hooks.slack.com/your-webhook",
  "maxRetries": 3,
  "rollbackEnabled": true,
  "monitoringEnabled": true
}
```

### Advanced Configuration

```json
{
  "notificationSettings": {
    "emailOnFailure": true,
    "webhookOnFailure": true,
    "emailOnSuccess": false,
    "webhookOnSuccess": false
  },
  "rollbackSettings": {
    "autoRollbackEnabled": true,
    "rollbackTimeoutMinutes": 30,
    "maxRollbackAttempts": 2
  },
  "deploymentSettings": {
    "timeoutMinutes": 45,
    "retryDelayMinutes": 5,
    "healthCheckEnabled": true,
    "healthCheckUrl": "https://your-app.com/health"
  }
}
```

## GitHub Actions Integration

### 1. Copy the Template

Copy `github-actions-amplify-template.yml` to `.github/workflows/amplify-deployment.yml`

### 2. Set Repository Secrets

In your GitHub repository settings, add these secrets:
- `AMPLIFY_APP_ID`
- `NOTIFICATION_EMAIL`
- `WEBHOOK_URL`

### 3. Customize the Workflow

Modify the workflow file to match your specific needs:
- Update branch names
- Adjust health check URLs
- Configure notification preferences

## CLI Commands

### Configuration Management
```bash
# Show current configuration
node continuous-deployment-cli.js config show

# Validate configuration
node continuous-deployment-cli.js config validate

# Reset to defaults
node continuous-deployment-cli.js config reset

# Show configuration template
node continuous-deployment-cli.js config template
```

### Monitoring and Testing
```bash
# Verify GitHub trigger setup
node continuous-deployment-cli.js verify-trigger

# Track a deployment
node continuous-deployment-cli.js track-deployment <deployment-id>

# Test notification system
node continuous-deployment-cli.js test-notification

# Test rollback functionality
node continuous-deployment-cli.js test-rollback
```

### Status and History
```bash
# Show current status
node continuous-deployment-cli.js status

# Show deployment history
node continuous-deployment-cli.js history

# Show help
node continuous-deployment-cli.js help
```

## Integration with Existing Scripts

### With deployment-verification.js
```javascript
const monitor = new ContinuousDeploymentMonitor(config);

// After deployment verification
if (verificationFailed) {
    await monitor.handleDeploymentFailure(deployment);
}
```

### With error-handling-orchestrator.js
```javascript
// Add continuous deployment monitoring to error handling
const cdMonitor = new ContinuousDeploymentMonitor(config);
errorOrchestrator.addHandler('deployment-failure', cdMonitor.handleDeploymentFailure);
```

## Troubleshooting

### Common Issues

1. **Configuration Not Found**
   ```bash
   # Generate default configuration
   node continuous-deployment-cli.js config template > continuous-deployment.json
   ```

2. **GitHub Trigger Not Working**
   ```bash
   # Verify trigger configuration
   node continuous-deployment-cli.js verify-trigger
   ```

3. **Notifications Not Sending**
   - Check environment variables
   - Validate email/webhook URLs
   - Test notification system

4. **Rollback Failing**
   - Ensure previous successful deployment exists
   - Check rollback permissions
   - Verify rollback settings

### Debug Mode

Enable debug logging by setting:
```bash
export DEBUG=continuous-deployment:*
```

### Log Files

The system creates several log files:
- `deployment-notifications.json` - Email notifications
- `webhook-notifications.json` - Webhook notifications  
- `rollback-history.json` - Rollback events
- `continuous-deployment-test-report.json` - Test results

## Best Practices

### 1. Configuration Management
- Store sensitive values in environment variables
- Use different configurations for different environments
- Regularly validate configuration

### 2. Monitoring
- Set up proper notification channels
- Monitor deployment success rates
- Review rollback frequency

### 3. Testing
- Run tests before deploying changes
- Test notification systems regularly
- Verify rollback procedures

### 4. Security
- Use secure webhook URLs
- Limit access to configuration files
- Regularly rotate credentials

## Requirements Compliance

### Requirement 5.1: Auto-start deployment on GitHub push
✅ **Implemented via:**
- GitHub trigger verification function
- GitHub Actions workflow template
- Configuration validation

### Requirement 5.2: Track deployment progress
✅ **Implemented via:**
- Deployment progress tracking system
- Phase-by-phase monitoring
- Real-time status updates
- Historical tracking

### Requirement 5.3: Send notification on deployment failure
✅ **Implemented via:**
- Email notification system
- Webhook notification system
- Detailed failure reporting
- Automatic rollback triggers

## Support

For issues or questions:
1. Check the troubleshooting section
2. Run the test suite to identify problems
3. Review log files for detailed error information
4. Use the CLI help command for usage information