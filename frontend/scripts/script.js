// Scroll Progress Bar (Mobile)
(function() {
    const scrollProgress = document.getElementById('scrollProgress');
    if (scrollProgress) {
        window.addEventListener('scroll', function() {
            const scrollTop = window.scrollY;
            const docHeight = document.documentElement.scrollHeight - window.innerHeight;
            const scrollPercent = (scrollTop / docHeight) * 100;
            scrollProgress.style.width = scrollPercent + '%';
        }, { passive: true });
    }
})();

// Unified Code Block Copy Function
function copyCode(button, codeId) {
    // Find the code element - either by ID or by finding the pre in the same code block
    let codeElement;
    if (codeId) {
        codeElement = document.getElementById(codeId);
    } else {
        // Find the pre element in the same code-block-unified
        const codeBlock = button.closest('.code-block-unified');
        if (codeBlock) {
            codeElement = codeBlock.querySelector('pre');
        }
    }
    
    if (!codeElement) return;
    
    const code = codeElement.innerText || codeElement.textContent;
    navigator.clipboard.writeText(code).then(() => {
        // Add copied class for animation
        button.classList.add('copied');
        
        // Reset after animation
        setTimeout(() => {
            button.classList.remove('copied');
        }, 2000);
    }).catch(err => {
        console.error('Failed to copy:', err);
        // Fallback for older browsers
        const textarea = document.createElement('textarea');
        textarea.value = code;
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.select();
        try {
            document.execCommand('copy');
            button.classList.add('copied');
            setTimeout(() => button.classList.remove('copied'), 2000);
        } catch (e) {
            console.error('Fallback copy failed:', e);
        }
        document.body.removeChild(textarea);
    });
}

// Simple syntax highlighting for JavaScript/Node.js (safe version)
function highlightJavaScriptSimple(text) {
    let html = text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
    
    // Highlight comments first (so they don't get re-processed)
    html = html.replace(/(\/\/[^\n]*)/g, '<span class="token-comment">$1</span>');
    
    // Highlight strings (single and double quotes)
    html = html.replace(/('(?:[^'\\]|\\.)*')/g, '<span class="token-string">$1</span>');
    html = html.replace(/("(?:[^"\\]|\\.)*")/g, '<span class="token-string">$1</span>');
    
    // Highlight keywords (word boundary safe)
    html = html.replace(/\b(const|let|var|await|async|function|return|if|else|for|while|require|export|default)\b/g, '<span class="token-cmd">$1</span>');
    
    // Highlight numbers
    html = html.replace(/\b(\d+\.?\d*)\b/g, '<span class="token-number">$1</span>');
    
    return html;
}

