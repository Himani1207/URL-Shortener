import { useState } from 'react';
import Modal from '../ui/Modal';
import Button from '../ui/Button';
import { linksApi } from '../../lib/api';
import { useToast } from '../../context/ToastContext';

/**
 * Confirmation before deleting a link.
 *
 * Deletion is irreversible and takes the click history with it, so it gets an
 * explicit confirmation rather than an undo toast — an undo would have to keep
 * the row alive server-side, which the API does not do.
 *
 * The dialog names the specific link and states that analytics go too. A generic
 * "Are you sure?" gets clicked through without being read.
 */
export default function DeleteLinkDialog({ link, open, onClose, onDeleted }) {
  const toast = useToast();
  const [deleting, setDeleting] = useState(false);

  const handleDelete = async () => {
    setDeleting(true);
    try {
      await linksApi.remove(link.id);
      toast.success('Link deleted');
      onDeleted?.(link);
      onClose();
    } catch (error) {
      toast.error(error.message ?? 'Could not delete the link');
    } finally {
      setDeleting(false);
    }
  };

  if (!link) return null;

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Delete this link?"
      size="sm"
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={deleting}>
            Cancel
          </Button>
          <Button variant="danger" onClick={handleDelete} loading={deleting}>
            Delete link
          </Button>
        </>
      }
    >
      <p className="text-sm leading-relaxed text-ink-600">
        <span className="font-mono text-ink-900">/{link.shortCode}</span> will stop
        working immediately, and its{' '}
        <strong className="font-medium text-ink-900">
          {link.clickCount ?? 0} recorded {link.clickCount === 1 ? 'click' : 'clicks'}
        </strong>{' '}
        will be permanently deleted. This cannot be undone.
      </p>

      <p className="mt-4 rounded-lg bg-amber-50 px-3.5 py-3 text-sm text-amber-800 ring-1 ring-inset ring-amber-600/20">
        If you only want to stop the link temporarily, pause it instead — that keeps
        the analytics.
      </p>
    </Modal>
  );
}
