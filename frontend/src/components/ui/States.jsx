/**
 * The three states every data view needs besides "has data": loading, empty and
 * failed. Grouped in one file because they are always used together and are
 * meaningless apart.
 *
 * Getting these wrong is the most common way a dashboard feels broken — a blank
 * table looks identical whether it is still loading, genuinely empty, or errored.
 */

export function Spinner({ className = 'h-5 w-5' }) {
  return (
    <svg className={`animate-spin ${className}`} viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" opacity="0.2" />
      <path d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
    </svg>
  );
}

export function LoadingState({ label = 'Loading…', className = '' }) {
  return (
    <div className={`flex flex-col items-center justify-center gap-3 py-16 ${className}`}>
      <Spinner className="h-6 w-6 text-brand-600" />
      <p className="text-sm text-ink-500">{label}</p>
    </div>
  );
}

/**
 * Skeleton rows for the links table.
 *
 * Preferred over a spinner where the shape of the result is known: it keeps the
 * layout from jumping when data lands, and reads as faster even at identical
 * latency.
 */
export function TableSkeleton({ rows = 4 }) {
  return (
    <div className="divide-y divide-ink-100">
      {Array.from({ length: rows }).map((_, index) => (
        <div key={index} className="flex items-center gap-4 px-5 py-4">
          <div className="h-9 w-9 shrink-0 animate-pulse rounded-lg bg-ink-100" />
          <div className="flex-1 space-y-2">
            <div className="h-3.5 w-1/3 animate-pulse rounded bg-ink-100" />
            <div className="h-3 w-1/2 animate-pulse rounded bg-ink-50" />
          </div>
          <div className="h-3.5 w-12 animate-pulse rounded bg-ink-100" />
          <div className="h-6 w-16 animate-pulse rounded-full bg-ink-100" />
        </div>
      ))}
    </div>
  );
}

export function EmptyState({ icon, title, description, action, className = '' }) {
  return (
    <div className={`flex flex-col items-center justify-center px-6 py-16 text-center ${className}`}>
      {icon && (
        <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-ink-50 text-ink-400">
          {icon}
        </div>
      )}
      <h3 className="text-base font-semibold text-ink-900">{title}</h3>
      {description && <p className="mt-1.5 max-w-sm text-sm text-ink-500">{description}</p>}
      {action && <div className="mt-6">{action}</div>}
    </div>
  );
}

export function ErrorState({ message, onRetry, className = '' }) {
  return (
    <div className={`flex flex-col items-center justify-center px-6 py-16 text-center ${className}`}>
      <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-red-50 text-red-500">
        <svg width="22" height="22" viewBox="0 0 20 20" fill="none" aria-hidden="true">
          <circle cx="10" cy="10" r="8.25" stroke="currentColor" strokeWidth="1.5" />
          <path d="M10 6v4.5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
          <circle cx="10" cy="13.6" r="1" fill="currentColor" />
        </svg>
      </div>
      <h3 className="text-base font-semibold text-ink-900">Something went wrong</h3>
      <p className="mt-1.5 max-w-sm text-sm text-ink-500">{message}</p>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="mt-6 rounded-lg border border-ink-200 px-4 py-2 text-sm font-medium text-ink-700 transition hover:bg-ink-50"
        >
          Try again
        </button>
      )}
    </div>
  );
}
