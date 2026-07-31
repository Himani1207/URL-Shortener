/**
 * The single button in the design system.
 *
 * Variants exist so a destructive action never looks like a primary one, and so
 * "primary" is a decision made once rather than re-derived from Tailwind classes
 * at each call site.
 *
 * Note `loading` disables the button as well as showing a spinner — a submit
 * button that stays clickable while a request is in flight is how you get
 * duplicate links created by an impatient double-click.
 *
 * The `active:scale-[0.98]` press is the one flourish: it makes a click feel
 * physical without any colour or motion that would read as noisy. It is excluded
 * from disabled buttons, where feedback would be a lie.
 */
const VARIANTS = {
  // No drop shadow on the solid variants: the accent is dark enough to hold its own
  // edge, and a shadow here would be the only one left in the interface.
  primary:
    'bg-brand-600 text-white hover:bg-brand-700 active:bg-brand-800 disabled:bg-brand-300',
  secondary:
    'bg-white text-ink-700 border border-ink-300 hover:border-ink-400 hover:text-ink-900 disabled:text-ink-400 disabled:border-ink-200',
  ghost:
    'bg-transparent text-ink-600 hover:bg-ink-100 hover:text-ink-900 disabled:text-ink-300',
  danger:
    'bg-red-700 text-white hover:bg-red-800 active:bg-red-900 disabled:bg-red-300',
  'danger-ghost':
    'bg-transparent text-red-700 hover:bg-red-50 disabled:text-red-300',
};

const SIZES = {
  // min-h rather than a fixed h: a button whose label wraps on a narrow screen
  // grows instead of clipping its own text.
  sm: 'min-h-9 px-3.5 py-1.5 text-sm gap-1.5',
  md: 'min-h-11 px-5 py-2.5 text-sm gap-2',
  lg: 'min-h-12 px-6 py-3 text-base gap-2.5',
};

/**
 * The class list behind every button, exposed so a link can wear it too.
 *
 * Needed because a control that navigates must be an `<a>`, not a `<button>` — it
 * has to honour middle-click, "open in new tab" and a copied address, none of which
 * a button gives you. Wrapping a `<button>` in an `<a>` is also invalid HTML:
 * interactive content cannot nest.
 */
export function buttonClasses({
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  inactive = false,
  className = '',
} = {}) {
  return [
    'inline-flex select-none items-center justify-center rounded-lg font-medium',
    'whitespace-nowrap transition-all duration-150 ease-out',
    inactive ? 'cursor-not-allowed' : 'active:scale-[0.98]',
    VARIANTS[variant],
    SIZES[size],
    fullWidth ? 'w-full' : '',
    className,
  ].join(' ');
}

export default function Button({
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled = false,
  fullWidth = false,
  icon = null,
  className = '',
  children,
  ...rest
}) {
  const isInactive = disabled || loading;

  return (
    <button
      disabled={isInactive}
      aria-busy={loading || undefined}
      className={buttonClasses({
        variant,
        size,
        fullWidth,
        inactive: isInactive,
        className,
      })}
      {...rest}
    >
      {loading ? <Spinner /> : icon}
      {children}
    </button>
  );
}

/**
 * A button-shaped link.
 *
 * `as` takes react-router's `<Link>` for in-app routes and defaults to a plain
 * `<a>` for external ones, so the choice of navigation mechanism stays with the
 * caller while the styling stays here.
 */
export function ButtonLink({
  as: Component = 'a',
  variant = 'secondary',
  size = 'md',
  fullWidth = false,
  icon = null,
  className = '',
  children,
  ...rest
}) {
  return (
    <Component
      className={buttonClasses({ variant, size, fullWidth, className })}
      {...rest}
    >
      {icon}
      {children}
    </Component>
  );
}

function Spinner() {
  return (
    <svg className="h-4 w-4 shrink-0 animate-spin" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" opacity="0.25" />
      <path d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
    </svg>
  );
}
