import { formatCount } from '../../lib/format';

/**
 * Ranked breakdown with an inline proportional bar.
 *
 * A horizontal bar list rather than a pie chart. Pie charts are genuinely hard to
 * read past three slices — comparing angles is much less accurate than comparing
 * lengths against a shared baseline — and browser/OS breakdowns routinely have
 * eight or more categories.
 */
export default function BreakdownList({ items, emptyLabel = 'No data yet', limit = 6 }) {
  if (!items?.length) {
    return <p className="py-6 text-center text-sm text-ink-500">{emptyLabel}</p>;
  }

  const total = items.reduce((sum, item) => sum + Number(item.count ?? 0), 0);
  const visible = items.slice(0, limit);
  const remainder = items.length - visible.length;

  return (
    <div className="space-y-3">
      {visible.map((item) => {
        const share = total > 0 ? (Number(item.count) / total) * 100 : 0;
        return (
          <div key={item.label}>
            <div className="mb-1.5 flex items-baseline justify-between gap-3">
              <span className="truncate text-sm text-ink-700">{item.label || 'Unknown'}</span>
              <span className="shrink-0 text-sm tabular-nums text-ink-500">
                {formatCount(item.count)}
                <span className="ml-1.5 text-ink-400">{share.toFixed(0)}%</span>
              </span>
            </div>

            {/* aria-hidden: the numbers above already carry the information, so
                announcing the bar too would just repeat it. */}
            <div className="h-1.5 overflow-hidden rounded-full bg-ink-100" aria-hidden="true">
              <div
                className="h-full rounded-full bg-brand-500 transition-[width] duration-300"
                style={{ width: `${share}%` }}
              />
            </div>
          </div>
        );
      })}

      {remainder > 0 && (
        <p className="pt-1 text-xs text-ink-400">
          +{remainder} more {remainder === 1 ? 'category' : 'categories'}
        </p>
      )}
    </div>
  );
}
