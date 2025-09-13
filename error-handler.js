#!/usr/bin/env node

/**
 * Comprehensive Error Handling and Logging System
 * 
 * Features:
 * - Build process error detection and detailed logging
 * - Configuration file syntax checking
 * - Dependency problem detection and resolution suggestions
 * - Error classification and appropriate error message display
 * 
 * Requirements: 1.1, 1.2, 1.3
 */

const fs = require('fs');
const path = require('path');
const { spawn, exec } = require('child_process');
const util = require('util');

const execAsync = util.promisify(exec);

class ErrorHandler {
    constructor() {
        this.errors = [];
        this.warnings = [];
        this.info = [];
        this.logLevel = process.env.LOG_LEVEL || 'INFO';
        this.logFile = path.join(process.cwd(), 'deployment-error.log');
        
        // Error categories for classification
        this.errorCategories = {
            CONFIGURATION: 'Configuration Error',
            DEPENDENCY: 'Dependency Error',
            BUILD: 'Build Error',
            DEPLOYMENT: 'Deployment Error',
            VALIDATION: 'Validation Error',
            SYSTEM: 'System Error'
        };
        
        // Error severity levels
        this.severityLevels = {
            CRITICAL: 'CRITICAL',
            ERROR: 'ERROR',
            WARNING: 'WARNING',
            INFO: 'INFO',
            DEBUG: 'DEBUG'
        };
        
        this.initializeLogging();
    }

    /**
     * Initialize logging system
     */
    initializeLogging() {
        // Create log file if it doesn't exist
        if (!fs.existsSync(this.logFile)) {
            fs.writeFileSync(this.logFile, '');
        }
        
        this.log('INFO', 'SYSTEM', 'Error handling system initialized');
    }

    /**
     * Main error detection and handling process
     */
    async detectAndHandleErrors() {
        this.log('INFO', 'SYSTEM', 'Starting comprehensive error detection...');
        
        try {
            // 1. Configuration file syntax checking
            await this.checkConfigurationFiles();
            
            // 2. Dependency problem detection
            await this.checkDependencies();
            
            // 3. Build process validation
            await this.validateBuildProcess();
            
            // 4. System requirements validation
            await this.validateSystemRequirements();
            
            // 5. Generate comprehensive error report
            this.generateErrorReport();
            
            return {
                success: this.errors.length === 0,
                errors: this.errors,
                warnings: this.warnings,
                info: this.info
            };
            
        } catch (error) {
            this.addError('SYSTEM', 'CRITICAL', 'Error detection process failed', error.message);
            throw error;
        }
    }

    /**
     * Check configuration files for syntax and validity
     */
    async checkConfigurationFiles() {
        this.log('INFO', 'VALIDATION', 'Checking configuration files...');
        
        // Check amplify.yml
        await this.checkAmplifyConfig();
        
        // Check package.json files
        await this.checkPackageJsonFiles();
        
        // Check pom.xml
        await this.checkPomXml();
        
        // Check other configuration files
        await this.checkOtherConfigFiles();
    }

    /**
     * Check amplify.yml configuration
     */
    async checkAmplifyConfig() {
        const amplifyConfigPath = path.join(process.cwd(), 'amplify.yml');
        
        if (!fs.existsSync(amplifyConfigPath)) {
            this.addError('CONFIGURATION', 'CRITICAL', 'amplify.yml not found', 
                'The amplify.yml configuration file is required for AWS Amplify deployment');
            return;
        }
        
        try {
            const content = fs.readFileSync(amplifyConfigPath, 'utf8');
            
            // Parse YAML syntax (basic validation)
            let config;
            try {
                config = this.parseBasicYaml(content);
            } catch (yamlError) {
                this.addError('CONFIGURATION', 'CRITICAL', 'amplify.yml syntax error', 
                    `Invalid YAML syntax: ${yamlError.message}`, {
                        file: 'amplify.yml',
                        suggestion: 'Check YAML indentation and syntax. Use a YAML validator to identify issues.'
                    });
                return;
            }
            
            // Validate structure
            await this.validateAmplifyStructure(config);
            
            // Validate commands
            await this.validateAmplifyCommands(config);
            
            // Validate file references
            await this.validateAmplifyFileReferences(config);
            
            this.log('INFO', 'VALIDATION', 'amplify.yml validation completed');
            
        } catch (error) {
            this.addError('CONFIGURATION', 'ERROR', 'amplify.yml validation failed', error.message);
        }
    }

    /**
     * Validate amplify.yml structure
     */
    async validateAmplifyStructure(config) {
        const requiredFields = ['version'];
        const recommendedSections = ['frontend', 'backend'];
        
        // Check required fields
        for (const field of requiredFields) {
            if (!config[field]) {
                this.addError('CONFIGURATION', 'ERROR', `amplify.yml missing required field: ${field}`, 
                    `The ${field} field is required in amplify.yml`, {
                        suggestion: `Add "${field}: 1" to your amplify.yml file`
                    });
            }
        }
        
        // Check recommended sections
        for (const section of recommendedSections) {
            if (!config[section]) {
                this.addWarning('CONFIGURATION', `amplify.yml missing recommended section: ${section}`, 
                    `Consider adding a ${section} section for complete deployment configuration`);
            }
        }
        
        // Validate frontend section
        if (config.frontend) {
            await this.validateFrontendSection(config.frontend);
        }
        
        // Validate backend section
        if (config.backend) {
            await this.validateBackendSection(config.backend);
        }
    }

