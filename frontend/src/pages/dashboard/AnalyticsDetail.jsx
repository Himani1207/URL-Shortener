import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import Card, { CardHeader } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Badge from '../../components/ui/Badge';
import StatTile from '../../components/analytics/StatTile';
import ClicksChart from '../../components/analytics/ClicksChart';
import BreakdownList from '../../components/analytics/BreakdownList';
import CopyButton from '../../components/links/CopyButton';
import { EmptyState, ErrorState, LoadingState } from '../../components/ui/States';
import { IconArrowLeft, IconChart, IconLock } from '../../components/ui/Icons';
import { formatDateTime, formatRelative, linkStatus } from '../../lib/format';
import { linksApi, shortLinkOrigin } from '../../lib/api';

/**
 * Per-link analytics.
 *
 * This is the "separate page" the brief asked for — the dashboard stays focused on
 * link management, and the detailed breakdowns live here where someone has
 * deliberately navigated to look at them.
 *
 * The summary, the click history and the link record are fetched in parallel. They
 * are independent endpoints, so awaiting them in sequence would treble the time to
 * first paint for no reason.
 *
 * The link record is what supplies status — active, paused or expired — and whether
 * a password is set. Those live on the link itself rather than in the analytics
 * summary, because they describe the link, not its traffic.
 */
const RANGES = [
  { days: 7, label: '7 days' },
  { days: 30, label: '30 days' },
  { days: 90, label: '90 days' },
];

