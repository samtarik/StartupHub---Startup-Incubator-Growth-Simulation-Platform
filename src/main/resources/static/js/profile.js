/* Profile and settings.

   Name changes go to PUT /api/auth/me; the password form calls
   POST /api/auth/change-password, which verifies the current password against
   the BCrypt hash on the server before replacing it. */

let profileUser = null;

async function init() {
  profileUser = await Api.get('/api/auth/me');
  renderShell(profileUser);
  connectNotifications(profileUser.id);

  paintProfile();
  wireProfileForm();
  wirePasswordForm();
  wireThemeSwitch();

  if (window.location.hash === '#settings') {
    document.getElementById('settings').scrollIntoView({ block: 'start' });
  }
}

function paintProfile() {
  document.getElementById('profile-avatar').textContent = initials(profileUser.name);
  document.getElementById('profile-name').textContent = profileUser.name;
  document.getElementById('profile-email').textContent = profileUser.email;
  document.getElementById('profile-role').textContent = Fmt.titleCase(profileUser.role);
  document.getElementById('name').value = profileUser.name;
  document.getElementById('email-readonly').value = profileUser.email;
  document.getElementById('role-readonly').value = Fmt.titleCase(profileUser.role);
}

function clearFormErrors(form) {
  form.querySelectorAll('.field.invalid').forEach(el => el.classList.remove('invalid'));
  form.querySelectorAll('.field-error').forEach(el => (el.textContent = ''));
}

function wireProfileForm() {
  const form = document.getElementById('profile-form');
  const btn = document.getElementById('save-profile-btn');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (btn.disabled) return;
    clearFormErrors(form);

    const name = document.getElementById('name').value.trim();
    if (!name) { attachFieldError(form, 'name', 'Name is required'); return; }
    if (name === profileUser.name) { toast('Nothing to save'); return; }

    setButtonLoading(btn, true, 'Saving…');
    try {
      profileUser = await Api.put('/api/auth/me', { name });
      paintProfile();
      renderShell(profileUser);   // the avatar and menu carry the name too
      toastSuccess('Profile updated');
    } catch (err) {
      if (err.fields) Object.entries(err.fields).forEach(([k, v]) => attachFieldError(form, k, v));
      toastError(err.message || 'Could not save your profile');
    } finally {
      setButtonLoading(btn, false);
    }
  });
}

function wirePasswordForm() {
  const form = document.getElementById('password-form');
  const btn = document.getElementById('save-password-btn');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (btn.disabled) return;
    clearFormErrors(form);

    const currentPassword = document.getElementById('currentPassword').value;
    const newPassword = document.getElementById('newPassword').value;

    let bad = false;
    if (!currentPassword) { attachFieldError(form, 'currentPassword', 'Enter your current password'); bad = true; }
    if (!newPassword) { attachFieldError(form, 'newPassword', 'Enter a new password'); bad = true; }
    else if (newPassword.length < 4) { attachFieldError(form, 'newPassword', 'Password must be at least 4 characters'); bad = true; }
    else if (newPassword === currentPassword) { attachFieldError(form, 'newPassword', 'Choose a password you have not used here'); bad = true; }
    if (bad) return;

    setButtonLoading(btn, true, 'Updating…');
    try {
      await Api.post('/api/auth/change-password', { currentPassword, newPassword });
      form.reset();
      toastSuccess('Password updated');
    } catch (err) {
      if (err.fields) Object.entries(err.fields).forEach(([k, v]) => attachFieldError(form, k, v));
      else attachFieldError(form, 'currentPassword', err.message || 'Could not update the password');
      toastError(err.message || 'Could not update the password');
    } finally {
      setButtonLoading(btn, false);
    }
  });
}

function wireThemeSwitch() {
  const btn = document.getElementById('theme-switch');
  const paint = () => { btn.textContent = Theme.get() === 'dark' ? 'Turn off' : 'Turn on'; };
  paint();
  btn.addEventListener('click', () => { Theme.toggle(); paint(); });
  document.addEventListener('themechange', paint);
}

init().catch(err => {
  if (err && err.status !== 401) toastError(err.message || 'Could not load your profile');
});
