import { useEffect, useState } from 'react';
import { api } from '../../services/api';
import { useNavigate } from 'react-router-dom';

interface AccountSummary {
  id: string;
  accountNumber: string;
  balance: number;
  currency: string;
  type: string;
}

export default function InternalTransfer() {
  const navigate = useNavigate();

  const [accounts, setAccounts] = useState<AccountSummary[]>([]);
  const [senderAccountId, setSenderAccountId] = useState('');
  const [receiverAccountNumber, setReceiverAccountNumber] = useState('');
  const [amount, setAmount] = useState('');
  const [title, setTitle] = useState('');

  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const [showPinModal, setShowPinModal] = useState(false);
  const [pin, setPin] = useState('');
  const [pinError, setPinError] = useState<string | null>(null);

  const [lockedUntil, setLockedUntil] = useState<Date | null>(null);
  const [nowTs, setNowTs] = useState<number>(() => Date.now());

  const loadAccounts = async () => {
    try {
      const response = await api.get('/api/accounts');
      setAccounts(response.data);
      if (response.data.length > 0 && !senderAccountId) {
        setSenderAccountId(response.data[0].id);
      }
    } catch {
      setError("Nie udało się pobrać Twoich kont. Spróbuj odświeżyć stronę.");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadAccounts();
  }, []);

  useEffect(() => {
  if (!lockedUntil) return;
  setNowTs(Date.now());
  const id = setInterval(() => setNowTs(Date.now()), 1000);
  return () => clearInterval(id);
}, [lockedUntil]);

useEffect(() => {
  if (lockedUntil && nowTs >= lockedUntil.getTime()) {
    setLockedUntil(null);
    setPinError(null);
  }
}, [nowTs, lockedUntil]);

  const validateForm = () => {
    const errors: Record<string, string> = {};
    const cleanReceiverNumber = receiverAccountNumber.replace(/\s+/g, '');

    if (!cleanReceiverNumber) {
      errors.receiverAccountNumber = "Podaj numer konta odbiorcy.";
    } else if (cleanReceiverNumber.length < 26) {
      errors.receiverAccountNumber = "Numer konta jest za krótki. Powinien zawierać kod kraju (np. PL) i 26 cyfr.";
    }

    if (!amount || parseFloat(amount) <= 0) {
      errors.amount = "Podaj poprawną kwotę przelewu (większą od 0).";
    }

    if (!title.trim()) {
      errors.title = "Tytuł przelewu nie może być pusty.";
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(false);
    if (!validateForm()) return;
    setPin('');
    setPinError(null);
    setShowPinModal(true);
  };

  const confirmWithPin = async () => {
  if (lockedUntil) return;
  if (!/^\d{4}$/.test(pin)) {
    setPinError("PIN musi składać się z dokładnie 4 cyfr.");
    return;
  }
  setPinError(null);
  setIsSubmitting(true);
  const cleanReceiverNumber = receiverAccountNumber.replace(/\s+/g, '');

  try {
    await api.post('/api/transactions/internal', {
      senderAccountId,
      receiverAccountNumber: cleanReceiverNumber,
      amount: parseFloat(amount),
      title,
      pin,
    });

    setSuccess(true);
    setReceiverAccountNumber('');
    setAmount('');
    setTitle('');
    setFieldErrors({});
    setShowPinModal(false);
    setPin('');
  } catch (err: any) {
    const status = err.response?.status;
    const data = err.response?.data;
    if (status === 423 && data?.lockedUntil) {
      setLockedUntil(new Date(data.lockedUntil));
      setPin('');
      setPinError(null);
    } else {
      const msg = data?.detail
        || data?.message
        || data?.errors?.[0]?.defaultMessage
        || "Wystąpił błąd podczas realizacji przelewu.";
      setPinError(msg);
    }
  } finally {
    setIsSubmitting(false);
    await loadAccounts();
  }
};

  const formatCurrency = (value: number, currency: string) => {
    return new Intl.NumberFormat('pl-PL', { style: 'currency', currency }).format(value);
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-zinc-950 flex justify-center items-center">
        <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  const formatCountdown = (ms: number) => {
  const totalSeconds = Math.max(0, Math.ceil(ms / 1000));
  const m = Math.floor(totalSeconds / 60);
  const s = totalSeconds % 60;
  return `${m}:${s.toString().padStart(2, '0')}`;
};

  return (
    <div className="min-h-screen bg-zinc-950 text-white font-sans flex flex-col">
      <nav className="bg-zinc-900/80 backdrop-blur-md border-b border-zinc-800 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="text-xl font-bold tracking-tight text-white">
            <span className="text-blue-400">Bankly</span>
          </div>
          <button onClick={() => navigate(-1)} className="text-zinc-400 hover:text-white flex items-center gap-2 transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
            Wróć
          </button>
        </div>
      </nav>

      <main className="flex-grow p-4 md:p-8 flex justify-center items-start pt-12">
        <div className="w-full max-w-xl bg-zinc-900 border border-zinc-800 rounded-2xl p-6 md:p-8 shadow-xl">

          <div className="mb-8 text-center">
            <div className="w-12 h-12 bg-blue-500/10 text-blue-400 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" /></svg>
            </div>
            <h2 className="text-2xl font-bold text-white">Nowy przelew wewnętrzny</h2>
            <p className="text-zinc-500 text-sm mt-1">Przelej środki na inne konto w naszym banku.</p>
          </div>

          {success && (
            <div className="bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 p-4 rounded-xl mb-6 flex items-center gap-3">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 flex-shrink-0" viewBox="0 0 20 20" fill="currentColor"><path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" /></svg>
              <span>Przelew został zrealizowany pomyślnie.</span>
            </div>
          )}

          {error && (
            <div className="bg-red-500/10 border border-red-500/20 text-red-400 p-4 rounded-xl mb-6 flex items-center gap-3">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 flex-shrink-0" viewBox="0 0 20 20" fill="currentColor"><path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" /></svg>
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} noValidate className="space-y-5">
            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-1.5">Z konta</label>
              <select
                value={senderAccountId}
                onChange={(e) => setSenderAccountId(e.target.value)}
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-white focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all appearance-none"
              >
                {accounts.map(acc => (
                  <option key={acc.id} value={acc.id}>
                    {acc.type === 'STANDARD' ? 'Konto Osobiste' : acc.type} - {formatCurrency(acc.balance, acc.currency)}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-1.5">Na rachunek docelowy</label>
              <input
                type="text"
                value={receiverAccountNumber}
                onChange={(e) => setReceiverAccountNumber(e.target.value)}
                placeholder="np. PL12 3456 7890 1234 5678 9012 3456"
                className={`w-full bg-zinc-950 border ${fieldErrors.receiverAccountNumber ? 'border-red-500' : 'border-zinc-800'} rounded-xl px-4 py-3 text-white placeholder-zinc-600 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all font-mono text-sm`}
              />
              {fieldErrors.receiverAccountNumber && (
                <p className="text-red-400 text-xs mt-1.5">{fieldErrors.receiverAccountNumber}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-1.5">Kwota przelewu</label>
              <div className="relative">
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  placeholder="0.00"
                  className={`w-full bg-zinc-950 border ${fieldErrors.amount ? 'border-red-500' : 'border-zinc-800'} rounded-xl pl-4 pr-16 py-3 text-white placeholder-zinc-600 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all text-lg font-medium`}
                />
                <div className="absolute inset-y-0 right-0 flex items-center pr-4 pointer-events-none text-zinc-500 font-medium">
                  PLN
                </div>
              </div>
              {fieldErrors.amount && (
                <p className="text-red-400 text-xs mt-1.5">{fieldErrors.amount}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-1.5">Tytuł przelewu</label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="np. Rozliczenie za obiad"
                className={`w-full bg-zinc-950 border ${fieldErrors.title ? 'border-red-500' : 'border-zinc-800'} rounded-xl px-4 py-3 text-white placeholder-zinc-600 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all`}
                maxLength={140}
              />
              {fieldErrors.title && (
                <p className="text-red-400 text-xs mt-1.5">{fieldErrors.title}</p>
              )}
            </div>

            <button
              type="submit"
              disabled={accounts.length === 0}
              className="w-full bg-blue-600 hover:bg-blue-500 text-white font-medium py-3.5 rounded-xl transition-colors mt-6 disabled:opacity-50 disabled:cursor-not-allowed flex justify-center items-center gap-2"
            >
              Wykonaj przelew
            </button>
          </form>
        </div>
      </main>

      {showPinModal && (
  <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center px-4">
    <div className="w-full max-w-sm bg-zinc-900 border border-zinc-800 rounded-2xl p-6 shadow-2xl">
      {lockedUntil ? (
        <>
          <div className="w-14 h-14 bg-red-500/10 border border-red-500/30 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-7 w-7 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </div>
          <h3 className="text-lg font-semibold text-white text-center mb-1">PIN został zablokowany</h3>
          <p className="text-zinc-400 text-sm text-center mb-5">
            Wpisałeś nieprawidłowy PIN zbyt wiele razy. Z powodów bezpieczeństwa
            potwierdzanie transakcji jest tymczasowo niedostępne.
          </p>

          <div className="bg-zinc-950 border border-red-500/30 rounded-xl px-4 py-5 mb-5 text-center">
            <p className="text-xs uppercase tracking-wider text-zinc-500 mb-1">Pozostały czas blokady</p>
            <p className="text-4xl font-mono font-bold text-red-400 tabular-nums">
              {formatCountdown(lockedUntil.getTime() - nowTs)}
            </p>
            <p className="text-xs text-zinc-600 mt-2">
              Odblokowanie: {lockedUntil.toLocaleTimeString('pl-PL')}
            </p>
          </div>

          <button
            type="button"
            onClick={() => { setShowPinModal(false); }}
            className="w-full bg-zinc-800 hover:bg-zinc-700 text-zinc-200 font-medium py-2.5 rounded-lg text-sm transition-colors"
          >
            Zamknij
          </button>
        </>
      ) : (
        <>
          <h3 className="text-lg font-semibold text-white text-center mb-1">Potwierdź PIN-em</h3>
          <p className="text-zinc-500 text-sm text-center mb-5">
            Wpisz 4-cyfrowy kod PIN, aby zatwierdzić przelew.
          </p>

          <input
            type="password"
            inputMode="numeric"
            autoFocus
            value={pin}
            onChange={(e) => {
              setPin(e.target.value.replace(/\D/g, '').slice(0, 4));
              setPinError(null);
            }}
            placeholder="••••"
            maxLength={4}
            className="w-full bg-zinc-800 border border-zinc-700 text-white text-center tracking-[0.6em] text-2xl rounded-lg px-4 py-3 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-colors"
          />

          {pinError && (
            <p className="text-red-400 text-xs mt-2 text-center">{pinError}</p>
          )}

          <div className="flex gap-3 mt-5">
            <button
              type="button"
              onClick={() => { setShowPinModal(false); setPin(''); setPinError(null); }}
              disabled={isSubmitting}
              className="flex-1 bg-zinc-800 hover:bg-zinc-700 text-zinc-200 font-medium py-2.5 rounded-lg text-sm transition-colors disabled:opacity-50"
            >
              Anuluj
            </button>
            <button
              type="button"
              onClick={confirmWithPin}
              disabled={isSubmitting || pin.length !== 4}
              className="flex-1 bg-blue-600 hover:bg-blue-500 disabled:bg-blue-600/50 disabled:cursor-not-allowed text-white font-medium py-2.5 rounded-lg text-sm transition-colors flex items-center justify-center gap-2"
            >
              {isSubmitting ? (
                <div className="animate-spin rounded-full h-5 w-5 border-t-2 border-b-2 border-white"></div>
              ) : (
                'Potwierdź'
              )}
            </button>
          </div>
        </>
      )}
    </div>
  </div>
)}
    </div>
  );
}