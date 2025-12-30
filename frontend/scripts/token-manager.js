/**
 * Token Manager - Handles OAuth 2.0-style token refresh
 * 
 * Features:
 * - Silent token refresh before expiration
 * - Token rotation for security
 * - Graceful session expiration handling
 * - Race condition prevention
 * - Activity-based session detection
 * 
 * Last Updated: December 2025
 */
const TokenManager = {
    refreshTimer: null,
    activityTimer: null,
    isRefreshing: false,
    sessionExpired: false,  // Flag to prevent multiple session expired handling
    initialized: false,
    initPromise: null,
    
    // Configuration
    config: {
        refreshBeforeExpiry: 2 * 60 * 1000,  // Refresh 2 minutes before expiry
        inactivityTimeout: 30 * 60 * 1000,   // 30 minutes of inactivity
        checkInterval: 60 * 1000,             // Check every minute
        maxRefreshRetries: 2                  // Max retries on network error
    },
    
    refreshRetryCount: 0,
    
    /**
     * Initialize the token manager
     * Returns a promise that resolves when initialization is complete
     */
    async init() {
        // Prevent multiple initializations
        if (this.initialized) {
            return;
        }
        
        // If already initializing, return the existing promise
        if (this.initPromise) {
            return this.initPromise;
        }
        
        this.initPromise = this._doInit();
        return this.initPromise;
    },
    
    async _doInit() {
        const token = localStorage.getItem('tbd_token');
        const refreshToken = localStorage.getItem('tbd_refresh_token');
        
        if (!token && !refreshToken) {
            console.log('🔐 TokenManager: No tokens found');
            this.initialized = true;
            return;
        }
        
        // Check if we need to refresh immediately
        const expiresAt = parseInt(localStorage.getItem('tbd_token_expires') || '0');
        const now = Date.now();
        
        if (expiresAt && expiresAt < now) {
            // Access token is expired, try to refresh
            if (refreshToken) {
                console.log('🔐 TokenManager: Access token expired, attempting refresh...');
                const success = await this.refresh();
                if (!success) {
                    // Refresh failed - session is truly expired
                    console.log('🔐 TokenManager: Refresh failed, session expired');
                    this.initialized = true;
                    return;
                }
            } else {
                // No refresh token, session expired
                console.log('🔐 TokenManager: No refresh token, session expired');
                this.sessionExpired = true;
                this.initialized = true;
                return;
            }
        }
        
        // Schedule next refresh
        this.scheduleRefresh();
        this.trackActivity();
        
        console.log('🔐 TokenManager initialized successfully');
        this.initialized = true;
    },
    
    /**
     * Schedule the next token refresh
     */
    scheduleRefresh() {
        // Don't schedule if session is already expired
        if (this.sessionExpired) {
            return;
        }
        
        const expiresAt = localStorage.getItem('tbd_token_expires');
        if (!expiresAt) {
            // No expiry stored, use default 15 min from now
            const defaultExpiry = Date.now() + (15 * 60 * 1000);
            localStorage.setItem('tbd_token_expires', defaultExpiry.toString());
        }
        
        const expiry = parseInt(localStorage.getItem('tbd_token_expires'));
        const refreshAt = expiry - this.config.refreshBeforeExpiry;
        const timeUntilRefresh = refreshAt - Date.now();
        
        clearTimeout(this.refreshTimer);
        
        if (timeUntilRefresh <= 0) {
            // Token needs refresh now
            this.refresh();
        } else {
            this.refreshTimer = setTimeout(() => this.refresh(), timeUntilRefresh);
            console.log(`🕐 Token refresh scheduled in ${Math.round(timeUntilRefresh / 1000 / 60)} minutes`);
        }
    },
    
    /**
     * Refresh the access token using the refresh token
     * Returns true if successful, false otherwise
     */
    async refresh() {
        // Don't refresh if session is already marked as expired
        if (this.sessionExpired) {
            console.log('🔄 Session already expired, skipping refresh');
            return false;
        }
        
        // Prevent concurrent refreshes
        if (this.isRefreshing) {
            console.log('🔄 Refresh already in progress, waiting...');
            // Wait for the current refresh to complete
            await new Promise(resolve => {
                const checkInterval = setInterval(() => {
                    if (!this.isRefreshing) {
                        clearInterval(checkInterval);
                        resolve();
                    }
                }, 100);
            });
            // Return current auth state after waiting
            return this.hasValidTokens();
        }
        
        const refreshToken = localStorage.getItem('tbd_refresh_token');
        if (!refreshToken) {
            console.log('❌ No refresh token available');
            this.markSessionExpired();
            return false;
        }
        
        this.isRefreshing = true;
        
        try {
            const response = await fetch(`${window.API_BASE_URL}/v1/auth/refresh`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ refresh_token: refreshToken })
            });
            
            if (response.ok) {
                const data = await response.json();
                
                // Store new tokens
                localStorage.setItem('tbd_token', data.access_token);
                localStorage.setItem('tbd_refresh_token', data.refresh_token);
                localStorage.setItem('tbd_token_expires', (Date.now() + (data.expires_in * 1000)).toString());
                
                console.log('✅ Token refreshed silently');
                this.refreshRetryCount = 0;
                
                // Schedule next refresh
                this.scheduleRefresh();
                
                // Dispatch event for any listeners
                window.dispatchEvent(new CustomEvent('tokenRefreshed'));
                
                this.isRefreshing = false;
                return true;
            } else {
                // Refresh failed - token is invalid
                console.log('❌ Token refresh failed:', response.status);
                this.markSessionExpired();
                this.isRefreshing = false;
                return false;
            }
        } catch (error) {
            console.error('❌ Token refresh network error:', error);
            this.isRefreshing = false;
            
            // On network error, retry a limited number of times
            this.refreshRetryCount++;
            if (this.refreshRetryCount < this.config.maxRefreshRetries) {
                console.log(`🔄 Retrying refresh (attempt ${this.refreshRetryCount + 1}/${this.config.maxRefreshRetries})...`);
                await new Promise(resolve => setTimeout(resolve, 2000)); // Wait 2 seconds
                return this.refresh();
            } else {
                console.log('❌ Max refresh retries exceeded');
                // Don't mark as expired on network error - user might have intermittent connectivity
                // Just schedule another attempt later
                this.refreshRetryCount = 0;
                setTimeout(() => this.refresh(), 30000);
                return false;
            }
        }
    },
    
    /**
     * Mark the session as expired and show modal
     */
    markSessionExpired() {
        // Prevent multiple triggers
        if (this.sessionExpired) {
            return;
        }
        
        console.log('🔒 Marking session as expired');
        this.sessionExpired = true;
        clearTimeout(this.refreshTimer);
        clearInterval(this.activityTimer);
        
        // Clear tokens immediately to prevent any further API calls from succeeding
        this.clearTokens();
        
        // Show modal
        this.showSessionExpiredModal();
    },
    
    /**
     * Track user activity for inactivity detection
     */
    trackActivity() {
        const events = ['mousedown', 'mousemove', 'keydown', 'scroll', 'touchstart'];
        let lastActivity = Date.now();
        
        const updateActivity = () => {
            lastActivity = Date.now();
        };
        
        events.forEach(event => {
            document.addEventListener(event, updateActivity, { passive: true });
        });
        
        // Check for inactivity periodically
        clearInterval(this.activityTimer);
        this.activityTimer = setInterval(() => {
            const inactiveTime = Date.now() - lastActivity;
            if (inactiveTime > this.config.inactivityTimeout) {
                console.log('⚠️ User inactive for 30 minutes');
            }
        }, this.config.checkInterval);
    },
    
    /**
     * Handle expired session - legacy method, calls markSessionExpired
     */
    handleSessionExpired() {
        this.markSessionExpired();
    },
    
    /**
     * Show a beautiful session expired modal
     */
    showSessionExpiredModal() {
        // Remove existing modal if present
        const existing = document.getElementById('session-expired-modal');
        if (existing) {
            return; // Modal already showing
        }
        
        const modal = document.createElement('div');
        modal.id = 'session-expired-modal';
        modal.innerHTML = `
            <div class="session-modal-overlay">
                <div class="session-modal">
                    <div class="session-modal-icon">
                        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <circle cx="12" cy="12" r="10" stroke="#6366f1" stroke-width="2"/>
                            <path d="M12 6v6l4 2" stroke="#6366f1" stroke-width="2" stroke-linecap="round"/>
                        </svg>
                    </div>
                    <h3>Session Expired</h3>
                    <p>Your session has expired for security reasons. Please sign in again to continue.</p>
                    <button class="btn btn-primary session-modal-btn" onclick="TokenManager.redirectToLogin()">
                        Sign In Again
                    </button>
                </div>
            </div>
        `;
        document.body.appendChild(modal);
        
        // Prevent scrolling and interaction with page behind
        document.body.style.overflow = 'hidden';
    },
    
    /**
     * Redirect to login and clear all tokens
     */
    redirectToLogin() {
        this.clearTokens();
        document.body.style.overflow = '';
        
        // Store the current page to redirect back after login
        const currentPath = window.location.pathname;
        if (currentPath !== '/signin.html' && currentPath !== '/signin' && 
            currentPath !== '/account.html' && currentPath !== '/account' &&
            !currentPath.includes('signin') && !currentPath.includes('account')) {
            localStorage.setItem('tbd_redirect_after_login', currentPath);
        }
        
        window.location.href = 'signin.html';
    },
    
    /**
     * Clear all stored tokens
     */
    clearTokens() {
        localStorage.removeItem('tbd_token');
        localStorage.removeItem('tbd_refresh_token');
        localStorage.removeItem('tbd_token_expires');
        localStorage.removeItem('tbd_account_id');
        localStorage.removeItem('tbd_username');
    },
    
    /**
     * Logout - revoke tokens on server and clear local storage
     */
    async logout() {
        const token = localStorage.getItem('tbd_token');
        
        // Best effort to notify server
        if (token) {
            try {
                await fetch(`${window.API_BASE_URL}/v1/auth/logout`, {
                    method: 'POST',
                    headers: { 
                        'Authorization': `Bearer ${token}`,
                        'Content-Type': 'application/json'
                    }
                });
                console.log('🚪 Server notified of logout');
            } catch (e) {
                console.log('⚠️ Could not notify server of logout');
            }
        }
        
        clearTimeout(this.refreshTimer);
        clearInterval(this.activityTimer);
        this.sessionExpired = true;
        this.clearTokens();
        window.location.href = 'signin.html';
    },
    
    /**
     * Check if tokens exist and are potentially valid
     * This is a quick synchronous check - doesn't verify with server
     */
    hasValidTokens() {
        const token = localStorage.getItem('tbd_token');
        const refreshToken = localStorage.getItem('tbd_refresh_token');
        const expiresAt = localStorage.getItem('tbd_token_expires');
        
        // No tokens at all
        if (!token && !refreshToken) {
            return false;
        }
        
        // Have refresh token - could potentially refresh
        if (refreshToken && !this.sessionExpired) {
            return true;
        }
        
        // Have access token that's not expired
        if (token && expiresAt && parseInt(expiresAt) > Date.now()) {
            return true;
        }
        
        return false;
    },
    
    /**
     * Check if user is authenticated
     * Returns false if session has been marked as expired
     */
    isAuthenticated() {
        // If session was marked as expired, always return false
        if (this.sessionExpired) {
            return false;
        }
        
        return this.hasValidTokens();
    },
    
    /**
     * Async version of isAuthenticated that ensures tokens are valid
     * Use this when you need to be certain the session is valid
     */
    async ensureAuthenticated() {
        if (this.sessionExpired) {
            return false;
        }
        
        // Wait for initialization if still in progress
        if (this.initPromise) {
            await this.initPromise;
        }
        
        // If still not initialized and session is expired, return false
        if (this.sessionExpired) {
            return false;
        }
        
        // Check if we have valid tokens
        if (!this.hasValidTokens()) {
            return false;
        }
        
        // If access token is expired, try to refresh
        const expiresAt = parseInt(localStorage.getItem('tbd_token_expires') || '0');
        if (expiresAt < Date.now()) {
            const refreshed = await this.refresh();
            return refreshed;
        }
        
        return true;
    },
    
    /**
     * Get a valid access token, refreshing if needed
     */
    async getValidToken() {
        if (this.sessionExpired) {
            return null;
        }
        
        const expiresAt = parseInt(localStorage.getItem('tbd_token_expires') || '0');
        
        // If token expires in less than 1 minute, refresh it
        if (expiresAt - Date.now() < 60000) {
            const success = await this.refresh();
            if (!success) {
                return null;
            }
        }
        
        return localStorage.getItem('tbd_token');
    },
    
    /**
     * Make an authenticated API call with automatic token refresh
     */
    async apiCall(url, options = {}) {
        // Don't make calls if session is expired
        if (this.sessionExpired) {
            console.log('🚫 Session expired, blocking API call');
            return null;
        }
        
        let token = await this.getValidToken();
        
        if (!token) {
            this.markSessionExpired();
            return null;
        }
        
        try {
            const response = await fetch(url, {
                ...options,
                headers: {
                    ...options.headers,
                    'Authorization': `Bearer ${token}`
                }
            });
            
            // If 401, try to refresh and retry once
            if (response.status === 401) {
                console.log('⚠️ Got 401, attempting token refresh...');
                const refreshed = await this.refresh();
                
                if (!refreshed) {
                    // Session is expired
                    return null;
                }
                
                token = localStorage.getItem('tbd_token');
                if (!token) {
                    return null;
                }
                
                // Retry with new token
                return fetch(url, {
                    ...options,
                    headers: {
                        ...options.headers,
                        'Authorization': `Bearer ${token}`
                    }
                });
            }
            
            return response;
        } catch (error) {
            console.error('API call error:', error);
            throw error;
        }
    }
};

