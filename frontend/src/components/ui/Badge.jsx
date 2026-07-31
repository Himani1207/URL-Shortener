/**
 * Small status pill.
 *
 * Every tone pairs a colour with a text label rather than relying on colour alone —
 * roughly one in twelve men has some form of colour vision deficiency, and a
 * red-versus-green dot with no words is unreadable to them.
 */
const TONES = {
  success: 'bg-emerald-50 text-emerald-800 ring-emerald-700/25',
  warning: 'bg-amber-50 text-amber-800 ring-amber-700/25',
  neutral: 'bg-ink-100 text-ink-600 ring-ink-400/40',
  brand: 'bg-brand-50 text-brand-800 ring-brand-600/25',
  danger: 'bg-red-50 text-red-800 ring-red-700/25',
};

const DOTS = {
  success: 'bg-emerald-500',
  warning: 'bg-amber-500',
  neutral: 'bg-ink-400',
  brand: 'bg-brand-500',
  danger: 'bg-red-500',
};

export default function Badge({ tone = 'neutral', dot = true, children, className = '' }) {
  return (
    <span
      className={[
        // Squared rather than a pill: at this size a capsule reads as a tag you
        // could remove, which these are not.
        'inline-flex items-center gap-1.5 rounded px-2 py-0.5',
        'text-xs font-medium ring-1 ring-inset',
        TONES[tone],
        className,
      ].join(' ')}
    >
      {dot && <span className={`h-1.5 w-1.5 rounded-full ${DOTS[tone]}`} aria-hidden="true" />}
      {children}
    </span>
  );
}
