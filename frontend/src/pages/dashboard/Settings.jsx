import { useNavigate } from 'react-router-dom';
import PageHeader from '../../components/layout/PageHeader';
import Card, { CardHeader } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import { useDashboardStats } from '../../hooks/useLinks';
import { useAuth } from '../../context/AuthContext';
import { formatCount } from '../../lib/format';

/**
 * Account settings.
 *
 * Profile fields are read-only, and deliberately so: the API exposes no update or
 * delete endpoint for a user. Rendering editable inputs backed by nothing would be
 * worse than showing the truth — the user would type, save, and find their change
 * silently discarded. Each section says plainly what is not available yet.
 */
export default function Settings() {
  const { user, logout } = useAuth();
  const { stats } = useDashboardStats();
  const navigate = useNavigate();

  const handleSignOut = () => {
    logout();
    navigate('/', { replace: true });
  };

  return (
    <>
      <PageHeader title="Settings" description="Your account details and usage." />

      <div className="space-y-6">
        <Card>
          <CardHeader
            title="Profile"
            description="Editing your profile is not available yet."
          />

          <div className="mt-6 grid gap-5 sm:grid-cols-2">
            <Input label="Name" value={user?.name ?? ''} readOnly disabled />
            <Input label="Email" value={user?.email ?? ''} readOnly disabled />
          </div>
        </Card>

        <Card>
          <CardHeader title="Usage" description="Your account totals." />

          <dl className="mt-6 grid gap-6 sm:grid-cols-3">
            {[
              ['Total links', stats?.totalLinks],
              ['Active links', stats?.activeLinks],
              ['Total clicks', stats?.totalClicks],
            ].map(([label, value]) => (
              <div key={label}>
                <dt className="text-sm text-ink-500">{label}</dt>
                <dd className="mt-1 text-2xl font-semibold tabular-nums text-ink-900">
                  {value === undefined || value === null ? '—' : formatCount(value)}
                </dd>
              </div>
            ))}
          </dl>

          <div className="mt-6 rounded-lg bg-ink-50 px-4 py-3">
            <p className="text-sm text-ink-600">
              You are on the <strong className="font-medium text-ink-900">Free</strong> plan.
              Billing is not connected in this build.
            </p>
          </div>
        </Card>

        <Card>
          <CardHeader
            title="API access"
            description="The same API this dashboard uses is documented and open to your account."
          />

          <p className="mt-4 text-sm leading-relaxed text-ink-600">
            Authenticate with <code className="rounded bg-ink-100 px-1.5 py-0.5 font-mono text-xs">
            POST /api/auth/login</code> and send the returned token as{' '}
            <code className="rounded bg-ink-100 px-1.5 py-0.5 font-mono text-xs">
            Authorization: Bearer &lt;token&gt;</code> on every request.
          </p>

          <a href="/swagger-ui.html" target="_blank" rel="noreferrer" className="mt-5 inline-block">
            <Button variant="secondary">Open API documentation</Button>
          </a>
        </Card>

        <Card>
          <CardHeader
            title="Session"
            description="Signing out discards your token on this device. The API is stateless, so there is no server session to end."
          />
          <div className="mt-5">
            <Button variant="danger" onClick={handleSignOut}>
              Sign out
            </Button>
          </div>
        </Card>
      </div>
    </>
  );
}
