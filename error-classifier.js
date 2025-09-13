#!/usr/bin/env node

/**
 * Error Classification and Message Display System
 * 
 * Features:
 * - Intelligent error classification and categorization
 * - Context-aware error message formatting
 * - Severity-based error prioritization
 * - Actionable error resolution suggestions
 * - Integration with all error handling components
 * 
 * Requirements: 1.1, 1.2, 1.3
 */

const fs = require('fs');
const path = require('path');

class ErrorClassifier {
    constructor(options = {}) {
        this.errors = [];
        this.classifications = new Map();
        this.messageTemplates = new Map();
        this.resolutionStrategies = new Map();

        this.options = {
            verbose: options.verbose || false,
            includeStackTrace: options.includeStackTrace || false,
            maxSimilarErrors: options.maxSimilarErrors || 5,
            ...options
        };

        this.initializeClassificationRules();
        this.initializeMessageTemplates();
        this.initializeResolutionStrategies();
    }

    /**
     * Initialize error classification rules
     */
    initializeClassificationRules() {
        // Configuration errors
        this.addClassificationRule('CONFIGURATION', [
            { pattern: /amplify\.yml.*syntax/i, subtype: 'YAML_SYNTAX', severity: 'CRITICAL' },
            { pattern: /amplify\.yml.*missing/i, subtype: 'MISSING_CONFIG', severity: 'CRITICAL' },
            { pattern: /package\.json.*invalid/i, subtype: 'JSON_SYNTAX', severity: 'ERROR' },
            { pattern: /pom\.xml.*invalid/i, subtype: 'XML_SYNTAX', severity: 'ERROR' },
            { pattern: /environment variable/i, subtype: 'ENV_VAR', severity: 'ERROR' },
            { pattern: /configuration.*missing/i, subtype: 'MISSING_CONFIG', severity: 'ERROR' }
        ]);

        // Dependency errors
        this.addClassificationRule('DEPENDENCY', [
            { pattern: /node.*not found/i, subtype: 'MISSING_NODEJS', severity: 'CRITICAL' },
            { pattern: /java.*not found/i, subtype: 'MISSING_JAVA', severity: 'CRITICAL' },
            { pattern: /maven.*not found/i, subtype: 'MISSING_MAVEN', severity: 'ERROR' },
            { pattern: /aws.*not found/i, subtype: 'MISSING_AWS_CLI', severity: 'CRITICAL' },
            { pattern: /npm.*install/i, subtype: 'NPM_INSTALL', severity: 'ERROR' },
            { pattern: /dependency.*missing/i, subtype: 'MISSING_DEPENDENCY', severity: 'ERROR' },
            { pattern: /version.*conflict/i, subtype: 'VERSION_CONFLICT', severity: 'WARNING' },
            { pattern: /credentials.*not configured/i, subtype: 'AWS_CREDENTIALS', severity: 'CRITICAL' }
        ]);

        // Build errors
        this.addClassificationRule('BUILD', [
            { pattern: /compilation.*failed/i, subtype: 'COMPILE_ERROR', severity: 'ERROR' },
            { pattern: /build.*failed/i, subtype: 'BUILD_FAILURE', severity: 'ERROR' },
            { pattern: /file.*not found/i, subtype: 'MISSING_FILE', severity: 'ERROR' },
            { pattern: /permission.*denied/i, subtype: 'PERMISSION', severity: 'ERROR' },
            { pattern: /out of memory/i, subtype: 'MEMORY', severity: 'ERROR' },
            { pattern: /timeout/i, subtype: 'TIMEOUT', severity: 'ERROR' },
            { pattern: /syntax.*error/i, subtype: 'SYNTAX_ERROR', severity: 'ERROR' }
        ]);

        // Deployment errors
        this.addClassificationRule('DEPLOYMENT', [
            { pattern: /amplify.*deploy.*failed/i, subtype: 'AMPLIFY_DEPLOY', severity: 'CRITICAL' },
            { pattern: /lambda.*deploy.*failed/i, subtype: 'LAMBDA_DEPLOY', severity: 'CRITICAL' },
            { pattern: /api.*gateway.*failed/i, subtype: 'API_GATEWAY', severity: 'ERROR' },
            { pattern: /cloudfront.*failed/i, subtype: 'CLOUDFRONT', severity: 'ERROR' },
            { pattern: /s3.*upload.*failed/i, subtype: 'S3_UPLOAD', severity: 'ERROR' },
            { pattern: /iam.*permission/i, subtype: 'IAM_PERMISSION', severity: 'ERROR' }
        ]);

        // System errors
        this.addClassificationRule('SYSTEM', [
            { pattern: /disk.*space/i, subtype: 'DISK_SPACE', severity: 'CRITICAL' },
            { pattern: /memory.*usage/i, subtype: 'MEMORY_USAGE', severity: 'WARNING' },
            { pattern: /network.*connectivity/i, subtype: 'NETWORK', severity: 'WARNING' },
            { pattern: /process.*terminated/i, subtype: 'PROCESS_TERMINATED', severity: 'ERROR' },
            { pattern: /system.*resource/i, subtype: 'RESOURCE_LIMIT', severity: 'ERROR' }
        ]);

        // Validation errors
        this.addClassificationRule('VALIDATION', [
            { pattern: /validation.*failed/i, subtype: 'VALIDATION_FAILURE', severity: 'ERROR' },
            { pattern: /schema.*validation/i, subtype: 'SCHEMA_VALIDATION', severity: 'ERROR' },
            { pattern: /format.*invalid/i, subtype: 'FORMAT_INVALID', severity: 'ERROR' },
            { pattern: /checksum.*mismatch/i, subtype: 'CHECKSUM_MISMATCH', severity: 'ERROR' }
        ]);
    }

