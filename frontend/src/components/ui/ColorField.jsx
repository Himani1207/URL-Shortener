import { useId } from 'react';

/**
 * Colour picker: a native swatch plus the hex value as editable text.
 *
 * Both halves are offered because they serve different people. The swatch opens the
 * operating system's picker, which is the fast way to browse. The text field is the
 * only way to enter an exact brand hex, which is what anyone customising a QR code
 * for a campaign actually wants.
 *
 * `<input type="color">` only ever emits a valid `#rrggbb`, so the swatch cannot
 * produce a bad value. The text field can, while it is being typed — "#0f5" is a
 * legitimate intermediate state — so it is committed to the caller only once it
 * parses. The field keeps showing whatever was typed either way, because snapping
 * the text back mid-edit makes the input feel broken.
 */
const HEX = /^#[0-9a-f]{6}$/i;

export default function ColorField({ label, value, onChange, presets = [], className = '' }) {
  const id = useId();

  const commit = (next) => {
    const candidate = next.startsWith('#') ? next : `#${next}`;
    if (HEX.test(candidate)) onChange(candidate.toLowerCase());
  };

  return (
    <div className={className}>
      <label htmlFor={id} className="mb-1.5 block text-sm font-medium text-ink-700">
        {label}
      </label>

      <div className="flex items-center gap-2">
        <div className="relative h-9 w-9 shrink-0 overflow-hidden rounded-lg border border-ink-200">
          <input
            id={id}
            type="color"
            value={HEX.test(value) ? value : '#000000'}
            onChange={(event) => onChange(event.target.value.toLowerCase())}
            // Scaled up and clipped by the wrapper: browsers add their own padding
            // inside a colour input, which leaves a pale border around the swatch.
            className="absolute -inset-2 h-[calc(100%+1rem)] w-[calc(100%+1rem)] cursor-pointer border-0 bg-transparent p-0"
            aria-label={`${label} swatch`}
          />
        </div>

        <input
          type="text"
          value={value}
          onChange={(event) => commit(event.target.value)}
          spellCheck="false"
          maxLength={7}
          aria-label={`${label} hex value`}
          className="h-9 w-full min-w-0 rounded-lg border border-ink-200 px-2.5 font-mono text-xs uppercase text-ink-700 transition-colors hover:border-ink-300 focus:border-brand-500 focus:outline-none"
        />
      </div>

      {presets.length > 0 && (
        <div className="mt-2 flex flex-wrap gap-1.5">
          {presets.map((preset) => (
            <button
              key={preset}
              type="button"
              onClick={() => onChange(preset)}
              aria-label={`Set ${label.toLowerCase()} to ${preset}`}
              aria-pressed={value.toLowerCase() === preset.toLowerCase()}
              className={[
                'h-6 w-6 rounded-md border transition-transform duration-150 hover:scale-110',
                value.toLowerCase() === preset.toLowerCase()
                  ? 'border-brand-600 ring-2 ring-brand-600 ring-offset-1'
                  : 'border-ink-200',
              ].join(' ')}
              style={{ backgroundColor: preset }}
            />
          ))}
        </div>
      )}
    </div>
  );
}
