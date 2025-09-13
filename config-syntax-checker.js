#!/usr/bin/env node

/**
 * Configuration File Syntax Checker
 * 
 * Features:
 * - Comprehensive syntax validation for all configuration files
 * - Detailed error reporting with line numbers and suggestions
 * - Integration with error handling system
 * - Support for YAML, JSON, XML, and TOML formats
 * 
 * Requirements: 1.1, 1.2, 1.3
 */

const fs = require('fs');
const path = require('path');
const ErrorHandler = require('./error-handler');

class ConfigSyntaxChecker {
    constructor(options = {}) {
        this.errorHandler = new ErrorHandler();
        this.errors = [];
        this.warnings = [];
        this.checkedFiles = [];
        
        // Configuration files to check
        this.configFiles = [
            { path: 'amplify.yml', type: 'yaml', required: true },
            { path: 'package.json', type: 'json', required: false },
            { path: 'frontend/package.json', type: 'json', required: false },
            { path: 'backend/pom.xml', type: 'xml', required: true },
            { path: 'template.yaml', type: 'yaml', required: false },
            { path: 'samconfig.toml', type: 'toml', required: false },
            { path: '.gitignore', type: 'text', required: false },
            { path: 'backend/src/main/resources/application.properties', type: 'properties', required: false },
            { path: 'backend/src/main/resources/application.yml', type: 'yaml', required: false }
        ];
        
        this.options = {
            verbose: options.verbose || false,
            stopOnFirstError: options.stopOnFirstError || false,
            ...options
        };
    }

    /**
     * Check all configuration files
     */
    async checkAllConfigurations() {
        console.log('🔍 Starting configuration syntax checking...');
        
        try {
            for (const config of this.configFiles) {
                await this.checkConfigFile(config);
            }
            
            // Generate comprehensive report
            this.generateSyntaxReport();
            
            if (this.errors.length === 0) {
                console.log('✅ All configuration files passed syntax validation');
                return true;
            } else {
                console.log(`❌ Configuration syntax validation failed with ${this.errors.length} errors`);
                return false;
            }
            
        } catch (error) {
            console.error('💥 Configuration syntax checking failed:', error.message);
            throw error;
        }
    }

    /**
     * Check individual configuration file
     */
    async checkConfigFile(config) {
        const { path: filePath, type, required } = config;
        
        if (!fs.existsSync(filePath)) {
            if (required) {
                this.addError('CONFIGURATION', 'CRITICAL', `Required configuration file missing: ${filePath}`, 
                    `The file ${filePath} is required but does not exist`);
            } else {
                this.log(`ℹ️  Optional configuration file not found: ${filePath}`);
            }
            return;
        }
        
        this.log(`🔍 Checking ${type.toUpperCase()} syntax: ${filePath}`);
        
        try {
            const content = fs.readFileSync(filePath, 'utf8');
            const result = await this.validateSyntax(content, type, filePath);
            
            this.checkedFiles.push({
                path: filePath,
                type,
                size: content.length,
                lines: content.split('\n').length,
                valid: result.valid,
                errors: result.errors || [],
                warnings: result.warnings || []
            });
            
            if (result.valid) {
                this.log(`✅ ${filePath}: Syntax valid`);
            } else {
                this.log(`❌ ${filePath}: Syntax errors found`);
                if (this.options.stopOnFirstError) {
                    throw new Error(`Syntax validation failed for ${filePath}`);
                }
            }
            
        } catch (error) {
            this.addError('CONFIGURATION', 'ERROR', `Failed to check ${filePath}`, error.message);
        }
    }

    /**
     * Validate syntax based on file type
     */
    async validateSyntax(content, type, filePath) {
        switch (type) {
            case 'yaml':
                return this.validateYAML(content, filePath);
            case 'json':
                return this.validateJSON(content, filePath);
            case 'xml':
                return this.validateXML(content, filePath);
            case 'toml':
                return this.validateTOML(content, filePath);
            case 'properties':
                return this.validateProperties(content, filePath);
            case 'text':
                return this.validateText(content, filePath);
            default:
                return { valid: true, warnings: [`Unknown file type: ${type}`] };
        }
    }

