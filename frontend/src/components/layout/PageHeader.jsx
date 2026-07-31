/**
 * Consistent page title block for dashboard routes.
 *
 * Exists so every page's heading sits at the same height with the same type
 * scale. When each page rolls its own, the title shifts by a few pixels as you
 * navigate — subtle enough to miss in isolation, obvious in use.
 */
export default function PageHeader({ title, description, action, className = '' }) {
  return (
    <div
      className={[
        // A rule under every page title, which is what gives the dashboard its
        // through-line now that cards no longer carry shadows.
        'mb-7 flex flex-col gap-4 border-b border-ink-200 pb-6',
        'sm:mb-9 sm:flex-row sm:items-end sm:justify-between',
        className,
      ].join(' ')}
    >
      <div className="min-w-0">
        <h1 className="text-[22px] font-semibold leading-tight tracking-tight text-ink-900 sm:text-[26px]">
          {title}
        </h1>
        {description && (
          <p className="mt-2 max-w-[60ch] text-sm leading-relaxed text-ink-500">{description}</p>
        )}
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  );
}
