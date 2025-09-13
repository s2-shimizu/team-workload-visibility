#!/usr/bin/env node

/**
 * Continuous Deployment CLI Tool
 * Command-line interface for managing continuous deployment settings
 */

const ContinuousDeploymentMonitor = require('./continuous-deployment-monitor');
const ContinuousDeploymentConfig = require('./continuous-deployment-config');

class ContinuousDeploymentCLI {
    constructor() {
        this.config = new ContinuousDeploymentConfig();
        this.monitor = null;
    }

    /**
     * Initialize monitor with current configuration
     */
    initializeMonitor() {
        const config = this.config.loadConfig();
        this.monitor = new ContinuousDeploymentMonitor(config);
        return this.monitor;
    }

    /**
     * Display help information
     */
    showHelp() {
        console.log(`
Continuous Deployment CLI Tool

Usage: node continuous-deployment-cli.js <command> [options]

Commands:
  verify-trigger     Verify GitHub push trigger configuration
  track-deployment   Track a deployment progress (requires deployment ID)
  test-notification  Test failure notification system
  test-rollback      Test automatic rollback functionality
  config             Manage configuration settings
  status             Show current deployment status
  history            Show deployment history
  help               Show this help message

Configuration Commands:
  config show        Show current configuration
  config validate    Validate current configuration
  config reset       Reset configuration to defaults
  config template    Generate configuration template

Examples:
  node continuous-deployment-cli.js verify-trigger
  node continuous-deployment-cli.js track-deployment deploy-123
  node continuous-deployment-cli.js config show
  node continuous-deployment-cli.js test-notification
        `);
    }

    /**
     * Run CLI command
     */
    async run(args) {
        const command = args[2];
        const subCommand = args[3];
        const options = args.slice(4);

        try {
            switch (command) {
                case 'verify-trigger':
                    await this.verifyTrigger();
                    break;

                case 'track-deployment':
                    if (!subCommand) {
                        console.error('❌ Deployment ID required');
                        console.log('Usage: node continuous-deployment-cli.js track-deployment <deployment-id>');
                        process.exit(1);
                    }
                    await this.trackDeployment(subCommand);
                    break;

                case 'test-notification':
                    await this.testNotification();
                    break;

                case 'test-rollback':
                    await this.testRollback();
                    break;

                case 'config':
                    await this.handleConfigCommand(subCommand, options);
                    break;

                case 'status':
                    await this.showStatus();
                    break;

                case 'history':
                    await this.showHistory();
                    break;

                case 'help':
                case '--help':
                case '-h':
                    this.showHelp();
                    break;

                default:
                    console.error(`❌ Unknown command: ${command}`);
                    this.showHelp();
                    process.exit(1);
            }
        } catch (error) {
            console.error('❌ Error executing command:', error.message);
            process.exit(1);
        }
    }

    /**
     * Verify GitHub trigger configuration
     */
    async verifyTrigger() {
        console.log('🔍 Verifying GitHub Push Trigger Configuration\n');
        
        const monitor = this.initializeMonitor();
        const result = await monitor.verifyGitHubTrigger();

        if (result.success) {
            console.log('\n✅ GitHub trigger configuration is valid');
        } else {
            console.log('\n⚠️  GitHub trigger configuration issues found');
            
            if (result.recommendations && result.recommendations.length > 0) {
                console.log('\n📋 Recommendations:');
                result.recommendations.forEach(rec => {
                    console.log(`  • ${rec}`);
                });
            }
        }

        return result.success;
    }

    /**
     * Track deployment progress
     */
    async trackDeployment(deploymentId) {
        console.log(`📊 Tracking Deployment Progress: ${deploymentId}\n`);
        
        const monitor = this.initializeMonitor();
        const result = await monitor.trackDeploymentProgress(deploymentId);

        if (result.success) {
            console.log(`\n✅ Deployment ${deploymentId} completed successfully`);
            console.log(`Duration: ${Math.round(result.duration / 1000)}s`);
        } else {
            console.log(`\n❌ Deployment ${deploymentId} failed`);
            console.log(`Error: ${result.error}`);
        }

        return result.success;
    }

    /**
     * Test notification system
     */
    async testNotification() {
        console.log('📧 Testing Notification System\n');
        
        const monitor = this.initializeMonitor();
        
        // Create a mock failed deployment
        const mockDeployment = {
            id: `test-${Date.now()}`,
            startTime: new Date(),
            status: 'FAILED',
            error: 'Test failure for notification testing',
            phases: {
                build: { status: 'FAILED', startTime: new Date(), endTime: new Date() }
            },
            logs: [
                { timestamp: new Date(), phase: 'build', message: 'Build started', level: 'INFO' },
                { timestamp: new Date(), phase: 'build', message: 'Test failure occurred', level: 'ERROR' }
            ]
        };

        const result = await monitor.handleDeploymentFailure(mockDeployment);
        
        console.log('✅ Test notification sent');
        console.log('Check deployment-notifications.json and webhook-notifications.json for details');
        
        return result;
    }

