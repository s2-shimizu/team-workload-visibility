#!/usr/bin/env node

/**
 * Error Handling and Logging Orchestrator
 * 
 * Master controller that integrates all error handling components:
 * - Build process monitoring and error detection
 * - Configuration file syntax checking
 * - Dependency problem detection and resolution suggestions
 * - Error classification and appropriate message display
 * 
 * Requirements: 1.1, 1.2, 1.3
 */

const fs = require('fs');
const path = require('path');
const ErrorHandler = require('./error-handler');
const BuildProcessMonitor = require('./build-process-monitor');
const ConfigSyntaxChecker = require('./config-syntax-checker');
const DependencyResolver = require('./dependency-resolver');
const ErrorClassifier = require('./error-classifier');

class ErrorHandlingOrchestrator {
    constructor(options = {}) {
        this.options = {
            verbose: options.verbose || false,
            skipBuildMonitoring: options.skipBuildMonitoring || false,
            skipConfigCheck: options.skipConfigCheck || false,
            skipDependencyCheck: options.skipDependencyCheck || false,
            outputDir: options.outputDir || '.',
            logLevel: options.logLevel || 'INFO',
            ...options
        };
        
        // Initialize components
        this.errorHandler = new ErrorHandler();
        this.buildMonitor = new BuildProcessMonitor({ logLevel: this.options.logLevel });
        this.configChecker = new ConfigSyntaxChecker({ verbose: this.options.verbose });
        this.dependencyResolver = new DependencyResolver({ verbose: this.options.verbose });
        this.errorClassifier = new ErrorClassifier({ verbose: this.options.verbose });
        
        // Collected results
        this.results = {
            errorHandler: null,
            buildMonitor: null,
            configChecker: null,
            dependencyResolver: null,
            allErrors: [],
            classifiedErrors: null,
            summary: null
        };
        
        this.startTime = Date.now();
    }

    /**
     * Run comprehensive error handling and logging
     */
    async runComprehensiveErrorHandling() {
        console.log('🚀 Starting comprehensive error handling and logging system...');
        console.log(`Log Level: ${this.options.logLevel}`);
        console.log(`Output Directory: ${this.options.outputDir}`);
        
        try {
            // Phase 1: Basic error detection and configuration validation
            await this.runPhase1();
            
            // Phase 2: Dependency analysis
            await this.runPhase2();
            
            // Phase 3: Build process monitoring (if requested)
            await this.runPhase3();
            
            // Phase 4: Error classification and reporting
            await this.runPhase4();
            
            // Phase 5: Generate comprehensive report
            await this.runPhase5();
            
            const duration = Date.now() - this.startTime;
            const success = this.results.summary.criticalIssues === 0;
            
            if (success) {
                console.log(`\n✅ Comprehensive error handling completed successfully in ${this.formatDuration(duration)}`);
                return true;
            } else {
                console.log(`\n❌ Critical issues found during error handling (${this.formatDuration(duration)})`);
                return false;
            }
            
        } catch (error) {
            console.error('\n💥 Error handling orchestration failed:', error.message);
            await this.generateFailureReport(error);
            throw error;
        }
    }

    /**
     * Phase 1: Basic error detection and configuration validation
     */
    async runPhase1() {
        console.log('\n📋 Phase 1: Basic Error Detection and Configuration Validation');
        console.log('================================================================');
        
        try {
            // Run basic error handler
            console.log('🔍 Running basic error detection...');
            this.results.errorHandler = await this.errorHandler.detectAndHandleErrors();
            this.collectErrors('errorHandler', this.results.errorHandler.errors);
            
            // Run configuration syntax checking
            if (!this.options.skipConfigCheck) {
                console.log('⚙️  Running configuration syntax checking...');
                this.results.configChecker = await this.configChecker.checkAllConfigurations();
                this.collectErrors('configChecker', this.configChecker.errors);
            } else {
                console.log('⏭️  Skipping configuration syntax checking');
            }
            
            console.log('✅ Phase 1 completed');
            
        } catch (error) {
            console.error('❌ Phase 1 failed:', error.message);
            throw error;
        }
    }

    /**
     * Phase 2: Dependency analysis
     */
    async runPhase2() {
        console.log('\n📦 Phase 2: Dependency Analysis');
        console.log('================================');
        
        try {
            if (!this.options.skipDependencyCheck) {
                console.log('🔍 Running dependency analysis...');
                this.results.dependencyResolver = await this.dependencyResolver.analyzeDependencies();
                this.collectErrors('dependencyResolver', this.dependencyResolver.issues);
            } else {
                console.log('⏭️  Skipping dependency analysis');
            }
            
            console.log('✅ Phase 2 completed');
            
        } catch (error) {
            console.error('❌ Phase 2 failed:', error.message);
            throw error;
        }
    }