    /**
     * Add classification rule
     */
    addClassificationRule(category, rules) {
        if (!this.classifications.has(category)) {
            this.classifications.set(category, []);
        }
        this.classifications.get(category).push(...rules);
    }

    /**
     * Initialize message templates
     */
    initializeMessageTemplates() {
        // Configuration error templates
        this.addMessageTemplate('CONFIGURATION', 'YAML_SYNTAX', {
            title: '⚙️  Configuration Syntax Error',
            format: 'YAML syntax error in {file}: {error}',
            context: 'This error occurs when the YAML configuration file has invalid syntax.',
            impact: 'Deployment will fail until the syntax is corrected.',
            urgency: 'IMMEDIATE'
        });

        this.addMessageTemplate('CONFIGURATION', 'MISSING_CONFIG', {
            title: '⚙️  Missing Configuration',
            format: 'Required configuration file missing: {file}',
            context: 'A required configuration file is not present in the project.',
            impact: 'The build or deployment process cannot proceed without this file.',
            urgency: 'IMMEDIATE'
        });

        // Dependency error templates
        this.addMessageTemplate('DEPENDENCY', 'MISSING_NODEJS', {
            title: '📦 Node.js Not Found',
            format: 'Node.js is not installed or not in PATH',
            context: 'Node.js is required for frontend build and package management.',
            impact: 'Frontend build will fail and npm commands will not work.',
            urgency: 'IMMEDIATE'
        });

        this.addMessageTemplate('DEPENDENCY', 'MISSING_JAVA', {
            title: '☕ Java Not Found',
            format: 'Java is not installed or not in PATH',
            context: 'Java is required for backend compilation and Spring Boot application.',
            impact: 'Backend build will fail and JAR file cannot be created.',
            urgency: 'IMMEDIATE'
        });

        this.addMessageTemplate('DEPENDENCY', 'AWS_CREDENTIALS', {
            title: '☁️  AWS Credentials Not Configured',
            format: 'AWS credentials are not configured for deployment',
            context: 'AWS credentials are required to deploy resources to AWS.',
            impact: 'Deployment to AWS will fail without proper authentication.',
            urgency: 'IMMEDIATE'
        });

        // Build error templates
        this.addMessageTemplate('BUILD', 'COMPILE_ERROR', {
            title: '🔨 Compilation Failed',
            format: 'Compilation failed in {component}: {error}',
            context: 'Source code compilation encountered errors.',
            impact: 'Build artifacts cannot be created until compilation issues are resolved.',
            urgency: 'HIGH'
        });

        this.addMessageTemplate('BUILD', 'MISSING_FILE', {
            title: '📄 Required File Missing',
            format: 'Required file not found: {file}',
            context: 'A file required for the build process is missing.',
            impact: 'Build process will fail until the missing file is provided.',
            urgency: 'HIGH'
        });

        // Add more templates for other categories...
        this.addDefaultTemplates();
    }

