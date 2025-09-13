#!/usr/bin/env node

/**
 * Frontend Build Script for Team Dashboard
 * 
 * Features:
 * - File integrity validation
 * - Optimized file copying with exclusions
 * - Build artifact verification
 * - Error reporting and logging
 */

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

class FrontendBuilder {
    constructor() {
        this.sourceDir = __dirname;
        this.buildDir = path.join(__dirname, 'build');
        this.errors = [];
        this.warnings = [];
        this.copiedFiles = [];
        
        // Required files configuration
        this.requiredFiles = [
            'index.html',
            'css/style.css',
            'js/app.js',
            'js/api-client.js',
            'package.json'
        ];
        
        // Optional files that should be included if they exist
        this.optionalFiles = [
            'js/auth-manager.js',
            'js/aws-config.js',
            'js/data-manager.js'
        ];
        
        // Files and patterns to exclude
        this.excludePatterns = [
            /^\.DS_Store$/,
            /^\.git/,
            /^node_modules/,
            /\.log$/,
            /\.tmp$/,
            /^build$/,
            /^dist$/,
            /\.bak$/,
            /~$/,
            /^build-script\.js$/,
            /^validate-files\.js$/,
            /^verify-build\.js$/,
            /^BUILD\.md$/
        ];
    }

    /**
     * Main build process
     */
    async build() {
        console.log('🚀 Starting frontend build process...');
        console.log(`Source: ${this.sourceDir}`);
        console.log(`Target: ${this.buildDir}`);
        
        try {
            // Step 1: Validate source files
            await this.validateSourceFiles();
            
            // Step 2: Clean and prepare build directory
            await this.prepareBuildDirectory();
            
            // Step 3: Copy files with optimization
            await this.copyFiles();
            
            // Step 4: Verify build artifacts
            await this.verifyBuildArtifacts();
            
            // Step 5: Generate build report
            this.generateBuildReport();
            
            console.log('✅ Frontend build completed successfully!');
            return true;
            
        } catch (error) {
            console.error('❌ Build failed:', error.message);
            this.errors.push(error.message);
            this.generateBuildReport();
            process.exit(1);
        }
    }

    /**
     * Validate source files integrity
     */
    async validateSourceFiles() {
        console.log('📋 Validating source files...');
        
        // Check required files
        for (const file of this.requiredFiles) {
            const filePath = path.join(this.sourceDir, file);
            if (!fs.existsSync(filePath)) {
                throw new Error(`Required file missing: ${file}`);
            }
            
            // Validate file content
            await this.validateFileContent(filePath, file);
        }
        
        // Check optional files
        for (const file of this.optionalFiles) {
            const filePath = path.join(this.sourceDir, file);
            if (fs.existsSync(filePath)) {
                console.log(`✓ Optional file found: ${file}`);
                await this.validateFileContent(filePath, file);
            } else {
                this.warnings.push(`Optional file not found: ${file}`);
            }
        }
        
        console.log('✅ Source file validation completed');
    }

    /**
     * Validate individual file content
     */
    async validateFileContent(filePath, fileName) {
        const content = fs.readFileSync(filePath, 'utf8');
        
        // HTML validation
        if (fileName.endsWith('.html')) {
            if (!content.includes('<!DOCTYPE html>')) {
                this.warnings.push(`${fileName}: Missing DOCTYPE declaration`);
            }
            if (!content.includes('<title>')) {
                this.warnings.push(`${fileName}: Missing title tag`);
            }
            // Check for required CSS and JS references
            if (fileName === 'index.html') {
                if (!content.includes('css/style.css')) {
                    this.errors.push(`${fileName}: Missing CSS reference`);
                }
                if (!content.includes('js/app.js') && !content.includes('js/api-client.js')) {
                    this.warnings.push(`${fileName}: No JavaScript references found`);
                }
            }
        }
        
        // CSS validation
        if (fileName.endsWith('.css')) {
            if (content.trim().length === 0) {
                this.warnings.push(`${fileName}: Empty CSS file`);
            }
            // Basic CSS syntax check
            const openBraces = (content.match(/{/g) || []).length;
            const closeBraces = (content.match(/}/g) || []).length;
            if (openBraces !== closeBraces) {
                this.errors.push(`${fileName}: Mismatched CSS braces`);
            }
        }
        
        // JavaScript validation
        if (fileName.endsWith('.js')) {
            if (content.trim().length === 0) {
                this.warnings.push(`${fileName}: Empty JavaScript file`);
            }
            // Basic syntax check for common issues
            if (content.includes('console.log') && !fileName.includes('debug')) {
                this.warnings.push(`${fileName}: Contains console.log statements`);
            }
        }
        
        // JSON validation
        if (fileName.endsWith('.json')) {
            try {
                JSON.parse(content);
            } catch (error) {
                this.errors.push(`${fileName}: Invalid JSON syntax - ${error.message}`);
            }
        }
        
        console.log(`✓ Validated: ${fileName} (${this.formatFileSize(content.length)})`);
    }

    /**
     * Prepare build directory
     */
    async prepareBuildDirectory() {
        console.log('📁 Preparing build directory...');
        
        // Remove existing build directory
        if (fs.existsSync(this.buildDir)) {
            fs.rmSync(this.buildDir, { recursive: true, force: true });
            console.log('🗑️  Cleaned existing build directory');
        }
        
        // Create build directory
        fs.mkdirSync(this.buildDir, { recursive: true });
        console.log('📁 Created build directory');
    }

    /**
     * Copy files with optimization and exclusions
     */
    async copyFiles() {
        console.log('📋 Copying files to build directory...');
        
        await this.copyDirectory(this.sourceDir, this.buildDir);
        
        console.log(`✅ Copied ${this.copiedFiles.length} files`);
    }

