/* Automatic revenue calculator (Finance tab).

   The browser only collects inputs. The amount always comes from the server:
     POST /finance/revenue/calculate   -> preview (nothing stored)
     POST /finance/revenue/calculated  -> server recalculates from the same inputs and stores ONE revenue record
   On the server the RevenueStrategy for the chosen model does the maths (Strategy pattern).

   Depends on globals from startup.js (startupId, invalidate, reloadTab) and ui.js. */

const CALC_MODELS = {
  SUBSCRIPTION: {
    label: 'Subscription',
    hint: 'Monthly revenue = paying customers × monthly price',
    fields: [
      { name: 'customers', label: 'Paying customers', integer: true, min: 1, step: '1', placeholder: '100' },
      { name: 'monthlyPrice', label: 'Monthly price (USD)', min: 0.01, step: '0.01', placeholder: '20' },
    ],
  },
  TRANSACTION: {
    label: 'Transaction / commission',
    hint: 'Revenue = transactions × average transaction value × commission %',
    fields: [
      { name: 'transactions', label: 'Number of transactions', integer: true, min: 1, step: '1', placeholder: '500' },
      { name: 'averageTransactionValue', label: 'Average transaction value (USD)', min: 0.01, step: '0.01', placeholder: '40' },
      { name: 'commissionPercent', label: 'Commission (%)', min: 0.01, max: 100, step: '0.01', placeholder: '5', colspan: 'full' },
    ],
  },
  ADVERTISEMENT: {
    label: 'Advertisement',
    hint: 'Revenue = (impressions ÷ 1,000) × CPM  (CPM = price per 1,000 impressions)',
    fields: [
      { name: 'impressions', label: 'Number of impressions', integer: true, min: 1, step: '1', placeholder: '100000' },
      { name: 'cpm', label: 'CPM (USD per 1,000 impressions)', min: 0.01, step: '0.01', placeholder: '5' },
    ],
  },
};

const calcState = { wired: false, result: null };

function initRevenueCalculator() {
  const form = document.getElementById('calc-form');
  if (!form) return;
  if (!calcState.wired) {
    calcState.wired = true;
    document.getElementById('calc-model').addEventListener('change', () => { renderCalcFields(); resetCalcResult(); });
    form.addEventListener('submit', onCalcSubmit);
    // Any edit to an input makes the previous result stale, so it must be recalculated before saving.
    form.addEventListener('input', (e) => {
      if (e.target.id === 'calc-source') return;
      resetCalcResult();
    });
    renderCalcFields();
  }
  resetCalcResult();
}

function currentCalcModel() {
  return CALC_MODELS[document.getElementById('calc-model').value];
}

/* Only the inputs that belong to the selected model are rendered. */
function renderCalcFields() {
  const cfg = currentCalcModel();
  document.getElementById('calc-hint').textContent = cfg.hint;
  document.getElementById('calc-fields').innerHTML = cfg.fields.map(f => `
    <div class="field ${f.colspan === 'full' ? 'full' : ''}" data-field="${f.name}">
      <label for="calc_${f.name}">${escapeHtml(f.label)}</label>
      <input id="calc_${f.name}" name="${f.name}" type="number" inputmode="decimal"
             min="${f.min}" ${f.max !== undefined ? `max="${f.max}"` : ''} step="${f.step}"
             placeholder="${f.placeholder}" autocomplete="off">
      <div class="field-error" data-error-for="${f.name}"></div>
    </div>`).join('');
}

function resetCalcResult() {
  calcState.result = null;
  const box = document.getElementById('calc-result');
  if (!box) return;
  box.classList.remove('ready');
  box.innerHTML = `<div class="calc-placeholder">Enter the numbers and press <strong>Calculate revenue</strong>.<br>The result is calculated by the server.</div>`;
}

/* Reads and checks the visible inputs. Returns { body } or null when something is invalid. */
function readCalcInputs(form) {
  clearFieldErrors(form);
  const cfg = currentCalcModel();
  const body = { model: document.getElementById('calc-model').value };
  let firstBad = null;

  for (const f of cfg.fields) {
    const raw = form.elements[f.name].value;
    const value = raw === '' ? NaN : Number(raw);
    let problem = null;
    if (Number.isNaN(value)) problem = `${f.label.replace(/ \(.*\)/, '')} is required.`;
    else if (value < f.min) problem = `${f.label.replace(/ \(.*\)/, '')} must be greater than 0.`;
    else if (f.max !== undefined && value > f.max) problem = `${f.label.replace(/ \(.*\)/, '')} cannot be more than ${f.max}.`;
    else if (f.integer && !Number.isInteger(value)) problem = `${f.label.replace(/ \(.*\)/, '')} must be a whole number.`;

    if (problem) {
      attachFieldError(form, f.name, problem);
      if (!firstBad) firstBad = f.name;
    }
    body[f.name] = value;
  }
  if (firstBad) { form.elements[firstBad].focus(); return null; }

  const source = document.getElementById('calc-source').value.trim();
  if (source) body.source = source;
  return body;
}

async function onCalcSubmit(e) {
  e.preventDefault();
  const form = e.currentTarget;
  const btn = document.getElementById('calc-btn');
  if (btn.disabled) return;

  const body = readCalcInputs(form);
  if (!body) return;

  setButtonLoading(btn, true, 'Calculating…');
  try {
    const result = await Api.post(`/api/startups/${startupId}/finance/revenue/calculate`, body);
    calcState.result = { body, result };
    showCalcResult(result);
    toastSuccess(`Revenue calculated: ${Fmt.money(result.amount, { compact: false })}.`);
  } catch (err) {
    resetCalcResult();
    document.getElementById('calc-result').innerHTML =
      `<div class="alert alert-danger" style="margin:0;">${escapeHtml(err.message || 'Could not calculate revenue.')}</div>`;
    toastError(err.message || 'Could not calculate revenue.');
  } finally {
    setButtonLoading(btn, false);
  }
}

function showCalcResult(result) {
  const box = document.getElementById('calc-result');
  box.classList.add('ready');
  const period = result.model === 'SUBSCRIPTION' ? 'Calculated monthly revenue' : 'Calculated revenue';
  box.innerHTML = `
    <div class="cr-label">${period}</div>
    <div class="cr-value">${Fmt.money(result.amount, { compact: false })}</div>
    <div class="cr-formula">${escapeHtml(result.formula)}</div>
    <div class="cr-actions">
      <button type="button" class="btn btn-success btn-sm" id="calc-save-btn">Save revenue</button>
    </div>`;
  document.getElementById('calc-save-btn').addEventListener('click', saveCalculatedRevenue);
}

async function saveCalculatedRevenue() {
  if (!calcState.result) return;
  const btn = document.getElementById('calc-save-btn');
  setButtonLoading(btn, true, 'Saving…');
  try {
    // Only the inputs are sent - the server calculates the stored amount again itself.
    await Api.post(`/api/startups/${startupId}/finance/revenue/calculated`, calcState.result.body);
    toastSuccess('Revenue saved successfully.', 'Totals, charts and the health score now include it.');
    document.getElementById('calc-form').reset();
    renderCalcFields(); // reset() puts the model back to Subscription, so rebuild its inputs
    resetCalcResult();
    invalidate('overview');
    await reloadTab('finance');
  } catch (err) {
    toastError(err.message || 'Could not save the revenue.');
    setButtonLoading(btn, false);
  }
}
