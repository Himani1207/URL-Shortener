import { memo } from 'react';
import { Link } from 'react-router-dom';
import Badge from '../ui/Badge';
import CopyButton from './CopyButton';
import { EmptyState, TableSkeleton } from '../ui/States';
import {
  IconChart,
  IconEdit,
  IconExternal,
  IconLink,
  IconPause,
  IconPlay,
  IconQr,
  IconTrash,
} from '../ui/Icons';
import { formatCount, formatDate, hostnameOf, linkStatus, truncateUrl } from '../../lib/format';

/**
 * The links table: Original URL, Short URL, Clicks, Created, Status, Actions.
 *
 * <b>Two layouts, one component.</b> At `md` and above it is a real table; below
 * that it becomes a stack of cards. A six-column table on a phone either scrolls
 * sideways or crushes every column to unreadable width, and neither is acceptable
 * for the primary view. The breakpoint moved from `sm` to `md` because 640px is
 * still too narrow for six columns — small tablets were getting the cramped
 * version.
 *
 * <b>Rows are memoised.</b> The parent re-renders on every mutation — a toggle, a
 * delete, an edit — and without this each of those re-renders every row in the
 * list. With row identity stable, only the row that actually changed repaints.
 *
 * Actions are icon buttons with tooltips and aria-labels rather than a kebab menu:
 * five actions is few enough to show directly, and one click beats two.
 */
function LinksTable({ links, loading = false, onEdit, onDelete, onToggle, onShowQr, emptyAction }) {
  if (loading) return <TableSkeleton rows={5} />;

  if (!links.length) {
    return (
      <EmptyState
        icon={<IconLink className="h-6 w-6" />}
        title="No links yet"
        description="Create your first short link and it will show up here with its click history."
        action={emptyAction}
      />
    );
  }

  return (
    <>
      {/* ---------------- Table (md and up) ---------------- */}
      <div className="scroll-x hidden md:block">
        <table className="w-full min-w-[760px] border-collapse">
          <thead>
            <tr className="border-b border-ink-200 bg-ink-50/60">
              <th className="cell text-left font-medium text-ink-600">Link</th>
              <th className="cell text-left font-medium text-ink-600">Short URL</th>
              <th className="cell text-right font-medium text-ink-600">Clicks</th>
              <th className="cell whitespace-nowrap text-left font-medium text-ink-600">Created</th>
              <th className="cell text-left font-medium text-ink-600">Status</th>
              <th className="cell text-right font-medium text-ink-600">
                <span className="sr-only">Actions</span>
              </th>
            </tr>
          </thead>

          <tbody className="divide-y divide-ink-100">
            {links.map((link, index) => (
              <TableRow
                key={link.id}
                link={link}
                index={index}
                onEdit={onEdit}
                onDelete={onDelete}
                onToggle={onToggle}
                onShowQr={onShowQr}
              />
            ))}
          </tbody>
        </table>
      </div>

      {/* ---------------- Cards (below md) ---------------- */}
      <ul className="divide-y divide-ink-100 md:hidden">
        {links.map((link, index) => (
          <MobileRow
            key={link.id}
            link={link}
            index={index}
            onEdit={onEdit}
            onDelete={onDelete}
            onToggle={onToggle}
            onShowQr={onShowQr}
          />
        ))}
      </ul>
    </>
  );
}

// ---------------------------------------------------------------------------

const TableRow = memo(function TableRow({ link, index, onEdit, onDelete, onToggle, onShowQr }) {
  const status = linkStatus(link);

  return (
    <tr
      className="stagger-in group transition-colors hover:bg-ink-50/50"
      // Capped at 6 so a long list does not take a visible age to finish
      // appearing; past that everything lands together.
      style={{ animationDelay: `${Math.min(index, 6) * 40}ms` }}
    >
      <td className="cell max-w-[280px]">
        <div className="flex items-center gap-3">
          <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-ink-100 text-ink-500 transition-colors group-hover:bg-brand-50 group-hover:text-brand-600">
            <IconGlobeSmall />
          </span>
          {/* min-w-0 is required for truncate to work inside a flex row. */}
          <div className="min-w-0">
            <p className="truncate font-medium text-ink-900">{hostnameOf(link.originalUrl)}</p>
            <p className="truncate text-xs text-ink-500" title={link.originalUrl}>
              {truncateUrl(link.originalUrl, 44)}
            </p>
          </div>
        </div>
      </td>

      <td className="cell">
        <div className="flex items-center gap-1">
          <a
            href={link.shortUrl}
            target="_blank"
            rel="noreferrer"
            className="rounded font-mono text-[13px] text-brand-600 transition-colors hover:text-brand-800 hover:underline"
          >
            /{link.shortCode}
          </a>
          <CopyButton value={link.shortUrl} />
        </div>
      </td>

      <td className="cell text-right font-medium tabular-nums text-ink-900">
        {formatCount(link.clickCount)}
      </td>

      <td className="cell whitespace-nowrap text-ink-600">{formatDate(link.createdAt)}</td>

      <td className="cell">
        <Badge tone={status.tone}>{status.label}</Badge>
      </td>

      <td className="cell">
        {/* Actions fade in on hover at desktop widths to keep the table calm, but
            stay permanently visible for keyboard and touch users, who have no
            hover state to reveal them with. */}
        <div className="flex items-center justify-end gap-0.5 opacity-100 transition-opacity duration-150 lg:opacity-60 lg:group-hover:opacity-100 lg:group-focus-within:opacity-100">
          <ActionButton label="View analytics" as={Link} to={`/dashboard/analytics/${link.shortCode}`}>
            <IconChart className="h-[18px] w-[18px]" />
          </ActionButton>

          <ActionButton label="Show QR code" onClick={() => onShowQr(link)}>
            <IconQr className="h-[18px] w-[18px]" />
          </ActionButton>

          <ActionButton
            label={link.active ? 'Pause link' : 'Resume link'}
            onClick={() => onToggle(link)}
          >
            {link.active ? (
              <IconPause className="h-[18px] w-[18px]" />
            ) : (
              <IconPlay className="h-[18px] w-[18px]" />
            )}
          </ActionButton>

          <ActionButton label="Edit link" onClick={() => onEdit(link)}>
            <IconEdit className="h-[18px] w-[18px]" />
          </ActionButton>

          <ActionButton label="Delete link" danger onClick={() => onDelete(link)}>
            <IconTrash className="h-[18px] w-[18px]" />
          </ActionButton>
        </div>
      </td>
    </tr>
  );
});