// Simple syntax highlighting for Python (safe version)
function highlightPythonSimple(text) {
    let html = text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
    
    // Highlight comments first
    html = html.replace(/(#[^\n]*)/g, '<span class="token-comment">$1</span>');
    
    // Highlight strings
    html = html.replace(/('(?:[^'\\]|\\.)*')/g, '<span class="token-string">$1</span>');
    html = html.replace(/("(?:[^"\\]|\\.)*")/g, '<span class="token-string">$1</span>');
    
    // Highlight keywords
    html = html.replace(/\b(import|from|def|class|if|else|elif|for|while|return|async|await|print|True|False|None)\b/g, '<span class="token-cmd">$1</span>');
    
    // Highlight numbers
    html = html.replace(/\b(\d+\.?\d*)\b/g, '<span class="token-number">$1</span>');
    
    return html;
}

// Simple syntax highlighting for Ruby (safe version)
function highlightRubySimple(text) {
    let html = text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
    
    // Highlight comments first
    html = html.replace(/(#[^\n]*)/g, '<span class="token-comment">$1</span>');
    
    // Highlight strings
    html = html.replace(/('(?:[^'\\]|\\.)*')/g, '<span class="token-string">$1</span>');
    html = html.replace(/("(?:[^"\\]|\\.)*")/g, '<span class="token-string">$1</span>');
    
    // Highlight keywords
    html = html.replace(/\b(require|def|class|if|else|elsif|end|return|puts|true|false|nil)\b/g, '<span class="token-cmd">$1</span>');
    
    // Highlight symbols
    html = html.replace(/(\s):(\w+)/g, '$1<span class="token-property">:$2</span>');
    
    // Highlight numbers
    html = html.replace(/\b(\d+\.?\d*)\b/g, '<span class="token-number">$1</span>');
    
    return html;
}

// Keep old functions for compatibility but don't use them
function highlightJavaScript(text) { return highlightJavaScriptSimple(text); }
function highlightPython(text) { return highlightPythonSimple(text); }
function highlightRuby(text) { return highlightRubySimple(text); }

// Syntax highlighting for cURL commands
function highlightCurl(text) {
    // Escape HTML first
    let html = text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
    
    // IMPORTANT: Mark quoted strings FIRST (before adding any spans)
    // This prevents the regex from matching span class attributes later
    html = html.replace(/"([^"]+)"/g, '%%QUOTE_START%%$1%%QUOTE_END%%');
    
    // Highlight curl command
    html = html.replace(/^(curl)\s/gm, '<span class="token-cmd">$1</span> ');
    
    // Highlight URLs (http/https)
    html = html.replace(/(https?:\/\/[^\s\\]+)/g, '<span class="token-url">$1</span>');
    
    // Highlight flags like -X, -H, -d, -u
    html = html.replace(/(\s)(-[A-Za-z]+)(\s)/g, '$1<span class="token-flag">$2</span>$3');
    
    // Now convert the quote placeholders to actual spans
    html = html.replace(/%%QUOTE_START%%([^%]+)%%QUOTE_END%%/g, '<span class="token-string">"$1"</span>');
    
    // Highlight single-quoted strings (JSON body)
    html = html.replace(/'(\{[\s\S]*?\})'/g, function(match, json) {
        return "'" + highlightJsonInline(json) + "'";
    });
    
    return html;
}

// Highlight JSON inline (within cURL body)
function highlightJsonInline(json) {
    let html = json;
    // Keys
    html = html.replace(/"([^"]+)"(\s*:)/g, '<span class="token-property">"$1"</span>$2');
    // String values (after colon)
    html = html.replace(/:(\s*)"([^"]+)"/g, ':$1<span class="token-string-value">"$2"</span>');
    return html;
}

// Syntax highlighting for JSON responses
function highlightJson(text) {
    // Escape HTML first
    let html = text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
    
    // Highlight property keys
    html = html.replace(/"([^"]+)"(\s*:)/g, '<span class="token-property">"$1"</span>$2');
    
    // Highlight string values (after colon, with quotes)
    html = html.replace(/:(\s*)"([^"]*)"/g, ':$1<span class="token-string-value">"$2"</span>');
    
    // Highlight numbers
    html = html.replace(/:(\s*)(\d+\.?\d*)/g, ':$1<span class="token-number">$2</span>');
    
    // Highlight booleans
    html = html.replace(/:(\s*)(true|false)/g, ':$1<span class="token-boolean">$2</span>');
    
    // Highlight null
    html = html.replace(/:(\s*)(null)/g, ':$1<span class="token-null">$2</span>');
    
    return html;
}

// Generate line numbers for code blocks
function addLineNumbers(codeBlock) {
    const content = codeBlock.querySelector('.code-block-content');
    if (!content) return;
    
    // Skip if this is a language-switching code block (handled separately)
    if (content.querySelector('.lang-code')) {
        return;
    }
    
    // Get pre element (single pre for non-language blocks)
    const pre = content.querySelector('pre');
    if (!pre) return;
    
    // Skip if line numbers already exist
    if (pre.previousElementSibling && pre.previousElementSibling.classList.contains('code-line-numbers')) {
        return;
    }
    
    // Count lines
    const codeText = pre.textContent || '';
    const lines = codeText.split('\n');
    const lineCount = lines.length;
    
    // Create line numbers element
    const lineNumbers = document.createElement('div');
    lineNumbers.className = 'code-line-numbers';
    
    for (let i = 1; i <= lineCount; i++) {
        const span = document.createElement('span');
        span.textContent = i;
        lineNumbers.appendChild(span);
    }
    
    // Insert line numbers before pre
    content.insertBefore(lineNumbers, pre);
}

