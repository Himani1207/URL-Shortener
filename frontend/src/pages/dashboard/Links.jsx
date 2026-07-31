import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../../components/layout/PageHeader';
import Card from '../../components/ui/Card';
import { ButtonLink } from '../../components/ui/Button';
import LinksTable from '../../components/links/LinksTable';
import QrModal from '../../components/links/QrModal';
import EditLinkModal from '../../components/links/EditLinkModal';
import DeleteLinkDialog from '../../components/links/DeleteLinkDialog';
import { ErrorState } from '../../components/ui/States';
import { IconPlus } from '../../components/ui/Icons';
import { useLinks } from '../../hooks/useLinks';

/**
 * Full link list with search and status filtering.
 *
 * Filtering runs client-side. The list is already loaded in full and a personal
 * account holds tens to hundreds of links, so a server round-trip per keystroke
 * would be slower and no more correct. If this ever needs pagination, the filter
 * moves to the API — the component boundary does not change.
 *
 * Creating happens on /dashboard/create, not in a dialog here. A modal cannot hold
 * the live QR preview and its controls without becoming a page in a box, and having
 * two ways to create a link means two forms to keep in step.
 */
const FILTERS = [
  { id: 'all', label: 'All' },
  { id: 'active', label: 'Active' },
  { id: 'paused', label: 'Paused' },
  { id: 'expired', label: 'Expired' },
];

export default function Links() {
  const { links, loading, error, reload, replaceLink, removeLink, toggleLink } = useLinks();

  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState('all');
  const [qrLink, setQrLink] = useState(null);
  const [editLink, setEditLink] = useState(null);
  const [deleteLink, setDeleteLink] = useState(null);

  const visibleLinks = useMemo(() => {
    const needle = query.trim().toLowerCase();

    return links.filter((link) => {
      const matchesQuery =
        !needle ||
        link.originalUrl?.toLowerCase().includes(needle) ||
        link.shortCode?.toLowerCase().includes(needle);

      if (!matchesQuery) return false;

      switch (filter) {
        case 'active':
          // Expired links are excluded even when still flagged active, since the
          // hourly sweep may not have run yet.
          return link.active && !link.expired;
        case 'paused':
          return !link.active;
        case 'expired':
          return Boolean(link.expired);
        default:
          return true;
      }
    });
  }, [links, query, filter]);

  const counts = useMemo(
    () => ({
      all: links.length,
      active: links.filter((link) => link.active && !link.expired).length,
      paused: links.filter((link) => !link.active).length,
      expired: links.filter((link) => link.expired).length,
    }),
    [links],
  );

  return (
    <>
      <PageHeader
        title="Links"
        description="Every link you have created, newest first."
        action={
          <ButtonLink
            as={Link}
            to="/dashboard/create"
            variant="primary"
            icon={<IconPlus className="h-[18px] w-[18px]" />}
          >
            New link
          </ButtonLink>
        }
      />

      <Card padded={false}>
        <div className="flex flex-col gap-3 border-b border-ink-100 px-4 py-4 sm:gap-4 sm:px-5 lg:flex-row lg:items-center lg:justify-between">
          <div className="relative lg:w-72">
            <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink-400">
              <svg width="18" height="18" viewBox="0 0 20 20" fill="none" aria-hidden="true">
                <circle cx="9" cy="9" r="6" stroke="currentColor" strokeWidth="1.6" />
                <path d="m13.5 13.5 3 3" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
              </svg>
            </span>
            <input
              type="search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search links…"
              aria-label="Search links"
              className="h-10 w-full rounded-lg border border-ink-200 pl-10 pr-3.5 text-sm transition-colors placeholder:text-ink-400 hover:border-ink-300 focus:border-brand-500 focus:outline-none"
            />
          </div>

          {/* Segmented control. Each option shows its count so an empty result is
              obviously an empty category rather than a broken filter.

              Scrolls horizontally rather than wrapping: four labelled tabs with
              counts exceed a 360px phone, and a wrapped segmented control reads
              as two separate controls. */}
          <div className="scroll-x -mx-1 px-1">
            <div
              className="flex w-max gap-1 rounded-lg bg-ink-100 p-1"
              role="tablist"
              aria-label="Filter links by status"
            >
              {FILTERS.map((option) => (
                <button
                  key={option.id}
                  type="button"
                  role="tab"
                  aria-selected={filter === option.id}
                  onClick={() => setFilter(option.id)}
                  className={[
                    'whitespace-nowrap rounded-md px-3 py-1.5 text-sm font-medium',
                    'transition-all duration-150 active:scale-95',
                    filter === option.id
                      ? 'bg-white text-ink-900 shadow-card'
                      : 'text-ink-500 hover:text-ink-800',
                  ].join(' ')}
                >
                  {option.label}
                  <span className="ml-1.5 text-xs tabular-nums text-ink-400">
                    {counts[option.id]}
                  </span>
                </button>
              ))}
            </div>
          </div>
        </div>

        {error ? (
          <ErrorState message={error} onRetry={reload} />
        ) : (
          <LinksTable
            links={visibleLinks}
            loading={loading}
            onEdit={setEditLink}
            onDelete={setDeleteLink}
            onToggle={toggleLink}
            onShowQr={setQrLink}
            emptyAction={
              <ButtonLink as={Link} to="/dashboard/create" variant="primary">
                Create your first link
              </ButtonLink>
            }
          />
        )}
      </Card>

      <QrModal link={qrLink} open={Boolean(qrLink)} onClose={() => setQrLink(null)} />
      <EditLinkModal
        link={editLink}
        open={Boolean(editLink)}
        onClose={() => setEditLink(null)}
        onSaved={replaceLink}
      />
      <DeleteLinkDialog
        link={deleteLink}
        open={Boolean(deleteLink)}
        onClose={() => setDeleteLink(null)}
        onDeleted={removeLink}
      />
    </>
  );
}
