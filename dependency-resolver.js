#!/usr/bin/env node

/**
 * Dependency Problem Detection and Resolution Suggestions
 * 
 * Features:
 * - Comprehensive dependency analysis
 * - Detailed resolution suggestions
 * - Version conflict detection
 * - Missing dependency identification
 * - Integration with error handling system
 * 
 * Requirements: 1.1, 1.2, 1.3
 */

const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');
const util = require('util');
const ErrorHandler = require('./error-handler');

const execAsync = util.promisify(exec);

class DependencyResolver {
    constructor(options = {}) {
        this.errorHandler = new ErrorHandler();
        this.dependencies = {
            system: [],
            nodejs: [],
            java: [],
            aws: []
        };
        this.issues = [];
        this.resolutions = [];
        
        this.options = {
            checkVersions: options.checkVersions !== false,
            suggestUpdates: options.suggestUpdates !== false,
            verbose: options.verbose || false,
            ...options
        };
        
        // Dependency requirements
        this.requirements = {
            system: [
                { name: 'Node.js', command: 'node --version', minVersion: '16.0.0', required: true },
                { name: 'npm', command: 'npm --version', minVersion: '8.0.0', required: true },
                { name: 'Java', command: 'java -version', minVersion: '17.0.0', required: true },
                { name: 'Maven', command: 'mvn --version', minVersion: '3.6.0', required: false },
                { name: 'AWS CLI', command: 'aws --version', minVersion: '2.0.0', required: true }
            ],
            nodejs: {
                required: [],
                recommended: ['express', 'cors', 'dotenv']
            },
            java: {
                required: [
                    'org.springframework.boot:spring-boot-starter',
                    'org.springframework.boot:spring-boot-starter-web'
                ],
                lambda: [
                    'com.amazonaws:aws-lambda-java-core',
                    'com.amazonaws:aws-serverless-java-container-spring'
                ]
            }
        };
    }

    /**
     * Analyze all dependencies
     */
    async analyzeDependencies() {
        console.log('🔍 Starting comprehensive dependency analysis...');
        
        try {
            // Analyze system dependencies
            await this.analyzeSystemDependencies();
            
            // Analyze Node.js dependencies
            await this.analyzeNodeJSDependencies();
            
            // Analyze Java dependencies
            await this.analyzeJavaDependencies();
            
            // Analyze AWS dependencies
            await this.analyzeAWSDependencies();
            
            // Generate resolution suggestions
            this.generateResolutionSuggestions();
            
            // Generate comprehensive report
            this.generateDependencyReport();
            
            if (this.issues.filter(i => i.severity === 'CRITICAL').length === 0) {
                console.log('✅ Dependency analysis completed successfully');
                return true;
            } else {
                console.log('❌ Critical dependency issues found');
                return false;
            }
            
        } catch (error) {
            console.error('💥 Dependency analysis failed:', error.message);
            throw error;
        }
    }

    /**
     * Analyze system dependencies
     */
    async analyzeSystemDependencies() {
        this.log('🖥️  Analyzing system dependencies...');
        
        for (const dep of this.requirements.system) {
            try {
                const result = await this.checkSystemDependency(dep);
                this.dependencies.system.push(result);
                
                if (!result.installed && dep.required) {
                    this.addIssue('DEPENDENCY', 'CRITICAL', 
                        `Required system dependency missing: ${dep.name}`,
                        `${dep.name} is required but not installed or not in PATH`);
                } else if (!result.installed) {
                    this.addIssue('DEPENDENCY', 'WARNING', 
                        `Optional system dependency missing: ${dep.name}`,
                        `${dep.name} is recommended but not installed`);
                } else if (result.versionIssue) {
                    this.addIssue('DEPENDENCY', 'ERROR', 
                        `System dependency version issue: ${dep.name}`,
                        `${dep.name} version ${result.version} is below minimum ${dep.minVersion}`);
                }
                
            } catch (error) {
                this.addIssue('DEPENDENCY', 'ERROR', 
                    `Failed to check system dependency: ${dep.name}`,
                    error.message);
            }
        }
    }

