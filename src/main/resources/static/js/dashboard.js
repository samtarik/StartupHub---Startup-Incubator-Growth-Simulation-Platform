/* Dashboard page.

   One HTML file holds several views (Dashboard, My Startups, Discover,
   Portfolio, Milestones, Notifications, Admin) and the hash decides which is
   visible, so navigating between them is instant and no page is duplicated.

   Which views exist at all depends on the signed-in user's role. */

let currentUser = null;
let summary = null;
let allStartups = [];
let myStartups = [];
const loaded = new Set();

const VIEWS = ['overview', 'startups', 'discover', 'portfolio', 'milestones', 'notifications', 'admin'];

/* ---- Boot --------------------------------------------------------------- */
async function init() {
  currentUser = await Api.get('/api/auth/me');
  renderShell(currentUser);

  window.addEventListener('hashchange', route);
  route();

  connectNotifications(currentUser.id, () => {
    // A live event can change any of these, so drop the caches and repaint
    // whatever the user is currently looking at.
    loaded.delete('overview');
    loaded.delete('notifications');
    loaded.delete('portfolio');
    route();
  });
}

function currentViewKey() {
  const key = (window.location.hash || '').replace('#', '');
  const allowed = linksForRole(currentUser.role).map(l => l.key);
  if (VIEWS.includes(key) && (allowed.includes(key) || key === 'startups' || key === 'portfolio')) return key;
  return 'overview';
}

function route() {
  const key = currentViewKey();
  VIEWS.forEach(v => {
    const el = document.getElementById(`view-${v}`);
    if (el) el.classList.toggle('hidden', v !== key);
  });
  setActiveNav(key);
  window.scrollTo(0, 0);

  const loaders = {
    overview: loadOverview,
    startups: loadMyStartups,
    discover: loadDiscover,
    portfolio: loadPortfolio,
    milestones: loadMentorMilestones,
    notifications: loadNotifications,
    admin: loadAdmin,
  };
  if (!loaded.has(key)) {
    loaded.add(key);
    loaders[key]().catch(err => toastError(err.message || `Could not load ${key}`));
  }
}

function reload(key) {
  loaded.delete(key);
  if (currentViewKey() === key) route();
}

/* ---- Shared pieces ------------------------------------------------------ */
function greetingFor(date = new Date()) {
  const h = date.getHours();
  if (h < 12) return 'Good morning';
  if (h < 18) return 'Good afternoon';
  return 'Good evening';
}

/* pct may be null, which means there was no previous month to compare with -
   in that case we show nothing rather than inventing a number. */
function deltaChip(pct, { upIsGood = true, suffix = '' } = {}) {
  if (pct === null || pct === undefined) return '';
  const rounded = Math.round(Math.abs(pct) * 10) / 10;
  if (rounded === 0) return `<span class="delta flat">→ 0%${suffix}</span>`;
  const rising = pct > 0;
  const good = rising === upIsGood;
  return `<span class="delta ${good ? 'up' : 'down'}">${rising ? '↑' : '↓'} ${rounded}%${suffix}</span>`;
}

function startupCard(s, { showInvest = false } = {}) {
  const progress = stageProgress(s.stage);
  return `
    <article class="startup-card">
      <div class="sc-top">
        <div style="min-width:0;">
          <div class="sc-name">${escapeHtml(s.name)}</div>
          <div class="subtle" style="font-size:0.78rem;">by ${escapeHtml(s.founderName)}</div>
        </div>
        ${stageBadge(s.stage)}
      </div>
      <p class="sc-desc">${escapeHtml(s.description || 'No description yet.')}</p>
      <div class="sc-stats">
        <div class="sc-stat"><div class="k">Valuation</div><div class="v">${Fmt.money(s.valuation)}</div></div>
        <div class="sc-stat"><div class="k">Started</div><div class="v" style="font-size:0.82rem;">${Fmt.date(s.createdAt)}</div></div>
        <div class="sc-stat"><div class="k">Stage</div><div class="v" style="font-size:0.82rem;">${Fmt.titleCase(s.stage)}</div></div>
      </div>
      <div>
        <div class="progress-row"><span>Stage progress</span><span class="mono">${progress}%</span></div>
        <div class="progress"><span style="width:${progress}%"></span></div>
      </div>
      <div class="toolbar">
        <a class="btn btn-outline btn-sm" href="/startup.html?id=${s.id}">Open startup</a>
        ${showInvest ? `<button class="btn btn-primary btn-sm" data-invest="${s.id}" data-name="${escapeHtml(s.name)}">Invest</button>` : ''}
      </div>
    </article>`;
}