    /**
     * Phase 3: Build process monitoring
     */
    async runPhase3() {
        console.log('\n🔨 Phase 3: Build Process Monitoring');
        console.log('====================================');
        
        try {
            if (!this.options.skipBuildMonitoring) {
                console.log('🔍 Running build process monitoring...');
                this.results.buildMonitor = await this.buildMonitor.monitorBuildProcess();
                this.collectErrors('buildMonitor', this.buildMonitor.buildErrors);
            } else {
                console.log('⏭️  Skipping build process monitoring');
            }
            
            console.log('✅ Phase 3 completed');
            
        } catch (error) {
            console.error('❌ Phase 3 failed:', error.message);
            // Don't throw here - build failures are expected to be handled
            this.collectErrors('buildMonitor', [{ 
                category: 'BUILD', 
                severity: 'ERROR', 
                title: 'Build monitoring failed', 
                message: error.message 
            }]);
        }
    }

    /**
     * Phase 4: Error classification and reporting
     */
    async runPhase4() {
        console.log('\n🏷️  Phase 4: Error Classification and Reporting');
        console.log('===============================================');
        
        try {
            console.log('🔍 Classifying and formatting errors...');
            this.results.classifiedErrors = this.errorClassifier.processErrors(this.results.allErrors);
            
            // Display classified errors
            this.errorClassifier.displayErrors(this.results.classifiedErrors);
            
            console.log('✅ Phase 4 completed');
            
        } catch (error) {
            console.error('❌ Phase 4 failed:', error.message);
            throw error;
        }
    }

    /**
     * Phase 5: Generate comprehensive report
     */
    async runPhase5() {
        console.log('\n📊 Phase 5: Comprehensive Report Generation');
        console.log('===========================================');
        
        try {
            console.log('📄 Generating comprehensive report...');
            
            // Generate summary
            this.results.summary = this.generateComprehensiveSummary();
            
            // Save all reports
            await this.saveComprehensiveReports();
            
            // Display final summary
            this.displayFinalSummary();
            
            console.log('✅ Phase 5 completed');
            
        } catch (error) {
            console.error('❌ Phase 5 failed:', error.message);
            throw error;
        }
    }

    /**
     * Collect errors from different components
     */
    collectErrors(source, errors) {
        if (!errors || !Array.isArray(errors)) return;
        
        for (const error of errors) {
            this.results.allErrors.push({
                ...error,
                source,
                timestamp: error.timestamp || new Date().toISOString()
            });
        }
        
        this.log(`📥 Collected ${errors.length} errors from ${source}`);
    }

    /**
     * Generate comprehensive summary
     */
    generateComprehensiveSummary() {
        const summary = {
            timestamp: new Date().toISOString(),
            duration: Date.now() - this.startTime,
            phases: {
                errorHandler: this.results.errorHandler !== null,
                configChecker: this.results.configChecker !== null,
                dependencyResolver: this.results.dependencyResolver !== null,
                buildMonitor: this.results.buildMonitor !== null
            },
            totalErrors: this.results.allErrors.length,
            errorsBySource: {},
            errorsBySeverity: {},
            errorsByCategory: {},
            criticalIssues: 0,
            actionableItems: 0,
            recommendations: []
        };
        
        // Count errors by source
        for (const error of this.results.allErrors) {
            const source = error.source || 'unknown';
            summary.errorsBySource[source] = (summary.errorsBySource[source] || 0) + 1;
            
            const severity = error.severity || 'UNKNOWN';
            summary.errorsBySeverity[severity] = (summary.errorsBySeverity[severity] || 0) + 1;
            
            const category = error.category || 'UNKNOWN';
            summary.errorsByCategory[category] = (summary.errorsByCategory[category] || 0) + 1;
            
            if (severity === 'CRITICAL') summary.criticalIssues++;
        }
        
        // Add classified error summary if available
        if (this.results.classifiedErrors) {
            summary.classifiedSummary = this.results.classifiedErrors.summary;
            summary.actionableItems = this.results.classifiedErrors.summary.actionRequired || 0;
        }
        
        // Generate high-level recommendations
        summary.recommendations = this.generateHighLevelRecommendations(summary);
        
        return summary;
    }

