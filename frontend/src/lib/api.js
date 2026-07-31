/**
 * Single HTTP client for the whole app.
 *
 * Everything network-related lives here so components never touch fetch directly.
 * That gives one place to attach the bearer token, one place to normalise errors,
 * and one place to handle an expired session — rather than the same try/catch
 * copy-pasted into a dozen components.
 */

/** Empty in dev: the Vite proxy makes /api same-origin, so CORS never applies. */
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

/**
 * Origin that short links are served from.
 *
 * Short links resolve on the backend, not on the React dev server, so this is not
 * necessarily `window.location.origin`. The server is the authority — it builds
 * `shortUrl` on every link it returns — but the Create page has to show a preview of
 * a link that does not exist yet, which is the one case with nothing to read it off.
 *
 * Configured explicitly in production. The fallback swaps the Vite dev port for the
 * backend's, which is only ever correct on a developer's machine and is why the
 * variable exists.
 */
export function shortLinkOrigin() {
  const configured = import.meta.env.VITE_SHORT_BASE_URL;
  if (configured) return configured.replace(/\/+$/, '');

  return window.location.origin.replace(':5173', ':8080');
}

const TOKEN_KEY = 'urlshortener.token';
const USER_KEY = 'urlshortener.user';

/**
 * Error carrying the server's structured response.
 *
 * The backend returns a uniform ErrorResponse for every failure, so the UI can
 * show `message` directly and read `fieldErrors` to mark individual inputs
 * instead of guessing from a status code.
 */
export class ApiError extends Error {
  constructor(message, status, fieldErrors) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors ?? null;
  }
}

// ---------------------------------------------------------------------------
// Token storage
// ---------------------------------------------------------------------------

/**
 * Tokens live in localStorage.
 *
 * The trade-off is deliberate and worth stating: localStorage is readable by any
 * script on the page, so it is vulnerable to XSS in a way an httpOnly cookie is
 * not. A cookie would need the backend to set it, plus CSRF protection, which is
 * a different authentication design than the one already built here. Given the
 * API is a stateless bearer-token API, this is the consistent choice — the real
 * mitigation is not injecting untrusted HTML, which this app never does.
 */
export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },
};

export const userStorage = {
  get: () => {
    try {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      // Corrupted entry: treat as signed out rather than crashing on boot.
      return null;
    }
  },
  set: (user) => localStorage.setItem(USER_KEY, JSON.stringify(user)),
};

// ---------------------------------------------------------------------------
// Core request
// ---------------------------------------------------------------------------

/**
 * Fires a request and normalises the outcome.
 *
 * @param {string} path            path beginning with '/'
 * @param {object} [options]
 * @param {string} [options.method]
 * @param {object} [options.body]  serialised as JSON when present
 * @param {boolean} [options.auth] attach the bearer token (default true)
 * @param {boolean} [options.raw]  resolve to a Blob instead of JSON (for the QR PNG)
 */
async function request(path, { method = 'GET', body, auth = true, raw = false } = {}) {
  const headers = {};

  if (body !== undefined) headers['Content-Type'] = 'application/json';

  if (auth) {
    const token = tokenStorage.get();
    if (token) headers.Authorization = `Bearer ${token}`;
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    // fetch only rejects on a genuine network failure, never on a 4xx/5xx.
    throw new ApiError(
      'Could not reach the server. Check your connection and try again.',
      0,
    );
  }

  if (response.status === 401 && auth) {
    // The token is gone or expired. Clear it so the app stops pretending to be
    // signed in, and let the router send the user to /login.
    tokenStorage.clear();
    window.dispatchEvent(new Event('auth:expired'));
    throw new ApiError('Your session has expired. Please sign in again.', 401);
  }

  if (!response.ok) {
    throw await toApiError(response);
  }

  if (raw) return response.blob();

  // 204 No Content has no body to parse.
  if (response.status === 204) return null;

  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

/** Builds an ApiError, falling back gracefully if the body is not our JSON shape. */
async function toApiError(response) {
  try {
    const payload = await response.json();
    return new ApiError(
      payload.message || `Request failed (${response.status})`,
      response.status,
      payload.fieldErrors,
    );
  } catch {
    return new ApiError(`Request failed (${response.status})`, response.status);
  }
}

// ---------------------------------------------------------------------------
// Endpoints
// ---------------------------------------------------------------------------

export const authApi = {
  register: (payload) =>
    request('/api/auth/register', { method: 'POST', body: payload, auth: false }),

  login: (payload) =>
    request('/api/auth/login', { method: 'POST', body: payload, auth: false }),

  me: () => request('/api/auth/me'),
};

export const linksApi = {
  list: () => request('/api/urls'),

  stats: () => request('/api/urls/stats'),

  create: (payload) => request('/api/urls', { method: 'POST', body: payload }),

  update: (id, payload) => request(`/api/urls/${id}`, { method: 'PUT', body: payload }),

  toggle: (id) => request(`/api/urls/${id}/toggle`, { method: 'PATCH' }),

  remove: (id) => request(`/api/urls/${id}`, { method: 'DELETE' }),

  analytics: (shortCode, limit = 100) =>
    request(`/api/urls/${encodeURIComponent(shortCode)}/analytics?limit=${limit}`),

  summary: (shortCode, days = 30) =>
    request(`/api/urls/${encodeURIComponent(shortCode)}/summary?days=${days}`),

  /**
   * QR codes come back as PNG bytes on an authenticated endpoint, so an
   * `<img src>` cannot fetch them — an image tag carries no Authorization header.
   * Fetching as a Blob and wrapping it in an object URL is what makes it work.
   * Callers must revokeObjectURL when done, or the blob leaks for the session.
   */
  qrCodeBlob: (shortCode, size) => {
    const query = size ? `?size=${size}` : '';
    return request(`/api/urls/${encodeURIComponent(shortCode)}/qr${query}`, { raw: true });
  },
};

/**
 * The one endpoint a visitor without an account can call.
 *
 * `auth: false` is not an optimisation — it is required. Someone opening a shared
 * link is not the owner, and sending a stale token from a previous session would
 * get the request rejected as a 401 before it ever reached the unlock logic.
 */
export const publicLinksApi = {
  unlock: (shortCode, password) =>
    request(`/api/public/links/${encodeURIComponent(shortCode)}/unlock`, {
      method: 'POST',
      body: { password },
      auth: false,
    }),
};
