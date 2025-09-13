#!/usr/bin/env node

/**
 * Build Verification Script
 * 
 * Verifies that build artifacts are functional and complete
 */

const fs = require('fs');
const path = require('path');
const http = require('http');
const { spawn } = require('child_process');

class BuildVerifier {
    constructor() {
        this.buildDir = path.join(__dirname, 'build');
        this.errors = [];
        this.warnings = [];
        this.testResults = [];
    }

    /**
     * Main verification process
     */
    async verify() {
        console.log('🔍 Starting build verification...');
        
        try {
            // Check build directory exists
            await this.checkBuildDirectory();
            
            // Verify file structure
            await this.verifyFileStructure();
            
            // Verify file contents
            await this.verifyFileContents();
            
            // Test static file serving
            await this.testStaticFileServing();
            
            // Generate verification report
            this.generateVerificationReport();
            
            if (this.errors.length > 0) {
                console.log('❌ Build verification failed');
                process.exit(1);
            } else {
                console.log('✅ Build verification completed successfully');
                return true;
            }
            
        } catch (error) {
            console.error('❌ Verification process failed:', error.message);
            process.exit(1);
        }
    }

    /**
     * Check build directory exists
     */
    async checkBuildDirectory() {
        console.log('📁 Checking build directory...');
        
        if (!fs.existsSync(this.buildDir)) {
            throw new Error('Build directory does not exist. Run npm run build first.');
        }
        
        const buildStat = fs.statSync(this.buildDir);
        if (!buildStat.isDirectory()) {
            throw new Error('Build path exists but is not a directory');
        }
        
        console.log('✓ Build directory exists');
    }

    /**
     * Verify file structure
     */
    async verifyFileStructure() {
        console.log('📋 Verifying file structure...');
        
        const requiredFiles = [
            'index.html',
            'css/style.css',
            'js/app.js',
            'js/api-client.js',
            'package.json'
        ];
        
        const expectedFiles = [
            'js/auth-manager.js',
            'js/aws-config.js',
            'js/data-manager.js',
            'build-report.json'
        ];
        
        // Check required files
        for (const file of requiredFiles) {
            const filePath = path.join(this.buildDir, file);
            if (!fs.existsSync(filePath)) {
                this.errors.push(`Required file missing in build: ${file}`);
            } else {
                console.log(`✓ Required file present: ${file}`);
            }
        }
        
        // Check expected files
        for (const file of expectedFiles) {
            const filePath = path.join(this.buildDir, file);
            if (fs.existsSync(filePath)) {
                console.log(`✓ Expected file present: ${file}`);
            } else {
                this.warnings.push(`Expected file missing: ${file}`);
            }
        }
        
        // Check for unexpected files
        const allFiles = this.getAllFiles(this.buildDir);
        const allowedPatterns = [
            /^index\.html$/,
            /^package\.json$/,
            /^css\/.*\.css$/,
            /^js\/.*\.js$/,
            /^.*-report\.json$/,
            /^validation-report\.json$/
        ];
        
        for (const file of allFiles) {
            const relativePath = path.relative(this.buildDir, file).replace(/\\/g, '/');
            const isAllowed = allowedPatterns.some(pattern => pattern.test(relativePath));
            
            if (!isAllowed) {
                this.warnings.push(`Unexpected file in build: ${relativePath}`);
            }
        }
    }

    /**
     * Verify file contents
     */
    async verifyFileContents() {
        console.log('📄 Verifying file contents...');
        
        // Verify HTML file
        await this.verifyHTMLFile();
        
        // Verify CSS file
        await this.verifyCSSFile();
        
        // Verify JavaScript files
        await this.verifyJavaScriptFiles();
        
        // Verify package.json
        await this.verifyPackageJson();
    }