    /**
     * Validate YAML syntax
     */
    validateYAML(content, filePath) {
        const errors = [];
        const warnings = [];
        
        try {
            // Basic YAML validation
            const lines = content.split('\n');
            let indentStack = [0];
            let inMultilineString = false;
            let multilineStringDelimiter = null;
            
            for (let i = 0; i < lines.length; i++) {
                const line = lines[i];
                const lineNum = i + 1;
                const trimmed = line.trim();
                
                // Skip empty lines and comments
                if (!trimmed || trimmed.startsWith('#')) continue;
                
                // Handle multiline strings
                if (inMultilineString) {
                    if (trimmed === multilineStringDelimiter) {
                        inMultilineString = false;
                        multilineStringDelimiter = null;
                    }
                    continue;
                }
                
                // Check for multiline string start
                if (trimmed.includes('|') || trimmed.includes('>')) {
                    const match = trimmed.match(/[|>][-+]?$/);
                    if (match) {
                        inMultilineString = true;
                        continue;
                    }
                }
                
                // Calculate indentation
                const indent = line.length - line.trimStart().length;
                
                // Check indentation consistency
                if (indent % 2 !== 0) {
                    warnings.push(`Line ${lineNum}: Odd indentation (${indent} spaces). YAML typically uses 2-space indentation.`);
                }
                
                // Check for tabs
                if (line.includes('\t')) {
                    errors.push(`Line ${lineNum}: Tabs are not allowed in YAML. Use spaces for indentation.`);
                }
                
                // Check for key-value pairs
                if (trimmed.includes(':')) {
                    const colonIndex = trimmed.indexOf(':');
                    const key = trimmed.substring(0, colonIndex).trim();
                    const value = trimmed.substring(colonIndex + 1).trim();
                    
                    // Check for invalid key characters
                    if (key.includes('[') || key.includes(']') || key.includes('{') || key.includes('}')) {
                        warnings.push(`Line ${lineNum}: Key contains special characters that may cause issues: "${key}"`);
                    }
                    
                    // Check for unquoted strings that might need quotes
                    if (value && !value.startsWith('"') && !value.startsWith("'") && 
                        (value.includes(':') || value.includes('#') || value.includes('@'))) {
                        warnings.push(`Line ${lineNum}: Value may need quotes: "${value}"`);
                    }
                }
                
                // Check for array items
                if (trimmed.startsWith('- ')) {
                    const arrayValue = trimmed.substring(2).trim();
                    if (arrayValue.includes(':') && !arrayValue.startsWith('"') && !arrayValue.startsWith("'")) {
                        warnings.push(`Line ${lineNum}: Array item with colon may need quotes: "${arrayValue}"`);
                    }
                }
                
                // Check for common YAML mistakes
                if (trimmed.includes('yes') || trimmed.includes('no') || 
                    trimmed.includes('true') || trimmed.includes('false')) {
                    const match = trimmed.match(/:\s*(yes|no|true|false)$/i);
                    if (match && !match[1].match(/^(true|false)$/)) {
                        warnings.push(`Line ${lineNum}: "${match[1]}" will be interpreted as boolean. Use quotes if you want a string.`);
                    }
                }
            }
            
            // Specific validation for amplify.yml
            if (filePath.includes('amplify.yml')) {
                this.validateAmplifyYAML(content, errors, warnings);
            }
            
            return {
                valid: errors.length === 0,
                errors,
                warnings
            };
            
        } catch (error) {
            errors.push(`YAML parsing error: ${error.message}`);
            return { valid: false, errors, warnings };
        }
    }