export default function AnalyticsDetail() {
  const { shortCode } = useParams();

  const [days, setDays] = useState(30);
  const [summary, setSummary] = useState(null);
  const [clicks, setClicks] = useState([]);
  const [link, setLink] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [summaryResult, clicksResult, linksResult] = await Promise.all([
        linksApi.summary(shortCode, days),
        linksApi.analytics(shortCode, 50),
        linksApi.list(),
      ]);
      setSummary(summaryResult);
      setClicks(clicksResult);
      // There is no get-one-by-short-code endpoint; the list is already owner-scoped
      // and small, so filtering it here beats adding a route for one field.
      setLink(linksResult.find((candidate) => candidate.shortCode === shortCode) ?? null);
    } catch (err) {
      setError(err.message ?? 'Could not load analytics for this link');
    } finally {
      setLoading(false);
    }
  }, [shortCode, days]);

  useEffect(() => {
    load();
  }, [load]);

  if (loading && !summary) {
    return <LoadingState label="Loading analytics…" />;
  }

  if (error) {
    return (
      <>
        <BackLink />
        <Card className="mt-6">
          <ErrorState message={error} onRetry={load} />
        </Card>
      </>
    );
  }

  // The server assembles shortUrl from the origin it actually serves redirects on,
  // so prefer it over anything reconstructed on the client.
  const shortUrl = link?.shortUrl ?? `${shortLinkOrigin()}/${shortCode}`;
  const status = link ? linkStatus(link) : null;

  return (
    <>
      <BackLink />

      <div className="mb-6 mt-4 flex flex-col gap-4 sm:mb-8 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="truncate font-mono text-xl font-semibold tracking-tight text-ink-900 sm:text-2xl">
              /{shortCode}
            </h1>

            {/* Status belongs beside the identity of the link, not buried in a
                tile — an expired link explains a flat chart at a glance. */}
            {status && <Badge tone={status.tone}>{status.label}</Badge>}

            {link?.passwordProtected && (
              <Badge tone="brand" dot={false}>
                <IconLock className="h-3.5 w-3.5" />
                Password protected
              </Badge>
            )}
          </div>

          <div className="mt-2 flex min-w-0 items-center gap-1">
            <span className="truncate text-sm text-ink-500">{shortUrl}</span>
            <CopyButton value={shortUrl} />
          </div>

          {link?.expiresAt && (
            <p className="mt-1 text-sm text-ink-500">
              {link.expired ? 'Expired' : 'Expires'} {formatDateTime(link.expiresAt)}
            </p>
          )}
        </div>

        {/* Range selector. 90 days is the ceiling because the daily series is
            zero-filled client-side, and a year of bars is unreadable anyway. */}
        <div className="flex shrink-0 gap-1 self-start rounded-lg bg-ink-100 p-1">
          {RANGES.map((range) => (
            <button
              key={range.days}
              type="button"
              onClick={() => setDays(range.days)}
              aria-pressed={days === range.days}
              className={[
                'rounded-md px-3 py-1.5 text-sm font-medium transition-colors',
                days === range.days
                  ? 'bg-white text-ink-900 shadow-card'
                  : 'text-ink-500 hover:text-ink-800',
              ].join(' ')}
            >
              {range.label}
            </button>
          ))}
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatTile label="Total clicks" value={summary?.totalClicks} caption="All time" />
        <StatTile
          label="Unique visitors"
          value={summary?.uniqueVisitors}
          caption="By distinct IP — approximate"
        />
        <StatTile label="Desktop" value={summary?.desktopUsers} />
        <StatTile label="Mobile" value={summary?.mobileUsers} />
      </div>

      <Card className="mt-6">
        <CardHeader title="Click timeline" description={`Last ${days} days.`} />
        <div className="mt-6">
          <ClicksChart data={summary?.clicksPerDay ?? []} />
        </div>
      </Card>

      <div className="mt-6 grid gap-6 lg:grid-cols-3">
        <Card>
          <CardHeader title="Browsers" />
          <div className="mt-5">
            <BreakdownList items={summary?.browsers} />
          </div>
        </Card>

        <Card>
          <CardHeader title="Operating systems" />
          <div className="mt-5">
            <BreakdownList items={summary?.operatingSystems} />
          </div>
        </Card>

        <Card>
          <CardHeader title="Devices" />
          <div className="mt-5">
            <BreakdownList items={summary?.devices} />
          </div>
        </Card>
      </div>

      <Card className="mt-6" padded={false}>
        <div className="px-6 py-5">
          <CardHeader
            title="Click history"
            description="The 50 most recent clicks on this link."
          />
        </div>

        <div className="border-t border-ink-100">
          {clicks.length === 0 ? (
            <EmptyState
              icon={<IconChart className="h-6 w-6" />}
              title="No clicks yet"
              description="Share the link and clicks will start appearing here within seconds."
            />
          ) : (
            <div className="scroll-x">
              <table className="w-full min-w-[560px] border-collapse">
                <thead>
                  <tr className="border-b border-ink-200 bg-ink-50/60">
                    <th className="cell text-left font-medium text-ink-600">When</th>
                    <th className="cell text-left font-medium text-ink-600">Browser</th>
                    <th className="cell text-left font-medium text-ink-600">OS</th>
                    <th className="cell text-left font-medium text-ink-600">Device</th>
                    <th className="cell text-left font-medium text-ink-600">IP</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ink-100">
                  {clicks.map((click, index) => (
                    <tr key={`${click.clickedAt}-${index}`} className="hover:bg-ink-50/50">
                      <td className="cell whitespace-nowrap text-ink-900">
                        <span title={formatDateTime(click.clickedAt)}>
                          {formatRelative(click.clickedAt)}
                        </span>
                      </td>
                      <td className="cell text-ink-600">{click.browser}</td>
                      <td className="cell text-ink-600">{click.operatingSystem}</td>
                      <td className="cell">
                        <Badge tone={click.device === 'Bot' ? 'neutral' : 'brand'} dot={false}>
                          {click.device}
                        </Badge>
                      </td>
                      <td className="cell font-mono text-xs text-ink-500">{click.ipAddress}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </Card>
    </>
  );
}

function BackLink() {
  return (
    <Link
      to="/dashboard/analytics"
      className="inline-flex items-center gap-1.5 rounded text-sm font-medium text-ink-500 transition-colors hover:text-ink-900"
    >
      <IconArrowLeft className="h-4 w-4" />
      All analytics
    </Link>
  );
}
