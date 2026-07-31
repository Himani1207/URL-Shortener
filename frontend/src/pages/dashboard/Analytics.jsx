import { Link } from 'react-router-dom';
import PageHeader from '../../components/layout/PageHeader';
import Card from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Badge from '../../components/ui/Badge';
import { EmptyState, ErrorState, TableSkeleton } from '../../components/ui/States';
import { IconChart, IconLink } from '../../components/ui/Icons';
import { formatCount, formatDate, hostnameOf, linkStatus } from '../../lib/format';
import { useLinks } from '../../hooks/useLinks';

/**
 * Analytics index: pick a link to inspect.
 *
 * The brief was explicit that analytics should open in a separate page rather
 * than filling the dashboard. This route is that entry point — a ranked list of
 * links by click volume, each opening its own detail page.
 *
 * Ranked by clicks rather than by date, because the question this page answers is
 * "which of my links is working", not "what did I make most recently" — the
 * Links page already answers that.
 */
export default function Analytics() {
  const { links, loading, error, reload } = useLinks();

  const ranked = [...links].sort((a, b) => (b.clickCount ?? 0) - (a.clickCount ?? 0));
  const totalClicks = links.reduce((sum, link) => sum + (link.clickCount ?? 0), 0);

  return (
    <>
      <PageHeader
        title="Analytics"
        description="Choose a link to see its click history in detail."
      />

      <Card padded={false}>
        <div className="flex items-center justify-between border-b border-ink-100 px-6 py-5">
          <div>
            <h2 className="text-base font-semibold text-ink-900">Links by clicks</h2>
            <p className="mt-1 text-sm text-ink-500">
              {formatCount(totalClicks)} total {totalClicks === 1 ? 'click' : 'clicks'} across{' '}
              {links.length} {links.length === 1 ? 'link' : 'links'}
            </p>
          </div>
        </div>

        {error && <ErrorState message={error} onRetry={reload} />}

        {!error && loading && <TableSkeleton rows={5} />}

        {!error && !loading && ranked.length === 0 && (
          <EmptyState
            icon={<IconChart className="h-6 w-6" />}
            title="Nothing to analyse yet"
            description="Once you create a link and it starts getting clicks, its analytics will appear here."
            action={
              <Link to="/dashboard/links">
                <Button>Create a link</Button>
              </Link>
            }
          />
        )}

        {!error && !loading && ranked.length > 0 && (
          <ul className="divide-y divide-ink-100">
            {ranked.map((link) => {
              const status = linkStatus(link);
              return (
                <li key={link.id}>
                  <Link
                    to={`/dashboard/analytics/${link.shortCode}`}
                    className="flex items-center gap-4 px-6 py-4 transition-colors hover:bg-ink-50/60"
                  >
                    <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-ink-100 text-ink-500">
                      <IconLink className="h-[18px] w-[18px]" />
                    </span>

                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <p className="truncate font-mono text-sm text-brand-600">
                          /{link.shortCode}
                        </p>
                        <Badge tone={status.tone}>{status.label}</Badge>
                      </div>
                      <p className="mt-0.5 truncate text-xs text-ink-500">
                        {hostnameOf(link.originalUrl)} · created {formatDate(link.createdAt)}
                      </p>
                    </div>

                    <div className="shrink-0 text-right">
                      <p className="text-lg font-semibold tabular-nums text-ink-900">
                        {formatCount(link.clickCount)}
                      </p>
                      <p className="text-xs text-ink-400">
                        {link.clickCount === 1 ? 'click' : 'clicks'}
                      </p>
                    </div>

                    <svg
                      className="h-4 w-4 shrink-0 text-ink-300"
                      viewBox="0 0 20 20"
                      fill="none"
                      aria-hidden="true"
                    >
                      <path
                        d="m7.5 4.5 6 5.5-6 5.5"
                        stroke="currentColor"
                        strokeWidth="1.6"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      />
                    </svg>
                  </Link>
                </li>
              );
            })}
          </ul>
        )}
      </Card>
    </>
  );
}