// Listen for storage changes from other tabs
window.addEventListener('storage', (event) => {
    if (event.key === 'tbd_token' && event.newValue === null) {
        // Token was cleared in another tab - user logged out
        console.log('🔄 Detected logout in another tab');
        TokenManager.sessionExpired = true;
        clearTimeout(TokenManager.refreshTimer);
        clearInterval(TokenManager.activityTimer);
        // Don't show modal - just redirect since user intentionally logged out
        window.location.href = 'signin.html';
    } else if (event.key === 'tbd_token' && event.newValue && !TokenManager.sessionExpired) {
        // Token was refreshed in another tab - update our state
        console.log('🔄 Token updated in another tab, syncing...');
        // Re-schedule refresh based on new token
        TokenManager.scheduleRefresh();
    } else if (event.key === 'tbd_refresh_token' && event.newValue === null && !TokenManager.sessionExpired) {
        // Refresh token was revoked - session expired
        console.log('🔄 Refresh token revoked in another tab');
        TokenManager.markSessionExpired();
    }
});

// Handle page visibility changes (tab switching, sleep/wake)
document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible' && !TokenManager.sessionExpired) {
        // Tab became visible - check if tokens are still valid
        console.log('👁️ Tab became visible, checking token validity...');
        const expiresAt = parseInt(localStorage.getItem('tbd_token_expires') || '0');
        const refreshToken = localStorage.getItem('tbd_refresh_token');
        
        if (!refreshToken) {
            // No refresh token anymore - another tab may have logged out
            TokenManager.markSessionExpired();
        } else if (expiresAt < Date.now()) {
            // Access token expired while tab was hidden - refresh now
            TokenManager.refresh();
        } else {
            // Token still valid - reschedule refresh
            TokenManager.scheduleRefresh();
        }
    }
});

// Initialize when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => TokenManager.init());
} else {
    TokenManager.init();
}

// Make globally available
window.TokenManager = TokenManager;
