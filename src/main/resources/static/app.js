/**
 * RAZORYIELD — EXECUTIVE LUXURY LIGHT ENGINE
 * Capabilities:
 * - 3D Silky Fluid Ribbon & Harmonic Wave Canvas (Pearlescent Apple/Stripe Light aesthetic)
 * - Mouse Spring Displacement & Interactive Wave Ripples
 * - Native Web Audio API Synthesizer (Zero-dependency acoustic haptics)
 * - Real-Time AI Yield Curve & Clearance Simulator with interactive SVG morphing
 * - Universal Command Palette (⌘K / Ctrl+K)
 * - Confetti Celebration Burst Engine
 * - Smooth Number Interpolation Counters
 * - Magnetic 3D Tilt Hover Effects
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
                osc.frequency.setValueAtTime(1200, this.ctx.currentTime);
                osc.frequency.exponentialRampToValueAtTime(800, this.ctx.currentTime + 0.025);
                gain.gain.setValueAtTime(0.012, this.ctx.currentTime);
                gain.gain.exponentialRampToValueAtTime(0.0001, this.ctx.currentTime + 0.025);
                osc.connect(gain);
                gain.connect(this.ctx.destination);
                osc.start();
                osc.stop(this.ctx.currentTime + 0.025);
            } catch (e) {}
        }

        click() {
            if (!this.enabled) return;
            try {
                this.initContext();
                const osc = this.ctx.createOscillator();
                const gain = this.ctx.createGain();
                osc.type = 'triangle';
                osc.frequency.setValueAtTime(520, this.ctx.currentTime);
                osc.frequency.exponentialRampToValueAtTime(260, this.ctx.currentTime + 0.06);
                gain.gain.setValueAtTime(0.035, this.ctx.currentTime);
                gain.gain.exponentialRampToValueAtTime(0.0001, this.ctx.currentTime + 0.06);
                osc.connect(gain);
                gain.connect(this.ctx.destination);
                osc.start();
                osc.stop(this.ctx.currentTime + 0.06);
            } catch (e) {}
        }

        success() {
            if (!this.enabled) return;
            try {
                this.initContext();
                const chords = [523.25, 659.25, 783.99, 1046.50]; // C5 major arpeggio
                chords.forEach((freq, idx) => {
                    const osc = this.ctx.createOscillator();
                    const gain = this.ctx.createGain();
                    osc.type = 'sine';
                    osc.frequency.setValueAtTime(freq, this.ctx.currentTime + idx * 0.05);
                    gain.gain.setValueAtTime(0.04, this.ctx.currentTime + idx * 0.05);
                    gain.gain.exponentialRampToValueAtTime(0.0001, this.ctx.currentTime + idx * 0.05 + 0.35);
                    osc.connect(gain);
                    gain.connect(this.ctx.destination);
                    osc.start(this.ctx.currentTime + idx * 0.05);
                    osc.stop(this.ctx.currentTime + idx * 0.05 + 0.35);
                });
            } catch (e) {}
        }

        radar() {
            if (!this.enabled) return;
            try {
                this.initContext();
                const osc = this.ctx.createOscillator();
                const gain = this.ctx.createGain();
                osc.type = 'sine';
                osc.frequency.setValueAtTime(350, this.ctx.currentTime);
                osc.frequency.linearRampToValueAtTime(950, this.ctx.currentTime + 0.18);
                gain.gain.setValueAtTime(0.03, this.ctx.currentTime);
                gain.gain.exponentialRampToValueAtTime(0.0001, this.ctx.currentTime + 0.22);
                osc.connect(gain);
                gain.connect(this.ctx.destination);
                osc.start();
                osc.stop(this.ctx.currentTime + 0.22);
            } catch (e) {}
        }
    }

    const sfx = new SoundEngine();

    // =========================================================================
    // 2. LUXURY SILKY FLUID RIBBON & MESH CANVAS (Apple/Stripe Light Mode)
    // =========================================================================
    function initAmbientCanvas() {
        const canvas = document.getElementById('ambientCanvas');
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        if (!ctx) return;

        let width = canvas.width = window.innerWidth;
        let height = canvas.height = window.innerHeight;

        const mouse = { x: width * 0.5, y: height * 0.3, targetX: width * 0.5, targetY: height * 0.3, active: false };

        window.addEventListener('resize', () => {
            width = canvas.width = window.innerWidth;
            height = canvas.height = window.innerHeight;
        });

        window.addEventListener('mousemove', (e) => {
            mouse.targetX = e.clientX;
            mouse.targetY = e.clientY;
            mouse.active = true;
        }, { passive: true });

        // Ribbons definition: harmonic undulating smooth curves
        const ribbons = [
            { baseY: height * 0.22, amplitude: 55, speed: 0.008, freq: 0.0016, color: 'rgba(5, 150, 105, 0.06)', width: 3 },
            { baseY: height * 0.35, amplitude: 70, speed: 0.006, freq: 0.0012, color: 'rgba(79, 70, 229, 0.05)', width: 2.5 },
            { baseY: height * 0.52, amplitude: 60, speed: 0.007, freq: 0.0014, color: 'rgba(6, 182, 212, 0.04)', width: 2 },
            { baseY: height * 0.70, amplitude: 85, speed: 0.005, freq: 0.0010, color: 'rgba(16, 185, 129, 0.05)', width: 3 },
            { baseY: height * 0.88, amplitude: 50, speed: 0.009, freq: 0.0018, color: 'rgba(99, 102, 241, 0.04)', width: 2 }
        ];

        // Soft floating ambient glow spheres
        const spheres = [
            { x: width * 0.2, y: height * 0.25, r: 240, vx: 0.3, vy: 0.2, color: 'rgba(16, 185, 129, 0.04)' },
            { x: width * 0.8, y: height * 0.35, r: 300, vx: -0.25, vy: 0.2, color: 'rgba(79, 70, 229, 0.035)' },
            { x: width * 0.5, y: height * 0.75, r: 280, vx: 0.2, vy: -0.25, color: 'rgba(6, 182, 212, 0.035)' }
        ];

        let tick = 0;

        function render() {
            tick++;
            // Smooth mouse interpolation (spring dampening)
            mouse.x += (mouse.targetX - mouse.x) * 0.06;
            mouse.y += (mouse.targetY - mouse.y) * 0.06;

            ctx.clearRect(0, 0, width, height);

            // 1. Draw floating soft aura spheres
            spheres.forEach(s => {
                s.x += s.vx;
                s.y += s.vy;
                if (s.x - s.r < 0 || s.x + s.r > width) s.vx *= -1;
                if (s.y - s.r < 0 || s.y + s.r > height) s.vy *= -1;

                const grad = ctx.createRadialGradient(s.x, s.y, 0, s.x, s.y, s.r);
                grad.addColorStop(0, s.color);
                grad.addColorStop(1, 'rgba(255, 255, 255, 0)');
                ctx.fillStyle = grad;
                ctx.beginPath();
                ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2);
                ctx.fill();
            });

            // 2. Draw silky harmonic ribbons with mouse ripple physics
            ribbons.forEach((r, idx) => {
                ctx.beginPath();
                ctx.lineWidth = r.width;
                ctx.strokeStyle = r.color;

                const step = 20;
                let started = false;

                for (let x = 0; x <= width + step; x += step) {
                    // Base harmonic sine waves
                    const timeOffset = tick * r.speed + idx * 2.0;
                    let wave = Math.sin(x * r.freq + timeOffset) * r.amplitude
                             + Math.cos(x * r.freq * 0.5 + timeOffset * 0.8) * (r.amplitude * 0.5);

                    // Interactive mouse displacement
                    const dx = x - mouse.x;
                    const dy = (r.baseY + wave) - mouse.y;
                    const dist = Math.sqrt(dx * dx + dy * dy);
                    const maxDist = 280;

                    if (dist < maxDist) {
                        const factor = (1 - dist / maxDist);
                        wave += Math.sin(factor * Math.PI) * 45 * (dy > 0 ? 1 : -1);
                    }

                    const y = r.baseY + wave;

                    if (!started) {
                        ctx.moveTo(x, y);
                        started = true;
                    } else {
                        ctx.lineTo(x, y);
                    }
                }
                ctx.stroke();
            });

            requestAnimationFrame(render);
        }

        render();
    }

    // =========================================================================
    // 3. SMOOTH NUMBER INTERPOLATION COUNTER
    // =========================================================================
    function animateCounters() {
        const counters = document.querySelectorAll('.stat-value, [data-counter]');
        counters.forEach(counter => {
            const rawText = counter.textContent.trim();
            const numericMatch = rawText.match(/[\d,.]+/);
            if (!numericMatch) return;

            const targetVal = parseFloat(numericMatch[0].replace(/,/g, ''));
            if (isNaN(targetVal) || targetVal === 0) return;

            const isRupee = rawText.startsWith('₹');
            const hasDecimals = rawText.includes('.');
            const startTime = performance.now();
            const duration = 1400; // ms

            function update(now) {
                const elapsed = now - startTime;
                const progress = Math.min(elapsed / duration, 1);
                // EaseOutExpo
                const ease = progress === 1 ? 1 : 1 - Math.pow(2, -10 * progress);
                const current = targetVal * ease;

                let formatted = hasDecimals
                    ? current.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
                    : Math.round(current).toLocaleString('en-IN');

                counter.textContent = (isRupee ? '₹' : '') + formatted;

                if (progress < 1) {
                    requestAnimationFrame(update);
                }
            }
            requestAnimationFrame(update);
        });
    }

    // =========================================================================
    // 4. INTERACTIVE REAL-TIME CLEARANCE SIMULATOR
    // =========================================================================
    function initYieldSimulator() {
        const slider = document.getElementById('simDiscountSlider') || document.getElementById('yieldDiscountSlider');
        const discountDisplay = document.getElementById('simDiscountVal') || document.getElementById('simDiscountPct');
        const curvePath = document.getElementById('simYieldCurve') || document.getElementById('simCurvePath');
        const curveDot = document.getElementById('simCurveDot');
        const liveNote = document.getElementById('simLiveNote');
        const chips = document.querySelectorAll('.sim-chip');
        const velEl = document.getElementById('simVelocityVal');
        const revEl = document.getElementById('simRevenueVal');
        const marginEl = document.getElementById('simMarginHealthVal') || document.getElementById('simMarginVal');
        const statusEl = document.getElementById('simStatusBadge');

        if (!slider || !discountDisplay) return;

        function recalculate() {
            const discount = parseInt(slider.value, 10);
            discountDisplay.textContent = discount + '%';

            // Highlight matching chip
            chips.forEach(chip => {
                const pct = parseInt(chip.getAttribute('data-pct'), 10);
                chip.classList.toggle('active', pct === discount);
            });

            // Smooth colored fill on slider track
            const fillPct = ((discount - 5) / (50 - 5)) * 100;
            slider.style.background = `linear-gradient(to right, #059669 0%, #059669 ${fillPct}%, #e2e8f0 ${fillPct}%, #e2e8f0 100%)`;

            // Velocity & recovery modeling
            const baseVelocity = 12;
            const velocityMultiplier = Math.pow(1 + (discount / 100), 2.2);
            const predictedVelocity = Math.round(baseVelocity * velocityMultiplier);

            const baseGross = 185000;
            const discountedGross = baseGross * (1 - (discount / 100));
            const recoveredRevenue = Math.round(discountedGross * (predictedVelocity / baseVelocity) * 0.45);

            const baseMargin = 38.0;
            const simulatedMargin = (baseMargin - (discount * 0.82)).toFixed(1);
            const isSafe = parseFloat(simulatedMargin) >= 15.0;

            if (velEl) velEl.textContent = predictedVelocity + ' items/day';
            if (revEl) revEl.textContent = '₹' + recoveredRevenue.toLocaleString('en-IN');
            if (marginEl) {
                if (isSafe) {
                    marginEl.textContent = 'Safe (' + simulatedMargin + '%)';
                    marginEl.style.color = 'var(--color-emerald-dark)';
                    if (statusEl) {
                        statusEl.className = 'badge badge-emerald';
                        statusEl.textContent = 'Safe';
                    }
                } else {
                    marginEl.textContent = 'At Risk (' + simulatedMargin + '%)';
                    marginEl.style.color = 'var(--color-rose-dark)';
                    if (statusEl) {
                        statusEl.className = 'badge badge-rose';
                        statusEl.textContent = 'Below 15%';
                    }
                }
            }

            if (liveNote) {
                liveNote.textContent = isSafe
                    ? `Moving ${predictedVelocity} items/day with ${simulatedMargin}% profit protected.`
                    : `Warning: ${simulatedMargin}% profit is below your 15% safety floor.`;
                liveNote.style.color = isSafe ? 'var(--text-muted)' : 'var(--color-rose-dark)';
            }

            // Morph SVG curve dynamically and move live tracking dot
            const norm = (discount - 5) / 45; // 0 to 1
            const midY = 80 - (norm * 35);
            const endY = 52 - (norm * 35);
            if (curvePath) {
                curvePath.setAttribute('d', `M 15 105 Q 80 ${midY + 16}, 165 ${midY}, 245 ${endY + 10}, 325 ${endY} L 325 125 L 15 125 Z`);
            }
            if (curveDot) {
                const dotX = 15 + norm * 305;
                const dotY = (105 - norm * 76);
                curveDot.setAttribute('cx', dotX);
                curveDot.setAttribute('cy', Math.max(dotY, 26));
            }
        }

        chips.forEach(chip => {
            chip.addEventListener('click', () => {
                const pct = parseInt(chip.getAttribute('data-pct'), 10);
                slider.value = pct;
                recalculate();
                if (window.sfx) window.sfx.tick();
            });
        });

        slider.addEventListener('input', () => {
            recalculate();
            if (window.sfx) window.sfx.tick();
        });

        recalculate();
    }

    // =========================================================================
    // =========================================================================
    // 5. REMOVED UNNECESSARY PALETTE
    // =========================================================================


    // =========================================================================
    // 6. CONFETTI CELEBRATION BURST ENGINE
    // =========================================================================
    function triggerConfetti(originX, originY) {
        const count = 36;
        const colors = ['#059669', '#10b981', '#4f46e5', '#6366f1', '#0891b2', '#f59e0b'];
        for (let i = 0; i < count; i++) {
            const piece = document.createElement('div');
            piece.style.position = 'fixed';
            piece.style.left = originX + 'px';
            piece.style.top = originY + 'px';
            piece.style.width = (Math.random() * 8 + 5) + 'px';
            piece.style.height = (Math.random() * 8 + 5) + 'px';
            piece.style.backgroundColor = colors[Math.floor(Math.random() * colors.length)];
            piece.style.borderRadius = Math.random() > 0.5 ? '50%' : '2px';
            piece.style.pointerEvents = 'none';
            piece.style.zIndex = '999999';

            const angle = Math.random() * Math.PI * 2;
            const velocity = Math.random() * 180 + 120;
            const vx = Math.cos(angle) * velocity;
            const vy = Math.sin(angle) * velocity - 160;

            document.body.appendChild(piece);

            const startTime = performance.now();
            function animatePiece(now) {
                const t = (now - startTime) / 1000;
                const curX = originX + vx * t;
                const curY = originY + vy * t + 0.5 * 500 * t * t;
                const opacity = Math.max(1 - t / 1.2, 0);

                piece.style.transform = `translate(${curX - originX}px, ${curY - originY}px) rotate(${t * 360}deg)`;
                piece.style.opacity = opacity;

                if (t < 1.2) {
                    requestAnimationFrame(animatePiece);
                } else {
                    piece.remove();
                }
            }
            requestAnimationFrame(animatePiece);
        }
    }

    // =========================================================================
    // 7. SFX TOGGLE BUTTON
    // =========================================================================
    function initSfxToggle() {
        const btn = document.getElementById('sfxToggleBtn');
        if (!btn) return;
        const updateUI = () => {
            btn.innerHTML = sfx.enabled
                ? `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"/></svg><span>SFX: ON</span>`
                : `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><line x1="23" y1="9" x2="17" y2="15"/><line x1="17" y1="9" x2="23" y2="15"/></svg><span>SFX: OFF</span>`;
        };
        btn.addEventListener('click', () => {
            sfx.toggle();
            updateUI();
        });
        updateUI();
    }

    // =========================================================================
    // 8. SCAN BUTTON RADAR EFFECT
    // =========================================================================
    function initScanEffects() {
        const form = document.getElementById('orchestrateForm');
        const btn = document.getElementById('scanBtn');
        if (!form || !btn) return;

        form.addEventListener('submit', () => {
            sfx.radar();
            btn.disabled = true;
            btn.innerHTML = `<span class="pulse-dot"></span><span>Agent Scanning Stock...</span>`;
        });
    }

    // =========================================================================
    // 9. MAGNETIC 3D TILT ON HOVER
    // =========================================================================
    function initMagneticTilt() {
        const cards = document.querySelectorAll('.card-glass, .stat-card, .process-step-card');
        cards.forEach(card => {
            card.addEventListener('mousemove', (e) => {
                const rect = card.getBoundingClientRect();
                const x = e.clientX - rect.left;
                const y = e.clientY - rect.top;
                const centerX = rect.width / 2;
                const centerY = rect.height / 2;
                const rotateX = ((y - centerY) / centerY) * -2.5;
                const rotateY = ((x - centerX) / centerX) * 2.5;

                card.style.transform = `perspective(800px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) translateY(-2px)`;
                card.style.transition = 'transform 0.1s ease-out';
            });

            card.addEventListener('mouseleave', () => {
                card.style.transform = 'perspective(800px) rotateX(0deg) rotateY(0deg) translateY(0)';
                card.style.transition = 'transform 0.4s cubic-bezier(0.16, 1, 0.3, 1)';
            });
        });
    }

    // =========================================================================
    // 10. APPROVAL CELEBRATION HOOK
    // =========================================================================
    function initApprovalHooks() {
        document.querySelectorAll('form[action^="/campaigns/"][action$="/approve"]').forEach(form => {
            form.addEventListener('submit', (e) => {
                const btn = form.querySelector('button[type="submit"]');
                if (btn) {
                    const rect = btn.getBoundingClientRect();
                    triggerConfetti(rect.left + rect.width / 2, rect.top);
                    sfx.success();
                }
            });
        });
    }

    // =========================================================================
    // 11. INITIALIZATION SEQUENCE
    // =========================================================================
    document.addEventListener('DOMContentLoaded', () => {
        initAmbientCanvas();
        animateCounters();
        initYieldSimulator();
        initSfxToggle();
        initScanEffects();
        initMagneticTilt();
        initApprovalHooks();

        // Audio ticks on interactive elements
        document.querySelectorAll('.btn, .nav-link, .stat-card, .proposal-card, .filter-tab, .process-step-card').forEach(el => {
            el.addEventListener('mouseenter', () => sfx.tick());
        });
    });

    // Expose helpers globally
    window.sfx = sfx;
    window.triggerConfetti = triggerConfetti;

})();