    /**
     * Check individual system dependency
     */
    async checkSystemDependency(dep) {
        try {
            const { stdout, stderr } = await execAsync(dep.command);
            const output = stdout || stderr;
            const version = this.extractVersion(output);
            
            const result = {
                name: dep.name,
                command: dep.command,
                installed: true,
                version: version,
                versionIssue: false,
                output: output.split('\n')[0]
            };
            
            if (dep.minVersion && version) {
                result.versionIssue = this.compareVersions(version, dep.minVersion) < 0;
            }
            
            this.log(`✅ ${dep.name}: ${version || 'installed'}`);
            return result;
            
        } catch (error) {
            this.log(`❌ ${dep.name}: not found`);
            return {
                name: dep.name,
                command: dep.command,
                installed: false,
                version: null,
                versionIssue: false,
                error: error.message
            };
        }
    }

    /**
     * Analyze Node.js dependencies
     */
    async analyzeNodeJSDependencies() {
        this.log('📦 Analyzing Node.js dependencies...');
        
        const packageJsonPaths = ['package.json', 'frontend/package.json'];
        
        for (const packagePath of packageJsonPaths) {
            if (fs.existsSync(packagePath)) {
                await this.analyzePackageJson(packagePath);
            }
        }
    }

    /**
     * Analyze package.json file
     */
    async analyzePackageJson(packagePath) {
        try {
            const content = fs.readFileSync(packagePath, 'utf8');
            const pkg = JSON.parse(content);
            const nodeModulesPath = path.join(path.dirname(packagePath), 'node_modules');
            
            const analysis = {
                path: packagePath,
                name: pkg.name,
                version: pkg.version,
                dependencies: pkg.dependencies || {},
                devDependencies: pkg.devDependencies || {},
                scripts: pkg.scripts || {},
                nodeModulesExists: fs.existsSync(nodeModulesPath),
                issues: []
            };
            
            // Check if node_modules exists
            if (!analysis.nodeModulesExists) {
                this.addIssue('DEPENDENCY', 'ERROR', 
                    `Node modules not installed for ${packagePath}`,
                    `Run 'npm install' in ${path.dirname(packagePath)} directory`);
                analysis.issues.push('node_modules missing');
            }
            
            // Check for missing dependencies
            if (analysis.nodeModulesExists) {
                for (const [depName, version] of Object.entries(analysis.dependencies)) {
                    const depPath = path.join(nodeModulesPath, depName);
                    if (!fs.existsSync(depPath)) {
                        this.addIssue('DEPENDENCY', 'WARNING', 
                            `Missing Node.js dependency: ${depName}`,
                            `Dependency ${depName}@${version} is declared but not installed`);
                        analysis.issues.push(`missing: ${depName}`);
                    }
                }
            }
            
            // Check for security vulnerabilities (simplified)
            await this.checkNodeSecurityIssues(packagePath, analysis);
            
            // Check for outdated dependencies
            if (this.options.checkVersions) {
                await this.checkOutdatedNodeDependencies(packagePath, analysis);
            }
            
            this.dependencies.nodejs.push(analysis);
            this.log(`📦 Analyzed: ${packagePath} (${Object.keys(analysis.dependencies).length} dependencies)`);
            
        } catch (error) {
            this.addIssue('DEPENDENCY', 'ERROR', 
                `Failed to analyze ${packagePath}`,
                error.message);
        }
    }

    /**
     * Check Node.js security issues
     */
    async checkNodeSecurityIssues(packagePath, analysis) {
        try {
            const { stdout } = await execAsync('npm audit --json', { 
                cwd: path.dirname(packagePath),
                timeout: 30000
            });
            
            const auditResult = JSON.parse(stdout);
            if (auditResult.vulnerabilities && Object.keys(auditResult.vulnerabilities).length > 0) {
                const vulnCount = Object.keys(auditResult.vulnerabilities).length;
                this.addIssue('DEPENDENCY', 'WARNING', 
                    `Security vulnerabilities found in ${packagePath}`,
                    `${vulnCount} vulnerabilities detected. Run 'npm audit fix' to resolve`);
                analysis.issues.push(`${vulnCount} vulnerabilities`);
            }
            
        } catch (error) {
            // npm audit might fail for various reasons, don't treat as critical
            this.log(`ℹ️  Could not check security issues for ${packagePath}: ${error.message}`);
        }
    }

