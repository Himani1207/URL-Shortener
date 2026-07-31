import { useCallback, useEffect, useState } from 'react';
import { linksApi } from '../lib/api';
import { useToast } from '../context/ToastContext';

/**
 * Loads and mutates the caller's links.
 *
 * The Links, Analytics and QR codes pages all need the same list plus the same
 * pause/delete behaviour. Putting it in a hook keeps that logic in one place —
 * without it, "toggle then refresh the row" would be re-implemented three times
 * and drift.
 *
 * Mutations update local state from the server's response rather than refetching
 * the whole list. One request instead of two, and the table does not flash through
 * a loading state for a change that affects a single row.
 */
export function useLinks() {
  const toast = useToast();

  const [links, setLinks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setLinks(await linksApi.list());
    } catch (err) {
      setError(err.message ?? 'Could not load your links');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  /** Prepends a newly created link so it appears without a round trip. */
  const addLink = useCallback((created) => {
    setLinks((current) => [created, ...current]);
  }, []);

  /** Replaces one row in place, preserving list order. */
  const replaceLink = useCallback((updated) => {
    setLinks((current) => current.map((link) => (link.id === updated.id ? updated : link)));
  }, []);

  const removeLink = useCallback((deleted) => {
    setLinks((current) => current.filter((link) => link.id !== deleted.id));
  }, []);

  const toggleLink = useCallback(
    async (link) => {
      try {
        const updated = await linksApi.toggle(link.id);
        replaceLink(updated);
        toast.success(updated.active ? 'Link resumed' : 'Link paused');
      } catch (err) {
        toast.error(err.message ?? 'Could not update the link');
      }
    },
    [replaceLink, toast],
  );

  return { links, loading, error, reload: load, addLink, replaceLink, removeLink, toggleLink };
}

/** Account-level totals for the dashboard tiles. */
export function useDashboardStats() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setStats(await linksApi.stats());
    } catch {
      // Non-fatal: the tiles are supporting information, and failing them should
      // not stop the links table from rendering. The UI falls back to dashes.
      setStats(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  return { stats, loading, reload: load };
}
