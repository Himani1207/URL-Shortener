/**
 * Presentation helpers.
 *
 * Kept out of components so a date renders identically in the links table, the
 * analytics page and the dashboard — the kind of thing that quietly drifts when
 * each component formats its own.
 */

/** e.g. "28 Jul 2026". */
export function formatDate(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';

  return new Intl.DateTimeFormat(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  }).format(date);
}

/** e.g. "28 Jul 2026, 14:32". */
export function formatDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';

  return new Intl.DateTimeFormat(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

/** e.g. "3 days ago". Falls back to an absolute date beyond a month. */
export function formatRelative(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';

  const seconds = Math.round((date.getTime() - Date.now()) / 1000);
  const absolute = Math.abs(seconds);

  const units = [
    ['second', 60],
    ['minute', 60],
    ['hour', 24],
    ['day', 7],
    ['week', 4.35],
  ];

  if (absolute > 60 * 60 * 24 * 30) return formatDate(value);

  const relative = new Intl.RelativeTimeFormat(undefined, { numeric: 'auto' });
  let amount = seconds;

  for (const [unit, step] of units) {
    if (Math.abs(amount) < step) return relative.format(Math.round(amount), unit);
    amount /= step;
  }
  return relative.format(Math.round(amount), 'month');
}

/** 1234 -> "1.2K". Keeps table columns from widening as counts grow. */
export function formatCount(value) {
  const number = Number(value ?? 0);
  if (number < 1000) return String(number);

  return new Intl.NumberFormat(undefined, {
    notation: 'compact',
    maximumFractionDigits: 1,
  }).format(number);
}

/**
 * Shortens a URL for display while keeping the parts that identify it.
 * Truncating the middle preserves both the domain and the tail, which a plain
 * `substring` would throw away.
 */
export function truncateUrl(url, max = 48) {
  if (!url) return '';
  if (url.length <= max) return url;

  const stripped = url.replace(/^https?:\/\//, '');
  if (stripped.length <= max) return stripped;

  const head = stripped.slice(0, max - 14);
  const tail = stripped.slice(-10);
  return `${head}…${tail}`;
}

/** Hostname only, for the favicon-style domain label in the links table. */
export function hostnameOf(url) {
  try {
    return new URL(url).hostname.replace(/^www\./, '');
  } catch {
    return url;
  }
}

/**
 * Copies text to the clipboard.
 *
 * The fallback matters: navigator.clipboard is undefined on any non-HTTPS origin
 * other than localhost, so without it "Copy" would silently do nothing wherever
 * the app is served over plain HTTP.
 */
export async function copyToClipboard(text) {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
      return true;
    }

    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    const ok = document.execCommand('copy');
    document.body.removeChild(textarea);
    return ok;
  } catch {
    return false;
  }
}

/**
 * Derives the display status of a link.
 * Order matters: an expired link is reported as expired even if `active` is still
 * true, because the hourly sweep may not have run yet.
 */
export function linkStatus(link) {
  if (link.expired) return { label: 'Expired', tone: 'warning' };
  if (!link.active) return { label: 'Paused', tone: 'neutral' };
  return { label: 'Active', tone: 'success' };
}
