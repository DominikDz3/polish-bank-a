import React, { useEffect, useState } from 'react';
import { CreditCard, AlertCircle, CheckCircle2 } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { cardService } from '../../services/cardService';
import type { CardSummary } from '../../services/cardService';

const CardsPage: React.FC = () => {
  const navigate = useNavigate();
  const [cards, setCards] = useState<CardSummary[]>([]);
  const [selected, setSelected] = useState<string>('');
  const [amount, setAmount] = useState('');
  const [merchant, setMerchant] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const load = async () => {
    const data = await cardService.list();
    setCards(data);
    if (data.length > 0 && !selected) setSelected(data[0].id);
  };

  useEffect(() => { load(); }, []);

  const handlePay = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);
    const card = cards.find(c => c.id === selected);
    if (!card) return;
    setIsLoading(true);
    try {
      await cardService.pay({
        cardId: selected,
        amount: parseFloat(amount),
        merchant,
        currency: card.currency,
      });
      setSuccess(`Płatność ${amount} ${card.currency} u ${merchant} została zrealizowana.`);
      setAmount('');
      setMerchant('');
      await load();
    } catch (err: any) {
      setError(err.response?.data?.detail
        || err.response?.data?.message
        || 'Płatność odrzucona.');
    } finally {
      setIsLoading(false);
    }
  };

  const fmt = (v: number | null, c: string) =>
    v == null ? '—' : new Intl.NumberFormat('pl-PL', { style: 'currency', currency: c }).format(v);

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

      <main className="flex-grow p-4 md:p-8 max-w-3xl mx-auto w-full">
        <h1 className="text-2xl font-semibold mb-6">Twoje karty</h1>

        <div className="grid gap-4 mb-6">
          {cards.map(card => (
            <div key={card.id} className="bg-zinc-900 border border-zinc-800 rounded-2xl p-5">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-3">
                  <CreditCard className="text-blue-400" size={22} />
                  <div>
                    <p className="font-mono text-sm text-zinc-300">{card.maskedNumber}</p>
                    <p className="text-xs text-zinc-500">{card.type} · {card.currency}</p>
                  </div>
                </div>
                {card.blocked && <span className="text-xs text-red-400 border border-red-500/30 rounded px-2 py-1">ZABLOKOWANA</span>}
              </div>
              <div className="grid grid-cols-3 gap-3 text-xs">
                <div className="bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2">
                  <p className="text-zinc-500 mb-1">Limit transakcji</p>
                  <p className="text-zinc-200 font-medium">{fmt(card.transactionLimit, card.currency)}</p>
                </div>
                <div className="bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2">
                  <p className="text-zinc-500 mb-1">Limit dzienny</p>
                  <p className="text-zinc-200 font-medium">{fmt(card.dailyLimit, card.currency)}</p>
                </div>
                <div className="bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2">
                  <p className="text-zinc-500 mb-1">Wydano dziś</p>
                  <p className="text-zinc-200 font-medium">{fmt(card.spentToday, card.currency)}</p>
                </div>
              </div>
            </div>
          ))}
          {cards.length === 0 && (
            <p className="text-zinc-500 text-sm">Nie posiadasz jeszcze żadnej karty.</p>
          )}
        </div>

        {cards.length > 0 && (
          <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-6">
            <h2 className="text-lg font-semibold mb-4">Symulacja płatności kartą</h2>

            {success && (
              <div className="flex items-center gap-2 bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-sm rounded-lg px-4 py-3 mb-4">
                <CheckCircle2 size={16} /> {success}
              </div>
            )}
            {error && (
              <div className="flex items-center gap-2 bg-red-500/10 border border-red-500/20 text-red-400 text-sm rounded-lg px-4 py-3 mb-4">
                <AlertCircle size={16} /> {error}
              </div>
            )}

            <form onSubmit={handlePay} className="grid gap-3 md:grid-cols-2">
              <select value={selected} onChange={e => setSelected(e.target.value)}
                className="bg-zinc-800 border border-zinc-700 text-white rounded-lg px-4 py-2.5 text-sm md:col-span-2">
                {cards.map(c => (
                  <option key={c.id} value={c.id}>{c.maskedNumber} ({c.type})</option>
                ))}
              </select>

              <input value={merchant} onChange={e => setMerchant(e.target.value)}
                placeholder="Merchant (np. Biedronka)"
                className="bg-zinc-800 border border-zinc-700 text-white rounded-lg px-4 py-2.5 text-sm" />

              <input type="number" step="0.01" min="0.01" value={amount}
                onChange={e => setAmount(e.target.value)}
                placeholder="Kwota"
                className="bg-zinc-800 border border-zinc-700 text-white rounded-lg px-4 py-2.5 text-sm" />

              <button type="submit" disabled={isLoading || !amount || !merchant}
                className="bg-blue-600 hover:bg-blue-500 disabled:bg-blue-600/50 text-white font-medium py-2.5 rounded-lg text-sm md:col-span-2">
                {isLoading ? 'Przetwarzanie...' : 'Zapłać kartą'}
              </button>
            </form>
          </div>
        )}
      </main>
    </div>
  );
};

export default CardsPage;