    /**
     * Validate amplify.yml specific structure
     */
    validateAmplifyYAML(content, errors, warnings) {
        const lines = content.split('\n');
        let hasVersion = false;
        let hasFrontend = false;
        let hasBackend = false;
        
        // Check required sections
        for (const line of lines) {
            const trimmed = line.trim();
            if (trimmed.startsWith('version:')) hasVersion = true;
            if (trimmed.startsWith('frontend:')) hasFrontend = true;
            if (trimmed.startsWith('backend:')) hasBackend = true;
        }
        
        if (!hasVersion) {
            errors.push('amplify.yml: Missing required "version" field');
        }
        
        if (!hasFrontend && !hasBackend) {
            warnings.push('amplify.yml: No frontend or backend configuration found');
        }
        
        // Check for common amplify.yml issues
        if (content.includes('aws-index.html')) {
            errors.push('amplify.yml: References non-existent "aws-index.html" file');
        }
        
        // Check build commands
        const buildCommandPattern = /commands:\s*\n((?:\s*-\s*.+\n)*)/g;
        let match;
        while ((match = buildCommandPattern.exec(content)) !== null) {
            const commands = match[1];
            if (commands.includes('cd ') && !commands.includes('&&')) {
                warnings.push('amplify.yml: "cd" commands should be chained with "&&" or use absolute paths');
            }
        }
    }

    /**
     * Validate JSON syntax
     */
    validateJSON(content, filePath) {
        const errors = [];
        const warnings = [];
        
        try {
            const parsed = JSON.parse(content);
            
            // Specific validation for package.json
            if (filePath.includes('package.json')) {
                this.validatePackageJSON(parsed, errors, warnings, filePath);
            }
            
            return {
                valid: errors.length === 0,
                errors,
                warnings
            };
            
        } catch (error) {
            // Try to provide more specific error information
            const match = error.message.match(/at position (\d+)/);
            if (match) {
                const position = parseInt(match[1]);
                const lines = content.substring(0, position).split('\n');
                const lineNum = lines.length;
                const columnNum = lines[lines.length - 1].length + 1;
                errors.push(`JSON syntax error at line ${lineNum}, column ${columnNum}: ${error.message}`);
            } else {
                errors.push(`JSON syntax error: ${error.message}`);
            }
            
            return { valid: false, errors, warnings };
        }
    }

    /**
     * Validate package.json specific structure
     */
    validatePackageJSON(pkg, errors, warnings, filePath) {
        // Check required fields
        const requiredFields = ['name', 'version'];
        for (const field of requiredFields) {
            if (!pkg[field]) {
                errors.push(`${filePath}: Missing required field "${field}"`);
            }
        }
        
        // Check recommended fields
        const recommendedFields = ['description', 'scripts'];
        for (const field of recommendedFields) {
            if (!pkg[field]) {
                warnings.push(`${filePath}: Missing recommended field "${field}"`);
            }
        }
        
        // Check scripts
        if (pkg.scripts) {
            const recommendedScripts = ['build', 'start'];
            for (const script of recommendedScripts) {
                if (!pkg.scripts[script]) {
                    warnings.push(`${filePath}: Missing recommended script "${script}"`);
                }
            }
        }
        
        // Check dependencies
        if (pkg.dependencies) {
            for (const [dep, version] of Object.entries(pkg.dependencies)) {
                if (!version || version === '') {
                    errors.push(`${filePath}: Dependency "${dep}" has empty version`);
                }
            }
        }
    }

    /**
     * Validate XML syntax
     */
    validateXML(content, filePath) {
        const errors = [];
        const warnings = [];
        
        try {
            // Basic XML validation
            if (!content.includes('<?xml')) {
                warnings.push('XML declaration missing');
            }
            
            // Check for balanced tags
            const openTags = content.match(/<[^/][^>]*>/g) || [];
            const closeTags = content.match(/<\/[^>]*>/g) || [];
            const selfClosingTags = content.match(/<[^>]*\/>/g) || [];
            
            // Simple tag balance check
            const openTagNames = openTags.map(tag => {
                const match = tag.match(/<([^\s>]+)/);
                return match ? match[1] : null;
            }).filter(Boolean);
            
            const closeTagNames = closeTags.map(tag => {
                const match = tag.match(/<\/([^>]+)>/);
                return match ? match[1] : null;
            }).filter(Boolean);
            
            // Check for unmatched tags (simplified)
            for (const tagName of openTagNames) {
                if (!closeTagNames.includes(tagName) && !selfClosingTags.some(tag => tag.includes(tagName))) {
                    warnings.push(`Potentially unmatched opening tag: <${tagName}>`);
                }
            }
            
            // Specific validation for pom.xml
            if (filePath.includes('pom.xml')) {
                this.validatePomXML(content, errors, warnings);
            }
            
            return {
                valid: errors.length === 0,
                errors,
                warnings
            };
            
        } catch (error) {
            errors.push(`XML validation error: ${error.message}`);
            return { valid: false, errors, warnings };
        }
    }

