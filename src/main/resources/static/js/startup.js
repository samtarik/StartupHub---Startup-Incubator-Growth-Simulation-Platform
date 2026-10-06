/* Startup detail page.

   Eight tabs over one startup. Each tab loads on first open, which also means
   charts are only built once their canvas is actually visible and has a size.

   Owner-only controls are hidden for everyone else, but the server checks
   ownership again in AccessGuard - the hiding is convenience, not security. */

const startupId = new URLSearchParams(window.location.search).get('id');
let currentUser = null;
let startup = null;
let isOwner = false;
const loadedTabs = new Set();

/* ---- Boot --------------------------------------------------------------- */
async function init() {
  if (!startupId) { window.location.replace('/dashboard.html'); return; }

  currentUser = await Api.get('/api/auth/me');
  renderShell(currentUser);
  connectNotifications(currentUser.id, () => {
    // Repaint only if Overview is the tab on screen; otherwise just drop the
    // cache so it refreshes the next time the user opens it.
    if (activeTabKey() === 'overview') reloadTab('overview');
    else loadedTabs.delete('overview');
    // MVP notifications (feature completed / launched) also change the MVP tab.
    if (activeTabKey() === 'mvp') reloadTab('mvp');
    else loadedTabs.delete('mvp');
  });

  startup = await Api.get(`/api/startups/${startupId}`);
  isOwner = currentUser.role === 'ADMIN' || startup.founderId === currentUser.id;

  renderHeader();
  applyOwnerVisibility();
  wireTabs();
  wireButtons();

  await loadTab('overview');
}

function renderHeader() {
  document.title = `${startup.name} · StartupHub`;
  document.getElementById('startup-name').textContent = startup.name;
  document.getElementById('startup-stage').innerHTML = stageBadge(startup.stage);
  document.getElementById('startup-desc').textContent = startup.description || 'No description yet.';

  const actions = document.getElementById('startup-actions');
  if (!isOwner) {
    actions.innerHTML = `<span class="badge badge-neutral">Read only</span>`;
    return;
  }
  const atLastStage = startup.stage === 'SCALE';
  actions.innerHTML = `
    <button class="btn btn-outline" id="edit-startup-btn">Edit startup</button>
    <button class="btn btn-outline" id="advance-btn" ${atLastStage ? 'disabled' : ''}>
      ${atLastStage ? 'Final stage reached' : 'Advance stage'}
    </button>
    <button class="btn btn-ghost" id="delete-btn">Delete startup</button>`;

  const advance = document.getElementById('advance-btn');
  if (advance && !atLastStage) advance.addEventListener('click', advanceStage);
  document.getElementById('delete-btn').addEventListener('click', deleteStartup);
  document.getElementById('edit-startup-btn').addEventListener('click', openEditStartupModal);
}

function openEditStartupModal() {
  openFormModal({
    title: 'Edit startup',
    subtitle: 'The stage moves with "Advance stage"; valuation also grows automatically with investments.',
    submitLabel: 'Save changes',
    fields: [
      { name: 'name', label: 'Startup name', type: 'text', required: true, colspan: 'full', value: startup.name },
      { name: 'description', label: 'Description', type: 'textarea', colspan: 'full', rows: 3, value: startup.description || '' },
      { name: 'valuation', label: 'Valuation (USD)', type: 'number', required: true, min: 0, step: '0.01',
        colspan: 'full', value: startup.valuation },
    ],
    onSubmit: async (values) => {
      startup = await Api.put(`/api/startups/${startupId}`, values);
      toastSuccess('Startup updated successfully.');
      renderHeader();
      applyOwnerVisibility();
      invalidate('overview');
      if (activeTabKey() !== 'overview') loadedTabs.delete('overview');
    },
  });
}

/* ---- Shared Edit / Delete helpers ---------------------------------------
   Every table row uses the same two buttons and the same confirm-then-delete flow. */
function rowActions(id, extra = '') {
  return `<span class="row-actions">${extra}
    <button class="btn btn-sm btn-outline" data-edit="${id}">Edit</button>
    <button class="btn btn-sm btn-ghost" data-delete="${id}">Delete</button></span>`;
}

/* Wires [data-edit] / [data-delete] buttons inside `holder` to handlers that receive the item. */
function wireRowActions(holder, items, { onEdit, onDelete }) {
  const byId = new Map(items.map(i => [String(i.id), i]));
  holder.querySelectorAll('[data-edit]').forEach(btn =>
    btn.addEventListener('click', () => { const i = byId.get(btn.dataset.edit); if (i && onEdit) onEdit(i); }));
  holder.querySelectorAll('[data-delete]').forEach(btn =>
    btn.addEventListener('click', () => { const i = byId.get(btn.dataset.delete); if (i && onDelete) onDelete(i); }));
}

async function confirmAndDelete({ title, message, confirmLabel = 'Delete', url, success, refresh }) {
  const ok = await confirmDialog({ title, message, confirmLabel });
  if (!ok) return;
  try {
    await Api.del(url);
    toastSuccess(success);
    await refresh();
  } catch (err) {
    toastError(err.message || 'Could not delete this item.');
  }
}

/* Hides every control marked owner-only from non-owners. */
function applyOwnerVisibility() {
  document.querySelectorAll('.owner-only').forEach(el => el.classList.toggle('hidden', !isOwner));
}

function wireTabs() {
  document.querySelectorAll('.tab').forEach(tab => {
    tab.addEventListener('click', () => {
      document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
      document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
      tab.classList.add('active');
      document.getElementById('panel-' + tab.dataset.tab).classList.add('active');
      loadTab(tab.dataset.tab);
    });
  });
}

const TAB_LOADERS = {
  overview: loadOverview,
  team: loadTeam,
  finance: loadFinance,
  investments: loadInvestments,
  growth: loadGrowth,
  mvp: loadMvp,
  roadmap: loadRoadmap,
  competitors: loadCompetitors,
};

function activeTabKey() {
  const active = document.querySelector('.tab.active');
  return active ? active.dataset.tab : 'overview';
}

async function loadTab(key) {
  if (loadedTabs.has(key)) return;
  loadedTabs.add(key);
  try {
    await TAB_LOADERS[key]();
  } catch (err) {
    loadedTabs.delete(key);
    if (err.status !== 401) toastError(err.message || `Could not load ${key}`);
  }
}

function reloadTab(key) {
  loadedTabs.delete(key);
  return loadTab(key);
}

/* Anything that changes money or headcount also changes the overview. */
function invalidate(...keys) {
  keys.forEach(k => loadedTabs.delete(k));
  const activeKey = activeTabKey();
  if (keys.includes(activeKey)) loadTab(activeKey);
}

/* ---- Header actions ----------------------------------------------------- */
async function advanceStage() {
  const next = STAGES[STAGES.indexOf(startup.stage) + 1];
  const ok = await confirmDialog({
    title: `Move ${startup.name} to ${Fmt.titleCase(next)}?`,
    message: 'This marks the current stage complete. You can keep advancing later, but you cannot go back.',
    confirmLabel: `Move to ${Fmt.titleCase(next)}`,
    danger: false,
    icon: '🚀',
  });
  if (!ok) return;

  const btn = document.getElementById('advance-btn');
  setButtonLoading(btn, true, 'Advancing…');
  try {
    startup = await Api.post(`/api/startups/${startupId}/advance-stage`);
    toastSuccess('Stage advanced', `${startup.name} is now at ${Fmt.titleCase(startup.stage)}.`);
    renderHeader();
    applyOwnerVisibility();
    await reloadTab('overview');
  } catch (err) {
    toastError(err.message || 'Could not advance the stage');
    setButtonLoading(btn, false);
  }
}

