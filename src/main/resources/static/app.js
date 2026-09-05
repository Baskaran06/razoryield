/**
 * RAZORYIELD — SENIOR FRONTEND ORCHESTRATION ENGINE
 * Core Capabilities:
 * - 3D Ambient WebGL/Canvas Particle Constellation with physics-based cursor repulsion
 * - Native Web Audio API Sound Synthesizer (Zero-dependency audio haptics)
 * - Dynamic Card Spotlight Border Tracker (Linear/Vercel aesthetic)
 * - Universal Command Palette (⌘K / Ctrl+K)
 * - Real-Time AI Yield Curve & Clearance Simulator with interactive SVG morphing
 * - Particle Burst / Confetti Celebration Engine
 * - Smooth Number Interpolation Counters
 * - Interactive Razorpay Webhook Simulation Pipeline
 */

(() => {
    'use strict';

    // =========================================================================
    // 1. NATIVE WEB AUDIO API SYNTHESIZER (AUDIO HAPTICS)
    // =========================================================================
    class SoundEngine {
        constructor() {
            this.ctx = null;
            this.enabled = localStorage.getItem('razoryield_sfx') !== 'disabled';
        }

        initContext() {
            if (!this.ctx && typeof AudioContext !== 'undefined') {
                const AudioCtx = window.AudioContext || window.webkitAudioContext;
                this.ctx = new AudioCtx();
            }
            if (this.ctx && this.ctx.state === 'suspended') {
                this.ctx.resume();
            }
        }

        toggle() {
            this.enabled = !this.enabled;
            localStorage.setItem('razoryield_sfx', this.enabled ? 'enabled' : 'disabled');
            if (this.enabled) {
                this.initContext();
                this.click();
            }
            return this.enabled;
        }

        tick() {
            if (!this.enabled) return;
            try {
                this.initContext();
                const osc = this.ctx.createOscillator();
                const gain = this.ctx.createGain();
                osc.type = 'sine';
                osc.frequency.setValueAtTime(1400, this.ctx.currentTime);
                osc.frequency.exponentialRampToValueAtTime(800, this.ctx.currentTime + 0.03);
                gain.gain.setValueAtTime(0.015, this.ctx.currentTime);
                gain.gain.exponentialRampToValueAtTime(0.0001, this.ctx.currentTime + 0.03);
                osc.connect(gain);
                gain.connect(this.ctx.destination);
                osc.start();
                osc.stop(this.ctx.currentTime + 0.03);
            } catch (e) {}
        }

        click() {
            if (!this.enabled) return;
            try {
                this.initContext();
                const osc = this.ctx.createOscillator();
                const gain = this.ctx.createGain();
                osc.type = 'triangle';
                osc.frequency.setValueAtTime(900, this.ctx.currentTime);
                osc.frequency.exponentialRampToValueAtTime(450, this.ctx.currentTime + 0.06);
                gain.gain.setValueAtTime(0.04, this.ctx.currentTime);
                gain.gain.exponentialRampToValueAtTime(0.0001, this.ctx.currentTime + 0.06);
                osc.connect(gain);
                gain.connect(this.ctx.destination);
                osc.start();
                osc.stop(this.ctx.currentTime + 0.06);
            } catch (e) {}
        }

        radar() {
            if (!this.enabled) return;
            try {
                this.initContext();
                const now = this.ctx.currentTime;
                const osc = this.ctx.createOscillator();
                const gain = this.ctx.createGain();
                osc.type = 'sawtooth';
                osc.frequency.setValueAtTime(200, now);
                osc.frequency.exponentialRampToValueAtTime(1800, now + 0.4);
                gain.gain.setValueAtTime(0.05, now);
                gain.gain.exponentialRampToValueAtTime(0.001, now + 0.45);
                osc.connect(gain);
                gain.connect(this.ctx.destination);
                osc.start(now);
                osc.stop(now + 0.45);
            } catch (e) {}
        }

        success() {
            if (!this.enabled) return;
            try {
                this.initContext();
                const now = this.ctx.currentTime;
                const notes = [523.25, 659.25, 783.99, 1046.50]; // C Major Chord
                notes.forEach((freq, idx) => {
                    const osc = this.ctx.createOscillator();
                    const gain = this.ctx.createGain();
                    osc.type = 'sine';
                    osc.frequency.value = freq;
                    const startTime = now + idx * 0.06;
                    gain.gain.setValueAtTime(0.035, startTime);
                    gain.gain.exponentialRampToValueAtTime(0.0001, startTime + 0.25);
                    osc.connect(gain);
                    gain.connect(this.ctx.destination);
                    osc.start(startTime);
                    osc.stop(startTime + 0.25);
                });
            } catch (e) {}
        }

        warn() {
            if (!this.enabled) return;
            try {
                this.initContext();
                const now = this.ctx.currentTime;
                const osc = this.ctx.createOscillator();
                const gain = this.ctx.createGain();
                osc.type = 'square';
                osc.frequency.setValueAtTime(320, now);
                osc.frequency.setValueAtTime(240, now + 0.08);
                gain.gain.setValueAtTime(0.03, now);
                gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.2);
                osc.connect(gain);
                gain.connect(this.ctx.destination);
                osc.start(now);
                osc.stop(now + 0.2);
            } catch (e) {}
        }
    }

    const sfx = new SoundEngine();
    window.sfx = sfx;

    // =========================================================================
    // 2. 3D AMBIENT SPATIAL PARTICLE CONSTELLATION (HTML5 CANVAS)
    // =========================================================================
    function initAmbientCanvas() {
        const canvas = document.getElementById('ambientCanvas');
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        if (!ctx) return;

        let width = canvas.width = window.innerWidth;
        let height = canvas.height = window.innerHeight;
        let particles = [];
        const particleCount = Math.min(Math.floor((width * height) / 18000), 75);

        const mouse = { x: -1000, y: -1000, radius: 140 };

        window.addEventListener('resize', () => {
            width = canvas.width = window.innerWidth;
            height = canvas.height = window.innerHeight;
        });

        window.addEventListener('mousemove', (e) => {
            mouse.x = e.clientX;
            mouse.y = e.clientY;
        });

        class Particle {
            constructor() {
                this.reset();
            }

            reset() {
                this.x = Math.random() * width;
                this.y = Math.random() * height;
                this.z = Math.random() * 0.8 + 0.2; // 3D depth scale
                this.vx = (Math.random() - 0.5) * 0.45 * this.z;
                this.vy = (Math.random() - 0.5) * 0.45 * this.z;
                this.radius = (Math.random() * 1.8 + 0.8) * this.z;
                this.baseAlpha = Math.random() * 0.35 + 0.15;
                this.alpha = this.baseAlpha;
                this.color = Math.random() > 0.65 ? '#10b981' : (Math.random() > 0.4 ? '#6366f1' : '#38bdf8');
            }

            update() {
                this.x += this.vx;
                this.y += this.vy;

                if (this.x < 0) this.x = width;
                if (this.x > width) this.x = 0;
                if (this.y < 0) this.y = height;
                if (this.y > height) this.y = 0;

                // Mouse interaction physics
                const dx = mouse.x - this.x;
                const dy = mouse.y - this.y;
                const dist = Math.sqrt(dx * dx + dy * dy);
                if (dist < mouse.radius) {
                    const force = (1 - dist / mouse.radius) * 1.5;
                    this.x -= (dx / dist) * force;
                    this.y -= (dy / dist) * force;
                    this.alpha = Math.min(this.baseAlpha * 2.2, 0.9);
                } else {
                    this.alpha += (this.baseAlpha - this.alpha) * 0.05;
                }
            }

            draw() {
                ctx.beginPath();
                ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
                ctx.fillStyle = this.color;
                ctx.globalAlpha = this.alpha;
                ctx.fill();
            }
        }

        for (let i = 0; i < particleCount; i++) {
            particles.push(new Particle());
        }

        function render() {
            ctx.clearRect(0, 0, width, height);

            // Connect nearby particles with subtle glowing neural lines
            for (let i = 0; i < particles.length; i++) {
                particles[i].update();
                particles[i].draw();

                for (let j = i + 1; j < particles.length; j++) {
                    const dx = particles[i].x - particles[j].x;
                    const dy = particles[i].y - particles[j].y;
                    const dist = Math.sqrt(dx * dx + dy * dy);

                    if (dist < 110) {
                        ctx.beginPath();
                        ctx.moveTo(particles[i].x, particles[i].y);
                        ctx.lineTo(particles[j].x, particles[j].y);
                        ctx.strokeStyle = '#6366f1';
                        ctx.globalAlpha = (1 - dist / 110) * 0.15 * particles[i].z;
                        ctx.lineWidth = 0.75 * particles[i].z;
                        ctx.stroke();
                    }
                }
            }
            requestAnimationFrame(render);
        }

        render();
    }

    // =========================================================================
    // 3. CARD SPOTLIGHT MOUSE-TRACKER (LINEAR / VERCEL RADIAL GLOW)
    // =========================================================================
    function initCardSpotlights() {
        const cards = document.querySelectorAll('.card-glass, .stat-card, .proposal-card, .interactive-card');
        window.addEventListener('mousemove', (e) => {
            cards.forEach(card => {
                const rect = card.getBoundingClientRect();
                const x = e.clientX - rect.left;
                const y = e.clientY - rect.top;
                card.style.setProperty('--mouse-x', `${x}px`);
                card.style.setProperty('--mouse-y', `${y}px`);
            });
        });
    }

    // =========================================================================
    // 4. CONFETTI CELEBRATION BURST ENGINE
    // =========================================================================
    function triggerConfetti(originX, originY) {
        const canvas = document.createElement('canvas');
        canvas.style.position = 'fixed';
        canvas.style.top = '0';
        canvas.style.left = '0';
        canvas.style.width = '100vw';
        canvas.style.height = '100vh';
        canvas.style.pointerEvents = 'none';
        canvas.style.zIndex = '9999';
        document.body.appendChild(canvas);

        const ctx = canvas.getContext('2d');
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;

        const colors = ['#10b981', '#6366f1', '#38bdf8', '#f59e0b', '#ec4899', '#ffffff'];
        const particles = [];
        const count = 70;
        const startX = originX || canvas.width / 2;
        const startY = originY || canvas.height / 2;

        for (let i = 0; i < count; i++) {
            const angle = Math.random() * Math.PI * 2;
            const speed = Math.random() * 8 + 3;
            particles.push({
                x: startX,
                y: startY,
                vx: Math.cos(angle) * speed,
                vy: Math.sin(angle) * speed - 3,
                gravity: 0.25,
                color: colors[Math.floor(Math.random() * colors.length)],
                size: Math.random() * 6 + 3,
                rotation: Math.random() * 360,
                vRot: (Math.random() - 0.5) * 12,
                opacity: 1
            });
        }

        let animationFrame;
        function animate() {
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            let alive = 0;

            particles.forEach(p => {
                p.x += p.vx;
                p.y += p.vy;
                p.vy += p.gravity;
                p.rotation += p.vRot;
                p.opacity -= 0.015;

                if (p.opacity > 0) {
                    alive++;
                    ctx.save();
                    ctx.translate(p.x, p.y);
                    ctx.rotate((p.rotation * Math.PI) / 180);
                    ctx.globalAlpha = Math.max(0, p.opacity);
                    ctx.fillStyle = p.color;
                    ctx.fillRect(-p.size / 2, -p.size / 2, p.size, p.size);
                    ctx.restore();
                }
            });

            if (alive > 0) {
                animationFrame = requestAnimationFrame(animate);
            } else {
                cancelAnimationFrame(animationFrame);
                canvas.remove();
            }
        }
        animate();
    }
    window.triggerConfetti = triggerConfetti;

    // =========================================================================
    // 5. NUMBER COUNTER ANIMATION INTERPOLATION
    // =========================================================================
    function animateCounters() {
        const counters = document.querySelectorAll('.animate-number');
        counters.forEach(counter => {
            const target = parseFloat(counter.getAttribute('data-target') || '0');
            const prefix = counter.getAttribute('data-prefix') || '';
            const suffix = counter.getAttribute('data-suffix') || '';
            const decimals = parseInt(counter.getAttribute('data-decimals') || '0', 10);
            const duration = 1200;
            const startTime = performance.now();

            function update(currentTime) {
                const elapsed = currentTime - startTime;
                const progress = Math.min(elapsed / duration, 1);
                // Ease out expo
                const ease = progress === 1 ? 1 : 1 - Math.pow(2, -10 * progress);
                const currentVal = target * ease;

                const formatted = currentVal.toLocaleString('en-IN', {
                    minimumFractionDigits: decimals,
                    maximumFractionDigits: decimals
                });

                counter.textContent = `${prefix}${formatted}${suffix}`;

                if (progress < 1) {
                    requestAnimationFrame(update);
                }
            }
            requestAnimationFrame(update);
        });
    }

    // =========================================================================
    // 6. REAL-TIME AI YIELD & CLEARANCE SIMULATOR WIDGET
    // =========================================================================
    function initYieldSimulator() {
        const slider = document.getElementById('simDiscountSlider');
        if (!slider) return;

        const discountVal = document.getElementById('simDiscountVal');
        const velocityVal = document.getElementById('simVelocityVal');
        const revenueVal = document.getElementById('simRevenueVal');
        const marginHealthVal = document.getElementById('simMarginHealthVal');
        const pathCurve = document.getElementById('simYieldCurve');

        function recalculate() {
            const disc = parseInt(slider.value, 10);
            if (discountVal) discountVal.textContent = `${disc}%`;

            // Yield math models
            const baseInventoryValue = 185000; // Simulated idle inventory in INR
            const elasticityFactor = 1 + (disc / 100) * 2.2;
            const clearanceRate = Math.min(Math.round(24 * elasticityFactor), 96);
            const estRevenue = Math.round(baseInventoryValue * (1 - disc / 100) * (clearanceRate / 100));

            // Margin floor health check (Breach happens if discount > 35%)
            let marginStatus = "Optimal (>22% ROI)";
            let marginClass = "text-emerald";
            if (disc > 25 && disc <= 35) {
                marginStatus = "Guarded (15-20% Floor)";
                marginClass = "text-amber";
            } else if (disc > 35) {
                marginStatus = "Floor Breached (HITL Block)";
                marginClass = "text-rose";
            }

            if (velocityVal) velocityVal.textContent = `${clearanceRate}% in 48h`;
            if (revenueVal) revenueVal.textContent = `₹${estRevenue.toLocaleString('en-IN')}`;
            if (marginHealthVal) {
                marginHealthVal.textContent = marginStatus;
                marginHealthVal.className = `number-font ${marginClass}`;
            }

            // Morph SVG curve
            if (pathCurve) {
                const heightOffset = (disc / 50) * 45;
                const d = `M 10 90 Q 70 ${80 - heightOffset * 0.7}, 140 ${70 - heightOffset}, 210 ${60 - heightOffset * 1.1}, 280 ${50 - heightOffset * 1.2} L 280 110 L 10 110 Z`;
                pathCurve.setAttribute('d', d);
            }
        }

        slider.addEventListener('input', () => {
            sfx.tick();
            recalculate();
        });

        recalculate();
    }

    // =========================================================================
    // 7. UNIVERSAL COMMAND PALETTE (⌘K / CTRL+K)
    // =========================================================================
    function initCommandPalette() {
        const palette = document.getElementById('commandPalette');
        const searchInput = document.getElementById('commandInput');
        const closeBtn = document.getElementById('commandCloseBtn');
        const triggerBtns = document.querySelectorAll('.command-palette-trigger');

        if (!palette || !searchInput) return;

        function openPalette() {
            palette.classList.add('active');
            searchInput.value = '';
            filterCommands('');
            searchInput.focus();
            sfx.click();
        }

        function closePalette() {
            palette.classList.remove('active');
            sfx.tick();
        }

        triggerBtns.forEach(btn => btn.addEventListener('click', openPalette));
        if (closeBtn) closeBtn.addEventListener('click', closePalette);

        window.addEventListener('keydown', (e) => {
            if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
                e.preventDefault();
                if (palette.classList.contains('active')) {
                    closePalette();
                } else {
                    openPalette();
                }
            } else if (e.key === 'Escape' && palette.classList.contains('active')) {
                closePalette();
            }
        });

        palette.addEventListener('click', (e) => {
            if (e.target === palette) closePalette();
        });

        function filterCommands(query) {
            const items = palette.querySelectorAll('.command-item');
            const q = query.toLowerCase().trim();
            items.forEach(item => {
                const text = item.textContent.toLowerCase();
                item.style.display = text.includes(q) ? 'flex' : 'none';
            });
        }

        searchInput.addEventListener('input', (e) => {
            filterCommands(e.target.value);
        });

        palette.querySelectorAll('.command-item').forEach(item => {
            item.addEventListener('click', () => {
                const action = item.getAttribute('data-action');
                closePalette();
                executeAction(action);
            });
        });

        function executeAction(action) {
            sfx.click();
            switch (action) {
                case 'scan':
                    const scanBtn = document.getElementById('scanBtn');
                    if (scanBtn) scanBtn.click();
                    break;
                case 'dashboard':
                    window.location.href = '/';
                    break;
                case 'campaigns':
                    window.location.href = '/campaigns';
                    break;
                case 'inventory':
                    window.location.href = '/inventory';
                    break;
                case 'payouts':
                    window.location.href = '/payments';
                    break;
                case 'activity':
                    window.location.href = '/activity';
                    break;
                case 'settings':
                    window.location.href = '/settings';
                    break;
                case 'toggle-sfx':
                    const sfxBtn = document.getElementById('sfxToggleBtn');
                    if (sfxBtn) sfxBtn.click();
                    break;
                case 'simulate-webhook':
                    window.location.href = '/payments#webhookSimulator';
                    break;
            }
        }
    }

    // =========================================================================
    // 8. AUDIO SFX TOGGLE BUTTON
    // =========================================================================
    function initSfxToggle() {
        const btn = document.getElementById('sfxToggleBtn');
        if (!btn) return;

        function updateUI() {
            const state = sfx.enabled;
            btn.innerHTML = state 
                ? `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"/></svg><span>SFX: ON</span>`
                : `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><line x1="23" y1="9" x2="17" y2="15"/><line x1="17" y1="9" x2="23" y2="15"/></svg><span>SFX: MUTED</span>`;
            btn.className = `btn btn-sm ${state ? 'btn-emerald-subtle' : 'btn-outline'}`;
        }

        btn.addEventListener('click', () => {
            sfx.toggle();
            updateUI();
        });

        updateUI();
    }

    // =========================================================================
    // 9. SCAN BUTTON RADAR EFFECT
    // =========================================================================
    function initScanEffects() {
        const form = document.getElementById('orchestrateForm');
        const btn = document.getElementById('scanBtn');
        if (!form || !btn) return;

        form.addEventListener('submit', () => {
            sfx.radar();
            btn.classList.add('scanning-active');
            btn.innerHTML = `
                <span class="spinner-dot"></span>
                <span>Agent Scanning Idle Stock...</span>
            `;
        });
    }

    // =========================================================================
    // 10. ANIMMASTER-QUALITY MOTION ENGINE (GSAP-equivalent, zero-dependency)
    // =========================================================================

    // --- 10a. Spring Physics Easing Function ---
    function springEase(t, damping = 0.7, frequency = 4) {
        return 1 - Math.exp(-damping * t * 10) * Math.cos(frequency * t * Math.PI * 2);
    }

    // --- 10b. IntersectionObserver Scroll Reveal System ---
    function initScrollRevealSystem() {
        const revealElements = document.querySelectorAll(
            '.anim-reveal, .card-glass, .stat-card, .page-header, .hero-grid, .filter-tabs, .timeline-node, .form-group'
        );
        if (!revealElements.length) return;

        // Pre-set all elements to hidden state via inline styles (no CSS class needed)
        revealElements.forEach((el, i) => {
            el.style.opacity = '0';
            el.style.transform = 'translateY(32px) scale(0.97)';
            el.style.transition = 'none'; // Remove transition until observed
            el.dataset.revealIndex = i;
        });

        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (!entry.isIntersecting) return;

                const el = entry.target;
                const idx = parseInt(el.dataset.revealIndex || '0', 10);
                const staggerDelay = Math.min(idx * 65, 400); // Stagger up to 400ms

                setTimeout(() => {
                    el.style.transition = 'opacity 0.7s cubic-bezier(0.16, 1, 0.3, 1), transform 0.7s cubic-bezier(0.16, 1, 0.3, 1)';
                    el.style.opacity = '1';
                    el.style.transform = 'translateY(0) scale(1)';
                    sfx.tick();
                }, staggerDelay);

                observer.unobserve(el);
            });
        }, { threshold: 0.08, rootMargin: '0px 0px -40px 0px' });

        // Small delay to ensure styles are applied before observing
        requestAnimationFrame(() => {
            revealElements.forEach(el => observer.observe(el));
        });
    }

    // --- 10c. Magnetic 3D Tilt Hover Effect (Awwwards-style) ---
    function initMagneticTilt() {
        const cards = document.querySelectorAll('.card-glass, .stat-card');
        cards.forEach(card => {
            card.addEventListener('mousemove', (e) => {
                const rect = card.getBoundingClientRect();
                const x = e.clientX - rect.left;
                const y = e.clientY - rect.top;
                const centerX = rect.width / 2;
                const centerY = rect.height / 2;

                const rotateX = ((y - centerY) / centerY) * -3; // Max ±3 degrees
                const rotateY = ((x - centerX) / centerX) * 3;

                card.style.transform = `perspective(800px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale(1.01)`;
                card.style.transition = 'transform 0.1s ease-out';
            });

            card.addEventListener('mouseleave', () => {
                card.style.transform = 'perspective(800px) rotateX(0deg) rotateY(0deg) scale(1)';
                card.style.transition = 'transform 0.55s cubic-bezier(0.16, 1, 0.3, 1)';
            });
        });
    }

    // --- 10d. Split Text Character Reveal Animation ---
    function initTextSplitAnimation() {
        const titles = document.querySelectorAll('.page-title');
        titles.forEach(title => {
            const text = title.textContent;
            title.textContent = '';
            title.style.opacity = '1';

            [...text].forEach((char, i) => {
                const span = document.createElement('span');
                span.textContent = char === ' ' ? '\u00A0' : char;
                span.style.cssText = `
                    display: inline-block;
                    opacity: 0;
                    transform: translateY(20px) rotateX(-45deg);
                    animation: charReveal 0.5s cubic-bezier(0.16, 1, 0.3, 1) ${i * 25}ms forwards;
                `;
                title.appendChild(span);
            });
        });

        // Inject keyframes if not already present
        if (!document.getElementById('charRevealKeyframes')) {
            const style = document.createElement('style');
            style.id = 'charRevealKeyframes';
            style.textContent = `
                @keyframes charReveal {
                    to {
                        opacity: 1;
                        transform: translateY(0) rotateX(0deg);
                    }
                }
                @keyframes slideInFromRight {
                    from {
                        opacity: 0;
                        transform: translateX(30px);
                    }
                    to {
                        opacity: 1;
                        transform: translateX(0);
                    }
                }
                @keyframes pulseGlow {
                    0%, 100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.15); }
                    50% { box-shadow: 0 0 20px 6px rgba(16, 185, 129, 0.08); }
                }
                @keyframes numberSlotMachine {
                    0% { transform: translateY(-100%); opacity: 0; }
                    60% { transform: translateY(5%); }
                    80% { transform: translateY(-2%); }
                    100% { transform: translateY(0); opacity: 1; }
                }
                @keyframes fadeInScale {
                    from { opacity: 0; transform: scale(0.85); }
                    to { opacity: 1; transform: scale(1); }
                }
                @keyframes shimmer {
                    0% { background-position: -200% center; }
                    100% { background-position: 200% center; }
                }
            `;
            document.head.appendChild(style);
        }
    }

    // --- 10e. Navbar Scroll Shrink Effect ---
    function initNavbarScrollEffect() {
        const navbar = document.querySelector('.navbar-glass');
        if (!navbar) return;

        let lastScroll = 0;
        let ticking = false;

        window.addEventListener('scroll', () => {
            lastScroll = window.scrollY;
            if (!ticking) {
                requestAnimationFrame(() => {
                    if (lastScroll > 60) {
                        navbar.style.padding = '0.45rem 1.75rem';
                        navbar.style.backdropFilter = 'blur(24px) saturate(1.6)';
                        navbar.style.boxShadow = '0 4px 30px rgba(0,0,0,0.4)';
                    } else {
                        navbar.style.padding = '';
                        navbar.style.backdropFilter = '';
                        navbar.style.boxShadow = '';
                    }
                    ticking = false;
                });
                ticking = true;
            }
        }, { passive: true });
    }

    // --- 10f. Smooth Badge Pulse on Stat Values ---
    function initStatValuePulse() {
        const statValues = document.querySelectorAll('.stat-value');
        statValues.forEach(val => {
            val.style.animation = 'fadeInScale 0.6s cubic-bezier(0.16, 1, 0.3, 1) 0.3s both';
        });

        // Add shimmer to brand-tag badges
        const brandTags = document.querySelectorAll('.brand-tag');
        brandTags.forEach(tag => {
            tag.style.background = 'linear-gradient(90deg, var(--color-emerald) 0%, var(--color-cyan) 50%, var(--color-emerald) 100%)';
            tag.style.backgroundSize = '200% auto';
            tag.style.webkitBackgroundClip = 'text';
            tag.style.webkitTextFillColor = 'transparent';
            tag.style.animation = 'shimmer 3s linear infinite';
        });
    }

    // --- 10g. Timeline Node Staggered Reveal ---
    function initTimelineAnimation() {
        const nodes = document.querySelectorAll('.timeline-node');
        if (!nodes.length) return;

        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (!entry.isIntersecting) return;
                const node = entry.target;
                const index = Array.from(nodes).indexOf(node);
                node.style.animation = `slideInFromRight 0.5s cubic-bezier(0.16, 1, 0.3, 1) ${index * 80}ms both`;
                observer.unobserve(node);
            });
        }, { threshold: 0.1 });

        nodes.forEach(node => {
            node.style.opacity = '0';
            observer.observe(node);
        });
    }

    // --- 10h. Table Row Staggered Cascade ---
    function initTableRowAnimation() {
        const rows = document.querySelectorAll('.data-table tbody tr');
        if (!rows.length) return;

        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (!entry.isIntersecting) return;

                const table = entry.target.closest('table');
                if (!table || table.dataset.animated) return;
                table.dataset.animated = 'true';

                const allRows = table.querySelectorAll('tbody tr');
                allRows.forEach((row, i) => {
                    row.style.opacity = '0';
                    row.style.transform = 'translateX(-16px)';
                    setTimeout(() => {
                        row.style.transition = 'opacity 0.4s ease, transform 0.4s cubic-bezier(0.16, 1, 0.3, 1)';
                        row.style.opacity = '1';
                        row.style.transform = 'translateX(0)';
                    }, i * 50);
                });

                observer.unobserve(entry.target);
            });
        }, { threshold: 0.05 });

        // Observe just the first row to trigger the cascade
        if (rows[0]) observer.observe(rows[0]);
    }

    // --- 10i. Page Load Wipe Transition ---
    function initPageTransition() {
        // Create a full-screen wipe overlay
        const wipe = document.createElement('div');
        wipe.style.cssText = `
            position: fixed; inset: 0; z-index: 99999;
            background: linear-gradient(135deg, #05070d 0%, #0a0f1a 100%);
            pointer-events: none;
            transition: opacity 0.45s cubic-bezier(0.16, 1, 0.3, 1),
                        transform 0.45s cubic-bezier(0.16, 1, 0.3, 1);
        `;
        document.body.prepend(wipe);

        requestAnimationFrame(() => {
            requestAnimationFrame(() => {
                wipe.style.opacity = '0';
                wipe.style.transform = 'scaleY(0)';
                wipe.style.transformOrigin = 'top';
                setTimeout(() => wipe.remove(), 500);
            });
        });
    }

    // --- 10j. Cursor Trail Glow (Subtle) ---
    function initCursorGlow() {
        const glow = document.createElement('div');
        glow.style.cssText = `
            position: fixed; pointer-events: none; z-index: 9998;
            width: 300px; height: 300px; border-radius: 50%;
            background: radial-gradient(circle, rgba(16, 185, 129, 0.04) 0%, transparent 70%);
            transform: translate(-50%, -50%);
            transition: left 0.15s ease-out, top 0.15s ease-out;
            will-change: left, top;
        `;
        document.body.appendChild(glow);

        document.addEventListener('mousemove', (e) => {
            glow.style.left = e.clientX + 'px';
            glow.style.top = e.clientY + 'px';
        }, { passive: true });
    }

    // =========================================================================
    // 11. DOM READY — FULL INITIALIZATION SEQUENCE
    // =========================================================================
    document.addEventListener('DOMContentLoaded', () => {
        // Core systems
        initAmbientCanvas();
        initCardSpotlights();
        animateCounters();
        initYieldSimulator();
        initCommandPalette();
        initSfxToggle();
        initScanEffects();

        // Animmaster-quality motion layer
        initPageTransition();
        initTextSplitAnimation();
        initScrollRevealSystem();
        initMagneticTilt();
        initNavbarScrollEffect();
        initStatValuePulse();
        initTimelineAnimation();
        initTableRowAnimation();
        initCursorGlow();

        // Acoustic haptics on interactive elements
        document.querySelectorAll('.btn, .nav-link, .stat-card, .proposal-card, .filter-tab').forEach(el => {
            el.addEventListener('mouseenter', () => sfx.tick());
        });
    });

})();
