/* MVP tab: the product a startup is building, its features, and progress computed from them.

   Progress is never typed in. The server derives it (completed features / total features x 100)
   and sends progress, completedFeatures and totalFeatures with the MVP.

   Depends on globals from startup.js (startupId, startup, isOwner, invalidate, reloadTab) and ui.js. */

const MVP_STATUSES = [
  { value: 'PLANNING', label: 'Planning' },
  { value: 'IN_DEVELOPMENT', label: 'In development' },
  { value: 'TESTING', label: 'Testing' },
  { value: 'LAUNCHED', label: 'Launched' },
];

function mvpStatusBadge(status) {
  const cls = { PLANNING: 'badge-neutral', IN_DEVELOPMENT: 'badge-brand', TESTING: 'badge-burn', LAUNCHED: 'badge-growth' }[status] || 'badge-neutral';
  const label = (MVP_STATUSES.find(s => s.value === status) || {}).label || Fmt.titleCase(status);
  return `<span class="badge ${cls}">${escapeHtml(label)}</span>`;
}

let currentMvp = null;

async function loadMvp() {
  const holder = document.getElementById('mvp-holder');
  if (!holder.dataset.loaded) holder.innerHTML = skeletonRows(4, 44);

  const mvp = await Api.get(`/api/startups/${startupId}/mvp`); // null (204) when none exists yet
  currentMvp = mvp;
  holder.dataset.loaded = '1';

  if (!mvp) {
    holder.innerHTML = `<div class="card">${emptyState({
      icon: '🧩',
      title: 'No MVP defined yet',
      description: isOwner
        ? 'Define the first version of your product, then list the features it needs. Progress is calculated automatically.'
        : 'The founder has not defined an MVP yet.',
      actionLabel: isOwner ? '+ Create MVP' : '',
      actionId: 'empty-create-mvp',
    })}</div>`;
    const btn = document.getElementById('empty-create-mvp');
    if (btn) btn.addEventListener('click', () => openMvpModal(null));
    return;
  }

  holder.innerHTML = mvpSummaryCard(mvp) + mvpFeaturesCard(mvp);
  wireMvp(holder, mvp);
}

function mvpProgressBar(mvp, large = true) {
  const tone = mvp.progress >= 100 ? 'growth' : '';
  return `
    <div class="progress ${large ? 'lg' : ''} ${tone}" role="progressbar" aria-valuemin="0" aria-valuemax="100"
         aria-valuenow="${mvp.progress}" aria-label="MVP progress">
      <span style="width:${mvp.progress}%"></span>
    </div>`;
}

function mvpSummaryCard(mvp) {
  return `
    <div class="card">
      <div class="mvp-summary">
        <div style="min-width:0;">
          <div class="mvp-title">
            <h3>MVP — Version ${escapeHtml(mvp.version)}</h3>
            ${mvpStatusBadge(mvp.status)}
          </div>
          <div style="font-weight:600;">${escapeHtml(mvp.name)}</div>
          ${mvp.description ? `<p class="muted" style="margin:6px 0 0; max-width:70ch;">${escapeHtml(mvp.description)}</p>` : ''}
        </div>
        ${isOwner ? `<div class="toolbar">
          <button class="btn btn-outline btn-sm" id="edit-mvp-btn">Edit MVP</button>
          <button class="btn btn-ghost btn-sm" id="delete-mvp-btn">Delete</button>
        </div>` : ''}
      </div>

      <div class="mvp-meta">
        <div><div class="k">Target users</div><div class="v">${escapeHtml(mvp.targetUsers || '—')}</div></div>
        <div><div class="k">Launch date</div><div class="v">${mvp.launchDate ? Fmt.date(mvp.launchDate) : '—'}</div></div>
        <div><div class="k">Last updated</div><div class="v">${Fmt.relative(mvp.updatedAt) || '—'}</div></div>
      </div>

      <div class="progress-row"><span>Progress</span><span class="mvp-progress-num">${mvp.progress}%</span></div>
      ${mvpProgressBar(mvp)}
      <div class="muted" style="margin-top:8px; font-size:0.86rem;">
        ${mvp.completedFeatures} of ${mvp.totalFeatures} feature${mvp.totalFeatures === 1 ? '' : 's'} completed
        ${mvp.totalFeatures === 0 ? '— add features and progress fills in automatically' : ''}
      </div>
    </div>`;
}