async function deleteStartup() {
  const ok = await confirmDialog({
    title: `Delete ${startup.name}?`,
    message: 'This removes the startup along with its team, finances, investments, campaigns and roadmaps. It cannot be undone.',
    confirmLabel: 'Delete permanently',
  });
  if (!ok) return;

  const btn = document.getElementById('delete-btn');
  setButtonLoading(btn, true, 'Deleting…');
  try {
    await Api.del(`/api/startups/${startupId}`);
    toastSuccess('Startup deleted');
    window.location.href = '/dashboard.html#startups';
  } catch (err) {
    toastError(err.message || 'Could not delete the startup');
    setButtonLoading(btn, false);
  }
}

/* ---- Helpers ------------------------------------------------------------ */
/* Finance rows carry a date, so the monthly series is grouped on the client. */
function groupByMonth(rows, months = 6) {
  const buckets = [];
  const now = new Date();
  for (let i = months - 1; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    buckets.push({
      key: `${d.getFullYear()}-${d.getMonth()}`,
      label: d.toLocaleString(undefined, { month: 'short' }),
      total: 0,
    });
  }
  const index = new Map(buckets.map(b => [b.key, b]));
  rows.forEach(r => {
    const d = new Date(r.recordedOn);
    if (isNaN(d)) return;
    const bucket = index.get(`${d.getFullYear()}-${d.getMonth()}`);
    if (bucket) bucket.total += r.amount;
  });
  return buckets;
}

function statRow(label, value, extra = '') {
  return `
    <div class="factor">
      <span class="fl" style="width:auto; flex:1;">${escapeHtml(label)}</span>
      <span class="fs mono" style="color:var(--ink); font-size:0.88rem;">${value}</span>
      ${extra}
    </div>`;
}

/* ---- Overview ----------------------------------------------------------- */
async function loadOverview() {
  document.getElementById('overview-kpis').innerHTML = skeletonCards(4);
  document.getElementById('health-holder').innerHTML = skeletonRows(5, 18);
  document.getElementById('glance-holder').innerHTML = skeletonRows(5, 18);

  const [analytics, health, finance] = await Promise.all([
    Api.get(`/api/startups/${startupId}/analytics`),
    Api.get(`/api/startups/${startupId}/analytics/health`),
    Api.get(`/api/startups/${startupId}/finance`),
  ]);

  const net = analytics.profit;
  document.getElementById('overview-kpis').innerHTML = [
    kpiCard({ label: 'Valuation', value: Fmt.money(analytics.valuation), icon: '📈', accent: 'brand',
      foot: analytics.totalInvested ? `${Fmt.money(analytics.totalInvested)} raised` : 'No funding yet' }),
    kpiCard({ label: 'Revenue', value: Fmt.money(analytics.totalRevenue), icon: '💵', accent: 'growth',
      foot: `${finance.revenues.length} entr${finance.revenues.length === 1 ? 'y' : 'ies'}` }),
    kpiCard({ label: 'Expenses', value: Fmt.money(analytics.totalExpense), icon: '🔥', accent: 'burn',
      foot: `${finance.expenses.length} entr${finance.expenses.length === 1 ? 'y' : 'ies'}` }),
    kpiCard({ label: net >= 0 ? 'Net position' : 'Burn', value: Fmt.money(Math.abs(net)),
      icon: net >= 0 ? '✅' : '⚠', accent: net >= 0 ? 'growth' : 'burn',
      foot: net >= 0 ? 'Revenue is ahead of spend' : 'Spending more than it earns' }),
  ].join('');

  document.getElementById('stage-timeline').innerHTML = stageTimelineHtml(startup.stage);
  renderHealth(health);

  document.getElementById('glance-holder').innerHTML =
    statRow('Team members', Fmt.number(analytics.employeeCount)) +
    statRow('Monthly payroll', Fmt.money(analytics.payroll)) +
    statRow('Investors', Fmt.number(analytics.investorCount)) +
    statRow('Campaigns', Fmt.number(analytics.campaignCount)) +
    statRow('Milestones done', `${analytics.milestonesDone}/${analytics.milestoneCount}`) +
    statRow('Competitors tracked', Fmt.number(analytics.competitorCount));

  renderOverviewTrend(finance);
  renderMvpOverview(analytics);
  wireProjectionForm();
}

function renderHealth(health) {
  const colour = health.score >= 70 ? 'var(--growth)' : health.score >= 45 ? 'var(--burn)' : 'var(--danger)';
  const icons = { GOOD: '●', WARN: '●', BAD: '●' };

  document.getElementById('health-holder').innerHTML = `
    <div class="health-top">
      <div class="score-ring" style="--pct:${health.score}; --ring-color:${colour};" role="img"
           aria-label="Health score ${health.score} out of 100">
        <span class="inner">
          <span class="num">${health.score}</span>
          <span class="den">out of 100</span>
        </span>
      </div>
      <div>
        <div style="font-weight:600; font-size:1.02rem;">${escapeHtml(health.grade)}</div>
        <p class="muted" style="margin:2px 0 0; font-size:0.84rem;">
          Scored from finance, team, roadmap, funding, growth and MVP.
        </p>
      </div>
    </div>
    <div class="factor-list">
      ${health.factors.map(f => `
        <div class="factor ${f.status}">
          <span class="fi" aria-hidden="true">${icons[f.status]}</span>
          <span class="fl">${escapeHtml(f.label)}</span>
          <span class="fd">${escapeHtml(f.detail)}</span>
          <span class="fs">${f.score}/${f.max}</span>
        </div>`).join('')}
    </div>`;
}

function renderOverviewTrend(finance) {
  const holder = document.getElementById('overview-trend-holder');
  if (!finance.revenues.length && !finance.expenses.length) {
    holder.innerHTML = emptyState({
      icon: '📊',
      title: 'No finance entries yet',
      description: isOwner ? 'Add revenue or an expense on the Finance tab and this chart fills in.'
                           : 'The founder has not recorded any finance entries yet.',
    });
    return;
  }
  const revenue = groupByMonth(finance.revenues);
  const expense = groupByMonth(finance.expenses);
  holder.innerHTML = '<canvas id="chart-overview-trend"></canvas>';
  Charts.revenueVsExpense('chart-overview-trend',
    revenue.map(b => b.label), revenue.map(b => b.total), expense.map(b => b.total));
}

function wireProjectionForm() {
  const form = document.getElementById('projection-form');
  if (form.dataset.wired) return;
  form.dataset.wired = '1';

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = document.getElementById('proj-btn');
    if (btn.disabled) return;

    form.querySelectorAll('.field.invalid').forEach(el => el.classList.remove('invalid'));
    const base = document.getElementById('proj-base');
    const customers = document.getElementById('proj-customers');

    let bad = false;
    if (base.value === '' || Number(base.value) < 0) { attachFieldError(form, 'baseAmount', 'Enter a base amount of 0 or more'); bad = true; }
    if (customers.value === '' || Number(customers.value) < 0) { attachFieldError(form, 'customers', 'Enter a customer count of 0 or more'); bad = true; }
    if (bad) return;

    setButtonLoading(btn, true, 'Calculating…');
    try {
      const result = await Api.post(`/api/startups/${startupId}/analytics/project`, {
        strategy: document.getElementById('proj-strategy').value,
        baseAmount: Number(base.value),
        customers: Number(customers.value),
      });
      const label = document.querySelector('#proj-strategy option:checked').textContent;
      document.getElementById('projection-result').innerHTML =
        `<div class="alert alert-info" style="margin:14px 0 0;">
           Projected monthly revenue with the <strong>${escapeHtml(label)}</strong> model:
           <strong class="mono">${Fmt.money(result.projection, { compact: false })}</strong>
         </div>`;
    } catch (err) {
      toastError(err.message || 'Could not calculate the projection');
    } finally {
      setButtonLoading(btn, false);
    }
  });
}

