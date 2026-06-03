import { useEffect, useState } from 'react';
import { klikAuthorizationService } from '../services/klikAuthorizationService';
import type { PendingKlikAuthorization } from '../services/klikAuthorizationService';

interface Props {
  authorization: PendingKlikAuthorization;
  onResolved: () => void;
}

export default function KlikAuthorizationModal({ authorization, onResolved }: Props) {
  const [pin, setPin] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [secondsLeft, setSecondsLeft] = useState(0);

  useEffect(() => {
    const expires = new Date(authorization.expiryTime + 'Z').getTime();
    const tick = () => {
      const left = Math.max(0, Math.floor((expires - Date.now()) / 1000));
      setSecondsLeft(left);
      if (left === 0) onResolved();
    };
    tick();
    const interval = setInterval(tick, 500);
    return () => clearInterval(interval);
  }, [authorization.expiryTime, onResolved]);

  const handleConfirm = async () => {
    setError('');
    setLoading(true);
    try {
      await klikAuthorizationService.confirm(authorization.id, pin);
      onResolved();
    } catch (err: any) {
      const detail = err.response?.data?.detail || 'Nie udało się zatwierdzić płatności.';
      setError(detail);
      setPin('');
    } finally {
      setLoading(false);
    }
  };

  const handleReject = async () => {
    setLoading(true);
    try {
      await klikAuthorizationService.reject(authorization.id);
    } catch {
      // ignore - i tak zamykamy modal
    } finally {
      onResolved();
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[100] bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-6 w-full max-w-sm shadow-2xl">
        <div className="flex items-center justify-between mb-4">
          <span className="text-blue-400 text-xs uppercase tracking-wider font-semibold">Płatność BLIK</span>
          <span className="text-zinc-500 text-xs font-mono">{secondsLeft}s</span>
        </div>

        <div className="text-center py-4">
          <div className="text-3xl font-bold text-white mb-1">
            {authorization.amount.toLocaleString('pl-PL', { minimumFractionDigits: 2 })} {authorization.currency}
          </div>
          <div className="text-zinc-400 text-sm">{authorization.merchantName}</div>
        </div>

        <div className="bg-zinc-950/50 border border-zinc-800 rounded-lg px-3 py-2 mb-5">
          <div className="text-zinc-600 text-xs uppercase tracking-wider">Z konta</div>
          <div className="text-zinc-300 text-xs font-mono mt-0.5">
            ••••{authorization.accountNumber.slice(-4)}
          </div>
        </div>

        <div className="mb-4">
          <label className="block text-zinc-400 text-sm mb-2">Wpisz PIN aby zatwierdzić</label>
          <input
            type="password"
            inputMode="numeric"
            pattern="\d{4}"
            maxLength={4}
            value={pin}
            onChange={e => setPin(e.target.value.replace(/\D/g, ''))}
            placeholder="••••"
            autoFocus
            className="w-full bg-zinc-800 border border-zinc-700 text-white text-center text-2xl tracking-widest font-mono rounded-lg px-3 py-3 focus:outline-none focus:border-blue-500"
          />
        </div>

        {error && (
          <div className="bg-red-500/10 border border-red-500/20 text-red-400 text-xs rounded-lg px-3 py-2 mb-3">
            {error}
          </div>
        )}

        <div className="flex gap-2">
          <button
            onClick={handleReject}
            disabled={loading}
            className="flex-1 bg-zinc-800 hover:bg-zinc-700 text-zinc-300 py-2.5 rounded-lg font-medium transition-colors disabled:opacity-50"
          >
            Odrzuć
          </button>
          <button
            onClick={handleConfirm}
            disabled={loading || pin.length !== 4}
            className="flex-1 bg-blue-600 hover:bg-blue-500 text-white py-2.5 rounded-lg font-medium transition-colors disabled:opacity-50 flex items-center justify-center"
          >
            {loading
              ? <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              : 'Zatwierdź'}
          </button>
        </div>
      </div>
    </div>
  );
}