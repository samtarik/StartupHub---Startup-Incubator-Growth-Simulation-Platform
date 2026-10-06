/* Shared UI kit: formatting, toasts, modals, confirmations, form validation,
   loading and empty states, and the light/dark theme switch.

   Every page loads this before its own script. Nothing here talks to the API. */

/* ---- Formatting --------------------------------------------------------- */
const Fmt = {
  money(value, { compact = true } = {}) {
    const n = Number(value) || 0;
    const sign = n < 0 ? '-' : '';
    const abs = Math.abs(n);
    if (!compact) return sign + '$' + abs.toLocaleString(undefined, { maximumFractionDigits: 2 });
    if (abs >= 1_000_000) return sign + '$' + (abs / 1_000_000).toFixed(abs >= 10_000_000 ? 0 : 1) + 'M';
    if (abs >= 1_000) return sign + '$' + (abs / 1_000).toFixed(abs >= 10_000 ? 0 : 1) + 'K';
    return sign + '$' + abs.toLocaleString(undefined, { maximumFractionDigits: 0 });
  },
  number(value) { return (Number(value) || 0).toLocaleString(); },
  percent(value, digits = 1) { return (Number(value) || 0).toFixed(digits) + '%'; },
  date(value) {
    if (!value) return '—';
    const d = new Date(value);
    if (isNaN(d)) return String(value);
    return d.toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
  },
  /* "5 minutes ago" / "Yesterday" - used by the activity feed. */
  relative(value) {
    if (!value) return '';
    const d = new Date(value);
    if (isNaN(d)) return '';
    const secs = Math.floor((Date.now() - d.getTime()) / 1000);
    if (secs < 60) return 'Just now';
    if (secs < 3600) { const m = Math.floor(secs / 60); return `${m} minute${m === 1 ? '' : 's'} ago`; }
    if (secs < 86400) { const h = Math.floor(secs / 3600); return `${h} hour${h === 1 ? '' : 's'} ago`; }
    if (secs < 172800) return 'Yesterday';
    if (secs < 2592000) { const dd = Math.floor(secs / 86400); return `${dd} days ago`; }
    return Fmt.date(value);
  },
  titleCase(value) {
    if (!value) return '';
    return String(value).replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
  },
};

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str ?? '';
  return div.innerHTML;
}

/* ---- Toasts ------------------------------------------------------------- */
function toastStack() {
  let stack = document.getElementById('toast-stack');
  if (!stack) {
    stack = document.createElement('div');
    stack.id = 'toast-stack';
    document.body.appendChild(stack);
  }
  return stack;
}

/* type: 'success' | 'error' | 'info' */
function toast(message, type = 'info', detail = '') {
  const stack = toastStack();
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.setAttribute('role', type === 'error' ? 'alert' : 'status');
  const icon = type === 'success' ? '✓' : type === 'error' ? '⚠' : '●';
  el.innerHTML = `
    <span class="ic">${icon}</span>
    <span class="tx">
      <span class="tt">${escapeHtml(message)}</span>
      ${detail ? `<span class="td">${escapeHtml(detail)}</span>` : ''}
    </span>`;
  stack.appendChild(el);
  setTimeout(() => el.remove(), type === 'error' ? 7000 : 4500);
}

const toastSuccess = (m, d) => toast(m, 'success', d);
const toastError = (m, d) => toast(m, 'error', d);

/* ---- Buttons in flight -------------------------------------------------- */
function setButtonLoading(btn, loading, loadingLabel) {
  if (!btn) return;
  if (loading) {
    btn.dataset.label = btn.dataset.label || btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = `<span class="spinner" aria-hidden="true"></span>${escapeHtml(loadingLabel || 'Saving…')}`;
  } else {
    btn.disabled = false;
    if (btn.dataset.label) btn.innerHTML = btn.dataset.label;
  }
}