    /**
     * Add default templates for common error types
     */
    addDefaultTemplates() {
        const defaultTemplate = {
            title: '❌ Error',
            format: '{message}',
            context: 'An error occurred during the process.',
            impact: 'The operation may not complete successfully.',
            urgency: 'MEDIUM'
        };

        // Add default templates for each category
        const categories = ['DEPLOYMENT', 'SYSTEM', 'VALIDATION'];
        const subtypes = ['GENERIC', 'UNKNOWN'];

        for (const category of categories) {
            for (const subtype of subtypes) {
                this.addMessageTemplate(category, subtype, {
                    ...defaultTemplate,
                    title: `${this.getCategoryIcon(category)} ${category} Error`
                });
            }
        }
    }

    /**
     * Add message template
     */
    addMessageTemplate(category, subtype, template) {
        const key = `${category}:${subtype}`;
        this.messageTemplates.set(key, template);
    }

    /**
     * Initialize resolution strategies
     */
    initializeResolutionStrategies() {
        // Configuration resolution strategies
        this.addResolutionStrategy('CONFIGURATION', 'YAML_SYNTAX', {
            immediate: [
                'Check YAML indentation (use 2 spaces, no tabs)',
                'Validate YAML syntax using online validator',
                'Look for missing colons or quotes in values'
            ],
            detailed: [
                'Open the configuration file in a text editor',
                'Check for consistent indentation (2 spaces per level)',
                'Ensure all string values with special characters are quoted',
                'Validate the file structure matches the expected schema',
                'Use a YAML linter or validator to identify specific issues'
            ],
            preventive: [
                'Use a code editor with YAML syntax highlighting',
                'Set up pre-commit hooks to validate YAML files',
                'Use configuration templates to avoid syntax errors'
            ]
        });

        this.addResolutionStrategy('DEPENDENCY', 'MISSING_NODEJS', {
            immediate: [
                'Install Node.js from https://nodejs.org/',
                'Verify installation: node --version',
                'Restart terminal after installation'
            ],
            detailed: [
                'Download Node.js LTS version from official website',
                'Run the installer with administrator privileges',
                'Add Node.js to system PATH if not done automatically',
                'Verify npm is also installed: npm --version',
                'Consider using Node Version Manager (nvm) for version management'
            ],
            preventive: [
                'Use Node Version Manager (nvm) for consistent environments',
                'Document required Node.js version in README',
                'Include Node.js version check in build scripts'
            ]
        });

        this.addResolutionStrategy('DEPENDENCY', 'AWS_CREDENTIALS', {
            immediate: [
                'Run: aws configure',
                'Enter AWS Access Key ID and Secret Access Key',
                'Set default region (e.g., us-east-1)'
            ],
            detailed: [
                'Obtain AWS credentials from AWS Console (IAM)',
                'Run "aws configure" command',
                'Enter AWS Access Key ID when prompted',
                'Enter AWS Secret Access Key when prompted',
                'Enter default region name (e.g., us-east-1)',
                'Enter default output format (json recommended)',
                'Verify configuration: aws sts get-caller-identity'
            ],
            preventive: [
                'Use IAM roles instead of access keys when possible',
                'Rotate access keys regularly',
                'Use AWS profiles for multiple environments',
                'Never commit AWS credentials to version control'
            ]
        });

        // Add more resolution strategies...
        this.addDefaultResolutionStrategies();
    }

    /**
     * Add default resolution strategies
     */
    addDefaultResolutionStrategies() {
        const defaultStrategy = {
            immediate: ['Review error message for specific details', 'Check recent changes to configuration'],
            detailed: ['Analyze error context and related components', 'Consult documentation for the affected component'],
            preventive: ['Implement proper testing procedures', 'Use version control for configuration changes']
        };

        // Add default strategies for categories without specific strategies
        const categories = ['BUILD', 'DEPLOYMENT', 'SYSTEM', 'VALIDATION'];
        for (const category of categories) {
            this.addResolutionStrategy(category, 'GENERIC', defaultStrategy);
        }
    }