/* ---- Team --------------------------------------------------------------- */
async function loadTeam() {
  const holder = document.getElementById('team-holder');
  holder.innerHTML = skeletonRows(3, 40);

  const team = await Api.get(`/api/startups/${startupId}/employees`);
  const payroll = team.reduce((sum, e) => sum + e.salary, 0);
  const avg = team.length ? Math.round(team.reduce((s, e) => s + e.performanceScore, 0) / team.length) : 0;

  document.getElementById('team-kpis').innerHTML = [
    kpiCard({ label: 'Headcount', value: Fmt.number(team.length), icon: '👥', accent: 'brand' }),
    kpiCard({ label: 'Monthly payroll', value: Fmt.money(payroll), icon: '💸', accent: 'burn' }),
    kpiCard({ label: 'Average performance', value: `${avg}/100`, icon: '⭐', accent: 'growth',
      foot: team.length ? '' : 'Hire someone to start tracking' }),
  ].join('');

  if (!team.length) {
    holder.innerHTML = emptyState({
      icon: '👥',
      title: 'No one on the team yet',
      description: isOwner ? 'Add your first team member to start tracking payroll and performance.'
                           : 'The founder has not added any team members yet.',
      actionLabel: isOwner ? '+ Add member' : '',
      actionId: 'empty-add-employee',
    });
    const btn = document.getElementById('empty-add-employee');
    if (btn) btn.addEventListener('click', openEmployeeModal);
    return;
  }

  holder.innerHTML = `<div class="table-wrap"><table>
      <thead><tr><th>Name</th><th>Role</th><th>Salary</th><th>Performance</th>${isOwner ? '<th></th>' : ''}</tr></thead>
      <tbody>` + team.map(e => `
        <tr>
          <td><strong>${escapeHtml(e.name)}</strong></td>
          <td>${escapeHtml(e.jobTitle)}</td>
          <td class="mono">${Fmt.money(e.salary, { compact: false })}</td>
          <td style="min-width:140px;">
            <div class="progress-row"><span class="mono">${e.performanceScore}/100</span></div>
            <div class="progress ${e.performanceScore >= 70 ? 'growth' : e.performanceScore >= 40 ? '' : 'burn'}">
              <span style="width:${e.performanceScore}%"></span>
            </div>
          </td>
          ${isOwner ? `<td class="actions">
            <button class="btn btn-sm btn-outline" data-promote="${e.id}">Promote</button>
            <button class="btn btn-sm btn-outline" data-edit="${e.id}">Edit</button>
            <button class="btn btn-sm btn-ghost" data-fire="${e.id}" data-name="${escapeHtml(e.name)}">Remove</button>
          </td>` : ''}
        </tr>`).join('') + `</tbody></table></div>`;

  wireRowActions(holder, team, { onEdit: openEditEmployeeModal });

  holder.querySelectorAll('[data-promote]').forEach(btn => {
    btn.addEventListener('click', async () => {
      setButtonLoading(btn, true, '…');
      try {
        await Api.post(`/api/employees/${btn.dataset.promote}/promote`);
        toastSuccess('Performance score raised by 10');
        invalidate('overview');
        await reloadTab('team');
      } catch (err) {
        toastError(err.message || 'Could not promote');
        setButtonLoading(btn, false);
      }
    });
  });

  holder.querySelectorAll('[data-fire]').forEach(btn => {
    btn.addEventListener('click', async () => {
      const ok = await confirmDialog({
        title: `Remove ${btn.dataset.name}?`,
        message: 'They will be taken off the team and out of the payroll total.',
        confirmLabel: 'Remove from team',
      });
      if (!ok) return;
      try {
        await Api.del(`/api/employees/${btn.dataset.fire}`);
        toastSuccess('Team member removed');
        invalidate('overview');
        await reloadTab('team');
      } catch (err) {
        toastError(err.message || 'Could not remove them');
      }
    });
  });
}

function openEmployeeModal() {
  openFormModal({
    title: 'Add a team member',
    subtitle: 'New hires start at a performance score of 50.',
    submitLabel: 'Add member',
    fields: [
      { name: 'name', label: 'Full name', type: 'text', required: true, placeholder: 'Sara Rahman' },
      { name: 'jobTitle', label: 'Job title', type: 'text', required: true, placeholder: 'Backend engineer' },
      { name: 'salary', label: 'Monthly salary (USD)', type: 'number', required: true, min: 0, step: '0.01',
        colspan: 'full', placeholder: '2500' },
    ],
    onSubmit: async (values) => {
      await Api.post(`/api/startups/${startupId}/employees`, values);
      toastSuccess('Team member added', `${values.name} joined the team.`);
      invalidate('overview');
      await reloadTab('team');
    },
  });
}

function openEditEmployeeModal(e) {
  openFormModal({
    title: `Edit ${e.name}`,
    submitLabel: 'Save changes',
    fields: [
      { name: 'name', label: 'Full name', type: 'text', required: true, value: e.name },
      { name: 'jobTitle', label: 'Job title', type: 'text', required: true, value: e.jobTitle },
      { name: 'salary', label: 'Monthly salary (USD)', type: 'number', required: true, min: 0, step: '0.01', value: e.salary },
      { name: 'performanceScore', label: 'Performance (0-100)', type: 'number', required: true, min: 0, max: 100, step: '1',
        value: e.performanceScore },
    ],
    onSubmit: async (values) => {
      await Api.put(`/api/employees/${e.id}`, values);
      toastSuccess('Employee updated successfully.');
      invalidate('overview');
      await reloadTab('team');
    },
  });
}

/* ---- Finance ------------------------------------------------------------ */
async function loadFinance() {
  document.getElementById('revenue-holder').innerHTML = skeletonRows(3, 32);
  document.getElementById('expense-holder').innerHTML = skeletonRows(3, 32);

  const finance = await Api.get(`/api/startups/${startupId}/finance`);
  const net = finance.netPosition;

  document.getElementById('finance-kpis').innerHTML = [
    kpiCard({ label: 'Total revenue', value: Fmt.money(finance.totalRevenue), icon: '💵', accent: 'growth' }),
    kpiCard({ label: 'Total expenses', value: Fmt.money(finance.totalExpense), icon: '🔥', accent: 'burn' }),
    kpiCard({ label: net >= 0 ? 'Net position' : 'Net loss', value: Fmt.money(Math.abs(net)),
      icon: net >= 0 ? '✅' : '⚠', accent: net >= 0 ? 'growth' : 'burn' }),
    kpiCard({ label: 'Burn rate', value: Fmt.money(Math.max(0, finance.burnRate)), icon: '📉', accent: 'burn',
      foot: finance.burnRate > 0 ? 'Spending above revenue' : 'Not burning cash' }),
  ].join('');

  renderFinanceCharts(finance);
  renderFinanceTables(finance);
  initRevenueCalculator();
}

