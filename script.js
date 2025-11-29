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
const tabs = document.querySelectorAll('.tab');
tabs.forEach(tab => {
    tab.addEventListener('click', function() {
        tabs.forEach(t => t.classList.remove('active'));
        this.classList.add('active');
    });
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
    
    if (mobileMenuToggle && navLinks) {
        mobileMenuToggle.addEventListener('click', function() {
            navLinks.classList.toggle('mobile-open');
            if (navLinks.classList.contains('mobile-open')) {
                if (menuIcon) menuIcon.style.display = 'none';
                if (closeIcon) closeIcon.style.display = 'block';
            } else {
                if (menuIcon) menuIcon.style.display = 'block';
                if (closeIcon) closeIcon.style.display = 'none';
            }
        });
        
        // Close menu when clicking on a link
        navLinks.querySelectorAll('a').forEach(link => {
            link.addEventListener('click', function() {
                navLinks.classList.remove('mobile-open');
                if (menuIcon) menuIcon.style.display = 'block';
                if (closeIcon) closeIcon.style.display = 'none';
            });
        });
        
        // Close menu when clicking outside
        document.addEventListener('click', function(event) {
            if (!navbar.contains(event.target) && navLinks.classList.contains('mobile-open')) {
                navLinks.classList.remove('mobile-open');
                if (menuIcon) menuIcon.style.display = 'block';
                if (closeIcon) closeIcon.style.display = 'none';
            }
        });
    }
});