    /**
     * Verify HTML file
     */
    async verifyHTMLFile() {
        const htmlPath = path.join(this.buildDir, 'index.html');
        if (!fs.existsSync(htmlPath)) {
            return; // Already reported as missing
        }
        
        const content = fs.readFileSync(htmlPath, 'utf8');
        
        // Check basic HTML structure
        if (!content.includes('<!DOCTYPE html>')) {
            this.errors.push('index.html: Missing DOCTYPE declaration');
        }
        
        if (!content.includes('<title>')) {
            this.warnings.push('index.html: Missing title tag');
        }
        
        // Check CSS references
        if (content.includes('css/style.css')) {
            const cssPath = path.join(this.buildDir, 'css/style.css');
            if (!fs.existsSync(cssPath)) {
                this.errors.push('index.html references css/style.css but file is missing');
            } else {
                console.log('✓ CSS reference verified');
            }
        }
        
        // Check JavaScript references
        const jsReferences = content.match(/src=["']([^"']*\.js)["']/g) || [];
        for (const ref of jsReferences) {
            const jsFile = ref.match(/src=["']([^"']*)["']/)[1];
            const jsPath = path.join(this.buildDir, jsFile);
            
            if (!fs.existsSync(jsPath)) {
                this.errors.push(`index.html references ${jsFile} but file is missing`);
            } else {
                console.log(`✓ JavaScript reference verified: ${jsFile}`);
            }
        }
        
        console.log('✓ HTML file verification completed');
    }

    /**
     * Verify CSS file
     */
    async verifyCSSFile() {
        const cssPath = path.join(this.buildDir, 'css/style.css');
        if (!fs.existsSync(cssPath)) {
            return; // Already reported as missing
        }
        
        const content = fs.readFileSync(cssPath, 'utf8');
        
        if (content.trim().length === 0) {
            this.errors.push('style.css is empty');
            return;
        }
        
        // Check for balanced braces
        const openBraces = (content.match(/{/g) || []).length;
        const closeBraces = (content.match(/}/g) || []).length;
        
        if (openBraces !== closeBraces) {
            this.errors.push('style.css has mismatched braces');
        }
        
        // Check for basic CSS rules
        if (!content.includes('{') || !content.includes('}')) {
            this.warnings.push('style.css appears to have no CSS rules');
        }
        
        console.log('✓ CSS file verification completed');
    }

    /**
     * Verify JavaScript files
     */
    async verifyJavaScriptFiles() {
        const jsFiles = ['js/app.js', 'js/api-client.js'];
        
        for (const jsFile of jsFiles) {
            const jsPath = path.join(this.buildDir, jsFile);
            if (!fs.existsSync(jsPath)) {
                continue; // Already reported as missing
            }
            
            const content = fs.readFileSync(jsPath, 'utf8');
            
            if (content.trim().length === 0) {
                this.errors.push(`${jsFile} is empty`);
                continue;
            }
            
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
                    this.errors.push(`${jsFile} has mismatched ${check.name}`);
                }
            }
            
