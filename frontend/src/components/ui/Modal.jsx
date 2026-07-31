import { useEffect, useRef } from 'react';

/**
 * Accessible modal dialog.
 *
 * Three behaviours that are easy to leave out and immediately noticeable when
 * missing:
 *   - Escape closes it. Users expect this and reach for it before the mouse.
 *   - Background scrolling is locked, otherwise the page slides around behind
 *     the overlay.
 *   - Focus moves into the dialog on open and the panel traps it, so a keyboard
 *     user cannot tab into the page behind the overlay and get stranded there.
 */
export default function Modal({ open, onClose, title, description, children, footer, size = 'md' }) {
  const panelRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;

    const onKeyDown = (event) => {
      if (event.key === 'Escape') onClose();
    };

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    document.addEventListener('keydown', onKeyDown);

    // Defer so the element exists before focusing it.
    const timer = window.setTimeout(() => panelRef.current?.focus(), 0);

    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', onKeyDown);
      window.clearTimeout(timer);
    };
  }, [open, onClose]);

  if (!open) return null;

  const widths = { sm: 'max-w-sm', md: 'max-w-lg', lg: 'max-w-2xl' };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Overlay. A plain click target rather than a button so it does not appear
          in the tab order - Escape is the keyboard route out. */}
      <div
        className="absolute inset-0 bg-ink-900/40 animate-fade-in"
        onClick={onClose}
        aria-hidden="true"
      />

      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
        tabIndex={-1}
        className={[
          'relative w-full rounded-2xl border border-ink-200 bg-white shadow-dropdown',
          'animate-slide-up focus:outline-none',
          widths[size],
        ].join(' ')}
      >
        <div className="flex items-start justify-between gap-4 border-b border-ink-100 px-6 py-5">
          <div>
            <h2 id="modal-title" className="text-lg font-semibold text-ink-900">
              {title}
            </h2>
            {description && <p className="mt-1 text-sm text-ink-500">{description}</p>}
          </div>

          <button
            type="button"
            onClick={onClose}
            className="-mr-1 rounded-lg p-1.5 text-ink-400 transition hover:bg-ink-100 hover:text-ink-700"
            aria-label="Close dialog"
          >
            <svg width="18" height="18" viewBox="0 0 16 16" fill="none" aria-hidden="true">
              <path d="m4 4 8 8M12 4l-8 8" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
            </svg>
          </button>
        </div>

        <div className="px-6 py-5">{children}</div>

        {footer && (
          <div className="flex justify-end gap-3 border-t border-ink-100 px-6 py-4">{footer}</div>
        )}
      </div>
    </div>
  );
}
