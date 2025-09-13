#!/usr/bin/env node

/**
 * Amplify Post-Deploy Verification Script
 * 
 * This script is designed to be run as part of the Amplify build process
 * to verify that the deployment was successful.
 */

const DeploymentVerifier = require('./deployment-verification');

async function runPostDeployVerification() {
    console.log('🚀 Starting Amplify post-deployment verification...');
    
    // Get URLs from environment variables set by Amplify
    const frontendUrl = process.env.AMPLIFY_APP_URL || process.env.FRONTEND_URL;
    const apiUrl = process.env.API_URL;
    
    if (!frontendUrl) {
        console.log('⚠️  Frontend URL not available, skipping verification');
        console.log('Set AMPLIFY_APP_URL or FRONTEND_URL environment variable to enable verification');
        return;
    }
    
    console.log(`Frontend URL: ${frontendUrl}`);
    if (apiUrl) {
        console.log(`API URL: ${apiUrl}`);
    } else {
        console.log('API URL: Not specified (API checks will be skipped)');
    }
    
    const verifier = new DeploymentVerifier({
        frontendUrl: frontendUrl,
        apiUrl: apiUrl,
        timeout: 15000, // Shorter timeout for build process
        retries: 2      // Fewer retries for build process
    });
    
    try {
        const success = await verifier.verify();
        
        if (success) {
            console.log('✅ Post-deployment verification completed successfully!');
            console.log('🎉 Your application is ready to use!');
        } else {
            console.log('❌ Post-deployment verification failed!');
            console.log('⚠️  The build completed but there may be runtime issues.');
            // Don't fail the build, just warn
        }
        
    } catch (error) {
        console.log('❌ Post-deployment verification encountered an error:', error.message);
        console.log('⚠️  This may indicate deployment issues, but the build will continue.');
        // Don't fail the build, just warn
    }
}

// Run verification
runPostDeployVerification().catch(error => {
    console.error('Post-deployment verification script failed:', error);
    // Don't exit with error code to avoid failing the build
});