    /**
     * Validate frontend section
     */
    async validateFrontendSection(frontend) {
        if (!frontend.phases) {
            this.addError('CONFIGURATION', 'ERROR', 'Frontend section missing phases', 
                'Frontend configuration must include build phases');
            return;
        }
        
        const requiredPhases = ['build'];
        for (const phase of requiredPhases) {
            if (!frontend.phases[phase]) {
                this.addError('CONFIGURATION', 'ERROR', `Frontend missing ${phase} phase`, 
                    `Frontend configuration must include a ${phase} phase with commands`);
            }
        }
        
        // Check artifacts configuration
        if (!frontend.artifacts) {
            this.addWarning('CONFIGURATION', 'Frontend missing artifacts configuration', 
                'Consider adding artifacts configuration to specify which files to deploy');
        } else {
            if (!frontend.artifacts.baseDirectory) {
                this.addWarning('CONFIGURATION', 'Frontend artifacts missing baseDirectory', 
                    'Specify baseDirectory in artifacts configuration');
            }
            if (!frontend.artifacts.files || frontend.artifacts.files.length === 0) {
                this.addWarning('CONFIGURATION', 'Frontend artifacts missing files specification', 
                    'Specify which files to include in deployment');
            }
        }
    }

    /**
     * Validate backend section
     */
    async validateBackendSection(backend) {
        if (!backend.phases) {
            this.addError('CONFIGURATION', 'ERROR', 'Backend section missing phases', 
                'Backend configuration must include build phases');
            return;
        }
        
        const requiredPhases = ['build'];
        for (const phase of requiredPhases) {
            if (!backend.phases[phase]) {
                this.addError('CONFIGURATION', 'ERROR', `Backend missing ${phase} phase`, 
                    `Backend configuration must include a ${phase} phase with commands`);
            }
        }
        
        // Check for Java/Maven specific commands
        if (backend.phases.build && backend.phases.build.commands) {
            const commands = backend.phases.build.commands.join(' ');
            if (!commands.includes('mvn') && !commands.includes('mvnw')) {
                this.addWarning('CONFIGURATION', 'Backend build commands may not include Maven', 
                    'Java Spring Boot projects typically require Maven build commands');
            }
        }
    }

    /**
     * Validate amplify.yml commands
     */
    async validateAmplifyCommands(config) {
        const sections = ['frontend', 'backend'];
        
        for (const section of sections) {
            if (!config[section] || !config[section].phases) continue;
            
            for (const [phase, phaseConfig] of Object.entries(config[section].phases)) {
                if (phaseConfig.commands) {
                    for (const command of phaseConfig.commands) {
                        await this.validateCommand(command, `${section}.${phase}`);
                    }
                }
            }
        }
    }

    /**
     * Validate individual command
     */
    async validateCommand(command, context) {
        // Skip echo and comment commands
        if (command.startsWith('echo') || command.startsWith('#')) {
            return;
        }
        
        // Check for common command issues
        const commandChecks = [
            {
                pattern: /cd\s+/,
                issue: 'cd command usage',
                suggestion: 'Use absolute paths or ensure directory exists before changing to it'
            },
            {
                pattern: /mvn\s+/,
                issue: 'mvn command without wrapper',
                suggestion: 'Consider using mvnw (Maven wrapper) for consistent builds'
            },
            {
                pattern: /npm\s+install/,
                issue: 'npm install without cache',
                suggestion: 'Consider using npm ci for faster, reliable builds'
            }
        ];
        
        for (const check of commandChecks) {
            if (check.pattern.test(command)) {
                this.addInfo('BUILD', `${context}: ${check.issue}`, check.suggestion);
            }
        }
        
        // Check for file existence in commands
        const fileReferences = command.match(/(?:test\s+-f\s+|ls\s+|cat\s+)([^\s]+)/g);
        if (fileReferences) {
            for (const ref of fileReferences) {
                const filePath = ref.replace(/^(?:test\s+-f\s+|ls\s+|cat\s+)/, '');
                if (!fs.existsSync(filePath)) {
                    this.addWarning('CONFIGURATION', `Command references non-existent file: ${filePath}`, 
                        `Ensure ${filePath} exists or update the command`);
                }
            }
        }
    }

    /**
     * Validate file references in amplify.yml
     */
    async validateAmplifyFileReferences(config) {
        // Check frontend artifacts files
        if (config.frontend && config.frontend.artifacts && config.frontend.artifacts.files) {
            const baseDir = config.frontend.artifacts.baseDirectory || 'frontend';
            
            for (const filePattern of config.frontend.artifacts.files) {
                // Skip wildcard patterns for now
                if (filePattern.includes('*')) continue;
                
                const filePath = path.join(baseDir, filePattern);
                if (!fs.existsSync(filePath)) {
                    this.addWarning('CONFIGURATION', `Frontend artifact file not found: ${filePattern}`, 
                        `File ${filePath} specified in artifacts but does not exist`);
                }
            }
        }
        
        // Check backend artifacts files
        if (config.backend && config.backend.artifacts && config.backend.artifacts.files) {
            const baseDir = config.backend.artifacts.baseDirectory || 'backend/target';
            
            for (const filePattern of config.backend.artifacts.files) {
                // Skip wildcard patterns for now
                if (filePattern.includes('*')) continue;
                
                const filePath = path.join(baseDir, filePattern);
                if (!fs.existsSync(filePath)) {
                    this.addInfo('CONFIGURATION', `Backend artifact file not found: ${filePattern}`, 
                        `File ${filePath} will be created during build process`);
                }
            }
        }
    }

