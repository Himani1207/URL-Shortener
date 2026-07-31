import { Suspense, useEffect, useRef, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import Logo from '../ui/Logo';
import { LoadingState } from '../ui/States';
import { IconClose, IconLogout, IconMenu } from '../ui/Icons';
import { useAuth } from '../../context/AuthContext';

/**
 * Shell for every signed-in page: one top navigation bar, then content.
 *
 * <b>Why a top bar and not a sidebar.</b> A 256px rail costs the same width on every
 * page whether or not that page needs it, and the pages here are wide ones — a links
 * table and a two-column create form. Moving navigation to a 56px-tall bar returns
 * that width to the content, which is what lets the create page go two-column at
 * around 1100px instead of needing 1280px.
 *
 * Navigation is plain text with a rule under the active item. No icons: six labels
 * do not need pictograms to be told apart, and an icon per row is decoration that
 * has to be redrawn every time the vocabulary changes.
 *
 * There is deliberately no "Dashboard" entry. Analytics already answers the
 * questions an overview page would, and two routes competing to be the home screen
 * only makes people wonder which one is authoritative.
 *
 * Uses react-router's <Outlet>, so the bar mounts once and stays mounted while the
 * content swaps — navigating does not re-render or re-animate the chrome.
 */
const NAV_ITEMS = [
  { to: '/dashboard/create', label: 'Create' },
  { to: '/dashboard/links', label: 'Links' },
  { to: '/dashboard/analytics', label: 'Analytics' },
  { to: '/dashboard/qr-codes', label: 'QR codes' },
  { to: '/dashboard/settings', label: 'Settings' },
];

export default function DashboardLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [menuOpen, setMenuOpen] = useState(false);
  const [accountOpen, setAccountOpen] = useState(false);
  const accountRef = useRef(null);

  /**
   * Closes both menus on any navigation.
   *
   * The onClick handlers cover taps, but not the browser back button or a redirect —
   * either of which would otherwise leave a menu covering the page just landed on.
   */
  useEffect(() => {
    setMenuOpen(false);
    setAccountOpen(false);
  }, [location.pathname]);

  /** Escape closes the mobile sheet, and the page behind it must not scroll. */
  useEffect(() => {
    if (!menuOpen) return undefined;

    const onKeyDown = (event) => {
      if (event.key === 'Escape') setMenuOpen(false);
    };

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    document.addEventListener('keydown', onKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [menuOpen]);

  /** A dropdown that ignores clicks elsewhere is a dropdown that gets stuck open. */
  useEffect(() => {
    if (!accountOpen) return undefined;

    const onPointerDown = (event) => {
      if (!accountRef.current?.contains(event.target)) setAccountOpen(false);
    };
    const onKeyDown = (event) => {
      if (event.key === 'Escape') setAccountOpen(false);
    };

    document.addEventListener('mousedown', onPointerDown);
    document.addEventListener('keydown', onKeyDown);

    return () => {
      document.removeEventListener('mousedown', onPointerDown);
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [accountOpen]);

  const handleLogout = () => {
    logout();
    navigate('/', { replace: true });
  };

  const initial = user?.name?.charAt(0)?.toUpperCase() ?? '?';

  return (
    <div className="min-h-screen bg-paper">
      <header className="sticky top-0 z-40 border-b border-ink-200 bg-paper/90 backdrop-blur-sm">
        <div className="mx-auto flex h-14 w-full max-w-[66rem] items-center gap-6 px-5 sm:px-8">
          <Logo to="/dashboard/create" />

          {/* Desktop navigation. The active rule is drawn on the bar's own bottom
              border, which is why the link carries the full height. */}
          <nav className="hidden h-full items-center gap-1 md:flex" aria-label="Main">
            {NAV_ITEMS.map(({ to, label }) => (
              <NavLink
                key={to}
                to={to}
                className={({ isActive }) =>
                  [
                    'relative flex h-full items-center px-3 text-sm transition-colors duration-150',
                    // -1px pulls the marker onto the header's border so the two read
                    // as one line rather than as two rules a hair apart.
                    'after:absolute after:inset-x-3 after:-bottom-px after:h-0.5 after:transition-colors',
                    isActive
                      ? 'font-semibold text-ink-900 after:bg-brand-600'
                      : 'text-ink-500 after:bg-transparent hover:text-ink-900',
                  ].join(' ')
                }
              >
                {label}
              </NavLink>
            ))}
          </nav>

          <div className="ml-auto flex items-center gap-1">
            {/* Account menu, desktop only — on mobile the same actions live in the
                sheet, where there is room to show the email in full. */}
            <div ref={accountRef} className="relative hidden md:block">
              <button
                type="button"
                onClick={() => setAccountOpen((open) => !open)}
                className="flex items-center gap-2 rounded px-2 py-1.5 text-sm text-ink-600 transition-colors hover:text-ink-900"
                aria-expanded={accountOpen}
                aria-haspopup="menu"
              >
                <span
                  className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-brand-600 text-xs font-semibold text-white"
                  aria-hidden="true"
                >
                  {initial}
                </span>
                <span className="max-w-[10rem] truncate">{user?.name}</span>
              </button>

              {accountOpen && (
                <div
                  role="menu"
                  className="absolute right-0 top-full z-50 mt-2 w-64 animate-slide-down rounded-md border border-ink-200 bg-white p-1 shadow-dropdown"
                >
                  <div className="border-b border-ink-100 px-3 py-2.5">
                    <p className="truncate text-sm font-medium text-ink-900">{user?.name}</p>
                    <p className="truncate text-xs text-ink-500">{user?.email}</p>
                  </div>
                  <button
                    type="button"
                    role="menuitem"
                    onClick={handleLogout}
                    className="mt-1 flex w-full items-center gap-2.5 rounded px-3 py-2 text-sm text-ink-600 transition-colors hover:bg-ink-100 hover:text-ink-900"
                  >
                    <IconLogout className="h-[18px] w-[18px] shrink-0 text-ink-400" />
                    Sign out
                  </button>
                </div>
              )}
            </div>

            <button
              type="button"
              onClick={() => setMenuOpen(true)}
              className="-mr-2 rounded p-2 text-ink-600 transition-colors hover:text-ink-900 active:scale-95 md:hidden"
              aria-label="Open navigation menu"
              aria-expanded={menuOpen}
            >
              <IconMenu />
            </button>
          </div>
        </div>
      </header>

      {/* Mobile sheet. Comes down from the top rather than in from the side: the
          trigger is in the top bar, so the panel appears where the finger already is. */}
      {menuOpen && (
        <div className="fixed inset-0 z-50 md:hidden">
          <div
            className="absolute inset-0 animate-fade-in bg-ink-900/30"
            onClick={() => setMenuOpen(false)}
            aria-hidden="true"
          />
          <div className="relative animate-slide-down border-b border-ink-200 bg-paper">
            <div className="flex h-14 items-center justify-between px-5">
              <Logo to="/dashboard/create" />
              <button
                type="button"
                onClick={() => setMenuOpen(false)}
                className="-mr-2 rounded p-2 text-ink-600 transition-colors hover:text-ink-900"
                aria-label="Close navigation menu"
              >
                <IconClose />
              </button>
            </div>

            <nav className="border-t border-ink-100 px-3 py-2" aria-label="Main">
              {NAV_ITEMS.map(({ to, label }) => (
                <NavLink
                  key={to}
                  to={to}
                  className={({ isActive }) =>
                    [
                      'flex items-center rounded px-3 py-3 text-sm transition-colors',
                      isActive
                        ? 'font-semibold text-ink-900'
                        : 'text-ink-600 hover:bg-ink-100 hover:text-ink-900',
                    ].join(' ')
                  }
                >
                  {({ isActive }) => (
                    <>
                      {/* A rule to the left of the label, so the current page is
                          identifiable without depending on weight alone. */}
                      <span
                        className={`mr-3 h-4 w-0.5 shrink-0 ${isActive ? 'bg-brand-600' : 'bg-transparent'}`}
                        aria-hidden="true"
                      />
                      {label}
                    </>
                  )}
                </NavLink>
              ))}
            </nav>

            <div className="border-t border-ink-100 px-6 py-4">
              <p className="truncate text-sm font-medium text-ink-900">{user?.name}</p>
              <p className="truncate text-xs text-ink-500">{user?.email}</p>
              <button
                type="button"
                onClick={handleLogout}
                className="mt-3 flex items-center gap-2.5 text-sm text-ink-600 transition-colors hover:text-ink-900"
              >
                <IconLogout className="h-[18px] w-[18px] shrink-0 text-ink-400" />
                Sign out
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Capped narrower than the old sidebar layout allowed. Content that runs the
          full width of a large monitor reads as a report, not a page. */}
      <main className="mx-auto w-full max-w-[66rem] px-5 py-8 sm:px-8 sm:py-10">
        {/* Route-level code splitting means a page can arrive a frame late. */}
        <Suspense fallback={<LoadingState label="Loading…" />}>
          <Outlet />
        </Suspense>
      </main>
    </div>
  );
}