function canCreateStartup() {
  return currentUser.role === 'FOUNDER' || currentUser.role === 'ADMIN';
}

function canInvest() {
  return currentUser.role === 'INVESTOR' || currentUser.role === 'ADMIN';
}

/* ---- Create startup ----------------------------------------------------- */
function openNewStartupModal() {
  openFormModal({
    title: 'Create a startup',
    subtitle: 'You can change the stage and add everything else once it exists.',
    submitLabel: 'Create startup',
    loadingLabel: 'Creating…',
    fields: [
      { name: 'name', label: 'Startup name', type: 'text', required: true, colspan: 'full', placeholder: 'NovaTech' },
      { name: 'description', label: 'What does it do?', type: 'textarea', colspan: 'full', rows: 3,
        placeholder: 'AI-powered productivity platform for small teams' },
    ],
    onSubmit: async (values) => {
      const created = await Api.post('/api/startups', values);
      toastSuccess('Startup created', `${created.name} is ready to set up.`);
      ['overview', 'startups', 'discover'].forEach(k => loaded.delete(k));
      route();
    },
  });
}

/* ---- Invest (from Discover) --------------------------------------------- */
function openInvestModal(startupId, startupName) {
  openFormModal({
    title: `Invest in ${startupName}`,
    subtitle: 'The founder is notified live the moment this is recorded.',
    submitLabel: 'Record investment',
    loadingLabel: 'Recording…',
    fields: [
      { name: 'type', label: 'Funding type', type: 'select', required: true, options: [
        { value: 'ANGEL', label: 'Angel' },
        { value: 'VENTURE_CAPITAL', label: 'Venture capital' },
        { value: 'CROWDFUNDING', label: 'Crowdfunding' },
      ] },
      { name: 'amount', label: 'Amount (USD)', type: 'number', required: true, min: 0.01, step: '0.01', placeholder: '25000' },
    ],
    onSubmit: async (values) => {
      await Api.post(`/api/startups/${startupId}/investments`, values);
      toastSuccess('Investment recorded', `${Fmt.money(values.amount, { compact: false })} into ${startupName}.`);
      ['overview', 'discover', 'portfolio'].forEach(k => loaded.delete(k));
      route();
    },
  });
}

/* ---- Dashboard view ----------------------------------------------------- */
async function loadOverview() {
  document.getElementById('greeting').textContent = `${greetingFor()}, ${currentUser.name.split(' ')[0]} 👋`;

  const actions = document.getElementById('overview-actions');
  actions.innerHTML = canCreateStartup()
    ? `<button class="btn btn-primary" id="new-startup-btn">+ New startup</button>` : '';
  const newBtn = document.getElementById('new-startup-btn');
  if (newBtn) newBtn.addEventListener('click', openNewStartupModal);

  document.getElementById('kpi-grid').innerHTML = skeletonCards(4);
  document.getElementById('overview-startups').innerHTML = skeletonCards(2);
  document.getElementById('activity-feed').innerHTML = skeletonRows(5, 34);

  const [sum, activity] = await Promise.all([
    Api.get('/api/dashboard/summary'),
    Api.get('/api/dashboard/activity?limit=10'),
  ]);
  summary = sum;

  renderKpis(sum);
  renderSubheading(sum);
  renderTrendChart(sum.trend || []);
  renderActivity(activity);
  await renderOverviewStartups();
}

function renderSubheading(sum) {
  const sub = document.getElementById('greeting-sub');
  if (currentUser.role === 'INVESTOR') {
    sub.textContent = sum.startupCount
      ? `You are backing ${sum.startupCount} startup${sum.startupCount === 1 ? '' : 's'}.`
      : 'Find a startup to back from the Discover tab.';
  } else if (currentUser.role === 'FOUNDER') {
    sub.textContent = sum.startupCount
      ? `Here's what's happening across your ${sum.startupCount === 1 ? 'startup' : `${sum.startupCount} startups`} today.`
      : 'Create your first startup to start tracking it.';
  } else {
    sub.textContent = `${sum.startupCount} startup${sum.startupCount === 1 ? '' : 's'} on the platform right now.`;
  }
}