// Auto-initialize code blocks on page load
document.addEventListener('DOMContentLoaded', function() {
    // Process all code blocks with syntax highlighting
    document.querySelectorAll('.code-block-unified').forEach((block, index) => {
        const allPre = block.querySelectorAll('pre');
        const copyBtn = block.querySelector('.code-copy-btn');
        const content = block.querySelector('.code-block-content');
        
        // If this block has language code blocks, ensure flex layout
        if (content && content.querySelector('.lang-code')) {
            content.style.display = 'flex';
            content.style.flexDirection = 'row';
        }
        
        // Process each pre element (for language switching)
        allPre.forEach((pre, preIndex) => {
            const code = pre.querySelector('code');
            const codeElement = code || pre; // Use pre directly if no code element
            
            // Skip if already highlighted (has span tags or data attribute)
            if (codeElement && !codeElement.dataset.highlighted) {
                // Check if code already contains highlighting spans
                const hasInlineHighlighting = codeElement.innerHTML.includes('<span class="token-');
                
                if (!hasInlineHighlighting) {
                    const text = codeElement.textContent || '';
                    
                    // Use data-language attribute first, then fall back to content detection
                    const lang = pre.dataset.language || '';
                    const isBash = lang === 'bash' || lang === 'shell' || text.trim().startsWith('curl') || block.classList.contains('curl');
                    const isJson = lang === 'json' || text.trim().startsWith('{') || text.trim().startsWith('[') || block.classList.contains('json');
                    const isJavaScript = lang === 'javascript' || lang === 'js' || (pre.classList.contains('lang-code') && pre.id && pre.id.includes('nodejs'));
                    const isPython = lang === 'python' || (pre.classList.contains('lang-code') && pre.id && pre.id.includes('python'));
                    const isRuby = lang === 'ruby' || (pre.classList.contains('lang-code') && pre.id && pre.id.includes('ruby'));
                    const isHttp = lang === 'http';
                    
                    if (isBash) {
                        codeElement.innerHTML = highlightCurl(text);
                    } else if (isJson) {
                        codeElement.innerHTML = highlightJson(text);
                    } else if (isJavaScript) {
                        codeElement.innerHTML = highlightJavaScriptSimple(text);
                    } else if (isPython) {
                        codeElement.innerHTML = highlightPythonSimple(text);
                    } else if (isRuby) {
                        codeElement.innerHTML = highlightRubySimple(text);
                    } else if (isHttp) {
                        // Simple HTTP header highlighting
                        codeElement.innerHTML = text.replace(/^([A-Za-z-]+):/gm, '<span class="token-property">$1</span>:')
                            .replace(/Bearer\s+([^\s]+)/g, 'Bearer <span class="token-var">$1</span>');
                    }
                }
                codeElement.dataset.highlighted = 'true';
            }
            
            // Set up copy button
            if (pre && copyBtn && !pre.id) {
                pre.id = 'code-' + index + '-' + preIndex;
                if (preIndex === 0) {
                    copyBtn.setAttribute('onclick', `copyCode(this, '${pre.id}')`);
                }
            }
        });
        
        // Add line numbers (skip for language code blocks - handled by tab switching)
        if (!content || !content.querySelector('.lang-code')) {
            addLineNumbers(block);
        }
    });
    
    // Process response blocks
    document.querySelectorAll('.response-block-unified').forEach((block, index) => {
        const pre = block.querySelector('pre');
        const code = pre ? pre.querySelector('code') : null;
        const content = block.querySelector('.response-content');
        
        // Apply JSON highlighting to response blocks
        if (code && !code.dataset.highlighted) {
            const text = code.textContent || '';
            code.innerHTML = highlightJson(text);
            code.dataset.highlighted = 'true';
        }
        
        // Add line numbers to response blocks
        if (content && pre && !content.querySelector('.code-line-numbers')) {
            const codeText = pre.textContent || '';
            const lines = codeText.split('\n');
            const lineCount = lines.length;
            
            const lineNumbers = document.createElement('div');
            lineNumbers.className = 'code-line-numbers';
            
            for (let i = 1; i <= lineCount; i++) {
                const span = document.createElement('span');
                span.textContent = i;
                lineNumbers.appendChild(span);
            }
            
            content.insertBefore(lineNumbers, pre);
        }
    });
});

// Smooth scrolling for anchor links
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        e.preventDefault();
        const target = document.querySelector(this.getAttribute('href'));
        if (target) {
            target.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            });
        }
    });
});

