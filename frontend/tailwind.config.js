/** @type {import('tailwindcss').Config} */

/**
 * Design tokens for the whole UI.
 *
 * Encoding the palette here rather than scattering hex values through components
 * means a visual direction is one file to change: a component reaches for
 * `bg-brand-600` and gets the one accent this product uses.
 *
 * <b>The direction.</b> Warm paper ground, near-black ink, one desaturated blue
 * accent. Separation comes from hairline rules and spacing — the shadow tokens are
 * deliberately empty, so a component asking for `shadow-card` gets nothing and has
 * to earn its edges with a border.
 *
 * The previous palette was Tailwind's stock blue on slate greys, which is where
 * every project starts and therefore what every project looks like. The neutrals
 * here are warm rather than blue-cast, which is what does most of the work — more
 * than the accent hue does.
 */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        /** The page ground. Distinct from `white`, which stays for raised surfaces. */
        paper: '#f4f2ec',

        // Single accent ramp, a desaturated ink blue. 600 is the primary action
        // colour; the lighter steps are for rules and hover states. It is used once
        // or twice per screen, never as a background wash.
        brand: {
          50: '#eff2f5',
          100: '#dde4ea',
          200: '#bfcdd8',
          300: '#95aabc',
          400: '#66829b',
          500: '#3f6180',
          600: '#26485f',
          700: '#1f3b4e',
          800: '#1a303e',
          900: '#152633',
        },

        // Warm neutral ramp for text, rules and surfaces. Replaces the slate ramp,
        // whose blue cast is the single biggest reason stock Tailwind UIs read as
        // interchangeable.
        ink: {
          50: '#faf9f6',
          100: '#efece4',
          200: '#dcd8ce',
          300: '#c3beb1',
          400: '#9a968d',
          500: '#6b6862',
          600: '#55524c',
          700: '#3f3d38',
          800: '#2b2926',
          900: '#1c1b17',
        },
      },

      fontFamily: {
        // Plain system stack. No webfont: nothing to download, nothing to shift
        // while it loads, and it looks native on every platform. Inter used to head
        // this list and has been removed — it is the default "product" face, so
        // specifying it is a way of looking like everything else.
        sans: [
          '-apple-system',
          'BlinkMacSystemFont',
          'Segoe UI',
          'Roboto',
          'Helvetica Neue',
          'Arial',
          'sans-serif',
        ],
        mono: ['ui-monospace', 'SFMono-Regular', 'Menlo', 'Consolas', 'monospace'],
      },

      borderRadius: {
        // Tight corners throughout. Soft 12–16px radii are the other half of the
        // stock look; at 4px the UI reads as drawn rather than rounded off.
        DEFAULT: '0.1875rem',
        md: '0.25rem',
        lg: '0.25rem',
        xl: '0.3125rem',
        '2xl': '0.375rem',
      },

      boxShadow: {
        // Empty on purpose. Cards separate with a 1px rule and whitespace; these
        // stay defined so the components asking for them keep working, and so the
        // decision lives here rather than in a sweep of every call site.
        card: 'none',
        'card-hover': 'none',

        // The one real elevation, for things that genuinely float above the page —
        // modals, toasts, menus. Warm-tinted to sit on the paper ground.
        dropdown: '0 10px 30px -8px rgb(28 27 23 / 0.18)',
      },

      keyframes: {
        'fade-in': {
          from: { opacity: '0' },
          to: { opacity: '1' },
        },
        'slide-up': {
          from: { opacity: '0', transform: 'translateY(8px)' },
          to: { opacity: '1', transform: 'translateY(0)' },
        },
        'slide-in-right': {
          from: { opacity: '0', transform: 'translateX(16px)' },
          to: { opacity: '1', transform: 'translateX(0)' },
        },
        'slide-down': {
          from: { opacity: '0', transform: 'translateY(-8px)' },
          to: { opacity: '1', transform: 'translateY(0)' },
        },
      },
      animation: {
        // Short and eased. Anything longer than ~200ms starts to feel sluggish
        // on repeated interactions like opening a modal.
        'fade-in': 'fade-in 150ms ease-out',
        'slide-up': 'slide-up 200ms cubic-bezier(0.16, 1, 0.3, 1)',
        'slide-in-right': 'slide-in-right 200ms cubic-bezier(0.16, 1, 0.3, 1)',
        'slide-down': 'slide-down 180ms cubic-bezier(0.16, 1, 0.3, 1)',
      },
    },
  },
  plugins: [],
};