    /**
     * Add resolution strategy
     */
    addResolutionStrategy(category, subtype, strategy) {
        const key = `${category}:${subtype}`;
        this.resolutionStrategies.set(key, strategy);
    }

    /**
     * Classify error
     */
    classifyError(error) {
        const errorText = `${error.title} ${error.message}`.toLowerCase();

        // Try to match against classification rules
        for (const [category, rules] of this.classifications.entries()) {
            for (const rule of rules) {
                if (rule.pattern.test(errorText)) {
                    return {
                        category,
                        subtype: rule.subtype,
                        severity: rule.severity,
                        confidence: this.calculateConfidence(errorText, rule.pattern)
                    };
                }
            }
        }

        // Default classification
        return {
            category: 'UNKNOWN',
            subtype: 'GENERIC',
            severity: error.severity || 'ERROR',
            confidence: 0.1
        };
    }

    /**
     * Calculate classification confidence
     */
    calculateConfidence(text, pattern) {
        const matches = text.match(pattern);
        if (!matches) return 0;

        // Base confidence on pattern specificity and match quality
        const patternComplexity = pattern.source.length;
        const matchLength = matches[0].length;

        return Math.min(0.9, (matchLength / text.length) * (patternComplexity / 100));
    }

    /**
     * Format error message
     */
    formatErrorMessage(error, classification) {
        const templateKey = `${classification.category}:${classification.subtype}`;
        let template = this.messageTemplates.get(templateKey);

        // Fallback to generic template
        if (!template) {
            template = this.messageTemplates.get(`${classification.category}:GENERIC`) ||
                this.messageTemplates.get('UNKNOWN:GENERIC') ||
                { title: '❌ Error', format: '{message}', context: '', impact: '', urgency: 'MEDIUM' };
        }

        // Format the message
        const formattedMessage = this.interpolateTemplate(template.format, {
            message: error.message,
            title: error.title,
            file: error.details?.file || 'unknown',
            component: error.details?.component || 'unknown',
            error: error.message
        });

        return {
            ...template,
            formattedMessage,
            classification,
            originalError: error,
            timestamp: new Date().toISOString()
        };
    }

    /**
     * Interpolate template with variables
     */
    interpolateTemplate(template, variables) {
        return template.replace(/\{(\w+)\}/g, (match, key) => {
            return variables[key] || match;
        });
    }

    /**
     * Get resolution suggestions
     */
    getResolutionSuggestions(classification) {
        const strategyKey = `${classification.category}:${classification.subtype}`;
        let strategy = this.resolutionStrategies.get(strategyKey);

        // Fallback to generic strategy
        if (!strategy) {
            strategy = this.resolutionStrategies.get(`${classification.category}:GENERIC`) ||
                this.resolutionStrategies.get('UNKNOWN:GENERIC') ||
            {
                immediate: ['Review error details and context'],
                detailed: ['Consult documentation for the affected component'],
                preventive: ['Implement proper error handling and validation']
            };
        }

        return strategy;
    }

    /**
     * Process and classify errors
     */
    processErrors(errors) {
        const processedErrors = [];
        const errorGroups = new Map();

        for (const error of errors) {
            // Classify the error
            const classification = this.classifyError(error);

            // Format the error message
            const formattedError = this.formatErrorMessage(error, classification);

            // Get resolution suggestions
            const resolutions = this.getResolutionSuggestions(classification);
            formattedError.resolutions = resolutions;

            // Group similar errors
            const groupKey = `${classification.category}:${classification.subtype}`;
            if (!errorGroups.has(groupKey)) {
                errorGroups.set(groupKey, []);
            }
            errorGroups.get(groupKey).push(formattedError);

            processedErrors.push(formattedError);
        }

        // Consolidate similar errors
        const consolidatedErrors = this.consolidateSimilarErrors(errorGroups);

        return {
            processed: processedErrors,
            consolidated: consolidatedErrors,
            summary: this.generateErrorSummary(processedErrors)
        };
    }

