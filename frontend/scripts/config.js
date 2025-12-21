// API Configuration
// Automatically detects environment and sets API URL
// Supports: localhost, sandbox (staging), and production
(function() {
    'use strict';
    
    // API URLs for different environments
    // Using Railway URLs - can be easily switched to custom domains later
    const API_URLS = {
        local: 'http://localhost:8080',
        sandbox: 'https://flow-platform-flow-platform-staging.up.railway.app',
        production: 'https://flow-platform-production.up.railway.app'
    };
    
    // Detect environment from hostname
    const hostname = window.location.hostname;
    const isLocalhost = hostname === 'localhost' || 
                       hostname === '127.0.0.1' ||
                       hostname === '';
    
    // Check if we're on a sandbox domain (only check hostname, not localStorage)
    const isSandboxDomain = hostname.includes('staging') || hostname.includes('stage') || hostname.includes('sandbox');
    
    // Check if user has manually selected an environment (only for API docs, not user pages)
    // For user-facing pages (sign-in, dashboard), always use production unless on sandbox domain
    const manualEnvironment = localStorage.getItem('api_environment');
    const isUserPage = window.location.pathname.includes('signin') || 
                       window.location.pathname.includes('account') ||
                       window.location.pathname.includes('dashboard');
    
    // Determine API URL
    let API_BASE_URL;
    if (isLocalhost) {
        API_BASE_URL = API_URLS.local;
    } else if (isSandboxDomain) {
        // If on sandbox domain, use sandbox API
        API_BASE_URL = API_URLS.sandbox;
    } else if (manualEnvironment && API_URLS[manualEnvironment] && !isUserPage) {
        // Manual override only for API docs, not user pages
        API_BASE_URL = API_URLS[manualEnvironment];
    } else {
        // Default to production for all user-facing pages
        API_BASE_URL = API_URLS.production;
    }
    
    // Make it globally available
    window.API_BASE_URL = API_BASE_URL;
    window.API_URLS = API_URLS;
    window.API_ENVIRONMENT = isLocalhost ? 'local' : (isSandboxDomain ? 'sandbox' : (manualEnvironment && !isUserPage ? manualEnvironment : 'production'));
    
    // Function to switch environment (for documentation toggles)
    window.setApiEnvironment = function(env) {
        if (API_URLS[env]) {
            localStorage.setItem('api_environment', env);
            window.API_BASE_URL = API_URLS[env];
            window.API_ENVIRONMENT = env;
            // Dispatch event so documentation can update
            window.dispatchEvent(new CustomEvent('apiEnvironmentChanged', { detail: env }));
            console.log(`🔧 Switched to ${env} environment:`, API_URLS[env]);
        }
    };
    
    // Function to get current environment
    window.getApiEnvironment = function() {
        return window.API_ENVIRONMENT;
    };
    
    // Log for debugging
    if (isLocalhost) {
        console.log('🔧 Development mode: Using local API at', API_BASE_URL);
    } else {
        console.log(`🚀 ${window.API_ENVIRONMENT} mode: Using API at`, API_BASE_URL);
    }
})();

