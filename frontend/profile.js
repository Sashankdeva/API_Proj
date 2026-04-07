/**
 * profile.js — Profile Page Logic
 * API_BASE is declared globally by auth.js (loaded before this file).
 */

let currentUser = null;
let editMode = false;
let originalData = {};
let gameTags = [];

// Safe ID getter — handles both 'id' (login response) and '_id' (Mongoose doc)
function getUserId() {
    if (!currentUser) return null;
    return currentUser.id || (currentUser._id ? currentUser._id.toString() : null);
}

// ── Init ──────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', async () => {
    currentUser = AuthService.getCurrentUser();
    if (!currentUser) { window.location.replace('login.html'); return; }

    // Bio char counter
    const bioInput = document.getElementById('input-bio');
    if (bioInput) {
        bioInput.addEventListener('input', () => {
            const el = document.getElementById('bioCharCount');
            if (el) el.textContent = bioInput.value.length;
        });
    }

    // Game tag input on Enter
    const tagInput = document.getElementById('input-gameTag');
    if (tagInput) {
        tagInput.addEventListener('keydown', e => {
            if (e.key === 'Enter') { e.preventDefault(); addTag(); }
        });
    }

    await loadProfile();

    // Close platform dropdown when clicking outside
    document.addEventListener('click', (e) => {
        const wrapper = document.getElementById('input-platforms-wrapper');
        if (wrapper && !wrapper.contains(e.target)) {
            const menu = document.getElementById('platformDropdownMenu');
            const btn = document.getElementById('platformDropdownBtn');
            if (menu) { menu.classList.remove('open'); updatePlatformLabel(); }
            if (btn) btn.classList.remove('open');
        }
    });
});

// ── Fetch & Render ────────────────────────────────────────────────
async function loadProfile() {
    const uid = getUserId();
    if (!uid) { renderProfile(currentUser); return; }
    try {
        const res = await fetch(`${API_BASE}/api/users/${uid}`);
        const data = await res.json();
        if (!res.ok) { renderProfile(currentUser); return; }
        AuthService.updateSessionUser(data.user);
        currentUser = AuthService.getCurrentUser();
        renderProfile(data.user);
    } catch {
        renderProfile(currentUser);
    }
}

function renderProfile(user) {
    const u = {
        username: user.username || '',
        displayName: user.displayName || user.username || '',
        email: user.email || '',
        bio: user.bio || '',
        gameTags: Array.isArray(user.gameTags) ? user.gameTags : [],
        avatarUrl: user.avatarUrl || '',
        createdAt: user.createdAt,
        // Personal Info
        firstName: user.firstName || '',
        lastName: user.lastName || '',
        age: user.age != null ? user.age : '',
        gender: user.gender || '',
        location: user.location || '',
        // Gaming Info
        favoriteGame: user.favoriteGame || '',
        skillLevel: user.skillLevel || '',
        platforms: Array.isArray(user.platforms) ? user.platforms : [],
        playStyle: user.playStyle || '',
    };

    originalData = { ...u, gameTags: [...u.gameTags] };
    gameTags = [...u.gameTags];

    const avatarUrl = u.avatarUrl ||
        `https://ui-avatars.com/api/?name=${encodeURIComponent(u.username[0]?.toUpperCase() || '?')}&background=0D8ABC&color=fff&size=128`;

    // ── Header ───────────────────────────────────────────────────
    const avatarEl = document.getElementById('profileAvatar');
    if (avatarEl) avatarEl.src = avatarUrl;

    setText('displayName', u.displayName || u.firstName || 'Player');
    setText('displayUsername', '@' + u.username);

    // Stats bar
    loadFriendCount();
    loadSquadCount();

    // ── Account Info ─────────────────────────────────────────────
    setText('display-username', u.username);
    setText('display-email', u.email);
    setInput('input-username', u.username);
    setInput('input-email', u.email);
    if (u.createdAt) {
        const d = new Date(u.createdAt).toLocaleDateString('en-US',
            { year: 'numeric', month: 'long', day: 'numeric' });
        setText('display-memberSince', d);
    }

    // ── Personal Info ─────────────────────────────────────────────
    setText('display-firstName', u.firstName || '—');
    setText('display-lastName', u.lastName || '—');
    setText('display-age', u.age || '—');
    setText('display-gender', u.gender || '—');
    setText('display-location', u.location || '—');
    setInput('input-firstName', u.firstName);
    setInput('input-lastName', u.lastName);
    setInput('input-age', u.age !== '' ? u.age : '');
    setSelect('input-gender', u.gender);
    setInput('input-location', u.location);

    // ── Gaming Info ───────────────────────────────────────────────
    setText('display-favoriteGame', u.favoriteGame || '—');
    setInput('input-favoriteGame', u.favoriteGame);
    setText('display-skillLevel', u.skillLevel || '—');
    setSelect('input-skillLevel', u.skillLevel);
    renderPlatformsDisplay(u.platforms);
    setPlatformCheckboxes(u.platforms);
    setText('display-playStyle', u.playStyle || '—');
    setSelect('input-playStyle', u.playStyle);

    // ── Bio ───────────────────────────────────────────────────────
    setText('display-bio', u.bio || 'Tell the world who you are as a gamer…');
    setInput('input-bio', u.bio);
    const bioCount = document.getElementById('bioCharCount');
    if (bioCount) bioCount.textContent = u.bio.length;

    // ── Game Tags ─────────────────────────────────────────────────
    renderGameTagsDisplay(u.gameTags);
    renderGameTagsEdit(u.gameTags);

    // ── Sidebar ───────────────────────────────────────────────────
    loadGlobalAvatar();
}