    /**
     * Check outdated Node.js dependencies
     */
    async checkOutdatedNodeDependencies(packagePath, analysis) {
        try {
            const { stdout } = await execAsync('npm outdated --json', { 
                cwd: path.dirname(packagePath),
                timeout: 30000
            });
            
            if (stdout.trim()) {
                const outdated = JSON.parse(stdout);
                const outdatedCount = Object.keys(outdated).length;
                
                if (outdatedCount > 0) {
                    this.addIssue('DEPENDENCY', 'INFO', 
                        `Outdated dependencies in ${packagePath}`,
                        `${outdatedCount} dependencies have newer versions available`);
                    analysis.issues.push(`${outdatedCount} outdated`);
                }
            }
            
        } catch (error) {
            // npm outdated returns non-zero exit code when outdated packages exist
            if (error.stdout) {
                try {
                    const outdated = JSON.parse(error.stdout);
                    const outdatedCount = Object.keys(outdated).length;
                    this.addIssue('DEPENDENCY', 'INFO', 
                        `Outdated dependencies in ${packagePath}`,
                        `${outdatedCount} dependencies have newer versions available`);
                    analysis.issues.push(`${outdatedCount} outdated`);
                } catch (parseError) {
                    // Ignore parsing errors
                }
            }
        }
    }

    /**
     * Analyze Java dependencies
     */
    async analyzeJavaDependencies() {
        this.log('☕ Analyzing Java dependencies...');
        
        const pomPath = path.join('backend', 'pom.xml');
        if (!fs.existsSync(pomPath)) {
            this.addIssue('DEPENDENCY', 'CRITICAL', 
                'Maven POM file missing',
                'backend/pom.xml is required for Java dependency management');
            return;
        }
        
        await this.analyzePomXml(pomPath);
    }

    /**
     * Analyze pom.xml file
     */
    async analyzePomXml(pomPath) {
        try {
            const content = fs.readFileSync(pomPath, 'utf8');
            
            const analysis = {
                path: pomPath,
                dependencies: this.extractMavenDependencies(content),
                plugins: this.extractMavenPlugins(content),
                properties: this.extractMavenProperties(content),
                issues: []
            };
            
            // Check for required Spring Boot dependencies
            const requiredDeps = this.requirements.java.required;
            for (const requiredDep of requiredDeps) {
                const found = analysis.dependencies.some(dep => 
                    dep.groupId && dep.artifactId && 
                    `${dep.groupId}:${dep.artifactId}`.includes(requiredDep.split(':')[1])
                );
                
                if (!found) {
                    this.addIssue('DEPENDENCY', 'ERROR', 
                        `Missing required Java dependency: ${requiredDep}`,
                        `Add ${requiredDep} to your pom.xml dependencies`);
                    analysis.issues.push(`missing: ${requiredDep}`);
                }
            }
            
            // Check for Lambda dependencies
            const lambdaDeps = this.requirements.java.lambda;
            let hasLambdaDeps = false;
            for (const lambdaDep of lambdaDeps) {
                const found = analysis.dependencies.some(dep => 
                    dep.groupId && dep.artifactId && 
                    `${dep.groupId}:${dep.artifactId}`.includes(lambdaDep.split(':')[1])
                );
                if (found) hasLambdaDeps = true;
            }
            
            if (!hasLambdaDeps) {
                this.addIssue('DEPENDENCY', 'WARNING', 
                    'No AWS Lambda dependencies found',
                    'Add AWS Lambda dependencies for serverless deployment');
                analysis.issues.push('no lambda dependencies');
            }
            
            // Check for Spring Boot Maven plugin
            const hasSpringBootPlugin = analysis.plugins.some(plugin => 
                plugin.artifactId && plugin.artifactId.includes('spring-boot-maven-plugin')
            );
            
            if (!hasSpringBootPlugin) {
                this.addIssue('DEPENDENCY', 'ERROR', 
                    'Spring Boot Maven plugin missing',
                    'Add spring-boot-maven-plugin to build plugins section');
                analysis.issues.push('missing spring-boot plugin');
            }
            
            // Check Java version
            const javaVersion = analysis.properties['java.version'] || analysis.properties['maven.compiler.source'];
            if (javaVersion) {
                const version = parseInt(javaVersion);
                if (version < 17) {
                    this.addIssue('DEPENDENCY', 'WARNING', 
                        `Java version ${version} is below recommended`,
                        'Consider upgrading to Java 17 or later for better performance and security');
                    analysis.issues.push(`java version ${version}`);
                }
            }
            
            // Check for dependency management
            await this.checkMavenDependencyManagement(pomPath, analysis);
            
            this.dependencies.java.push(analysis);
            this.log(`☕ Analyzed: ${pomPath} (${analysis.dependencies.length} dependencies)`);
            
        } catch (error) {
            this.addIssue('DEPENDENCY', 'ERROR', 
                `Failed to analyze ${pomPath}`,
                error.message);
        }
    }

