// Environment Toggle Utility
// Updates all API URLs in documentation pages based on selected environment

(function() {
    'use strict';
    
    // API URLs for different environments
    // These should match config.js - using Railway URLs
    const API_URLS = window.API_URLS || {
        sandbox: 'https://api-sandbox.ground.com',
        production: 'https://api.ground.com'
    };
    
    // Get current environment from localStorage or default to production
    function getCurrentEnvironment() {
        return localStorage.getItem('api_environment') || 'production';
    }
    
    // Set environment
    function setEnvironment(env) {
        if (API_URLS[env]) {
            localStorage.setItem('api_environment', env);
            updateAllUrls(env);
            window.dispatchEvent(new CustomEvent('apiEnvironmentChanged', { detail: env }));
        }
    }
    
    // Update all API URLs in the page
    function updateAllUrls(env) {
        const apiUrl = API_URLS[env];
        const oldUrls = [
            'https://api.ground.com',
            'https://api.flow.com',
            'https://api-sandbox.ground.com',
            'https://flow-platform-staging.up.railway.app',
            'https://flow-platform-flow-platform-staging.up.railway.app',
            'https://flow-platform-production.up.railway.app'
        ];
        
        // Update all code blocks and pre elements
        document.querySelectorAll('pre code, .code-example-box code, code').forEach(codeEl => {
            let text = codeEl.textContent;
            oldUrls.forEach(oldUrl => {
                if (text.includes(oldUrl)) {
                    text = text.replace(new RegExp(oldUrl.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g'), apiUrl);
                    codeEl.textContent = text;
                }
            });
        });
        
        // Update any text content that contains API URLs
        const walker = document.createTreeWalker(
            document.body,
            NodeFilter.SHOW_TEXT,
            null,
            false
        );
        
        let node;
        while (node = walker.nextNode()) {
            let text = node.textContent;
            oldUrls.forEach(oldUrl => {
                if (text.includes(oldUrl)) {
                    const parent = node.parentElement;
                    if (parent && (parent.tagName === 'CODE' || parent.tagName === 'PRE' || parent.classList.contains('code-example-box'))) {
                        node.textContent = text.replace(new RegExp(oldUrl.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g'), apiUrl);
                    }
                }
            });
        }
    }
    
    // Create toggle UI
    function createToggle() {
        const currentEnv = getCurrentEnvironment();
        
        // Check if toggle already exists
        if (document.getElementById('env-toggle-container')) {
            return;
        }
        
        // Find a good place to insert the toggle (usually near the first code example or at the top)
        const firstCodeExample = document.querySelector('.code-example-box, pre code, .code-header');
        const container = document.createElement('div');
        container.id = 'env-toggle-container';
        container.style.cssText = 'display: flex; justify-content: flex-end; align-items: center; gap: 12px; margin-bottom: 16px; padding: 12px 16px; background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0;';
        
        const label = document.createElement('span');
        label.textContent = 'Environment:';
        label.style.cssText = 'font-size: 14px; color: #64748b; font-weight: 500;';
        
        const toggleSwitch = document.createElement('div');
        toggleSwitch.style.cssText = 'display: flex; background: white; border-radius: 6px; padding: 2px; box-shadow: 0 1px 2px rgba(0,0,0,0.05);';
        
        const prodBtn = document.createElement('button');
        prodBtn.textContent = 'Production';
        prodBtn.dataset.env = 'production';
        prodBtn.className = 'env-toggle-btn';
        prodBtn.style.cssText = 'padding: 6px 16px; border: none; background: transparent; border-radius: 4px; cursor: pointer; font-size: 13px; font-weight: 500; color: #64748b; transition: all 0.2s; font-family: inherit;';
        
        const sandboxBtn = document.createElement('button');
        sandboxBtn.textContent = 'Sandbox';
        sandboxBtn.dataset.env = 'sandbox';
        sandboxBtn.className = 'env-toggle-btn';
        sandboxBtn.style.cssText = 'padding: 6px 16px; border: none; background: transparent; border-radius: 4px; cursor: pointer; font-size: 13px; font-weight: 500; color: #64748b; transition: all 0.2s; font-family: inherit;';
        
        // Set active state
        function setActive(env) {
            [prodBtn, sandboxBtn].forEach(btn => {
                if (btn.dataset.env === env) {
                    btn.style.background = '#0f172a';
                    btn.style.color = 'white';
                    btn.style.fontWeight = '600';
                } else {
                    btn.style.background = 'transparent';
                    btn.style.color = '#64748b';
                    btn.style.fontWeight = '500';
                }
            });
        }
        
        setActive(currentEnv);
        
        // Add click handlers
        prodBtn.addEventListener('click', () => {
            setEnvironment('production');
            setActive('production');
        });
        
        sandboxBtn.addEventListener('click', () => {
            setEnvironment('sandbox');
            setActive('sandbox');
        });
        
        toggleSwitch.appendChild(prodBtn);
        toggleSwitch.appendChild(sandboxBtn);
        
        container.appendChild(label);
        container.appendChild(toggleSwitch);
        
        // Insert before first code example, or at the top of main content
        if (firstCodeExample) {
            const parent = firstCodeExample.closest('.code-example-box, .guide-section, section') || firstCodeExample.parentElement;
            if (parent) {
                parent.insertBefore(container, firstCodeExample.closest('.code-example-box') || firstCodeExample);
            } else {
                document.body.insertBefore(container, document.body.firstChild);
            }
        } else {
            const main = document.querySelector('main, .getting-started-main, .api-content');
            if (main) {
                main.insertBefore(container, main.firstChild);
            } else {
                document.body.insertBefore(container, document.body.firstChild);
            }
        }
    }
    
    // Initialize on DOM ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function() {
            createToggle();
            updateAllUrls(getCurrentEnvironment());
        });
    } else {
        createToggle();
        updateAllUrls(getCurrentEnvironment());
    }
    
    // Map internal env names to user-facing labels
    const envLabels = {
        'production': 'Production',
        'sandbox': 'Sandbox'
    };
    
    // Update button text to show user-facing labels
    function updateButtonLabels() {
        const buttons = document.querySelectorAll('.env-toggle-btn');
        buttons.forEach(btn => {
            const env = btn.dataset.env;
            if (envLabels[env]) {
                btn.textContent = envLabels[env];
            }
        });
    }
    
    // Listen for environment changes
    window.addEventListener('apiEnvironmentChanged', function(event) {
        const env = event.detail;
        const buttons = document.querySelectorAll('.env-toggle-btn');
        buttons.forEach(btn => {
            // Update button text
            const btnEnv = btn.dataset.env;
            if (envLabels[btnEnv]) {
                btn.textContent = envLabels[btnEnv];
            }
            
            if (btn.dataset.env === env) {
                btn.style.background = '#0f172a';
                btn.style.color = 'white';
                btn.style.fontWeight = '600';
            } else {
                btn.style.background = 'transparent';
                btn.style.color = '#64748b';
                btn.style.fontWeight = '500';
            }
        });
    });
    
    // Update labels on initialization
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', updateButtonLabels);
    } else {
        updateButtonLabels();
    }
    
    // Export for use in other scripts
    window.envToggle = {
        setEnvironment: setEnvironment,
        getCurrentEnvironment: getCurrentEnvironment,
        updateAllUrls: updateAllUrls
    };
})();

