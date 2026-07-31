import { useId } from 'react';

/**
 * Labelled text input with inline error support.
 *
 * The label is always rendered and tied to the input via a generated id — a
 * placeholder is not a label: it disappears the moment someone types, and screen
 * readers do not reliably announce it.
 *
 * `error` is wired through aria-invalid and aria-describedby so an assistive
 * technology reports the same failure a sighted user sees in red. The backend's
 * ErrorResponse.fieldErrors map feeds straight into this prop.
 */
export default function Input({
  label,
  error,
  hint,
  prefix,
  className = '',
  id: providedId,
  ...rest
}) {
  const generatedId = useId();
  const id = providedId ?? generatedId;
  const errorId = `${id}-error`;
  const hintId = `${id}-hint`;

  return (
    <div className={className}>
      {label && (
        <label htmlFor={id} className="mb-1.5 block text-sm font-medium text-ink-700">
          {label}
        </label>
      )}

      <div className="relative">
        {prefix && (
          <span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-sm text-ink-400">
            {prefix}
          </span>
        )}

        <input
          id={id}
          aria-invalid={error ? 'true' : undefined}
          aria-describedby={error ? errorId : hint ? hintId : undefined}
          className={[
            'h-11 w-full rounded-lg border bg-white text-sm text-ink-900',
            'placeholder:text-ink-400',
            'transition-colors duration-150',
            'disabled:cursor-not-allowed disabled:bg-ink-50 disabled:text-ink-400',
            prefix ? 'pl-[var(--prefix-pad,4.5rem)] pr-3.5' : 'px-3.5',
            error
              ? 'border-red-400 focus:border-red-500'
              : 'border-ink-200 hover:border-ink-300 focus:border-brand-500',
          ].join(' ')}
          style={prefix ? { paddingLeft: `${prefix.length * 0.55 + 1.75}rem` } : undefined}
          {...rest}
        />
      </div>

      {error && (
        <p id={errorId} className="mt-1.5 text-sm text-red-600">
          {error}
        </p>
      )}
      {!error && hint && (
        <p id={hintId} className="mt-1.5 text-sm text-ink-500">
          {hint}
        </p>
      )}
    </div>
  );
}
