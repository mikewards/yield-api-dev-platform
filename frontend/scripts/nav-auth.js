// Navigation Authentication Handler
(function() {
    const token = localStorage.getItem('tbd_token');
    const authMode = localStorage.getItem('tbd_auth_mode');
    
    // Get display name based on auth mode
    let displayName = 'Account';
    if (authMode === 'rcac') {
        // RCAC: Use user name or email
        displayName = localStorage.getItem('tbd_user_name') || 
                     localStorage.getItem('tbd_username') || 
                     'Account';
        // If we have a current business, show that instead
        const businessName = localStorage.getItem('tbd_current_business_name');
        if (businessName) {
            displayName = businessName;
        }
    } else {
        // Legacy: Use username
        displayName = localStorage.getItem('tbd_username') || 'Account';
    }
    
    const loggedOutNav = document.getElementById('navAuthLoggedOut');
    const loggedInNav = document.getElementById('navAuthLoggedIn');
    const navUsername = document.getElementById('navUsername');
    const dropdownBtn = document.getElementById('accountDropdownBtn');
    const dropdown = document.getElementById('accountDropdown');
    const signOutBtn = document.getElementById('navSignOutBtn');
    
    if (token && loggedInNav && loggedOutNav) {
        // User is logged in
        loggedOutNav.style.display = 'none';
        loggedInNav.style.display = 'flex';
        if (navUsername) {
            navUsername.textContent = displayName;
        }
    }
    
    // Dropdown toggle
    if (dropdownBtn && dropdown) {
        dropdownBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            dropdown.classList.toggle('active');
        });
        
        // Close dropdown when clicking outside
        document.addEventListener('click', function(e) {
            if (!dropdown.contains(e.target) && !dropdownBtn.contains(e.target)) {
                dropdown.classList.remove('active');
            }
        });
    }
    
    // Sign out handler
    if (signOutBtn) {
        signOutBtn.addEventListener('click', async function() {
            // Try to logout via API first
            try {
                const token = localStorage.getItem('tbd_token');
                if (token) {
                    await fetch(`${API_BASE_URL || ''}/v1/users/logout`, {
                        method: 'POST',
                        headers: { 'Authorization': `Bearer ${token}` }
                    });
                }
            } catch (e) {
                console.log('Logout API call failed, clearing local storage anyway');
            }
            
            // Clear all auth data
            localStorage.removeItem('tbd_token');
            localStorage.removeItem('tbd_refresh_token');
            localStorage.removeItem('tbd_token_expires');
            localStorage.removeItem('tbd_auth_mode');
            // Legacy
            localStorage.removeItem('tbd_account_id');
            localStorage.removeItem('tbd_username');
            // RCAC
            localStorage.removeItem('tbd_user_id');
            localStorage.removeItem('tbd_user_name');
            localStorage.removeItem('tbd_businesses');
            localStorage.removeItem('tbd_current_business_id');
            localStorage.removeItem('tbd_current_business_name');
            
            window.location.href = 'signin.html';
        });
    }
})();

