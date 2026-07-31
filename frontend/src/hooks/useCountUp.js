import { useEffect, useRef, useState } from 'react';

/**
 * Animates a number up from zero when it first appears.
 *
 * Purely decorative, and used only on the dashboard stat tiles. It gives the
 * dashboard a moment of life on load without adding a single pixel of chrome —
 * the restrained version of "make it fun".
 *
 * Three things keep it from being annoying:
 *   - It respects `prefers-reduced-motion` and snaps straight to the value.
 *   - It only animates the first time a value arrives. Refreshing the stats after
 *     creating a link re-counts from zero otherwise, which reads as the number
 *     having reset.
 *   - It eases out, so it decelerates into the final figure instead of stopping
 *     dead.
 *
 * Driven by requestAnimationFrame rather than setInterval so it stays in step with
 * the display's refresh rate and pauses when the tab is backgrounded.
 */
export function useCountUp(target, duration = 900) {
  const [display, setDisplay] = useState(0);
  const hasAnimated = useRef(false);
  const frameRef = useRef(null);

  useEffect(() => {
    const value = Number(target);

    if (!Number.isFinite(value)) {
      setDisplay(0);
      return undefined;
    }

    const prefersReducedMotion = window.matchMedia?.(
      '(prefers-reduced-motion: reduce)',
    )?.matches;

    // Subsequent updates jump straight to the new figure — see above.
    if (hasAnimated.current || prefersReducedMotion || value === 0) {
      setDisplay(value);
      hasAnimated.current = true;
      return undefined;
    }

    hasAnimated.current = true;
    const startedAt = performance.now();

    const step = (now) => {
      const progress = Math.min((now - startedAt) / duration, 1);
      // easeOutExpo: fast at first, then settles.
      const eased = progress === 1 ? 1 : 1 - Math.pow(2, -10 * progress);

      setDisplay(Math.round(eased * value));

      if (progress < 1) {
        frameRef.current = requestAnimationFrame(step);
      }
    };

    frameRef.current = requestAnimationFrame(step);

    return () => {
      if (frameRef.current) cancelAnimationFrame(frameRef.current);
    };
  }, [target, duration]);

  return display;
}
