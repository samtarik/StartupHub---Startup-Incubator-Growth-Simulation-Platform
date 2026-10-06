/* Thin fetch wrapper: same-origin session cookies, JSON in and out, and a
   401 bounce to the login page unless the caller opts out.

   Errors are thrown as ApiError so callers can read err.fields and attach a
   message to the exact input the backend complained about. */

class ApiError extends Error {
  constructor(message, status, fields) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fields = fields || null;
  }
}

const Api = {
  async request(method, url, body, opts = {}) {
    let res;
    try {
      res = await fetch(url, {
        method,
        credentials: 'include',
        headers: body !== undefined ? { 'Content-Type': 'application/json' } : {},
        body: body !== undefined ? JSON.stringify(body) : undefined,
      });
    } catch (networkError) {
      throw new ApiError('Cannot reach the server. Check that StartupHub is still running.', 0);
    }

    if (res.status === 401 && !opts.allowUnauthenticated) {
      window.location.href = '/login.html';
      return Promise.reject(new ApiError('Your session has expired.', 401));
    }

    if (!res.ok) {
      let message = `Request failed (${res.status})`;
      let fields = null;
      try {
        const data = await res.json();
        if (data) {
          message = data.message || message;
          fields = data.fields || null;
        }
      } catch (_) { /* error body was not JSON */ }
      throw new ApiError(message, res.status, fields);
    }

    if (res.status === 204) return null;
    const text = await res.text();
    return text ? JSON.parse(text) : null;
  },
  get(url, opts) { return this.request('GET', url, undefined, opts); },
  post(url, body, opts) { return this.request('POST', url, body ?? {}, opts); },
  put(url, body, opts) { return this.request('PUT', url, body ?? {}, opts); },
  patch(url, body, opts) { return this.request('PATCH', url, body ?? {}, opts); },
  del(url, opts) { return this.request('DELETE', url, undefined, opts); },
};

/* Login goes through Spring Security's form-login filter, which reads standard
   form parameters rather than JSON - so this one call is x-www-form-urlencoded. */
async function loginRequest(email, password) {
  const res = await fetch('/api/auth/login', {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ email, password }),
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new ApiError(data.message || 'Login failed', res.status);
  return data;
}

async function logoutRequest() {
  await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' });
  window.location.href = '/login.html';
}