    /**
     * Validate pom.xml specific structure
     */
    validatePomXML(content, errors, warnings) {
        // Check required elements
        const requiredElements = ['<project', '<groupId>', '<artifactId>', '<version>'];
        for (const element of requiredElements) {
            if (!content.includes(element)) {
                errors.push(`pom.xml: Missing required element "${element}"`);
            }
        }
        
        // Check for Spring Boot plugin
        if (!content.includes('spring-boot-maven-plugin')) {
            warnings.push('pom.xml: Spring Boot Maven plugin not found');
        }
        
        // Check for Lambda dependencies
        const lambdaDeps = [
            'aws-lambda-java-core',
            'aws-serverless-java-container'
        ];
        
        for (const dep of lambdaDeps) {
            if (!content.includes(dep)) {
                warnings.push(`pom.xml: Lambda dependency "${dep}" not found`);
            }
        }
        
        // Check Java version
        if (content.includes('<java.version>')) {
            const match = content.match(/<java\.version>([^<]+)<\/java\.version>/);
            if (match) {
                const version = parseInt(match[1]);
                if (version < 17) {
                    warnings.push(`pom.xml: Java version ${version} is below recommended version 17`);
                }
            }
        }
    }

    /**
     * Validate TOML syntax
     */
    validateTOML(content, filePath) {
        const errors = [];
        const warnings = [];
        
        try {
            const lines = content.split('\n');
            
            for (let i = 0; i < lines.length; i++) {
                const line = lines[i];
                const lineNum = i + 1;
                const trimmed = line.trim();
                
                // Skip empty lines and comments
                if (!trimmed || trimmed.startsWith('#')) continue;
                
                // Check section headers
                if (trimmed.startsWith('[') && trimmed.endsWith(']')) {
                    const section = trimmed.slice(1, -1);
                    if (section.includes(' ') && !section.includes('"')) {
                        warnings.push(`Line ${lineNum}: Section name with spaces should be quoted: [${section}]`);
                    }
                }
                
                // Check key-value pairs
                if (trimmed.includes('=')) {
                    const [key, ...valueParts] = trimmed.split('=');
                    const value = valueParts.join('=').trim();
                    
                    if (!key.trim()) {
                        errors.push(`Line ${lineNum}: Empty key before "="`);
                    }
                    
                    if (!value) {
                        errors.push(`Line ${lineNum}: Empty value after "="`);
                    }
                }
            }
            
            return {
                valid: errors.length === 0,
                errors,
                warnings
            };
            
        } catch (error) {
            errors.push(`TOML validation error: ${error.message}`);
            return { valid: false, errors, warnings };
        }
    }

    /**
     * Validate properties file
     */
    validateProperties(content, filePath) {
        const errors = [];
        const warnings = [];
        
        try {
            const lines = content.split('\n');
            
            for (let i = 0; i < lines.length; i++) {
                const line = lines[i];
                const lineNum = i + 1;
                const trimmed = line.trim();
                
                // Skip empty lines and comments
                if (!trimmed || trimmed.startsWith('#') || trimmed.startsWith('!')) continue;
                
                // Check key-value pairs
                if (trimmed.includes('=') || trimmed.includes(':')) {
                    const separator = trimmed.includes('=') ? '=' : ':';
                    const [key, ...valueParts] = trimmed.split(separator);
                    const value = valueParts.join(separator).trim();
                    
                    if (!key.trim()) {
                        errors.push(`Line ${lineNum}: Empty key before "${separator}"`);
                    }
                    
                    // Check for spaces in keys (should be escaped)
                    if (key.includes(' ') && !key.includes('\\ ')) {
                        warnings.push(`Line ${lineNum}: Key contains unescaped spaces: "${key.trim()}"`);
                    }
                } else if (trimmed.length > 0) {
                    warnings.push(`Line ${lineNum}: Line does not appear to be a valid property: "${trimmed}"`);
                }
            }
            
            return {
                valid: errors.length === 0,
                errors,
                warnings
            };
            
        } catch (error) {
            errors.push(`Properties validation error: ${error.message}`);
            return { valid: false, errors, warnings };
        }
    }

