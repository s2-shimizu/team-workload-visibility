#!/usr/bin/env node

/**
 * Deployment Verification Script
 * 
 * Verifies that the deployed application is functioning correctly
 * after AWS Amplify deployment completes.
 * 
 * Requirements covered:
 * - 4.1: Frontend page availability check
 * - 4.2: API endpoint response verification  
 * - 4.3: Static resource delivery verification
 */

const https = require('https');
const http = require('http');
const { URL } = require('url');
const fs = require('fs');
const path = require('path');

class DeploymentVerifier {
    constructor(options = {}) {
        this.frontendUrl = options.frontendUrl || process.env.FRONTEND_URL;
        this.apiUrl = options.apiUrl || process.env.API_URL;
        this.timeout = options.timeout || 30000;
        this.retries = options.retries || 3;
        this.retryDelay = options.retryDelay || 2000;
        
        this.results = {
            timestamp: new Date().toISOString(),
            success: false,
            frontend: {
                pageAvailability: [],
                staticResources: []
            },
            api: {
                endpoints: []
            },
            errors: [],
            warnings: []
        };
    }

    /**
     * Main verification process
     */
    async verify() {
        console.log('🚀 Starting deployment verification...');
        console.log(`Frontend URL: ${this.frontendUrl || 'Not specified'}`);
        console.log(`API URL: ${this.apiUrl || 'Not specified'}`);
        
        try {
            // Validate configuration
            this.validateConfiguration();
            
            // Verify frontend availability
            await this.verifyFrontendAvailability();
            
            // Verify static resources
            await this.verifyStaticResources();
            
            // Verify API endpoints
            await this.verifyApiEndpoints();
            
            // Generate verification report
            this.generateReport();
            
            // Determine overall success
            this.results.success = this.results.errors.length === 0;
            
            if (this.results.success) {
                console.log('✅ Deployment verification completed successfully');
                return true;
            } else {
                console.log('❌ Deployment verification failed');
                process.exit(1);
            }
            
        } catch (error) {
            console.error('❌ Verification process failed:', error.message);
            this.results.errors.push(`Verification process failed: ${error.message}`);
            this.generateReport();
            process.exit(1);
        }
    }

    /**
     * Validate configuration
     */
    validateConfiguration() {
        console.log('🔧 Validating configuration...');
        
        if (!this.frontendUrl) {
            throw new Error('Frontend URL is required. Set FRONTEND_URL environment variable or pass frontendUrl option.');
        }
        
        if (!this.apiUrl) {
            this.results.warnings.push('API URL not specified. API endpoint verification will be skipped.');
        }
        
        // Validate URL format
        try {
            new URL(this.frontendUrl);
        } catch (error) {
            throw new Error(`Invalid frontend URL format: ${this.frontendUrl}`);
        }
        
        if (this.apiUrl) {
            try {
                new URL(this.apiUrl);
            } catch (error) {
                throw new Error(`Invalid API URL format: ${this.apiUrl}`);
            }
        }
        
        console.log('✓ Configuration validated');
    }

    /**
     * Verify frontend page availability
     */
    async verifyFrontendAvailability() {
        console.log('🌐 Verifying frontend page availability...');
        
        const pages = [
            { path: '/', name: 'Main page (index.html)' },
            { path: '/index.html', name: 'Index page direct access' }
        ];
        
        for (const page of pages) {
            const url = this.frontendUrl + page.path;
            const result = await this.checkPageAvailability(url, page.name);
            this.results.frontend.pageAvailability.push(result);
            
            if (result.success) {
                console.log(`✓ ${page.name}: Available`);
            } else {
                console.log(`❌ ${page.name}: ${result.error}`);
                this.results.errors.push(`Frontend page unavailable: ${page.name} - ${result.error}`);
            }
        }
    }

    /**
     * Check individual page availability
     */
    async checkPageAvailability(url, name) {
        const result = {
            url,
            name,
            success: false,
            statusCode: null,
            responseTime: null,
            contentLength: null,
            error: null
        };
        
        const startTime = Date.now();
        
        try {
            const response = await this.makeHttpRequest(url);
            result.statusCode = response.statusCode;
            result.responseTime = Date.now() - startTime;
            result.contentLength = response.headers['content-length'] || 'unknown';
            
            if (response.statusCode === 200) {
                result.success = true;
                
                // Verify HTML content
                const content = await this.readResponseBody(response);
                if (content.includes('<!DOCTYPE html>') || content.includes('<html')) {
                    result.htmlValid = true;
                } else {
                    result.htmlValid = false;
                    this.results.warnings.push(`${name}: Response doesn't appear to be valid HTML`);
                }
                
                // Check for basic HTML structure
                if (content.includes('<title>')) {
                    result.hasTitle = true;
                } else {
                    this.results.warnings.push(`${name}: Missing title tag`);
                }
                
            } else {
                result.error = `HTTP ${response.statusCode}`;
            }
            
        } catch (error) {
            result.error = error.message;
        }
        
        return result;
    }

