document.addEventListener('DOMContentLoaded', () => {
    // ── Auth guards ───────────────────────────────────────────────
    const page = (location.pathname.split('/').pop()
        || location.href.split('/').pop().split('?')[0]
        || 'home.html').split('?')[0];

    const APP_PAGES = ['home.html', 'profile.html', 'friends.html',
        'squads.html', 'events.html', 'chat.html'];
    const AUTH_PAGES = ['login.html', 'signup.html'];

    const isLoggedIn = !!(sessionStorage.getItem('squadup_user') &&
        sessionStorage.getItem('squadup_token'));

    if (APP_PAGES.includes(page) && !isLoggedIn) {
        window.location.replace('login.html');
        return;
    }
    if (AUTH_PAGES.includes(page) && isLoggedIn) {
        window.location.replace('home.html');
        return;
    }

    // ── Setup ─────────────────────────────────────────────────────
    setupPasswordToggles();

    // Clear login error as user types
    ['username', 'password'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.addEventListener('input', () => clearError('login-error'));
    });

    // Clear signup error as user types
    ['username', 'email', 'password', 'confirm-password'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.addEventListener('input', () => clearError('signup-error'));
    });

    // Load sidebar avatar + username on all app pages
    loadSidebarUser();
    loadPendingBadge();

    // ── Global Context Socket ─────────────────────────────────────
    if (typeof io !== 'undefined' && isLoggedIn) {
        // Only initialize globally if not heavily managed by individual pages (like chat)
        // Actually, we can use a small global socket just for navigation badges.
        const authUser = JSON.parse(sessionStorage.getItem('squadup_user') || '{}');
        if (authUser.id) {
            const url = typeof API_BASE !== 'undefined' ? API_BASE : 'http://127.0.0.1:5500';
            const globalSocket = io(url, { query: { userId: authUser.id } });
            
            globalSocket.on('friend-update', () => {
                loadPendingBadge();
                if (typeof loadFriends === 'function') loadFriends();
                if (typeof loadPending === 'function') loadPending();
                if (typeof loadPendingRequests === 'function') loadPendingRequests(); // chat.html
            });
        }
    }
});

// ── Sidebar: avatar + username ────────────────────────────────────
function loadSidebarUser() {
    const authUser = JSON.parse(sessionStorage.getItem('squadup_user') || '{}');
    if (!authUser) return;

    const username = authUser.username || '';
    const displayName = authUser.displayName || username;
    const avatarUrl = authUser.avatarUrl ||
        `https://ui-avatars.com/api/?name=${encodeURIComponent(username[0]?.toUpperCase() || '?')}&background=0D8ABC&color=fff`;

    const sidebarImg = document.getElementById('sidebar-avatar-img');
    if (sidebarImg) sidebarImg.src = avatarUrl;

    const usernameEl = document.getElementById('sidebar-username');
    if (usernameEl) usernameEl.textContent = username ? '@' + username : '';
}

// Alias used by profile.js
function loadGlobalAvatar() { loadSidebarUser(); }

// ── Sidebar: pending requests badge ──────────────────────────────
async function loadPendingBadge() {
    const badge = document.getElementById('sidebar-pending-badge');
    if (!badge) return;

    const authUser = JSON.parse(sessionStorage.getItem('squadup_user') || 'null');
    if (!authUser?.id) return;

    try {
        const url = typeof API_BASE !== 'undefined' ? API_BASE : 'http://127.0.0.1:5500';
        const res = await fetch(`${url}/api/chat/friends/pending/${authUser.id}`);
        if (res.ok) {
            const data = await res.json();
            const count = (data.pending || []).filter(p => p && p.username).length;
            badge.textContent = count;
            badge.style.display = count > 0 ? '' : 'none';
        }
    } catch (_) {
        badge.style.display = 'none';
    }
}

// ── Error helpers ─────────────────────────────────────────────────
function showError(elementId, message) {
    const el = document.getElementById(elementId);
    if (!el) return;
    el.textContent = message;
    el.style.display = 'block';
}

function clearError(elementId) {
    const el = document.getElementById(elementId);
    if (!el) return;
    el.textContent = '';
    el.style.display = 'none';
}

// ── Login handler (async) ─────────────────────────────────────────
async function handleLogin(event) {
    event.preventDefault();
    clearError('login-error');

    const submitBtn = event.target.querySelector('[type="submit"]') || event.submitter;
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;

    if (submitBtn) { submitBtn.disabled = true; submitBtn.textContent = 'Logging in…'; }

    const result = await AuthService.login(username, password);

    if (!result.success) {
        showError('login-error', result.error);
        if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = 'Launch'; }
        return;
    }

    window.location.href = 'home.html';
}

// ── Signup handler (async) ────────────────────────────────────────
async function handleSignup(event) {
    event.preventDefault();
    clearError('signup-error');

    const submitBtn = event.target.querySelector('[type="submit"]') || event.submitter;
    const username = document.getElementById('username').value.trim();
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const confirm = document.getElementById('confirm-password').value;

    if (password !== confirm) {
        showError('signup-error', 'Passwords do not match.');
        return;
    }

    if (submitBtn) { submitBtn.disabled = true; submitBtn.textContent = 'Creating account…'; }

    const result = await AuthService.register(username, email, password);

    if (!result.success) {
        showError('signup-error', result.error);
        if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = 'Register'; }
        return;
    }

    // Show brief success, then send to login
    showError('signup-error', '✅ Account created! Redirecting to login…');
    document.getElementById('signup-error').style.color = '#3cff8f';
    setTimeout(() => window.location.href = 'login.html', 1500);
}

// ── Password visibility toggle ────────────────────────────────────
function setupPasswordToggles() {
    document.querySelectorAll('input[type="password"]').forEach(input => {
        const wrapper = document.createElement('div');
        wrapper.style.position = 'relative';
        input.parentNode.insertBefore(wrapper, input);
        wrapper.appendChild(input);
        input.style.paddingRight = '40px';

        const toggleBtn = document.createElement('button');
        toggleBtn.type = 'button';
        toggleBtn.className = 'password-toggle';
        Object.assign(toggleBtn.style, {
            position: 'absolute', right: '12px', top: '50%',
            transform: 'translateY(-50%)', background: 'none',
            border: 'none', cursor: 'pointer',
            color: 'var(--primary-color)', padding: '0',
            display: 'flex', alignItems: 'center',
            justifyContent: 'center', zIndex: '10'
        });

        toggleBtn.innerHTML = `
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
                 stroke="currentColor" stroke-width="2" stroke-linecap="round"
                 stroke-linejoin="round" class="eye-open" style="width:20px;height:20px;">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                <circle cx="12" cy="12" r="3"></circle>
            </svg>
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
                 stroke="currentColor" stroke-width="2" stroke-linecap="round"
                 stroke-linejoin="round" class="eye-closed" style="display:none;width:20px;height:20px;">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                <line x1="1" y1="1" x2="23" y2="23"></line>
            </svg>`;

        toggleBtn.addEventListener('click', () => {
            const open = toggleBtn.querySelector('.eye-open');
            const closed = toggleBtn.querySelector('.eye-closed');
            if (input.type === 'password') {
                input.type = 'text';
                open.style.display = 'none';
                closed.style.display = 'block';
            } else {
                input.type = 'password';
                open.style.display = 'block';
                closed.style.display = 'none';
            }
        });

        wrapper.appendChild(toggleBtn);
    });
}
