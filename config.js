// API Configuration
// Automatically detects environment and sets API URL
// Supports: localhost, sandbox (staging), and production
(function() {
    'use strict';
    
    // API URLs for different environments
    // Using Railway URLs - can be easily switched to custom domains later
    const API_URLS = {
        local: 'http://localhost:8080',
        staging: 'https://flow-platform-staging.up.railway.app',
        production: 'https://flow-platform-production.up.railway.app'
    };
    
    // Detect environment from hostname
    const hostname = window.location.hostname;
    const isLocalhost = hostname === 'localhost' || 
                       hostname === '127.0.0.1' ||
                       hostname === '';
    
    // Check if we're on a staging domain (you can customize this)
    const isStaging = hostname.includes('staging') || 
                     hostname.includes('stage') ||
                     localStorage.getItem('api_environment') === 'staging';
    
    // Check if user has manually selected an environment
    const manualEnvironment = localStorage.getItem('api_environment');
    
    // Determine API URL
    let API_BASE_URL;
    if (isLocalhost) {
        API_BASE_URL = API_URLS.local;
    } else if (manualEnvironment && API_URLS[manualEnvironment]) {
        API_BASE_URL = API_URLS[manualEnvironment];
    } else if (isStaging) {
        API_BASE_URL = API_URLS.staging;
    } else {
        API_BASE_URL = API_URLS.production;
    }
    
    // Make it globally available
    window.API_BASE_URL = API_BASE_URL;
    window.API_URLS = API_URLS;
    window.API_ENVIRONMENT = isLocalhost ? 'local' : (manualEnvironment || (isStaging ? 'staging' : 'production'));
    
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