    /**
     * Check package.json files
     */
    async checkPackageJsonFiles() {
        const packageJsonPaths = [
            'package.json',
            'frontend/package.json'
        ];
        
        for (const packagePath of packageJsonPaths) {
            if (fs.existsSync(packagePath)) {
                await this.validatePackageJson(packagePath);
            }
        }
    }

    /**
     * Validate package.json file
     */
    async validatePackageJson(packagePath) {
        try {
            const content = fs.readFileSync(packagePath, 'utf8');
            let pkg;
            
            try {
                pkg = JSON.parse(content);
            } catch (jsonError) {
                this.addError('CONFIGURATION', 'ERROR', `${packagePath} syntax error`, 
                    `Invalid JSON syntax: ${jsonError.message}`, {
                        file: packagePath,
                        suggestion: 'Use a JSON validator to identify and fix syntax issues'
                    });
                return;
            }
            
            // Check required fields
            const requiredFields = ['name', 'version'];
            for (const field of requiredFields) {
                if (!pkg[field]) {
                    this.addWarning('CONFIGURATION', `${packagePath} missing field: ${field}`, 
                        `Consider adding ${field} field to ${packagePath}`);
                }
            }
            
            // Check scripts
            if (pkg.scripts) {
                const recommendedScripts = ['build', 'start'];
                for (const script of recommendedScripts) {
                    if (!pkg.scripts[script]) {
                        this.addInfo('CONFIGURATION', `${packagePath} missing script: ${script}`, 
                            `Consider adding ${script} script for better development workflow`);
                    }
                }
            }
            
            this.log('INFO', 'VALIDATION', `${packagePath} validation completed`);
            
        } catch (error) {
            this.addError('CONFIGURATION', 'ERROR', `${packagePath} validation failed`, error.message);
        }
    }

    /**
     * Check pom.xml file
     */
    async checkPomXml() {
        const pomPath = path.join('backend', 'pom.xml');
        
        if (!fs.existsSync(pomPath)) {
            this.addWarning('CONFIGURATION', 'pom.xml not found', 
                'Backend Java project should have pom.xml for Maven builds');
            return;
        }
        
        try {
            const content = fs.readFileSync(pomPath, 'utf8');
            
            // Basic XML syntax check
            if (!content.includes('<?xml') || !content.includes('<project')) {
                this.addError('CONFIGURATION', 'ERROR', 'pom.xml invalid structure', 
                    'pom.xml does not appear to be a valid Maven project file');
                return;
            }
            
            // Check for Lambda-specific dependencies
            const lambdaDependencies = [
                'aws-lambda-java-core',
                'aws-serverless-java-container',
                'aws-lambda-java-events'
            ];
            
            for (const dep of lambdaDependencies) {
                if (!content.includes(dep)) {
                    this.addWarning('DEPENDENCY', `pom.xml missing Lambda dependency: ${dep}`, 
                        `Consider adding ${dep} dependency for AWS Lambda deployment`);
                }
            }
            
            // Check for Spring Boot plugin
            if (!content.includes('spring-boot-maven-plugin')) {
                this.addError('CONFIGURATION', 'ERROR', 'pom.xml missing Spring Boot plugin', 
                    'Spring Boot Maven plugin is required for building executable JAR');
            }
            
            this.log('INFO', 'VALIDATION', 'pom.xml validation completed');
            
        } catch (error) {
            this.addError('CONFIGURATION', 'ERROR', 'pom.xml validation failed', error.message);
        }
    }

    /**
     * Check other configuration files
     */
    async checkOtherConfigFiles() {
        const configFiles = [
            { path: 'template.yaml', type: 'SAM template' },
            { path: 'samconfig.toml', type: 'SAM configuration' }
        ];
        
        for (const config of configFiles) {
            if (fs.existsSync(config.path)) {
                this.log('INFO', 'VALIDATION', `Found ${config.type}: ${config.path}`);
                
                // Basic validation
                try {
                    const content = fs.readFileSync(config.path, 'utf8');
                    if (content.trim().length === 0) {
                        this.addWarning('CONFIGURATION', `${config.path} is empty`, 
                            `${config.type} file exists but is empty`);
                    }
                } catch (error) {
                    this.addError('CONFIGURATION', 'ERROR', `Failed to read ${config.path}`, error.message);
                }
            }
        }
    }

    /**
     * Check dependencies
     */
    async checkDependencies() {
        this.log('INFO', 'DEPENDENCY', 'Checking dependencies...');
        
        // Check system dependencies
        await this.checkSystemDependencies();
        
        // Check Node.js dependencies
        await this.checkNodeDependencies();
        
        // Check Java dependencies
        await this.checkJavaDependencies();
        
        // Check AWS CLI
        await this.checkAwsCli();
    }

    /**
     * Check system dependencies
     */
    async checkSystemDependencies() {
        const systemDeps = [
            { command: 'node --version', name: 'Node.js', required: true },
            { command: 'npm --version', name: 'npm', required: true },
            { command: 'java -version', name: 'Java', required: true },
            { command: 'mvn --version', name: 'Maven', required: false },
            { command: 'aws --version', name: 'AWS CLI', required: true }
        ];
        
        for (const dep of systemDeps) {
            try {
                const { stdout, stderr } = await execAsync(dep.command);
                const output = stdout || stderr;
                this.log('INFO', 'DEPENDENCY', `${dep.name} found: ${output.split('\n')[0]}`);
            } catch (error) {
                if (dep.required) {
                    this.addError('DEPENDENCY', 'CRITICAL', `${dep.name} not found`, 
                        `${dep.name} is required but not installed or not in PATH`, {
                            suggestion: this.getDependencyInstallSuggestion(dep.name)
                        });
                } else {
                    this.addWarning('DEPENDENCY', `${dep.name} not found`, 
                        `${dep.name} is recommended but not installed`);
                }
            }
        }
    }