function renderFinanceCharts(finance) {
  const trendHolder = document.getElementById('finance-trend-holder');
  const breakdownHolder = document.getElementById('finance-breakdown-holder');

  if (!finance.revenues.length && !finance.expenses.length) {
    const empty = emptyState({ icon: '📊', title: 'Nothing to chart yet',
      description: 'Add a revenue or expense entry and the charts appear.' });
    trendHolder.innerHTML = empty;
    breakdownHolder.innerHTML = empty;
    return;
  }

  const revenue = groupByMonth(finance.revenues);
  const expense = groupByMonth(finance.expenses);
  trendHolder.innerHTML = '<canvas id="chart-finance-trend"></canvas>';
  Charts.revenueVsExpense('chart-finance-trend',
    revenue.map(b => b.label), revenue.map(b => b.total), expense.map(b => b.total));

  if (!finance.expenses.length) {
    breakdownHolder.innerHTML = emptyState({ icon: '🔥', title: 'No expenses recorded',
      description: 'Categories appear here once you log spending.' });
    return;
  }
  const byCategory = {};
  finance.expenses.forEach(e => {
    const key = e.category || 'Uncategorised';
    byCategory[key] = (byCategory[key] || 0) + e.amount;
  });
  const sorted = Object.entries(byCategory).sort((a, b) => b[1] - a[1]).slice(0, 8);
  breakdownHolder.innerHTML = '<canvas id="chart-finance-breakdown"></canvas>';
  Charts.expenseBreakdown('chart-finance-breakdown', sorted.map(e => e[0]), sorted.map(e => e[1]));
}

function renderFinanceTables(finance) {
  const revHolder = document.getElementById('revenue-holder');
  const expHolder = document.getElementById('expense-holder');

  const rows = (items, labelKey, kind) => items
    .slice()
    .sort((a, b) => new Date(b.recordedOn) - new Date(a.recordedOn))
    .map(i => `
      <tr>
        <td class="mono">${Fmt.money(i.amount, { compact: false })}</td>
        <td>${escapeHtml(i[labelKey] || '—')}${i.model
          ? `<span class="badge badge-calc" title="${escapeHtml(i.details || '')}">Calculated</span>` : ''}</td>
        <td class="subtle nowrap">${Fmt.date(i.recordedOn)}</td>
        ${isOwner ? `<td class="actions">${rowActions(i.id)}</td>` : ''}
      </tr>`).join('');
  const actionHead = isOwner ? '<th></th>' : '';

  revHolder.innerHTML = finance.revenues.length
    ? `<div class="table-wrap"><table><thead><tr><th>Amount</th><th>Source</th><th>Date</th>${actionHead}</tr></thead>
       <tbody>${rows(finance.revenues, 'source', 'revenue')}</tbody></table></div>`
    : emptyState({ icon: '💵', title: 'No revenue recorded',
        description: isOwner ? 'Log your first sale or contract, or use the calculator above.' : 'Nothing recorded yet.',
        actionLabel: isOwner ? '+ Add revenue' : '', actionId: 'empty-add-revenue' });

  expHolder.innerHTML = finance.expenses.length
    ? `<div class="table-wrap"><table><thead><tr><th>Amount</th><th>Category</th><th>Date</th>${actionHead}</tr></thead>
       <tbody>${rows(finance.expenses, 'category', 'expense')}</tbody></table></div>`
    : emptyState({ icon: '🔥', title: 'No expenses recorded',
        description: isOwner ? 'Log salaries, tools or marketing spend.' : 'Nothing recorded yet.',
        actionLabel: isOwner ? '+ Add expense' : '', actionId: 'empty-add-expense' });

  wireRowActions(revHolder, finance.revenues, { onEdit: openEditRevenueModal, onDelete: deleteRevenue });
  wireRowActions(expHolder, finance.expenses, { onEdit: openEditExpenseModal, onDelete: deleteExpense });

  const r = document.getElementById('empty-add-revenue');
  if (r) r.addEventListener('click', openRevenueModal);
  const e = document.getElementById('empty-add-expense');
  if (e) e.addEventListener('click', openExpenseModal);
}

function openRevenueModal() {
  openFormModal({
    title: 'Record revenue',
    subtitle: 'Dated today, which is what the monthly charts group on.',
    submitLabel: 'Add revenue',
    fields: [
      { name: 'amount', label: 'Amount (USD)', type: 'number', required: true, min: 0.01, step: '0.01', placeholder: '5000' },
      { name: 'source', label: 'Source', type: 'text', required: true, placeholder: 'Subscription sales' },
    ],
    onSubmit: async (values) => {
      await Api.post(`/api/startups/${startupId}/finance/revenue`, values);
      toastSuccess('Revenue recorded');
      invalidate('overview');
      await reloadTab('finance');
    },
  });
}

function openEditRevenueModal(r) {
  openFormModal({
    title: 'Edit revenue',
    subtitle: r.model ? 'This record came from the calculator. Changing the amount marks it as manually adjusted.' : '',
    submitLabel: 'Save changes',
    fields: [
      { name: 'amount', label: 'Amount (USD)', type: 'number', required: true, min: 0.01, step: '0.01', value: r.amount },
      { name: 'source', label: 'Source', type: 'text', required: true, value: r.source || '' },
      { name: 'recordedOn', label: 'Date', type: 'date', required: true, colspan: 'full', value: r.recordedOn || '' },
    ],
    onSubmit: async (values) => {
      await Api.put(`/api/startups/${startupId}/finance/revenue/${r.id}`, values);
      toastSuccess('Revenue updated successfully.');
      invalidate('overview');
      await reloadTab('finance');
    },
  });
}

function openEditExpenseModal(e) {
  openFormModal({
    title: 'Edit expense',
    submitLabel: 'Save changes',
    fields: [
      { name: 'amount', label: 'Amount (USD)', type: 'number', required: true, min: 0.01, step: '0.01', value: e.amount },
      { name: 'category', label: 'Category', type: 'text', required: true, value: e.category || '' },
      { name: 'recordedOn', label: 'Date', type: 'date', required: true, colspan: 'full', value: e.recordedOn || '' },
    ],
    onSubmit: async (values) => {
      await Api.put(`/api/startups/${startupId}/finance/expense/${e.id}`, values);
      toastSuccess('Expense updated successfully.');
      invalidate('overview');
      await reloadTab('finance');
    },
  });
}

function deleteRevenue(r) {
  return confirmAndDelete({
    title: 'Delete this revenue record?',
    message: `${Fmt.money(r.amount, { compact: false })} from "${r.source}" will be removed from your totals and charts.`,
    confirmLabel: 'Delete revenue',
    url: `/api/startups/${startupId}/finance/revenue/${r.id}`,
    success: 'Revenue deleted.',
    refresh: async () => { invalidate('overview'); await reloadTab('finance'); },
  });
}

function deleteExpense(e) {
  return confirmAndDelete({
    title: 'Delete this expense?',
    message: `${Fmt.money(e.amount, { compact: false })} for "${e.category}" will be removed from your totals and charts.`,
    confirmLabel: 'Delete expense',
    url: `/api/startups/${startupId}/finance/expense/${e.id}`,
    success: 'Expense deleted.',
    refresh: async () => { invalidate('overview'); await reloadTab('finance'); },
  });
}

function openExpenseModal() {
  openFormModal({
    title: 'Record an expense',
    subtitle: 'Use consistent categories so the breakdown chart stays readable.',
    submitLabel: 'Add expense',
    fields: [
      { name: 'amount', label: 'Amount (USD)', type: 'number', required: true, min: 0.01, step: '0.01', placeholder: '1200' },
      { name: 'category', label: 'Category', type: 'text', required: true, placeholder: 'Salaries' },
    ],
    onSubmit: async (values) => {
      await Api.post(`/api/startups/${startupId}/finance/expense`, values);
      toastSuccess('Expense recorded');
      invalidate('overview');
      await reloadTab('finance');
    },
  });
}

