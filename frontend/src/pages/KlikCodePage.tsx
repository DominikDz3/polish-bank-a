import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { api } from '../services/api';
import { klikCodeService } from '../services/klikCodeService';
import type { KlikCode } from '../services/klikCodeService';

interface Account {
  id: string;
  accountNumber: string;
  balance: number;
  currency: string;
  type: string;
}

export default function KlikCodePage() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const [accounts, setAccounts] = useState<Account[]>([]);
  const [selectedAccountId, setSelectedAccountId] = useState<string>('');
  const [code, setCode] = useState<KlikCode | null>(null);
  const [secondsLeft, setSecondsLeft] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Pobierz konta przy mount
  useEffect(() => {
    api.get('/api/accounts').then(r => {
      const standard = r.data.filter((a: Account) => a.type !== 'JUNIOR');
      setAccounts(standard);
      if (standard.length > 0) setSelectedAccountId(standard[0].id);
    });
  }, []);

  const generate = useCallback(async (accountId: string) => {
    setError('');
    setLoading(true);
    try {
      const result = await klikCodeService.generateCode(accountId);
      setCode(result);
    } catch {
      setError('Nie udało się wygenerować kodu. Spróbuj ponownie.');
      setCode(null);
    } finally {
      setLoading(false);
    }
  }, []);

  // Auto-generuj kod gdy wybierzemy konto albo zmienimy
  useEffect(() => {
    if (selectedAccountId) {
      generate(selectedAccountId);
    }
  }, [selectedAccountId, generate]);

  // Timer + auto-regeneracja
  useEffect(() => {
    if (!code) return;

    // Backend zwraca LocalDateTime bez strefy ale jest to UTC — dodajemy Z
    const expires = new Date(code.expiresAt + 'Z').getTime();

    const tick = () => {
      const left = Math.max(0, Math.floor((expires - Date.now()) / 1000));
      setSecondsLeft(left);
      if (left === 0) {
        setCode(null);
        if (selectedAccountId) generate(selectedAccountId);
      }
    };

    tick();
    const interval = setInterval(tick, 500);
    return () => clearInterval(interval);
  }, [code, generate, selectedAccountId]);

  const formatCode = (c: string) => `${c.slice(0, 3)} ${c.slice(3)}`;

  return (
    <div className="min-h-screen bg-zinc-950 text-white">
      <nav className="bg-zinc-900/80 backdrop-blur-md border-b border-zinc-800 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button onClick={() => navigate('/dashboard')} className="text-zinc-400 hover:text-white text-sm transition-colors">
              ← Wróć
            </button>
            <div className="h-5 w-px bg-zinc-700" />
            <span className="text-xl font-bold"><span className="text-blue-400">Bankly</span></span>
            <span className="text-zinc-500 text-sm hidden md:block">/ Kod BLIK</span>
          </div>
          <div className="flex items-center gap-6">
            <div className="hidden md:flex flex-col text-right">
              <span className="text-zinc-200 text-sm font-medium">{user?.email}</span>
              <span className="text-zinc-500 text-xs">Numer klienta: <span className="text-zinc-400">{user?.customerNumber}</span></span>
            </div>
            <div className="h-8 w-px bg-zinc-800 hidden md:block" />
            <button onClick={logout} className="text-zinc-400 hover:text-white px-4 py-2 rounded-lg font-medium transition-colors">
              Wyloguj się
            </button>
          </div>
        </div>
      </nav>

      <main className="max-w-md mx-auto px-6 py-10">
        <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-8">
          <h1 className="text-xl font-semibold text-white mb-2">Kod BLIK</h1>
          <p className="text-zinc-500 text-sm mb-6">Użyj kodu do płatności w sklepie lub online. Odświeży się automatycznie.</p>

          {accounts.length === 1 ? (
            <div className="mb-6 bg-zinc-950/50 border border-zinc-800 rounded-xl px-4 py-3">
                <div className="text-zinc-500 text-xs uppercase tracking-wider mb-1">Płacisz z</div>
                    <div className="text-zinc-200 text-sm font-medium">
                        {accounts[0].type === 'STANDARD' ? 'Konto Osobiste' : accounts[0].type}
                    </div>
                    <div className="text-zinc-500 text-xs font-mono mt-0.5">
                        ••••{accounts[0].accountNumber.slice(-4)} · {accounts[0].balance.toLocaleString('pl-PL', { minimumFractionDigits: 2 })} {accounts[0].currency}
                    </div>
            </div>
        ) : accounts.length > 1 ? (
        <div className="mb-6">
            <div className="text-zinc-500 text-xs uppercase tracking-wider mb-2">Płacisz z</div>
                <div className="space-y-2">
                    {accounts.map(a => {
                    const isSelected = a.id === selectedAccountId;
                    return (
                <button
                    key={a.id}
                    onClick={() => setSelectedAccountId(a.id)}
                    className={`w-full text-left rounded-xl px-4 py-3 transition-all border ${
                    isSelected
                    ? 'bg-blue-500/10 border-blue-500/50'
                    : 'bg-zinc-950/50 border-zinc-800 hover:border-zinc-700'
                    }`}
                >
                <div className="flex items-center justify-between">
                    <div>
                        <div className={`text-sm font-medium ${isSelected ? 'text-blue-300' : 'text-zinc-200'}`}>
                            {a.type === 'STANDARD' ? 'Konto Osobiste' : a.type === 'JUNIOR' ? 'Konto Junior' : a.type}
                        </div>
                        <div className="text-zinc-500 text-xs font-mono mt-0.5">
                            ••••{a.accountNumber.slice(-4)}
                        </div>
                    </div>
                    <div className={`text-sm font-semibold ${isSelected ? 'text-blue-300' : 'text-zinc-300'}`}>
                        {a.balance.toLocaleString('pl-PL', { minimumFractionDigits: 2 })} {a.currency}
                    </div>
                </div>
                </button>
                );
            })}
            </div>
        </div>
        ) : null}

          {error && (
            <div className="bg-red-500/10 border border-red-500/20 text-red-400 text-sm rounded-lg px-4 py-3 mb-4">
              {error}
            </div>
          )}

          {loading && !code ? (
            <div className="flex justify-center py-12">
              <span className="w-8 h-8 border-2 border-blue-500/30 border-t-blue-500 rounded-full animate-spin" />
            </div>
          ) : code ? (
            <div className="text-center py-6">
              <div className="text-6xl font-mono font-bold text-blue-400 tracking-widest mb-4">
                {formatCode(code.code)}
              </div>
              <div className="relative w-full h-2 bg-zinc-800 rounded-full overflow-hidden mb-3">
                <div
                  className="absolute inset-y-0 left-0 bg-blue-500 transition-all duration-500"
                  style={{ width: `${(secondsLeft / 120) * 100}%` }}
                />
              </div>
              <p className="text-zinc-500 text-sm">
                Ważny jeszcze: <span className="text-zinc-300 font-medium">{secondsLeft}s</span>
              </p>
              <p className="text-zinc-600 text-xs mt-3">
                Kod odświeży się automatycznie po wygaśnięciu
              </p>
            </div>
          ) : null}
        </div>
      </main>
    </div>
  );
}