    /**
     * Extract Maven dependencies from pom.xml
     */
    extractMavenDependencies(content) {
        const dependencies = [];
        const depPattern = /<dependency>\s*<groupId>([^<]+)<\/groupId>\s*<artifactId>([^<]+)<\/artifactId>\s*(?:<version>([^<]+)<\/version>)?\s*(?:<scope>([^<]+)<\/scope>)?\s*<\/dependency>/g;
        
        let match;
        while ((match = depPattern.exec(content)) !== null) {
            dependencies.push({
                groupId: match[1],
                artifactId: match[2],
                version: match[3] || 'inherited',
                scope: match[4] || 'compile'
            });
        }
        
        return dependencies;
    }

    /**
     * Extract Maven plugins from pom.xml
     */
    extractMavenPlugins(content) {
        const plugins = [];
        const pluginPattern = /<plugin>\s*(?:<groupId>([^<]+)<\/groupId>\s*)?<artifactId>([^<]+)<\/artifactId>\s*(?:<version>([^<]+)<\/version>)?\s*(?:<configuration>[\s\S]*?<\/configuration>)?\s*<\/plugin>/g;
        
        let match;
        while ((match = pluginPattern.exec(content)) !== null) {
            plugins.push({
                groupId: match[1] || 'org.apache.maven.plugins',
                artifactId: match[2],
                version: match[3] || 'inherited'
            });
        }
        
        return plugins;
    }

    /**
     * Extract Maven properties from pom.xml
     */
    extractMavenProperties(content) {
        const properties = {};
        const propPattern = /<properties>\s*([\s\S]*?)\s*<\/properties>/;
        const match = content.match(propPattern);
        
        if (match) {
            const propsContent = match[1];
            const propItemPattern = /<([^>]+)>([^<]+)<\/\1>/g;
            
            let propMatch;
            while ((propMatch = propItemPattern.exec(propsContent)) !== null) {
                properties[propMatch[1]] = propMatch[2];
            }
        }
        
        return properties;
    }

    /**
     * Check Maven dependency management
     */
    async checkMavenDependencyManagement(pomPath, analysis) {
        try {
            // Check if Maven wrapper exists
            const backendDir = path.dirname(pomPath);
            const mvnwPath = path.join(backendDir, process.platform === 'win32' ? 'mvnw.cmd' : 'mvnw');
            
            if (!fs.existsSync(mvnwPath)) {
                this.addIssue('DEPENDENCY', 'WARNING', 
                    'Maven wrapper not found',
                    'Consider using Maven wrapper (mvnw) for consistent builds');
                analysis.issues.push('no maven wrapper');
            }
            
            // Check Maven local repository
            const { stdout } = await execAsync('mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout', 
                { cwd: backendDir, timeout: 30000 });
            
            const localRepo = stdout.trim();
            if (localRepo && fs.existsSync(localRepo)) {
                this.log(`📁 Maven local repository: ${localRepo}`);
            } else {
                this.addIssue('DEPENDENCY', 'INFO', 
                    'Maven local repository not found',
                    'Dependencies will be downloaded on first build');
                analysis.issues.push('no local repository');
            }
            
        } catch (error) {
            this.log(`ℹ️  Could not check Maven dependency management: ${error.message}`);
        }
    }

    /**
     * Analyze AWS dependencies
     */
    async analyzeAWSDependencies() {
        this.log('☁️  Analyzing AWS dependencies...');
        
        // Check AWS CLI configuration
        await this.checkAWSCLIConfiguration();
        
        // Check AWS SDK dependencies
        await this.checkAWSSDKDependencies();
    }

