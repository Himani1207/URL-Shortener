import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import { ApiError } from '../lib/api';
import { useAuth } from '../context/AuthContext';
import { AuthShell } from './Login';

/**
 * Registration page.
 *
 * If the visitor arrived from the landing page's hero form, the URL they were
 * trying to shorten travels in location state and is handed to the dashboard,
 * which creates it immediately after signup. Making someone paste the same link
 * twice is a small thing that reads as sloppy.
 *
 * Password confirmation is validated client-side only: the API has no such field,
 * and a mismatch is a typo rather than a server concern.
 */
export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});

  const pendingUrl = location.state?.pendingUrl ?? null;

  const handleSubmit = async (event) => {
    event.preventDefault();
    setFormError(null);
    setFieldErrors({});

    if (password !== confirmPassword) {
      setFieldErrors({ confirmPassword: 'Passwords do not match' });
      return;
    }
    if (password.length < 8) {
      setFieldErrors({ password: 'Use at least 8 characters' });
      return;
    }

    setSubmitting(true);
    try {
      await register({ name: name.trim(), email: email.trim(), password });

      // Someone who arrived with a URL in hand goes straight to the create page
      // with it filled in, rather than having it shortened for them behind their
      // back — they still get to choose an alias, an expiry or a password.
      navigate(pendingUrl ? '/dashboard/create' : '/dashboard', {
        replace: true,
        state: pendingUrl ? { pendingUrl } : undefined,
      });
    } catch (error) {
      if (error instanceof ApiError && error.fieldErrors) {
        setFieldErrors(error.fieldErrors);
      } else {
        setFormError(error.message ?? 'Could not create your account');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthShell
      title="Create your account"
      subtitle="Free to start. No card required."
      footer={
        <>
          Already have an account?{' '}
          <Link to="/login" className="rounded font-medium text-brand-600 hover:text-brand-700">
            Sign in
          </Link>
        </>
      }
    >
      {pendingUrl && (
        <div className="mb-5 rounded-lg border border-brand-200 bg-brand-50 px-4 py-3">
          <p className="text-xs font-medium text-brand-700">Ready to shorten</p>
          <p className="mt-0.5 truncate text-sm text-brand-900">{pendingUrl}</p>
        </div>
      )}

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
          label="Name"
          autoComplete="name"
          required
          placeholder="Your name"
          value={name}
          onChange={(event) => setName(event.target.value)}
          error={fieldErrors.name}
        />

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
          autoComplete="new-password"
          required
          placeholder="At least 8 characters"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          error={fieldErrors.password}
        />

        <Input
          label="Confirm password"
          type="password"
          autoComplete="new-password"
          required
          placeholder="Re-enter your password"
          value={confirmPassword}
          onChange={(event) => setConfirmPassword(event.target.value)}
          error={fieldErrors.confirmPassword}
        />

        <Button type="submit" fullWidth loading={submitting}>
          {submitting ? 'Creating account…' : 'Create account'}
        </Button>

        <p className="text-center text-xs leading-relaxed text-ink-400">
          By creating an account you agree to the Terms of Service and Privacy Policy.
        </p>
      </form>
    </AuthShell>
  );
}
