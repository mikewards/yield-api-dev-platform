// Tooltip functionality for links without content
document.addEventListener('DOMContentLoaded', function() {
    // Create tooltip element
    const tooltip = document.createElement('div');
    tooltip.id = 'tooltip';
    tooltip.style.cssText = `
        position: fixed;
        background: var(--primary, #0F172A);
        color: white;
        padding: 8px 12px;
        border-radius: 6px;
        font-size: 13px;
        font-weight: 500;
        pointer-events: none;
        z-index: 10000;
        opacity: 0;
        transition: opacity 0.2s ease;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        max-width: 200px;
        text-align: center;
    `;
    document.body.appendChild(tooltip);

    // Handle links with data-tooltip attribute
    document.querySelectorAll('a[data-tooltip]').forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const tooltipText = this.getAttribute('data-tooltip');
            
            // Show tooltip
            tooltip.textContent = tooltipText;
            tooltip.style.opacity = '1';
            
            // Position tooltip near the click
            const rect = this.getBoundingClientRect();
            tooltip.style.left = (rect.left + rect.width / 2 - tooltip.offsetWidth / 2) + 'px';
            tooltip.style.top = (rect.top - tooltip.offsetHeight - 8) + 'px';
            
            // Hide tooltip after 2 seconds
            setTimeout(() => {
                tooltip.style.opacity = '0';
            }, 2000);
        });
    });
});