/* ---- Investments -------------------------------------------------------- */
async function loadInvestments() {
  const holder = document.getElementById('investments-holder');
  holder.innerHTML = skeletonRows(3, 32);

  const data = await Api.get(`/api/startups/${startupId}/investments`);
  const items = data.investments;
  const average = items.length ? data.totalRaised / items.length : 0;

  document.getElementById('investment-kpis').innerHTML = [
    kpiCard({ label: 'Total raised', value: Fmt.money(data.totalRaised), icon: '💰', accent: 'growth' }),
    kpiCard({ label: 'Investors', value: Fmt.number(data.investorCount), icon: '🤝', accent: 'brand' }),
    kpiCard({ label: 'Average cheque', value: Fmt.money(average), icon: '📊', accent: 'brand' }),
  ].join('');

  if (!items.length) {
    holder.innerHTML = emptyState({
      icon: '💰',
      title: 'No investment yet',
      description: 'Any signed-in user can back this startup — the founder gets a live notification when they do.',
      actionLabel: '+ Record investment',
      actionId: 'empty-add-investment',
    });
    document.getElementById('investment-chart-holder').innerHTML =
      emptyState({ icon: '🥧', title: 'Nothing to break down yet' });
    const btn = document.getElementById('empty-add-investment');
    if (btn) btn.addEventListener('click', openInvestmentModal);
    return;
  }

  holder.innerHTML = `<div class="table-wrap"><table>
      <thead><tr><th>Investor</th><th>Type</th><th>Amount</th><th>Date</th><th></th></tr></thead>
      <tbody>` + items
        .slice().sort((a, b) => new Date(b.investedOn) - new Date(a.investedOn))
        .map(i => `
        <tr>
          <td><strong>${escapeHtml(i.investorName)}</strong></td>
          <td><span class="badge badge-neutral">${Fmt.titleCase(i.type)}</span></td>
          <td class="mono">${Fmt.money(i.amount, { compact: false })}</td>
          <td class="subtle nowrap">${Fmt.date(i.investedOn)}</td>
          <td class="actions">${canManageInvestment(i) ? rowActions(i.id) : ''}</td>
        </tr>`).join('') + `</tbody></table></div>`;

  wireRowActions(holder, items, { onEdit: openEditInvestmentModal, onDelete: deleteInvestment });

  const byType = {};
  items.forEach(i => { byType[i.type] = (byType[i.type] || 0) + i.amount; });
  document.getElementById('investment-chart-holder').innerHTML = '<canvas id="chart-investments"></canvas>';
  Charts.investmentMix('chart-investments', Object.keys(byType).map(Fmt.titleCase), Object.values(byType));
}

/* The founder (or admin) and the investor who made the investment may change it; the server checks again. */
function canManageInvestment(i) {
  return isOwner || i.investorId === currentUser.id;
}

async function refreshAfterInvestmentChange() {
  startup = await Api.get(`/api/startups/${startupId}`);
  renderHeader();
  applyOwnerVisibility();
  invalidate('overview');
  await reloadTab('investments');
}

function openEditInvestmentModal(inv) {
  openFormModal({
    title: 'Edit investment',
    subtitle: `Investor: ${inv.investorName}. The startup valuation is adjusted by the change in amount.`,
    submitLabel: 'Save changes',
    fields: [
      { name: 'type', label: 'Funding type', type: 'select', required: true, value: inv.type, options: [
        { value: 'ANGEL', label: 'Angel' },
        { value: 'VENTURE_CAPITAL', label: 'Venture capital' },
        { value: 'CROWDFUNDING', label: 'Crowdfunding' },
      ] },
      { name: 'amount', label: 'Amount (USD)', type: 'number', required: true, min: 0.01, step: '0.01', value: inv.amount },
      { name: 'investedOn', label: 'Date', type: 'date', required: true, colspan: 'full', value: inv.investedOn || '' },
    ],
    onSubmit: async (values) => {
      await Api.put(`/api/investments/${inv.id}`, values);
      toastSuccess('Investment updated successfully.');
      await refreshAfterInvestmentChange();
    },
  });
}

function deleteInvestment(inv) {
  return confirmAndDelete({
    title: 'Delete this investment?',
    message: `${Fmt.money(inv.amount, { compact: false })} from ${inv.investorName} will be removed and the valuation reduced by the same amount.`,
    confirmLabel: 'Delete investment',
    url: `/api/investments/${inv.id}`,
    success: 'Investment deleted.',
    refresh: refreshAfterInvestmentChange,
  });
}

function openInvestmentModal() {
  openFormModal({
    title: `Invest in ${startup.name}`,
    subtitle: 'Recorded against your account and pushed to the founder instantly.',
    submitLabel: 'Record investment',
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
      toastSuccess('Investment recorded', 'Valuation updated.');
      startup = await Api.get(`/api/startups/${startupId}`);
      renderHeader();
      applyOwnerVisibility();
      invalidate('overview');
      await reloadTab('investments');
    },
  });
}

/* ---- Growth (campaigns) -------------------------------------------------- */
async function loadGrowth() {
  const holder = document.getElementById('campaigns-holder');
  holder.innerHTML = skeletonRows(3, 32);

  const campaigns = await Api.get(`/api/startups/${startupId}/campaigns`);
  const budget = campaigns.reduce((s, c) => s + c.budget, 0);
  const conversions = campaigns.reduce((s, c) => s + c.conversions, 0);
  const measured = campaigns.filter(c => c.reach > 0);
  const avgRate = measured.length ? measured.reduce((s, c) => s + c.conversionRate, 0) / measured.length : 0;

  document.getElementById('growth-kpis').innerHTML = [
    kpiCard({ label: 'Campaign spend', value: Fmt.money(budget), icon: '📣', accent: 'brand',
      foot: `${campaigns.length} campaign${campaigns.length === 1 ? '' : 's'}` }),
    kpiCard({ label: 'Conversions', value: Fmt.number(conversions), icon: '🎯', accent: 'growth' }),
    kpiCard({ label: 'Average conversion rate', value: measured.length ? Fmt.percent(avgRate) : '—',
      icon: '📈', accent: 'growth',
      foot: measured.length ? `From ${measured.length} measured campaign${measured.length === 1 ? '' : 's'}`
                            : 'Record results to see this' }),
  ].join('');

  const chartHolder = document.getElementById('campaign-chart-holder');
  if (!campaigns.length) {
    chartHolder.innerHTML = emptyState({ icon: '📣', title: 'No campaigns yet' });
    holder.innerHTML = emptyState({
      icon: '📣',
      title: 'No marketing campaigns',
      description: isOwner ? 'Launch a campaign, then record its reach and conversions to see how it performed.'
                           : 'The founder has not launched any campaigns yet.',
      actionLabel: isOwner ? '+ New campaign' : '',
      actionId: 'empty-add-campaign',
    });
    const btn = document.getElementById('empty-add-campaign');
    if (btn) btn.addEventListener('click', openCampaignModal);
    return;
  }

  chartHolder.innerHTML = '<canvas id="chart-campaigns"></canvas>';
  Charts.campaignPerformance('chart-campaigns',
    campaigns.map(c => c.name), campaigns.map(c => c.budget), campaigns.map(c => c.conversions));

  holder.innerHTML = `<div class="table-wrap"><table>
      <thead><tr><th>Campaign</th><th>Budget</th><th>Reach</th><th>Conversions</th><th>Rate</th>${isOwner ? '<th></th>' : ''}</tr></thead>
      <tbody>` + campaigns.map(c => `
        <tr>
          <td><strong>${escapeHtml(c.name)}</strong></td>
          <td class="mono">${Fmt.money(c.budget, { compact: false })}</td>
          <td class="mono">${c.reach ? Fmt.number(c.reach) : '<span class="subtle">Not measured</span>'}</td>
          <td class="mono">${Fmt.number(c.conversions)}</td>
          <td class="mono">${c.reach ? Fmt.percent(c.conversionRate) : '—'}</td>
          ${isOwner ? `<td class="actions">
            <button class="btn btn-sm btn-outline" data-metrics="${c.id}" data-name="${escapeHtml(c.name)}"
                    data-reach="${c.reach}" data-conv="${c.conversions}">Record results</button>
            ${rowActions(c.id)}
          </td>` : ''}
        </tr>`).join('') + `</tbody></table></div>`;

  wireRowActions(holder, campaigns, { onEdit: openEditCampaignModal, onDelete: deleteCampaign });
  holder.querySelectorAll('[data-metrics]').forEach(btn => {
    btn.addEventListener('click', () => openMetricsModal(btn.dataset));
  });
}

