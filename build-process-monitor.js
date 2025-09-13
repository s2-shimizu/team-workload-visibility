#!/usr/bin/env node

/**
 * Build Process Monitoring and Error Detection
 * 
 * Features:
 * - Real-time build process monitoring
 * - Detailed error detection and classification
 * - Build step validation and logging
 * - Integration with error handling system
 * 
 * Requirements: 1.1, 1.2, 1.3
 */

const fs = require('fs');
const path = require('path');
const { spawn, exec } = require('child_process');
const util = require('util');
const ErrorHandler = require('./error-handler');

const execAsync = util.promisify(exec);

class BuildProcessMonitor {
    constructor(options = {}) {
        this.errorHandler = new ErrorHandler();
        this.buildSteps = [];
        this.currentStep = null;
        this.buildStartTime = null;
        this.buildEndTime = null;
        this.buildLogs = [];
        this.buildErrors = [];
        this.buildWarnings = [];
        
        // Configuration
        this.config = {
            logLevel: options.logLevel || 'INFO',
            outputFile: options.outputFile || 'build-process.log',
            maxLogSize: options.maxLogSize || 10 * 1024 * 1024, // 10MB
            timeout: options.timeout || 30 * 60 * 1000, // 30 minutes
            ...options
        };
        
        this.initializeMonitoring();
    }

    /**
     * Initialize monitoring system
     */
    initializeMonitoring() {
        this.log('INFO', 'MONITOR', 'Build process monitoring initialized');
        
        // Create log file
        if (!fs.existsSync(this.config.outputFile)) {
            fs.writeFileSync(this.config.outputFile, '');
        }
        
        // Set up process handlers
        process.on('SIGINT', () => this.handleProcessExit('SIGINT'));
        process.on('SIGTERM', () => this.handleProcessExit('SIGTERM'));
        process.on('uncaughtException', (error) => this.handleUncaughtException(error));
    }

    /**
     * Monitor complete build process
     */
    async monitorBuildProcess() {
        this.log('INFO', 'MONITOR', 'Starting complete build process monitoring...');
        this.buildStartTime = Date.now();
        
        try {
            // Step 1: Pre-build validation
            await this.monitorStep('pre-build-validation', 'Pre-build Validation', async () => {
                await this.validatePreBuildRequirements();
            });
            
            // Step 2: Frontend build
            await this.monitorStep('frontend-build', 'Frontend Build', async () => {
                await this.monitorFrontendBuild();
            });
            
            // Step 3: Backend build
            await this.monitorStep('backend-build', 'Backend Build', async () => {
                await this.monitorBackendBuild();
            });
            
            // Step 4: Post-build validation
            await this.monitorStep('post-build-validation', 'Post-build Validation', async () => {
                await this.validatePostBuildArtifacts();
            });
            
            this.buildEndTime = Date.now();
            this.generateBuildReport();
            
            if (this.buildErrors.length === 0) {
                this.log('INFO', 'MONITOR', 'Build process completed successfully');
                return true;
            } else {
                this.log('ERROR', 'MONITOR', `Build process failed with ${this.buildErrors.length} errors`);
                return false;
            }
            
        } catch (error) {
            this.buildEndTime = Date.now();
            this.addBuildError('MONITOR', 'Build process monitoring failed', error.message);
            this.generateBuildReport();
            throw error;
        }
    }

    /**
     * Monitor individual build step
     */
    async monitorStep(stepId, stepName, stepFunction) {
        const step = {
            id: stepId,
            name: stepName,
            startTime: Date.now(),
            endTime: null,
            status: 'RUNNING',
            errors: [],
            warnings: [],
            logs: []
        };
        
        this.buildSteps.push(step);
        this.currentStep = step;
        
        this.log('INFO', 'STEP', `Starting step: ${stepName}`);
        
        try {
            await stepFunction();
            step.status = 'SUCCESS';
            step.endTime = Date.now();
            this.log('INFO', 'STEP', `Completed step: ${stepName} (${step.endTime - step.startTime}ms)`);
        } catch (error) {
            step.status = 'FAILED';
            step.endTime = Date.now();
            step.errors.push(error.message);
            this.addBuildError('STEP', `Step failed: ${stepName}`, error.message);
            this.log('ERROR', 'STEP', `Failed step: ${stepName} - ${error.message}`);
            throw error;
        } finally {
            this.currentStep = null;
        }
    }

