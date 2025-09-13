#!/usr/bin/env node

/**
 * Frontend File Validation Script
 * 
 * Validates static files for integrity and consistency
 * before the build process begins
 */

const fs = require('fs');
const path = require('path');

class FileValidator {
    constructor() {
        this.sourceDir = __dirname;
        this.errors = [];
        this.warnings = [];
        
        // File validation rules
        this.validationRules = {
            'index.html': {
                required: true,
                checks: [
                    { type: 'contains', value: '<!DOCTYPE html>', message: 'Missing DOCTYPE declaration' },
                    { type: 'contains', value: '<title>', message: 'Missing title tag' },
                    { type: 'contains', value: 'css/style.css', message: 'Missing CSS reference' },
                    { type: 'encoding', value: 'utf8', message: 'File should be UTF-8 encoded' }
                ]
            },
            'css/style.css': {
                required: true,
                checks: [
                    { type: 'notEmpty', message: 'CSS file should not be empty' },
                    { type: 'cssValidation', message: 'CSS syntax validation failed' }
                ]
            },
            'js/app.js': {
                required: true,
                checks: [
                    { type: 'notEmpty', message: 'JavaScript file should not be empty' },
                    { type: 'jsBasicValidation', message: 'JavaScript basic validation failed' }
                ]
            },
            'js/api-client.js': {
                required: true,
                checks: [
                    { type: 'notEmpty', message: 'JavaScript file should not be empty' },
                    { type: 'jsBasicValidation', message: 'JavaScript basic validation failed' }
                ]
            },
            'package.json': {
                required: true,
                checks: [
                    { type: 'jsonValidation', message: 'Invalid JSON syntax' },
                    { type: 'packageJsonValidation', message: 'Invalid package.json structure' }
                ]
            }
        };
        
        // Optional files that should be validated if present
        this.optionalFiles = [
            'js/auth-manager.js',
            'js/aws-config.js',
            'js/data-manager.js'
        ];
    }

    /**
     * Main validation process
     */
    async validate() {
        console.log('🔍 Starting file validation...');
        
        try {
            // Validate required files
            await this.validateRequiredFiles();
            
            // Validate optional files
            await this.validateOptionalFiles();
            
            // Cross-reference validation
            await this.validateCrossReferences();
            
            // Generate validation report
            this.generateValidationReport();
            
            if (this.errors.length > 0) {
                console.log('❌ Validation failed with errors');
                process.exit(1);
            } else {
                console.log('✅ File validation completed successfully');
                return true;
            }
            
        } catch (error) {
            console.error('❌ Validation process failed:', error.message);
            process.exit(1);
        }
    }

    /**
     * Validate required files
     */
    async validateRequiredFiles() {
        console.log('📋 Validating required files...');
        
        for (const [fileName, rules] of Object.entries(this.validationRules)) {
            const filePath = path.join(this.sourceDir, fileName);
            
            if (!fs.existsSync(filePath)) {
                this.errors.push(`Required file missing: ${fileName}`);
                continue;
            }
            
            await this.validateFile(filePath, fileName, rules);
        }
    }

    /**
     * Validate optional files
     */
    async validateOptionalFiles() {
        console.log('📋 Validating optional files...');
        
        for (const fileName of this.optionalFiles) {
            const filePath = path.join(this.sourceDir, fileName);
            
            if (fs.existsSync(filePath)) {
                console.log(`✓ Optional file found: ${fileName}`);
                
                // Apply basic JavaScript validation for optional JS files
                if (fileName.endsWith('.js')) {
                    const rules = {
                        checks: [
                            { type: 'notEmpty', message: 'JavaScript file should not be empty' },
                            { type: 'jsBasicValidation', message: 'JavaScript basic validation failed' }
                        ]
                    };
                    await this.validateFile(filePath, fileName, rules);
                }
            }
        }
    }