    /**
     * Validate text file (basic checks)
     */
    validateText(content, filePath) {
        const warnings = [];
        
        // Check for very long lines
        const lines = content.split('\n');
        for (let i = 0; i < lines.length; i++) {
            if (lines[i].length > 200) {
                warnings.push(`Line ${i + 1}: Very long line (${lines[i].length} characters)`);
            }
        }
        
        // Check for mixed line endings
        if (content.includes('\r\n') && content.includes('\n')) {
            warnings.push('Mixed line endings detected (CRLF and LF)');
        }
        
        return {
            valid: true,
            errors: [],
            warnings
        };
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
        
        // Also add to error handler
        this.errorHandler.addError(category, severity, title, message);
    }

    /**
     * Generate syntax report
     */
    generateSyntaxReport() {
        console.log('\n📊 Configuration Syntax Report');
        console.log('===============================');
        
        console.log(`Files Checked: ${this.checkedFiles.length}`);
        console.log(`Total Errors: ${this.errors.length}`);
        console.log(`Total Warnings: ${this.warnings.length}`);
        
        // File details
        console.log('\n📋 File Details:');
        this.checkedFiles.forEach((file, index) => {
            const status = file.valid ? '✅' : '❌';
            console.log(`  ${index + 1}. ${status} ${file.path} (${file.type.toUpperCase()}, ${file.lines} lines)`);
            
            if (file.errors.length > 0) {
                file.errors.forEach(error => {
                    console.log(`     ❌ ${error}`);
                });
            }
            
            if (file.warnings.length > 0 && this.options.verbose) {
                file.warnings.slice(0, 3).forEach(warning => {
                    console.log(`     ⚠️  ${warning}`);
                });
                if (file.warnings.length > 3) {
                    console.log(`     ... and ${file.warnings.length - 3} more warnings`);
                }
            }
        });
        
        // Summary by file type
        const byType = this.checkedFiles.reduce((acc, file) => {
            if (!acc[file.type]) acc[file.type] = { total: 0, valid: 0 };
            acc[file.type].total++;
            if (file.valid) acc[file.type].valid++;
            return acc;
        }, {});
        
        console.log('\n📊 Summary by File Type:');
        for (const [type, stats] of Object.entries(byType)) {
            console.log(`  ${type.toUpperCase()}: ${stats.valid}/${stats.total} valid`);
        }
        
        // Save detailed report
        this.saveSyntaxReport();
    }

    /**
     * Save syntax report to file
     */
    saveSyntaxReport() {
        const reportPath = 'config-syntax-report.json';
        const report = {
            timestamp: new Date().toISOString(),
            summary: {
                filesChecked: this.checkedFiles.length,
                totalErrors: this.errors.length,
                totalWarnings: this.warnings.length,
                allValid: this.errors.length === 0
            },
            files: this.checkedFiles,
            errors: this.errors,
            warnings: this.warnings
        };
        
        try {
            fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
            console.log(`\n📄 Syntax report saved to: ${reportPath}`);
        } catch (error) {
            console.warn(`Failed to write syntax report: ${error.message}`);
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
module.exports = ConfigSyntaxChecker;

// Run if called directly
if (require.main === module) {
    const args = process.argv.slice(2);
    const options = {
        verbose: args.includes('--verbose') || args.includes('-v'),
        stopOnFirstError: args.includes('--stop-on-error')
    };
    
    const checker = new ConfigSyntaxChecker(options);
    
    checker.checkAllConfigurations()
        .then(success => {
            if (success) {
                console.log('\n✅ Configuration syntax checking completed successfully');
                process.exit(0);
            } else {
                console.log('\n❌ Configuration syntax checking found issues');
                process.exit(1);
            }
        })
        .catch(error => {
            console.error('\n💥 Configuration syntax checking failed:', error.message);
            process.exit(1);
        });
}