    /**
     * Check AWS CLI configuration
     */
    async checkAWSCLIConfiguration() {
        try {
            // Check AWS CLI version
            const { stdout } = await execAsync('aws --version');
            const version = this.extractVersion(stdout);
            
            this.dependencies.aws.push({
                name: 'AWS CLI',
                version: version,
                installed: true
            });
            
            // Check AWS credentials
            try {
                await execAsync('aws sts get-caller-identity');
                this.log('✅ AWS credentials configured');
            } catch (credError) {
                this.addIssue('DEPENDENCY', 'CRITICAL', 
                    'AWS credentials not configured',
                    'Run "aws configure" to set up your AWS credentials for deployment');
            }
            
            // Check AWS region
            try {
                const { stdout: region } = await execAsync('aws configure get region');
                if (!region.trim()) {
                    this.addIssue('DEPENDENCY', 'WARNING', 
                        'AWS region not configured',
                        'Set default AWS region with "aws configure set region us-east-1"');
                } else {
                    this.log(`🌍 AWS region: ${region.trim()}`);
                }
            } catch (error) {
                this.addIssue('DEPENDENCY', 'WARNING', 
                    'Could not determine AWS region',
                    'Ensure AWS region is configured');
            }
            
        } catch (error) {
            this.addIssue('DEPENDENCY', 'CRITICAL', 
                'AWS CLI not found',
                'Install AWS CLI from https://aws.amazon.com/cli/');
        }
    }

    /**
     * Check AWS SDK dependencies
     */
    async checkAWSSDKDependencies() {
        // Check for AWS SDK in Java dependencies
        const javaDeps = this.dependencies.java;
        for (const analysis of javaDeps) {
            const hasAWSSDK = analysis.dependencies.some(dep => 
                dep.groupId && dep.groupId.includes('amazonaws')
            );
            
            if (!hasAWSSDK) {
                this.addIssue('DEPENDENCY', 'INFO', 
                    'No AWS SDK dependencies found in Java project',
                    'Consider adding AWS SDK dependencies if you need to interact with AWS services');
            }
        }
    }

    /**
     * Generate resolution suggestions
     */
    generateResolutionSuggestions() {
        this.log('💡 Generating resolution suggestions...');
        
        // Group issues by category
        const issuesByCategory = this.groupIssuesByCategory();
        
        for (const [category, issues] of Object.entries(issuesByCategory)) {
            const resolution = this.createResolutionForCategory(category, issues);
            if (resolution) {
                this.resolutions.push(resolution);
            }
        }
    }

    /**
     * Group issues by category
     */
    groupIssuesByCategory() {
        return this.issues.reduce((groups, issue) => {
            const key = `${issue.category}-${issue.title.split(':')[0]}`;
            if (!groups[key]) groups[key] = [];
            groups[key].push(issue);
            return groups;
        }, {});
    }

    /**
     * Create resolution for category
     */
    createResolutionForCategory(category, issues) {
        const firstIssue = issues[0];
        const severity = issues.some(i => i.severity === 'CRITICAL') ? 'CRITICAL' : 
                        issues.some(i => i.severity === 'ERROR') ? 'HIGH' : 'MEDIUM';
        
        // System dependency resolutions
        if (category.includes('system dependency missing')) {
            return {
                category: 'SYSTEM_DEPENDENCIES',
                severity,
                title: 'Install Missing System Dependencies',
                description: `${issues.length} system dependencies need to be installed`,
                steps: this.getSystemDependencyInstallSteps(issues),
                automated: false,
                estimatedTime: '10-30 minutes'
            };
        }
        
        // Node.js dependency resolutions
        if (category.includes('Node modules not installed')) {
            return {
                category: 'NODEJS_DEPENDENCIES',
                severity,
                title: 'Install Node.js Dependencies',
                description: 'Node.js dependencies need to be installed',
                steps: [
                    'Navigate to the project directory',
                    'Run: npm install',
                    'If using frontend directory: cd frontend && npm install',
                    'Verify installation: npm list'
                ],
                automated: true,
                command: 'npm install && cd frontend && npm install',
                estimatedTime: '2-5 minutes'
            };
        }
        
        // Java dependency resolutions
        if (category.includes('Java dependency')) {
            return {
                category: 'JAVA_DEPENDENCIES',
                severity,
                title: 'Fix Java Dependencies',
                description: 'Java dependencies need to be added or updated',
                steps: this.getJavaDependencyFixSteps(issues),
                automated: false,
                estimatedTime: '5-15 minutes'
            };
        }
        
        // AWS dependency resolutions
        if (category.includes('AWS')) {
            return {
                category: 'AWS_DEPENDENCIES',
                severity,
                title: 'Configure AWS Dependencies',
                description: 'AWS configuration needs to be set up',
                steps: this.getAWSDependencyFixSteps(issues),
                automated: false,
                estimatedTime: '5-10 minutes'
            };
        }
        
        return null;
    }