    /**
     * Generate high-level recommendations
     */
    generateHighLevelRecommendations(summary) {
        const recommendations = [];
        
        // Critical issues
        if (summary.criticalIssues > 0) {
            recommendations.push({
                priority: 'CRITICAL',
                title: 'Resolve Critical Issues',
                description: `${summary.criticalIssues} critical issues must be resolved before deployment`,
                action: 'Review and fix all critical errors listed in the detailed report'
            });
        }
        
        // Configuration issues
        if (summary.errorsByCategory.CONFIGURATION > 0) {
            recommendations.push({
                priority: 'HIGH',
                title: 'Fix Configuration Issues',
                description: `${summary.errorsByCategory.CONFIGURATION} configuration issues found`,
                action: 'Validate and correct all configuration files'
            });
        }
        
        // Dependency issues
        if (summary.errorsByCategory.DEPENDENCY > 0) {
            recommendations.push({
                priority: 'HIGH',
                title: 'Resolve Dependencies',
                description: `${summary.errorsByCategory.DEPENDENCY} dependency issues found`,
                action: 'Install missing dependencies and resolve version conflicts'
            });
        }
        
        // Build issues
        if (summary.errorsByCategory.BUILD > 0) {
            recommendations.push({
                priority: 'MEDIUM',
                title: 'Fix Build Issues',
                description: `${summary.errorsByCategory.BUILD} build issues found`,
                action: 'Review build configuration and resolve compilation errors'
            });
        }
        
        // Success case
        if (summary.totalErrors === 0) {
            recommendations.push({
                priority: 'INFO',
                title: 'System Ready',
                description: 'No critical issues found',
                action: 'Proceed with deployment'
            });
        }
        
        return recommendations;
    }

    /**
     * Save comprehensive reports
     */
    async saveComprehensiveReports() {
        const reportDir = path.join(this.options.outputDir, 'error-reports');
        
        // Create reports directory
        if (!fs.existsSync(reportDir)) {
            fs.mkdirSync(reportDir, { recursive: true });
        }
        
        // Master report
        const masterReport = {
            timestamp: new Date().toISOString(),
            summary: this.results.summary,
            allErrors: this.results.allErrors,
            classifiedErrors: this.results.classifiedErrors,
            componentResults: {
                errorHandler: this.results.errorHandler,
                configChecker: this.results.configChecker,
                dependencyResolver: this.results.dependencyResolver,
                buildMonitor: this.results.buildMonitor
            }
        };
        
        const masterReportPath = path.join(reportDir, 'master-error-report.json');
        fs.writeFileSync(masterReportPath, JSON.stringify(masterReport, null, 2));
        console.log(`📄 Master report saved: ${masterReportPath}`);
        
        // Summary report (human-readable)
        const summaryReportPath = path.join(reportDir, 'error-summary.md');
        const summaryContent = this.generateMarkdownSummary();
        fs.writeFileSync(summaryReportPath, summaryContent);
        console.log(`📄 Summary report saved: ${summaryReportPath}`);
        
        // Save individual component reports
        if (this.results.classifiedErrors) {
            this.errorClassifier.saveClassificationReport(this.results.classifiedErrors);
        }
    }

    /**
     * Generate markdown summary
     */
    generateMarkdownSummary() {
        const summary = this.results.summary;
        const duration = this.formatDuration(summary.duration);
        
        let markdown = `# Error Handling Summary Report\n\n`;
        markdown += `**Generated:** ${summary.timestamp}\n`;
        markdown += `**Duration:** ${duration}\n`;
        markdown += `**Total Errors:** ${summary.totalErrors}\n`;
        markdown += `**Critical Issues:** ${summary.criticalIssues}\n\n`;
        
        // Phases executed
        markdown += `## Phases Executed\n\n`;
        for (const [phase, executed] of Object.entries(summary.phases)) {
            const status = executed ? '✅' : '⏭️';
            markdown += `- ${status} ${phase}\n`;
        }
        
        // Error breakdown
        if (summary.totalErrors > 0) {
            markdown += `\n## Error Breakdown\n\n`;
            
            markdown += `### By Severity\n`;
            for (const [severity, count] of Object.entries(summary.errorsBySeverity)) {
                markdown += `- **${severity}:** ${count}\n`;
            }
            
            markdown += `\n### By Category\n`;
            for (const [category, count] of Object.entries(summary.errorsByCategory)) {
                markdown += `- **${category}:** ${count}\n`;
            }
            
            markdown += `\n### By Source\n`;
            for (const [source, count] of Object.entries(summary.errorsBySource)) {
                markdown += `- **${source}:** ${count}\n`;
            }
        }
        
        // Recommendations
        if (summary.recommendations.length > 0) {
            markdown += `\n## Recommendations\n\n`;
            for (const rec of summary.recommendations) {
                markdown += `### ${rec.title} (${rec.priority})\n`;
                markdown += `${rec.description}\n\n`;
                markdown += `**Action:** ${rec.action}\n\n`;
            }
        }
        
        // Next steps
        markdown += `\n## Next Steps\n\n`;
        if (summary.criticalIssues > 0) {
            markdown += `1. **IMMEDIATE:** Resolve ${summary.criticalIssues} critical issues\n`;
            markdown += `2. Review detailed error reports in the error-reports directory\n`;
            markdown += `3. Follow resolution suggestions for each error\n`;
            markdown += `4. Re-run error handling after fixes\n`;
        } else {
            markdown += `1. Review any warnings or informational messages\n`;
            markdown += `2. Consider implementing preventive measures\n`;
            markdown += `3. Proceed with deployment\n`;
        }
        
        return markdown;
    }

