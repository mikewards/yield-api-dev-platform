/**
 * Token Manager - Handles OAuth 2.0-style token refresh
 * 
 * Features:
 * - Silent token refresh before expiration
 * - Token rotation for security
 * - Graceful session expiration handling
 * - Activity-based session detection
 */
const TokenManager = {
    refreshTimer: null,
    activityTimer: null,
    isRefreshing: false,
    
    // Configuration
    config: {
        refreshBeforeExpiry: 2 * 60 * 1000,  // Refresh 2 minutes before expiry
        inactivityTimeout: 30 * 60 * 1000,   // 30 minutes of inactivity triggers warning
        checkInterval: 60 * 1000              // Check every minute
    },
    
    /**
     * Initialize the token manager
     */
    init() {
        const token = localStorage.getItem('tbd_token');
        if (token) {
            this.scheduleRefresh();
            this.trackActivity();
            console.log('🔐 TokenManager initialized');
        }
    },
    
    /**
     * Schedule the next token refresh
     */
    scheduleRefresh() {
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
            // Token already expired or about to expire, refresh now
            this.refresh();
        } else {
            this.refreshTimer = setTimeout(() => this.refresh(), timeUntilRefresh);
            console.log(`🕐 Token refresh scheduled in ${Math.round(timeUntilRefresh / 1000 / 60)} minutes`);
        }
    },
    
    /**
     * Refresh the access token using the refresh token
     */
    async refresh() {
        if (this.isRefreshing) {
            console.log('🔄 Refresh already in progress, skipping...');
            return;
        }
        
        const refreshToken = localStorage.getItem('tbd_refresh_token');
        if (!refreshToken) {
            console.log('❌ No refresh token available');
            this.handleSessionExpired();
            return;
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
                
                // Schedule next refresh
                this.scheduleRefresh();
                
                // Dispatch event for any listeners
                window.dispatchEvent(new CustomEvent('tokenRefreshed'));
            } else {
                const errorData = await response.json().catch(() => ({}));
                console.log('❌ Token refresh failed:', errorData);
                this.handleSessionExpired();
            }
        } catch (error) {
            console.error('❌ Token refresh network error:', error);
            // On network error, retry in 30 seconds
            setTimeout(() => {
                this.isRefreshing = false;
                this.refresh();
            }, 30000);
        } finally {
            this.isRefreshing = false;
        }
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
                // Could show warning here, but for now just let token expire naturally
            }
        }, this.config.checkInterval);
    },
    
    /**
     * Handle expired session - show modal instead of abrupt redirect
     */
    handleSessionExpired() {
        clearTimeout(this.refreshTimer);
        clearInterval(this.activityTimer);
        this.showSessionExpiredModal();
    },
    
    /**
     * Show a beautiful session expired modal
     */
    showSessionExpiredModal() {
        // Remove existing modal if present
        const existing = document.getElementById('session-expired-modal');
        if (existing) {
            existing.remove();
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
        
        // Prevent scrolling
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
        if (currentPath !== '/signin.html' && currentPath !== '/signin' && currentPath !== '/account.html' && currentPath !== '/account') {
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
                // Ignore errors - we're logging out anyway
                console.log('⚠️ Could not notify server of logout');
            }
        }
        
        clearTimeout(this.refreshTimer);
        clearInterval(this.activityTimer);
        this.clearTokens();
        window.location.href = 'signin.html';
    },
    
    /**
     * Check if user is authenticated
     */
    isAuthenticated() {
        const token = localStorage.getItem('tbd_token');
        const expiresAt = localStorage.getItem('tbd_token_expires');
        
        if (!token) return false;
        
        // If we have a refresh token, consider authenticated even if access token expired
        // (we can refresh it)
        const refreshToken = localStorage.getItem('tbd_refresh_token');
        if (refreshToken) return true;
        
        // No refresh token, check if access token is still valid
        if (expiresAt && parseInt(expiresAt) < Date.now()) {
            return false;
        }
        
        return true;
    },
    
    /**
     * Get a valid access token, refreshing if needed
     */
    async getValidToken() {
        const expiresAt = parseInt(localStorage.getItem('tbd_token_expires') || '0');
        
        // If token expires in less than 1 minute, refresh it
        if (expiresAt - Date.now() < 60000) {
            await this.refresh();
        }
        
        return localStorage.getItem('tbd_token');
    },
    
    /**
     * Make an authenticated API call with automatic token refresh
     */
    async apiCall(url, options = {}) {
        let token = await this.getValidToken();
        
        if (!token) {
            this.handleSessionExpired();
            return null;
        }
        
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
            await this.refresh();
            
            token = localStorage.getItem('tbd_token');
            if (!token) {
                return response; // Return original 401 response
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
    }
};

// Initialize when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => TokenManager.init());
} else {
    TokenManager.init();
}

// Make globally available
window.TokenManager = TokenManager;

