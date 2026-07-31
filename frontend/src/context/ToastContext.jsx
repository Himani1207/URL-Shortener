import { createContext, useCallback, useContext, useMemo, useState } from 'react';

/**
 * Lightweight toast notifications.
 *
 * Every mutation in the dashboard — copy, delete, pause, save — needs to confirm
 * itself. Without feedback the user cannot tell a successful delete from a silent
 * failure. Kept to a context rather than a library because the whole requirement
 * is "show a line of text for a few seconds".
 */
const ToastContext = createContext(null);

let nextId = 0;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const dismiss = useCallback((id) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const show = useCallback(
    (message, tone = 'success', duration = 3200) => {
      const id = ++nextId;
      setToasts((current) => [...current, { id, message, tone }]);
      // Errors linger a little longer - they usually need reading, not glancing at.
      window.setTimeout(() => dismiss(id), tone === 'error' ? duration + 1500 : duration);
    },
    [dismiss],
  );

  const value = useMemo(
    () => ({
      success: (message) => show(message, 'success'),
      error: (message) => show(message, 'error'),
    }),
    [show],
  );

  return (
    <ToastContext.Provider value={value}>
      {children}

      {/* aria-live so screen readers announce the toast; a purely visual
          confirmation is no confirmation for a non-sighted user. */}
      <div
        className="pointer-events-none fixed bottom-6 right-6 z-50 flex w-full max-w-sm flex-col gap-2"
        role="status"
        aria-live="polite"
      >
        {toasts.map((toast) => (
          <div
            key={toast.id}
            className={[
              'pointer-events-auto flex items-start gap-3 rounded-xl border px-4 py-3',
              'shadow-dropdown animate-slide-in-right',
              toast.tone === 'error'
                ? 'border-red-200 bg-red-50 text-red-800'
                : 'border-emerald-200 bg-emerald-50 text-emerald-800',
            ].join(' ')}
          >
            <span className="mt-0.5 shrink-0" aria-hidden="true">
              {toast.tone === 'error' ? <IconAlert /> : <IconCheck />}
            </span>
            <p className="text-sm font-medium leading-relaxed">{toast.message}</p>
            <button
              type="button"
              onClick={() => dismiss(toast.id)}
              className="ml-auto shrink-0 rounded p-0.5 opacity-50 transition hover:opacity-100"
              aria-label="Dismiss notification"
            >
              <IconClose />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used inside a <ToastProvider>');
  }
  return context;
}

function IconCheck() {
  return (
    <svg width="18" height="18" viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <circle cx="10" cy="10" r="9" stroke="currentColor" strokeWidth="1.5" />
      <path d="m6.5 10.5 2.4 2.4 4.6-5.3" stroke="currentColor" strokeWidth="1.8"
            strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function IconAlert() {
  return (
    <svg width="18" height="18" viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <circle cx="10" cy="10" r="9" stroke="currentColor" strokeWidth="1.5" />
      <path d="M10 5.8v5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
      <circle cx="10" cy="13.8" r="1" fill="currentColor" />
    </svg>
  );
}

function IconClose() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="m4 4 8 8M12 4l-8 8" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
    </svg>
  );
}
