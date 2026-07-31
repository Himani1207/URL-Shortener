import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../../components/layout/PageHeader';
import Card from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import QrModal from '../../components/links/QrModal';
import { EmptyState, ErrorState, Spinner } from '../../components/ui/States';
import { IconDownload, IconQr } from '../../components/ui/Icons';
import { hostnameOf } from '../../lib/format';
import { linksApi } from '../../lib/api';
import { useLinks } from '../../hooks/useLinks';
import { useToast } from '../../context/ToastContext';

/**
 * QR code gallery.
 *
 * Each tile fetches its own PNG through {@link QrTile}. Fetching them all up front
 * in the page would mean one blocking wait for the whole grid, and the images are
 * independent — this way tiles fill in as they arrive.
 */
export default function QrCodes() {
  const { links, loading, error, reload } = useLinks();
  const [previewLink, setPreviewLink] = useState(null);

  // Paused and expired links still have QR codes, but scanning one leads to the
  // unavailable page, so they are not worth putting in a gallery meant for
  // printing and sharing.
  const printableLinks = links.filter((link) => link.active && !link.expired);

  return (
    <>
      <PageHeader
        title="QR codes"
        description="Every active link has a QR code. Scans route through the short link, so they are counted as clicks."
      />

      {error && (
        <Card>
          <ErrorState message={error} onRetry={reload} />
        </Card>
      )}

      {!error && loading && (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, index) => (
            <div key={index} className="h-64 animate-pulse rounded-xl bg-ink-100" />
          ))}
        </div>
      )}

      {!error && !loading && printableLinks.length === 0 && (
        <Card>
          <EmptyState
            icon={<IconQr className="h-6 w-6" />}
            title="No active links"
            description="Create a link — or resume a paused one — and its QR code will appear here."
            action={
              <Link to="/dashboard/links">
                <Button>Go to links</Button>
              </Link>
            }
          />
        </Card>
      )}

      {!error && !loading && printableLinks.length > 0 && (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {printableLinks.map((link) => (
            <QrTile key={link.id} link={link} onPreview={() => setPreviewLink(link)} />
          ))}
        </div>
      )}

      <QrModal
        link={previewLink}
        open={Boolean(previewLink)}
        onClose={() => setPreviewLink(null)}
      />
    </>
  );
}

/**
 * One QR card.
 *
 * Same object-URL lifecycle as the modal: the blob stays in memory until
 * revoked, so the cleanup function is required, not defensive. A gallery of
 * thirty links would otherwise hold thirty PNGs for the life of the page.
 */
function QrTile({ link, onPreview }) {
  const toast = useToast();
  const [objectUrl, setObjectUrl] = useState(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let cancelled = false;
    let createdUrl = null;

    linksApi
      .qrCodeBlob(link.shortCode)
      .then((blob) => {
        if (cancelled) return;
        createdUrl = URL.createObjectURL(blob);
        setObjectUrl(createdUrl);
      })
      .catch(() => {
        if (!cancelled) setFailed(true);
      });

    return () => {
      cancelled = true;
      if (createdUrl) URL.revokeObjectURL(createdUrl);
    };
  }, [link.shortCode]);

  const handleDownload = async (event) => {
    event.stopPropagation();
    try {
      // 1000px rather than the 300px preview: the preview is too coarse to print.
      const blob = await linksApi.qrCodeBlob(link.shortCode, 1000);
      const url = URL.createObjectURL(blob);

      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = `qr-${link.shortCode}.png`;
      document.body.appendChild(anchor);
      anchor.click();
      document.body.removeChild(anchor);

      URL.revokeObjectURL(url);
      toast.success('QR code downloaded');
    } catch {
      toast.error('Could not download the QR code');
    }
  };

  return (
    <div className="group rounded-lg border border-ink-200 bg-white p-5 transition-colors hover:border-ink-400">
      <button
        type="button"
        onClick={onPreview}
        className="flex h-40 w-full items-center justify-center rounded-lg bg-ink-50"
        aria-label={`Preview QR code for /${link.shortCode}`}
      >
        {objectUrl && (
          <img
            src={objectUrl}
            alt=""
            className="h-36 w-36 animate-fade-in rounded bg-white p-1.5"
          />
        )}
        {!objectUrl && !failed && <Spinner className="h-5 w-5 text-brand-600" />}
        {failed && <span className="text-sm text-ink-400">Unavailable</span>}
      </button>

      <div className="mt-4 min-w-0">
        <p className="truncate font-mono text-sm text-brand-600">/{link.shortCode}</p>
        <p className="mt-0.5 truncate text-xs text-ink-500">{hostnameOf(link.originalUrl)}</p>
      </div>

      <div className="mt-4 flex gap-2">
        <Button variant="secondary" size="sm" fullWidth onClick={onPreview}>
          Preview
        </Button>
        <Button
          variant="secondary"
          size="sm"
          onClick={handleDownload}
          aria-label={`Download QR code for /${link.shortCode}`}
          icon={<IconDownload className="h-[18px] w-[18px]" />}
        />
      </div>
    </div>
  );
}
