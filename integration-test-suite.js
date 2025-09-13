#!/usr/bin/env node

/**
 * Integration Test Suite for Amplify Deployment
 * 
 * Comprehensive integration testing and deployment verification that covers:
 * - Modified amplify.yml configuration test deployment
 * - Frontend and backend integration verification
 * - Performance testing and load testing
 * - Production environment final verification
 * 
 * Requirements: 4.1, 4.2, 4.3, 5.1
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');
const DeploymentVerifier = require('./deployment-verification');
const PreDeploymentChecker = require('./pre-deployment-checker');
const ContinuousDeploymentMonitor = require('./continuous-deployment-monitor');

class IntegrationTestSuite {
    constructor(options = {}) {
        this.options = {
            frontendUrl: options.frontendUrl || process.env.FRONTEND_URL,
            apiUrl: options.apiUrl || process.env.API_URL,
            amplifyAppId: options.amplifyAppId || process.env.AMPLIFY_APP_ID,
            skipDeployment: options.skipDeployment || false,
            performanceThreshold: options.performanceThreshold || 3000, // 3 seconds
            loadTestDuration: options.loadTestDuration || 30, // 30 seconds
            loadTestConcurrency: options.loadTestConcurrency || 10,
            ...options
        };
        
        this.testResults = {
            timestamp: new Date().toISOString(),
            success: false,
            phases: {
                preDeploymentCheck: { status: 'PENDING', results: null },
                testDeployment: { status: 'PENDING', results: null },
                integrationVerification: { status: 'PENDING', results: null },
                performanceTesting: { status: 'PENDING', results: null },
                loadTesting: { status: 'PENDING', results: null },
                productionVerification: { status: 'PENDING', results: null }
            },
            errors: [],
            warnings: [],
            metrics: {}
        };
    }

    /**
     * Run complete integration test suite
     */
    async runIntegrationTests() {
        console.log('🚀 Starting Integration Test Suite for Amplify Deployment');
        console.log('========================================================\n');
        
        try {
            // Phase 1: Pre-deployment validation
            await this.runPreDeploymentCheck();
            
            // Phase 2: Test deployment with modified amplify.yml
            if (!this.options.skipDeployment) {
                await this.runTestDeployment();
            } else {
                console.log('⚠️  Skipping test deployment (skipDeployment=true)');
                this.testResults.phases.testDeployment.status = 'SKIPPED';
            }
            
            // Phase 3: Frontend and backend integration verification
            await this.runIntegrationVerification();
            
            // Phase 4: Performance testing
            await this.runPerformanceTesting();
            
            // Phase 5: Load testing
            await this.runLoadTesting();
            
            // Phase 6: Production environment final verification
            await this.runProductionVerification();
            
            // Generate comprehensive report
            this.generateIntegrationReport();
            
            // Determine overall success
            const failedPhases = Object.values(this.testResults.phases)
                .filter(phase => phase.status === 'FAILED').length;
            
            this.testResults.success = failedPhases === 0;
            
            if (this.testResults.success) {
                console.log('✅ Integration test suite completed successfully!');
                return true;
            } else {
                console.log(`❌ Integration test suite failed (${failedPhases} phases failed)`);
                process.exit(1);
            }
            
        } catch (error) {
            console.error('💥 Integration test suite execution failed:', error.message);
            this.testResults.errors.push(`Suite execution failed: ${error.message}`);
            this.generateIntegrationReport();
            process.exit(1);
        }
    }

    /**
     * Phase 1: Pre-deployment validation
     */
    async runPreDeploymentCheck() {
        console.log('📋 Phase 1: Pre-deployment Validation');
        console.log('=====================================\n');
        
        try {
            const checker = new PreDeploymentChecker({
                verbose: true,
                skipDependencyCheck: false,
                skipCommandCheck: false
            });
            
            const result = await checker.runPreDeploymentChecks();
            
            this.testResults.phases.preDeploymentCheck = {
                status: result ? 'SUCCESS' : 'FAILED',
                results: {
                    passed: result,
                    errors: checker.errors,
                    warnings: checker.warnings,
                    successes: checker.successes
                },
                timestamp: new Date().toISOString()
            };
            
            if (!result) {
                this.testResults.errors.push('Pre-deployment checks failed');
                console.log('❌ Pre-deployment validation failed - stopping integration tests');
                throw new Error('Pre-deployment validation failed');
            }
            
            console.log('✅ Pre-deployment validation completed successfully\n');
            
        } catch (error) {
            this.testResults.phases.preDeploymentCheck.status = 'FAILED';
            this.testResults.errors.push(`Pre-deployment check failed: ${error.message}`);
            throw error;
        }
    }

    /**
     * Phase 2: Test deployment with modified amplify.yml
     */
    async runTestDeployment() {
        console.log('🚀 Phase 2: Test Deployment with Modified amplify.yml');
        console.log('===================================================\n');
        
        try {
            // Validate amplify.yml configuration
            await this.validateAmplifyConfiguration();
            
            // Simulate deployment process
            const deploymentResult = await this.simulateDeployment();
            
            this.testResults.phases.testDeployment = {
                status: deploymentResult.success ? 'SUCCESS' : 'FAILED',
                results: deploymentResult,
                timestamp: new Date().toISOString()
            };
            
            if (!deploymentResult.success) {
                this.testResults.errors.push('Test deployment failed');
                throw new Error(`Test deployment failed: ${deploymentResult.error}`);
            }
            
            console.log('✅ Test deployment completed successfully\n');
            
        } catch (error) {
            this.testResults.phases.testDeployment.status = 'FAILED';
            this.testResults.errors.push(`Test deployment failed: ${error.message}`);
            throw error;
        }
    }

    /**
     * Validate amplify.yml configuration
     */
    async validateAmplifyConfiguration() {
        console.log('🔍 Validating amplify.yml configuration...');
        
        if (!fs.existsSync('amplify.yml')) {
            throw new Error('amplify.yml not found');
        }
        
        const content = fs.readFileSync('amplify.yml', 'utf8');
        console.log('✓ amplify.yml file exists and is readable');
        
        // Check for required sections
        const requiredSections = ['version', 'frontend', 'backend'];
        for (const section of requiredSections) {
            if (!content.includes(`${section}:`)) {
                throw new Error(`amplify.yml missing required section: ${section}`);
            }
        }
        console.log('✓ All required sections present in amplify.yml');
        
        // Validate frontend configuration
        if (!content.includes('baseDirectory: frontend')) {
            throw new Error('Frontend baseDirectory not set to "frontend"');
        }
        console.log('✓ Frontend configuration validated');
        
        // Validate backend configuration
        if (!content.includes('baseDirectory: backend/target')) {
            throw new Error('Backend baseDirectory not set to "backend/target"');
        }
        console.log('✓ Backend configuration validated');
        
        console.log('✅ amplify.yml configuration validation completed');
    }

    /**
     * Simulate deployment process
     */
    async simulateDeployment() {
        console.log('🔄 Simulating deployment process...');
        
        const monitor = new ContinuousDeploymentMonitor({
            amplifyAppId: this.options.amplifyAppId || 'test-app-id',
            branchName: 'main',
            rollbackEnabled: false
        });
        
        const deploymentId = `integration-test-${Date.now()}`;
        
        try {
            const result = await monitor.trackDeploymentProgress(deploymentId);
            
            if (result.success) {
                console.log('✅ Deployment simulation completed successfully');
                return {
                    success: true,
                    deploymentId,
                    duration: result.duration,
                    phases: result.deployment.phases
                };
            } else {
                return {
                    success: false,
                    error: result.error,
                    deploymentId
                };
            }
            
        } catch (error) {
            return {
                success: false,
                error: error.message,
                deploymentId
            };
        }
    }

    /**
     * Phase 3: Frontend and backend integration verification
     */
    async runIntegrationVerification() {
        console.log('🔗 Phase 3: Frontend and Backend Integration Verification');
        console.log('========================================================\n');
        
        try {
            if (!this.options.frontendUrl) {
                throw new Error('Frontend URL not provided - cannot run integration verification');
            }
            
            const verifier = new DeploymentVerifier({
                frontendUrl: this.options.frontendUrl,
                apiUrl: this.options.apiUrl,
                timeout: 30000,
                retries: 3
            });
            
            const result = await verifier.verify();
            
            this.testResults.phases.integrationVerification = {
                status: result ? 'SUCCESS' : 'FAILED',
                results: verifier.results,
                timestamp: new Date().toISOString()
            };
            
            if (!result) {
                this.testResults.errors.push('Integration verification failed');
                throw new Error('Integration verification failed');
            }
            
            console.log('✅ Integration verification completed successfully\n');
            
        } catch (error) {
            this.testResults.phases.integrationVerification.status = 'FAILED';
            this.testResults.errors.push(`Integration verification failed: ${error.message}`);
            throw error;
        }
    }

    /**
     * Phase 4: Performance testing
     */
    async runPerformanceTesting() {
        console.log('⚡ Phase 4: Performance Testing');
        console.log('===============================\n');
        
        try {
            if (!this.options.frontendUrl) {
                console.log('⚠️  Frontend URL not provided - skipping performance testing');
                this.testResults.phases.performanceTesting.status = 'SKIPPED';
                return;
            }
            
            const performanceResults = await this.measurePerformance();
            
            this.testResults.phases.performanceTesting = {
                status: performanceResults.passed ? 'SUCCESS' : 'FAILED',
                results: performanceResults,
                timestamp: new Date().toISOString()
            };
            
            if (!performanceResults.passed) {
                this.testResults.warnings.push('Performance testing failed - may impact user experience');
            }
            
            console.log('✅ Performance testing completed\n');
            
        } catch (error) {
            this.testResults.phases.performanceTesting.status = 'FAILED';
            this.testResults.warnings.push(`Performance testing failed: ${error.message}`);
            console.log(`⚠️  Performance testing failed: ${error.message}\n`);
        }
    }

    /**
     * Measure performance metrics
     */
    async measurePerformance() {
        console.log('📊 Measuring performance metrics...');
        
        const metrics = {
            pageLoadTime: null,
            apiResponseTime: null,
            staticResourceLoadTime: null,
            totalLoadTime: null
        };
        
        // Measure page load time
        const pageLoadStart = Date.now();
        try {
            await this.makeHttpRequest(this.options.frontendUrl);
            metrics.pageLoadTime = Date.now() - pageLoadStart;
            console.log(`✓ Page load time: ${metrics.pageLoadTime}ms`);
        } catch (error) {
            console.log(`❌ Page load test failed: ${error.message}`);
            throw error;
        }
        
        // Measure API response time (if API URL provided)
        if (this.options.apiUrl) {
            const apiStart = Date.now();
            try {
                await this.makeHttpRequest(this.options.apiUrl + '/health');
                metrics.apiResponseTime = Date.now() - apiStart;
                console.log(`✓ API response time: ${metrics.apiResponseTime}ms`);
            } catch (error) {
                console.log(`⚠️  API response test failed: ${error.message}`);
                metrics.apiResponseTime = -1; // Indicate failure
            }
        }
        
        // Measure static resource load time
        const staticStart = Date.now();
        try {
            await this.makeHttpRequest(this.options.frontendUrl + '/css/style.css');
            metrics.staticResourceLoadTime = Date.now() - staticStart;
            console.log(`✓ Static resource load time: ${metrics.staticResourceLoadTime}ms`);
        } catch (error) {
            console.log(`⚠️  Static resource test failed: ${error.message}`);
            metrics.staticResourceLoadTime = -1;
        }
        
        // Calculate total load time (page + static resources)
        if (metrics.pageLoadTime && metrics.staticResourceLoadTime > 0) {
            metrics.totalLoadTime = metrics.pageLoadTime + metrics.staticResourceLoadTime;
        } else {
            metrics.totalLoadTime = metrics.pageLoadTime;
        }
        
        // Evaluate performance
        const passed = metrics.totalLoadTime <= this.options.performanceThreshold;
        
        console.log(`📈 Total load time: ${metrics.totalLoadTime}ms`);
        console.log(`🎯 Performance threshold: ${this.options.performanceThreshold}ms`);
        console.log(`${passed ? '✅' : '❌'} Performance test: ${passed ? 'PASSED' : 'FAILED'}`);
        
        this.testResults.metrics.performance = metrics;
        
        return {
            passed,
            metrics,
            threshold: this.options.performanceThreshold
        };
    }

    /**
     * Phase 5: Load testing
     */
    async runLoadTesting() {
        console.log('🔥 Phase 5: Load Testing');
        console.log('========================\n');
        
        try {
            if (!this.options.frontendUrl) {
                console.log('⚠️  Frontend URL not provided - skipping load testing');
                this.testResults.phases.loadTesting.status = 'SKIPPED';
                return;
            }
            
            const loadTestResults = await this.performLoadTest();
            
            this.testResults.phases.loadTesting = {
                status: loadTestResults.passed ? 'SUCCESS' : 'FAILED',
                results: loadTestResults,
                timestamp: new Date().toISOString()
            };
            
            if (!loadTestResults.passed) {
                this.testResults.warnings.push('Load testing failed - application may not handle concurrent users well');
            }
            
            console.log('✅ Load testing completed\n');
            
        } catch (error) {
            this.testResults.phases.loadTesting.status = 'FAILED';
            this.testResults.warnings.push(`Load testing failed: ${error.message}`);
            console.log(`⚠️  Load testing failed: ${error.message}\n`);
        }
    }

    /**
     * Perform load test
     */
    async performLoadTest() {
        console.log(`🚀 Starting load test: ${this.options.loadTestConcurrency} concurrent users for ${this.options.loadTestDuration}s`);
        
        const startTime = Date.now();
        const endTime = startTime + (this.options.loadTestDuration * 1000);
        const results = {
            totalRequests: 0,
            successfulRequests: 0,
            failedRequests: 0,
            averageResponseTime: 0,
            minResponseTime: Infinity,
            maxResponseTime: 0,
            responseTimes: []
        };
        
        const workers = [];
        
        // Create concurrent workers
        for (let i = 0; i < this.options.loadTestConcurrency; i++) {
            workers.push(this.loadTestWorker(endTime, results));
        }
        
        // Wait for all workers to complete
        await Promise.all(workers);
        
        // Calculate metrics
        if (results.responseTimes.length > 0) {
            results.averageResponseTime = results.responseTimes.reduce((a, b) => a + b, 0) / results.responseTimes.length;
            results.minResponseTime = Math.min(...results.responseTimes);
            results.maxResponseTime = Math.max(...results.responseTimes);
        }
        
        const successRate = (results.successfulRequests / results.totalRequests) * 100;
        const passed = successRate >= 95; // 95% success rate threshold
        
        console.log(`📊 Load test results:`);
        console.log(`   Total requests: ${results.totalRequests}`);
        console.log(`   Successful: ${results.successfulRequests}`);
        console.log(`   Failed: ${results.failedRequests}`);
        console.log(`   Success rate: ${successRate.toFixed(2)}%`);
        console.log(`   Average response time: ${results.averageResponseTime.toFixed(2)}ms`);
        console.log(`   Min response time: ${results.minResponseTime}ms`);
        console.log(`   Max response time: ${results.maxResponseTime}ms`);
        console.log(`${passed ? '✅' : '❌'} Load test: ${passed ? 'PASSED' : 'FAILED'}`);
        
        this.testResults.metrics.loadTest = results;
        
        return {
            passed,
            ...results,
            successRate,
            duration: this.options.loadTestDuration
        };
    }

    /**
     * Load test worker
     */
    async loadTestWorker(endTime, results) {
        while (Date.now() < endTime) {
            const requestStart = Date.now();
            
            try {
                await this.makeHttpRequest(this.options.frontendUrl);
                const responseTime = Date.now() - requestStart;
                
                results.totalRequests++;
                results.successfulRequests++;
                results.responseTimes.push(responseTime);
                
            } catch (error) {
                results.totalRequests++;
                results.failedRequests++;
            }
            
            // Small delay to prevent overwhelming the server
            await new Promise(resolve => setTimeout(resolve, 100));
        }
    }

    /**
     * Phase 6: Production environment final verification
     */
    async runProductionVerification() {
        console.log('🏭 Phase 6: Production Environment Final Verification');
        console.log('===================================================\n');
        
        try {
            const verificationResults = await this.performProductionVerification();
            
            this.testResults.phases.productionVerification = {
                status: verificationResults.passed ? 'SUCCESS' : 'FAILED',
                results: verificationResults,
                timestamp: new Date().toISOString()
            };
            
            if (!verificationResults.passed) {
                this.testResults.errors.push('Production verification failed');
                throw new Error('Production verification failed');
            }
            
            console.log('✅ Production verification completed successfully\n');
            
        } catch (error) {
            this.testResults.phases.productionVerification.status = 'FAILED';
            this.testResults.errors.push(`Production verification failed: ${error.message}`);
            throw error;
        }
    }

    /**
     * Perform production environment verification
     */
    async performProductionVerification() {
        console.log('🔍 Performing production environment verification...');
        
        const checks = {
            httpsEnabled: false,
            securityHeaders: false,
            cacheHeaders: false,
            compressionEnabled: false,
            errorHandling: false
        };
        
        if (!this.options.frontendUrl) {
            throw new Error('Frontend URL required for production verification');
        }
        
        try {
            // Check HTTPS
            if (this.options.frontendUrl.startsWith('https://')) {
                checks.httpsEnabled = true;
                console.log('✓ HTTPS enabled');
            } else {
                console.log('❌ HTTPS not enabled');
            }
            
            // Check security and cache headers
            const response = await this.makeHttpRequestWithHeaders(this.options.frontendUrl);
            
            // Security headers check
            const securityHeaders = ['x-frame-options', 'x-content-type-options', 'x-xss-protection'];
            const foundSecurityHeaders = securityHeaders.filter(header => 
                response.headers[header] || response.headers[header.toLowerCase()]
            );
            
            if (foundSecurityHeaders.length > 0) {
                checks.securityHeaders = true;
                console.log(`✓ Security headers present: ${foundSecurityHeaders.join(', ')}`);
            } else {
                console.log('⚠️  No security headers found');
            }
            
            // Cache headers check
            const cacheHeaders = ['cache-control', 'etag', 'expires'];
            const foundCacheHeaders = cacheHeaders.filter(header => 
                response.headers[header] || response.headers[header.toLowerCase()]
            );
            
            if (foundCacheHeaders.length > 0) {
                checks.cacheHeaders = true;
                console.log(`✓ Cache headers present: ${foundCacheHeaders.join(', ')}`);
            } else {
                console.log('⚠️  No cache headers found');
            }
            
            // Compression check
            const contentEncoding = response.headers['content-encoding'] || response.headers['Content-Encoding'];
            if (contentEncoding && (contentEncoding.includes('gzip') || contentEncoding.includes('br'))) {
                checks.compressionEnabled = true;
                console.log(`✓ Compression enabled: ${contentEncoding}`);
            } else {
                console.log('⚠️  Compression not detected');
            }
            
            // Error handling check (try accessing non-existent page)
            try {
                await this.makeHttpRequest(this.options.frontendUrl + '/non-existent-page');
            } catch (error) {
                if (error.message.includes('404')) {
                    checks.errorHandling = true;
                    console.log('✓ 404 error handling working');
                } else {
                    console.log('⚠️  Error handling unclear');
                }
            }
            
            const passedChecks = Object.values(checks).filter(Boolean).length;
            const totalChecks = Object.keys(checks).length;
            const passed = passedChecks >= Math.ceil(totalChecks * 0.6); // 60% threshold
            
            console.log(`📊 Production checks: ${passedChecks}/${totalChecks} passed`);
            console.log(`${passed ? '✅' : '❌'} Production verification: ${passed ? 'PASSED' : 'FAILED'}`);
            
            return {
                passed,
                checks,
                passedChecks,
                totalChecks
            };
            
        } catch (error) {
            console.log(`❌ Production verification failed: ${error.message}`);
            return {
                passed: false,
                error: error.message,
                checks
            };
        }
    }

    /**
     * Make HTTP request
     */
    makeHttpRequest(url, method = 'GET') {
        return new Promise((resolve, reject) => {
            const https = require('https');
            const http = require('http');
            const { URL } = require('url');
            
            const urlObj = new URL(url);
            const isHttps = urlObj.protocol === 'https:';
            const client = isHttps ? https : http;
            
            const options = {
                hostname: urlObj.hostname,
                port: urlObj.port || (isHttps ? 443 : 80),
                path: urlObj.pathname + urlObj.search,
                method: method,
                headers: {
                    'User-Agent': 'IntegrationTestSuite/1.0'
                },
                timeout: 10000,
                // For testing with self-signed certificates or localhost HTTPS
                rejectUnauthorized: false
            };
            
            const req = client.request(options, (res) => {
                let data = '';
                res.on('data', chunk => data += chunk);
                res.on('end', () => resolve({ statusCode: res.statusCode, data, headers: res.headers }));
            });
            
            req.on('error', (error) => {
                // Handle SSL/TLS errors gracefully for testing
                if (error.code === 'EPROTO' || error.code === 'ECONNRESET') {
                    reject(new Error(`Connection error: ${error.message}`));
                } else {
                    reject(error);
                }
            });
            
            req.on('timeout', () => {
                req.destroy();
                reject(new Error('Request timeout'));
            });
            
            req.end();
        });
    }

    /**
     * Make HTTP request with headers
     */
    async makeHttpRequestWithHeaders(url) {
        const response = await this.makeHttpRequest(url);
        return response;
    }

    /**
     * Generate comprehensive integration report
     */
    generateIntegrationReport() {
        console.log('\n📊 Integration Test Suite Report');
        console.log('=================================\n');
        
        // Summary
        const phases = Object.entries(this.testResults.phases);
        const completedPhases = phases.filter(([_, phase]) => phase.status !== 'PENDING').length;
        const successfulPhases = phases.filter(([_, phase]) => phase.status === 'SUCCESS').length;
        const failedPhases = phases.filter(([_, phase]) => phase.status === 'FAILED').length;
        const skippedPhases = phases.filter(([_, phase]) => phase.status === 'SKIPPED').length;
        
        console.log(`📈 Summary:`);
        console.log(`   Total phases: ${phases.length}`);
        console.log(`   Completed: ${completedPhases}`);
        console.log(`   Successful: ${successfulPhases} ✅`);
        console.log(`   Failed: ${failedPhases} ❌`);
        console.log(`   Skipped: ${skippedPhases} ⚠️`);
        console.log(`   Overall success: ${this.testResults.success ? 'YES' : 'NO'}`);
        
        // Phase details
        console.log(`\n📋 Phase Results:`);
        phases.forEach(([phaseName, phase]) => {
            const statusIcon = {
                'SUCCESS': '✅',
                'FAILED': '❌',
                'SKIPPED': '⚠️',
                'PENDING': '⏳'
            }[phase.status] || '❓';
            
            console.log(`   ${statusIcon} ${phaseName}: ${phase.status}`);
        });
        
        // Performance metrics
        if (this.testResults.metrics.performance) {
            console.log(`\n⚡ Performance Metrics:`);
            const perf = this.testResults.metrics.performance;
            console.log(`   Page load time: ${perf.pageLoadTime}ms`);
            if (perf.apiResponseTime > 0) {
                console.log(`   API response time: ${perf.apiResponseTime}ms`);
            }
            if (perf.staticResourceLoadTime > 0) {
                console.log(`   Static resource load time: ${perf.staticResourceLoadTime}ms`);
            }
            console.log(`   Total load time: ${perf.totalLoadTime}ms`);
        }
        
        // Load test metrics
        if (this.testResults.metrics.loadTest) {
            console.log(`\n🔥 Load Test Metrics:`);
            const load = this.testResults.metrics.loadTest;
            console.log(`   Total requests: ${load.totalRequests}`);
            console.log(`   Success rate: ${((load.successfulRequests / load.totalRequests) * 100).toFixed(2)}%`);
            console.log(`   Average response time: ${load.averageResponseTime.toFixed(2)}ms`);
        }
        
        // Errors and warnings
        if (this.testResults.errors.length > 0) {
            console.log(`\n❌ Errors:`);
            this.testResults.errors.forEach(error => console.log(`   - ${error}`));
        }
        
        if (this.testResults.warnings.length > 0) {
            console.log(`\n⚠️  Warnings:`);
            this.testResults.warnings.forEach(warning => console.log(`   - ${warning}`));
        }
        
        // Save detailed report
        const reportPath = 'integration-test-report.json';
        try {
            fs.writeFileSync(reportPath, JSON.stringify(this.testResults, null, 2));
            console.log(`\n📄 Detailed report saved to: ${reportPath}`);
        } catch (error) {
            console.warn(`Failed to save report: ${error.message}`);
        }
        
        console.log('\n' + '='.repeat(50));
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
            case '--amplify-app-id':
                options.amplifyAppId = value;
                break;
            case '--skip-deployment':
                options.skipDeployment = true;
                i--; // No value for this flag
                break;
            case '--performance-threshold':
                options.performanceThreshold = parseInt(value);
                break;
            case '--load-test-duration':
                options.loadTestDuration = parseInt(value);
                break;
            case '--load-test-concurrency':
                options.loadTestConcurrency = parseInt(value);
                break;
            case '--help':
                console.log(`
Integration Test Suite for Amplify Deployment

Usage: node integration-test-suite.js [options]

Options:
  --frontend-url <url>           Frontend URL to test (required)
  --api-url <url>               API URL to test (optional)
  --amplify-app-id <id>         Amplify App ID (optional)
  --skip-deployment             Skip test deployment phase
  --performance-threshold <ms>   Performance threshold in milliseconds (default: 3000)
  --load-test-duration <s>      Load test duration in seconds (default: 30)
  --load-test-concurrency <n>   Number of concurrent users for load test (default: 10)
  --help                        Show this help message

Environment Variables:
  FRONTEND_URL                  Frontend URL (alternative to --frontend-url)
  API_URL                      API URL (alternative to --api-url)
  AMPLIFY_APP_ID               Amplify App ID (alternative to --amplify-app-id)

Examples:
  node integration-test-suite.js --frontend-url https://main.d1234567890.amplifyapp.com
  node integration-test-suite.js --frontend-url https://example.com --api-url https://api.example.com --skip-deployment
  FRONTEND_URL=https://example.com node integration-test-suite.js
                `);
                process.exit(0);
                break;
        }
    }
    
    const suite = new IntegrationTestSuite(options);
    suite.runIntegrationTests().catch(error => {
        console.error('Integration test suite failed:', error);
        process.exit(1);
    });
}

module.exports = IntegrationTestSuite;