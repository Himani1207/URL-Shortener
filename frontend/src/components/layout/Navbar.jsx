import { useState } from 'react';
import { Link } from 'react-router-dom';
import Logo from '../ui/Logo';
import Button from '../ui/Button';
import { IconMenu } from '../ui/Icons';
import { useAuth } from '../../context/AuthContext';

/**
 * Marketing-site navigation.
 *
 * The right-hand side swaps between "Log in / Get started" and a single
 * "Dashboard" link depending on session state — asking someone who is already
 * signed in to log in again is a small thing that reads as unfinished.
 */
export default function Navbar() {
  const { isAuthenticated } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);

  const sections = [
    { label: 'Features', href: '#features' },
    { label: 'How it works', href: '#how-it-works' },
    { label: 'Pricing', href: '#pricing' },
  ];

  return (
    <header className="sticky top-0 z-40 border-b border-ink-200 bg-paper/90 backdrop-blur-sm">
      <nav className="container-page flex h-14 items-center justify-between">
        <div className="flex items-center gap-10">
          <Logo />

          <ul className="hidden items-center gap-7 md:flex">
            {sections.map((section) => (
              <li key={section.href}>
                <a
                  href={section.href}
                  className="rounded text-sm text-ink-500 transition-colors hover:text-ink-900"
                >
                  {section.label}
                </a>
              </li>
            ))}
          </ul>
        </div>

        <div className="hidden items-center gap-3 md:flex">
          {isAuthenticated ? (
            <Link to="/dashboard">
              <Button size="sm">Go to dashboard</Button>
            </Link>
          ) : (
            <>
              <Link
                to="/login"
                className="rounded px-2 py-1 text-sm text-ink-500 transition-colors hover:text-ink-900"
              >
                Log in
              </Link>
              <Link to="/register">
                <Button size="sm">Get started</Button>
              </Link>
            </>
          )}
        </div>

        <button
          type="button"
          className="rounded p-2 text-ink-600 transition hover:text-ink-900 md:hidden"
          onClick={() => setMobileOpen((open) => !open)}
          aria-expanded={mobileOpen}
          aria-label="Toggle navigation menu"
        >
          <IconMenu />
        </button>
      </nav>

      {mobileOpen && (
        <div className="border-t border-ink-200 bg-paper md:hidden">
          <div className="container-page flex flex-col gap-1 py-4">
            {sections.map((section) => (
              <a
                key={section.href}
                href={section.href}
                onClick={() => setMobileOpen(false)}
                className="rounded px-3 py-2.5 text-sm text-ink-600 transition hover:bg-ink-100 hover:text-ink-900"
              >
                {section.label}
              </a>
            ))}

            <div className="mt-3 flex flex-col gap-2 border-t border-ink-200 pt-4">
              {isAuthenticated ? (
                <Link to="/dashboard" onClick={() => setMobileOpen(false)}>
                  <Button fullWidth>Go to dashboard</Button>
                </Link>
              ) : (
                <>
                  <Link to="/login" onClick={() => setMobileOpen(false)}>
                    <Button variant="secondary" fullWidth>
                      Log in
                    </Button>
                  </Link>
                  <Link to="/register" onClick={() => setMobileOpen(false)}>
                    <Button fullWidth>Get started</Button>
                  </Link>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </header>
  );
}