    /**
     * Verify static resources delivery
     */
    async verifyStaticResources() {
        console.log('📁 Verifying static resources delivery...');
        
        const resources = [
            { path: '/css/style.css', type: 'CSS', contentType: 'text/css' },
            { path: '/js/app.js', type: 'JavaScript', contentType: 'application/javascript' },
            { path: '/js/api-client.js', type: 'JavaScript', contentType: 'application/javascript' },
            { path: '/package.json', type: 'JSON', contentType: 'application/json' }
        ];
        
        for (const resource of resources) {
            const url = this.frontendUrl + resource.path;
            const result = await this.checkStaticResource(url, resource);
            this.results.frontend.staticResources.push(result);
            
            if (result.success) {
                console.log(`✓ ${resource.type}: ${resource.path} - Available (${result.contentLength} bytes)`);
            } else {
                console.log(`❌ ${resource.type}: ${resource.path} - ${result.error}`);
                this.results.errors.push(`Static resource unavailable: ${resource.path} - ${result.error}`);
            }
        }
    }

    /**
     * Check individual static resource
     */
    async checkStaticResource(url, resource) {
        const result = {
            url,
            path: resource.path,
            type: resource.type,
            success: false,
            statusCode: null,
            contentType: null,
            contentLength: null,
            responseTime: null,
            error: null
        };
        
        const startTime = Date.now();
        
        try {
            const response = await this.makeHttpRequest(url);
            result.statusCode = response.statusCode;
            result.responseTime = Date.now() - startTime;
            result.contentType = response.headers['content-type'] || 'unknown';
            result.contentLength = response.headers['content-length'] || 'unknown';
            
            if (response.statusCode === 200) {
                result.success = true;
                
                // Verify content type if expected
                if (resource.contentType && !result.contentType.includes(resource.contentType.split('/')[1])) {
                    this.results.warnings.push(`${resource.path}: Unexpected content type ${result.contentType}, expected ${resource.contentType}`);
                }
                
                // Verify content is not empty
                const content = await this.readResponseBody(response);
                if (content.length === 0) {
                    this.results.warnings.push(`${resource.path}: Resource is empty`);
                }
                
                // Basic content validation
                if (resource.type === 'CSS' && !content.includes('{')) {
                    this.results.warnings.push(`${resource.path}: CSS file appears to have no rules`);
                }
                
                if (resource.type === 'JavaScript' && content.length < 10) {
                    this.results.warnings.push(`${resource.path}: JavaScript file appears to be very small or empty`);
                }
                
                if (resource.type === 'JSON') {
                    try {
                        JSON.parse(content);
                    } catch (error) {
                        this.results.warnings.push(`${resource.path}: Invalid JSON format`);
                    }
                }
                
            } else {
                result.error = `HTTP ${response.statusCode}`;
            }
            
        } catch (error) {
            result.error = error.message;
        }
        
        return result;
    }

    /**
     * Verify API endpoints
     */
    async verifyApiEndpoints() {
        if (!this.apiUrl) {
            console.log('⚠️  Skipping API endpoint verification (API URL not specified)');
            return;
        }
        
        console.log('🔌 Verifying API endpoints...');
        
        const endpoints = [
            { path: '/health', method: 'GET', name: 'Health check' },
            { path: '/actuator/health', method: 'GET', name: 'Actuator health' },
            { path: '/api/status', method: 'GET', name: 'API status' },
            { path: '/api/workload-status', method: 'GET', name: 'Workload status list' },
            { path: '/api/workload-status/my', method: 'GET', name: 'My workload status' },
            { path: '/api/team-issues', method: 'GET', name: 'Team issues list' },
            { path: '/api/team-issues/open', method: 'GET', name: 'Open team issues' },
            { path: '/api/team-issues/statistics', method: 'GET', name: 'Issue statistics' }
        ];
        
        for (const endpoint of endpoints) {
            const url = this.apiUrl + endpoint.path;
            const result = await this.checkApiEndpoint(url, endpoint);
            this.results.api.endpoints.push(result);
            
            if (result.success) {
                console.log(`✓ ${endpoint.name}: ${endpoint.method} ${endpoint.path} - OK (${result.responseTime}ms)`);
            } else {
                console.log(`❌ ${endpoint.name}: ${endpoint.method} ${endpoint.path} - ${result.error}`);
                this.results.errors.push(`API endpoint failed: ${endpoint.path} - ${result.error}`);
            }
        }
    }