function openCampaignModal() {
  openFormModal({
    title: 'Launch a campaign',
    subtitle: 'Goes through the Command pattern on the server, so it lands in the audit history.',
    submitLabel: 'Launch campaign',
    fields: [
      { name: 'name', label: 'Campaign name', type: 'text', required: true, placeholder: 'Spring launch' },
      { name: 'budget', label: 'Budget (USD)', type: 'number', required: true, min: 0, step: '0.01', placeholder: '1500' },
    ],
    onSubmit: async (values) => {
      await Api.post(`/api/startups/${startupId}/campaigns`, values);
      toastSuccess('Campaign launched');
      invalidate('overview');
      await reloadTab('growth');
    },
  });
}

function openEditCampaignModal(c) {
  openFormModal({
    title: `Edit ${c.name}`,
    subtitle: 'Conversions cannot be higher than reach.',
    submitLabel: 'Save changes',
    fields: [
      { name: 'name', label: 'Campaign name', type: 'text', required: true, colspan: 'full', value: c.name },
      { name: 'budget', label: 'Budget (USD)', type: 'number', required: true, min: 0, step: '0.01', colspan: 'full', value: c.budget },
      { name: 'reach', label: 'People reached', type: 'number', required: true, min: 0, step: '1', value: c.reach },
      { name: 'conversions', label: 'Conversions', type: 'number', required: true, min: 0, step: '1', value: c.conversions },
    ],
    onSubmit: async (values, helpers) => {
      if (values.conversions > values.reach) {
        helpers.setFieldError('conversions', 'Conversions cannot be higher than reach');
        throw new Error('Check the numbers you entered');
      }
      await Api.put(`/api/campaigns/${c.id}`, values);
      toastSuccess('Campaign updated successfully.');
      invalidate('overview');
      await reloadTab('growth');
    },
  });
}

function deleteCampaign(c) {
  return confirmAndDelete({
    title: `Delete ${c.name}?`,
    message: 'The campaign and its recorded results will be removed.',
    confirmLabel: 'Delete campaign',
    url: `/api/campaigns/${c.id}`,
    success: 'Campaign deleted.',
    refresh: async () => { invalidate('overview'); await reloadTab('growth'); },
  });
}

function openMetricsModal(data) {
  openFormModal({
    title: `Results for ${data.name}`,
    subtitle: 'Conversions cannot be higher than reach.',
    submitLabel: 'Save results',
    fields: [
      { name: 'reach', label: 'People reached', type: 'number', required: true, min: 0, step: '1',
        value: data.reach, placeholder: '12000' },
      { name: 'conversions', label: 'Conversions', type: 'number', required: true, min: 0, step: '1',
        value: data.conv, placeholder: '340' },
    ],
    onSubmit: async (values, helpers) => {
      if (values.conversions > values.reach) {
        helpers.setFieldError('conversions', 'Conversions cannot be higher than reach');
        throw new Error('Check the numbers you entered');
      }
      await Api.post(`/api/campaigns/${data.metrics}/metrics`, values);
      toastSuccess('Campaign results saved');
      invalidate('overview');
      await reloadTab('growth');
    },
  });
}

/* ---- Roadmap ------------------------------------------------------------ */
async function loadRoadmap() {
  const holder = document.getElementById('roadmap-holder');
  holder.innerHTML = skeletonRows(3, 60);

  const roadmaps = await Api.get(`/api/startups/${startupId}/roadmaps`);
  const all = roadmaps.flatMap(r => r.milestones);
  const done = all.filter(m => m.status === 'DONE').length;
  const overdue = all.filter(m => m.overdue).length;

  document.getElementById('roadmap-kpis').innerHTML = [
    kpiCard({ label: 'Milestones', value: Fmt.number(all.length), icon: '🗺', accent: 'brand',
      foot: `Across ${roadmaps.length} roadmap${roadmaps.length === 1 ? '' : 's'}` }),
    kpiCard({ label: 'Completed', value: `${done}/${all.length || 0}`, icon: '✅', accent: 'growth',
      foot: all.length ? `${Math.round((done / all.length) * 100)}% done` : 'Nothing planned yet' }),
    kpiCard({ label: 'Overdue', value: Fmt.number(overdue), icon: '⚠',
      accent: overdue ? 'burn' : 'growth',
      foot: overdue ? 'Past the deadline and not done' : 'Nothing is late' }),
  ].join('');

  if (!roadmaps.length) {
    holder.innerHTML = emptyState({
      icon: '🗺',
      title: 'No roadmap yet',
      description: isOwner ? 'Create a roadmap, then break it into milestones with deadlines.'
                           : 'The founder has not published a roadmap yet.',
      actionLabel: isOwner ? '+ New roadmap' : '',
      actionId: 'empty-add-roadmap',
    });
    const btn = document.getElementById('empty-add-roadmap');
    if (btn) btn.addEventListener('click', openRoadmapModal);
    return;
  }

  // Mentors can move milestones along even though they do not own the startup.
  const canEditMilestones = isOwner || currentUser.role === 'MENTOR';

  holder.innerHTML = roadmaps.map(r => {
    const total = r.milestones.length;
    const finished = r.milestones.filter(m => m.status === 'DONE').length;
    const pct = total ? Math.round((finished / total) * 100) : 0;

    return `
      <div class="card" style="margin-bottom:16px;">
        <div class="card-head">
          <div style="min-width:0;">
            <h4>${escapeHtml(r.title)}</h4>
            <p class="muted" style="margin:0; font-size:0.84rem;">${finished} of ${total} milestones complete</p>
          </div>
          ${isOwner ? `<div class="toolbar" style="flex-wrap:nowrap;">
            <button class="btn btn-sm btn-outline" data-add-milestone="${r.id}">+ Milestone</button>
            <button class="btn btn-sm btn-outline" data-edit-roadmap="${r.id}">Edit</button>
            <button class="btn btn-sm btn-ghost" data-delete-roadmap="${r.id}">Delete</button>
          </div>` : ''}
        </div>

        <div style="margin-bottom:16px;">
          <div class="progress-row"><span>Roadmap progress</span><span class="mono">${pct}%</span></div>
          <div class="progress ${pct === 100 ? 'growth' : ''}"><span style="width:${pct}%"></span></div>
        </div>

        ${total ? r.milestones.map(m => milestoneRow(m, canEditMilestones, isOwner)).join('')
                : `<p class="muted" style="margin:0;">No milestones on this roadmap yet.</p>`}
      </div>`;
  }).join('');

  holder.querySelectorAll('[data-add-milestone]').forEach(btn => {
    btn.addEventListener('click', () => openMilestoneModal(btn.dataset.addMilestone));
  });
  holder.querySelectorAll('[data-milestone-done]').forEach(btn => {
    btn.addEventListener('click', () => completeMilestone(btn, btn.dataset.milestoneDone));
  });
  holder.querySelectorAll('[data-milestone-edit]').forEach(btn => {
    btn.addEventListener('click', () => openMilestoneStatusModal(btn.dataset));
  });

  const roadmapById = new Map(roadmaps.map(r => [String(r.id), r]));
  const milestoneById = new Map(all.map(m => [String(m.id), m]));
  holder.querySelectorAll('[data-edit-roadmap]').forEach(btn =>
    btn.addEventListener('click', () => openEditRoadmapModal(roadmapById.get(btn.dataset.editRoadmap))));
  holder.querySelectorAll('[data-delete-roadmap]').forEach(btn =>
    btn.addEventListener('click', () => deleteRoadmap(roadmapById.get(btn.dataset.deleteRoadmap))));
  holder.querySelectorAll('[data-full-edit]').forEach(btn =>
    btn.addEventListener('click', () => openEditMilestoneModal(milestoneById.get(btn.dataset.fullEdit))));
  holder.querySelectorAll('[data-delete-milestone]').forEach(btn =>
    btn.addEventListener('click', () => deleteMilestone(milestoneById.get(btn.dataset.deleteMilestone))));
}

