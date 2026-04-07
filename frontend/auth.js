/**
 * AuthService — Backend-connected Authentication
 *
 * All user data is stored in MongoDB via the Node/Express API.
 * Session is tracked via sessionStorage (clears on browser close).
 */

const API_BASE = (window.location.hostname === '127.0.0.1' || window.location.hostname === 'localhost')
    ? 'http://127.0.0.1:5500'
    : window.location.origin;

const AuthService = (() => {

    const USER_KEY = 'squadup_user';    // current session user  — sessionStorage
    const TOKEN_KEY = 'squadup_token';   // session token         — sessionStorage

    /**
     * Register a new user.
     * @returns {Promise<{ success: boolean, error: string|null }>}
     */
    async function register(username, email, password) {
        try {
            const res = await fetch(`${API_BASE}/api/auth/register`, {
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
            const res = await fetch(`${API_BASE}/api/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: identifier, password })
            });
            const data = await res.json();
            if (!res.ok) return { success: false, error: data.error || 'Login failed.' };

            // Store full user object and a token in sessionStorage
            sessionStorage.setItem(USER_KEY, JSON.stringify(data.user));
            sessionStorage.setItem(TOKEN_KEY, 'session_' + data.user.id);

            return { success: true, error: null };
        } catch {
            return { success: false, error: 'Cannot reach the server. Is it running?' };
        }
    }

    /** Log out the current user */
    function logout() {
        sessionStorage.removeItem('squadup_banner_dismissed');
        sessionStorage.removeItem(USER_KEY);
        sessionStorage.removeItem(TOKEN_KEY);
    }

    /** Get the currently logged-in user from session, or null */
    function getCurrentUser() {
        return JSON.parse(sessionStorage.getItem(USER_KEY) || 'null');
    }

    /** Update the locally stored session user (e.g. after a profile edit) */
    function updateSessionUser(updatedUser) {
        const current = getCurrentUser();
        if (!current) return;
        // Normalise: ensure 'id' is always present (Mongoose docs use _id)
        const merged = { ...current, ...updatedUser };
        if (!merged.id && merged._id) merged.id = merged._id.toString();
        sessionStorage.setItem(USER_KEY, JSON.stringify(merged));
    }

    return { login, register, logout, getCurrentUser, updateSessionUser };

})();