    /**
     * Display final summary
     */
    displayFinalSummary() {
        const summary = this.results.summary;
        
        console.log('\n🎯 FINAL SUMMARY');
        console.log('================');
        console.log(`Duration: ${this.formatDuration(summary.duration)}`);
        console.log(`Total Errors: ${summary.totalErrors}`);
        console.log(`Critical Issues: ${summary.criticalIssues}`);
        console.log(`Actionable Items: ${summary.actionableItems}`);
        
        // Status indicator
        if (summary.criticalIssues === 0) {
            console.log('\n🟢 STATUS: READY FOR DEPLOYMENT');
        } else {
            console.log('\n🔴 STATUS: CRITICAL ISSUES REQUIRE ATTENTION');
        }
        
        // Top recommendations
        if (summary.recommendations.length > 0) {
            console.log('\n🎯 TOP RECOMMENDATIONS:');
            summary.recommendations.slice(0, 3).forEach((rec, index) => {
                console.log(`${index + 1}. [${rec.priority}] ${rec.title}`);
                console.log(`   ${rec.description}`);
            });
        }
        
        console.log('\n📁 Reports saved in: error-reports/');
        console.log('   - master-error-report.json (detailed data)');
        console.log('   - error-summary.md (human-readable)');
        console.log('   - Additional component reports');
    }

    /**
     * Generate failure report
     */
    async generateFailureReport(error) {
        const failureReport = {
            timestamp: new Date().toISOString(),
            duration: Date.now() - this.startTime,
            failureReason: error.message,
            stackTrace: error.stack,
            partialResults: this.results,
            recommendations: [
                'Check system requirements and dependencies',
                'Verify file permissions and access rights',
                'Review error logs for specific issues',
                'Contact support if the issue persists'
            ]
        };
        
        try {
            const reportPath = path.join(this.options.outputDir, 'error-handling-failure.json');
            fs.writeFileSync(reportPath, JSON.stringify(failureReport, null, 2));
            console.log(`💥 Failure report saved: ${reportPath}`);
        } catch (saveError) {
            console.error('Failed to save failure report:', saveError.message);
        }
    }

    /**
     * Format duration for display
     */
    formatDuration(ms) {
        const seconds = Math.floor(ms / 1000);
        const minutes = Math.floor(seconds / 60);
        
        if (minutes > 0) {
            return `${minutes}m ${seconds % 60}s`;
        } else {
            return `${seconds}s`;
        }
    }

    /**
     * Log message
     */
    log(message) {
        if (this.options.verbose) {
            console.log(message);
        }
    }
}

// Export for use as module
module.exports = ErrorHandlingOrchestrator;

// Run if called directly
if (require.main === module) {
    const args = process.argv.slice(2);
    const options = {
        verbose: args.includes('--verbose') || args.includes('-v'),
        skipBuildMonitoring: args.includes('--skip-build'),
        skipConfigCheck: args.includes('--skip-config'),
        skipDependencyCheck: args.includes('--skip-deps'),
        logLevel: args.includes('--debug') ? 'DEBUG' : 'INFO'
    };
    
    // Parse output directory
    const outputDirIndex = args.indexOf('--output-dir');
    if (outputDirIndex !== -1 && args[outputDirIndex + 1]) {
        options.outputDir = args[outputDirIndex + 1];
    }
    
    const orchestrator = new ErrorHandlingOrchestrator(options);
    
    orchestrator.runComprehensiveErrorHandling()
        .then(success => {
            if (success) {
                console.log('\n✅ Error handling orchestration completed successfully');
                process.exit(0);
            } else {
                console.log('\n❌ Critical issues found - review reports and resolve before deployment');
                process.exit(1);
            }
        })
        .catch(error => {
            console.error('\n💥 Error handling orchestration failed:', error.message);
            process.exit(1);
        });
}