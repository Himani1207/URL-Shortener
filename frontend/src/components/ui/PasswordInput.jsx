import { useId, useState } from 'react';
import { IconEye, IconEyeOff } from './Icons';

/**
 * Password field with a show/hide control.
 *
 * Reveal exists because these are passwords typed once and shared verbally or in a
 * message — a typo the user cannot see means a link nobody can open, and there is no
 * "forgot password" flow for a link. Masking is still the default, so the field is
 * safe by default and readable on demand.
 *
 * Deliberately not built on top of `Input`: the trailing button needs the input's
 * padding to change with it, and threading that through as another prop would make
 * the shared component worse for every other caller.
 */
export default function PasswordInput({
  label,
  error,
  hint,
  className = '',
  id: providedId,
  ...rest
}) {
  const generatedId = useId();
  const id = providedId ?? generatedId;
  const errorId = `${id}-error`;
  const hintId = `${id}-hint`;

  const [revealed, setRevealed] = useState(false);

  return (
    <div className={className}>
      {label && (
        <label htmlFor={id} className="mb-1.5 block text-sm font-medium text-ink-700">
          {label}
        </label>
      )}

      <div className="relative">
        <input
          id={id}
          type={revealed ? 'text' : 'password'}
          // Not `current-password`: this is a new value being set, and offering to
          // autofill the user's own account password here would be actively wrong.
          autoComplete="new-password"
          spellCheck="false"
          aria-invalid={error ? 'true' : undefined}
          aria-describedby={error ? errorId : hint ? hintId : undefined}
          className={[
            'h-11 w-full rounded-lg border bg-white pl-3.5 pr-11 text-sm text-ink-900',
            'placeholder:text-ink-400',
            'transition-colors duration-150',
            'disabled:cursor-not-allowed disabled:bg-ink-50 disabled:text-ink-400',
            error
              ? 'border-red-400 focus:border-red-500'
              : 'border-ink-200 hover:border-ink-300 focus:border-brand-500',
          ].join(' ')}
          {...rest}
        />

        <button
          type="button"
          onClick={() => setRevealed((shown) => !shown)}
          // tabIndex -1 would hide this from keyboard users, who need reveal most.
          className="absolute right-1 top-1/2 -translate-y-1/2 rounded-md p-2 text-ink-400 transition-colors hover:text-ink-700"
          aria-label={revealed ? 'Hide password' : 'Show password'}
          aria-pressed={revealed}
        >
          {revealed ? (
            <IconEyeOff className="h-[18px] w-[18px]" />
          ) : (
            <IconEye className="h-[18px] w-[18px]" />
          )}
        </button>
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