    /**
     * Validate pre-build requirements
     */
    async validatePreBuildRequirements() {
        this.log('INFO', 'VALIDATION', 'Validating pre-build requirements...');
        
        // Check required files
        const requiredFiles = [
            'amplify.yml',
            'frontend/index.html',
            'frontend/css/style.css',
            'frontend/js/app.js',
            'frontend/js/api-client.js',
            'backend/pom.xml'
        ];
        
        for (const file of requiredFiles) {
            if (!fs.existsSync(file)) {
                throw new Error(`Required file missing: ${file}`);
            }
        }
        
        // Run error handler validation
        const result = await this.errorHandler.detectAndHandleErrors();
        if (!result.success) {
            const criticalErrors = result.errors.filter(e => e.severity === 'CRITICAL');
            if (criticalErrors.length > 0) {
                throw new Error(`Critical configuration errors found: ${criticalErrors.length}`);
            }
        }
        
        this.log('INFO', 'VALIDATION', 'Pre-build requirements validated successfully');
    }

    /**
     * Monitor frontend build process
     */
    async monitorFrontendBuild() {
        this.log('INFO', 'BUILD', 'Starting frontend build monitoring...');
        
        const frontendDir = 'frontend';
        
        // Check if build script exists
        const buildScriptPath = path.join(frontendDir, 'build-script.js');
        if (fs.existsSync(buildScriptPath)) {
            await this.executeAndMonitor('node build-script.js', { cwd: frontendDir });
        } else {
            // Fallback to manual build process
            await this.executeManualFrontendBuild();
        }
        
        // Validate build output
        await this.validateFrontendBuildOutput();
        
        this.log('INFO', 'BUILD', 'Frontend build monitoring completed');
    }

    /**
     * Execute manual frontend build
     */
    async executeManualFrontendBuild() {
        this.log('INFO', 'BUILD', 'Executing manual frontend build...');
        
        const buildDir = path.join('frontend', 'build');
        
        // Clean build directory
        if (fs.existsSync(buildDir)) {
            fs.rmSync(buildDir, { recursive: true, force: true });
        }
        fs.mkdirSync(buildDir, { recursive: true });
        
        // Copy files
        const filesToCopy = [
            'index.html',
            'css/style.css',
            'js/app.js',
            'js/api-client.js',
            'package.json'
        ];
        
        for (const file of filesToCopy) {
            const srcPath = path.join('frontend', file);
            const destPath = path.join(buildDir, file);
            
            if (fs.existsSync(srcPath)) {
                // Ensure destination directory exists
                const destDir = path.dirname(destPath);
                fs.mkdirSync(destDir, { recursive: true });
                
                fs.copyFileSync(srcPath, destPath);
                this.log('INFO', 'BUILD', `Copied: ${file}`);
            } else {
                this.addBuildWarning('BUILD', `Optional file not found: ${file}`);
            }
        }
    }

    /**
     * Validate frontend build output
     */
    async validateFrontendBuildOutput() {
        this.log('INFO', 'VALIDATION', 'Validating frontend build output...');
        
        const buildDir = path.join('frontend', 'build');
        if (!fs.existsSync(buildDir)) {
            throw new Error('Frontend build directory not found');
        }
        
        const requiredFiles = [
            'index.html',
            'css/style.css',
            'js/app.js',
            'js/api-client.js'
        ];
        
        for (const file of requiredFiles) {
            const filePath = path.join(buildDir, file);
            if (!fs.existsSync(filePath)) {
                throw new Error(`Required build artifact missing: ${file}`);
            }
            
            // Check file is not empty
            const stats = fs.statSync(filePath);
            if (stats.size === 0) {
                throw new Error(`Build artifact is empty: ${file}`);
            }
        }
        
        this.log('INFO', 'VALIDATION', 'Frontend build output validated successfully');
    }

    /**
     * Monitor backend build process
     */
    async monitorBackendBuild() {
        this.log('INFO', 'BUILD', 'Starting backend build monitoring...');
        
        const backendDir = 'backend';
        
        // Check Maven wrapper
        const mvnwPath = path.join(backendDir, process.platform === 'win32' ? 'mvnw.cmd' : 'mvnw');
        const mvnCommand = fs.existsSync(mvnwPath) ? './mvnw' : 'mvn';
        
        // Clean previous builds
        await this.executeAndMonitor(`${mvnCommand} clean`, { cwd: backendDir });
        
        // Compile and package
        await this.executeAndMonitor(`${mvnCommand} package -DskipTests`, { cwd: backendDir });
        
        // Validate build output
        await this.validateBackendBuildOutput();
        
        this.log('INFO', 'BUILD', 'Backend build monitoring completed');
    }