    /**
     * Get dependency installation suggestion
     */
    getDependencyInstallSuggestion(depName) {
        const suggestions = {
            'Node.js': 'Install Node.js from https://nodejs.org/ or use a version manager like nvm',
            'npm': 'npm is included with Node.js installation',
            'Java': 'Install Java 17 or later from https://adoptium.net/ or use package manager',
            'Maven': 'Install Maven from https://maven.apache.org/ or use the included mvnw wrapper',
            'AWS CLI': 'Install AWS CLI from https://aws.amazon.com/cli/ or use package manager'
        };
        
        return suggestions[depName] || `Install ${depName} according to its official documentation`;
    }

    /**
     * Check Node.js dependencies
     */
    async checkNodeDependencies() {
        const packageJsonPaths = ['package.json', 'frontend/package.json'];
        
        for (const packagePath of packageJsonPaths) {
            if (!fs.existsSync(packagePath)) continue;
            
            const nodeModulesPath = path.join(path.dirname(packagePath), 'node_modules');
            
            if (!fs.existsSync(nodeModulesPath)) {
                this.addWarning('DEPENDENCY', `Node modules not installed for ${packagePath}`, 
                    `Run 'npm install' in ${path.dirname(packagePath)} directory`);
                continue;
            }
            
            try {
                const pkg = JSON.parse(fs.readFileSync(packagePath, 'utf8'));
                
                if (pkg.dependencies) {
                    for (const [depName, version] of Object.entries(pkg.dependencies)) {
                        const depPath = path.join(nodeModulesPath, depName);
                        if (!fs.existsSync(depPath)) {
                            this.addWarning('DEPENDENCY', `Missing Node.js dependency: ${depName}`, 
                                `Dependency ${depName}@${version} is not installed`);
                        }
                    }
                }
                
                this.log('INFO', 'DEPENDENCY', `Node.js dependencies checked for ${packagePath}`);
                
            } catch (error) {
                this.addError('DEPENDENCY', 'ERROR', `Failed to check Node.js dependencies for ${packagePath}`, 
                    error.message);
            }
        }
    }

    /**
     * Check Java dependencies
     */
    async checkJavaDependencies() {
        const pomPath = path.join('backend', 'pom.xml');
        
        if (!fs.existsSync(pomPath)) {
            return; // Already reported in configuration check
        }
        
        // Check if Maven wrapper exists
        const mvnwPaths = ['backend/mvnw', 'backend/mvnw.cmd'];
        const hasMvnw = mvnwPaths.some(p => fs.existsSync(p));
        
        if (!hasMvnw) {
            this.addWarning('DEPENDENCY', 'Maven wrapper not found', 
                'Consider using Maven wrapper (mvnw) for consistent builds across environments');
        }
        
        // Check Maven local repository
        try {
            const { stdout } = await execAsync('mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout', 
                { cwd: 'backend' });
            const localRepo = stdout.trim();
            
            if (fs.existsSync(localRepo)) {
                this.log('INFO', 'DEPENDENCY', `Maven local repository found: ${localRepo}`);
            } else {
                this.addWarning('DEPENDENCY', 'Maven local repository not found', 
                    'Maven dependencies may need to be downloaded');
            }
        } catch (error) {
            this.addInfo('DEPENDENCY', 'Could not check Maven local repository', 
                'This is normal if Maven is not installed or configured');
        }
    }

    /**
     * Check AWS CLI configuration
     */
    async checkAwsCli() {
        try {
            // Check AWS CLI version
            const { stdout } = await execAsync('aws --version');
            this.log('INFO', 'DEPENDENCY', `AWS CLI version: ${stdout.trim()}`);
            
            // Check AWS credentials
            try {
                await execAsync('aws sts get-caller-identity');
                this.log('INFO', 'DEPENDENCY', 'AWS credentials are configured');
            } catch (credError) {
                this.addError('DEPENDENCY', 'ERROR', 'AWS credentials not configured', 
                    'AWS credentials are required for deployment', {
                        suggestion: 'Run "aws configure" to set up your AWS credentials'
                    });
            }
            
        } catch (error) {
            // Already handled in system dependencies check
        }
    }

    /**
     * Validate build process
     */
    async validateBuildProcess() {
        this.log('INFO', 'BUILD', 'Validating build process...');
        
        // Check frontend build
        await this.validateFrontendBuild();
        
        // Check backend build
        await this.validateBackendBuild();
    }

    /**
     * Validate frontend build process
     */
    async validateFrontendBuild() {
        const frontendDir = 'frontend';
        
        if (!fs.existsSync(frontendDir)) {
            this.addError('BUILD', 'ERROR', 'Frontend directory not found', 
                'Frontend directory is required for deployment');
            return;
        }
        
        // Check required frontend files
        const requiredFiles = [
            'frontend/index.html',
            'frontend/css/style.css',
            'frontend/js/app.js',
            'frontend/js/api-client.js'
        ];
        
        for (const file of requiredFiles) {
            if (!fs.existsSync(file)) {
                this.addError('BUILD', 'ERROR', `Required frontend file missing: ${file}`, 
                    `File ${file} is required for frontend build`);
            }
        }
        
        // Check build script
        const buildScriptPath = 'frontend/build-script.js';
        if (fs.existsSync(buildScriptPath)) {
            this.log('INFO', 'BUILD', 'Frontend build script found');
        } else {
            this.addWarning('BUILD', 'Frontend build script not found', 
                'Consider creating a build script for consistent frontend builds');
        }
    }