function priorityBadge(priority) {
  if (priority === 'HIGH') return '<span class="badge badge-danger">High</span>';
  if (priority === 'LOW') return '<span class="badge badge-neutral">Low</span>';
  return '<span class="badge badge-brand">Medium</span>';
}

function milestoneRow(m, canEdit, canFullEdit) {
  const progress = m.status === 'DONE' ? 100 : m.progress;
  const bar = m.status === 'DONE' ? 'growth' : m.overdue ? 'danger' : '';
  return `
    <div style="padding:13px 0; border-top:1px solid var(--border);">
      <div class="row-between" style="align-items:flex-start;">
        <div style="min-width:0; flex:1;">
          <div style="display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
            <strong style="font-size:0.94rem;">${escapeHtml(m.title)}</strong>
            ${milestoneStatusBadge(m)}
            ${priorityBadge(m.priority)}
          </div>
          ${m.description ? `<p class="muted" style="margin:5px 0 0; font-size:0.85rem;">${escapeHtml(m.description)}</p>` : ''}
          <div class="subtle" style="font-size:0.79rem; margin-top:5px;">
            ${m.deadline ? `Due ${Fmt.date(m.deadline)}` : 'No deadline set'}
          </div>
        </div>
        ${canEdit ? `<div class="toolbar" style="flex-wrap:nowrap;">
          ${canFullEdit
            ? `<button class="btn btn-sm btn-outline" data-full-edit="${m.id}">Edit</button>`
            : `<button class="btn btn-sm btn-ghost" data-milestone-edit="${m.id}" data-name="${escapeHtml(m.title)}"
                  data-status="${m.status}" data-progress="${m.progress}">Update</button>`}
          ${m.status !== 'DONE' ? `<button class="btn btn-sm btn-success" data-milestone-done="${m.id}">Mark done</button>` : ''}
          ${canFullEdit ? `<button class="btn btn-sm btn-ghost" data-delete-milestone="${m.id}">Delete</button>` : ''}
        </div>` : ''}
      </div>
      <div style="margin-top:10px;">
        <div class="progress ${bar}"><span style="width:${progress}%"></span></div>
      </div>
    </div>`;
}

async function completeMilestone(btn, id) {
  setButtonLoading(btn, true, 'Saving…');
  try {
    await Api.post(`/api/milestones/${id}/complete`);
    toastSuccess('Milestone completed', 'The founder gets a live notification.');
    invalidate('overview');
    await reloadTab('roadmap');
  } catch (err) {
    toastError(err.message || 'Could not complete the milestone');
    setButtonLoading(btn, false);
  }
}

function openEditRoadmapModal(r) {
  openFormModal({
    title: 'Edit roadmap',
    submitLabel: 'Save changes',
    fields: [
      { name: 'title', label: 'Roadmap title', type: 'text', required: true, colspan: 'full', value: r.title },
    ],
    onSubmit: async (values) => {
      await Api.put(`/api/roadmaps/${r.id}`, values);
      toastSuccess('Roadmap updated successfully.');
      await reloadTab('roadmap');
    },
  });
}

function deleteRoadmap(r) {
  return confirmAndDelete({
    title: `Delete "${r.title}"?`,
    message: `The roadmap and its ${r.milestones.length} milestone${r.milestones.length === 1 ? '' : 's'} will be removed.`,
    confirmLabel: 'Delete roadmap',
    url: `/api/roadmaps/${r.id}`,
    success: 'Roadmap deleted.',
    refresh: async () => { invalidate('overview'); await reloadTab('roadmap'); },
  });
}

function openEditMilestoneModal(m) {
  openFormModal({
    title: 'Edit milestone',
    subtitle: 'Marking it done notifies the founder over WebSocket.',
    submitLabel: 'Save changes',
    fields: [
      { name: 'title', label: 'Milestone', type: 'text', required: true, colspan: 'full', value: m.title },
      { name: 'description', label: 'Details', type: 'textarea', colspan: 'full', rows: 2, value: m.description || '' },
      { name: 'deadline', label: 'Deadline', type: 'date', value: m.deadline || '' },
      { name: 'priority', label: 'Priority', type: 'select', value: m.priority, options: [
        { value: 'LOW', label: 'Low' }, { value: 'MEDIUM', label: 'Medium' }, { value: 'HIGH', label: 'High' },
      ] },
      { name: 'status', label: 'Status', type: 'select', required: true, value: m.status, options: [
        { value: 'PENDING', label: 'Pending' }, { value: 'IN_PROGRESS', label: 'In progress' },
        { value: 'DELAYED', label: 'Delayed' }, { value: 'DONE', label: 'Done' },
      ] },
      { name: 'progress', label: 'Progress (%)', type: 'number', required: true, min: 0, max: 100, step: '1', value: m.progress },
    ],
    onSubmit: async (values) => {
      await Api.put(`/api/milestones/${m.id}`, values);
      toastSuccess('Milestone updated successfully.');
      invalidate('overview');
      await reloadTab('roadmap');
    },
  });
}

function deleteMilestone(m) {
  return confirmAndDelete({
    title: `Delete "${m.title}"?`,
    message: 'The milestone is removed from the roadmap and from the health score.',
    confirmLabel: 'Delete milestone',
    url: `/api/milestones/${m.id}`,
    success: 'Milestone deleted.',
    refresh: async () => { invalidate('overview'); await reloadTab('roadmap'); },
  });
}

function openRoadmapModal() {
  openFormModal({
    title: 'Create a roadmap',
    subtitle: 'Group related milestones — for example "Product v1" or "Go to market".',
    submitLabel: 'Create roadmap',
    fields: [
      { name: 'title', label: 'Roadmap title', type: 'text', required: true, colspan: 'full', placeholder: 'Product v1' },
    ],
    onSubmit: async (values) => {
      await Api.post(`/api/startups/${startupId}/roadmaps`, values);
      toastSuccess('Roadmap created');
      await reloadTab('roadmap');
    },
  });
}