    /**
     * Get system dependency install steps
     */
    getSystemDependencyInstallSteps(issues) {
        const steps = [];
        
        for (const issue of issues) {
            const depName = issue.title.split(':')[1].trim();
            
            switch (depName) {
                case 'Node.js':
                    steps.push('Install Node.js from https://nodejs.org/ or use nvm');
                    break;
                case 'Java':
                    steps.push('Install Java 17+ from https://adoptium.net/');
                    break;
                case 'Maven':
                    steps.push('Install Maven from https://maven.apache.org/ or use included mvnw');
                    break;
                case 'AWS CLI':
                    steps.push('Install AWS CLI from https://aws.amazon.com/cli/');
                    break;
                default:
                    steps.push(`Install ${depName} according to official documentation`);
            }
        }
        
        steps.push('Restart terminal/command prompt after installation');
        steps.push('Verify installations by running version commands');
        
        return steps;
    }

    /**
     * Get Java dependency fix steps
     */
    getJavaDependencyFixSteps(issues) {
        const steps = [
            'Open backend/pom.xml in your editor',
            'Add missing dependencies to the <dependencies> section:'
        ];
        
        for (const issue of issues) {
            if (issue.message.includes('spring-boot-starter')) {
                steps.push('  - Add Spring Boot starter dependencies');
            }
            if (issue.message.includes('aws-lambda')) {
                steps.push('  - Add AWS Lambda dependencies for serverless deployment');
            }
            if (issue.message.includes('spring-boot-maven-plugin')) {
                steps.push('  - Add Spring Boot Maven plugin to <build><plugins> section');
            }
        }
        
        steps.push('Save pom.xml');
        steps.push('Run: mvn clean compile to verify dependencies');
        
        return steps;
    }

    /**
     * Get AWS dependency fix steps
     */
    getAWSDependencyFixSteps(issues) {
        const steps = [];
        
        for (const issue of issues) {
            if (issue.message.includes('credentials')) {
                steps.push('Configure AWS credentials:');
                steps.push('  - Run: aws configure');
                steps.push('  - Enter your AWS Access Key ID');
                steps.push('  - Enter your AWS Secret Access Key');
                steps.push('  - Set default region (e.g., us-east-1)');
            }
            if (issue.message.includes('region')) {
                steps.push('Set AWS region: aws configure set region us-east-1');
            }
        }
        
        steps.push('Verify configuration: aws sts get-caller-identity');
        
        return steps;
    }

    /**
     * Extract version from command output
     */
    extractVersion(output) {
        const versionPatterns = [
            /v?(\d+\.\d+\.\d+)/,
            /version\s+(\d+\.\d+\.\d+)/i,
            /(\d+\.\d+\.\d+)/
        ];
        
        for (const pattern of versionPatterns) {
            const match = output.match(pattern);
            if (match) {
                return match[1];
            }
        }
        
        return null;
    }

    /**
     * Compare version strings
     */
    compareVersions(version1, version2) {
        const v1Parts = version1.split('.').map(Number);
        const v2Parts = version2.split('.').map(Number);
        
        for (let i = 0; i < Math.max(v1Parts.length, v2Parts.length); i++) {
            const v1Part = v1Parts[i] || 0;
            const v2Part = v2Parts[i] || 0;
            
            if (v1Part < v2Part) return -1;
            if (v1Part > v2Part) return 1;
        }
        
        return 0;
    }

    /**
     * Add issue
     */
    addIssue(category, severity, title, message) {
        const issue = {
            category,
            severity,
            title,
            message,
            timestamp: new Date().toISOString()
        };
        
        this.issues.push(issue);
        
        // Also add to error handler
        this.errorHandler.addError(category, severity, title, message);
    }

