/**
 * Continuous Deployment Configuration
 * Manages configuration settings for continuous deployment monitoring
 */

const fs = require('fs');
const path = require('path');

class ContinuousDeploymentConfig {
    constructor() {
        this.configFile = path.join(__dirname, 'continuous-deployment.json');
        this.defaultConfig = {
            amplifyAppId: process.env.AMPLIFY_APP_ID || '',
            branchName: 'main',
            notificationEmail: process.env.NOTIFICATION_EMAIL || '',
            webhookUrl: process.env.WEBHOOK_URL || '',
            maxRetries: 3,
            rollbackEnabled: true,
            monitoringEnabled: true,
            notificationSettings: {
                emailOnFailure: true,
                webhookOnFailure: true,
                emailOnSuccess: false,
                webhookOnSuccess: false
            },
            rollbackSettings: {
                autoRollbackEnabled: true,
                rollbackTimeoutMinutes: 30,
                maxRollbackAttempts: 2
            },
            deploymentSettings: {
                timeoutMinutes: 45,
                retryDelayMinutes: 5,
                healthCheckEnabled: true,
                healthCheckUrl: process.env.HEALTH_CHECK_URL || ''
            }
        };
    }

    /**
     * Load configuration from file or create default
     */
    loadConfig() {
        try {
            if (fs.existsSync(this.configFile)) {
                const fileConfig = JSON.parse(fs.readFileSync(this.configFile, 'utf8'));
                return { ...this.defaultConfig, ...fileConfig };
            } else {
                // Create default config file
                this.saveConfig(this.defaultConfig);
                return this.defaultConfig;
            }
        } catch (error) {
            console.warn('Warning: Could not load config, using defaults:', error.message);
            return this.defaultConfig;
        }
    }

    /**
     * Save configuration to file
     */
    saveConfig(config) {
        try {
            fs.writeFileSync(this.configFile, JSON.stringify(config, null, 2));
            console.log('✅ Configuration saved successfully');
            return true;
        } catch (error) {
            console.error('❌ Failed to save configuration:', error.message);
            return false;
        }
    }

    /**
     * Update specific configuration values
     */
    updateConfig(updates) {
        const currentConfig = this.loadConfig();
        const newConfig = { ...currentConfig, ...updates };
        return this.saveConfig(newConfig);
    }

    /**
     * Validate configuration
     */
    validateConfig(config = null) {
        const configToValidate = config || this.loadConfig();
        const errors = [];
        const warnings = [];

        // Required fields validation
        if (!configToValidate.amplifyAppId) {
            errors.push('amplifyAppId is required');
        }

        if (!configToValidate.branchName) {
            errors.push('branchName is required');
        }

        // Optional but recommended fields
        if (!configToValidate.notificationEmail) {
            warnings.push('notificationEmail is not configured - email notifications will be disabled');
        }

        if (!configToValidate.webhookUrl) {
            warnings.push('webhookUrl is not configured - webhook notifications will be disabled');
        }

        // Validate numeric values
        if (configToValidate.maxRetries < 0 || configToValidate.maxRetries > 10) {
            errors.push('maxRetries must be between 0 and 10');
        }

        if (configToValidate.deploymentSettings.timeoutMinutes < 5 || configToValidate.deploymentSettings.timeoutMinutes > 120) {
            errors.push('deploymentSettings.timeoutMinutes must be between 5 and 120');
        }

        return {
            valid: errors.length === 0,
            errors,
            warnings
        };
    }

    /**
     * Get environment-specific configuration
     */
    getEnvironmentConfig(environment = 'production') {
        const baseConfig = this.loadConfig();
        
        const environmentOverrides = {
            development: {
                rollbackEnabled: false,
                notificationSettings: {
                    emailOnFailure: false,
                    webhookOnFailure: false
                }
            },
            staging: {
                rollbackEnabled: true,
                notificationSettings: {
                    emailOnFailure: true,
                    webhookOnFailure: false
                }
            },
            production: {
                rollbackEnabled: true,
                notificationSettings: {
                    emailOnFailure: true,
                    webhookOnFailure: true
                }
            }
        };

        const envOverride = environmentOverrides[environment] || {};
        return { ...baseConfig, ...envOverride };
    }

    /**
     * Generate configuration template
     */
    generateConfigTemplate() {
        const template = {
            ...this.defaultConfig,
            // Add comments as properties for documentation
            _comments: {
                amplifyAppId: "Your AWS Amplify App ID (required)",
                branchName: "Git branch to monitor for deployments (default: main)",
                notificationEmail: "Email address for deployment notifications",
                webhookUrl: "Webhook URL for deployment notifications",
                maxRetries: "Maximum number of retry attempts for failed deployments",
                rollbackEnabled: "Enable automatic rollback on deployment failure",
                monitoringEnabled: "Enable deployment monitoring and tracking"
            }
        };

        return template;
    }

    /**
     * Export configuration for external tools
     */
    exportConfig(format = 'json') {
        const config = this.loadConfig();
        
        switch (format.toLowerCase()) {
            case 'json':
                return JSON.stringify(config, null, 2);
            
            case 'yaml':
                // Simple YAML export (would need yaml library for complex cases)
                return Object.entries(config)
                    .map(([key, value]) => `${key}: ${JSON.stringify(value)}`)
                    .join('\n');
            
            case 'env':
                // Export as environment variables
                const envVars = [];
                envVars.push(`AMPLIFY_APP_ID=${config.amplifyAppId}`);
                envVars.push(`NOTIFICATION_EMAIL=${config.notificationEmail}`);
                envVars.push(`WEBHOOK_URL=${config.webhookUrl}`);
                envVars.push(`ROLLBACK_ENABLED=${config.rollbackEnabled}`);
                return envVars.join('\n');
            
            default:
                throw new Error(`Unsupported export format: ${format}`);
        }
    }

    /**
     * Reset configuration to defaults
     */
    resetToDefaults() {
        return this.saveConfig(this.defaultConfig);
    }
}

module.exports = ContinuousDeploymentConfig;