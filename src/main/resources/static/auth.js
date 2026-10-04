/**
 * AuthService — Backend-connected Authentication
 *
 * All user data is stored by the Spring Boot API.
 * Session is tracked via sessionStorage (clears on browser close): the user object plus the
 * bearer token returned by /api/v1/auth/login, which is sent with every API request.
 */

// The Spring Boot app serves this page, so the API lives on the same origin.
// Opened straight from disk (file://), fall back to the default local server.
const API_BASE = window.location.protocol === 'file:'
    ? 'http://127.0.0.1:5500'
    : window.location.origin;

const AuthService = (() => {

    const USER_KEY = 'squadup_user';    // current session user  — sessionStorage
    const TOKEN_KEY = 'squadup_token';   // bearer token          — sessionStorage

    /**
     * Register a new user.
     * @returns {Promise<{ success: boolean, error: string|null }>}
     */
    async function register(username, email, password) {
        try {
            const res = await fetch(`${API_BASE}/api/v1/auth/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, email, password })
            });
            const data = await res.json();
            if (!res.ok) return { success: false, error: data.error || 'Registration failed.' };
            return { success: true, error: null };
        } catch {
            return { success: false, error: 'Cannot reach the server. Is it running?' };
        }
    }

    /**
     * Log in an existing user.
     * @returns {Promise<{ success: boolean, error: string|null }>}
     */
    async function login(identifier, password) {
        try {
            const res = await fetch(`${API_BASE}/api/v1/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: identifier, password })
            });
            const data = await res.json();
            if (!res.ok) return { success: false, error: data.error || 'Login failed.' };

            // Store full user object and the bearer token in sessionStorage
            sessionStorage.setItem(USER_KEY, JSON.stringify(data.user));
            sessionStorage.setItem(TOKEN_KEY, data.token);

            return { success: true, error: null };
        } catch {
            return { success: false, error: 'Cannot reach the server. Is it running?' };
        }
    }

    /** Log out the current user (the server revokes the token too) */
    function logout() {
        const token = getToken();
        if (token) {
            fetch(`${API_BASE}/api/v1/auth/logout`, {
                method: 'POST',
                headers: { Authorization: 'Bearer ' + token },
                keepalive: true
            }).catch(() => { });
        }
        sessionStorage.removeItem('squadup_banner_dismissed');
        sessionStorage.removeItem(USER_KEY);
        sessionStorage.removeItem(TOKEN_KEY);
    }

    /** Get the currently logged-in user from session, or null */
    function getCurrentUser() {
        return JSON.parse(sessionStorage.getItem(USER_KEY) || 'null');
    }

    /** Bearer token of the current session, or null */
    function getToken() {
        return sessionStorage.getItem(TOKEN_KEY);
    }

    /** Update the locally stored session user (e.g. after a profile edit) */
    function updateSessionUser(updatedUser) {
        const current = getCurrentUser();
        if (!current) return;
        sessionStorage.setItem(USER_KEY, JSON.stringify({ ...current, ...updatedUser }));
    }

    return { login, register, logout, getCurrentUser, getToken, updateSessionUser };

})();

/**
 * Adds "Authorization: Bearer <token>" to every call to our API, so the pages can keep
 * using plain fetch(). If the server says the session is no longer valid (401), the user
 * is sent back to the login page.
 */
(() => {
    const nativeFetch = window.fetch.bind(window);

    window.fetch = async (input, init = {}) => {
        const url = typeof input === 'string' ? input : input.url;
        const isApi = url.startsWith(API_BASE + '/api/') || url.startsWith('/api/');
        const isAuthCall = url.includes('/api/v1/auth/');
        const token = AuthService.getToken();

        if (isApi && token) {
            const headers = new Headers(init.headers || {});
            if (!headers.has('Authorization')) headers.set('Authorization', 'Bearer ' + token);
            init = { ...init, headers };
        }

        const res = await nativeFetch(input, init);

        if (isApi && !isAuthCall && res.status === 401 && token) {
            AuthService.logout();
            window.location.replace('login.html');
        }
        return res;
    };
})();
