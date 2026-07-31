import { useEffect, useState } from 'react';
import { IconCheck, IconCopy } from '../ui/Icons';
import { copyToClipboard } from '../../lib/format';
import { useToast } from '../../context/ToastContext';

/**
 * Copy-to-clipboard control with inline confirmation.
 *
 * The icon swaps to a tick for ~1.6s after a successful copy. That in-place
 * feedback matters more than the toast: the user's attention is already on the
 * button they just pressed, and copy is the single most-used action in the table.
 *
 * The timer is cleared on unmount so a state update cannot fire against a
 * component that has been removed — easy to hit here, since deleting a row
 * unmounts its buttons.
 */
export default function CopyButton({ value, label = 'Copy short link', className = '' }) {
  const toast = useToast();
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!copied) return undefined;
    const timer = window.setTimeout(() => setCopied(false), 1600);
    return () => window.clearTimeout(timer);
  }, [copied]);

  const handleCopy = async () => {
    const ok = await copyToClipboard(value);
    if (ok) {
      setCopied(true);
    } else {
      // Fails on a non-secure origin with execCommand unavailable. Say so rather
      // than leaving the user to wonder why paste gives them the wrong thing.
      toast.error('Could not copy — copy the link manually');
    }
  };

  return (
    <button
      type="button"
      onClick={handleCopy}
      aria-label={copied ? 'Copied' : label}
      title={copied ? 'Copied' : label}
      className={[
        'inline-flex h-8 w-8 items-center justify-center rounded-lg',
        'transition-colors duration-150',
        copied ? 'text-emerald-600' : 'text-ink-400 hover:bg-ink-100 hover:text-ink-700',
        className,
      ].join(' ')}
    >
      {copied ? <IconCheck className="h-[18px] w-[18px]" /> : <IconCopy className="h-[18px] w-[18px]" />}
    </button>
  );
}
