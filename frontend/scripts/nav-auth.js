// Navigation Authentication Handler
(function() {
    const token = localStorage.getItem('tbd_token');
    const username = localStorage.getItem('tbd_username');
    
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
        if (navUsername && username) {
            navUsername.textContent = username;
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
    
    // Sign out handler - use TokenManager if available (properly revokes refresh tokens)
    if (signOutBtn) {
        signOutBtn.addEventListener('click', function() {
            if (window.TokenManager) {
                TokenManager.logout();
            } else {
                // Fallback for pages without TokenManager
                localStorage.removeItem('tbd_token');
                localStorage.removeItem('tbd_refresh_token');
                localStorage.removeItem('tbd_account_id');
                localStorage.removeItem('tbd_username');
                localStorage.removeItem('tbd_token_expires');
                window.location.href = 'signin.html';
            }
        });
    }
})();

