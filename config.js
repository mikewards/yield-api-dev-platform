// API Configuration
// Automatically detects environment and sets API URL
(function() {
    'use strict';
    
    // Detect environment
    const isLocalhost = window.location.hostname === 'localhost' || 
                       window.location.hostname === '127.0.0.1' ||
                       window.location.hostname === '';
    
    // Set API base URL based on environment
    // For local development, use localhost:8080
    // For production (Cloudflare Pages), use Railway URL
    const API_BASE_URL = isLocalhost 
        ? 'http://localhost:8080'
        : 'https://flow-platform-production.up.railway.app';
    
    // Make it globally available
    window.API_BASE_URL = API_BASE_URL;
    
    // Log for debugging (remove in production if needed)
    if (isLocalhost) {
        console.log('🔧 Development mode: Using local API at', API_BASE_URL);
    } else {
        console.log('🚀 Production mode: Using Railway API at', API_BASE_URL);
    }
})();

