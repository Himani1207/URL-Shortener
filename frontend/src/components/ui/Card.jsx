/**
 * Surface container.
 *
 * A white panel on the paper ground, separated by a 1px rule and nothing else —
 * the shadow tokens are empty by design, so this is a drawn edge rather than a
 * simulated lift.
 *
 * `interactive` darkens the rule on hover for cards that are themselves clickable.
 * It is opt-in rather than default: a card that responds without being clickable
 * promises an interaction that is not there. It no longer translates upward, which
 * is a shadow-era gesture that looks unmoored with nothing beneath it to cast onto.
 */
export default function Card({
  className = '',
  padded = true,
  interactive = false,
  children,
  ...rest
}) {
  return (
    <div
      className={[
        'rounded-lg border border-ink-200 bg-white',
        interactive ? 'transition-colors duration-200 hover:border-ink-400' : '',
        padded ? 'p-5 sm:p-6' : '',
        className,
      ].join(' ')}
      {...rest}
    >
      {children}
    </div>
  );
}

/** Header row for a card: title on the left, optional action on the right. */
export function CardHeader({ title, description, action, className = '' }) {
  return (
    <div className={`flex flex-wrap items-start justify-between gap-x-4 gap-y-2 ${className}`}>
      <div className="min-w-0">
        <h2 className="text-[15px] font-semibold tracking-tight text-ink-900">{title}</h2>
        {description && <p className="mt-1 text-sm leading-relaxed text-ink-500">{description}</p>}
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  );
}