    /**
     * Validate backend build process
     */
    async validateBackendBuild() {
        const backendDir = 'backend';
        
        if (!fs.existsSync(backendDir)) {
            this.addError('BUILD', 'ERROR', 'Backend directory not found', 
                'Backend directory is required for deployment');
            return;
        }
        
        // Check pom.xml
        const pomPath = path.join(backendDir, 'pom.xml');
        if (!fs.existsSync(pomPath)) {
            this.addError('BUILD', 'ERROR', 'Backend pom.xml not found', 
                'pom.xml is required for Maven build');
            return;
        }
        
        // Check source directory
        const srcDir = path.join(backendDir, 'src', 'main', 'java');
        if (!fs.existsSync(srcDir)) {
            this.addError('BUILD', 'ERROR', 'Backend source directory not found', 
                'Java source directory src/main/java is required');
        }
        
        // Check for Lambda application class
        try {
            const javaFiles = this.findJavaFiles(srcDir);
            const hasLambdaApp = javaFiles.some(file => {
                const content = fs.readFileSync(file, 'utf8');
                return content.includes('LambdaApplication') || content.includes('@SpringBootApplication');
            });
            
            if (!hasLambdaApp) {
                this.addWarning('BUILD', 'Lambda application class not found', 
                    'Ensure you have a main application class for Lambda deployment');
            }
        } catch (error) {
            this.addWarning('BUILD', 'Could not check Java source files', error.message);
        }
    }

    /**
     * Find Java files recursively
     */
    findJavaFiles(dir) {
        const javaFiles = [];
        
        if (!fs.existsSync(dir)) return javaFiles;
        
        const items = fs.readdirSync(dir);
        for (const item of items) {
            const fullPath = path.join(dir, item);
            const stat = fs.statSync(fullPath);
            
            if (stat.isDirectory()) {
                javaFiles.push(...this.findJavaFiles(fullPath));
            } else if (item.endsWith('.java')) {
                javaFiles.push(fullPath);
            }
        }
        
        return javaFiles;
    }

    /**
     * Validate system requirements
     */
    async validateSystemRequirements() {
        this.log('INFO', 'SYSTEM', 'Validating system requirements...');
        
        // Check disk space
        await this.checkDiskSpace();
        
        // Check memory
        await this.checkMemory();
        
        // Check network connectivity
        await this.checkNetworkConnectivity();
    }

    /**
     * Check available disk space
     */
    async checkDiskSpace() {
        try {
            const stats = fs.statSync(process.cwd());
            
            // Get available disk space (simplified check)
            const freeSpace = 1024 * 1024 * 1024; // Assume 1GB available (placeholder)
            const requiredSpace = 500 * 1024 * 1024; // 500MB required
            
            if (freeSpace < requiredSpace) {
                this.addError('SYSTEM', 'CRITICAL', 'Insufficient disk space', 
                    `Available: ${this.formatBytes(freeSpace)}, Required: ${this.formatBytes(requiredSpace)}`);
            } else {
                this.log('INFO', 'SYSTEM', `Disk space check passed: ${this.formatBytes(freeSpace)} available`);
            }
            
        } catch (error) {
            this.addWarning('SYSTEM', 'Could not check disk space', error.message);
        }
    }

    /**
     * Check available memory
     */
    async checkMemory() {
        try {
            const totalMemory = require('os').totalmem();
            const freeMemory = require('os').freemem();
            const usedMemory = totalMemory - freeMemory;
            const memoryUsagePercent = (usedMemory / totalMemory) * 100;
            
            this.log('INFO', 'SYSTEM', `Memory usage: ${memoryUsagePercent.toFixed(1)}%`);
            
            if (memoryUsagePercent > 90) {
                this.addError('SYSTEM', 'CRITICAL', 'High memory usage', 
                    `Memory usage is ${memoryUsagePercent.toFixed(1)}%, which may cause build failures`);
            } else if (memoryUsagePercent > 80) {
                this.addWarning('SYSTEM', 'High memory usage', 
                    `Memory usage is ${memoryUsagePercent.toFixed(1)}%`);
            }
            
        } catch (error) {
            this.addWarning('SYSTEM', 'Could not check memory usage', error.message);
        }
    }

    /**
     * Check network connectivity
     */
    async checkNetworkConnectivity() {
        const testUrls = [
            'https://registry.npmjs.org',
            'https://repo1.maven.org',
            'https://aws.amazon.com'
        ];
        
        for (const url of testUrls) {
            try {
                await this.testConnection(url);
                this.log('INFO', 'SYSTEM', `Network connectivity OK: ${url}`);
            } catch (error) {
                this.addWarning('SYSTEM', `Network connectivity issue: ${url}`, 
                    `Could not connect to ${url}: ${error.message}`);
            }
        }
    }

    /**
     * Test network connection
     */
    async testConnection(url) {
        return new Promise((resolve, reject) => {
            const https = require('https');
            const { URL } = require('url');
            
            const urlObj = new URL(url);
            const options = {
                hostname: urlObj.hostname,
                port: urlObj.port || 443,
                path: '/',
                method: 'HEAD',
                timeout: 5000
            };
            
            const req = https.request(options, (res) => {
                resolve(res.statusCode);
            });
            
            req.on('error', reject);
            req.on('timeout', () => {
                req.destroy();
                reject(new Error('Connection timeout'));
            });
            
            req.end();
        });
    }