function mvpFeaturesCard(mvp) {
  const rows = mvp.features.map(f => `
    <div class="feature-row ${f.completed ? 'done' : ''}" data-feature="${f.id}">
      <button class="feature-check ${f.completed ? 'done' : ''}" data-toggle="${f.id}" ${isOwner ? '' : 'disabled'}
              title="${f.completed ? 'Reopen feature' : 'Mark as complete'}"
              aria-label="${f.completed ? 'Reopen' : 'Complete'} ${escapeHtml(f.title)}">${f.completed ? '✓' : ''}</button>
      <div class="feature-body">
        <div class="feature-title"><strong>${escapeHtml(f.title)}</strong>${priorityBadge(f.priority)}</div>
        ${f.description ? `<p class="feature-desc">${escapeHtml(f.description)}</p>` : ''}
      </div>
      ${isOwner ? `<div class="feature-actions">
        <button class="btn btn-sm btn-outline" data-edit-feature="${f.id}">Edit</button>
        <button class="btn btn-sm btn-ghost" data-delete-feature="${f.id}">Delete</button>
      </div>` : ''}
    </div>`).join('');

  return `
    <div class="card" style="margin-top:16px;">
      <div class="card-head">
        <h4>MVP features</h4>
        ${isOwner ? `<button class="btn btn-primary btn-sm" id="add-feature-btn">+ Add feature</button>` : ''}
      </div>
      ${mvp.features.length
        ? `<div class="feature-list">${rows}</div>`
        : emptyState({ icon: '📝', title: 'No features yet',
            description: isOwner ? 'List what the MVP must do — registration, payments, reports…' : 'Nothing has been listed yet.' })}
    </div>`;
}

function wireMvp(holder, mvp) {
  const byId = new Map(mvp.features.map(f => [String(f.id), f]));
  const on = (sel, fn) => holder.querySelectorAll(sel).forEach(el => el.addEventListener('click', () => fn(el)));

  const edit = document.getElementById('edit-mvp-btn');
  if (edit) edit.addEventListener('click', () => openMvpModal(mvp));
  const del = document.getElementById('delete-mvp-btn');
  if (del) del.addEventListener('click', () => deleteMvp(mvp));
  const add = document.getElementById('add-feature-btn');
  if (add) add.addEventListener('click', () => openFeatureModal(mvp, null));

  on('[data-toggle]', btn => toggleFeature(mvp, byId.get(btn.dataset.toggle), btn));
  on('[data-edit-feature]', btn => openFeatureModal(mvp, byId.get(btn.dataset.editFeature)));
  on('[data-delete-feature]', btn => deleteFeature(mvp, byId.get(btn.dataset.deleteFeature)));
}

/* One place to refresh everything an MVP change can affect. */
async function afterMvpChange() {
  invalidate('overview'); // health score, glance card and MVP progress on the overview
  await reloadTab('mvp');
}

function openMvpModal(mvp) {
  const editing = !!mvp;
  openFormModal({
    title: editing ? 'Edit MVP' : 'Create MVP',
    subtitle: editing ? 'Progress is calculated from the features, so it is not editable here.'
                      : 'You can add the feature list right after this.',
    submitLabel: editing ? 'Save changes' : 'Create MVP',
    wide: true,
    fields: [
      { name: 'name', label: 'MVP name', type: 'text', required: true, value: editing ? mvp.name : `${startup.name} MVP`, placeholder: 'StartupHub MVP' },
      { name: 'version', label: 'Version', type: 'text', required: true, value: editing ? mvp.version : '1.0', placeholder: '1.0' },
      { name: 'status', label: 'Status', type: 'select', required: true, value: editing ? mvp.status : 'PLANNING',
        options: MVP_STATUSES.map(s => ({ value: s.value, label: s.label })) },
      { name: 'launchDate', label: 'Launch date', type: 'date', value: editing ? (mvp.launchDate || '') : '' },
      { name: 'targetUsers', label: 'Target users', type: 'text', colspan: 'full', value: editing ? (mvp.targetUsers || '') : '',
        placeholder: 'University students' },
      { name: 'description', label: 'Description', type: 'textarea', colspan: 'full', rows: 3,
        value: editing ? (mvp.description || '') : '', placeholder: 'What does the first version do?' },
    ],
    onSubmit: async (values) => {
      if (editing) {
        await Api.put(`/api/startups/${startupId}/mvp/${mvp.id}`, values);
        toastSuccess('MVP updated successfully.');
      } else {
        await Api.post(`/api/startups/${startupId}/mvp`, values);
        toastSuccess('MVP created successfully.');
      }
      await afterMvpChange();
    },
  });
}

