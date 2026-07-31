import { Link } from 'react-router-dom';

/**
 * Wordmark used in the navigation bar and on the auth pages.
 *
 * One component so the mark can never drift between surfaces, and so the
 * destination is decided in a single place.
 *
 * The mark is drawn in the accent colour on the page ground rather than reversed
 * out of a filled tile. A rounded coloured square behind a white glyph is the house
 * style of every SaaS starter; an unboxed mark sitting on the paper reads as part of
 * the page instead of an app icon pasted onto it.
 */
export default function Logo({ to = '/', compact = false, className = '' }) {
  const mark = (
    <svg
      width="20"
      height="20"
      viewBox="0 0 32 32"
      fill="none"
      className="shrink-0 text-brand-600"
      aria-hidden="true"
    >
      <path
        d="M13 19a4 4 0 0 1 0-6l2-2a4 4 0 0 1 6 0 4 4 0 0 1 0 6l-1 1"
        stroke="currentColor"
        strokeWidth="2.6"
        strokeLinecap="round"
      />
      <path
        d="M19 13a4 4 0 0 1 0 6l-2 2a4 4 0 0 1-6 0 4 4 0 0 1 0-6l1-1"
        stroke="currentColor"
        strokeWidth="2.6"
        strokeLinecap="round"
      />
    </svg>
  );

  const content = (
    <span className={`flex items-center gap-2 ${className}`}>
      {mark}
      {!compact && (
        <span className="text-[15px] font-semibold tracking-tight text-ink-900">Linkly</span>
      )}
    </span>
  );

  return to ? (
    <Link to={to} className="rounded" aria-label="Linkly home">
      {content}
    </Link>
  ) : (
    content
  );
}
