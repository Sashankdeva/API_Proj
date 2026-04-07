/**
 * home.js - Home Page & Search Modal Logic
 */

let allPublicSquads = []; // Store fetched squads
let availableGames = new Set();
let availableLanguages = new Set();

document.addEventListener('DOMContentLoaded', async () => {
    // Current user is guaranteed by script.js auth guard on this page
    const user = JSON.parse(sessionStorage.getItem('squadup_user'));
    if (!user) return;
    
    await loadPublicSquads(user.id);
});

// ── Fetch & Render ──────────────────────────────────────────────────
async function loadPublicSquads(userId) {
    const API = typeof API_BASE !== 'undefined' ? API_BASE : 'http://127.0.0.1:5500';

    try {
        const res = await fetch(`${API}/api/chat/squads/public/${userId}`);
        const data = await res.json();
        
        allPublicSquads = data.squads || [];
        
        // Extract unique games and languages for dynamic filters
        availableGames.clear();
        availableLanguages.clear();
        allPublicSquads.forEach(sq => {
            if (sq.game) availableGames.add(sq.game);
            if (sq.language) availableLanguages.add(sq.language);
        });
        buildDynamicFilters();
        renderTrendingTags(allPublicSquads);  // ← dynamic trending

    } catch (e) {
        console.error("Error loading public squads:", e);
        renderTrendingTags([]);  // fallback
    }
}

function renderSquadGrid(squads, containerId) {
    const grid = document.getElementById(containerId);
    if (!grid) return;
    
    grid.innerHTML = '';

    if (!squads.length && containerId === 'modal-squads-grid') {
        grid.innerHTML = `<div style="text-align:center; padding: 2rem; width: 100%; grid-column: 1 / -1;">No squads found matching your filters.</div>`;
        return;
    }

    squads.forEach(s => {
        const memberCount = Array.isArray(s.members) ? s.members.length : 0;
        const memberAvatars = (s.members || []).slice(0, 4).map(m => {
            const name = typeof m === 'object' ? (m.displayName || m.username || '?') : '?';
            const url = (typeof m === 'object' && m.avatarUrl) ? m.avatarUrl :
                `https://ui-avatars.com/api/?name=${encodeURIComponent(name[0].toUpperCase())}&background=141419&color=00f3ff&size=32`;
            return `<img src="${escHtml(url)}" alt="${escHtml(name)}" class="squad-member-avatar">`;
        }).join('') + (memberCount > 4 ? `<div class="squad-member-more">+${memberCount - 4}</div>` : '');

        const isFull = memberCount >= s.maxMembers;

        const card = document.createElement('div');
        card.className = 'squad-card';
        card.innerHTML = `
            <div class="squad-card-header">
                <h3>${escHtml(s.name)}</h3>
                <span class="squad-game-tag">${escHtml(s.game)}</span>
            </div>
            <div class="squad-card-body">
                <p class="squad-members-count">
                    <i class="fas fa-users"></i> ${memberCount}/${s.maxMembers} Members
                    &nbsp;·&nbsp;<i class="fas fa-globe"></i> ${escHtml(s.language)}
                </p>
                <div class="squad-members-avatars">${memberAvatars}</div>
            </div>
            <div class="squad-card-actions">
                <button class="btn-primary" style="width: 100%; padding: 10px; font-size: 0.9rem;" 
                    ${isFull ? 'disabled style="background:var(--text-muted);color:#333;cursor:not-allowed;"' : `onclick="joinSquad('${s._id}', this)"`}>
                    <i class="fas ${isFull ? 'fa-lock' : 'fa-door-open'}"></i> ${isFull ? 'FULL' : 'JOIN SQUAD'}
                </button>
            </div>`;
        grid.appendChild(card);
    });
}

// ── Trending Games ──────────────────────────────────────────────────

/**
 * Ranks games by total players across their squads,
 * then by squad count as a tiebreaker. Returns top N.
 */
function computeTrendingGames(squads, topN = 5) {
    const gameMap = {};
    squads.forEach(sq => {
        if (!sq.game) return;
        if (!gameMap[sq.game]) gameMap[sq.game] = { squadCount: 0, playerCount: 0 };
        gameMap[sq.game].squadCount++;
        gameMap[sq.game].playerCount += Array.isArray(sq.members) ? sq.members.length : 0;
    });
    return Object.entries(gameMap)
        .sort((a, b) =>
            b[1].playerCount - a[1].playerCount ||
            b[1].squadCount  - a[1].squadCount
        )
        .slice(0, topN)
        .map(([game, stats]) => ({ game, ...stats }));
}

function renderTrendingTags(squads) {
    const container = document.getElementById('trendingTagsContainer');
    const loading   = document.getElementById('trendingLoading');
    if (!container) return;

    // Remove the loading placeholder
    if (loading) loading.remove();

    // Remove any previously rendered tags (re-render safe)
    container.querySelectorAll('.tag').forEach(t => t.remove());

    const trending = computeTrendingGames(squads);

    if (!trending.length) {
        const empty = document.createElement('span');
        empty.style.cssText = 'color:var(--text-muted);font-size:0.85rem;';
        empty.textContent = 'No squads yet — be the first to create one!';
        container.appendChild(empty);
        return;
    }

    trending.forEach(({ game, squadCount, playerCount }) => {
        const tag = document.createElement('span');
        tag.className = 'tag';
        tag.title = `${squadCount} squad${squadCount !== 1 ? 's' : ''} · ${playerCount} player${playerCount !== 1 ? 's' : ''}`;
        tag.innerHTML = `${escHtml(game)} <span style="
            background:rgba(0,243,255,0.15);
            color:var(--primary-color);
            font-size:0.7rem;
            padding:1px 5px;
            border-radius:8px;
            margin-left:4px;
            font-weight:700;
        ">${playerCount}👥</span>`;
        tag.onclick = () => openSearchModalWithFilter(game);
        container.appendChild(tag);
    });
}

