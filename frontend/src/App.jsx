import { Suspense, lazy } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';

import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import ProtectedRoute from './routes/ProtectedRoute';
import ScrollToTop from './routes/ScrollToTop';
import { LoadingState } from './components/ui/States';

import DashboardLayout from './components/layout/DashboardLayout';

// Eager: the landing page is the entry point for most visits, so lazy-loading it
// would only add a round trip before anything renders.
import Landing from './pages/Landing';
import Login from './pages/Login';
import Register from './pages/Register';

/**
 * Dashboard routes are code-split.
 *
 * They are only reachable after signing in, so their JavaScript has no business
 * being in the bundle a first-time visitor downloads to read the landing page.
 * Splitting here roughly halves the initial payload, which matters most on the
 * slow connections where it is hardest to notice during development.
 */
// There is no Dashboard route. Analytics is the overview — a second summary page
// alongside it only raised the question of which one was authoritative.
const CreateLink = lazy(() => import('./pages/dashboard/CreateLink'));
const Links = lazy(() => import('./pages/dashboard/Links'));
const Analytics = lazy(() => import('./pages/dashboard/Analytics'));
const AnalyticsDetail = lazy(() => import('./pages/dashboard/AnalyticsDetail'));
const QrCodes = lazy(() => import('./pages/dashboard/QrCodes'));
const Settings = lazy(() => import('./pages/dashboard/Settings'));

const LinkUnavailable = lazy(() => import('./pages/LinkUnavailable'));
const ProtectedLink = lazy(() => import('./pages/ProtectedLink'));
const NotFound = lazy(() => import('./pages/NotFound'));

/**
 * Route table and global providers.
 *
 * Provider order is deliberate: ToastProvider wraps AuthProvider so that auth
 * failures can raise a toast, and both sit inside BrowserRouter so anything below
 * can navigate.
 *
 * The dashboard routes are nested under a single <DashboardLayout>, which means
 * the sidebar mounts once and stays mounted while the content swaps. Wrapping each
 * page in its own layout instead would remount and re-animate the chrome on every
 * navigation.
 *
 * The guard is applied to the layout rather than to each child, so a new dashboard
 * page cannot be added unprotected by accident.
 */
export default function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <AuthProvider>
          <ScrollToTop />

          <Suspense fallback={<FullPageLoader />}>
            <Routes>
              {/* ---------------- Public ---------------- */}
              <Route path="/" element={<Landing />} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />

              {/* Where RedirectController sends a link it could not serve. */}
              <Route path="/link-unavailable" element={<LinkUnavailable />} />

              {/* Where RedirectController sends a password-protected link. Public
                  by necessity: the visitor is the recipient, not the owner. */}
              <Route path="/protected" element={<ProtectedLink />} />

              {/* ---------------- Dashboard ---------------- */}
              <Route
                path="/dashboard"
                element={
                  <ProtectedRoute>
                    <DashboardLayout />
                  </ProtectedRoute>
                }
              >
                {/* Creating is why people open the product, so it is what the
                    dashboard root resolves to. */}
                <Route index element={<Navigate to="/dashboard/create" replace />} />
                {/* The single place a link is created; every "new link" affordance
                    in the app routes here rather than opening its own dialog. */}
                <Route path="create" element={<CreateLink />} />
                <Route path="links" element={<Links />} />
                <Route path="analytics" element={<Analytics />} />
                {/* Per-link detail — the "separate page" for analytics. */}
                <Route path="analytics/:shortCode" element={<AnalyticsDetail />} />
                <Route path="qr-codes" element={<QrCodes />} />
                <Route path="settings" element={<Settings />} />
                {/* Unknown dashboard path: back to the dashboard root rather than
                    a 404, which would drop the user out of the app shell. */}
                <Route path="*" element={<Navigate to="/dashboard" replace />} />
              </Route>

              <Route path="*" element={<NotFound />} />
            </Routes>
          </Suspense>
        </AuthProvider>
      </ToastProvider>
    </BrowserRouter>
  );
}

/** Shown only while a lazily-loaded route chunk is in flight. */
function FullPageLoader() {
  return (
    <div className="flex min-h-screen items-center justify-center">
      <LoadingState label="Loading…" />
    </div>
  );
}