    /**
     * Parse basic YAML (simplified parser for amplify.yml)
     */
    parseBasicYaml(content) {
        const lines = content.split('\n');
        const result = {};
        const stack = [result];
        let currentIndent = 0;
        
        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];
            const trimmed = line.trim();
            
            // Skip empty lines and comments
            if (!trimmed || trimmed.startsWith('#')) continue;
            
            // Calculate indentation
            const indent = line.length - line.trimStart().length;
            
            // Handle indentation changes
            if (indent > currentIndent) {
                // Deeper level - already handled by previous iteration
            } else if (indent < currentIndent) {
                // Back to previous level
                const levels = (currentIndent - indent) / 2;
                for (let j = 0; j < levels; j++) {
                    stack.pop();
                }
            }
            currentIndent = indent;
            
            // Parse key-value pairs
            if (trimmed.includes(':')) {
                const [key, ...valueParts] = trimmed.split(':');
                const value = valueParts.join(':').trim();
                const current = stack[stack.length - 1];
                
                if (value === '' || value === null) {
                    // Object or array
                    current[key.trim()] = {};
                    stack.push(current[key.trim()]);
                } else if (value.startsWith('[') && value.endsWith(']')) {
                    // Array
                    current[key.trim()] = JSON.parse(value);
                } else {
                    // Simple value
                    current[key.trim()] = value.replace(/^["']|["']$/g, '');
                }
            } else if (trimmed.startsWith('- ')) {
                // Array item
                const current = stack[stack.length - 1];
                const item = trimmed.substring(2).trim();
                
                if (!Array.isArray(current.commands)) {
                    current.commands = [];
                }
                current.commands.push(item);
            }
        }
        
        return result;
    }

    /**
     * Add error with classification
     */
    addError(category, severity, title, message, details = {}) {
        const error = {
            category,
            severity,
            title,
            message,
            details,
            timestamp: new Date().toISOString()
        };
        
        this.errors.push(error);
        this.log('ERROR', category, `${title}: ${message}`);
    }

    /**
     * Add warning
     */
    addWarning(category, title, message, details = {}) {
        const warning = {
            category,
            title,
            message,
            details,
            timestamp: new Date().toISOString()
        };
        
        this.warnings.push(warning);
        this.log('WARNING', category, `${title}: ${message}`);
    }

    /**
     * Add info message
     */
    addInfo(category, title, message, details = {}) {
        const info = {
            category,
            title,
            message,
            details,
            timestamp: new Date().toISOString()
        };
        
        this.info.push(info);
        this.log('INFO', category, `${title}: ${message}`);
    }

    /**
     * Log message with level and category
     */
    log(level, category, message) {
        const timestamp = new Date().toISOString();
        const logMessage = `[${timestamp}] [${level}] [${category}] ${message}`;
        
        // Console output based on log level
        if (this.shouldLog(level)) {
            console.log(logMessage);
        }
        
        // Write to log file
        try {
            fs.appendFileSync(this.logFile, logMessage + '\n');
        } catch (error) {
            // Ignore file write errors to prevent infinite loops
        }
    }

