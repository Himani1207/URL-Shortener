import { useMemo, useState } from 'react';
import { formatDate } from '../../lib/format';

/**
 * Clicks-over-time bar chart.
 *
 * Hand-drawn SVG rather than a charting library. Recharts or Chart.js would add
 * roughly 100KB gzipped to render one bar series, and the brief explicitly ruled
 * out heavy analytics widgets. This is about eighty lines, has no dependency, and
 * inherits the design tokens directly.
 *
 * The backend returns a contiguous zero-filled series, so no gap handling is
 * needed here — a sparse series would otherwise render as a misleading straight
 * line between distant dates.
 */
export default function ClicksChart({ data, height = 200 }) {
  const [hovered, setHovered] = useState(null);

  const { max, bars } = useMemo(() => {
    // Floor of 1 so an all-zero series renders a flat baseline instead of
    // dividing by zero and producing NaN heights.
    const peak = Math.max(1, ...data.map((point) => point.count));
    return {
      max: peak,
      bars: data.map((point) => ({ ...point, ratio: point.count / peak })),
    };
  }, [data]);

  if (!data.length) {
    return (
      <div className="flex items-center justify-center py-12 text-sm text-ink-500">
        No click data yet
      </div>
    );
  }

  // Show at most six date labels; more than that and they collide.
  const labelEvery = Math.max(1, Math.ceil(bars.length / 6));

  return (
    <div className="w-full">
      <div className="flex items-end gap-[3px]" style={{ height }} role="img"
           aria-label={`Clicks per day. Peak of ${max} clicks.`}>
        {bars.map((bar, index) => (
          <div
            key={bar.date}
            className="group relative flex h-full flex-1 items-end"
            onMouseEnter={() => setHovered(index)}
            onMouseLeave={() => setHovered(null)}
          >
            {/* Track: makes the full height readable even where a bar is tiny. */}
            <div className="absolute inset-0 rounded-sm bg-ink-50 opacity-0 transition-opacity group-hover:opacity-100" />

            <div
              className={[
                'relative w-full rounded-t-[3px] transition-colors duration-150',
                hovered === index ? 'bg-brand-600' : 'bg-brand-500/70',
              ].join(' ')}
              style={{
                // 2px minimum so a zero-click day is still a visible tick on the
                // axis rather than nothing at all.
                height: `${Math.max(bar.ratio * 100, bar.count > 0 ? 4 : 1.5)}%`,
              }}
            />

            {hovered === index && (
              <div className="pointer-events-none absolute bottom-full left-1/2 z-10 mb-2 -translate-x-1/2 whitespace-nowrap rounded-lg bg-ink-900 px-2.5 py-1.5 text-xs text-white shadow-dropdown">
                <span className="font-semibold">{bar.count}</span>{' '}
                {bar.count === 1 ? 'click' : 'clicks'}
                <span className="ml-1.5 text-ink-300">{formatDate(bar.date)}</span>
              </div>
            )}
          </div>
        ))}
      </div>

      <div className="mt-3 flex justify-between border-t border-ink-100 pt-2.5">
        {bars.map((bar, index) =>
          index % labelEvery === 0 ? (
            <span key={bar.date} className="text-[11px] tabular-nums text-ink-400">
              {new Date(bar.date).toLocaleDateString(undefined, {
                day: 'numeric',
                month: 'short',
              })}
            </span>
          ) : null,
        )}
      </div>
    </div>
  );
}
