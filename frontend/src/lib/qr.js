import qrcodeGenerator from 'qrcode-generator';

/**
 * Client-side QR rendering.
 *
 * <b>Why this exists alongside the server's /qr endpoint.</b> The server renders a
 * black-on-white PNG for a link that already exists, which is exactly right for the
 * QR gallery. The Create page needs something the server cannot give it: a code that
 * redraws on every keystroke, in colours the user is still choosing, for a link that
 * has not been created yet. Round-tripping that to the API would mean a request per
 * character typed. So: preview and download are rendered here, and the gallery keeps
 * using the cached server endpoint.
 *
 * `qrcode-generator` is the encoder only — it has no dependencies and returns a
 * module matrix. Everything about how that matrix is painted lives in this file, so
 * the preview on screen and the downloaded PNG are drawn by the same code and cannot
 * disagree.
 */

/**
 * Error-correction level.
 *
 * 'M' recovers ~15% of a damaged code. 'L' would make the matrix slightly less dense
 * for long URLs, but these codes end up printed, photocopied and photographed at an
 * angle, and the density difference is not worth the failed scans.
 */
const ERROR_CORRECTION = 'M';

/**
 * Quiet-zone width, in modules.
 *
 * The QR specification requires 4. Trimming it makes the code look tidier and
 * measurably harder for scanners to find, so it is not configurable.
 */
const QUIET_ZONE = 4;

/** The three sizes offered in the UI, in CSS pixels. */
export const QR_SIZES = [
  { id: 'small', label: 'Small', px: 160 },
  { id: 'medium', label: 'Medium', px: 224 },
  { id: 'large', label: 'Large', px: 288 },
];

/** Resolution of the downloaded PNG, regardless of the preview size on screen. */
export const QR_DOWNLOAD_PX = 1024;

/** The ink from the palette, not pure black — it matches the rest of the UI. */
export const DEFAULT_QR_FOREGROUND = '#1c1b17';
export const DEFAULT_QR_BACKGROUND = '#ffffff';

/**
 * Draws `text` as a QR code onto a canvas.
 *
 * The canvas is sized in device pixels and scaled back down with CSS, so the code
 * stays crisp on a retina display instead of being upscaled from a 224px bitmap.
 *
 * @param {HTMLCanvasElement} canvas
 * @param {string} text            payload to encode
 * @param {object} options
 * @param {number} options.size    drawn size in CSS pixels
 * @param {string} options.foreground  CSS colour for the dark modules
 * @param {string} options.background  CSS colour for the light modules
 * @param {number} [options.pixelRatio] device pixel ratio; defaults to the window's
 * @returns {boolean} false when the payload could not be encoded
 */
export function drawQrToCanvas(canvas, text, options) {
  const {
    size,
    foreground = DEFAULT_QR_FOREGROUND,
    background = DEFAULT_QR_BACKGROUND,
    pixelRatio = window.devicePixelRatio || 1,
  } = options;

  if (!canvas || !text) return false;

  let qr;
  try {
    // Type 0 lets the library pick the smallest version that fits the payload,
    // so a short link and a long destination both render at sensible density.
    qr = qrcodeGenerator(0, ERROR_CORRECTION);
    qr.addData(text);
    qr.make();
  } catch {
    // Thrown when the payload exceeds the largest QR version. A URL long enough
    // to do that is pathological, but the caller still gets a clean "no preview"
    // rather than a crashed render.
    return false;
  }

  const moduleCount = qr.getModuleCount();
  const totalModules = moduleCount + QUIET_ZONE * 2;

  // Round the module size down to a whole device pixel. A fractional module size
  // leaves seams between cells that some scanners read as noise.
  const scale = Math.max(1, Math.floor((size * pixelRatio) / totalModules));
  const canvasPx = scale * totalModules;

  canvas.width = canvasPx;
  canvas.height = canvasPx;
  canvas.style.width = `${size}px`;
  canvas.style.height = `${size}px`;

  const context = canvas.getContext('2d');
  if (!context) return false;

  context.fillStyle = background;
  context.fillRect(0, 0, canvasPx, canvasPx);

  context.fillStyle = foreground;
  for (let row = 0; row < moduleCount; row += 1) {
    for (let column = 0; column < moduleCount; column += 1) {
      if (!qr.isDark(row, column)) continue;
      context.fillRect(
        (column + QUIET_ZONE) * scale,
        (row + QUIET_ZONE) * scale,
        scale,
        scale,
      );
    }
  }

  return true;
}

/**
 * Renders a QR code and hands the browser a PNG download.
 *
 * Rendered into a detached canvas at {@link QR_DOWNLOAD_PX} rather than exporting
 * the on-screen one: the preview is 160–288px, which is too coarse to print.
 *
 * @returns {Promise<boolean>} false when the payload could not be encoded
 */
export function downloadQrPng(text, { filename, foreground, background }) {
  const canvas = document.createElement('canvas');

  // pixelRatio 1: the size is already given in the final pixel dimensions we want.
  const ok = drawQrToCanvas(canvas, text, {
    size: QR_DOWNLOAD_PX,
    foreground,
    background,
    pixelRatio: 1,
  });
  if (!ok) return Promise.resolve(false);

  return new Promise((resolve) => {
    canvas.toBlob((blob) => {
      if (!blob) {
        resolve(false);
        return;
      }

      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = filename;
      document.body.appendChild(anchor);
      anchor.click();
      document.body.removeChild(anchor);

      // Revoking immediately is safe: the download has already been handed to the
      // browser by the time click() returns. Without this the PNG stays in memory
      // for the life of the page.
      URL.revokeObjectURL(url);
      resolve(true);
    }, 'image/png');
  });
}

/**
 * Whether a foreground/background pair has enough contrast for a scanner.
 *
 * Scanners threshold the image to black and white, so a low-contrast pair produces
 * a code that looks fine on screen and fails every scan. The 3:1 floor is
 * deliberately below WCAG's text minimum — this is machine legibility, not reading —
 * but well above the point where thresholding becomes unreliable.
 */
export function hasScannableContrast(foreground, background) {
  const a = relativeLuminance(foreground);
  const b = relativeLuminance(background);
  if (a === null || b === null) return true;

  const ratio = (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
  return ratio >= 3;
}

/** WCAG relative luminance for a #rrggbb string; null if it cannot be parsed. */
function relativeLuminance(hex) {
  const match = /^#?([0-9a-f]{6})$/i.exec(hex ?? '');
  if (!match) return null;

  const value = parseInt(match[1], 16);
  const channels = [(value >> 16) & 255, (value >> 8) & 255, value & 255];

  const [r, g, b] = channels.map((channel) => {
    const c = channel / 255;
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
  });

  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}
