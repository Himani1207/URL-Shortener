import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import Logo from '../components/ui/Logo';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import { ApiError } from '../lib/api';
import { useAuth } from '../context/AuthContext';

/**
 * Sign-in page.
 *
 * After a successful login the user is sent to wherever they were originally
 * heading — ProtectedRoute stores that path in location state — falling back to
 * the dashboard. Always landing on the dashboard would lose the destination of
 * anyone who followed a deep link into the app.
 */
export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});

  const destination = location.state?.from ?? '/dashboard';

  const handleSubmit = async (event) => {
    event.preventDefault();
    setFormError(null);
    setFieldErrors({});
    setSubmitting(true);

    try {
      await login({ email: email.trim(), password });
      navigate(destination, { replace: true });
    } catch (error) {
      if (error instanceof ApiError && error.fieldErrors) {
        setFieldErrors(error.fieldErrors);
      } else {
        // The API returns one message for both "no such account" and "wrong
        // password" by design, so it is shown as a form-level error rather than
        // being attributed to a specific field.
        setFormError(error.message ?? 'Could not sign you in');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthShell
      title="Welcome back"
      subtitle="Sign in to manage your links and see how they are doing."
      footer={
        <>
          Don&apos;t have an account?{' '}
          <Link to="/register" className="rounded font-medium text-brand-600 hover:text-brand-700">
            Create one
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-5" noValidate>
        {formError && (
          <div
            role="alert"
            className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
          >
            {formError}
          </div>
        )}

        <Input
          label="Email"
          type="email"
          autoComplete="email"
          required
          placeholder="you@company.com"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          error={fieldErrors.email}
        />

        <Input
          label="Password"
          type="password"
          autoComplete="current-password"
          required
          placeholder="••••••••"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          error={fieldErrors.password}
        />

        <Button type="submit" fullWidth loading={submitting}>
          {submitting ? 'Signing in…' : 'Sign in'}
        </Button>
      </form>
    </AuthShell>
  );
}

/**
 * Shared frame for the login and register pages.
 *
 * Exported so both pages stay pixel-identical — the card would otherwise drift
 * between them as one gets adjusted and the other does not.
 */
export function AuthShell({ title, subtitle, children, footer }) {
  return (
    <div className="flex min-h-screen flex-col bg-ink-50/40">
      <header className="container-page flex h-16 items-center">
        <Logo />
      </header>

      <main className="flex flex-1 items-center justify-center px-5 py-10">
        <div className="w-full max-w-md">
          <div className="rounded-2xl border border-ink-200 bg-white p-8 shadow-card">
            <h1 className="text-2xl font-semibold tracking-tight text-ink-900">{title}</h1>
            <p className="mt-2 text-sm text-ink-500">{subtitle}</p>

            <div className="mt-7">{children}</div>
          </div>

          <p className="mt-6 text-center text-sm text-ink-500">{footer}</p>
        </div>
      </main>
    </div>
  );
}