// ── Friend count stat ─────────────────────────────────────────────
async function loadFriendCount() {
    const uid = getUserId();
    if (!uid) return;
    try {
        const res = await fetch(`${API_BASE}/api/chat/friends/${uid}`);
        const data = await res.json();
        const el = document.getElementById('statFriends');
        if (el) el.textContent = (data.friends || []).length;
    } catch { /* silently fail */ }
}

// ── Squad count stat ──────────────────────────────────────────────
async function loadSquadCount() {
    const uid = getUserId();
    if (!uid) return;
    try {
        const res = await fetch(`${API_BASE}/api/chat/squads/${uid}`);
        const data = await res.json();
        const el = document.getElementById('statSquads');
        if (el) el.textContent = (data.squads || []).length;
    } catch { /* silently fail */ }
}

// ── Game Tags ─────────────────────────────────────────────────────
function renderGameTagsDisplay(tags) {
    const el = document.getElementById('display-gamesTags');
    if (!el) return;
    el.innerHTML = tags.length
        ? tags.map(t => `<span class="game-tag">${escHtml(t)}</span>`).join('')
        : '<span class="empty-tag">No games added yet</span>';
}
function renderGameTagsEdit(tags) {
    const el = document.getElementById('selectedGameTags');
    if (!el) return;
    el.innerHTML = tags.map(t =>
        `<span class="game-tag">${escHtml(t)}
            <button type="button" class="tag-remove" onclick="removeTag('${escHtml(t)}')">×</button>
         </span>`).join('');
}
function addTag() {
    const input = document.getElementById('input-gameTag');
    if (!input) return;
    const val = input.value.trim();
    if (!val || gameTags.includes(val)) { input.value = ''; return; }
    if (gameTags.length >= 10) { showToast('Max 10 game tags.', true); return; }
    gameTags.push(val);
    input.value = '';
    renderGameTagsEdit(gameTags);
}
function removeTag(tag) {
    gameTags = gameTags.filter(t => t !== tag);
    renderGameTagsEdit(gameTags);
}

// ── Platform helpers ──────────────────────────────────────────────
function renderPlatformsDisplay(platforms) {
    const el = document.getElementById('display-platforms');
    if (!el) return;
    el.innerHTML = platforms.length
        ? platforms.map(p => `<span class="game-tag">${escHtml(p)}</span>`).join('')
        : '—';
}
function setPlatformCheckboxes(platforms) {
    const menu = document.getElementById('platformDropdownMenu');
    if (!menu) return;
    menu.querySelectorAll('input[type="checkbox"]').forEach(cb => {
        cb.checked = platforms.includes(cb.value);
    });
    updatePlatformLabel();
}
function updatePlatformLabel() {
    const menu = document.getElementById('platformDropdownMenu');
    const label = document.getElementById('platformDropdownLabel');
    if (!menu || !label) return;
    const checked = [...menu.querySelectorAll('input[type="checkbox"]:checked')].map(cb => cb.value);
    label.textContent = checked.length ? checked.join(', ') : 'Select platforms…';
}
function togglePlatformDropdown() {
    const btn = document.getElementById('platformDropdownBtn');
    const menu = document.getElementById('platformDropdownMenu');
    if (!btn || !menu) return;
    const isOpen = menu.classList.toggle('open');
    btn.classList.toggle('open', isOpen);
    // Update label on every toggle (in case checkboxes changed)
    if (!isOpen) updatePlatformLabel();
}
function getCheckedPlatforms() {
    const menu = document.getElementById('platformDropdownMenu');
    if (!menu) return [];
    return [...menu.querySelectorAll('input[type="checkbox"]:checked')].map(cb => cb.value);
}

