/* The application shell: brand, role-aware navigation, notification bell and
   profile menu. Rendered into #app-shell by every signed-in page.

   The links a role sees are a convenience, not a security boundary - the server
   enforces permissions in AccessGuard and SecurityConfig regardless of what the
   navigation shows. */

const NavLinks = {
  FOUNDER: [
    { key: 'overview', label: 'Dashboard' },
    { key: 'startups', label: 'My Startups' },
    { key: 'discover', label: 'Discover' },
    { key: 'notifications', label: 'Notifications' },
  ],
  INVESTOR: [
    { key: 'overview', label: 'Dashboard' },
    { key: 'discover', label: 'Discover' },
    { key: 'portfolio', label: 'My Investments' },
    { key: 'notifications', label: 'Notifications' },
  ],
  MENTOR: [
    { key: 'overview', label: 'Dashboard' },
    { key: 'discover', label: 'Startups' },
    { key: 'milestones', label: 'Milestones' },
    { key: 'notifications', label: 'Notifications' },
  ],
  ADMIN: [
    { key: 'overview', label: 'Dashboard' },
    { key: 'discover', label: 'Startups' },
    { key: 'admin', label: 'Users' },
    { key: 'notifications', label: 'Notifications' },
  ],
};

function linksForRole(role) { return NavLinks[role] || NavLinks.FOUNDER; }

function initials(name) {
  return (name || '?').trim().split(/\s+/).slice(0, 2).map(p => p[0]).join('').toUpperCase();
}

/* activeKey matches a nav key on the dashboard; pass null on other pages. */
function renderShell(user, activeKey = null) {
  const root = document.getElementById('app-shell');
  if (!root) return;

  const links = linksForRole(user.role).map(l =>
    `<a href="/dashboard.html#${l.key}" data-nav="${l.key}" class="${l.key === activeKey ? 'active' : ''}">${l.label}</a>`
  ).join('');

  root.innerHTML = `
    <header class="topbar">
      <div class="topbar-inner">
        <a class="brand" href="/dashboard.html"><span class="mark" aria-hidden="true"></span>StartupHub</a>
        <button class="icon-btn hamburger" id="nav-toggle" aria-label="Open navigation" aria-expanded="false">☰</button>
        <nav class="topnav" id="topnav" aria-label="Main">${links}</nav>
        <div class="topbar-actions">
          <span class="ws-dot" id="ws-indicator" title="Live updates" aria-hidden="true"></span>
          <a class="icon-btn" href="/dashboard.html#notifications" id="bell" aria-label="Notifications" title="Notifications">
            🔔<span class="count hidden" id="bell-count">0</span>
          </a>
          <div class="menu-wrap">
            <button class="avatar-btn" id="profile-btn" aria-haspopup="true" aria-expanded="false">
              <span class="avatar" aria-hidden="true">${initials(user.name)}</span>
              <span class="avatar-name">${escapeHtml(user.name)}</span>
            </button>
            <div class="menu" id="profile-menu" hidden>
              <div class="menu-head">
                <div class="n">${escapeHtml(user.name)}</div>
                <div class="e">${escapeHtml(user.email)}</div>
                <span class="badge badge-brand" style="margin-top:7px;">${Fmt.titleCase(user.role)}</span>
              </div>
              <a href="/profile.html">👤 Profile</a>
              <a href="/profile.html#settings">⚙ Settings</a>
              <a href="/dashboard.html#notifications">🔔 Notifications</a>
              <div class="sep"></div>
              <button id="theme-toggle">🌙 Dark mode</button>
              <div class="sep"></div>
              <button id="logout-btn" class="danger-item">↪ Log out</button>
            </div>
          </div>
        </div>
      </div>
    </header>
    <div id="toast-stack"></div>`;

  // Mobile navigation
  const toggle = document.getElementById('nav-toggle');
  const topnav = document.getElementById('topnav');
  toggle.addEventListener('click', () => {
    const open = topnav.classList.toggle('open');
    toggle.setAttribute('aria-expanded', String(open));
  });
  topnav.addEventListener('click', (e) => {
    if (e.target.tagName === 'A') { topnav.classList.remove('open'); toggle.setAttribute('aria-expanded', 'false'); }
  });

  // Profile menu
  const menuBtn = document.getElementById('profile-btn');
  const menu = document.getElementById('profile-menu');
  const closeMenu = () => { menu.hidden = true; menuBtn.setAttribute('aria-expanded', 'false'); };
  menuBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    menu.hidden = !menu.hidden;
    menuBtn.setAttribute('aria-expanded', String(!menu.hidden));
  });
  document.addEventListener('click', (e) => { if (!menu.hidden && !menu.contains(e.target)) closeMenu(); });
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeMenu(); });

  // Theme switch
  const themeBtn = document.getElementById('theme-toggle');
  const paintThemeLabel = () => {
    themeBtn.textContent = Theme.get() === 'dark' ? '☀ Light mode' : '🌙 Dark mode';
  };
  paintThemeLabel();
  themeBtn.addEventListener('click', () => { Theme.toggle(); paintThemeLabel(); });

  document.getElementById('logout-btn').addEventListener('click', logoutRequest);

  refreshBell();
}

function setActiveNav(key) {
  document.querySelectorAll('.topnav a[data-nav]').forEach(a => {
    a.classList.toggle('active', a.dataset.nav === key);
  });
}

/* Unread badge on the bell. Called on load and again after every live event. */
async function refreshBell() {
  const badge = document.getElementById('bell-count');
  if (!badge) return;
  try {
    const { count } = await Api.get('/api/notifications/unread-count');
    badge.textContent = count > 9 ? '9+' : String(count);
    badge.classList.toggle('hidden', !count);
  } catch (_) { /* a failed badge refresh should never break the page */ }
}

function setWsIndicator(live) {
  const el = document.getElementById('ws-indicator');
  if (el) {
    el.classList.toggle('live', live);
    el.title = live ? 'Live updates connected' : 'Live updates offline';
  }
}

/* Stage helpers shared by the dashboard and the startup page. */
const STAGES = ['IDEA', 'MVP', 'GROWTH', 'SCALE'];

function stageProgress(stage) {
  const idx = STAGES.indexOf(stage);
  return idx < 0 ? 0 : Math.round(((idx + 1) / STAGES.length) * 100);
}

function stageBadge(stage) {
  return `<span class="badge badge-stage-${stage}">${stage}</span>`;
}

/* The four stages are a real sequence, so they are numbered and connected. */
function stageTimelineHtml(stage) {
  const idx = STAGES.indexOf(stage);
  return `<ul class="timeline">` + STAGES.map((s, i) => {
    const cls = i < idx ? 'done' : i === idx ? 'current' : '';
    const mark = i < idx ? '✓' : String(i + 1);
    const sub = i < idx ? 'Completed' : i === idx ? 'Current stage' : 'Upcoming';
    return `<li class="${cls}">
              <span class="dot" aria-hidden="true">${mark}</span>
              <span class="lbl">${Fmt.titleCase(s)}</span>
              <span class="sub">${sub}</span>
            </li>`;
  }).join('') + `</ul>`;
}