// Tab switching in integration section
document.addEventListener('DOMContentLoaded', function() {
    const tabs = document.querySelectorAll('.tab[data-lang]');
    const codeBlocks = document.querySelectorAll('.lang-code');
    
    function updateLineNumbers(activePre, container) {
        if (!activePre || !container) return;
        
        const content = container.querySelector('.code-block-content');
        if (!content) return;
        
        // Remove existing line numbers
        const existing = content.querySelector('.code-line-numbers');
        if (existing) {
            existing.remove();
        }
        
        // Count lines for active pre
        const codeText = activePre.textContent || '';
        const lines = codeText.split('\n');
        const lineCount = lines.length;
        
        // Create line numbers
        const lineNumbers = document.createElement('div');
        lineNumbers.className = 'code-line-numbers';
        
        for (let i = 1; i <= lineCount; i++) {
            const span = document.createElement('span');
            span.textContent = i;
            lineNumbers.appendChild(span);
        }
        
        // Insert at the beginning of content (before all pre elements)
        const firstChild = content.firstChild;
        if (firstChild) {
            content.insertBefore(lineNumbers, firstChild);
        } else {
            content.appendChild(lineNumbers);
        }
    }
    
    tabs.forEach(tab => {
        tab.addEventListener('click', function() {
            const lang = this.dataset.lang;
            
            // Update tab active state
            tabs.forEach(t => t.classList.remove('active'));
            this.classList.add('active');
            
            // Show/hide code blocks
            codeBlocks.forEach(block => {
                if (block.id === `lang-code-${lang}`) {
                    block.classList.add('active');
                } else {
                    block.classList.remove('active');
                }
            });
            
            // Update line numbers for active code block
            const codeBlockContainer = tab.closest('.integration-visual')?.querySelector('.code-block-unified');
            const activePre = codeBlockContainer?.querySelector('.lang-code.active');
            if (codeBlockContainer && activePre) {
                updateLineNumbers(activePre, codeBlockContainer);
            }
        });
    });
    
    // Initialize line numbers for default active tab
    const activeTab = document.querySelector('.tab.active[data-lang]');
    if (activeTab) {
        const codeBlockContainer = activeTab.closest('.integration-visual')?.querySelector('.code-block-unified');
        const activePre = codeBlockContainer?.querySelector('.lang-code.active');
        if (codeBlockContainer && activePre) {
            updateLineNumbers(activePre, codeBlockContainer);
        }
    }
});

// Add scroll animation
const observerOptions = {
    threshold: 0.1,
    rootMargin: '0px 0px -50px 0px'
};

const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            entry.target.style.opacity = '1';
            entry.target.style.transform = 'translateY(0)';
        }
    });
}, observerOptions);

// Observe feature cards
document.querySelectorAll('.feature-card').forEach(card => {
    card.style.opacity = '0';
    card.style.transform = 'translateY(20px)';
    card.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
    observer.observe(card);
});

// Navbar scroll effect
let lastScroll = 0;
const navbar = document.querySelector('.navbar');

window.addEventListener('scroll', () => {
    const currentScroll = window.pageYOffset;
    
    if (currentScroll > 100) {
        navbar.style.boxShadow = '0 4px 12px rgba(0, 0, 0, 0.05)';
    } else {
        navbar.style.boxShadow = 'none';
    }
    
    lastScroll = currentScroll;
});

// Mobile menu toggle
document.addEventListener('DOMContentLoaded', function() {
    const mobileMenuToggle = document.getElementById('mobileMenuToggle');
    const navLinks = document.getElementById('navLinks');
    const menuIcon = document.getElementById('menuIcon');
    const closeIcon = document.getElementById('closeIcon');
    const navbar = document.querySelector('.navbar');
    
    function openMenu() {
        navLinks.classList.add('mobile-open');
        document.body.classList.add('menu-open');
        if (menuIcon) menuIcon.style.display = 'none';
        if (closeIcon) closeIcon.style.display = 'block';
    }
    
    function closeMenu() {
        navLinks.classList.remove('mobile-open');
        document.body.classList.remove('menu-open');
        if (menuIcon) menuIcon.style.display = 'block';
        if (closeIcon) closeIcon.style.display = 'none';
    }
    
    if (mobileMenuToggle && navLinks) {
        mobileMenuToggle.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            
            if (navLinks.classList.contains('mobile-open')) {
                closeMenu();
            } else {
                openMenu();
            }
        });
        
        // Close menu when clicking on a link
        navLinks.querySelectorAll('a, button').forEach(link => {
            link.addEventListener('click', function() {
                closeMenu();
            });
        });
        
        // Close menu when clicking outside
        document.addEventListener('click', function(event) {
            if (navLinks.classList.contains('mobile-open')) {
                if (!navbar.contains(event.target)) {
                    closeMenu();
                }
            }
        });
        
        // Close menu on escape key
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && navLinks.classList.contains('mobile-open')) {
                closeMenu();
            }
        });
        
        // Close menu on resize to desktop
        window.addEventListener('resize', function() {
            if (window.innerWidth > 768 && navLinks.classList.contains('mobile-open')) {
                closeMenu();
            }
        });
    }
});
