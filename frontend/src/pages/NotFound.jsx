import { Link } from 'react-router-dom';
import Logo from '../components/ui/Logo';
import Button from '../components/ui/Button';

/**
 * Client-side 404 for an unmatched frontend route.
 *
 * Distinct from {@link LinkUnavailable}, which handles a short link that failed to
 * resolve. This one means the app itself has no such page — a mistyped dashboard
 * URL, or a stale bookmark.
 */
export default function NotFound() {
  return (
    <div className="flex min-h-screen flex-col bg-ink-50/40">
      <header className="container-page flex h-16 items-center">
        <Logo />
      </header>

      <main className="flex flex-1 items-center justify-center px-5 py-10">
        <div className="w-full max-w-md text-center">
          <p className="font-mono text-sm font-semibold text-brand-600">404</p>
          <h1 className="mt-3 text-2xl font-semibold tracking-tight text-ink-900">
            Page not found
          </h1>
          <p className="mt-3 text-sm leading-relaxed text-ink-500">
            The page you are looking for does not exist, or it may have moved.
          </p>

          <div className="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
            <Link to="/">
              <Button variant="secondary" fullWidth>
                Go to homepage
              </Button>
            </Link>
            <Link to="/dashboard">
              <Button fullWidth>Go to dashboard</Button>
            </Link>
          </div>
        </div>
      </main>
    </div>
  );
}
