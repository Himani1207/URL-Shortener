import { memo } from 'react';
import { formatCount } from '../../lib/format';
import { useCountUp } from '../../hooks/useCountUp';

/**
 * Single metric tile.
 *
 * Kept intentionally plain — a number, a label, and optionally one line of
 * context. The brief called for a clean dashboard rather than large analytics
 * widgets, and a tile that tries to be a chart as well is how dashboards get
 * cluttered.
 *
 * `tabular-nums` stops the digits from shifting width as values change, which is
 * what makes a row of tiles look unstable on refresh. The value area also keeps a
 * fixed height across loading and loaded states so the card never resizes when
 * data arrives.
 *
 * Memoised: the dashboard re-renders on every link mutation, and these tiles only
 * change when their own value does.
 */
function StatTile({ label, value, caption, icon, loading = false }) {
  const animated = useCountUp(value ?? 0);
  const hasValue = value !== null && value !== undefined;

  return (
    <div className="group rounded-lg border border-ink-200 bg-white p-5 transition-colors duration-200 hover:border-ink-400">
      <div className="flex items-center justify-between gap-2">
        <p className="truncate text-sm font-medium text-ink-500">{label}</p>
        {icon && (
          <span className="shrink-0 text-ink-300 transition-colors duration-200 group-hover:text-brand-500">
            {icon}
          </span>
        )}
      </div>

      {/* Fixed height across both states — this is what prevents the layout shift
          when the stats request resolves. */}
      <div className="mt-2 flex h-9 items-center">
        {loading ? (
          <div className="h-7 w-20 animate-pulse rounded bg-ink-100" />
        ) : (
          <p className="text-3xl font-semibold tabular-nums tracking-tight text-ink-900">
            {hasValue ? formatCount(animated) : '—'}
          </p>
        )}
      </div>

      {/* Reserved even when empty, for the same reason. */}
      <p className="mt-1 h-4 text-xs text-ink-500">{!loading && caption ? caption : ''}</p>
    </div>
  );
}

export default memo(StatTile);