    /**
     * Check individual API endpoint
     */
    async checkApiEndpoint(url, endpoint) {
        const result = {
            url,
            path: endpoint.path,
            method: endpoint.method,
            name: endpoint.name,
            success: false,
            statusCode: null,
            responseTime: null,
            contentType: null,
            responseData: null,
            error: null
        };
        
        const startTime = Date.now();
        
        try {
            const response = await this.makeHttpRequest(url, endpoint.method);
            result.statusCode = response.statusCode;
            result.responseTime = Date.now() - startTime;
            result.contentType = response.headers['content-type'] || 'unknown';
            
            if (response.statusCode === 200) {
                result.success = true;
                
                // Try to parse JSON response
                const content = await this.readResponseBody(response);
                if (result.contentType.includes('application/json')) {
                    try {
                        result.responseData = JSON.parse(content);
                        
                        // Validate specific endpoint responses
                        this.validateApiResponse(endpoint.path, result.responseData);
                        
                    } catch (error) {
                        this.results.warnings.push(`${endpoint.path}: Response is not valid JSON`);
                    }
                }
                
            } else {
                result.error = `HTTP ${response.statusCode}`;
            }
            
        } catch (error) {
            result.error = error.message;
        }
        
        return result;
    }

    /**
     * Validate API response structure
     */
    validateApiResponse(path, data) {
        switch (path) {
            case '/health':
            case '/actuator/health':
                if (!data.status) {
                    this.results.warnings.push(`${path}: Missing status field in health response`);
                }
                break;
                
            case '/api/status':
                if (!data.status || !data.message) {
                    this.results.warnings.push(`${path}: Missing required fields in status response`);
                }
                break;
                
            case '/api/workload-status':
            case '/api/team-issues':
            case '/api/team-issues/open':
                if (!Array.isArray(data)) {
                    this.results.warnings.push(`${path}: Response should be an array`);
                }
                break;
                
            case '/api/workload-status/my':
                if (!data.userId || !data.workloadLevel) {
                    this.results.warnings.push(`${path}: Missing required fields in workload status response`);
                }
                break;
                
            case '/api/team-issues/statistics':
                if (typeof data.open !== 'number' || typeof data.resolved !== 'number') {
                    this.results.warnings.push(`${path}: Statistics should contain numeric values`);
                }
                break;
        }
    }

    /**
     * Make HTTP request with retry logic
     */
    async makeHttpRequest(url, method = 'GET') {
        let lastError;
        
        for (let attempt = 1; attempt <= this.retries; attempt++) {
            try {
                return await this.performHttpRequest(url, method);
            } catch (error) {
                lastError = error;
                
                if (attempt < this.retries) {
                    console.log(`⚠️  Request failed (attempt ${attempt}/${this.retries}), retrying in ${this.retryDelay}ms...`);
                    await this.sleep(this.retryDelay);
                }
            }
        }
        
        throw lastError;
    }

    /**
     * Perform single HTTP request
     */
    performHttpRequest(url, method = 'GET') {
        return new Promise((resolve, reject) => {
            const urlObj = new URL(url);
            const isHttps = urlObj.protocol === 'https:';
            const client = isHttps ? https : http;
            
            const options = {
                hostname: urlObj.hostname,
                port: urlObj.port || (isHttps ? 443 : 80),
                path: urlObj.pathname + urlObj.search,
                method: method,
                headers: {
                    'User-Agent': 'DeploymentVerifier/1.0',
                    'Accept': '*/*'
                },
                timeout: this.timeout
            };
            
            const req = client.request(options, (res) => {
                resolve(res);
            });
            
            req.on('error', (error) => {
                reject(new Error(`Request failed: ${error.message}`));
            });
            
            req.on('timeout', () => {
                req.destroy();
                reject(new Error(`Request timeout after ${this.timeout}ms`));
            });
            
            req.end();
        });
    }

    /**
     * Read response body
     */
    readResponseBody(response) {
        return new Promise((resolve, reject) => {
            let data = '';
            
            response.on('data', (chunk) => {
                data += chunk;
            });
            
            response.on('end', () => {
                resolve(data);
            });
            
            response.on('error', (error) => {
                reject(error);
            });
        });
    }

