import { useEffect, useState } from 'react';
import axios from 'axios';
import { api } from '../services/api';
import { klikAliasService, type KlikAliasView } from '../services/klikAliasService';


interface AccountSummary {
  id: string;
  accountNumber: string;
  balance: number;
  currency: string;
}

export default function KlikAliasesSection() {
  const [aliases, setAliases] = useState<KlikAliasView[]>([]);
  const [accounts, setAccounts] = useState<AccountSummary[]>([]);
  const [accountId, setAccountId] = useState('');
  const [phone, setPhone] = useState('+48');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const load = async () => {
    try {
      const [a, ac] = await Promise.all([
        klikAliasService.list(),
        api.get<AccountSummary[]>('/api/accounts'),
      ]);
      setAliases(a);
      setAccounts(ac.data);
      if (ac.data.length > 0 && !accountId) setAccountId(ac.data[0].id);
    } catch {
      setError('Nie udało się pobrać danych.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);
    if (!/^\+\d{8,15}$/.test(phone)) {
      setError('Numer w formacie E.164, np. +48501234567');
      return;
    }
    setSubmitting(true);
    try {
      await klikAliasService.register(accountId, phone);
      setPhone('+48');
      setSuccess('Numer został zarejestrowany w KLIK.');
      await load();
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        const data = err.response?.data as { detail?: string; message?: string } | undefined;
        setError(data?.detail || data?.message || 'Nie udało się zarejestrować numeru.');
      } else {
        setError('Nie udało się zarejestrować numeru.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const remove = async (id: string) => {
    if (!confirm('Usunąć ten numer z KLIK?')) return;
    try {
      await klikAliasService.remove(id);
      await load();
    } catch {
      setError('Nie udało się usunąć numeru.');
    }
  };

  if (loading) {
    return <div className="text-zinc-500 text-sm">Ładowanie…</div>;
  }

  return (
    <div>
      <h2 className="text-lg font-semibold mb-1">Numery KLIK</h2>
      <p className="text-zinc-500 text-sm mb-6">Zarejestruj swój numer, żeby otrzymywać przelewy KLIK na telefon.</p>

      {success && <div className="bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 p-3 rounded-xl mb-4 text-sm">{success}</div>}
      {error && <div className="bg-red-500/10 border border-red-500/20 text-red-400 p-3 rounded-xl mb-4 text-sm">{error}</div>}

      <form onSubmit={submit} className="grid md:grid-cols-[1fr_1fr_auto] gap-3 mb-8">
        <div>
          <label className="block text-xs text-zinc-500 mb-1">Numer telefonu</label>
          <input value={phone} onChange={(e) => setPhone(e.target.value)}
            placeholder="+48501234567"
            className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-2.5 text-white font-mono text-sm" />
        </div>
        <div>
          <label className="block text-xs text-zinc-500 mb-1">Konto</label>
          <select value={accountId} onChange={(e) => setAccountId(e.target.value)}
            className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-2.5 text-white text-sm">
            {accounts.map(acc => (
              <option key={acc.id} value={acc.id}>{acc.accountNumber}</option>
            ))}
          </select>
        </div>
        <button type="submit" disabled={submitting}
          className="self-end bg-blue-600 hover:bg-blue-500 disabled:opacity-50 px-5 py-2.5 rounded-xl text-sm font-medium">
          {submitting ? '...' : 'Dodaj'}
        </button>
      </form>

      <h3 className="text-xs uppercase tracking-wider text-zinc-500 mb-3">Aktywne numery</h3>
      {aliases.length === 0 ? (
        <p className="text-zinc-500 text-sm">Nie masz jeszcze zarejestrowanych numerów.</p>
      ) : (
        <ul className="space-y-2">
          {aliases.map(a => (
            <li key={a.id} className="bg-zinc-950 border border-zinc-800 rounded-xl p-3 flex items-center justify-between">
              <div>
                <p className="font-mono text-white text-sm">{a.alias}</p>
                <p className="text-xs text-zinc-500">{a.accountNumber}</p>
              </div>
              <button onClick={() => remove(a.id)}
                className="text-red-400 hover:text-red-300 text-sm">Usuń</button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}