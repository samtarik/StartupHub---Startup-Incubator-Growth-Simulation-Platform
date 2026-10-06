/* Login and registration. One file serves both pages - it wires up whichever
   form is present. Validation happens here first, then again on the server. */

function markError(name, message) {
  const wrap = document.querySelector(`[data-field="${name}"]`);
  if (!wrap) return;
  wrap.classList.add('invalid');
  const box = wrap.querySelector(`[data-error-for="${name}"]`);
  if (box) box.textContent = message;
}

function clearErrors(form) {
  form.querySelectorAll('.field.invalid').forEach(el => el.classList.remove('invalid'));
  form.querySelectorAll('.field-error').forEach(el => (el.textContent = ''));
  const box = document.getElementById('alert-box');
  if (box) box.innerHTML = '';
}

function showAlert(kind, message) {
  const box = document.getElementById('alert-box');
  if (box) box.innerHTML = `<div class="alert alert-${kind}">${escapeHtml(message)}</div>`;
}

/* Password reveal toggle, shared by both pages. */
function wirePasswordToggle() {
  const btn = document.getElementById('toggle-password');
  const input = document.getElementById('password');
  if (!btn || !input) return;
  btn.addEventListener('click', () => {
    const showing = input.type === 'text';
    input.type = showing ? 'password' : 'text';
    btn.textContent = showing ? 'Show' : 'Hide';
    btn.setAttribute('aria-label', showing ? 'Show password' : 'Hide password');
    input.focus();
  });
}

function clearOnInput(form) {
  form.querySelectorAll('input').forEach(el => {
    el.addEventListener('input', () => {
      const wrap = el.closest('.field');
      if (wrap) { wrap.classList.remove('invalid'); const b = wrap.querySelector('.field-error'); if (b) b.textContent = ''; }
    });
  });
}

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/* ---- Login -------------------------------------------------------------- */
const loginForm = document.getElementById('login-form');
if (loginForm) {
  wirePasswordToggle();
  clearOnInput(loginForm);

  if (new URLSearchParams(window.location.search).get('registered')) {
    showAlert('success', 'Account created. Log in below.');
  }

  loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = document.getElementById('submit-btn');
    if (btn.disabled) return;
    clearErrors(loginForm);

    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;

    let bad = false;
    if (!email) { markError('email', 'Email is required'); bad = true; }
    else if (!EMAIL_RE.test(email)) { markError('email', 'Enter a valid email address'); bad = true; }
    if (!password) { markError('password', 'Password is required'); bad = true; }
    if (bad) return;

    setButtonLoading(btn, true, 'Logging in…');
    try {
      await loginRequest(email, password);
      window.location.href = '/dashboard.html';
    } catch (err) {
      showAlert('danger', err.message || 'Login failed');
      setButtonLoading(btn, false);
      document.getElementById('password').focus();
    }
  });
}

/* ---- Registration ------------------------------------------------------- */
const registerForm = document.getElementById('register-form');
if (registerForm) {
  wirePasswordToggle();
  clearOnInput(registerForm);

  registerForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = document.getElementById('submit-btn');
    if (btn.disabled) return;
    clearErrors(registerForm);

    const name = document.getElementById('name').value.trim();
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const roleEl = registerForm.querySelector('input[name="role"]:checked');

    let bad = false;
    if (!name) { markError('name', 'Name is required'); bad = true; }
    if (!email) { markError('email', 'Email is required'); bad = true; }
    else if (!EMAIL_RE.test(email)) { markError('email', 'Enter a valid email address'); bad = true; }
    if (!password) { markError('password', 'Password is required'); bad = true; }
    else if (password.length < 4) { markError('password', 'Password must be at least 4 characters'); bad = true; }
    if (!roleEl) { markError('role', 'Choose a role'); bad = true; }
    if (bad) return;

    setButtonLoading(btn, true, 'Creating account…');
    try {
      await Api.post('/api/auth/register',
        { name, email, password, role: roleEl.value },
        { allowUnauthenticated: true });
      window.location.href = '/login.html?registered=1';
    } catch (err) {
      if (err.fields) Object.entries(err.fields).forEach(([k, v]) => markError(k, v));
      showAlert('danger', err.message || 'Could not create your account');
      setButtonLoading(btn, false);
    }
  });
}