function renderKpis(sum) {
  const grid = document.getElementById('kpi-grid');

  if (currentUser.role === 'INVESTOR') {
    grid.innerHTML = [
      kpiCard({ label: 'Total invested', value: Fmt.money(sum.myInvested || 0), icon: '💰', accent: 'growth',
        foot: `${sum.myInvestmentCount || 0} investment${(sum.myInvestmentCount || 0) === 1 ? '' : 's'}` }),
      kpiCard({ label: 'Startups backed', value: Fmt.number(sum.startupCount), icon: '🚀', accent: 'brand',
        foot: 'In your portfolio' }),
      kpiCard({ label: 'Portfolio valuation', value: Fmt.money(sum.totalValuation), icon: '📈', accent: 'brand',
        foot: deltaChip(sum.valuationChange) || 'Combined valuation' }),
      kpiCard({ label: 'Portfolio revenue', value: Fmt.money(sum.totalRevenue), icon: '💵', accent: 'growth',
        foot: deltaChip(sum.revenueChange) || 'Across backed startups' }),
    ].join('');
    return;
  }

  const teamFoot = sum.teamMembers
    ? `Average performance ${sum.avgPerformance}/100`
    : 'No one hired yet';

  grid.innerHTML = [
    kpiCard({ label: 'Total valuation', value: Fmt.money(sum.totalValuation), icon: '📈', accent: 'brand',
      foot: deltaChip(sum.valuationChange) ||
            (sum.newFundingThisMonth ? `${Fmt.money(sum.newFundingThisMonth)} raised this month` : 'No funding yet') }),
    kpiCard({ label: 'Revenue', value: Fmt.money(sum.totalRevenue), icon: '💵', accent: 'growth',
      foot: deltaChip(sum.revenueChange) || 'All time' }),
    kpiCard({ label: 'Expenses', value: Fmt.money(sum.totalExpense), icon: '🔥', accent: 'burn',
      foot: deltaChip(sum.expenseChange, { upIsGood: false }) || 'All time' }),
    kpiCard({ label: 'Team members', value: Fmt.number(sum.teamMembers), icon: '👥', accent: 'brand',
      foot: teamFoot }),
  ].join('');
}

function renderTrendChart(trend) {
  const holder = document.getElementById('trend-holder');
  const hasData = trend.some(p => p.revenue || p.expense);

  if (!hasData) {
    holder.innerHTML = emptyState({
      icon: '📊',
      title: 'No finance data yet',
      description: 'Record revenue and expenses on a startup and the trend will appear here.',
    });
    return;
  }

  holder.innerHTML = '<canvas id="chart-trend"></canvas>';
  Charts.revenueVsExpense('chart-trend',
    trend.map(p => p.month),
    trend.map(p => p.revenue),
    trend.map(p => p.expense));
}

function renderActivity(items) {
  const el = document.getElementById('activity-feed');
  if (!items.length) {
    el.innerHTML = emptyState({
      icon: '🕓',
      title: 'Nothing has happened yet',
      description: 'Investments, milestones and finance entries will show up here as they happen.',
    });
    return;
  }
  const icons = { investment: '💰', revenue: '💵', expense: '🔥', notification: '🔔', startup: '🚀', mvp: '🧩' };
  el.innerHTML = `<ul class="activity">` + items.map(a => `
    <li>
      <span class="ic ${a.type}" aria-hidden="true">${icons[a.type] || '●'}</span>
      <span style="min-width:0;">
        <span class="msg">${escapeHtml(a.message)}</span><br>
        <span class="when">${escapeHtml(Fmt.relative(a.at))}</span>
      </span>
    </li>`).join('') + `</ul>`;
}

async function renderOverviewStartups() {
  const el = document.getElementById('overview-startups');
  const heading = document.getElementById('startups-heading');
  let list;

  if (currentUser.role === 'FOUNDER') {
    heading.textContent = 'My startups';
    myStartups = await Api.get('/api/startups/mine');
    list = myStartups;
  } else if (currentUser.role === 'INVESTOR') {
    heading.textContent = 'Startups you back';
    const data = await Api.get('/api/investments/mine');
    const ids = [...new Set(data.investments.map(i => i.startupId))];
    allStartups = await Api.get('/api/startups');
    list = allStartups.filter(s => ids.includes(s.id));
  } else {
    heading.textContent = 'Newest on the platform';
    allStartups = await Api.get('/api/startups');
    list = [...allStartups].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  }

  if (!list.length) {
    el.innerHTML = `<div style="grid-column:1/-1;">` + emptyState({
      icon: '🚀',
      title: canCreateStartup() ? 'No startups yet' : 'Nothing here yet',
      description: canCreateStartup()
        ? 'Create your first startup and start tracking its stage, team and finances.'
        : 'Browse the Discover tab to find startups on the platform.',
      actionLabel: canCreateStartup() ? '+ New startup' : '',
      actionId: 'empty-new-startup',
    }) + `</div>`;
    const btn = document.getElementById('empty-new-startup');
    if (btn) btn.addEventListener('click', openNewStartupModal);
    return;
  }

  el.innerHTML = list.slice(0, 4).map(s => startupCard(s)).join('');
}

