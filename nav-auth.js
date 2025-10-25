// Navigation Authentication Handler
(function() {
    const token = localStorage.getItem('flow_token');
    const username = localStorage.getItem('flow_username');
    
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
    
    // Sign out handler
    if (signOutBtn) {
        signOutBtn.addEventListener('click', function() {
            localStorage.removeItem('flow_token');
            localStorage.removeItem('flow_account_id');
            localStorage.removeItem('flow_username');
            localStorage.removeItem('flow_token_expires');
            window.location.href = 'signin.html';
        });
    }
})();