    /**
     * Validate backend build output
     */
    async validateBackendBuildOutput() {
        this.log('INFO', 'VALIDATION', 'Validating backend build output...');
        
        const targetDir = path.join('backend', 'target');
        if (!fs.existsSync(targetDir)) {
            throw new Error('Backend target directory not found');
        }
        
        // Check for JAR files
        const jarFiles = fs.readdirSync(targetDir).filter(file => file.endsWith('.jar'));
        if (jarFiles.length === 0) {
            throw new Error('No JAR files found in target directory');
        }
        
        // Find the main JAR file (not original or sources)
        const mainJar = jarFiles.find(jar => 
            !jar.includes('original-') && 
            !jar.includes('-sources.jar') && 
            !jar.includes('-javadoc.jar')
        );
        
        if (!mainJar) {
            throw new Error('Main JAR file not found');
        }
        
        // Check JAR file size
        const jarPath = path.join(targetDir, mainJar);
        const stats = fs.statSync(jarPath);
        if (stats.size < 1024) { // Less than 1KB
            throw new Error(`JAR file appears to be too small: ${stats.size} bytes`);
        }
        
        this.log('INFO', 'VALIDATION', `Backend build output validated: ${mainJar} (${this.formatBytes(stats.size)})`);
    }

    /**
     * Validate post-build artifacts
     */
    async validatePostBuildArtifacts() {
        this.log('INFO', 'VALIDATION', 'Validating post-build artifacts...');
        
        // Check frontend artifacts
        const frontendBuildDir = path.join('frontend', 'build');
        if (fs.existsSync(frontendBuildDir)) {
            const frontendSize = this.calculateDirectorySize(frontendBuildDir);
            this.log('INFO', 'VALIDATION', `Frontend build size: ${this.formatBytes(frontendSize)}`);
        }
        
        // Check backend artifacts
        const backendTargetDir = path.join('backend', 'target');
        if (fs.existsSync(backendTargetDir)) {
            const backendSize = this.calculateDirectorySize(backendTargetDir);
            this.log('INFO', 'VALIDATION', `Backend build size: ${this.formatBytes(backendSize)}`);
        }
        
        // Validate amplify.yml references
        await this.validateAmplifyArtifactReferences();
        
        this.log('INFO', 'VALIDATION', 'Post-build artifacts validated successfully');
    }

    /**
     * Validate amplify.yml artifact references
     */
    async validateAmplifyArtifactReferences() {
        const amplifyPath = 'amplify.yml';
        if (!fs.existsSync(amplifyPath)) {
            return;
        }
        
        const content = fs.readFileSync(amplifyPath, 'utf8');
        
        // Check frontend artifacts
        if (content.includes('baseDirectory: frontend')) {
            const frontendBuildDir = path.join('frontend', 'build');
            if (!fs.existsSync(frontendBuildDir)) {
                this.addBuildWarning('VALIDATION', 'Frontend build directory not found for amplify.yml reference');
            }
        }
        
        // Check backend artifacts
        if (content.includes('baseDirectory: backend/target')) {
            const backendTargetDir = path.join('backend', 'target');
            if (!fs.existsSync(backendTargetDir)) {
                this.addBuildWarning('VALIDATION', 'Backend target directory not found for amplify.yml reference');
            }
        }
    }

    /**
     * Execute command and monitor output
     */
    async executeAndMonitor(command, options = {}) {
        this.log('INFO', 'EXEC', `Executing: ${command}`);
        
        return new Promise((resolve, reject) => {
            const child = spawn(command, [], {
                shell: true,
                stdio: ['pipe', 'pipe', 'pipe'],
                ...options
            });
            
            let stdout = '';
            let stderr = '';
            
            child.stdout.on('data', (data) => {
                const output = data.toString();
                stdout += output;
                this.logBuildOutput('STDOUT', output);
            });
            
            child.stderr.on('data', (data) => {
                const output = data.toString();
                stderr += output;
                this.logBuildOutput('STDERR', output);
                
                // Check for common error patterns
                this.detectErrorPatterns(output);
            });
            
            child.on('close', (code) => {
                if (code === 0) {
                    this.log('INFO', 'EXEC', `Command completed successfully: ${command}`);
                    resolve({ stdout, stderr, code });
                } else {
                    const error = new Error(`Command failed with code ${code}: ${command}`);
                    error.stdout = stdout;
                    error.stderr = stderr;
                    error.code = code;
                    reject(error);
                }
            });
            
            child.on('error', (error) => {
                this.log('ERROR', 'EXEC', `Command execution error: ${error.message}`);
                reject(error);
            });
            
            // Set timeout
            setTimeout(() => {
                child.kill('SIGTERM');
                reject(new Error(`Command timeout after ${this.config.timeout}ms: ${command}`));
            }, this.config.timeout);
        });
    }