function openMilestoneModal(roadmapId) {
  openFormModal({
    title: 'Add a milestone',
    submitLabel: 'Add milestone',
    fields: [
      { name: 'title', label: 'Milestone', type: 'text', required: true, colspan: 'full', placeholder: 'Ship the beta' },
      { name: 'description', label: 'Details', type: 'textarea', colspan: 'full', rows: 2,
        placeholder: 'What has to be true for this to count as done?' },
      { name: 'deadline', label: 'Deadline', type: 'date' },
      { name: 'priority', label: 'Priority', type: 'select', value: 'MEDIUM', options: [
        { value: 'LOW', label: 'Low' },
        { value: 'MEDIUM', label: 'Medium' },
        { value: 'HIGH', label: 'High' },
      ] },
    ],
    onSubmit: async (values) => {
      await Api.post(`/api/roadmaps/${roadmapId}/milestones`, values);
      toastSuccess('Milestone added');
      invalidate('overview');
      await reloadTab('roadmap');
    },
  });
}

function openMilestoneStatusModal(data) {
  openFormModal({
    title: `Update ${data.name}`,
    subtitle: 'Marking it done notifies the founder over WebSocket.',
    submitLabel: 'Save',
    fields: [
      { name: 'status', label: 'Status', type: 'select', required: true, value: data.status, options: [
        { value: 'PENDING', label: 'Pending' },
        { value: 'IN_PROGRESS', label: 'In progress' },
        { value: 'DELAYED', label: 'Delayed' },
        { value: 'DONE', label: 'Done' },
      ] },
      { name: 'progress', label: 'Progress (%)', type: 'number', required: true, min: 0, max: 100, step: '1',
        value: data.progress },
    ],
    onSubmit: async (values) => {
      await Api.post(`/api/milestones/${data.milestoneEdit}/status`, values);
      toastSuccess('Milestone updated');
      invalidate('overview');
      await reloadTab('roadmap');
    },
  });
}

/* ---- Competitors -------------------------------------------------------- */
async function loadCompetitors() {
  const holder = document.getElementById('competitors-holder');
  holder.innerHTML = skeletonRows(3, 32);

  const competitors = await Api.get(`/api/startups/${startupId}/competitors`);
  if (!competitors.length) {
    holder.innerHTML = emptyState({
      icon: '🧭',
      title: 'No competitors tracked',
      description: isOwner ? 'Add the companies you are up against, with their strengths and weaknesses.'
                           : 'The founder has not tracked any competitors yet.',
      actionLabel: isOwner ? '+ Add competitor' : '',
      actionId: 'empty-add-competitor',
    });
    const btn = document.getElementById('empty-add-competitor');
    if (btn) btn.addEventListener('click', openCompetitorModal);
    return;
  }

  const sorted = competitors.slice().sort((a, b) => b.marketShare - a.marketShare);
  holder.innerHTML = `<div class="table-wrap"><table>
      <thead><tr><th>Competitor</th><th style="min-width:160px;">Market share</th><th>Strengths</th><th>Weaknesses</th><th>Opportunities</th><th>Threats</th>${isOwner ? '<th></th>' : ''}</tr></thead>
      <tbody>` + sorted.map(c => `
        <tr>
          <td><strong>${escapeHtml(c.name)}</strong></td>
          <td>
            <div class="progress-row"><span class="mono">${Fmt.percent(c.marketShare)}</span></div>
            <div class="progress burn"><span style="width:${Math.min(100, c.marketShare)}%"></span></div>
          </td>
          <td class="muted" style="font-size:0.85rem;">${escapeHtml(c.strengths || '—')}</td>
          <td class="muted" style="font-size:0.85rem;">${escapeHtml(c.weaknesses || '—')}</td>
          <td class="muted" style="font-size:0.85rem;">${escapeHtml(c.opportunities || '—')}</td>
          <td class="muted" style="font-size:0.85rem;">${escapeHtml(c.threats || '—')}</td>
          ${isOwner ? `<td class="actions">${rowActions(c.id)}</td>` : ''}
        </tr>`).join('') + `</tbody></table></div>`;

  wireRowActions(holder, sorted, { onEdit: openEditCompetitorModal, onDelete: deleteCompetitor });
}

function openEditCompetitorModal(c) {
  openFormModal({
    title: `Edit ${c.name}`,
    subtitle: 'Keep the SWOT notes current as the market moves.',
    submitLabel: 'Save changes',
    wide: true,
    fields: [
      { name: 'name', label: 'Company name', type: 'text', required: true, value: c.name },
      { name: 'marketShare', label: 'Market share (%)', type: 'number', required: true, min: 0, max: 100, step: '0.01', value: c.marketShare },
      { name: 'strengths', label: 'Strengths', type: 'textarea', colspan: 'full', rows: 2, value: c.strengths || '' },
      { name: 'weaknesses', label: 'Weaknesses', type: 'textarea', colspan: 'full', rows: 2, value: c.weaknesses || '' },
      { name: 'opportunities', label: 'Opportunities', type: 'textarea', colspan: 'full', rows: 2, value: c.opportunities || '' },
      { name: 'threats', label: 'Threats', type: 'textarea', colspan: 'full', rows: 2, value: c.threats || '' },
    ],
    onSubmit: async (values) => {
      await Api.put(`/api/startups/${startupId}/competitors/${c.id}`, values);
      toastSuccess('Competitor updated successfully.');
      invalidate('overview');
      await reloadTab('competitors');
    },
  });
}

function deleteCompetitor(c) {
  return confirmAndDelete({
    title: `Stop tracking ${c.name}?`,
    message: 'The competitor and its SWOT notes will be removed.',
    confirmLabel: 'Delete competitor',
    url: `/api/startups/${startupId}/competitors/${c.id}`,
    success: 'Competitor deleted.',
    refresh: async () => { invalidate('overview'); await reloadTab('competitors'); },
  });
}

function openCompetitorModal() {
  openFormModal({
    title: 'Add a competitor',
    subtitle: 'A quick SWOT entry — what they are good at and where the gap is.',
    submitLabel: 'Add competitor',
    wide: true,
    fields: [
      { name: 'name', label: 'Company name', type: 'text', required: true, placeholder: 'Acme Corp' },
      { name: 'marketShare', label: 'Market share (%)', type: 'number', required: true, min: 0, max: 100, step: '0.01', placeholder: '12.5' },
      { name: 'strengths', label: 'Strengths', type: 'textarea', colspan: 'full', rows: 2, placeholder: 'Strong brand, large sales team' },
      { name: 'weaknesses', label: 'Weaknesses', type: 'textarea', colspan: 'full', rows: 2, placeholder: 'Dated product, slow support' },
      { name: 'opportunities', label: 'Opportunities', type: 'textarea', colspan: 'full', rows: 2, placeholder: 'Gap in the student market' },
      { name: 'threats', label: 'Threats', type: 'textarea', colspan: 'full', rows: 2, placeholder: 'Could undercut our pricing' },
    ],
    onSubmit: async (values) => {
      await Api.post(`/api/startups/${startupId}/competitors`, values);
      toastSuccess('Competitor added');
      invalidate('overview');
      await reloadTab('competitors');
    },
  });
}

/* ---- Wire the toolbar buttons ------------------------------------------- */
function wireButtons() {
  const pairs = [
    ['add-employee-btn', openEmployeeModal],
    ['add-revenue-btn', openRevenueModal],
    ['add-expense-btn', openExpenseModal],
    ['add-investment-btn', openInvestmentModal],
    ['add-campaign-btn', openCampaignModal],
    ['add-roadmap-btn', openRoadmapModal],
    ['add-competitor-btn', openCompetitorModal],
  ];
  pairs.forEach(([id, handler]) => {
    const el = document.getElementById(id);
    if (el) el.addEventListener('click', handler);
  });
}

init().catch(err => {
  if (err && err.status !== 401) toastError(err.message || 'Could not load this startup');
});
