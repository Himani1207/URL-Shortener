import { useEffect, useRef, useState } from 'react';
import ColorField from '../ui/ColorField';
import { IconQr } from '../ui/Icons';
import { QR_SIZES, drawQrToCanvas, hasScannableContrast } from '../../lib/qr';

/**
 * Live QR preview with the three customisations the brief allows: foreground
 * colour, background colour and size.
 *
 * The code is redrawn synchronously on every change. That is deliberate rather than
 * debounced — encoding a URL and filling a few hundred rectangles is well under a
 * frame, and a preview that lags behind the input it is previewing defeats the point
 * of having one.
 *
 * Contrast is checked rather than restricted. Someone picking two similar colours
 * gets a warning and can still proceed; silently clamping their choice would be
 * worse, because they would never learn why the code they downloaded does not scan.
 */
/**
 * Presets drawn from the product's own palette rather than from a generic swatch
 * set. Every one of these is dark enough to scan against a light background, so a
 * preset can never produce an unreadable code — the contrast warning below is for
 * hand-picked values only.
 */
const FOREGROUND_PRESETS = ['#1c1b17', '#26485f', '#3f3d38', '#7a2e2a', '#2f5241'];
const BACKGROUND_PRESETS = ['#ffffff', '#faf9f6', '#f4f2ec', '#efece4', '#eff2f5'];

export default function QrPreview({
  value,
  foreground,
  background,
  size,
  onForegroundChange,
  onBackgroundChange,
  onSizeChange,
  caption,
}) {
  const canvasRef = useRef(null);
  const [rendered, setRendered] = useState(false);

  const sizePx = QR_SIZES.find((option) => option.id === size)?.px ?? QR_SIZES[1].px;

  useEffect(() => {
    if (!value) {
      setRendered(false);
      return;
    }
    setRendered(
      drawQrToCanvas(canvasRef.current, value, { size: sizePx, foreground, background }),
    );
  }, [value, sizePx, foreground, background]);

  const lowContrast = rendered && !hasScannableContrast(foreground, background);

  return (
    <div className="space-y-6">
      {/* Fixed-height stage so switching size does not shunt the controls below it
          up and down the page. Sized for the largest option plus its padding. */}
      <div className="flex h-[336px] items-center justify-center rounded-xl border border-ink-200 bg-ink-50/60 p-6">
        {/* The canvas is always mounted and merely hidden when there is nothing to
            show. Rendering it conditionally on `rendered` would deadlock: the flag
            is set by drawing into this element, so the element has to exist before
            the draw can succeed, and it would never be created. */}
        <canvas
          ref={canvasRef}
          // A rendering of the value named in the caption beneath it, so
          // announcing it separately would only repeat that.
          aria-hidden="true"
          className={rendered ? 'animate-fade-in rounded-lg' : 'hidden'}
          style={{ width: sizePx, height: sizePx }}
        />

        {!rendered && (
          <div className="px-6 text-center">
            <span className="mx-auto flex h-11 w-11 items-center justify-center rounded-xl bg-white text-ink-300 shadow-card">
              <IconQr className="h-5 w-5" />
            </span>
            <p className="mt-3 text-sm text-ink-500">
              {value ? 'This link is too long to encode' : 'Your QR code appears here'}
            </p>
            {!value && (
              <p className="mt-1 text-xs text-ink-400">
                Start typing a destination URL to see it
              </p>
            )}
          </div>
        )}
      </div>

      {rendered && caption && (
        <p className="-mt-2 text-center text-xs text-ink-400">{caption}</p>
      )}

      <div>
        <p className="mb-2 text-sm font-medium text-ink-700">Size</p>
        <div className="flex gap-1 rounded-lg bg-ink-100 p-1" role="group" aria-label="QR code size">
          {QR_SIZES.map((option) => (
            <button
              key={option.id}
              type="button"
              onClick={() => onSizeChange(option.id)}
              aria-pressed={size === option.id}
              className={[
                'flex-1 rounded-md px-3 py-1.5 text-sm font-medium',
                'transition-all duration-150 active:scale-95',
                size === option.id
                  ? 'bg-white text-ink-900 shadow-card'
                  : 'text-ink-500 hover:text-ink-800',
              ].join(' ')}
            >
              {option.label}
            </button>
          ))}
        </div>
        <p className="mt-1.5 text-xs text-ink-400">
          Affects the preview only — downloads are always 1024&nbsp;px.
        </p>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <ColorField
          label="Foreground"
          value={foreground}
          onChange={onForegroundChange}
          presets={FOREGROUND_PRESETS}
        />
        <ColorField
          label="Background"
          value={background}
          onChange={onBackgroundChange}
          presets={BACKGROUND_PRESETS}
        />
      </div>

      {lowContrast && (
        <p className="flex items-start gap-2 rounded-lg bg-amber-50 px-3 py-2.5 text-sm text-amber-800 animate-fade-in">
          <svg
            className="mt-0.5 h-4 w-4 shrink-0"
            viewBox="0 0 20 20"
            fill="none"
            aria-hidden="true"
          >
            <circle cx="10" cy="10" r="8.25" stroke="currentColor" strokeWidth="1.5" />
            <path d="M10 6v4.5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
            <circle cx="10" cy="13.6" r="1" fill="currentColor" />
          </svg>
          These colours are too close together. Most scanners will fail to read the
          code — pick a darker foreground or a lighter background.
        </p>
      )}
    </div>
  );
}
