import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { authApi, tokenStorage, userStorage } from '../lib/api';

/**
 * Authentication state for the app.
 *
 * Context rather than prop drilling: the navbar, sidebar, protected routes and
 * settings page all need the current user, and they sit at completely different
 * depths of the tree.
 *
 * The `loading` flag exists to prevent a specific bug. On a hard refresh the token
 * is in localStorage but not yet verified. Without a loading state the router would
 * briefly see `user === null`, bounce a signed-in user to /login, and only then
 * finish validating — a visible flash on every reload.
 */
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => userStorage.get());
  const [loading, setLoading] = useState(() => Boolean(tokenStorage.get()));

  /**
   * Validates a restored token once on mount.
   *
   * A token in storage proves nothing — it may be expired or belong to a deleted
   * account. /api/auth/me is the cheap way to find out before rendering the app
   * as signed in.
   *
   * <b>Only a 401 signs the user out.</b> The previous version cleared the token on
   * any rejection, which meant a brief network blip or a backend that had not
   * finished booting logged the user out and made them re-enter their password —
   * for a token that was perfectly valid. On anything other than a 401 the cached
   * profile is kept and the session continues; the next real request will surface
   * the problem if it is genuine.
   */
  useEffect(() => {
    if (!tokenStorage.get()) {
      setLoading(false);
      return undefined;
    }

    let cancelled = false;

    authApi
      .me()
      .then((profile) => {
        if (cancelled) return;
        setUser(profile);
        userStorage.set(profile);
      })
      .catch((error) => {
        if (cancelled) return;

        if (error?.status === 401) {
          tokenStorage.clear();
          setUser(null);
        }
        // Otherwise: network error, 5xx, or the API being down. Keep the session
        // and let the user carry on.
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  /**
   * The API client dispatches `auth:expired` when any request comes back 401,
   * which is how a session that lapses mid-use gets noticed without every
   * component having to check.
   */
  useEffect(() => {
    const onExpired = () => setUser(null);
    window.addEventListener('auth:expired', onExpired);
    return () => window.removeEventListener('auth:expired', onExpired);
  }, []);

  const applySession = useCallback((response) => {
    tokenStorage.set(response.token);
    userStorage.set(response.user);
    setUser(response.user);
    return response.user;
  }, []);

  const login = useCallback(
    async (credentials) => applySession(await authApi.login(credentials)),
    [applySession],
  );

  const register = useCallback(
    async (details) => applySession(await authApi.register(details)),
    [applySession],
  );

  const logout = useCallback(() => {
    // Purely client-side: the API is stateless, so there is no server session to
    // end. Discarding the token is the whole of signing out.
    tokenStorage.clear();
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({ user, loading, isAuthenticated: Boolean(user), login, register, logout }),
    [user, loading, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside an <AuthProvider>');
  }
  return context;
}