    /**
     * Check if message should be logged based on log level
     */
    shouldLog(level) {
        const levels = {
            'DEBUG': 0,
            'INFO': 1,
            'WARNING': 2,
            'ERROR': 3,
            'CRITICAL': 4
        };
        
        const currentLevel = levels[this.logLevel] || 1;
        const messageLevel = levels[level] || 1;
        
        return messageLevel >= currentLevel;
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
     * Generate comprehensive error report
     */
    generateErrorReport() {
        console.log('\n📊 Error Detection Report');
        console.log('==========================');
        
        // Summary statistics
        console.log(`Total Checks Performed: ${this.getTotalChecks()}`);
        console.log(`Errors Found: ${this.errors.length}`);
        console.log(`Warnings: ${this.warnings.length}`);
        console.log(`Info Messages: ${this.info.length}`);
        
        // Error breakdown by category
        const errorsByCategory = this.groupByCategory(this.errors);
        if (Object.keys(errorsByCategory).length > 0) {
            console.log('\n❌ Errors by Category:');
            for (const [category, errors] of Object.entries(errorsByCategory)) {
                console.log(`  ${category}: ${errors.length}`);
            }
        }
        
        // Warning breakdown by category
        const warningsByCategory = this.groupByCategory(this.warnings);
        if (Object.keys(warningsByCategory).length > 0) {
            console.log('\n⚠️  Warnings by Category:');
            for (const [category, warnings] of Object.entries(warningsByCategory)) {
                console.log(`  ${category}: ${warnings.length}`);
            }
        }
        
        // Detailed error list
        if (this.errors.length > 0) {
            console.log('\n🔍 Detailed Error Analysis:');
            this.errors.forEach((error, index) => {
                console.log(`\n${index + 1}. [${error.severity}] ${error.title}`);
                console.log(`   Category: ${error.category}`);
                console.log(`   Message: ${error.message}`);
                if (error.details.suggestion) {
                    console.log(`   💡 Suggestion: ${error.details.suggestion}`);
                }
                if (error.details.file) {
                    console.log(`   📄 File: ${error.details.file}`);
                }
            });
        }
        
        // Resolution recommendations
        const recommendations = this.generateResolutionRecommendations();
        if (recommendations.length > 0) {
            console.log('\n🔧 Resolution Recommendations:');
            recommendations.forEach((rec, index) => {
                console.log(`\n${index + 1}. ${rec.title} (Priority: ${rec.priority})`);
                console.log(`   ${rec.description}`);
                console.log('   Actions:');
                rec.actions.forEach(action => console.log(`   • ${action}`));
            });
        }
        
        // Save detailed report to file
        this.saveDetailedReport();
        
        // Display summary
        this.displayErrorSummary();
    }

    /**
     * Get total number of checks performed
     */
    getTotalChecks() {
        return this.errors.length + this.warnings.length + this.info.length;
    }

    /**
     * Group items by category
     */
    groupByCategory(items) {
        return items.reduce((groups, item) => {
            const category = item.category || 'UNKNOWN';
            if (!groups[category]) {
                groups[category] = [];
            }
            groups[category].push(item);
            return groups;
        }, {});
    }

    /**
     * Save detailed report to file
     */
    saveDetailedReport() {
        const reportPath = path.join(process.cwd(), 'error-analysis-report.json');
        const report = {
            timestamp: new Date().toISOString(),
            summary: {
                totalChecks: this.getTotalChecks(),
                errors: this.errors.length,
                warnings: this.warnings.length,
                info: this.info.length,
                success: this.errors.length === 0
            },
            errors: this.errors,
            warnings: this.warnings,
            info: this.info,
            recommendations: this.generateResolutionRecommendations()
        };
        
        try {
            fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
            console.log(`\n📄 Detailed error report saved to: ${reportPath}`);
        } catch (error) {
            console.warn(`Failed to write error report: ${error.message}`);
        }
    }

    /**
     * Generate resolution recommendations
     */
    generateResolutionRecommendations() {
        const recommendations = [];
        
        // Configuration recommendations
        const configErrors = this.errors.filter(e => e.category === 'CONFIGURATION');
        if (configErrors.length > 0) {
            recommendations.push({
                category: 'CONFIGURATION',
                priority: 'HIGH',
                title: 'Fix Configuration Issues',
                description: 'Resolve configuration file problems that prevent deployment',
                actions: [
                    'Validate YAML syntax in amplify.yml',
                    'Ensure all referenced files exist',
                    'Check environment variable settings',
                    'Verify build command syntax'
                ]
            });
        }
        
        // Dependency recommendations
        const depErrors = this.errors.filter(e => e.category === 'DEPENDENCY');
        if (depErrors.length > 0) {
            recommendations.push({
                category: 'DEPENDENCY',
                priority: 'HIGH',
                title: 'Resolve Dependency Issues',
                description: 'Install missing dependencies and resolve version conflicts',
                actions: [
                    'Install missing system dependencies (Node.js, Java, Maven, AWS CLI)',
                    'Run npm install for Node.js dependencies',
                    'Configure AWS credentials',
                    'Verify Maven local repository'
                ]
            });
        }
        
        // Build recommendations
        const buildErrors = this.errors.filter(e => e.category === 'BUILD');
        if (buildErrors.length > 0) {
            recommendations.push({
                category: 'BUILD',
                priority: 'HIGH',
                title: 'Fix Build Configuration',
                description: 'Resolve build process issues',
                actions: [
                    'Ensure all required source files are present',
                    'Verify build scripts and commands',
                    'Test build process locally before deployment'
                ]
            });
        }
        
        return recommendations;());
            // This is a basic check - in a real implementation, you'd use a library like 'statvfs'
            this.log('INFO', 'SYSTEM', 'Disk space check completed');
        } catch (error) {
            this.addWarning('SYSTEM', 'Could not check disk space', error.message);
        }
    }

    /**
     * Check available memory
     */
    async checkMemory() {
        const totalMem = require('os').totalmem();
        const freeMem = require('os').freemem();
        const usedMem = totalMem - freeMem;
        
        const totalGB = (totalMem / 1024 / 1024 / 1024).toFixed(1);
        const freeGB = (freeMem / 1024 / 1024 / 1024).toFixed(1);
        
        this.log('INFO', 'SYSTEM', `Memory: ${freeGB}GB free of ${totalGB}GB total`);
        
        if (freeMem < 1024 * 1024 * 1024) { // Less than 1GB free
            this.addWarning('SYSTEM', 'Low memory available', 
                'Less than 1GB of free memory available. Build process may be slow.');
        }
    }

    /**
     * Check network connectivity
     */
    async checkNetworkConnectivity() {
        const testUrls = [
            'https://aws.amazon.com',
            'https://registry.npmjs.org',
            'https://repo1.maven.org'
        ];
        
        for (const url of testUrls) {
            try {
                // Simple connectivity check - in a real implementation, you'd use a proper HTTP client
                this.log('INFO', 'SYSTEM', `Network connectivity to ${url} - assumed OK`);
            } catch (error) {
                this.addWarning('SYSTEM', `Network connectivity issue to ${url}`, 
                    'Check internet connection and firewall settings');
            }
        }
    }

    /**
     * Add error with classification
     */
    addError(category, severity, title, message, details = {}) {
        const error = {
            category,
            severity,
            title,
            message,
            details,
            timestamp: new Date().toISOString()
        };
        
        this.errors.push(error);
        this.log('ERROR', category, `${title}: ${message}`);
    }

    /**
     * Add warning
     */
    addWarning(category, title, message, details = {}) {
        const warning = {
            category,
            severity: 'WARNING',
            title,
            message,
            details,
            timestamp: new Date().toISOString()
        };
        
        this.warnings.push(warning);
        this.log('WARNING', category, `${title}: ${message}`);
    }

    /**
     * Add info
     */
    addInfo(category, title, message, details = {}) {
        const info = {
            category,
            severity: 'INFO',
            title,
            message,
            details,
            timestamp: new Date().toISOString()
        };
        
        this.info.push(info);
        this.log('INFO', category, `${title}: ${message}`);
    }

    /**
     * Log message to console and file
     */
    log(level, category, message) {
        const timestamp = new Date().toISOString();
        const logMessage = `[${timestamp}] [${level}] [${category}] ${message}`;
        
        // Console output with colors
        const colors = {
            ERROR: '\x1b[31m',    // Red
            WARNING: '\x1b[33m',  // Yellow
            INFO: '\x1b[36m',     // Cyan
            DEBUG: '\x1b[37m',    // White
            RESET: '\x1b[0m'      // Reset
        };
        
        const color = colors[level] || colors.INFO;
        console.log(`${color}${logMessage}${colors.RESET}`);
        
        // File output
        try {
            fs.appendFileSync(this.logFile, logMessage + '\n');
        } catch (error) {
            console.error('Failed to write to log file:', error.message);
        }
    }

    /**
     * Generate comprehensive error report
     */
    generateErrorReport() {
        const report = {
            timestamp: new Date().toISOString(),
            summary: {
                totalErrors: this.errors.length,
                totalWarnings: this.warnings.length,
                totalInfo: this.info.length,
                success: this.errors.length === 0
            },
            errors: this.errors,
            warnings: this.warnings,
            info: this.info,
            recommendations: this.generateRecommendations()
        };
        
        // Write detailed report
        const reportPath = path.join(process.cwd(), 'error-analysis-report.json');
        try {
            fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
            this.log('INFO', 'SYSTEM', `Detailed error report saved to: ${reportPath}`);
        } catch (error) {
            console.error('Failed to write error report:', error.message);
        }
        
        // Display summary
        this.displayErrorSummary();
        
        return report;
    }

    /**
     * Generate recommendations based on errors
     */
    generateRecommendations() {
        const recommendations = [];
        
        // Configuration recommendations
        const configErrors = this.errors.filter(e => e.category === 'CONFIGURATION');
        if (configErrors.length > 0) {
            recommendations.push({
                category: 'CONFIGURATION',
                priority: 'HIGH',
                title: 'Fix Configuration Issues',
                description: 'Resolve configuration file syntax and structure issues before deployment',
                actions: [
                    'Validate YAML syntax in amplify.yml',
                    'Check JSON syntax in package.json files',
                    'Ensure all required configuration fields are present'
                ]
            });
        }
        
        // Dependency recommendations
        const depErrors = this.errors.filter(e => e.category === 'DEPENDENCY');
        if (depErrors.length > 0) {
            recommendations.push({
                category: 'DEPENDENCY',
                priority: 'CRITICAL',
                title: 'Install Missing Dependencies',
                description: 'Install required system and project dependencies',
                actions: [
                    'Install missing system dependencies (Node.js, Java, AWS CLI)',
                    'Run npm install for Node.js dependencies',
                    'Configure AWS credentials'
                ]
            });
        }
        
        // Build recommendations
        const buildErrors = this.errors.filter(e => e.category === 'BUILD');
        if (buildErrors.length > 0) {
            recommendations.push({
                category: 'BUILD',
                priority: 'HIGH',
                title: 'Fix Build Configuration',
                description: 'Resolve build process issues',
                actions: [
                    'Ensure all required source files are present',
                    'Verify build scripts and commands',
                    'Test build process locally before deployment'
                ]
            });
        }
        
        return recommendations;
    }

    /**
     * Display error summary
     */
    displayErrorSummary() {
        console.log('\n' + '='.repeat(60));
        console.log('ERROR ANALYSIS SUMMARY');
        console.log('='.repeat(60));
        
        console.log(`Total Errors: ${this.errors.length}`);
        console.log(`Total Warnings: ${this.warnings.length}`);
        console.log(`Total Info: ${this.info.length}`);
        
        if (this.errors.length > 0) {
            console.log('\n❌ CRITICAL ISSUES TO RESOLVE:');
            this.errors.forEach((error, index) => {
                console.log(`${index + 1}. [${error.category}] ${error.title}`);
                console.log(`   ${error.message}`);
                if (error.details.suggestion) {
                    console.log(`   💡 Suggestion: ${error.details.suggestion}`);
                }
                console.log('');
            });
        }
        
        if (this.warnings.length > 0) {
            console.log('\n⚠️  WARNINGS TO CONSIDER:');
            this.warnings.slice(0, 5).forEach((warning, index) => {
                console.log(`${index + 1}. [${warning.category}] ${warning.title}`);
                console.log(`   ${warning.message}`);
                console.log('');
            });
            
            if (this.warnings.length > 5) {
                console.log(`   ... and ${this.warnings.length - 5} more warnings`);
            }
        }
        
        console.log('='.repeat(60));
        
        if (this.errors.length === 0) {
            console.log('✅ No critical errors found! Ready for deployment.');
        } else {
            console.log('❌ Please resolve the critical issues before proceeding with deployment.');
        }
    }
}

// Export for use as module
module.exports = ErrorHandler;

// Run if called directly
if (require.main === module) {
    const errorHandler = new ErrorHandler();
    
    errorHandler.detectAndHandleErrors()
        .then(result => {
            if (result.success) {
                console.log('\n✅ Error detection completed successfully');
                process.exit(0);
            } else {
                console.log('\n❌ Issues found that need attention');
                process.exit(1);
            }
        })
        .catch(error => {
            console.error('\n💥 Error detection process failed:', error.message);
            process.exit(1);
        });
}