    /**
     * Detect error patterns in build output
     */
    detectErrorPatterns(output) {
        const errorPatterns = [
            { pattern: /ERROR/i, category: 'BUILD', severity: 'ERROR' },
            { pattern: /FAILED/i, category: 'BUILD', severity: 'ERROR' },
            { pattern: /Exception/i, category: 'BUILD', severity: 'ERROR' },
            { pattern: /Cannot find/i, category: 'DEPENDENCY', severity: 'ERROR' },
            { pattern: /No such file/i, category: 'BUILD', severity: 'ERROR' },
            { pattern: /Permission denied/i, category: 'SYSTEM', severity: 'ERROR' },
            { pattern: /WARN/i, category: 'BUILD', severity: 'WARNING' },
            { pattern: /deprecated/i, category: 'BUILD', severity: 'WARNING' }
        ];
        
        for (const { pattern, category, severity } of errorPatterns) {
            if (pattern.test(output)) {
                const message = output.trim().split('\n')[0]; // First line
                if (severity === 'ERROR') {
                    this.addBuildError(category, 'Build output error detected', message);
                } else {
                    this.addBuildWarning(category, message);
                }
            }
        }
    }

    /**
     * Log build output
     */
    logBuildOutput(type, output) {
        const lines = output.split('\n').filter(line => line.trim());
        for (const line of lines) {
            this.buildLogs.push({
                timestamp: new Date().toISOString(),
                type,
                message: line.trim()
            });
            
            // Also log to main log
            this.log('DEBUG', 'BUILD', `[${type}] ${line.trim()}`);
        }
    }

    /**
     * Add build error
     */
    addBuildError(category, title, message) {
        const error = {
            category,
            title,
            message,
            timestamp: new Date().toISOString(),
            step: this.currentStep ? this.currentStep.id : 'unknown'
        };
        
        this.buildErrors.push(error);
        
        if (this.currentStep) {
            this.currentStep.errors.push(error);
        }
    }

    /**
     * Add build warning
     */
    addBuildWarning(category, message) {
        const warning = {
            category,
            message,
            timestamp: new Date().toISOString(),
            step: this.currentStep ? this.currentStep.id : 'unknown'
        };
        
        this.buildWarnings.push(warning);
        
        if (this.currentStep) {
            this.currentStep.warnings.push(warning);
        }
    }

    /**
     * Generate build report
     */
    generateBuildReport() {
        const duration = this.buildEndTime - this.buildStartTime;
        
        console.log('\n📊 Build Process Report');
        console.log('========================');
        console.log(`Total Duration: ${this.formatDuration(duration)}`);
        console.log(`Steps Completed: ${this.buildSteps.length}`);
        console.log(`Errors: ${this.buildErrors.length}`);
        console.log(`Warnings: ${this.buildWarnings.length}`);
        
        // Step details
        console.log('\n📋 Build Steps:');
        this.buildSteps.forEach((step, index) => {
            const stepDuration = step.endTime - step.startTime;
            const status = step.status === 'SUCCESS' ? '✅' : '❌';
            console.log(`  ${index + 1}. ${status} ${step.name} (${this.formatDuration(stepDuration)})`);
            
            if (step.errors.length > 0) {
                step.errors.forEach(error => {
                    console.log(`     ❌ ${error}`);
                });
            }
        });
        
        // Error details
        if (this.buildErrors.length > 0) {
            console.log('\n❌ Build Errors:');
            this.buildErrors.forEach((error, index) => {
                console.log(`  ${index + 1}. [${error.category}] ${error.title}`);
                console.log(`     ${error.message}`);
                console.log(`     Step: ${error.step}`);
            });
        }
        
        // Warning details
        if (this.buildWarnings.length > 0) {
            console.log('\n⚠️  Build Warnings:');
            this.buildWarnings.slice(0, 5).forEach((warning, index) => {
                console.log(`  ${index + 1}. [${warning.category}] ${warning.message}`);
            });
            
            if (this.buildWarnings.length > 5) {
                console.log(`     ... and ${this.buildWarnings.length - 5} more warnings`);
            }
        }
        
        // Save detailed report
        this.saveBuildReport();
    }