            console.log(`✓ JavaScript file verified: ${jsFile}`);
        }
    }

    /**
     * Verify package.json
     */
    async verifyPackageJson() {
        const packagePath = path.join(this.buildDir, 'package.json');
        if (!fs.existsSync(packagePath)) {
            return; // Already reported as missing
        }
        
        try {
            const content = fs.readFileSync(packagePath, 'utf8');
            const pkg = JSON.parse(content);
            
            // Check required fields
            const requiredFields = ['name', 'version'];
            for (const field of requiredFields) {
                if (!pkg[field]) {
                    this.warnings.push(`package.json missing field: ${field}`);
                }
            }
            
            console.log('✓ package.json verification completed');
            
        } catch (error) {
            this.errors.push(`package.json is invalid: ${error.message}`);
        }
    }

    /**
     * Test static file serving
     */
    async testStaticFileServing() {
        console.log('🌐 Testing static file serving...');
        
        return new Promise((resolve) => {
            const port = 3001;
            const server = http.createServer((req, res) => {
                let filePath = path.join(this.buildDir, req.url === '/' ? 'index.html' : req.url);
                
                // Security check
                if (!filePath.startsWith(this.buildDir)) {
                    res.writeHead(403);
                    res.end('Forbidden');
                    return;
                }
                
                if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
                    const ext = path.extname(filePath);
                    const contentTypes = {
                        '.html': 'text/html',
                        '.css': 'text/css',
                        '.js': 'application/javascript',
                        '.json': 'application/json'
                    };
                    
                    res.writeHead(200, { 'Content-Type': contentTypes[ext] || 'text/plain' });
                    fs.createReadStream(filePath).pipe(res);
                } else {
                    res.writeHead(404);
                    res.end('Not Found');
                }
            });
            
            server.listen(port, () => {
                console.log(`📡 Test server started on port ${port}`);
                
                // Test file requests
                this.testFileRequests(port).then(() => {
                    server.close(() => {
                        console.log('📡 Test server stopped');
                        resolve();
                    });
                });
            });
            
            server.on('error', (error) => {
                if (error.code === 'EADDRINUSE') {
                    console.log(`⚠️  Port ${port} is in use, skipping static file serving test`);
                    this.warnings.push(`Could not test static file serving: port ${port} in use`);
                } else {
                    this.errors.push(`Static file serving test failed: ${error.message}`);
                }
                resolve();
            });
        });
    }

    /**
     * Test file requests
     */
    async testFileRequests(port) {
        const testUrls = [
            '/',
            '/css/style.css',
            '/js/app.js',
            '/js/api-client.js',
            '/package.json'
        ];
        
        for (const url of testUrls) {
            try {
                await this.makeRequest(port, url);
                console.log(`✓ Static file served: ${url}`);
                this.testResults.push({ url, status: 'success' });
            } catch (error) {
                console.log(`❌ Failed to serve: ${url} - ${error.message}`);
                this.testResults.push({ url, status: 'failed', error: error.message });
                this.errors.push(`Static file serving failed for ${url}: ${error.message}`);
            }
        }
    }

    /**
     * Make HTTP request
     */
    makeRequest(port, path) {
        return new Promise((resolve, reject) => {
            const req = http.get(`http://localhost:${port}${path}`, (res) => {
                if (res.statusCode === 200) {
                    resolve(res);
                } else {
                    reject(new Error(`HTTP ${res.statusCode}`));
                }
            });
            
            req.on('error', reject);
            req.setTimeout(5000, () => {
                req.destroy();
                reject(new Error('Request timeout'));
            });
        });
    }

    /**
     * Get all files recursively
     */
    getAllFiles(dir) {
        const files = [];
        const items = fs.readdirSync(dir);
        
        for (const item of items) {
            const fullPath = path.join(dir, item);
            const stat = fs.statSync(fullPath);
            
            if (stat.isDirectory()) {
                files.push(...this.getAllFiles(fullPath));
            } else {
                files.push(fullPath);
            }
        }
        
        return files;
    }

    /**
     * Generate verification report
     */
    generateVerificationReport() {
        console.log('\n📊 Verification Report');
        console.log('======================');
        
        console.log(`Test results: ${this.testResults.length}`);
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
        
        // Write verification report
        const reportPath = path.join(this.buildDir, 'verification-report.json');
        const report = {
            timestamp: new Date().toISOString(),
            success: this.errors.length === 0,
            testResults: this.testResults,
            warnings: this.warnings,
            errors: this.errors
        };
        
        try {
            fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
            console.log(`\n📄 Verification report saved to: ${reportPath}`);
        } catch (error) {
            console.warn(`Failed to write verification report: ${error.message}`);
        }
    }
}

// Run verification if called directly
if (require.main === module) {
    const verifier = new BuildVerifier();
    verifier.verify().catch(error => {
        console.error('Verification failed:', error);
        process.exit(1);
    });
}

module.exports = BuildVerifier;