    /**
     * Consolidate similar errors
     */
    consolidateSimilarErrors(errorGroups) {
        const consolidated = [];

        for (const [groupKey, errors] of errorGroups.entries()) {
            if (errors.length === 1) {
                consolidated.push(errors[0]);
            } else {
                // Create consolidated error for multiple similar errors
                const firstError = errors[0];
                const consolidatedError = {
                    ...firstError,
                    title: `${firstError.title} (${errors.length} occurrences)`,
                    formattedMessage: `${firstError.formattedMessage} (and ${errors.length - 1} similar)`,
                    occurrences: errors.length,
                    examples: errors.slice(0, this.options.maxSimilarErrors),
                    consolidated: true
                };
                consolidated.push(consolidatedError);
            }
        }

        // Sort by severity and urgency
        return consolidated.sort((a, b) => {
            const severityOrder = { 'CRITICAL': 4, 'ERROR': 3, 'WARNING': 2, 'INFO': 1 };
            const urgencyOrder = { 'IMMEDIATE': 4, 'HIGH': 3, 'MEDIUM': 2, 'LOW': 1 };

            const aSeverity = severityOrder[a.classification.severity] || 0;
            const bSeverity = severityOrder[b.classification.severity] || 0;
            const aUrgency = urgencyOrder[a.urgency] || 0;
            const bUrgency = urgencyOrder[b.urgency] || 0;

            if (aSeverity !== bSeverity) return bSeverity - aSeverity;
            return bUrgency - aUrgency;
        });
    }

    /**
     * Generate error summary
     */
    generateErrorSummary(errors) {
        const summary = {
            total: errors.length,
            bySeverity: {},
            byCategory: {},
            byUrgency: {},
            criticalCount: 0,
            actionRequired: 0
        };

        for (const error of errors) {
            const severity = error.classification.severity;
            const category = error.classification.category;
            const urgency = error.urgency;

            // Count by severity
            summary.bySeverity[severity] = (summary.bySeverity[severity] || 0) + 1;

            // Count by category
            summary.byCategory[category] = (summary.byCategory[category] || 0) + 1;

            // Count by urgency
            summary.byUrgency[urgency] = (summary.byUrgency[urgency] || 0) + 1;

            // Count critical and action required
            if (severity === 'CRITICAL') summary.criticalCount++;
            if (urgency === 'IMMEDIATE' || urgency === 'HIGH') summary.actionRequired++;
        }

        return summary;
    }

    /**
     * Display formatted errors
     */
    displayErrors(processedResult) {
        const { consolidated, summary } = processedResult;

        console.log('\n🚨 Error Classification Report');
        console.log('===============================');

        // Display summary
        this.displayErrorSummary(summary);

        // Display consolidated errors
        console.log('\n📋 Classified Errors:');
        for (let i = 0; i < consolidated.length; i++) {
            const error = consolidated[i];
            this.displayFormattedError(error, i + 1);
        }

        // Display resolution priorities
        this.displayResolutionPriorities(consolidated);
    }

    /**
     * Display error summary
     */
    displayErrorSummary(summary) {
        console.log(`\n📊 Summary: ${summary.total} errors found`);
        console.log(`   Critical: ${summary.criticalCount}`);
        console.log(`   Action Required: ${summary.actionRequired}`);

        console.log('\n📈 By Severity:');
        for (const [severity, count] of Object.entries(summary.bySeverity)) {
            const icon = this.getSeverityIcon(severity);
            console.log(`   ${icon} ${severity}: ${count}`);
        }

        console.log('\n📂 By Category:');
        for (const [category, count] of Object.entries(summary.byCategory)) {
            const icon = this.getCategoryIcon(category);
            console.log(`   ${icon} ${category}: ${count}`);
        }
    }