/* ---- My startups view --------------------------------------------------- */
async function loadMyStartups() {
  const el = document.getElementById('my-startups');
  el.innerHTML = skeletonCards(3);
  document.getElementById('new-startup-btn-2').onclick = openNewStartupModal;
  document.getElementById('new-startup-btn-2').classList.toggle('hidden', !canCreateStartup());

  myStartups = await Api.get('/api/startups/mine');
  if (!myStartups.length) {
    el.innerHTML = `<div style="grid-column:1/-1;">` + emptyState({
      icon: '🚀',
      title: 'No startups yet',
      description: 'Create one and you can add a team, finances, investors and a roadmap to it.',
      actionLabel: canCreateStartup() ? '+ New startup' : '',
      actionId: 'empty-new-startup-2',
    }) + `</div>`;
    const btn = document.getElementById('empty-new-startup-2');
    if (btn) btn.addEventListener('click', openNewStartupModal);
    return;
  }
  el.innerHTML = myStartups.map(s => startupCard(s)).join('');
}

/* ---- Discover view ------------------------------------------------------ */
async function loadDiscover() {
  const list = document.getElementById('discover-list');
  list.innerHTML = skeletonCards(3);

  if (currentUser.role === 'MENTOR' || currentUser.role === 'ADMIN') {
    document.getElementById('discover-title').textContent = 'All startups';
    document.getElementById('discover-sub').textContent = 'Every startup on the platform.';
  }

  allStartups = await Api.get('/api/startups');

  ['search-input', 'stage-filter', 'sort-select'].forEach(id => {
    const el = document.getElementById(id);
    el.oninput = renderDiscover;
    el.onchange = renderDiscover;
  });

  // Delegated once, so reloading the list does not stack duplicate handlers.
  if (!list.dataset.wired) {
    list.dataset.wired = '1';
    list.addEventListener('click', (e) => {
      const btn = e.target.closest('[data-invest]');
      if (btn) openInvestModal(Number(btn.dataset.invest), btn.dataset.name);
    });
  }

  renderDiscover();
}

function renderDiscover() {
  const term = document.getElementById('search-input').value.trim().toLowerCase();
  const stage = document.getElementById('stage-filter').value;
  const sort = document.getElementById('sort-select').value;

  let rows = allStartups.filter(s => {
    const matchesStage = !stage || s.stage === stage;
    const haystack = `${s.name} ${s.description || ''} ${s.founderName}`.toLowerCase();
    return matchesStage && (!term || haystack.includes(term));
  });

  const sorters = {
    valuation: (a, b) => b.valuation - a.valuation,
    newest: (a, b) => new Date(b.createdAt) - new Date(a.createdAt),
    name: (a, b) => a.name.localeCompare(b.name),
    stage: (a, b) => STAGES.indexOf(b.stage) - STAGES.indexOf(a.stage),
  };
  rows = [...rows].sort(sorters[sort] || sorters.valuation);

  const count = document.getElementById('discover-count');
  count.textContent = rows.length === allStartups.length
    ? `${rows.length} startup${rows.length === 1 ? '' : 's'}`
    : `${rows.length} of ${allStartups.length} startups match`;

  const list = document.getElementById('discover-list');
  if (!rows.length) {
    list.innerHTML = `<div style="grid-column:1/-1;">` + emptyState({
      icon: '🔍',
      title: 'No startups match',
      description: 'Try a different search term or clear the stage filter.',
    }) + `</div>`;
    return;
  }
  list.innerHTML = rows.map(s => startupCard(s, { showInvest: canInvest() })).join('');
}

