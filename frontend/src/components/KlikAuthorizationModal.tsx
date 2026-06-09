import { useEffect, useState } from 'react';
import { klikAuthorizationService } from '../services/klikAuthorizationService';
import type { PendingKlikAuthorization } from '../services/klikAuthorizationService';

interface Props {
  authorization: PendingKlikAuthorization;
  onResolved: () => void;
}

type ResultState =
  | { type: 'pending' }
  | { type: 'success'; title: string; subtitle: string }
  | { type: 'failure'; title: string; subtitle: string };

export default function KlikAuthorizationModal({ authorization, onResolved }: Props) {
  const [pin, setPin] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [secondsLeft, setSecondsLeft] = useState(0);
  const [result, setResult] = useState<ResultState>({ type: 'pending' });

  useEffect(() => {
    if (result.type !== 'pending') return;
    const expires = new Date(authorization.expiryTime + 'Z').getTime();
    const tick = () => {
      const left = Math.max(0, Math.floor((expires - Date.now()) / 1000));
      setSecondsLeft(left);
      if (left === 0) {
        setResult({
          type: 'failure',
          title: 'Czas minął',
          subtitle: 'Autoryzacja wygasła.',
        });
      }
    };
    tick();
    const interval = setInterval(tick, 500);
    return () => clearInterval(interval);
  }, [authorization.expiryTime, result.type]);

  // Auto-zamknięcie po sukcesie/błędzie
  useEffect(() => {
    if (result.type === 'pending') return;
    const timer = setTimeout(() => onResolved(), 2500);
    return () => clearTimeout(timer);
  }, [result, onResolved]);

  const formatAmount = () =>
    `${authorization.amount.toLocaleString('pl-PL', { minimumFractionDigits: 2 })} ${authorization.currency}`;

  const handleConfirm = async () => {
    setError('');
    setLoading(true);
    try {
      await klikAuthorizationService.confirm(authorization.id, pin);
      setResult({
        type: 'success',
        title: 'Płatność zrealizowana',
        subtitle: `${formatAmount()} • ${authorization.merchantName}`,
      });
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
      setResult({
        type: 'failure',
        title: 'Płatność odrzucona',
        subtitle: `${formatAmount()} • ${authorization.merchantName}`,
      });
    } catch {
      setResult({
        type: 'failure',
        title: 'Płatność odrzucona',
        subtitle: `${formatAmount()} • ${authorization.merchantName}`,
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[100] bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-6 w-full max-w-sm shadow-2xl">
        {result.type === 'success' && (
          <div className="text-center py-6">
            <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-green-500/15 border border-green-500/30 flex items-center justify-center">
              <svg className="w-8 h-8 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
              </svg>
            </div>
            <h2 className="text-lg font-semibold text-white mb-1">{result.title}</h2>
            <p className="text-zinc-400 text-sm">{result.subtitle}</p>
          </div>
        )}

        {result.type === 'failure' && (
          <div className="text-center py-6">
            <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-red-500/15 border border-red-500/30 flex items-center justify-center">
              <svg className="w-8 h-8 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </div>
            <h2 className="text-lg font-semibold text-white mb-1">{result.title}</h2>
            <p className="text-zinc-400 text-sm">{result.subtitle}</p>
          </div>
        )}

        {result.type === 'pending' && (
          <>
            <div className="flex items-center justify-between mb-4">
              <span className="text-blue-400 text-xs uppercase tracking-wider font-semibold">Płatność BLIK</span>
              <span className="text-zinc-500 text-xs font-mono">{secondsLeft}s</span>
            </div>

            <div className="text-center py-4">
              <div className="text-3xl font-bold text-white mb-1">{formatAmount()}</div>
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
          </>
        )}
      </div>
    </div>
  );
}