    /**
     * Save build report to file
     */
    saveBuildReport() {
        const reportPath = 'build-process-report.json';
        const report = {
            timestamp: new Date().toISOString(),
            duration: this.buildEndTime - this.buildStartTime,
            success: this.buildErrors.length === 0,
            steps: this.buildSteps,
            errors: this.buildErrors,
            warnings: this.buildWarnings,
            logs: this.buildLogs.slice(-100) // Last 100 log entries
        };
        
        try {
            fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
            console.log(`\n📄 Build report saved to: ${reportPath}`);
        } catch (error) {
            console.warn(`Failed to write build report: ${error.message}`);
        }
    }

    /**
     * Calculate directory size
     */
    calculateDirectorySize(dirPath) {
        let totalSize = 0;
        
        if (!fs.existsSync(dirPath)) return 0;
        
        const items = fs.readdirSync(dirPath);
        for (const item of items) {
            const itemPath = path.join(dirPath, item);
            const stat = fs.statSync(itemPath);
            
            if (stat.isDirectory()) {
                totalSize += this.calculateDirectorySize(itemPath);
            } else {
                totalSize += stat.size;
            }
        }
        
        return totalSize;
    }

    /**
     * Format bytes for display
     */
    formatBytes(bytes) {
        const units = ['B', 'KB', 'MB', 'GB'];
        let size = bytes;
        let unitIndex = 0;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return `${size.toFixed(1)} ${units[unitIndex]}`;
    }

    /**
     * Format duration for display
     */
    formatDuration(ms) {
        const seconds = Math.floor(ms / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);
        
        if (hours > 0) {
            return `${hours}h ${minutes % 60}m ${seconds % 60}s`;
        } else if (minutes > 0) {
            return `${minutes}m ${seconds % 60}s`;
        } else {
            return `${seconds}s`;
        }
    }

    /**
     * Log message
     */
    log(level, category, message) {
        const timestamp = new Date().toISOString();
        const logMessage = `[${timestamp}] [${level}] [${category}] ${message}`;
        
        // Console output
        if (this.shouldLog(level)) {
            console.log(logMessage);
        }
        
        // File output
        try {
            fs.appendFileSync(this.config.outputFile, logMessage + '\n');
        } catch (error) {
            // Ignore file write errors
        }
    }

    /**
     * Check if message should be logged
     */
    shouldLog(level) {
        const levels = { 'DEBUG': 0, 'INFO': 1, 'WARNING': 2, 'ERROR': 3 };
        const currentLevel = levels[this.config.logLevel] || 1;
        const messageLevel = levels[level] || 1;
        return messageLevel >= currentLevel;
    }

    /**
     * Handle process exit
     */
    handleProcessExit(signal) {
        this.log('INFO', 'MONITOR', `Build process monitoring terminated by ${signal}`);
        if (this.buildStartTime && !this.buildEndTime) {
            this.buildEndTime = Date.now();
            this.addBuildError('MONITOR', 'Build process interrupted', `Process terminated by ${signal}`);
            this.generateBuildReport();
        }
        process.exit(1);
    }

    /**
     * Handle uncaught exceptions
     */
    handleUncaughtException(error) {
        this.log('ERROR', 'MONITOR', `Uncaught exception: ${error.message}`);
        this.addBuildError('MONITOR', 'Uncaught exception', error.message);
        if (this.buildStartTime && !this.buildEndTime) {
            this.buildEndTime = Date.now();
            this.generateBuildReport();
        }
        process.exit(1);
    }
}

// Export for use as module
module.exports = BuildProcessMonitor;

// Run if called directly
if (require.main === module) {
    const monitor = new BuildProcessMonitor();
    
    monitor.monitorBuildProcess()
        .then(success => {
            if (success) {
                console.log('\n✅ Build process monitoring completed successfully');
                process.exit(0);
            } else {
                console.log('\n❌ Build process monitoring detected issues');
                process.exit(1);
            }
        })
        .catch(error => {
            console.error('\n💥 Build process monitoring failed:', error.message);
            process.exit(1);
        });
}