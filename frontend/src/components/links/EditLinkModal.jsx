import { useEffect, useState } from 'react';
import Modal from '../ui/Modal';
import Button from '../ui/Button';
import Input from '../ui/Input';
import { linksApi, ApiError } from '../../lib/api';
import { useToast } from '../../context/ToastContext';

/**
 * Edit a link's destination, expiry and active state.
 *
 * The short code is shown read-only. It is genuinely immutable server-side —
 * changing it would break every copy of the link already in circulation — and
 * showing it disabled communicates that better than omitting it, which would just
 * look like a missing feature.
 */
export default function EditLinkModal({ link, open, onClose, onSaved }) {
  const toast = useToast();

  const [originalUrl, setOriginalUrl] = useState('');
  const [expiresAt, setExpiresAt] = useState('');
  const [active, setActive] = useState(true);
  const [saving, setSaving] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});

  // Re-seed whenever a different link is opened, so the form never shows the
  // previous row's values for a moment.
  useEffect(() => {
    if (!link) return;
    setOriginalUrl(link.originalUrl ?? '');
    // <input type="datetime-local"> wants "YYYY-MM-DDTHH:mm" with no zone or
    // seconds; the API sends full ISO-8601, so trim to the first 16 characters.
    setExpiresAt(link.expiresAt ? link.expiresAt.slice(0, 16) : '');
    setActive(Boolean(link.active));
    setFieldErrors({});
  }, [link]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setFieldErrors({});
    setSaving(true);

    try {
      const updated = await linksApi.update(link.id, {
        originalUrl: originalUrl.trim(),
        // null tells the backend "leave unchanged"; there is deliberately no way
        // to clear an expiry from this form, which would need a distinct signal.
        expiresAt: expiresAt || null,
        active,
      });

      toast.success('Link updated');
      onSaved?.(updated);
      onClose();
    } catch (error) {
      if (error instanceof ApiError && error.fieldErrors) {
        setFieldErrors(error.fieldErrors);
      } else {
        toast.error(error.message ?? 'Could not update the link');
      }
    } finally {
      setSaving(false);
    }
  };

  if (!link) return null;

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Edit link"
      description="Changes take effect immediately for anyone who opens the link."
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={saving}>
            Cancel
          </Button>
          <Button type="submit" form="edit-link-form" loading={saving}>
            Save changes
          </Button>
        </>
      }
    >
      <form id="edit-link-form" onSubmit={handleSubmit} className="space-y-5">
        <Input
          label="Short link"
          value={link.shortUrl}
          readOnly
          disabled
          hint="The short code cannot be changed — existing copies of this link would stop working."
        />

        <Input
          label="Destination URL"
          type="text"
          inputMode="url"
          spellCheck="false"
          value={originalUrl}
          onChange={(event) => setOriginalUrl(event.target.value)}
          error={fieldErrors.originalUrl}
        />

        <Input
          label="Expires on"
          type="datetime-local"
          value={expiresAt}
          onChange={(event) => setExpiresAt(event.target.value)}
          error={fieldErrors.expiresAt}
        />

        <label className="flex cursor-pointer items-start gap-3 rounded-lg border border-ink-200 p-4 transition-colors hover:bg-ink-50">
          <input
            type="checkbox"
            checked={active}
            onChange={(event) => setActive(event.target.checked)}
            className="mt-0.5 h-4 w-4 rounded border-ink-300 text-brand-600 focus:ring-brand-500"
          />
          <span>
            <span className="block text-sm font-medium text-ink-900">Link is active</span>
            <span className="mt-0.5 block text-sm text-ink-500">
              Pausing keeps the link and its click history, but visitors see an
              unavailable page instead of the destination.
            </span>
          </span>
        </label>
      </form>
    </Modal>
  );
}