    /**
     * Sleep utility
     */
    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    /**
     * Generate verification report
     */
    generateReport() {
        console.log('\n📊 Deployment Verification Report');
        console.log('==================================');
        
        // Summary
        const totalChecks = this.results.frontend.pageAvailability.length + 
                           this.results.frontend.staticResources.length + 
                           this.results.api.endpoints.length;
        const successfulChecks = this.results.frontend.pageAvailability.filter(r => r.success).length +
                                this.results.frontend.staticResources.filter(r => r.success).length +
                                this.results.api.endpoints.filter(r => r.success).length;
        
        console.log(`Total checks: ${totalChecks}`);
        console.log(`Successful: ${successfulChecks}`);
        console.log(`Failed: ${totalChecks - successfulChecks}`);
        console.log(`Warnings: ${this.results.warnings.length}`);
        console.log(`Errors: ${this.results.errors.length}`);
        
        // Frontend results
        console.log('\n🌐 Frontend Verification:');
        this.results.frontend.pageAvailability.forEach(result => {
            const status = result.success ? '✓' : '❌';
            console.log(`  ${status} ${result.name}: ${result.success ? 'OK' : result.error} (${result.responseTime || 'N/A'}ms)`);
        });
        
        console.log('\n📁 Static Resources:');
        this.results.frontend.staticResources.forEach(result => {
            const status = result.success ? '✓' : '❌';
            console.log(`  ${status} ${result.path}: ${result.success ? 'OK' : result.error} (${result.contentLength || 'N/A'} bytes)`);
        });
        
        // API results
        if (this.results.api.endpoints.length > 0) {
            console.log('\n🔌 API Endpoints:');
            this.results.api.endpoints.forEach(result => {
                const status = result.success ? '✓' : '❌';
                console.log(`  ${status} ${result.path}: ${result.success ? 'OK' : result.error} (${result.responseTime || 'N/A'}ms)`);
            });
        }
        
        // Warnings
        if (this.results.warnings.length > 0) {
            console.log('\n⚠️  Warnings:');
            this.results.warnings.forEach(warning => console.log(`  - ${warning}`));
        }
        
        // Errors
        if (this.results.errors.length > 0) {
            console.log('\n❌ Errors:');
            this.results.errors.forEach(error => console.log(`  - ${error}`));
        }
        
        // Save report to file
        this.saveReportToFile();
    }

    /**
     * Save report to file
     */
    saveReportToFile() {
        const reportPath = 'deployment-verification-report.json';
        
        try {
            fs.writeFileSync(reportPath, JSON.stringify(this.results, null, 2));
            console.log(`\n📄 Verification report saved to: ${reportPath}`);
        } catch (error) {
            console.warn(`Failed to write verification report: ${error.message}`);
        }
    }
}

// CLI interface
if (require.main === module) {
    const args = process.argv.slice(2);
    const options = {};
    
    // Parse command line arguments
    for (let i = 0; i < args.length; i += 2) {
        const key = args[i];
        const value = args[i + 1];
        
        switch (key) {
            case '--frontend-url':
                options.frontendUrl = value;
                break;
            case '--api-url':
                options.apiUrl = value;
                break;
            case '--timeout':
                options.timeout = parseInt(value);
                break;
            case '--retries':
                options.retries = parseInt(value);
                break;
            case '--help':
                console.log(`
Deployment Verification Script

Usage: node deployment-verification.js [options]

Options:
  --frontend-url <url>    Frontend URL to verify (required)
  --api-url <url>         API URL to verify (optional)
  --timeout <ms>          Request timeout in milliseconds (default: 30000)
  --retries <count>       Number of retries for failed requests (default: 3)
  --help                  Show this help message

Environment Variables:
  FRONTEND_URL           Frontend URL (alternative to --frontend-url)
  API_URL               API URL (alternative to --api-url)

Examples:
  node deployment-verification.js --frontend-url https://main.d1234567890.amplifyapp.com
  node deployment-verification.js --frontend-url https://main.d1234567890.amplifyapp.com --api-url https://api.example.com
  FRONTEND_URL=https://main.d1234567890.amplifyapp.com node deployment-verification.js
                `);
                process.exit(0);
                break;
        }
    }
    
    const verifier = new DeploymentVerifier(options);
    verifier.verify().catch(error => {
        console.error('Verification failed:', error);
        process.exit(1);
    });
}

module.exports = DeploymentVerifier;