/* ---- Loading and empty states ------------------------------------------- */
function skeletonRows(count = 3, height = 14) {
  return Array.from({ length: count }, () => `<div class="skeleton" style="height:${height}px;"></div>`).join('');
}
function skeletonCards(count = 4) {
  return Array.from({ length: count }, () => `<div class="skeleton skeleton-card"></div>`).join('');
}
function skeletonTableRows(cols, rows = 3) {
  return Array.from({ length: rows }, () =>
    `<tr>${Array.from({ length: cols }, () => '<td><div class="skeleton"></div></td>').join('')}</tr>`).join('');
}

/* An empty screen is an invitation to act, so it always offers the next step. */
function emptyState({ icon = '📭', title, description = '', actionLabel = '', actionId = '' }) {
  return `
    <div class="empty-state">
      <div class="ic" aria-hidden="true">${icon}</div>
      <div class="t">${escapeHtml(title)}</div>
      ${description ? `<p class="d">${escapeHtml(description)}</p>` : ''}
      ${actionLabel ? `<button class="btn btn-primary btn-sm" id="${actionId}">${escapeHtml(actionLabel)}</button>` : ''}
    </div>`;
}
function emptyRow(cols, text) {
  return `<tr><td colspan="${cols}" class="muted" style="text-align:center; padding:26px;">${escapeHtml(text)}</td></tr>`;
}

/* ---- Modals ------------------------------------------------------------- */
let openModals = 0;

function baseModal(innerHtml, { wide = false, onClose } = {}) {
  const backdrop = document.createElement('div');
  backdrop.className = 'modal-backdrop';
  backdrop.innerHTML = `<div class="modal ${wide ? 'wide' : ''}" role="dialog" aria-modal="true">${innerHtml}</div>`;
  document.body.appendChild(backdrop);
  openModals++;
  document.body.style.overflow = 'hidden';

  const close = () => {
    if (!backdrop.isConnected) return;
    backdrop.remove();
    openModals = Math.max(0, openModals - 1);
    if (openModals === 0) document.body.style.overflow = '';
    document.removeEventListener('keydown', onKey);
    if (onClose) onClose();
  };
  const onKey = (e) => { if (e.key === 'Escape') close(); };

  document.addEventListener('keydown', onKey);
  backdrop.addEventListener('mousedown', (e) => { if (e.target === backdrop) close(); });
  backdrop.querySelectorAll('[data-close]').forEach(el => el.addEventListener('click', close));

  return { backdrop, modal: backdrop.querySelector('.modal'), close };
}

/* Confirmation dialog for destructive actions. Resolves true/false. */
function confirmDialog({ title, message, confirmLabel = 'Confirm', cancelLabel = 'Cancel', danger = true, icon = '⚠' }) {
  return new Promise(resolve => {
    let settled = false;
    const done = (value) => { if (!settled) { settled = true; resolve(value); } };

    const { modal, close } = baseModal(`
      <div class="modal-head">
        <div style="display:flex; gap:13px;">
          <div class="modal-icon" aria-hidden="true">${icon}</div>
          <div>
            <h4>${escapeHtml(title)}</h4>
            <p>${escapeHtml(message)}</p>
          </div>
        </div>
      </div>
      <div class="modal-foot">
        <button class="btn btn-outline" data-cancel>${escapeHtml(cancelLabel)}</button>
        <button class="btn ${danger ? 'btn-danger' : 'btn-primary'}" data-confirm>${escapeHtml(confirmLabel)}</button>
      </div>`, { onClose: () => done(false) });

    modal.querySelector('[data-cancel]').addEventListener('click', () => { done(false); close(); });
    modal.querySelector('[data-confirm]').addEventListener('click', () => { done(true); close(); });
    modal.querySelector('[data-confirm]').focus();
  });
}

/* ---- Form modal ---------------------------------------------------------
   Fields are described as data, so every form in the app validates, reports
   errors and handles its loading state the same way.

   field: { name, label, type, required, min, max, step, placeholder, options,
            value, rows, hint, help, colspan }                                */