    /**
     * Validate individual file
     */
    async validateFile(filePath, fileName, rules) {
        try {
            const content = fs.readFileSync(filePath, 'utf8');
            const fileSize = fs.statSync(filePath).size;
            
            console.log(`🔍 Validating: ${fileName} (${this.formatFileSize(fileSize)})`);
            
            for (const check of rules.checks) {
                const result = await this.runValidationCheck(content, check, fileName);
                if (!result.valid) {
                    if (result.severity === 'error') {
                        this.errors.push(`${fileName}: ${result.message}`);
                    } else {
                        this.warnings.push(`${fileName}: ${result.message}`);
                    }
                }
            }
            
            console.log(`✓ ${fileName} validation completed`);
            
        } catch (error) {
            this.errors.push(`${fileName}: Failed to read file - ${error.message}`);
        }
    }

    /**
     * Run individual validation check
     */
    async runValidationCheck(content, check, fileName) {
        switch (check.type) {
            case 'contains':
                return {
                    valid: content.includes(check.value),
                    message: check.message,
                    severity: 'error'
                };
                
            case 'notEmpty':
                return {
                    valid: content.trim().length > 0,
                    message: check.message,
                    severity: 'error'
                };
                
            case 'encoding':
                // Basic UTF-8 validation
                try {
                    Buffer.from(content, 'utf8').toString('utf8');
                    return { valid: true };
                } catch (error) {
                    return {
                        valid: false,
                        message: check.message,
                        severity: 'error'
                    };
                }
                
            case 'cssValidation':
                return this.validateCSS(content);
                
            case 'jsBasicValidation':
                return this.validateJavaScript(content);
                
            case 'jsonValidation':
                return this.validateJSON(content);
                
            case 'packageJsonValidation':
                return this.validatePackageJson(content);
                
            default:
                return {
                    valid: true,
                    message: `Unknown validation type: ${check.type}`,
                    severity: 'warning'
                };
        }
    }

    /**
     * Validate CSS content
     */
    validateCSS(content) {
        const issues = [];
        
        // Check for balanced braces
        const openBraces = (content.match(/{/g) || []).length;
        const closeBraces = (content.match(/}/g) || []).length;
        
        if (openBraces !== closeBraces) {
            issues.push('Mismatched CSS braces');
        }
        
        // Check for basic CSS structure
        if (!content.includes('{') && !content.includes('}')) {
            issues.push('No CSS rules found');
        }
        
        // Check for common CSS issues
        if (content.includes(';;')) {
            issues.push('Double semicolons found');
        }
        
        return {
            valid: issues.length === 0,
            message: issues.join(', '),
            severity: issues.length > 0 ? 'error' : 'info'
        };
    }

    /**
     * Validate JavaScript content
     */
    validateJavaScript(content) {
        const issues = [];
        
        // Check for balanced parentheses, brackets, and braces
        const checks = [
            { open: '(', close: ')', name: 'parentheses' },
            { open: '[', close: ']', name: 'brackets' },
            { open: '{', close: '}', name: 'braces' }
        ];
        
        for (const check of checks) {
            const openCount = (content.match(new RegExp('\\' + check.open, 'g')) || []).length;
            const closeCount = (content.match(new RegExp('\\' + check.close, 'g')) || []).length;
            
            if (openCount !== closeCount) {
                issues.push(`Mismatched ${check.name}`);
            }
        }
        
        // Check for common JavaScript issues
        if (content.includes('console.log') && !content.includes('// DEBUG')) {
            issues.push('Contains console.log statements (consider removing for production)');
        }
        
        // Check for basic function structure
        if (!content.includes('function') && !content.includes('=>') && !content.includes('class')) {
            issues.push('No functions or classes found');
        }
        
        return {
            valid: issues.length === 0,
            message: issues.join(', '),
            severity: issues.some(issue => issue.includes('Mismatched')) ? 'error' : 'warning'
        };
    }

    /**
     * Validate JSON content
     */
    validateJSON(content) {
        try {
            JSON.parse(content);
            return { valid: true };
        } catch (error) {
            return {
                valid: false,
                message: `Invalid JSON: ${error.message}`,
                severity: 'error'
            };
        }
    }

