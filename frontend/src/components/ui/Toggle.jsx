import { useId } from 'react';

/**
 * Switch control for a binary setting that takes effect immediately.
 *
 * Built on a real `<button role="switch">` rather than a styled checkbox. A switch
 * and a checkbox are not the same thing to a screen reader: a checkbox is announced
 * as "checked", which implies a form that still has to be submitted, while a switch
 * is announced as "on" or "off". Here the control reveals a field the moment it is
 * flipped, so "on/off" is the honest description.
 *
 * The label sits outside the button and is wired up with aria-labelledby, so the
 * whole row is not one enormous click target — someone reading the description
 * should be able to select the text without toggling the setting.
 */
export default function Toggle({
  checked,
  onChange,
  label,
  description,
  disabled = false,
  className = '',
}) {
  const labelId = useId();

  return (
    <div className={`flex items-start justify-between gap-4 ${className}`}>
      <div className="min-w-0">
        <p id={labelId} className="text-sm font-medium text-ink-900">
          {label}
        </p>
        {description && (
          <p className="mt-1 text-sm leading-relaxed text-ink-500">{description}</p>
        )}
      </div>

      <button
        type="button"
        role="switch"
        aria-checked={checked}
        aria-labelledby={labelId}
        disabled={disabled}
        onClick={() => onChange(!checked)}
        className={[
          'relative mt-0.5 inline-flex h-6 w-11 shrink-0 items-center rounded-full',
          'transition-colors duration-200 ease-out',
          disabled ? 'cursor-not-allowed opacity-50' : 'cursor-pointer',
          checked ? 'bg-brand-600' : 'bg-ink-200',
        ].join(' ')}
      >
        <span
          className={[
            'inline-block h-[18px] w-[18px] rounded-full bg-white shadow-sm',
            'transition-transform duration-200 ease-out',
            checked ? 'translate-x-[23px]' : 'translate-x-[3px]',
          ].join(' ')}
          aria-hidden="true"
        />
      </button>
    </div>
  );
}