function fieldHtml(f) {
  const id = `f_${f.name}`;
  const req = f.required ? 'required' : '';
  let control;

  if (f.type === 'select') {
    const opts = (f.options || []).map(o =>
      `<option value="${escapeHtml(o.value)}" ${String(o.value) === String(f.value ?? '') ? 'selected' : ''}>${escapeHtml(o.label)}</option>`).join('');
    control = `<select id="${id}" name="${f.name}" ${req}>${opts}</select>`;
  } else if (f.type === 'textarea') {
    control = `<textarea id="${id}" name="${f.name}" rows="${f.rows || 3}" ${req}
                 placeholder="${escapeHtml(f.placeholder || '')}">${escapeHtml(f.value ?? '')}</textarea>`;
  } else {
    const min = f.min !== undefined ? `min="${f.min}"` : '';
    const max = f.max !== undefined ? `max="${f.max}"` : '';
    const step = f.step !== undefined ? `step="${f.step}"` : '';
    control = `<input id="${id}" name="${f.name}" type="${f.type || 'text'}" ${req} ${min} ${max} ${step}
                 value="${escapeHtml(f.value ?? '')}" placeholder="${escapeHtml(f.placeholder || '')}"
                 autocomplete="off">`;
  }

  return `
    <div class="field ${f.colspan === 'full' ? 'full' : ''}" data-field="${f.name}">
      <label for="${id}">${escapeHtml(f.label)}${f.required ? '' : ' <span class="subtle">(optional)</span>'}</label>
      ${control}
      ${f.hint ? `<div class="hint">${escapeHtml(f.hint)}</div>` : ''}
      <div class="field-error" data-error-for="${f.name}"></div>
    </div>`;
}

function readValue(f, form) {
  const el = form.elements[f.name];
  if (!el) return null;
  const raw = el.value;
  if (f.type === 'number') return raw === '' ? null : Number(raw);
  // An empty date must go to the server as null, not as "" - Jackson cannot
  // parse an empty string into a LocalDate.
  if (f.type === 'date') return raw === '' ? null : raw;
  return typeof raw === 'string' ? raw.trim() : raw;
}

/* Client-side checks that mirror the bean validation on the server. The server
   remains the real gate - this just saves the user a round trip. */
function validateField(f, value) {
  if (f.required && (value === null || value === '' || value === undefined)) {
    return `${f.label} is required`;
  }
  if (value === null || value === '') return null;

  if (f.type === 'number') {
    if (Number.isNaN(value)) return `${f.label} must be a number`;
    if (f.min !== undefined && value < f.min) {
      return f.min === 0 ? `${f.label} cannot be negative` : `${f.label} must be at least ${f.min}`;
    }
    if (f.max !== undefined && value > f.max) return `${f.label} cannot be more than ${f.max}`;
  }
  if (f.type === 'email' && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
    return 'Enter a valid email address';
  }
  if (f.type === 'date' && Number.isNaN(new Date(value).getTime())) {
    return 'Enter a valid date';
  }
  if (f.validate) return f.validate(value);
  return null;
}

function attachFieldError(form, name, message) {
  const wrap = form.querySelector(`[data-field="${name}"]`);
  if (!wrap) return;
  wrap.classList.add('invalid');
  const box = wrap.querySelector(`[data-error-for="${name}"]`);
  if (box) box.textContent = message;
}

function clearFieldErrors(form) {
  form.querySelectorAll('.field.invalid').forEach(el => el.classList.remove('invalid'));
  form.querySelectorAll('.field-error').forEach(el => (el.textContent = ''));
}

