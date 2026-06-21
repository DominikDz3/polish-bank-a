import { useEffect, useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import { klikP2PService } from '../services/klikP2PService';
import { useAuth } from '../contexts/AuthContext';

interface AccountSummary {
  id: string;
  accountNumber: string;
  balance: number;
  currency: string;
}

interface ErrorResponseData {
  lockedUntil?: string;
  detail?: string;
  message?: string;
}

export default function KlikP2PTransfer() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const [accounts, setAccounts] = useState<AccountSummary[]>([]);
  const [senderAccountId, setSenderAccountId] = useState('');
  const [phone, setPhone] = useState('+48');
  const [receiverName, setReceiverName] = useState('');
  const [amount, setAmount] = useState('');
  const [title, setTitle] = useState('');

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const [showPinModal, setShowPinModal] = useState(false);
  const [pin, setPin] = useState('');
  const [pinError, setPinError] = useState<string | null>(null);
  const [lockedUntil, setLockedUntil] = useState<Date | null>(null);
  const [nowTs, setNowTs] = useState<number>(() => Date.now());

  const loadAccounts = async () => {
    try {
      const res = await api.get<AccountSummary[]>('/api/accounts');
      setAccounts(res.data);
      if (res.data.length > 0 && !senderAccountId) setSenderAccountId(res.data[0].id);
    } catch {
      setError('Nie udało się pobrać kont.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadAccounts(); }, []);

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

  const validate = () => {
    const errs: Record<string, string> = {};
    if (!/^\+\d{8,15}$/.test(phone)) errs.phone = 'Numer w formacie E.164 (np. +48501234567).';
    if (!receiverName.trim()) errs.receiverName = 'Podaj nazwę odbiorcy.';
    if (!amount || parseFloat(amount) <= 0) errs.amount = 'Podaj kwotę większą od 0.';
    if (!title.trim()) errs.title = 'Tytuł nie może być pusty.';
    setFieldErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);
    if (!validate()) return;
    setPin('');
    setPinError(null);
    setShowPinModal(true);
  };

  const confirmWithPin = async () => {
    if (lockedUntil) return;
    if (!/^\d{4}$/.test(pin)) {
      setPinError('PIN musi mieć 4 cyfry.');
      return;
    }
    setPinError(null);
    setSubmitting(true);
    try {
      const res = await klikP2PService.send({
        senderAccountId,
        phone,
        receiverName,
        amount: parseFloat(amount),
        title,
        pin,
      });
      const route = res.routing === 'INTERNAL' ? 'wewnętrznie' : `przez Express ELIXIR do ${res.receiverBank}`;
      setSuccess(`Przelew P2P zrealizowany ${route} (${res.status}).`);
      setPhone('+48');
      setReceiverName('');
      setAmount('');
      setTitle('');
      setShowPinModal(false);
      setPin('');
      await loadAccounts();
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        const st = err.response?.status;
        const data = err.response?.data as ErrorResponseData | undefined;
        if (st === 423 && data?.lockedUntil) {
          setLockedUntil(new Date(data.lockedUntil));
          setPin('');
        } else {
          setPinError(data?.detail || data?.message || 'Błąd realizacji przelewu.');
        }
      } else {
        setPinError('Błąd realizacji przelewu.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const formatCurrency = (v: number, c: string) =>
    new Intl.NumberFormat('pl-PL', { style: 'currency', currency: c }).format(v);

  const formatCountdown = (ms: number) => {
    const t = Math.max(0, Math.ceil(ms / 1000));
    return `${Math.floor(t / 60)}:${(t % 60).toString().padStart(2, '0')}`;
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-zinc-950 flex justify-center items-center">
        <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-zinc-950 text-white font-sans flex flex-col">
      <nav className="bg-zinc-900/80 backdrop-blur-md border-b border-zinc-800 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button onClick={() => navigate(-1)} className="text-zinc-400 hover:text-white">← Wróć</button>
            <div className="h-6 w-px bg-zinc-800"></div>
            <div className="text-xl font-bold"><span className="text-blue-400">Bankly</span></div>
            <span className="text-zinc-500 text-sm hidden md:inline">/ Przelew na telefon (KLIK)</span>
          </div>
          <div className="flex items-center gap-6">
            <span className="text-zinc-200 text-sm hidden md:inline">{user?.email}</span>
            <button onClick={logout} className="text-zinc-400 hover:text-white px-4 py-2 rounded-lg">Wyloguj się</button>
          </div>
        </div>
      </nav>

      <main className="flex-grow p-4 md:p-8 flex justify-center items-start pt-12">
        <div className="w-full max-w-xl bg-zinc-900 border border-zinc-800 rounded-2xl p-6 md:p-8 shadow-xl">
          <h2 className="text-2xl font-bold mb-1">Przelew na telefon (KLIK)</h2>
          <p className="text-zinc-500 text-sm mb-6">Wpisz numer telefonu odbiorcy zarejestrowany w KLIK — zostanie zrealizowany natychmiast.</p>

          {success && <div className="bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 p-4 rounded-xl mb-4">{success}</div>}
          {error && <div className="bg-red-500/10 border border-red-500/20 text-red-400 p-4 rounded-xl mb-4">{error}</div>}

          <form onSubmit={submit} noValidate className="space-y-5">
            <div>
              <label className="block text-sm text-zinc-400 mb-1.5">Z konta</label>
              <select value={senderAccountId} onChange={(e) => setSenderAccountId(e.target.value)}
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-white">
                {accounts.map(acc => (
                  <option key={acc.id} value={acc.id}>
                    {acc.accountNumber} — {formatCurrency(acc.balance, acc.currency)}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm text-zinc-400 mb-1.5">Numer telefonu odbiorcy</label>
              <input value={phone} onChange={(e) => setPhone(e.target.value)}
                placeholder="+48501234567"
                className={`w-full bg-zinc-950 border ${fieldErrors.phone ? 'border-red-500' : 'border-zinc-800'} rounded-xl px-4 py-3 text-white font-mono`} />
              {fieldErrors.phone && <p className="text-red-400 text-xs mt-1.5">{fieldErrors.phone}</p>}
            </div>

            <div>
              <label className="block text-sm text-zinc-400 mb-1.5">Nazwa odbiorcy</label>
              <input value={receiverName} onChange={(e) => setReceiverName(e.target.value)}
                placeholder="Jan Kowalski"
                className={`w-full bg-zinc-950 border ${fieldErrors.receiverName ? 'border-red-500' : 'border-zinc-800'} rounded-xl px-4 py-3 text-white`} />
              {fieldErrors.receiverName && <p className="text-red-400 text-xs mt-1.5">{fieldErrors.receiverName}</p>}
            </div>

            <div>
              <label className="block text-sm text-zinc-400 mb-1.5">Kwota</label>
              <div className="relative">
                <input type="number" step="0.01" min="0.01" value={amount}
                  onChange={(e) => setAmount(e.target.value)} placeholder="0.00"
                  className={`w-full bg-zinc-950 border ${fieldErrors.amount ? 'border-red-500' : 'border-zinc-800'} rounded-xl pl-4 pr-16 py-3 text-white text-lg`} />
                <div className="absolute inset-y-0 right-0 flex items-center pr-4 text-zinc-500">PLN</div>
              </div>
              {fieldErrors.amount && <p className="text-red-400 text-xs mt-1.5">{fieldErrors.amount}</p>}
            </div>

            <div>
              <label className="block text-sm text-zinc-400 mb-1.5">Tytuł</label>
              <input value={title} onChange={(e) => setTitle(e.target.value)} maxLength={140}
                placeholder="np. Za pizzę"
                className={`w-full bg-zinc-950 border ${fieldErrors.title ? 'border-red-500' : 'border-zinc-800'} rounded-xl px-4 py-3 text-white`} />
              {fieldErrors.title && <p className="text-red-400 text-xs mt-1.5">{fieldErrors.title}</p>}
            </div>

            <button type="submit" disabled={accounts.length === 0}
              className="w-full bg-blue-600 hover:bg-blue-500 text-white font-medium py-3.5 rounded-xl mt-6 disabled:opacity-50">
              Wyślij KLIK
            </button>
          </form>
        </div>
      </main>

      {showPinModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center px-4">
          <div className="w-full max-w-sm bg-zinc-900 border border-zinc-800 rounded-2xl p-6 shadow-2xl">
            {lockedUntil ? (
              <>
                <h3 className="text-lg font-semibold text-center mb-1">PIN zablokowany</h3>
                <div className="bg-zinc-950 border border-red-500/30 rounded-xl px-4 py-5 my-5 text-center">
                  <p className="text-4xl font-mono font-bold text-red-400">
                    {formatCountdown(lockedUntil.getTime() - nowTs)}
                  </p>
                </div>
                <button type="button" onClick={() => setShowPinModal(false)}
                  className="w-full bg-zinc-800 hover:bg-zinc-700 py-2.5 rounded-lg text-sm">Zamknij</button>
              </>
            ) : (
              <>
                <h3 className="text-lg font-semibold text-center mb-1">Potwierdź PIN-em</h3>
                <p className="text-zinc-500 text-sm text-center mb-5">4-cyfrowy kod PIN.</p>
                <input type="password" inputMode="numeric" autoFocus value={pin}
                  onChange={(e) => { setPin(e.target.value.replace(/\D/g, '').slice(0, 4)); setPinError(null); }}
                  placeholder="••••" maxLength={4}
                  className="w-full bg-zinc-800 border border-zinc-700 text-center tracking-[0.6em] text-2xl rounded-lg px-4 py-3" />
                {pinError && <p className="text-red-400 text-xs mt-2 text-center">{pinError}</p>}
                <div className="flex gap-3 mt-5">
                  <button type="button" onClick={() => { setShowPinModal(false); setPin(''); setPinError(null); }}
                    disabled={submitting}
                    className="flex-1 bg-zinc-800 hover:bg-zinc-700 py-2.5 rounded-lg text-sm disabled:opacity-50">
                    Anuluj
                  </button>
                  <button type="button" onClick={confirmWithPin} disabled={submitting || pin.length !== 4}
                    className="flex-1 bg-blue-600 hover:bg-blue-500 disabled:bg-blue-600/50 py-2.5 rounded-lg text-sm">
                    {submitting ? '...' : 'Potwierdź'}
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