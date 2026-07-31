import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import Logo from '../components/ui/Logo';
import Button from '../components/ui/Button';
import PasswordInput from '../components/ui/PasswordInput';
import { IconLock } from '../components/ui/Icons';
import { publicLinksApi } from '../lib/api';

/**
 * Password prompt for a protected short link.
 *
 * Reached only by a redirect from the backend, which sends the visitor here instead
 * of resolving the link. That indirection is what keeps the destination secret: the
 * server never puts it in a response until the password checks out, so it cannot be
 * read out of the page source or a network log by someone who has not unlocked it.
 *
 * On success the browser is sent to the destination with `location.replace`, not
 * `assign` — replacing this page in history means the back button returns the
 * visitor to wherever they came from rather than to a prompt they have already
 * answered.
 */
export default function ProtectedLink() {
  const [searchParams] = useSearchParams();
  const shortCode = searchParams.get('code') ?? '';

  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(null);

    if (!password) {
      setError('Enter the password for this link');
      return;
    }

    setSubmitting(true);
    try {
      const { originalUrl } = await publicLinksApi.unlock(shortCode, password);
      window.location.replace(originalUrl);
    } catch (err) {
      // The server answers every failure identically — wrong password, unknown
      // code, paused link — so that this page cannot be used to probe which short
      // codes exist. Showing its message verbatim preserves that.
      setError(err?.message ?? 'Incorrect password');
      setPassword('');
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen flex-col bg-ink-50/40">
      <header className="container-page flex h-16 items-center">
        <Logo />
      </header>

      <main className="flex flex-1 items-center justify-center px-5 py-10">
        <div className="w-full max-w-md">
          <div className="rounded-2xl border border-ink-200 bg-white p-8 shadow-card">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
              <IconLock className="h-6 w-6" />
            </div>

            <h1 className="mt-5 text-xl font-semibold tracking-tight text-ink-900">
              This link is password protected
            </h1>
            <p className="mt-2.5 text-sm leading-relaxed text-ink-500">
              Enter the password you were given to continue to the destination.
            </p>

            {shortCode && (
              // Rendered as text, never as markup — the value comes straight from
              // the query string.
              <p className="mt-5 rounded-lg bg-ink-50 px-3 py-2.5 font-mono text-sm text-ink-600">
                /{shortCode}
              </p>
            )}

            <form onSubmit={handleSubmit} className="mt-6">
              <PasswordInput
                label="Password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                error={error}
                autoFocus
                maxLength={72}
              />

              <Button type="submit" fullWidth loading={submitting} className="mt-5">
                {submitting ? 'Checking…' : 'Continue'}
              </Button>
            </form>
          </div>

          <p className="mt-6 text-center text-sm text-ink-500">
            Do not have the password?{' '}
            <Link to="/" className="font-medium text-brand-600 hover:underline">
              Go to the homepage
            </Link>
          </p>
        </div>
      </main>
    </div>
  );
}