    /**
     * Recursively copy directory with exclusions
     */
    async copyDirectory(srcDir, destDir) {
        const items = fs.readdirSync(srcDir);
        
        for (const item of items) {
            const srcPath = path.join(srcDir, item);
            const destPath = path.join(destDir, item);
            
            // Check if item should be excluded
            if (this.shouldExclude(item, srcPath)) {
                console.log(`⏭️  Skipped: ${path.relative(this.sourceDir, srcPath)}`);
                continue;
            }
            
            const stat = fs.statSync(srcPath);
            
            if (stat.isDirectory()) {
                // Create directory and copy contents
                fs.mkdirSync(destPath, { recursive: true });
                await this.copyDirectory(srcPath, destPath);
            } else {
                // Copy file
                await this.copyFile(srcPath, destPath);
            }
        }
    }

    /**
     * Copy individual file with validation
     */
    async copyFile(srcPath, destPath) {
        try {
            // Ensure destination directory exists
            const destDir = path.dirname(destPath);
            fs.mkdirSync(destDir, { recursive: true });
            
            // Copy file
            fs.copyFileSync(srcPath, destPath);
            
            // Verify copy integrity
            const srcHash = this.calculateFileHash(srcPath);
            const destHash = this.calculateFileHash(destPath);
            
            if (srcHash !== destHash) {
                throw new Error(`File copy integrity check failed: ${path.relative(this.sourceDir, srcPath)}`);
            }
            
            const relativePath = path.relative(this.sourceDir, srcPath);
            this.copiedFiles.push(relativePath);
            console.log(`✓ Copied: ${relativePath}`);
            
        } catch (error) {
            throw new Error(`Failed to copy ${srcPath}: ${error.message}`);
        }
    }

    /**
     * Check if file/directory should be excluded
     */
    shouldExclude(name, fullPath) {
        // Check against exclude patterns
        for (const pattern of this.excludePatterns) {
            if (pattern instanceof RegExp) {
                if (pattern.test(name)) {
                    return true;
                }
            } else if (name === pattern) {
                return true;
            }
        }
        
        // Additional checks
        const stat = fs.statSync(fullPath);
        
        // Skip hidden files (except .gitkeep)
        if (name.startsWith('.') && name !== '.gitkeep') {
            return true;
        }
        
        // Skip very large files (>10MB)
        if (stat.isFile() && stat.size > 10 * 1024 * 1024) {
            this.warnings.push(`Large file skipped: ${name} (${this.formatFileSize(stat.size)})`);
            return true;
        }
        
        return false;
    }

    /**
     * Verify build artifacts
     */
    async verifyBuildArtifacts() {
        console.log('🔍 Verifying build artifacts...');
        
        // Check that all required files were copied
        for (const file of this.requiredFiles) {
            const buildPath = path.join(this.buildDir, file);
            if (!fs.existsSync(buildPath)) {
                throw new Error(`Required file missing in build: ${file}`);
            }
        }
        
        // Verify HTML file references
        const indexPath = path.join(this.buildDir, 'index.html');
        if (fs.existsSync(indexPath)) {
            const content = fs.readFileSync(indexPath, 'utf8');
            
            // Check CSS references
            if (content.includes('css/style.css')) {
                const cssPath = path.join(this.buildDir, 'css/style.css');
                if (!fs.existsSync(cssPath)) {
                    this.errors.push('index.html references css/style.css but file is missing');
                }
            }
            
            // Check JS references
            const jsReferences = content.match(/src=["']([^"']*\.js)["']/g) || [];
            for (const ref of jsReferences) {
                const jsFile = ref.match(/src=["']([^"']*)["']/)[1];
                const jsPath = path.join(this.buildDir, jsFile);
                if (!fs.existsSync(jsPath)) {
                    this.errors.push(`index.html references ${jsFile} but file is missing`);
                }
            }
        }
        
        // Calculate total build size
        const buildSize = this.calculateDirectorySize(this.buildDir);
        console.log(`📊 Build size: ${this.formatFileSize(buildSize)}`);
        
        if (buildSize > 50 * 1024 * 1024) { // 50MB
            this.warnings.push(`Large build size: ${this.formatFileSize(buildSize)}`);
        }
        
        console.log('✅ Build artifact verification completed');
    }

    /**
     * Generate build report
     */
    generateBuildReport() {
        console.log('\n📊 Build Report');
        console.log('================');
        
        console.log(`Files copied: ${this.copiedFiles.length}`);
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
        
        // Write detailed report to file
        const reportPath = path.join(this.buildDir, 'build-report.json');
        const report = {
            timestamp: new Date().toISOString(),
            success: this.errors.length === 0,
            copiedFiles: this.copiedFiles,
            warnings: this.warnings,
            errors: this.errors,
            buildSize: fs.existsSync(this.buildDir) ? this.calculateDirectorySize(this.buildDir) : 0
        };
        
        try {
            fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
            console.log(`\n📄 Detailed report saved to: ${reportPath}`);
        } catch (error) {
            console.warn(`Failed to write build report: ${error.message}`);
        }
    }

    /**
     * Calculate file hash for integrity checking
     */
    calculateFileHash(filePath) {
        const content = fs.readFileSync(filePath);
        return crypto.createHash('sha256').update(content).digest('hex');
    }

    /**
     * Calculate directory size recursively
     */
    calculateDirectorySize(dirPath) {
        let totalSize = 0;
        
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

// Run build if called directly
if (require.main === module) {
    const builder = new FrontendBuilder();
    builder.build().catch(error => {
        console.error('Build failed:', error);
        process.exit(1);
    });
}

module.exports = FrontendBuilder;