    /**
     * Generate dependency report
     */
    generateDependencyReport() {
        console.log('\n📊 Dependency Analysis Report');
        console.log('==============================');
        
        // Summary
        const criticalIssues = this.issues.filter(i => i.severity === 'CRITICAL').length;
        const errorIssues = this.issues.filter(i => i.severity === 'ERROR').length;
        const warningIssues = this.issues.filter(i => i.severity === 'WARNING').length;
        
        console.log(`Critical Issues: ${criticalIssues}`);
        console.log(`Errors: ${errorIssues}`);
        console.log(`Warnings: ${warningIssues}`);
        console.log(`Total Issues: ${this.issues.length}`);
        
        // System dependencies
        console.log('\n🖥️  System Dependencies:');
        this.dependencies.system.forEach(dep => {
            const status = dep.installed ? '✅' : '❌';
            const version = dep.version ? ` (${dep.version})` : '';
            console.log(`  ${status} ${dep.name}${version}`);
        });
        
        // Node.js dependencies
        if (this.dependencies.nodejs.length > 0) {
            console.log('\n📦 Node.js Projects:');
            this.dependencies.nodejs.forEach(proj => {
                const status = proj.nodeModulesExists ? '✅' : '❌';
                const depCount = Object.keys(proj.dependencies).length;
                console.log(`  ${status} ${proj.path} (${depCount} dependencies)`);
                if (proj.issues.length > 0) {
                    proj.issues.forEach(issue => console.log(`     ⚠️  ${issue}`));
                }
            });
        }
        
        // Java dependencies
        if (this.dependencies.java.length > 0) {
            console.log('\n☕ Java Projects:');
            this.dependencies.java.forEach(proj => {
                const depCount = proj.dependencies.length;
                console.log(`  📄 ${proj.path} (${depCount} dependencies)`);
                if (proj.issues.length > 0) {
                    proj.issues.forEach(issue => console.log(`     ⚠️  ${issue}`));
                }
            });
        }
        
        // Resolution suggestions
        if (this.resolutions.length > 0) {
            console.log('\n💡 Resolution Suggestions:');
            this.resolutions.forEach((resolution, index) => {
                console.log(`\n${index + 1}. ${resolution.title} (${resolution.severity} priority)`);
                console.log(`   ${resolution.description}`);
                console.log(`   Estimated time: ${resolution.estimatedTime}`);
                
                if (resolution.automated && resolution.command) {
                    console.log(`   🤖 Automated fix: ${resolution.command}`);
                } else {
                    console.log('   📋 Manual steps:');
                    resolution.steps.forEach(step => console.log(`     • ${step}`));
                }
            });
        }
        
        // Save detailed report
        this.saveDependencyReport();
    }

    /**
     * Save dependency report to file
     */
    saveDependencyReport() {
        const reportPath = 'dependency-analysis-report.json';
        const report = {
            timestamp: new Date().toISOString(),
            summary: {
                totalIssues: this.issues.length,
                criticalIssues: this.issues.filter(i => i.severity === 'CRITICAL').length,
                errorIssues: this.issues.filter(i => i.severity === 'ERROR').length,
                warningIssues: this.issues.filter(i => i.severity === 'WARNING').length
            },
            dependencies: this.dependencies,
            issues: this.issues,
            resolutions: this.resolutions
        };
        
        try {
            fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
            console.log(`\n📄 Dependency report saved to: ${reportPath}`);
        } catch (error) {
            console.warn(`Failed to write dependency report: ${error.message}`);
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
module.exports = DependencyResolver;

// Run if called directly
if (require.main === module) {
    const args = process.argv.slice(2);
    const options = {
        verbose: args.includes('--verbose') || args.includes('-v'),
        checkVersions: !args.includes('--no-version-check'),
        suggestUpdates: !args.includes('--no-updates')
    };
    
    const resolver = new DependencyResolver(options);
    
    resolver.analyzeDependencies()
        .then(success => {
            if (success) {
                console.log('\n✅ Dependency analysis completed successfully');
                process.exit(0);
            } else {
                console.log('\n❌ Critical dependency issues found');
                process.exit(1);
            }
        })
        .catch(error => {
            console.error('\n💥 Dependency analysis failed:', error.message);
            process.exit(1);
        });
}