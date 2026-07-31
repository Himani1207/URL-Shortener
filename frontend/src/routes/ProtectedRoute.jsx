import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { LoadingState } from '../components/ui/States';

/**
 * Gate for signed-in routes.
 *
 * Two details that matter:
 *
 *   1. It waits for `loading`. On a hard refresh the token is present but not yet
 *      validated; redirecting during that window would bounce a legitimately
 *      signed-in user to /login on every reload.
 *
 *   2. It records the attempted path in location state, so after signing in the
 *      user lands where they were going rather than always on the dashboard.
 *      `replace` keeps the guard out of history, so Back does not walk into it.
 *
 * This is a UX gate, not a security boundary — the API authorises every request
 * independently. Removing it client-side gains an attacker nothing.
 */
export default function ProtectedRoute({ children }) {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <LoadingState label="Checking your session…" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }

  return children;
}
