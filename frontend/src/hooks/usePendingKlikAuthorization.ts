import { useEffect, useState, useCallback } from 'react';
import { klikAuthorizationService } from '../services/klikAuthorizationService';
import type { PendingKlikAuthorization } from '../services/klikAuthorizationService';

export function usePendingKlikAuthorization(intervalMs = 2000, enabled = true) {
  const [pending, setPending] = useState<PendingKlikAuthorization | null>(null);

  const refresh = useCallback(async () => {
    try {
      const list = await klikAuthorizationService.getPending();
      setPending(list.length > 0 ? list[0] : null);
    } catch {
    }
  }, []);

  useEffect(() => {
    if (!enabled) return;

    let cancelled = false;

    const poll = async () => {
      try {
        const list = await klikAuthorizationService.getPending();
        if (!cancelled) {
          setPending(list.length > 0 ? list[0] : null);
        }
      } catch {
      }
    };

    poll();
    const interval = setInterval(poll, intervalMs);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [intervalMs, enabled]);

  return { pending, refresh };
}