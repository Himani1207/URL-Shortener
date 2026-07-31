import { Link, useSearchParams } from 'react-router-dom';
import Logo from '../components/ui/Logo';
import Button from '../components/ui/Button';

/**
 * Landing page for a short link that cannot be served.
 *
 * {@code RedirectController} redirects here instead of returning a JSON error,
 * because these requests come from a browser address bar rather than from
 * JavaScript — a visitor who follows a shared link should get a sentence, not a
 * response body full of braces.
 *
 * The three reasons are worded differently on purpose. "Expired" and "paused" tell
 * the visitor the link was real and the owner controls it, which is actionable —
 * they can ask for a new one. "Not found" usually means a typo.
 */
const REASONS = {
  expired: {
    title: 'This link has expired',
    body: 'The person who created it set an expiry date, and that date has passed. Ask them for an updated link.',
  },
  inactive: {
    title: 'This link is paused',
    body: 'Its owner has temporarily disabled it. It may start working again later.',
  },
  'not-found': {
    title: 'This link does not exist',
    body: 'Check the address for a typo. Short codes are case-sensitive, so "Abc" and "abc" are different links.',
  },
};

export default function LinkUnavailable() {
  const [searchParams] = useSearchParams();
  const code = searchParams.get('code');
  const reason = searchParams.get('reason') ?? 'not-found';

  const { title, body } = REASONS[reason] ?? REASONS['not-found'];

  return (
    <div className="flex min-h-screen flex-col bg-ink-50/40">
      <header className="container-page flex h-16 items-center">
        <Logo />
      </header>

      <main className="flex flex-1 items-center justify-center px-5 py-10">
        <div className="w-full max-w-md text-center">
          <div className="rounded-2xl border border-ink-200 bg-white p-8 shadow-card">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-amber-50 text-amber-600">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path
                  d="M10.3 13.7a4.2 4.2 0 0 1 0-5.9l2.4-2.4a4.2 4.2 0 0 1 5.9 5.9l-1.2 1.2"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  strokeLinecap="round"
                />
                <path
                  d="M13.7 10.3a4.2 4.2 0 0 0-5.9 0l-2.4 2.4a4.2 4.2 0 0 0 5.9 5.9l1.2-1.2"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  strokeLinecap="round"
                />
                <path d="m4 4 16 16" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
              </svg>
            </div>

            <h1 className="mt-5 text-xl font-semibold tracking-tight text-ink-900">{title}</h1>
            <p className="mt-3 text-sm leading-relaxed text-ink-500">{body}</p>

            {code && (
              // Rendered as text, never as markup — the value comes straight from
              // the query string.
              <p className="mt-5 rounded-lg bg-ink-50 px-3 py-2.5 font-mono text-sm text-ink-600">
                /{code}
              </p>
            )}

            <div className="mt-7 flex flex-col gap-3 sm:flex-row sm:justify-center">
              <Link to="/">
                <Button variant="secondary" fullWidth>
                  Go to homepage
                </Button>
              </Link>
              <Link to="/register">
                <Button fullWidth>Create your own links</Button>
              </Link>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