/* ---- Portfolio view (investor) ------------------------------------------ */
async function loadPortfolio() {
  const body = document.getElementById('portfolio-body');
  body.innerHTML = skeletonTableRows(5, 3);

  const data = await Api.get('/api/investments/mine');
  const items = data.investments;

  document.getElementById('portfolio-kpis').innerHTML = [
    kpiCard({ label: 'Total invested', value: Fmt.money(data.totalInvested), icon: '💰', accent: 'growth' }),
    kpiCard({ label: 'Startups backed', value: Fmt.number(data.startupCount), icon: '🚀', accent: 'brand' }),
    kpiCard({ label: 'Investments made', value: Fmt.number(items.length), icon: '🧾', accent: 'brand' }),
    kpiCard({ label: 'Average cheque', value: Fmt.money(items.length ? data.totalInvested / items.length : 0),
      icon: '📊', accent: 'brand' }),
  ].join('');

  if (!items.length) {
    body.innerHTML = emptyRow(5, 'You have not backed any startup yet.');
    document.getElementById('portfolio-chart-holder').innerHTML = emptyState({
      icon: '💰',
      title: 'No investments yet',
      description: 'Back a startup from the Discover tab and it will appear here.',
    });
    return;
  }

  body.innerHTML = items
    .sort((a, b) => new Date(b.investedOn) - new Date(a.investedOn))
    .map(i => `
      <tr>
        <td>${escapeHtml(i.startupName)}</td>
        <td><span class="badge badge-neutral">${Fmt.titleCase(i.type)}</span></td>
        <td class="mono">${Fmt.money(i.amount, { compact: false })}</td>
        <td>${Fmt.date(i.investedOn)}</td>
        <td class="actions"><a class="btn btn-sm btn-outline" href="/startup.html?id=${i.startupId}">Open</a></td>
      </tr>`).join('');

  const byType = {};
  items.forEach(i => { byType[i.type] = (byType[i.type] || 0) + i.amount; });
  document.getElementById('portfolio-chart-holder').innerHTML = '<canvas id="chart-portfolio"></canvas>';
  Charts.investmentMix('chart-portfolio', Object.keys(byType).map(Fmt.titleCase), Object.values(byType));
}

/* ---- Mentor milestones view --------------------------------------------- */
async function loadMentorMilestones() {
  const el = document.getElementById('mentor-milestones');
  el.innerHTML = skeletonRows(4, 44);

  const startups = await Api.get('/api/startups');
  // One request per startup; capped so a large platform does not fan out badly.
  const subset = startups.slice(0, 8);
  const roadmapSets = await Promise.all(
    subset.map(s => Api.get(`/api/startups/${s.id}/roadmaps`).catch(() => []))
  );

  const open = [];
  subset.forEach((s, i) => {
    (roadmapSets[i] || []).forEach(r => {
      r.milestones.filter(m => m.status !== 'DONE')
        .forEach(m => open.push({ ...m, startupName: s.name, startupId: s.id, roadmapTitle: r.title }));
    });
  });

  if (!open.length) {
    el.innerHTML = emptyState({
      icon: '✅',
      title: 'Nothing is outstanding',
      description: 'Every milestone on the startups you mentor is already done.',
    });
    return;
  }

  open.sort((a, b) => {
    if (!a.deadline) return 1;
    if (!b.deadline) return -1;
    return new Date(a.deadline) - new Date(b.deadline);
  });

  el.innerHTML = `<div class="card"><div class="table-wrap"><table>
      <thead><tr><th>Milestone</th><th>Startup</th><th>Deadline</th><th>Status</th><th></th></tr></thead>
      <tbody>` + open.map(m => `
        <tr>
          <td>
            <strong>${escapeHtml(m.title)}</strong>
            ${m.description ? `<div class="subtle" style="font-size:0.79rem;">${escapeHtml(m.description)}</div>` : ''}
          </td>
          <td>${escapeHtml(m.startupName)}</td>
          <td class="mono">${Fmt.date(m.deadline)}</td>
          <td>${milestoneStatusBadge(m)}</td>
          <td class="actions">
            <button class="btn btn-sm btn-success" data-complete="${m.id}">Mark done</button>
          </td>
        </tr>`).join('') + `</tbody></table></div></div>`;

  el.querySelectorAll('[data-complete]').forEach(btn => {
    btn.addEventListener('click', async () => {
      setButtonLoading(btn, true, 'Saving…');
      try {
        await Api.post(`/api/milestones/${btn.dataset.complete}/complete`);
        toastSuccess('Milestone completed');
        reload('milestones');
      } catch (err) {
        toastError(err.message || 'Could not update the milestone');
        setButtonLoading(btn, false);
      }
    });
  });
}

