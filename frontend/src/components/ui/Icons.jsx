/**
 * Inline SVG icon set.
 *
 * Hand-rolled rather than pulled from an icon package: the app needs about fifteen
 * icons, and a library would add a dependency plus a tree-shaking concern for
 * roughly 4KB of paths. All are 20x20 on a 1.6 stroke so they sit consistently
 * next to 14px text.
 *
 * Every icon is aria-hidden — they always accompany a text label or an
 * aria-label on the control, so announcing them would just duplicate it.
 */

const base = {
  width: 20,
  height: 20,
  viewBox: '0 0 20 20',
  fill: 'none',
  'aria-hidden': true,
  strokeWidth: 1.6,
  stroke: 'currentColor',
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
};

export const IconDashboard = (p) => (
  <svg {...base} {...p}>
    <rect x="2.5" y="2.5" width="6" height="6" rx="1.5" />
    <rect x="11.5" y="2.5" width="6" height="6" rx="1.5" />
    <rect x="2.5" y="11.5" width="6" height="6" rx="1.5" />
    <rect x="11.5" y="11.5" width="6" height="6" rx="1.5" />
  </svg>
);

export const IconLink = (p) => (
  <svg {...base} {...p}>
    <path d="M8.5 11.5a3.5 3.5 0 0 0 5 0l2-2a3.5 3.5 0 0 0-5-5l-1 1" />
    <path d="M11.5 8.5a3.5 3.5 0 0 0-5 0l-2 2a3.5 3.5 0 0 0 5 5l1-1" />
  </svg>
);

export const IconChart = (p) => (
  <svg {...base} {...p}>
    <path d="M3 17h14" />
    <path d="M6 17V9.5" />
    <path d="M10 17V4.5" />
    <path d="M14 17v-5" />
  </svg>
);

export const IconQr = (p) => (
  <svg {...base} {...p}>
    <rect x="2.5" y="2.5" width="5.5" height="5.5" rx="1" />
    <rect x="12" y="2.5" width="5.5" height="5.5" rx="1" />
    <rect x="2.5" y="12" width="5.5" height="5.5" rx="1" />
    <path d="M12 12h2.5v2.5H12zM17.5 12v2M15 17.5h2.5V15" />
  </svg>
);

export const IconSettings = (p) => (
  <svg {...base} {...p}>
    <circle cx="10" cy="10" r="2.5" />
    <path d="M10 2.5v1.8M10 15.7v1.8M17.5 10h-1.8M4.3 10H2.5M15.3 4.7l-1.3 1.3M6 14l-1.3 1.3M15.3 15.3 14 14M6 6 4.7 4.7" />
  </svg>
);

export const IconCopy = (p) => (
  <svg {...base} {...p}>
    <rect x="7" y="7" width="10.5" height="10.5" rx="2" />
    <path d="M13 4.5a2 2 0 0 0-2-2H4.5a2 2 0 0 0-2 2V11a2 2 0 0 0 2 2" />
  </svg>
);

export const IconCheck = (p) => (
  <svg {...base} {...p}>
    <path d="m4 10.5 4 4 8-9" strokeWidth="2" />
  </svg>
);

export const IconTrash = (p) => (
  <svg {...base} {...p}>
    <path d="M3.5 5.5h13M8 5.5V4a1.5 1.5 0 0 1 1.5-1.5h1A1.5 1.5 0 0 1 12 4v1.5" />
    <path d="M5.5 5.5 6.2 16a1.5 1.5 0 0 0 1.5 1.4h4.6a1.5 1.5 0 0 0 1.5-1.4l.7-10.5" />
  </svg>
);

export const IconEdit = (p) => (
  <svg {...base} {...p}>
    <path d="M13.2 3.3a1.9 1.9 0 0 1 2.7 2.7l-8.4 8.4-3.6.9.9-3.6z" />
  </svg>
);

export const IconExternal = (p) => (
  <svg {...base} {...p}>
    <path d="M11 3h6v6" />
    <path d="M17 3l-8 8" />
    <path d="M15.5 12v3.5a2 2 0 0 1-2 2h-9a2 2 0 0 1-2-2v-9a2 2 0 0 1 2-2H8" />
  </svg>
);

