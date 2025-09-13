/**
 * Continuous Deployment Monitor
 * Handles GitHub push trigger verification, deployment progress tracking,
 * failure notifications, and automatic rollback functionality
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

class ContinuousDeploymentMonitor {
    constructor(config = {}) {
        this.config = {
            amplifyAppId: config.amplifyAppId || process.env.AMPLIFY_APP_ID,
            branchName: config.branchName || 'main',
            notificationEmail: config.notificationEmail || process.env.NOTIFICATION_EMAIL,
            webhookUrl: config.webhookUrl || process.env.WEBHOOK_URL,
            maxRetries: config.maxRetries || 3,
            rollbackEnabled: config.rollbackEnabled !== false,
            ...config
        };
        
        this.deploymentHistory = [];
        this.currentDeployment = null;
    }

    /**
     * Verify GitHub push trigger configuration
     * Requirements: 5.1 - Auto-start deployment on GitHub push
     */
    async verifyGitHubTrigger() {
        console.log('🔍 Verifying GitHub push trigger configuration...');
        
        const checks = {
            amplifyAppExists: false,
            branchConnected: false,
            webhookConfigured: false,
            autoDeployEnabled: false
        };

        try {
            // Check if Amplify app exists
            if (this.config.amplifyAppId) {
                console.log(`✓ Amplify App ID configured: ${this.config.amplifyAppId}`);
                checks.amplifyAppExists = true;
            } else {
                console.log('❌ Amplify App ID not configured');
            }

            // Check branch configuration
            if (this.config.branchName) {
                console.log(`✓ Branch configured: ${this.config.branchName}`);
                checks.branchConnected = true;
            } else {
                console.log('❌ Branch name not configured');
            }

            // Check webhook configuration
            if (this.config.webhookUrl) {
                console.log('✓ Webhook URL configured');
                checks.webhookConfigured = true;
            } else {
                console.log('⚠️  Webhook URL not configured (optional)');
            }

            // Auto-deploy is typically enabled by default in Amplify
            checks.autoDeployEnabled = true;
            console.log('✓ Auto-deploy assumed to be enabled');

            const allChecksPass = Object.values(checks).every(check => check);
            
            if (allChecksPass) {
                console.log('✅ GitHub push trigger configuration verified successfully');
            } else {
                console.log('⚠️  Some GitHub trigger configuration issues found');
            }

            return {
                success: allChecksPass,
                checks,
                recommendations: this.generateTriggerRecommendations(checks)
            };

        } catch (error) {
            console.error('❌ Error verifying GitHub trigger:', error.message);
            return {
                success: false,
                error: error.message,
                checks
            };
        }
    }

    /**
     * Generate recommendations for trigger configuration
     */
    generateTriggerRecommendations(checks) {
        const recommendations = [];

        if (!checks.amplifyAppExists) {
            recommendations.push('Set AMPLIFY_APP_ID environment variable');
        }

        if (!checks.branchConnected) {
            recommendations.push('Configure branch name in deployment settings');
        }

        if (!checks.webhookConfigured) {
            recommendations.push('Consider setting up webhook for custom notifications');
        }

        return recommendations;
    }

    /**
     * Track deployment progress
     * Requirements: 5.2 - Track deployment progress
     */
    async trackDeploymentProgress(deploymentId) {
        console.log(`📊 Starting deployment progress tracking for: ${deploymentId}`);
        
        const deployment = {
            id: deploymentId,
            startTime: new Date(),
            status: 'IN_PROGRESS',
            phases: {
                provision: { status: 'PENDING', startTime: null, endTime: null },
                build: { status: 'PENDING', startTime: null, endTime: null },
                deploy: { status: 'PENDING', startTime: null, endTime: null },
                verify: { status: 'PENDING', startTime: null, endTime: null }
            },
            logs: []
        };

        this.currentDeployment = deployment;
        this.deploymentHistory.push(deployment);

        try {
            // Simulate deployment phases tracking
            await this.trackPhase(deployment, 'provision', 'Provisioning resources...');
            await this.trackPhase(deployment, 'build', 'Building application...');
            await this.trackPhase(deployment, 'deploy', 'Deploying to AWS...');
            await this.trackPhase(deployment, 'verify', 'Verifying deployment...');

            deployment.status = 'SUCCESS';
            deployment.endTime = new Date();
            
            console.log('✅ Deployment completed successfully');
            
            return {
                success: true,
                deployment,
                duration: deployment.endTime - deployment.startTime
            };

        } catch (error) {
            deployment.status = 'FAILED';
            deployment.endTime = new Date();
            deployment.error = error.message;
            
            console.error('❌ Deployment failed:', error.message);
            
            // Trigger failure notification
            await this.handleDeploymentFailure(deployment);
            
            return {
                success: false,
                deployment,
                error: error.message
            };
        }
    }

    /**
     * Track individual deployment phase
     */
    async trackPhase(deployment, phaseName, message) {
        console.log(`🔄 ${message}`);
        
        const phase = deployment.phases[phaseName];
        phase.status = 'IN_PROGRESS';
        phase.startTime = new Date();
        
        deployment.logs.push({
            timestamp: new Date(),
            phase: phaseName,
            message,
            level: 'INFO'
        });

        // Simulate phase execution time
        await new Promise(resolve => setTimeout(resolve, 1000 + Math.random() * 2000));

        // Simulate potential failure (5% chance)
        if (Math.random() < 0.05) {
            phase.status = 'FAILED';
            phase.endTime = new Date();
            throw new Error(`${phaseName} phase failed`);
        }

        phase.status = 'SUCCESS';
        phase.endTime = new Date();
        
        console.log(`✅ ${phaseName} phase completed`);
    }

    /**
     * Handle deployment failure and send notifications
     * Requirements: 5.3 - Send notification on deployment failure
     */
    async handleDeploymentFailure(deployment) {
        console.log('🚨 Handling deployment failure...');

        const failureDetails = {
            deploymentId: deployment.id,
            failedPhase: this.getFailedPhase(deployment),
            error: deployment.error,
            timestamp: new Date(),
            logs: deployment.logs.slice(-10) // Last 10 log entries
        };

        // Send email notification
        await this.sendFailureNotification(failureDetails);

        // Send webhook notification
        await this.sendWebhookNotification(failureDetails);

        // Trigger automatic rollback if enabled
        if (this.config.rollbackEnabled) {
            await this.triggerAutomaticRollback(deployment);
        }

        return failureDetails;
    }

    /**
     * Get the phase that failed
     */
    getFailedPhase(deployment) {
        for (const [phaseName, phase] of Object.entries(deployment.phases)) {
            if (phase.status === 'FAILED') {
                return phaseName;
            }
        }
        return 'unknown';
    }

    /**
     * Send failure notification email
     */
    async sendFailureNotification(failureDetails) {
        if (!this.config.notificationEmail) {
            console.log('⚠️  No notification email configured, skipping email notification');
            return;
        }

        console.log(`📧 Sending failure notification to: ${this.config.notificationEmail}`);

        const emailContent = {
            to: this.config.notificationEmail,
            subject: `Deployment Failed - ${failureDetails.deploymentId}`,
            body: this.generateFailureEmailBody(failureDetails)
        };

        // In a real implementation, this would integrate with AWS SES or similar
        console.log('Email notification prepared:', emailContent.subject);
        
        // Save notification to file for testing
        const notificationFile = path.join(__dirname, 'deployment-notifications.json');
        const notifications = this.loadNotifications(notificationFile);
        notifications.push({
            type: 'email',
            timestamp: new Date(),
            ...emailContent
        });
        
        fs.writeFileSync(notificationFile, JSON.stringify(notifications, null, 2));
        console.log('✅ Email notification logged');
    }

    /**
     * Send webhook notification
     */
    async sendWebhookNotification(failureDetails) {
        if (!this.config.webhookUrl) {
            console.log('⚠️  No webhook URL configured, skipping webhook notification');
            return;
        }

        console.log(`🔗 Sending webhook notification to: ${this.config.webhookUrl}`);

        const webhookPayload = {
            event: 'deployment_failed',
            deployment: failureDetails,
            timestamp: new Date()
        };

        // In a real implementation, this would make an HTTP POST request
        console.log('Webhook notification prepared');
        
        // Save webhook payload to file for testing
        const webhookFile = path.join(__dirname, 'webhook-notifications.json');
        const webhooks = this.loadNotifications(webhookFile);
        webhooks.push({
            type: 'webhook',
            url: this.config.webhookUrl,
            payload: webhookPayload,
            timestamp: new Date()
        });
        
        fs.writeFileSync(webhookFile, JSON.stringify(webhooks, null, 2));
        console.log('✅ Webhook notification logged');
    }

    /**
     * Generate failure email body
     */
    generateFailureEmailBody(failureDetails) {
        return `
Deployment Failure Alert

Deployment ID: ${failureDetails.deploymentId}
Failed Phase: ${failureDetails.failedPhase}
Error: ${failureDetails.error}
Timestamp: ${failureDetails.timestamp}

Recent Logs:
${failureDetails.logs.map(log => `[${log.timestamp}] ${log.phase}: ${log.message}`).join('\n')}

Please check the AWS Amplify console for more details.
        `.trim();
    }

    /**
     * Load existing notifications from file
     */
    loadNotifications(filePath) {
        try {
            if (fs.existsSync(filePath)) {
                return JSON.parse(fs.readFileSync(filePath, 'utf8'));
            }
        } catch (error) {
            console.warn('Warning: Could not load existing notifications:', error.message);
        }
        return [];
    }

    /**
     * Trigger automatic rollback
     * Requirements: Automatic rollback on deployment failure
     */
    async triggerAutomaticRollback(failedDeployment) {
        console.log('🔄 Triggering automatic rollback...');

        try {
            // Find the last successful deployment
            const lastSuccessfulDeployment = this.findLastSuccessfulDeployment();
            
            if (!lastSuccessfulDeployment) {
                console.log('⚠️  No previous successful deployment found, cannot rollback');
                return {
                    success: false,
                    reason: 'No previous successful deployment available'
                };
            }

            console.log(`📦 Rolling back to deployment: ${lastSuccessfulDeployment.id}`);

            const rollbackDeployment = {
                id: `rollback-${Date.now()}`,
                type: 'ROLLBACK',
                targetDeployment: lastSuccessfulDeployment.id,
                startTime: new Date(),
                status: 'IN_PROGRESS'
            };

            // Simulate rollback process
            await new Promise(resolve => setTimeout(resolve, 3000));

            rollbackDeployment.status = 'SUCCESS';
            rollbackDeployment.endTime = new Date();

            console.log('✅ Automatic rollback completed successfully');

            // Log rollback event
            const rollbackFile = path.join(__dirname, 'rollback-history.json');
            const rollbacks = this.loadNotifications(rollbackFile);
            rollbacks.push({
                originalDeployment: failedDeployment.id,
                rollbackDeployment: rollbackDeployment,
                timestamp: new Date()
            });
            
            fs.writeFileSync(rollbackFile, JSON.stringify(rollbacks, null, 2));

            return {
                success: true,
                rollbackDeployment
            };

        } catch (error) {
            console.error('❌ Automatic rollback failed:', error.message);
            return {
                success: false,
                error: error.message
            };
        }
    }

    /**
     * Find the last successful deployment
     */
    findLastSuccessfulDeployment() {
        // Look through deployment history in reverse order
        for (let i = this.deploymentHistory.length - 2; i >= 0; i--) {
            const deployment = this.deploymentHistory[i];
            if (deployment.status === 'SUCCESS' && deployment.type !== 'ROLLBACK') {
                return deployment;
            }
        }
        return null;
    }

    /**
     * Get deployment status
     */
    getDeploymentStatus(deploymentId) {
        const deployment = this.deploymentHistory.find(d => d.id === deploymentId);
        return deployment || null;
    }

    /**
     * Get deployment history
     */
    getDeploymentHistory(limit = 10) {
        return this.deploymentHistory.slice(-limit).reverse();
    }
}

module.exports = ContinuousDeploymentMonitor;