import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';

/**
 * Resets scroll position on navigation.
 *
 * A single-page app does not reload the document, so the browser keeps the previous
 * scroll offset. Without this, following a link from halfway down the links table
 * opens the analytics page already scrolled into its middle — which reads as a
 * rendering bug rather than as retained state.
 *
 * <b>This jump is deliberately instant</b>, even though the app has smooth scrolling
 * enabled globally. Animating a route change means watching the old page scroll
 * past before the new one appears, which feels slow rather than polished. Smooth
 * scrolling is for in-page anchors, where the motion tells you where you went.
 *
 * Hash links are exempt so the landing page's #features and #pricing anchors still
 * scroll to their target — smoothly, via `scroll-behavior` in the stylesheet.
 */
export default function ScrollToTop() {
  const { pathname, hash } = useLocation();

  useEffect(() => {
    if (hash) return;

    // `behavior: 'instant'` is only supported in newer engines, and the CSS
    // `scroll-behavior: smooth` on <html> would otherwise animate this jump.
    // Suspending it around a plain scrollTo works identically everywhere.
    const root = document.documentElement;
    const previous = root.style.scrollBehavior;

    root.style.scrollBehavior = 'auto';
    window.scrollTo(0, 0);
    root.style.scrollBehavior = previous;
  }, [pathname, hash]);

  return null;
}