function openFormModal({ title, subtitle = '', fields, submitLabel = 'Save', loadingLabel = 'Saving…', wide = false, onSubmit }) {
  const { modal, close } = baseModal(`
    <form novalidate>
      <div class="modal-head">
        <div>
          <h4>${escapeHtml(title)}</h4>
          ${subtitle ? `<p>${escapeHtml(subtitle)}</p>` : ''}
        </div>
        <button type="button" class="x" data-close aria-label="Close">×</button>
      </div>
      <div class="modal-body">
        <div class="form-grid">${fields.map(fieldHtml).join('')}</div>
      </div>
      <div class="modal-foot">
        <button type="button" class="btn btn-outline" data-close>Cancel</button>
        <button type="submit" class="btn btn-primary">${escapeHtml(submitLabel)}</button>
      </div>
    </form>`, { wide });

  const form = modal.querySelector('form');
  const submitBtn = form.querySelector('button[type="submit"]');

  // Clear a field's error as soon as the user starts fixing it.
  form.querySelectorAll('input, select, textarea').forEach(el => {
    el.addEventListener('input', () => {
      const wrap = el.closest('.field');
      if (wrap) { wrap.classList.remove('invalid'); const b = wrap.querySelector('.field-error'); if (b) b.textContent = ''; }
    });
  });

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (submitBtn.disabled) return; // guards against double submission
    clearFieldErrors(form);

    const values = {};
    let firstBad = null;
    for (const f of fields) {
      const value = readValue(f, form);
      const problem = validateField(f, value);
      if (problem) {
        attachFieldError(form, f.name, problem);
        if (!firstBad) firstBad = f.name;
      }
      values[f.name] = value;
    }
    if (firstBad) {
      const el = form.elements[firstBad];
      if (el && el.focus) el.focus();
      return;
    }

    setButtonLoading(submitBtn, true, loadingLabel);
    try {
      await onSubmit(values, { close, form, setFieldError: (n, m) => attachFieldError(form, n, m) });
      close();
    } catch (err) {
      // Field-level errors from the server land on the matching input.
      if (err.fields) {
        Object.entries(err.fields).forEach(([name, message]) => attachFieldError(form, name, message));
      }
      toastError(err.message || 'Something went wrong', err.fields ? '' : 'Please check the information and try again.');
      setButtonLoading(submitBtn, false);
    }
  });

  const first = form.querySelector('input, select, textarea');
  if (first) setTimeout(() => first.focus(), 30);
  return { close, form };
}

/* ---- Theme -------------------------------------------------------------- */
const Theme = {
  key: 'startuphub-theme',
  get() {
    try { return localStorage.getItem(this.key) || 'light'; } catch (_) { return 'light'; }
  },
  apply(mode) {
    document.documentElement.setAttribute('data-theme', mode);
    try { localStorage.setItem(this.key, mode); } catch (_) { /* private mode */ }
    document.dispatchEvent(new CustomEvent('themechange', { detail: { mode } }));
  },
  toggle() { this.apply(this.get() === 'dark' ? 'light' : 'dark'); },
  init() { document.documentElement.setAttribute('data-theme', this.get()); },
};
Theme.init();


/* ---- Shared display pieces ----------------------------------------------
   Used by both the dashboard and the startup page, so they live here rather
   than being written twice. */

function kpiCard({ label, value, accent = 'brand', icon = '', foot = '' }) {
  return `
    <article class="kpi">
      <div class="kpi-label">
        ${icon ? `<span class="kpi-accent ${accent}" aria-hidden="true">${icon}</span>` : ''}
        ${escapeHtml(label)}
      </div>
      <div class="kpi-value">${value}</div>
      <div class="kpi-foot">${foot || '&nbsp;'}</div>
    </article>`;
}

/* Overdue beats the stored status: a milestone past its deadline reads as
   overdue even if nobody has changed its status yet. */
function milestoneStatusBadge(m) {
  if (m.status === 'DONE') return '<span class="badge badge-growth">Done</span>';
  if (m.overdue) return '<span class="badge badge-danger">Overdue</span>';
  if (m.status === 'IN_PROGRESS') return '<span class="badge badge-brand">In progress</span>';
  if (m.status === 'DELAYED') return '<span class="badge badge-burn">Delayed</span>';
  return '<span class="badge badge-neutral">Pending</span>';
}