export const IconPlus = (p) => (
  <svg {...base} {...p}>
    <path d="M10 4v12M4 10h12" strokeWidth="1.8" />
  </svg>
);

export const IconPause = (p) => (
  <svg {...base} {...p}>
    <rect x="6" y="4" width="2.5" height="12" rx="1" />
    <rect x="11.5" y="4" width="2.5" height="12" rx="1" />
  </svg>
);

export const IconPlay = (p) => (
  <svg {...base} {...p}>
    <path d="M6.5 4.5 15 10l-8.5 5.5z" />
  </svg>
);

export const IconDownload = (p) => (
  <svg {...base} {...p}>
    <path d="M10 3v9M6.5 8.5 10 12l3.5-3.5" />
    <path d="M3.5 14v1.5a2 2 0 0 0 2 2h9a2 2 0 0 0 2-2V14" />
  </svg>
);

export const IconArrowLeft = (p) => (
  <svg {...base} {...p}>
    <path d="M16 10H4M8.5 5.5 4 10l4.5 4.5" />
  </svg>
);

export const IconLogout = (p) => (
  <svg {...base} {...p}>
    <path d="M12.5 14v1.5a2 2 0 0 1-2 2h-5a2 2 0 0 1-2-2v-11a2 2 0 0 1 2-2h5a2 2 0 0 1 2 2V6" />
    <path d="M17 10H8M13.5 6.5 17 10l-3.5 3.5" />
  </svg>
);

export const IconMenu = (p) => (
  <svg {...base} {...p}>
    <path d="M3 5.5h14M3 10h14M3 14.5h14" strokeWidth="1.8" />
  </svg>
);

export const IconGlobe = (p) => (
  <svg {...base} {...p}>
    <circle cx="10" cy="10" r="7.5" />
    <path d="M2.5 10h15M10 2.5c2 2.3 3 4.9 3 7.5s-1 5.2-3 7.5c-2-2.3-3-4.9-3-7.5s1-5.2 3-7.5z" />
  </svg>
);

export const IconShield = (p) => (
  <svg {...base} {...p}>
    <path d="M10 2.5 4 5v4.6c0 3.5 2.4 6.7 6 7.9 3.6-1.2 6-4.4 6-7.9V5z" />
    <path d="m7.5 10 1.8 1.8L13 7.8" />
  </svg>
);

export const IconBolt = (p) => (
  <svg {...base} {...p}>
    <path d="M11 2.5 4.5 11H9.5l-.5 6.5L15.5 9H10.5z" />
  </svg>
);

export const IconClock = (p) => (
  <svg {...base} {...p}>
    <circle cx="10" cy="10" r="7.5" />
    <path d="M10 5.5V10l3 1.8" />
  </svg>
);

export const IconLock = (p) => (
  <svg {...base} {...p}>
    <rect x="4" y="8.5" width="12" height="9" rx="2" />
    <path d="M7 8.5V6a3 3 0 0 1 6 0v2.5" />
  </svg>
);

export const IconEye = (p) => (
  <svg {...base} {...p}>
    <path d="M1.8 10S4.9 4.8 10 4.8 18.2 10 18.2 10 15.1 15.2 10 15.2 1.8 10 1.8 10z" />
    <circle cx="10" cy="10" r="2.3" />
  </svg>
);

/** The struck-through eye. Paired with IconEye to toggle password visibility. */
export const IconEyeOff = (p) => (
  <svg {...base} {...p}>
    <path d="M7.9 5.2A7.6 7.6 0 0 1 10 4.8c5.1 0 8.2 5.2 8.2 5.2a15 15 0 0 1-2.5 3" />
    <path d="M4.6 6.5A14.8 14.8 0 0 0 1.8 10S4.9 15.2 10 15.2a7.7 7.7 0 0 0 3-.6" />
    <path d="M8.4 8.4a2.3 2.3 0 0 0 3.2 3.2" />
    <path d="m3.5 3.5 13 13" />
  </svg>
);

export const IconClose = (p) => (
  <svg {...base} {...p}>
    <path d="m5.5 5.5 9 9M14.5 5.5l-9 9" />
  </svg>
);

export const IconArrowRight = (p) => (
  <svg {...base} {...p}>
    <path d="M4 10h12M11.5 5.5 16 10l-4.5 4.5" />
  </svg>
);