    /**
     * Display formatted error
     */
    displayFormattedError(error, index) {
        const severityIcon = this.getSeverityIcon(error.classification.severity);
        const urgencyIcon = this.getUrgencyIcon(error.urgency);

        console.log(`\n${index}. ${error.title}`);
        console.log(`   ${severityIcon} Severity: ${error.classification.severity}`);
        console.log(`   ${urgencyIcon} Urgency: ${error.urgency}`);
        console.log(`   📝 Message: ${error.formattedMessage}`);

        if (error.context) {
            console.log(`   💡 Context: ${error.context}`);
        }

        if (error.impact) {
            console.log(`   ⚡ Impact: ${error.impact}`);
        }

        if (error.consolidated) {
            console.log(`   🔄 Occurrences: ${error.occurrences}`);
        }

        // Display immediate resolution steps
        if (error.resolutions && error.resolutions.immediate) {
            console.log('   🔧 Immediate Actions:');
            error.resolutions.immediate.forEach(action => {
                console.log(`      • ${action}`);
            });
        }

        if (this.options.verbose && error.resolutions && error.resolutions.detailed) {
            console.log('   📋 Detailed Steps:');
            error.resolutions.detailed.forEach(step => {
                console.log(`      ${step}`);
            });
        }
    }

    /**
     * Display resolution priorities
     */
    displayResolutionPriorities(errors) {
        const immediateActions = errors.filter(e => e.urgency === 'IMMEDIATE');
        const highPriority = errors.filter(e => e.urgency === 'HIGH');

        if (immediateActions.length > 0) {
            console.log('\n🚨 IMMEDIATE ACTION REQUIRED:');
            immediateActions.forEach((error, index) => {
                console.log(`${index + 1}. ${error.title}`);
                if (error.resolutions && error.resolutions.immediate) {
                    error.resolutions.immediate.forEach(action => {
                        console.log(`   • ${action}`);
                    });
                }
            });
        }

        if (highPriority.length > 0) {
            console.log('\n⚡ HIGH PRIORITY:');
            highPriority.forEach((error, index) => {
                console.log(`${index + 1}. ${error.title}`);
            });
        }
    }

    /**
     * Get severity icon
     */
    getSeverityIcon(severity) {
        const icons = {
            'CRITICAL': '🔴',
            'ERROR': '🟠',
            'WARNING': '🟡',
            'INFO': '🔵'
        };
        return icons[severity] || '⚪';
    }

    /**
     * Get category icon
     */
    getCategoryIcon(category) {
        const icons = {
            'CONFIGURATION': '⚙️',
            'DEPENDENCY': '📦',
            'BUILD': '🔨',
            'DEPLOYMENT': '🚀',
            'SYSTEM': '🖥️',
            'VALIDATION': '✅'
        };
        return icons[category] || '❓';
    }

    /**
     * Get urgency icon
     */
    getUrgencyIcon(urgency) {
        const icons = {
            'IMMEDIATE': '🚨',
            'HIGH': '⚡',
            'MEDIUM': '⏰',
            'LOW': '📅'
        };
        return icons[urgency] || '⏰';
    }

    /**
     * Save classification report
     */
    saveClassificationReport(processedResult) {
        const reportPath = 'error-classification-report.json';
        const report = {
            timestamp: new Date().toISOString(),
            summary: processedResult.summary,
            errors: processedResult.consolidated,
            metadata: {
                totalRules: Array.from(this.classifications.values()).reduce((sum, rules) => sum + rules.length, 0),
                totalTemplates: this.messageTemplates.size,
                totalStrategies: this.resolutionStrategies.size
            }
        };

        try {
            fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
            console.log(`\n📄 Classification report saved to: ${reportPath}`);
        } catch (error) {
            console.warn(`Failed to write classification report: ${error.message}`);
        }
    }
}

// Export for use as module
module.exports = ErrorClassifier;

// Run if called directly
if (require.main === module) {
    // Example usage with sample errors
    const classifier = new ErrorClassifier({ verbose: true });

    const sampleErrors = [
        {
            title: 'Configuration Error',
            message: 'amplify.yml syntax error: invalid YAML indentation',
            severity: 'CRITICAL',
            details: { file: 'amplify.yml' }
        },
        {
            title: 'Dependency Error',
            message: 'Node.js not found in PATH',
            severity: 'CRITICAL',
            details: {}
        },
        {
            title: 'Build Error',
            message: 'Compilation failed: missing import statement',
            severity: 'ERROR',
            details: { component: 'backend' }
        }
    ];

    const result = classifier.processErrors(sampleErrors);
    classifier.displayErrors(result);
    classifier.saveClassificationReport(result);
}