async function deleteMvp(mvp) {
  const ok = await confirmDialog({
    title: 'Delete this MVP?',
    message: `MVP ${mvp.version} and all ${mvp.totalFeatures} of its features will be removed. This cannot be undone.`,
    confirmLabel: 'Delete MVP',
  });
  if (!ok) return;
  try {
    await Api.del(`/api/startups/${startupId}/mvp/${mvp.id}`);
    toastSuccess('MVP deleted.');
    await afterMvpChange();
  } catch (err) {
    toastError(err.message || 'Could not delete the MVP.');
  }
}

function openFeatureModal(mvp, feature) {
  const editing = !!feature;
  openFormModal({
    title: editing ? 'Edit feature' : 'Add an MVP feature',
    submitLabel: editing ? 'Save changes' : 'Add feature',
    fields: [
      { name: 'title', label: 'Feature', type: 'text', required: true, colspan: 'full',
        value: editing ? feature.title : '', placeholder: 'Payment integration' },
      { name: 'description', label: 'Details', type: 'textarea', colspan: 'full', rows: 2,
        value: editing ? (feature.description || '') : '', placeholder: 'What must it do?' },
      { name: 'priority', label: 'Priority', type: 'select', value: editing ? feature.priority : 'MEDIUM', options: [
        { value: 'LOW', label: 'Low' }, { value: 'MEDIUM', label: 'Medium' }, { value: 'HIGH', label: 'High' },
      ] },
    ],
    onSubmit: async (values) => {
      if (editing) {
        await Api.put(`/api/startups/${startupId}/mvp/${mvp.id}/features/${feature.id}`, values);
        toastSuccess('Feature updated successfully.');
      } else {
        await Api.post(`/api/startups/${startupId}/mvp/${mvp.id}/features`, values);
        toastSuccess('Feature added successfully.');
      }
      await afterMvpChange();
    },
  });
}

async function toggleFeature(mvp, feature, btn) {
  if (!feature || btn.disabled) return;
  btn.disabled = true;
  const action = feature.completed ? 'reopen' : 'complete';
  try {
    await Api.patch(`/api/startups/${startupId}/mvp/${mvp.id}/features/${feature.id}/${action}`);
    toastSuccess(action === 'complete' ? 'Feature marked as completed.' : 'Feature reopened.');
    await afterMvpChange();
  } catch (err) {
    toastError(err.message || 'Could not update the feature.');
    btn.disabled = false;
  }
}

async function deleteFeature(mvp, feature) {
  if (!feature) return;
  const ok = await confirmDialog({
    title: `Delete "${feature.title}"?`,
    message: 'The feature is removed and MVP progress is recalculated.',
    confirmLabel: 'Delete feature',
  });
  if (!ok) return;
  try {
    await Api.del(`/api/startups/${startupId}/mvp/${mvp.id}/features/${feature.id}`);
    toastSuccess('Feature deleted.');
    await afterMvpChange();
  } catch (err) {
    toastError(err.message || 'Could not delete the feature.');
  }
}

/* Small progress card for the Overview tab. Uses the numbers the analytics endpoint already returns. */
function renderMvpOverview(analytics) {
  const holder = document.getElementById('mvp-overview-holder');
  if (!holder) return;
  if (!analytics.mvpExists) {
    holder.innerHTML = `<p class="muted" style="margin:0 0 10px; font-size:0.88rem;">${
      isOwner ? 'No MVP yet. Define one to track feature progress.' : 'No MVP defined yet.'}</p>
      <button class="btn btn-outline btn-sm" id="goto-mvp-btn">${isOwner ? 'Set up MVP' : 'Open MVP tab'}</button>`;
  } else {
    holder.innerHTML = `
      <div class="row-between" style="margin-bottom:8px;">
        <strong>Version ${escapeHtml(analytics.mvpVersion)}</strong>${mvpStatusBadge(analytics.mvpStatus)}
      </div>
      <div class="progress-row"><span>${analytics.mvpFeaturesDone} of ${analytics.mvpFeatureCount} features</span>
        <span class="mono">${analytics.mvpProgress}%</span></div>
      <div class="progress ${analytics.mvpProgress >= 100 ? 'growth' : ''}"><span style="width:${analytics.mvpProgress}%"></span></div>
      <button class="btn btn-ghost btn-sm" id="goto-mvp-btn" style="margin-top:10px; padding-left:0;">View MVP →</button>`;
  }
  const go = document.getElementById('goto-mvp-btn');
  if (go) go.addEventListener('click', () => document.querySelector('.tab[data-tab="mvp"]').click());
}
