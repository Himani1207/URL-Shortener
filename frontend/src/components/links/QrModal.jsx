import { useEffect, useState } from 'react';
import Modal from '../ui/Modal';
import Button from '../ui/Button';
import CopyButton from './CopyButton';
import { Spinner } from '../ui/States';
import { IconDownload } from '../ui/Icons';
import { linksApi } from '../../lib/api';
import { useToast } from '../../context/ToastContext';

/**
 * QR preview and download.
 *
 * The QR endpoint is authenticated, so `<img src="/api/urls/x/qr">` cannot work —
 * an image tag sends no Authorization header. The image is fetched as a Blob and
 * wrapped in an object URL instead.
 *
 * Object URLs hold their blob in memory until explicitly revoked, so the cleanup
 * in the effect is not optional: without it, opening this modal repeatedly leaks
 * a PNG each time for the lifetime of the page.
 */
export default function QrModal({ link, open, onClose }) {
  const toast = useToast();
  const [objectUrl, setObjectUrl] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!open || !link) return undefined;

    let cancelled = false;
    let createdUrl = null;

    setLoading(true);
    linksApi
      .qrCodeBlob(link.shortCode)
      .then((blob) => {
        if (cancelled) return;
        createdUrl = URL.createObjectURL(blob);
        setObjectUrl(createdUrl);
      })
      .catch(() => {
        if (!cancelled) toast.error('Could not load the QR code');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
      if (createdUrl) URL.revokeObjectURL(createdUrl);
      setObjectUrl(null);
    };
  }, [open, link, toast]);

  const handleDownload = async () => {
    try {
      // Re-fetched at 1000px: the preview is 300px, which is too coarse for print.
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

  if (!link) return null;

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="QR code"
      description="Scans route through the short link, so they are counted in analytics."
      size="sm"
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
          <Button onClick={handleDownload} icon={<IconDownload className="h-[18px] w-[18px]" />}>
            Download PNG
          </Button>
        </>
      }
    >
      <div className="flex flex-col items-center">
        <div className="flex h-[248px] w-[248px] items-center justify-center rounded-xl border border-ink-200 bg-white p-3">
          {loading && <Spinner className="h-6 w-6 text-brand-600" />}
          {!loading && objectUrl && (
            <img
              src={objectUrl}
              alt={`QR code for ${link.shortUrl}`}
              className="h-full w-full animate-fade-in"
            />
          )}
          {!loading && !objectUrl && (
            <p className="px-4 text-center text-sm text-ink-500">QR code unavailable</p>
          )}
        </div>

        <div className="mt-5 flex w-full items-center justify-center gap-1 rounded-lg bg-ink-50 px-3 py-2.5">
          <span className="truncate font-mono text-[13px] text-ink-700">{link.shortUrl}</span>
          <CopyButton value={link.shortUrl} />
        </div>
      </div>
    </Modal>
  );
}