// ── Edit Mode ─────────────────────────────────────────────────────
function toggleEditMode() {
    editMode = !editMode;

    // pf-display: visible by default → hide in edit mode
    document.querySelectorAll('.pf-display').forEach(el =>
        el.style.display = editMode ? 'none' : '');

    // pf-input: CSS default is display:none → must explicitly set 'block'
    document.querySelectorAll('.pf-input').forEach(el =>
        el.style.display = editMode ? 'block' : 'none');

    // pf-hint: same as pf-input
    document.querySelectorAll('.pf-hint').forEach(el =>
        el.style.display = editMode ? 'block' : 'none');

    // Member Since group visibility
    const memberSinceGroup = document.getElementById('memberSinceGroup');
    if (memberSinceGroup) memberSinceGroup.style.display = editMode ? 'none' : 'block';

    // Tag input group needs flex
    const tagGroup = document.getElementById('tagInputGroup');
    if (tagGroup) tagGroup.style.display = editMode ? 'flex' : 'none';

    // Save/Cancel action bar
    const actionBar = document.getElementById('editActionBar');
    if (actionBar) actionBar.style.display = editMode ? 'flex' : 'none';

    // Edit button label + icon
    const btn = document.getElementById('toggleEditBtn');
    if (btn) {
        btn.querySelector('span').textContent = editMode ? 'Cancel Edit' : 'Edit Profile';
        btn.querySelector('i').className = editMode ? 'fas fa-times' : 'fas fa-pen';
    }

    if (!editMode) {
        // Cancel → restore original
        gameTags = [...originalData.gameTags];
        renderProfile({ ...currentUser, ...originalData });
    }
}
function cancelEdit() { if (editMode) toggleEditMode(); }

// ── Save Profile ──────────────────────────────────────────────────
async function saveProfile() {
    const payload = {
        bio: getInput('input-bio'),
        gameTags: gameTags,
        // Personal Info
        firstName: getInput('input-firstName'),
        lastName: getInput('input-lastName'),
        age: getInput('input-age'),
        gender: getSelect('input-gender'),
        location: getInput('input-location'),
        // Gaming Info
        favoriteGame: getInput('input-favoriteGame'),
        skillLevel: getSelect('input-skillLevel'),
        platforms: getCheckedPlatforms(),
        playStyle: getSelect('input-playStyle'),
        // Display name from first+last or standalone displayName
        displayName: buildDisplayName(
            getInput('input-firstName'),
            getInput('input-lastName'),
            currentUser.displayName || currentUser.username
        )
    };

    const newUsername = getInput('input-username');
    if (newUsername && newUsername !== currentUser.username) {
        payload.newUsername = newUsername;
    }

    const saveBtn = document.querySelector('.btn-save');
    if (saveBtn) {
        saveBtn.disabled = true;
        saveBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Saving…';
    }

    try {
        const res = await fetch(`${API_BASE}/api/users/${getUserId()}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await res.json();

        if (!res.ok) { showToast(data.error || 'Failed to save.', true); return; }

        AuthService.updateSessionUser(data.user);
        currentUser = AuthService.getCurrentUser();

        editMode = true;        // flip first so toggleEditMode correctly switches back
        toggleEditMode();
        renderProfile(data.user);
        showToast('✅ Profile saved!');

    } catch {
        showToast('❌ Server unreachable.', true);
    } finally {
        if (saveBtn) {
            saveBtn.disabled = false;
            saveBtn.innerHTML = '<i class="fas fa-check"></i> Save Changes';
        }
    }
}

// Helper: build display name from first+last, or fall back to existing
function buildDisplayName(firstName, lastName, fallback) {
    const parts = [firstName, lastName].filter(Boolean).join(' ').trim();
    return parts || fallback || '';
}

// ── Delete Account ────────────────────────────────────────────────
async function deleteAccount() {
    if (!confirm('Are you absolutely sure? This permanently deletes your account.')) return;
    try {
        const res = await fetch(`${API_BASE}/api/users/${getUserId()}`, { method: 'DELETE' });
        if (res.ok) { AuthService.logout(); window.location.href = 'login.html'; }
        else {
            const data = await res.json();
            showToast(data.error || 'Failed to delete.', true);
        }
    } catch { showToast('Server unreachable.', true); }
}

// ── Logout ────────────────────────────────────────────────────────
function logout() {
    AuthService.logout();
    window.location.href = 'login.html';
}

// ── Toast ─────────────────────────────────────────────────────────
function showToast(message, isError = false) {
    const toast = document.getElementById('toast');
    if (!toast) return;
    toast.textContent = message;
    toast.style.background = isError ? 'rgba(255,42,109,0.92)' : 'rgba(0,160,90,0.92)';
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), 3000);
}

// ── Helpers ───────────────────────────────────────────────────────
function setText(id, text) {
    const el = document.getElementById(id);
    if (el) el.textContent = text ?? '';
}
function setInput(id, value) {
    const el = document.getElementById(id);
    if (el) el.value = value ?? '';
}
function setSelect(id, value) {
    const el = document.getElementById(id);
    if (el) el.value = value ?? '';
}
function getInput(id) {
    const el = document.getElementById(id);
    return el ? el.value.trim() : '';
}
function getSelect(id) {
    const el = document.getElementById(id);
    return el ? el.value : '';
}
function escHtml(str) {
    return String(str).replace(/[&<>"']/g, c =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
