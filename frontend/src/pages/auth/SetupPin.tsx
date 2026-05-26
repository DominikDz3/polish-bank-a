import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertCircle, ShieldCheck } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';

const SetupPin: React.FC = () => {
  const navigate = useNavigate();
  const { setPin, user } = useAuth();
  const [pin, setPinValue] = useState('');
  const [confirmPin, setConfirmPin] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const isValid = /^\d{4}$/.test(pin) && pin === confirmPin;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (!/^\d{4}$/.test(pin)) {
      setError('PIN musi składać się z dokładnie 4 cyfr.');
      return;
    }
    if (pin !== confirmPin) {
      setError('Kody PIN nie są identyczne.');
      return;
    }
    setIsLoading(true);
    try {
      await setPin(pin, confirmPin);
      navigate('/dashboard');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Nie udało się ustawić PIN-u. Spróbuj ponownie.');
    } finally {
      setIsLoading(false);
    }
  };

  const onlyDigits = (value: string, setter: (v: string) => void) => {
    const cleaned = value.replace(/\D/g, '').slice(0, 4);
    setter(cleaned);
    setError('');
  };

  return (
    <div className="min-h-screen bg-zinc-950 flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-8">
          <div className="w-12 h-12 bg-blue-500/10 border border-blue-500/20 rounded-full flex items-center justify-center mx-auto mb-4">
            <ShieldCheck className="text-blue-400" size={22} />
          </div>
          <h1 className="text-xl font-semibold text-white text-center mb-2">Ustaw kod PIN</h1>
          <p className="text-zinc-500 text-sm text-center mb-6">
            {user?.customerNumber && (
              <>Numer klienta: <span className="text-zinc-300 font-mono">{user.customerNumber}</span><br /></>
            )}
            Kod PIN będzie potrzebny do potwierdzania wszystkich operacji finansowych.
          </p>

          {error && (
            <div className="flex items-center gap-2 bg-red-500/10 border border-red-500/20 text-red-400 text-sm rounded-lg px-4 py-3 mb-5">
              <AlertCircle size={16} className="shrink-0" />
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-1.5">Nowy PIN (4 cyfry)</label>
              <input
                type="password"
                inputMode="numeric"
                autoComplete="new-password"
                value={pin}
                onChange={(e) => onlyDigits(e.target.value, setPinValue)}
                placeholder="••••"
                maxLength={4}
                className="w-full bg-zinc-800 border border-zinc-700 text-white text-center tracking-[0.6em] text-2xl rounded-lg px-4 py-3 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-colors"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-1.5">Potwierdź PIN</label>
              <input
                type="password"
                inputMode="numeric"
                autoComplete="new-password"
                value={confirmPin}
                onChange={(e) => onlyDigits(e.target.value, setConfirmPin)}
                placeholder="••••"
                maxLength={4}
                className={`w-full bg-zinc-800 border text-white text-center tracking-[0.6em] text-2xl rounded-lg px-4 py-3 focus:outline-none focus:ring-1 transition-colors ${
                  confirmPin && confirmPin !== pin
                    ? 'border-red-500/50 focus:border-red-500 focus:ring-red-500'
                    : 'border-zinc-700 focus:border-blue-500 focus:ring-blue-500'
                }`}
              />
              {confirmPin && confirmPin !== pin && (
                <p className="text-xs text-red-400 mt-1">Kody PIN nie są identyczne</p>
              )}
            </div>

            <button
              type="submit"
              disabled={!isValid || isLoading}
              className="w-full bg-blue-600 hover:bg-blue-500 disabled:bg-blue-600/50 disabled:cursor-not-allowed text-white font-medium py-2.5 rounded-lg text-sm transition-colors flex items-center justify-center gap-2"
            >
              {isLoading ? (
                <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                'Zapisz PIN'
              )}
            </button>
          </form>
        </div>

        <p className="text-center text-xs text-zinc-600 mt-6">
          Twojego PIN-u nikomu nie podawaj. Jest hashowany i przechowywany w bezpieczny sposób.
        </p>
      </div>
    </div>
  );
};

export default SetupPin;