/* ---- Notifications view ------------------------------------------------- */
async function loadNotifications() {
  const el = document.getElementById('notification-list');
  el.innerHTML = skeletonRows(4, 46);

  const markAll = document.getElementById('mark-all-read');
  markAll.onclick = async () => {
    setButtonLoading(markAll, true, 'Updating…');
    try {
      await Api.post('/api/notifications/read-all');
      toastSuccess('All notifications marked as read');
      refreshBell();
      reload('notifications');
    } catch (err) {
      toastError(err.message || 'Could not update notifications');
    } finally {
      setButtonLoading(markAll, false);
    }
  };

  const items = await Api.get('/api/notifications');
  if (!items.length) {
    el.innerHTML = emptyState({
      icon: '🔔',
      title: 'No notifications yet',
      description: 'When an investment lands or a milestone is completed, it shows up here instantly.',
    });
    markAll.classList.add('hidden');
    return;
  }
  markAll.classList.toggle('hidden', !items.some(n => !n.read));

  el.innerHTML = items.map(n => `
    <div class="notif ${n.read ? '' : 'unread'}">
      <span style="min-width:0;">
        <span class="msg">${escapeHtml(n.message)}</span><br>
        <span class="when">${escapeHtml(Fmt.relative(n.createdAt))}</span>
      </span>
      ${n.read ? '' : `<button class="btn btn-sm btn-outline" data-read="${n.id}">Mark read</button>`}
    </div>`).join('');

  el.querySelectorAll('[data-read]').forEach(btn => {
    btn.addEventListener('click', async () => {
      setButtonLoading(btn, true, '…');
      try {
        await Api.post(`/api/notifications/${btn.dataset.read}/read`);
        refreshBell();
        reload('notifications');
      } catch (err) {
        toastError(err.message || 'Could not mark it as read');
        setButtonLoading(btn, false);
      }
    });
  });
}

/* ---- Admin view --------------------------------------------------------- */
async function loadAdmin() {
  const body = document.getElementById('admin-users-body');
  body.innerHTML = skeletonTableRows(3, 4);

  const [stats, users] = await Promise.all([
    Api.get('/api/admin/stats'),
    Api.get('/api/admin/users'),
  ]);

  document.getElementById('admin-kpis').innerHTML = [
    kpiCard({ label: 'Users', value: Fmt.number(stats.userCount), icon: '👤', accent: 'brand',
      foot: `${stats.byRole.FOUNDER} founders · ${stats.byRole.INVESTOR} investors` }),
    kpiCard({ label: 'Startups', value: Fmt.number(stats.startupCount), icon: '🚀', accent: 'brand' }),
    kpiCard({ label: 'Capital raised', value: Fmt.money(stats.totalRaised), icon: '💰', accent: 'growth',
      foot: `${stats.investmentCount} investments` }),
    kpiCard({ label: 'Total valuation', value: Fmt.money(stats.totalValuation), icon: '📈', accent: 'brand' }),
  ].join('');

  body.innerHTML = users.length ? users.map(u => `
    <tr>
      <td>${escapeHtml(u.name)}</td>
      <td class="subtle">${escapeHtml(u.email)}</td>
      <td><span class="badge badge-neutral">${Fmt.titleCase(u.role)}</span></td>
    </tr>`).join('') : emptyRow(3, 'No users registered.');

  Charts.render('chart-stages', (c, self) => ({
    type: 'bar',
    data: {
      labels: Object.keys(stats.byStage).map(Fmt.titleCase),
      datasets: [{ label: 'Startups', data: Object.values(stats.byStage),
                   backgroundColor: c.brand, borderRadius: 6, maxBarThickness: 40 }],
    },
    options: self.baseOptions({
      plugins: { legend: { display: false } },
      scales: {
        x: { grid: { display: false }, ticks: { color: c.muted, font: { family: 'Inter, sans-serif', size: 11 } } },
        y: { beginAtZero: true, grid: { color: c.grid, drawBorder: false },
             ticks: { color: c.muted, precision: 0, font: { family: 'IBM Plex Mono, monospace', size: 11 } } },
      },
    }),
  }));
}

init().catch(err => {
  if (err && err.status !== 401) toastError(err.message || 'Could not load the dashboard');
});
