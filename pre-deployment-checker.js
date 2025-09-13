#!/usr/bin/env node

/**
 * Pre-deployment Check Tool
 * 
 * Comprehensive validation tool that checks all aspects of the deployment
 * configuration before attempting to deploy to AWS Amplify.
 * 
 * Features:
 * - amplify.yml syntax validation
 * - Required files existence check
 * - Build commands executability validation
 * - Dependencies availability verification
 * 
 * Requirements: 1.1, 1.2, 3.1, 3.2
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

class PreDeploymentChecker {
    constructor(options = {}) {
        this.options = {
            verbose: options.verbose || false,
            skipDependencyCheck: options.skipDependencyCheck || false,
            skipCommandCheck: options.skipCommandCheck || false,
            ...options
        };
        
        this.errors = [];
        this.warnings = [];
        this.successes = [];
        this.checkedItems = [];
        
        // Configuration for checks
        this.amplifyConfigPath = 'amplify.yml';
        this.requiredFiles = [
            'amplify.yml',
            'frontend/index.html',
            'frontend/css/style.css',
            'frontend/js/app.js',
            'frontend/js/api-client.js',
            'backend/pom.xml',
            'backend/src/main/java'
        ];
        
        this.optionalFiles = [
            'frontend/package.json',
            'template.yaml',
            'samconfig.toml',
            'backend/src/main/resources/application.yml',
            'backend/src/main/resources/application.properties'
        ];
    }

    /**
     * Main pre-deployment check process
     */
    async runPreDeploymentChecks() {
        console.log('🚀 Starting pre-deployment checks...\n');
        
        try {
            // 1. Validate amplify.yml syntax
            await this.validateAmplifyYmlSyntax();
            
            // 2. Check required files existence
            await this.checkRequiredFiles();
            
            // 3. Validate build commands executability
            if (!this.options.skipCommandCheck) {
                await this.validateBuildCommands();
            }
            
            // 4. Verify dependencies availability
            if (!this.options.skipDependencyCheck) {
                await this.verifyDependencies();
            }
            
            // 5. Generate comprehensive report
            this.generatePreDeploymentReport();
            
            if (this.errors.length === 0) {
                console.log('✅ All pre-deployment checks passed! Ready for deployment.');
                return true;
            } else {
                console.log(`❌ Pre-deployment checks failed with ${this.errors.length} errors.`);
                return false;
            }
            
        } catch (error) {
            console.error('💥 Pre-deployment check process failed:', error.message);
            throw error;
        }
    }

    /**
     * Validate amplify.yml syntax and structure
     */
    async validateAmplifyYmlSyntax() {
        this.log('INFO', '=== Amplify.yml Syntax Validation ===');
        
        if (!fs.existsSync(this.amplifyConfigPath)) {
            this.addError('CONFIGURATION', 'CRITICAL', 
                'amplify.yml file not found', 
                'The amplify.yml configuration file is required for Amplify deployment');
            return;
        }
        
        try {
            const content = fs.readFileSync(this.amplifyConfigPath, 'utf8');
            
            // Basic YAML syntax validation
            const yamlValidation = this.validateYAMLSyntax(content);
            if (!yamlValidation.valid) {
                this.addError('CONFIGURATION', 'ERROR', 
                    'amplify.yml syntax error', 
                    yamlValidation.message);
                return;
            }
            
            // Parse and validate structure
            const config = this.parseAmplifyConfig(content);
            if (!config) {
                this.addError('CONFIGURATION', 'ERROR', 
                    'Failed to parse amplify.yml', 
                    'Unable to parse YAML content');
                return;
            }
            
            // Validate required sections
            this.validateAmplifyConfigStructure(config);
            
            // Validate file references
            this.validateAmplifyFileReferences(config);
            
            // Validate build commands syntax
            this.validateAmplifyBuildCommands(config);
            
            this.addSuccess('amplify.yml syntax validation completed');
            
        } catch (error) {
            this.addError('CONFIGURATION', 'ERROR', 
                'amplify.yml validation failed', 
                error.message);
        }
    }

    /**
     * Validate YAML syntax
     */
    validateYAMLSyntax(content) {
        try {
            const lines = content.split('\n');
            const issues = [];
            
            for (let i = 0; i < lines.length; i++) {
                const line = lines[i];
                const lineNum = i + 1;
                
                // Check for tabs (YAML should use spaces)
                if (line.includes('\t')) {
                    issues.push(`Line ${lineNum}: Contains tabs (use spaces for indentation)`);
                }
                
                // Check for inconsistent indentation
                const indent = line.length - line.trimStart().length;
                if (indent > 0 && indent % 2 !== 0) {
                    issues.push(`Line ${lineNum}: Inconsistent indentation (use 2-space indentation)`);
                }
                
                // Check for trailing spaces
                if (line.endsWith(' ') && line.trim() !== '') {
                    issues.push(`Line ${lineNum}: Contains trailing spaces`);
                }
            }
            
            return {
                valid: issues.length === 0,
                message: issues.join('; ')
            };
            
        } catch (error) {
            return {
                valid: false,
                message: `YAML syntax error: ${error.message}`
            };
        }
    }

    /**
     * Parse amplify.yml configuration
     */
    parseAmplifyConfig(content) {
        try {
            // Simple YAML parser for amplify.yml structure
            const config = {};
            const lines = content.split('\n');
            let currentSection = null;
            let currentSubsection = null;
            let currentPhase = null;
            let currentCommandsSection = null;
            
            for (const line of lines) {
                const trimmed = line.trim();
                if (!trimmed || trimmed.startsWith('#')) continue;
                
                const indent = line.length - line.trimStart().length;
                
                if (indent === 0 && trimmed.includes(':')) {
                    // Top-level section (version, frontend, backend)
                    const [key, value] = trimmed.split(':').map(s => s.trim());
                    if (value) {
                        config[key] = value;
                    } else {
                        config[key] = {};
                        currentSection = key;
                    }
                    currentSubsection = null;
                    currentPhase = null;
                    currentCommandsSection = null;
                } else if (indent === 2 && currentSection && trimmed.includes(':')) {
                    // Second-level section (phases, artifacts, cache)
                    const [key, value] = trimmed.split(':').map(s => s.trim());
                    if (!config[currentSection]) config[currentSection] = {};
                    
                    if (value) {
                        config[currentSection][key] = value;
                    } else {
                        config[currentSection][key] = {};
                        currentSubsection = key;
                    }
                    currentPhase = null;
                    currentCommandsSection = null;
                } else if (indent === 4 && currentSection && currentSubsection && trimmed.includes(':')) {
                    // Third-level section (preBuild, build, postBuild)
                    const [key, value] = trimmed.split(':').map(s => s.trim());
                    if (!config[currentSection][currentSubsection]) {
                        config[currentSection][currentSubsection] = {};
                    }
                    
                    if (value) {
                        config[currentSection][currentSubsection][key] = value;
                    } else {
                        config[currentSection][currentSubsection][key] = {};
                        currentPhase = key;
                    }
                    currentCommandsSection = null;
                } else if (indent === 6 && currentPhase && trimmed === 'commands:') {
                    // Commands section
                    currentCommandsSection = 'commands';
                    if (!config[currentSection][currentSubsection][currentPhase].commands) {
                        config[currentSection][currentSubsection][currentPhase].commands = [];
                    }
                } else if (indent === 8 && trimmed.startsWith('- ') && currentCommandsSection === 'commands') {
                    // Command list item
                    const command = trimmed.substring(2);
                    if (currentSection && currentSubsection && currentPhase) {
                        if (!config[currentSection][currentSubsection][currentPhase].commands) {
                            config[currentSection][currentSubsection][currentPhase].commands = [];
                        }
                        config[currentSection][currentSubsection][currentPhase].commands.push(command);
                    }
                } else if (indent === 6 && trimmed.startsWith('- ') && currentPhase) {
                    // Direct command list (alternative format)
                    const command = trimmed.substring(2);
                    if (currentSection && currentSubsection && currentPhase) {
                        if (!config[currentSection][currentSubsection][currentPhase].commands) {
                            config[currentSection][currentSubsection][currentPhase].commands = [];
                        }
                        config[currentSection][currentSubsection][currentPhase].commands.push(command);
                    }
                }
            }
            
            return config;
            
        } catch (error) {
            console.warn('Failed to parse amplify.yml:', error.message);
            return null;
        }
    }

    /**
     * Validate amplify.yml structure
     */
    validateAmplifyConfigStructure(config) {
        // Check version
        if (!config.version) {
            this.addError('CONFIGURATION', 'ERROR', 
                'Missing version field', 
                'amplify.yml must specify a version');
        } else {
            this.addSuccess(`amplify.yml version: ${config.version}`);
        }
        
        // Check for at least one build configuration
        if (!config.frontend && !config.backend) {
            this.addError('CONFIGURATION', 'ERROR', 
                'No build configuration found', 
                'amplify.yml must contain either frontend or backend configuration');
        }
        
        // Validate frontend configuration
        if (config.frontend) {
            this.validateFrontendConfig(config.frontend);
        }
        
        // Validate backend configuration
        if (config.backend) {
            this.validateBackendConfig(config.backend);
        }
    }

    /**
     * Validate frontend configuration
     */
    validateFrontendConfig(frontendConfig) {
        this.log('INFO', 'Validating frontend configuration...');
        
        // Check phases
        if (!frontendConfig.phases) {
            this.addWarning('Frontend configuration missing phases section');
        } else {
            const phases = frontendConfig.phases;
            
            // Check for build phase
            if (!phases.build) {
                this.addWarning('Frontend missing build phase');
            } else if (!phases.build.commands || phases.build.commands.length === 0) {
                this.addWarning('Frontend build phase has no commands');
            }
        }
        
        // Check artifacts
        if (!frontendConfig.artifacts) {
            this.addWarning('Frontend configuration missing artifacts section');
        } else {
            const artifacts = frontendConfig.artifacts;
            
            if (!artifacts.baseDirectory) {
                this.addWarning('Frontend artifacts missing baseDirectory');
            }
            
            if (!artifacts.files || artifacts.files.length === 0) {
                this.addWarning('Frontend artifacts missing files specification');
            }
        }
        
        this.addSuccess('Frontend configuration structure validated');
    }

    /**
     * Validate backend configuration
     */
    validateBackendConfig(backendConfig) {
        this.log('INFO', 'Validating backend configuration...');
        
        // Check phases
        if (!backendConfig.phases) {
            this.addError('CONFIGURATION', 'ERROR', 
                'Backend configuration missing phases section', 
                'Backend must have build phases defined');
        } else {
            const phases = backendConfig.phases;
            
            // Check for build phase
            if (!phases.build) {
                this.addError('CONFIGURATION', 'ERROR', 
                    'Backend missing build phase', 
                    'Backend must have a build phase with Maven commands');
            } else if (!phases.build.commands || phases.build.commands.length === 0) {
                this.addError('CONFIGURATION', 'ERROR', 
                    'Backend build phase has no commands', 
                    'Backend build phase must contain Maven build commands');
            }
        }
        
        // Check artifacts
        if (!backendConfig.artifacts) {
            this.addError('CONFIGURATION', 'ERROR', 
                'Backend configuration missing artifacts section', 
                'Backend must specify artifacts to deploy');
        }
        
        this.addSuccess('Backend configuration structure validated');
    }

    /**
     * Validate file references in amplify.yml
     */
    validateAmplifyFileReferences(config) {
        this.log('INFO', 'Validating file references in amplify.yml...');
        
        const fileReferences = [];
        
        // Extract file references from artifacts
        if (config.frontend && config.frontend.artifacts) {
            const artifacts = config.frontend.artifacts;
            if (artifacts.baseDirectory) {
                fileReferences.push({
                    type: 'directory',
                    path: artifacts.baseDirectory,
                    context: 'frontend.artifacts.baseDirectory'
                });
            }
        }
        
        if (config.backend && config.backend.artifacts) {
            const artifacts = config.backend.artifacts;
            if (artifacts.baseDirectory) {
                fileReferences.push({
                    type: 'directory',
                    path: artifacts.baseDirectory,
                    context: 'backend.artifacts.baseDirectory'
                });
            }
        }
        
        // Validate each reference
        for (const ref of fileReferences) {
            if (ref.type === 'directory') {
                if (!fs.existsSync(ref.path)) {
                    this.addError('CONFIGURATION', 'ERROR', 
                        `Referenced directory not found: ${ref.path}`, 
                        `Directory referenced in ${ref.context} does not exist`);
                } else {
                    this.addSuccess(`Directory reference validated: ${ref.path}`);
                }
            }
        }
    }

    /**
     * Validate build commands in amplify.yml
     */
    validateAmplifyBuildCommands(config) {
        this.log('INFO', 'Validating build commands syntax...');
        
        const commandSections = [];
        
        // Collect all command sections
        if (config.frontend && config.frontend.phases) {
            Object.entries(config.frontend.phases).forEach(([phase, phaseConfig]) => {
                if (phaseConfig.commands) {
                    commandSections.push({
                        section: `frontend.phases.${phase}`,
                        commands: phaseConfig.commands
                    });
                }
            });
        }
        
        if (config.backend && config.backend.phases) {
            Object.entries(config.backend.phases).forEach(([phase, phaseConfig]) => {
                if (phaseConfig.commands) {
                    commandSections.push({
                        section: `backend.phases.${phase}`,
                        commands: phaseConfig.commands
                    });
                }
            });
        }
        
        // Validate each command section
        for (const section of commandSections) {
            this.validateCommandSection(section.section, section.commands);
        }
    }

    /**
     * Validate command section
     */
    validateCommandSection(sectionName, commands) {
        for (let i = 0; i < commands.length; i++) {
            const command = commands[i];
            const commandNum = i + 1;
            
            // Check for common issues
            if (command.includes('cd ') && !command.includes('&&')) {
                this.addWarning(`${sectionName} command ${commandNum}: 'cd' should be chained with '&&'`);
            }
            
            if (command.includes('exit 1') && !command.includes('||')) {
                this.addWarning(`${sectionName} command ${commandNum}: 'exit 1' should be conditional`);
            }
            
            // Check for required tools
            if (command.includes('mvn') && sectionName.includes('backend')) {
                this.checkedItems.push({
                    type: 'tool_requirement',
                    tool: 'maven',
                    context: sectionName
                });
            }
            
            if (command.includes('java')) {
                this.checkedItems.push({
                    type: 'tool_requirement',
                    tool: 'java',
                    context: sectionName
                });
            }
        }
        
        this.addSuccess(`Command section validated: ${sectionName}`);
    }

    /**
     * Check required files existence
     */
    async checkRequiredFiles() {
        this.log('INFO', '=== Required Files Check ===');
        
        for (const filePath of this.requiredFiles) {
            if (fs.existsSync(filePath)) {
                // Check if it's a directory or file
                const stats = fs.statSync(filePath);
                if (stats.isDirectory()) {
                    this.addSuccess(`Required directory exists: ${filePath}`);
                } else {
                    const size = this.formatFileSize(stats.size);
                    this.addSuccess(`Required file exists: ${filePath} (${size})`);
                }
            } else {
                this.addError('FILES', 'CRITICAL', 
                    `Required file/directory missing: ${filePath}`, 
                    `This file or directory is required for deployment`);
            }
        }
        
        // Check optional files
        this.log('INFO', 'Checking optional files...');
        for (const filePath of this.optionalFiles) {
            if (fs.existsSync(filePath)) {
                const stats = fs.statSync(filePath);
                const size = stats.isDirectory() ? 'directory' : this.formatFileSize(stats.size);
                this.addSuccess(`Optional file found: ${filePath} (${size})`);
            } else {
                this.log('INFO', `Optional file not found: ${filePath}`);
            }
        }
    }

    /**
     * Validate build commands executability
     */
    async validateBuildCommands() {
        this.log('INFO', '=== Build Commands Validation ===');
        
        // Test basic commands availability (Windows compatible)
        const isWindows = process.platform === 'win32';
        const basicCommands = [
            { command: 'echo "test"', name: 'echo', required: true },
            { 
                command: isWindows ? 'dir /b' : 'ls --version', 
                name: isWindows ? 'dir' : 'ls', 
                required: true 
            },
            { 
                command: isWindows ? 'if exist package.json echo exists' : 'test -f package.json', 
                name: isWindows ? 'if exist' : 'test', 
                required: true 
            }
        ];
        
        for (const cmd of basicCommands) {
            try {
                execSync(cmd.command, { stdio: 'pipe', timeout: 5000 });
                this.addSuccess(`Basic command available: ${cmd.name}`);
            } catch (error) {
                if (cmd.required) {
                    this.addError('COMMANDS', 'ERROR', 
                        `Required command not available: ${cmd.name}`, 
                        `Command '${cmd.command}' failed: ${error.message}`);
                } else {
                    this.addWarning(`Optional command not available: ${cmd.name}`);
                }
            }
        }
        
        // Test frontend build commands
        await this.validateFrontendBuildCommands();
        
        // Test backend build commands
        await this.validateBackendBuildCommands();
    }

    /**
     * Validate frontend build commands
     */
    async validateFrontendBuildCommands() {
        this.log('INFO', 'Validating frontend build commands...');
        
        // Check if frontend directory exists
        if (!fs.existsSync('frontend')) {
            this.addError('COMMANDS', 'ERROR', 
                'Frontend directory not found', 
                'Cannot validate frontend build commands without frontend directory');
            return;
        }
        
        // Test file existence checks
        const frontendFiles = [
            'frontend/index.html',
            'frontend/css/style.css',
            'frontend/js/app.js',
            'frontend/js/api-client.js'
        ];
        
        const isWindows = process.platform === 'win32';
        
        for (const file of frontendFiles) {
            try {
                const checkCommand = isWindows ? `if exist ${file} echo exists` : `test -f ${file}`;
                execSync(checkCommand, { stdio: 'pipe' });
                this.addSuccess(`Frontend file check command works: ${file}`);
            } catch (error) {
                this.addError('COMMANDS', 'ERROR', 
                    `Frontend file check failed: ${file}`, 
                    'File existence check command failed');
            }
        }
        
        // Test directory listing
        try {
            const listCommand = isWindows ? 'dir frontend' : 'ls -la frontend/';
            execSync(listCommand, { stdio: 'pipe' });
            this.addSuccess('Frontend directory listing command works');
        } catch (error) {
            this.addError('COMMANDS', 'ERROR', 
                'Frontend directory listing failed', 
                error.message);
        }
    }

    /**
     * Validate backend build commands
     */
    async validateBackendBuildCommands() {
        this.log('INFO', 'Validating backend build commands...');
        
        // Check if backend directory exists
        if (!fs.existsSync('backend')) {
            this.addError('COMMANDS', 'ERROR', 
                'Backend directory not found', 
                'Cannot validate backend build commands without backend directory');
            return;
        }
        
        // Test Java availability
        try {
            const javaVersion = execSync('java -version', { stdio: 'pipe', encoding: 'utf8' });
            this.addSuccess('Java command available');
            this.log('INFO', `Java version check passed`);
        } catch (error) {
            this.addError('COMMANDS', 'CRITICAL', 
                'Java not available', 
                'Java is required for backend build but not found in PATH');
        }
        
        // Test Maven availability
        try {
            execSync('mvn -version', { stdio: 'pipe' });
            this.addSuccess('Maven command available');
        } catch (error) {
            // Try Maven wrapper
            try {
                execSync('cd backend && ./mvnw -version', { stdio: 'pipe' });
                this.addSuccess('Maven wrapper available');
            } catch (wrapperError) {
                this.addError('COMMANDS', 'CRITICAL', 
                    'Maven not available', 
                    'Neither mvn nor mvnw is available for backend build');
            }
        }
        
        // Test pom.xml existence
        const isWindows = process.platform === 'win32';
        try {
            const checkCommand = isWindows ? 'if exist backend\\pom.xml echo exists' : 'test -f backend/pom.xml';
            execSync(checkCommand, { stdio: 'pipe' });
            this.addSuccess('Backend pom.xml exists');
        } catch (error) {
            this.addError('COMMANDS', 'CRITICAL', 
                'Backend pom.xml not found', 
                'Maven build requires pom.xml file');
        }
        
        // Test basic Maven commands (dry run)
        if (fs.existsSync('backend/pom.xml')) {
            const isWindows = process.platform === 'win32';
            try {
                const mvnCommand = isWindows ? 
                    'cd backend & mvn help:effective-pom -q' : 
                    'cd backend && mvn help:effective-pom -q';
                execSync(mvnCommand, { 
                    stdio: 'pipe', 
                    timeout: 30000 
                });
                this.addSuccess('Maven configuration is valid');
            } catch (error) {
                try {
                    const mvnwCommand = isWindows ? 
                        'cd backend & mvnw.cmd help:effective-pom -q' : 
                        'cd backend && ./mvnw help:effective-pom -q';
                    execSync(mvnwCommand, { 
                        stdio: 'pipe', 
                        timeout: 30000 
                    });
                    this.addSuccess('Maven wrapper configuration is valid');
                } catch (wrapperError) {
                    this.addWarning('Maven configuration validation failed (may require dependencies)');
                }
            }
        }
    }

    /**
     * Verify dependencies availability
     */
    async verifyDependencies() {
        this.log('INFO', '=== Dependencies Verification ===');
        
        // Check system dependencies
        await this.checkSystemDependencies();
        
        // Check frontend dependencies
        await this.checkFrontendDependencies();
        
        // Check backend dependencies
        await this.checkBackendDependencies();
    }

    /**
     * Check system dependencies
     */
    async checkSystemDependencies() {
        this.log('INFO', 'Checking system dependencies...');
        
        const systemDeps = [
            { command: 'node --version', name: 'Node.js', required: false },
            { command: 'npm --version', name: 'npm', required: false },
            { command: 'git --version', name: 'Git', required: true }
        ];
        
        for (const dep of systemDeps) {
            try {
                const version = execSync(dep.command, { stdio: 'pipe', encoding: 'utf8' }).trim();
                this.addSuccess(`${dep.name} available: ${version}`);
            } catch (error) {
                if (dep.required) {
                    this.addError('DEPENDENCIES', 'ERROR', 
                        `Required system dependency missing: ${dep.name}`, 
                        `${dep.name} is required but not available`);
                } else {
                    this.addWarning(`Optional system dependency missing: ${dep.name}`);
                }
            }
        }
    }

    /**
     * Check frontend dependencies
     */
    async checkFrontendDependencies() {
        this.log('INFO', 'Checking frontend dependencies...');
        
        const packageJsonPath = 'frontend/package.json';
        if (fs.existsSync(packageJsonPath)) {
            try {
                const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
                
                if (packageJson.dependencies) {
                    const depCount = Object.keys(packageJson.dependencies).length;
                    this.addSuccess(`Frontend has ${depCount} dependencies defined`);
                }
                
                if (packageJson.scripts && packageJson.scripts.build) {
                    this.addSuccess('Frontend build script defined');
                } else {
                    this.addWarning('Frontend package.json missing build script');
                }
                
                // Check if node_modules exists
                if (fs.existsSync('frontend/node_modules')) {
                    this.addSuccess('Frontend node_modules directory exists');
                } else {
                    this.addWarning('Frontend node_modules not found (run npm install)');
                }
                
            } catch (error) {
                this.addError('DEPENDENCIES', 'ERROR', 
                    'Frontend package.json parsing failed', 
                    error.message);
            }
        } else {
            this.log('INFO', 'Frontend package.json not found (static files only)');
        }
    }

    /**
     * Check backend dependencies
     */
    async checkBackendDependencies() {
        this.log('INFO', 'Checking backend dependencies...');
        
        const pomPath = 'backend/pom.xml';
        if (fs.existsSync(pomPath)) {
            try {
                const pomContent = fs.readFileSync(pomPath, 'utf8');
                
                // Check for essential dependencies
                const essentialDeps = [
                    'spring-boot-starter',
                    'spring-boot-starter-web',
                    'aws-lambda-java-core'
                ];
                
                for (const dep of essentialDeps) {
                    if (pomContent.includes(dep)) {
                        this.addSuccess(`Backend dependency found: ${dep}`);
                    } else {
                        this.addWarning(`Backend dependency not found: ${dep}`);
                    }
                }
                
                // Check Java version
                const javaVersionMatch = pomContent.match(/<java\.version>([^<]+)<\/java\.version>/);
                if (javaVersionMatch) {
                    const javaVersion = javaVersionMatch[1];
                    this.addSuccess(`Backend Java version: ${javaVersion}`);
                    
                    if (parseInt(javaVersion) < 17) {
                        this.addWarning('Java version below 17 (consider upgrading)');
                    }
                } else {
                    this.addWarning('Java version not specified in pom.xml');
                }
                
                // Check Maven wrapper
                if (fs.existsSync('backend/mvnw')) {
                    this.addSuccess('Maven wrapper available');
                } else {
                    this.addWarning('Maven wrapper not found');
                }
                
            } catch (error) {
                this.addError('DEPENDENCIES', 'ERROR', 
                    'Backend pom.xml parsing failed', 
                    error.message);
            }
        } else {
            this.addError('DEPENDENCIES', 'CRITICAL', 
                'Backend pom.xml not found', 
                'Maven build requires pom.xml file');
        }
    }

    /**
     * Add error
     */
    addError(category, severity, title, message) {
        const error = {
            category,
            severity,
            title,
            message,
            timestamp: new Date().toISOString()
        };
        
        this.errors.push(error);
        this.log('ERROR', `${title}: ${message}`);
    }

    /**
     * Add warning
     */
    addWarning(message) {
        this.warnings.push({
            message,
            timestamp: new Date().toISOString()
        });
        this.log('WARNING', message);
    }

    /**
     * Add success
     */
    addSuccess(message) {
        this.successes.push({
            message,
            timestamp: new Date().toISOString()
        });
        this.log('SUCCESS', message);
    }

    /**
     * Log message
     */
    log(level, message) {
        const prefix = {
            'SUCCESS': '✅',
            'WARNING': '⚠️',
            'ERROR': '❌',
            'INFO': 'ℹ️'
        }[level] || 'ℹ️';
        
        if (this.options.verbose || level !== 'INFO') {
            console.log(`${prefix} ${message}`);
        }
    }

    /**
     * Generate pre-deployment report
     */
    generatePreDeploymentReport() {
        console.log('\n📊 Pre-deployment Check Report');
        console.log('================================');
        
        console.log(`✅ Successes: ${this.successes.length}`);
        console.log(`⚠️  Warnings: ${this.warnings.length}`);
        console.log(`❌ Errors: ${this.errors.length}`);
        
        // Show critical errors
        const criticalErrors = this.errors.filter(e => e.severity === 'CRITICAL');
        if (criticalErrors.length > 0) {
            console.log('\n🚨 Critical Issues (Must Fix):');
            criticalErrors.forEach((error, index) => {
                console.log(`  ${index + 1}. ${error.title}`);
                console.log(`     ${error.message}`);
            });
        }
        
        // Show regular errors
        const regularErrors = this.errors.filter(e => e.severity !== 'CRITICAL');
        if (regularErrors.length > 0) {
            console.log('\n❌ Errors (Should Fix):');
            regularErrors.forEach((error, index) => {
                console.log(`  ${index + 1}. ${error.title}`);
                console.log(`     ${error.message}`);
            });
        }
        
        // Show warnings
        if (this.warnings.length > 0) {
            console.log('\n⚠️  Warnings (Consider Fixing):');
            this.warnings.slice(0, 5).forEach((warning, index) => {
                console.log(`  ${index + 1}. ${warning.message}`);
            });
            
            if (this.warnings.length > 5) {
                console.log(`  ... and ${this.warnings.length - 5} more warnings`);
            }
        }
        
        // Deployment readiness assessment
        console.log('\n🎯 Deployment Readiness:');
        if (this.errors.length === 0) {
            console.log('  ✅ Ready for deployment!');
        } else if (criticalErrors.length > 0) {
            console.log('  ❌ Not ready - critical issues must be resolved');
        } else {
            console.log('  ⚠️  Deployment possible but errors should be fixed');
        }
        
        // Save detailed report
        this.savePreDeploymentReport();
    }

    /**
     * Save pre-deployment report to file
     */
    savePreDeploymentReport() {
        const reportPath = 'pre-deployment-check-report.json';
        const report = {
            timestamp: new Date().toISOString(),
            summary: {
                successes: this.successes.length,
                warnings: this.warnings.length,
                errors: this.errors.length,
                criticalErrors: this.errors.filter(e => e.severity === 'CRITICAL').length,
                deploymentReady: this.errors.length === 0
            },
            successes: this.successes,
            warnings: this.warnings,
            errors: this.errors,
            checkedItems: this.checkedItems
        };
        
        try {
            fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
            console.log(`\n📄 Detailed report saved to: ${reportPath}`);
        } catch (error) {
            console.warn(`Failed to write report: ${error.message}`);
        }
    }

    /**
     * Format file size for display
     */
    formatFileSize(bytes) {
        const units = ['B', 'KB', 'MB', 'GB'];
        let size = bytes;
        let unitIndex = 0;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return `${size.toFixed(1)} ${units[unitIndex]}`;
    }
}

// Export for use as module
module.exports = PreDeploymentChecker;

// Run if called directly
if (require.main === module) {
    const args = process.argv.slice(2);
    const options = {
        verbose: args.includes('--verbose') || args.includes('-v'),
        skipDependencyCheck: args.includes('--skip-deps'),
        skipCommandCheck: args.includes('--skip-commands')
    };
    
    const checker = new PreDeploymentChecker(options);
    
    checker.runPreDeploymentChecks()
        .then(success => {
            if (success) {
                console.log('\n🎉 Pre-deployment checks completed successfully!');
                console.log('Your application is ready for deployment to AWS Amplify.');
                process.exit(0);
            } else {
                console.log('\n⚠️  Pre-deployment checks found issues.');
                console.log('Please review and fix the issues before deploying.');
                process.exit(1);
            }
        })
        .catch(error => {
            console.error('\n💥 Pre-deployment check failed:', error.message);
            process.exit(1);
        });
}