const MobileRow = memo(function MobileRow({ link, index, onEdit, onDelete, onToggle, onShowQr }) {
  const status = linkStatus(link);

  return (
    <li
      className="stagger-in px-4 py-4 sm:px-5"
      style={{ animationDelay: `${Math.min(index, 6) * 40}ms` }}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-medium text-ink-900">
            {hostnameOf(link.originalUrl)}
          </p>
          <p className="mt-0.5 truncate text-xs text-ink-500">
            {truncateUrl(link.originalUrl, 40)}
          </p>
        </div>
        <Badge tone={status.tone} className="shrink-0">
          {status.label}
        </Badge>
      </div>

      <div className="mt-3 flex flex-wrap items-center gap-x-1 gap-y-2">
        <a
          href={link.shortUrl}
          target="_blank"
          rel="noreferrer"
          className="rounded font-mono text-[13px] text-brand-600"
        >
          /{link.shortCode}
        </a>
        <CopyButton value={link.shortUrl} />
        <span className="ml-auto whitespace-nowrap text-xs text-ink-500">
          {formatCount(link.clickCount)} clicks · {formatDate(link.createdAt)}
        </span>
      </div>

      <div className="mt-3 flex items-center gap-0.5 border-t border-ink-100 pt-3">
        <ActionButton label="View analytics" as={Link} to={`/dashboard/analytics/${link.shortCode}`}>
          <IconChart className="h-[18px] w-[18px]" />
        </ActionButton>
        <ActionButton label="Show QR code" onClick={() => onShowQr(link)}>
          <IconQr className="h-[18px] w-[18px]" />
        </ActionButton>
        <ActionButton
          label={link.active ? 'Pause link' : 'Resume link'}
          onClick={() => onToggle(link)}
        >
          {link.active ? (
            <IconPause className="h-[18px] w-[18px]" />
          ) : (
            <IconPlay className="h-[18px] w-[18px]" />
          )}
        </ActionButton>
        <ActionButton label="Edit link" onClick={() => onEdit(link)}>
          <IconEdit className="h-[18px] w-[18px]" />
        </ActionButton>
        <ActionButton label="Delete link" danger onClick={() => onDelete(link)}>
          <IconTrash className="h-[18px] w-[18px]" />
        </ActionButton>

        <a
          href={link.originalUrl}
          target="_blank"
          rel="noreferrer"
          aria-label="Open destination"
          className="ml-auto inline-flex h-9 w-9 items-center justify-center rounded-lg text-ink-400 transition-colors hover:bg-ink-100 hover:text-ink-700"
        >
          <IconExternal className="h-[18px] w-[18px]" />
        </a>
      </div>
    </li>
  );
});

/**
 * Icon-only action.
 *
 * `as` lets the same visual affordance be either a button or a router Link —
 * "view analytics" is navigation and should be a real anchor so it supports
 * middle-click and open-in-new-tab, while the rest are genuine buttons.
 *
 * 36px square: comfortably above the ~44px-with-spacing touch target guidance once
 * the surrounding padding is counted, and large enough to hit on a phone.
 */
function ActionButton({ label, danger = false, as: Component = 'button', children, ...rest }) {
  return (
    <Component
      {...(Component === 'button' ? { type: 'button' } : {})}
      aria-label={label}
      title={label}
      className={[
        'inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-lg',
        'transition-all duration-150 active:scale-90',
        danger
          ? 'text-ink-400 hover:bg-red-50 hover:text-red-600'
          : 'text-ink-400 hover:bg-ink-100 hover:text-ink-700',
      ].join(' ')}
      {...rest}
    >
      {children}
    </Component>
  );
}

function IconGlobeSmall() {
  return (
    <svg width="18" height="18" viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <circle cx="10" cy="10" r="7" stroke="currentColor" strokeWidth="1.5" />
      <path
        d="M3 10h14M10 3c1.9 2.2 2.8 4.6 2.8 7s-.9 4.8-2.8 7c-1.9-2.2-2.8-4.6-2.8-7S8.1 5.2 10 3z"
        stroke="currentColor"
        strokeWidth="1.5"
      />
    </svg>
  );
}

export default memo(LinksTable);
