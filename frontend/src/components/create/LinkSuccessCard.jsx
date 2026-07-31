import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import Button, { ButtonLink } from '../ui/Button';
import Badge from '../ui/Badge';
import { IconCheck, IconChart, IconCopy, IconDownload, IconExternal, IconPlus } from '../ui/Icons';
import { copyToClipboard, formatDateTime } from '../../lib/format';
import { useToast } from '../../context/ToastContext';
import { QR_SIZES, downloadQrPng, drawQrToCanvas } from '../../lib/qr';

/**
 * What replaces the form once a link exists.
 *
 * Shown in place rather than by navigating away, so the thing the user just made is
 * still on screen when they reach for it. Every value they configured is echoed
 * back — including the ones they left alone — because the most common question
 * immediately after creating a link is "did the expiry actually take?".
 *
 * The QR here is redrawn from the real short URL, not carried over from the preview.
 * The preview encodes a best guess while the alias is still being typed; this one
 * encodes the link that now exists, which is the only version worth downloading.
 */
export default function LinkSuccessCard({ link, qr, onCreateAnother }) {
  const toast = useToast();
  const canvasRef = useRef(null);
  const [copied, setCopied] = useState(false);

  const sizePx = QR_SIZES.find((option) => option.id === qr.size)?.px ?? QR_SIZES[1].px;

  useEffect(() => {
    drawQrToCanvas(canvasRef.current, link.shortUrl, {
      size: sizePx,
      foreground: qr.foreground,
      background: qr.background,
    });
  }, [link.shortUrl, sizePx, qr.foreground, qr.background]);

  useEffect(() => {
    if (!copied) return undefined;
    const timer = window.setTimeout(() => setCopied(false), 1600);
    return () => window.clearTimeout(timer);
  }, [copied]);

  const handleCopy = async () => {
    const ok = await copyToClipboard(link.shortUrl);
    if (ok) setCopied(true);
    else toast.error('Could not copy — copy the link manually');
  };

  const handleDownload = async () => {
    const ok = await downloadQrPng(link.shortUrl, {
      filename: `qr-${link.shortCode}.png`,
      foreground: qr.foreground,
      background: qr.background,
    });
    if (ok) toast.success('QR code downloaded');
    else toast.error('Could not generate the QR code');
  };

  return (
    <div className="animate-slide-up rounded-2xl border border-ink-200 bg-white shadow-card">
      <div className="flex items-start gap-4 border-b border-ink-100 px-6 py-6 sm:px-8">
        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-emerald-50 text-emerald-600">
          <IconCheck className="h-5 w-5" />
        </span>
        <div className="min-w-0">
          <h2 className="text-lg font-semibold tracking-tight text-ink-900">
            Your link is live
          </h2>
          <p className="mt-1 text-sm leading-relaxed text-ink-500">
            It works immediately. Clicks are recorded from the first visit.
          </p>
        </div>
      </div>

      <div className="grid gap-8 px-6 py-7 sm:px-8 lg:grid-cols-[minmax(0,1fr)_auto] lg:gap-10">
        <div className="min-w-0">
          {/* The short URL gets its own emphasised block — it is the one thing on
              this card the user came for. */}
          <div className="rounded-xl border border-brand-200 bg-brand-50/60 px-4 py-3.5">
            <p className="text-xs font-medium uppercase tracking-wide text-brand-700">
              Short link
            </p>
            <div className="mt-1.5 flex items-center gap-2">
              <a
                href={link.shortUrl}
                target="_blank"
                rel="noreferrer"
                className="min-w-0 flex-1 truncate font-mono text-sm font-medium text-brand-800 hover:underline"
              >
                {link.shortUrl}
              </a>
            </div>
          </div>

          <dl className="mt-6 space-y-4">
            <DetailRow label="Destination">
              <a
                href={link.originalUrl}
                target="_blank"
                rel="noreferrer"
                className="break-all text-ink-700 hover:text-brand-600 hover:underline"
              >
                {link.originalUrl}
              </a>
            </DetailRow>

            <DetailRow label="Custom alias">
              {/* The server echoes the code it actually assigned, so a generated
                  code is distinguishable from an alias the user chose. */}
              {link.customAlias ? (
                <span className="font-mono text-ink-700">/{link.customAlias}</span>
              ) : (
                <span className="text-ink-400">None — code generated automatically</span>
              )}
            </DetailRow>

            <DetailRow label="Expires">
              {link.expiresAt ? (
                <span className="text-ink-700">{formatDateTime(link.expiresAt)}</span>
              ) : (
                <span className="text-ink-400">Never</span>
              )}
            </DetailRow>

            <DetailRow label="Password protected">
              <Badge tone={link.passwordProtected ? 'brand' : 'neutral'}>
                {link.passwordProtected ? 'Yes' : 'No'}
              </Badge>
            </DetailRow>
          </dl>
        </div>

        <div className="flex flex-col items-center lg:items-start">
          <div
            className="flex items-center justify-center rounded-xl border border-ink-200 p-4"
            style={{ backgroundColor: qr.background }}
          >
            <canvas
              ref={canvasRef}
              aria-hidden="true"
              className="rounded"
              style={{ width: sizePx, height: sizePx }}
            />
          </div>
          <p className="mt-2.5 text-center text-xs text-ink-400 lg:text-left">
            Scans count as clicks
          </p>
        </div>
      </div>

      <div className="flex flex-col gap-3 border-t border-ink-100 px-6 py-5 sm:flex-row sm:flex-wrap sm:items-center sm:px-8">
        <Button
          onClick={handleCopy}
          icon={
            copied ? (
              <IconCheck className="h-[18px] w-[18px]" />
            ) : (
              <IconCopy className="h-[18px] w-[18px]" />
            )
          }
        >
          {copied ? 'Copied' : 'Copy link'}
        </Button>

        <ButtonLink
          href={link.shortUrl}
          target="_blank"
          rel="noreferrer"
          icon={<IconExternal className="h-[18px] w-[18px]" />}
        >
          Open link
        </ButtonLink>

        <Button
          variant="secondary"
          onClick={handleDownload}
          icon={<IconDownload className="h-[18px] w-[18px]" />}
        >
          Download QR
        </Button>

        {/* Analytics deliberately lives on its own page rather than being unfolded
            here — a link created seconds ago has nothing to show yet. */}
        <ButtonLink
          as={Link}
          to={`/dashboard/analytics/${link.shortCode}`}
          icon={<IconChart className="h-[18px] w-[18px]" />}
        >
          View analytics
        </ButtonLink>

        <Button
          variant="ghost"
          onClick={onCreateAnother}
          icon={<IconPlus className="h-[18px] w-[18px]" />}
          className="sm:ml-auto"
        >
          Create another
        </Button>
      </div>
    </div>
  );
}

/** One label/value pair in the summary list. */
function DetailRow({ label, children }) {
  return (
    <div className="grid gap-1 sm:grid-cols-[9rem_minmax(0,1fr)] sm:gap-4">
      <dt className="text-sm text-ink-500">{label}</dt>
      <dd className="min-w-0 text-sm">{children}</dd>
    </div>
  );
}