    /**
     * Test rollback functionality
     */
    async testRollback() {
        console.log('🔄 Testing Automatic Rollback Functionality\n');
        
        const monitor = this.initializeMonitor();
        
        // Create mock deployment history
        monitor.deploymentHistory.push({
            id: 'deploy-success-1',
            status: 'SUCCESS',
            startTime: new Date(Date.now() - 3600000), // 1 hour ago
            endTime: new Date(Date.now() - 3500000)
        });

        const mockFailedDeployment = {
            id: `test-failed-${Date.now()}`,
            status: 'FAILED',
            error: 'Test failure for rollback testing'
        };

        const result = await monitor.triggerAutomaticRollback(mockFailedDeployment);
        
        if (result.success) {
            console.log('✅ Automatic rollback test completed successfully');
            console.log('Check rollback-history.json for details');
        } else {
            console.log('❌ Automatic rollback test failed:', result.reason || result.error);
        }
        
        return result.success;
    }

    /**
     * Handle configuration commands
     */
    async handleConfigCommand(subCommand, options) {
        switch (subCommand) {
            case 'show':
                this.showConfig();
                break;

            case 'validate':
                this.validateConfig();
                break;

            case 'reset':
                this.resetConfig();
                break;

            case 'template':
                this.showConfigTemplate();
                break;

            default:
                console.error(`❌ Unknown config command: ${subCommand}`);
                console.log('Available config commands: show, validate, reset, template');
        }
    }

    /**
     * Show current configuration
     */
    showConfig() {
        console.log('📋 Current Configuration:\n');
        const config = this.config.loadConfig();
        console.log(JSON.stringify(config, null, 2));
    }

    /**
     * Validate configuration
     */
    validateConfig() {
        console.log('✅ Validating Configuration:\n');
        const validation = this.config.validateConfig();
        
        if (validation.valid) {
            console.log('✅ Configuration is valid');
        } else {
            console.log('❌ Configuration validation failed');
            
            if (validation.errors.length > 0) {
                console.log('\nErrors:');
                validation.errors.forEach(error => {
                    console.log(`  • ${error}`);
                });
            }
        }

        if (validation.warnings.length > 0) {
            console.log('\nWarnings:');
            validation.warnings.forEach(warning => {
                console.log(`  • ${warning}`);
            });
        }
    }

    /**
     * Reset configuration to defaults
     */
    resetConfig() {
        console.log('🔄 Resetting Configuration to Defaults...');
        const success = this.config.resetToDefaults();
        
        if (success) {
            console.log('✅ Configuration reset successfully');
        } else {
            console.log('❌ Failed to reset configuration');
        }
    }

    /**
     * Show configuration template
     */
    showConfigTemplate() {
        console.log('📋 Configuration Template:\n');
        const template = this.config.generateConfigTemplate();
        console.log(JSON.stringify(template, null, 2));
    }

    /**
     * Show current status
     */
    async showStatus() {
        console.log('📊 Continuous Deployment Status:\n');
        
        const monitor = this.initializeMonitor();
        
        if (monitor.currentDeployment) {
            console.log('Current Deployment:');
            console.log(`  ID: ${monitor.currentDeployment.id}`);
            console.log(`  Status: ${monitor.currentDeployment.status}`);
            console.log(`  Started: ${monitor.currentDeployment.startTime}`);
        } else {
            console.log('No active deployment');
        }

        const history = monitor.getDeploymentHistory(5);
        if (history.length > 0) {
            console.log('\nRecent Deployments:');
            history.forEach(deployment => {
                console.log(`  ${deployment.id}: ${deployment.status} (${deployment.startTime})`);
            });
        }
    }

    /**
     * Show deployment history
     */
    async showHistory() {
        console.log('📚 Deployment History:\n');
        
        const monitor = this.initializeMonitor();
        const history = monitor.getDeploymentHistory(10);
        
        if (history.length === 0) {
            console.log('No deployment history available');
            return;
        }

        history.forEach(deployment => {
            console.log(`${deployment.id}:`);
            console.log(`  Status: ${deployment.status}`);
            console.log(`  Started: ${deployment.startTime}`);
            if (deployment.endTime) {
                const duration = Math.round((deployment.endTime - deployment.startTime) / 1000);
                console.log(`  Duration: ${duration}s`);
            }
            if (deployment.error) {
                console.log(`  Error: ${deployment.error}`);
            }
            console.log('');
        });
    }
}

// Run CLI if called directly
if (require.main === module) {
    const cli = new ContinuousDeploymentCLI();
    cli.run(process.argv).catch(error => {
        console.error('❌ CLI Error:', error.message);
        process.exit(1);
    });
}

module.exports = ContinuousDeploymentCLI;