    /**
     * Validate package.json structure
     */
    validatePackageJson(content) {
        try {
            const pkg = JSON.parse(content);
            const issues = [];
            
            // Check required fields
            const requiredFields = ['name', 'version', 'scripts'];
            for (const field of requiredFields) {
                if (!pkg[field]) {
                    issues.push(`Missing required field: ${field}`);
                }
            }
            
            // Check scripts
            if (pkg.scripts) {
                const requiredScripts = ['build'];
                for (const script of requiredScripts) {
                    if (!pkg.scripts[script]) {
                        issues.push(`Missing required script: ${script}`);
                    }
                }
            }
            
            return {
                valid: issues.length === 0,
                message: issues.join(', '),
                severity: 'error'
            };
            
        } catch (error) {
            return {
                valid: false,
                message: `Invalid package.json: ${error.message}`,
                severity: 'error'
            };
        }
    }

    /**
     * Validate cross-references between files
     */
    async validateCrossReferences() {
        console.log('🔗 Validating cross-references...');
        
        const indexPath = path.join(this.sourceDir, 'index.html');
        if (!fs.existsSync(indexPath)) {
            return; // Already reported as missing
        }
        
        const indexContent = fs.readFileSync(indexPath, 'utf8');
        
        // Check CSS references
        const cssReferences = indexContent.match(/href=["']([^"']*\.css)["']/g) || [];
        for (const ref of cssReferences) {
            const cssFile = ref.match(/href=["']([^"']*)["']/)[1];
            const cssPath = path.join(this.sourceDir, cssFile);
            
            if (!fs.existsSync(cssPath)) {
                this.errors.push(`index.html references ${cssFile} but file does not exist`);
            } else {
                console.log(`✓ CSS reference validated: ${cssFile}`);
            }
        }
        
        // Check JavaScript references
        const jsReferences = indexContent.match(/src=["']([^"']*\.js)["']/g) || [];
        for (const ref of jsReferences) {
            const jsFile = ref.match(/src=["']([^"']*)["']/)[1];
            const jsPath = path.join(this.sourceDir, jsFile);
            
            if (!fs.existsSync(jsPath)) {
                this.errors.push(`index.html references ${jsFile} but file does not exist`);
            } else {
                console.log(`✓ JavaScript reference validated: ${jsFile}`);
            }
        }
        
        // Check for API endpoint references
        const apiReferences = indexContent.match(/API_BASE_URL|api\//g) || [];
        if (apiReferences.length > 0) {
            console.log(`ℹ️  Found ${apiReferences.length} API references in HTML`);
        }
    }

    /**
     * Generate validation report
     */
    generateValidationReport() {
        console.log('\n📊 Validation Report');
        console.log('====================');
        
        console.log(`Warnings: ${this.warnings.length}`);
        console.log(`Errors: ${this.errors.length}`);
        
        if (this.warnings.length > 0) {
            console.log('\n⚠️  Warnings:');
            this.warnings.forEach(warning => console.log(`  - ${warning}`));
        }
        
        if (this.errors.length > 0) {
            console.log('\n❌ Errors:');
            this.errors.forEach(error => console.log(`  - ${error}`));
        }
        
        // Write validation report
        const reportPath = path.join(this.sourceDir, 'validation-report.json');
        const report = {
            timestamp: new Date().toISOString(),
            valid: this.errors.length === 0,
            warnings: this.warnings,
            errors: this.errors
        };
        
        try {
            fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
            console.log(`\n📄 Validation report saved to: ${reportPath}`);
        } catch (error) {
            console.warn(`Failed to write validation report: ${error.message}`);
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

// Run validation if called directly
if (require.main === module) {
    const validator = new FileValidator();
    validator.validate().catch(error => {
        console.error('Validation failed:', error);
        process.exit(1);
    });
}

module.exports = FileValidator;