// ── Search & Filter Logic ───────────────────────────────────────────

function buildDynamicFilters() {
    const gameSelect = document.getElementById('filter-game');
    const langSelect = document.getElementById('filter-language');
    if (!gameSelect || !langSelect) return;

    // Preserve "Games" option
    gameSelect.innerHTML = `<option value="">Games</option>`;
    const gamesRaw = Array.from(availableGames).sort((a,b) => a.localeCompare(b));
    gamesRaw.forEach(game => {
        gameSelect.innerHTML += `<option value="${escHtml(game)}">${escHtml(game)}</option>`;
    });

    // Preserve "Languages" option
    langSelect.innerHTML = `<option value="">Languages</option>`;
    
    // Static list of popular languages
    const predefinedLangs = [
        'English', 'Spanish', 'French', 'German', 
        'Japanese', 'Hindi', 'Portuguese', 'Russian'
    ];
    
    // Merge predefined with any extra languages dynamically created
    const extraLangs = Array.from(availableLanguages)
        .filter(lang => !predefinedLangs.includes(lang))
        .sort((a,b) => a.localeCompare(b));
        
    const finalLangs = [...predefinedLangs, ...extraLangs];

    finalLangs.forEach(lang => {
        langSelect.innerHTML += `<option value="${escHtml(lang)}">${escHtml(lang)}</option>`;
    });
}

function applyFilters() {
    const textQuery = document.getElementById('modalSearchInput').value.toLowerCase().trim();
    const gameQuery = document.getElementById('filter-game').value;
    const langQuery = document.getElementById('filter-language').value;
    
    const filtered = allPublicSquads.filter(sq => {
        // Match Text Search
        const matchText = !textQuery || 
                          sq.name.toLowerCase().includes(textQuery) || 
                          sq.game.toLowerCase().includes(textQuery) ||
                          sq.language.toLowerCase().includes(textQuery);
                          
        // Match Comboboxes
        const matchGame = !gameQuery || sq.game === gameQuery;
        const matchLang = !langQuery || sq.language === langQuery;

        return matchText && matchGame && matchLang;
    });

    renderSquadGrid(filtered, 'modal-squads-grid');
}

// ── Modal Handlers ──────────────────────────────────────────────────

function openSearchModal() {
    const modal = document.getElementById('searchFilterModal');
    const content = modal.querySelector('.search-modal-content');
    
    content.classList.remove('closing');
    modal.style.display = 'flex';
    document.getElementById('modalSearchInput').focus();
    
    applyFilters();
}

function openSearchModalWithFilter(gameStr) {
    const modal = document.getElementById('searchFilterModal');
    const content = modal.querySelector('.search-modal-content');
    
    content.classList.remove('closing');
    modal.style.display = 'flex';
    
    // Set up the inputs
    document.getElementById('modalSearchInput').value = ''; 
    const gameSelect = document.getElementById('filter-game');
    const langSelect = document.getElementById('filter-language');
    
    if(gameSelect) gameSelect.value = gameStr;
    if(langSelect) langSelect.value = '';

    applyFilters();
}

function closeSearchModal() {
    const modal = document.getElementById('searchFilterModal');
    const content = modal.querySelector('.search-modal-content');
    
    content.classList.add('closing');
    
    setTimeout(() => {
        modal.style.display = 'none';
        
        // Reset inputs completely
        document.getElementById('modalSearchInput').value = '';
        const gameSelect = document.getElementById('filter-game');
        const langSelect = document.getElementById('filter-language');
        if(gameSelect) gameSelect.value = '';
        if(langSelect) langSelect.value = '';
        
    }, 240); 
}

// ── Join Squad ──────────────────────────────────────────────────────

async function joinSquad(squadId, btnElement) {
    const user = JSON.parse(sessionStorage.getItem('squadup_user'));
    if (!user) return;
    
    btnElement.disabled = true;
    btnElement.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Joining...';

    const API = typeof API_BASE !== 'undefined' ? API_BASE : 'http://127.0.0.1:5500';
    
    try {
        const res = await fetch(`${API}/api/chat/squads/${squadId}/join`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId: user.id })
        });
        
        const data = await res.json();
        
        if (data.success) {
            btnElement.innerHTML = '<i class="fas fa-check"></i> Joined!';
            btnElement.style.background = 'rgba(0,160,90,0.8)';
            btnElement.style.color = 'white';
            
            // Redirect to Squads Page after a short delay
            setTimeout(() => {
                window.location.href = 'squads.html';
            }, 800);
        } else {
            alert(data.error || "Failed to join squad.");
            btnElement.disabled = false;
            btnElement.innerHTML = '<i class="fas fa-door-open"></i> JOIN SQUAD';
        }
    } catch(err) {
         alert("Cannot connect to server.");
         btnElement.disabled = false;
         btnElement.innerHTML = '<i class="fas fa-door-open"></i> JOIN SQUAD';
    }
}

// Helper
function escHtml(str) {
    return String(str).replace(/[&<>"']/g, c =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
