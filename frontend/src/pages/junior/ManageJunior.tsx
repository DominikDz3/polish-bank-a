import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { AlertCircle, CheckCircle2, CreditCard, Save } from 'lucide-react';
import { cardService } from '../../services/cardService';
import type { CardSummary } from '../../services/cardService';
import { juniorService } from '../../services/juniorService';
import type { JuniorResponse } from '../../services/juniorService';
import { useAuth } from '../../contexts/AuthContext';

const ManageJunior: React.FC = () => {
  const navigate = useNavigate();
  const { accountId } = useParams<{ accountId: string }>();
  const {user, logout} = useAuth();
  const [junior, setJunior] = useState<JuniorResponse | null>(null);
  const [cards, setCards] = useState<CardSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [savingCardId, setSavingCardId] = useState<string | null>(null);
  const [drafts, setDrafts] = useState<Record<string, { transactionLimit: string; dailyLimit: string }>>({});

  const load = async () => {
    if (!accountId) return;
    setLoading(true);
    try {
      const juniors = await juniorService.list();
      const j = juniors.find(j => j.accountId === accountId);
      if (!j) {
        setError('Konto Junior nie znalezione lub nie należy do Ciebie.');
        return;
      }
      setJunior(j);

      const cardList = await cardService.listForJunior(accountId);
      setCards(cardList);
      const initial: Record<string, { transactionLimit: string; dailyLimit: string }> = {};
      cardList.forEach(c => {
        initial[c.id] = {
          transactionLimit: c.transactionLimit?.toString() ?? '',
          dailyLimit: c.dailyLimit?.toString() ?? '',
        };
      });
      setDrafts(initial);
    } catch (err: any) {
      setError(err.response?.data?.detail || err.response?.data?.message || 'Nie udało się pobrać danych.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [accountId]);

  const handleSave = async (cardId: string) => {
    setError(null);
    setSuccess(null);
    setSavingCardId(cardId);
    try {
      const d = drafts[cardId];
      const payload: { transactionLimit?: number; dailyLimit?: number } = {};
      if (d.transactionLimit !== '') payload.transactionLimit = parseFloat(d.transactionLimit);
      if (d.dailyLimit !== '') payload.dailyLimit = parseFloat(d.dailyLimit);

      const res = await cardService.updateLimits(cardId, payload);
      setSuccess(res.message);
      await load();
    } catch (err: any) {
      setError(err.response?.data?.detail || err.response?.data?.message || 'Nie udało się zapisać limitów.');
    } finally {
      setSavingCardId(null);
    }
  };

  const fmt = (v: number, c: string) =>
    new Intl.NumberFormat('pl-PL', { style: 'currency', currency: c }).format(v);

  return (
    <div className="min-h-screen bg-zinc-950 text-white font-sans flex flex-col">
      <nav className="bg-zinc-900/80 backdrop-blur-md border-b border-zinc-800 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
            <div className="flex items-center gap-4">
                <button type="button" onClick={() => navigate('/dashboard?tab=junior')}
                className="text-zinc-400 hover:text-white flex items-center gap-2 transition-colors">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
                Wróć
                </button>
            <div className="h-6 w-px bg-zinc-800"></div>
            <div className="text-xl font-bold tracking-tight text-white">
                <span className="text-blue-400">Bankly</span>
            </div>
         <span className="text-zinc-500 text-sm hidden md:inline">/ Zarządzanie kontem Junior</span>
        </div>

        <div className="flex items-center gap-6">
            <div className="hidden md:flex flex-col text-right">
                <span className="text-zinc-200 text-sm font-medium">{user?.email}</span>
                <span className="text-zinc-500 text-xs mt-0.5 tracking-wide">
                Numer klienta: <span className="text-zinc-400">{user?.customerNumber}</span>
                </span>
        </div>
            <div className="h-8 w-px bg-zinc-800 hidden md:block"></div>
            <button
                onClick={logout}
                className="text-zinc-400 hover:text-white px-4 py-2 rounded-lg font-medium transition-colors duration-150"
            >
                Wyloguj się
            </button>
            </div>
         </div>
        </nav>

      <main className="flex-grow p-4 md:p-8 max-w-3xl mx-auto w-full">
        {loading ? (
          <div className="flex justify-center py-20">
            <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-blue-500"></div>
          </div>
        ) : (
          <>
            {junior && (
              <header className="mb-6">
                <p className="text-zinc-500 text-sm">Zarządzanie kontem</p>
                <h1 className="text-3xl font-bold text-zinc-100">{junior.firstName} {junior.lastName}</h1>
                <p className="text-zinc-500 text-sm mt-1 font-mono">{junior.accountNumber}</p>
                <p className="text-zinc-400 text-sm mt-2">Saldo: <span className="text-white font-medium">{fmt(junior.balance, junior.currency)}</span></p>
              </header>
            )}

            {success && (
              <div className="bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 p-3 rounded-xl mb-4 flex items-center gap-2 text-sm">
                <CheckCircle2 size={16} /> {success}
              </div>
            )}
            {error && (
              <div className="bg-red-500/10 border border-red-500/20 text-red-400 p-3 rounded-xl mb-4 flex items-center gap-2 text-sm">
                <AlertCircle size={16} /> {error}
              </div>
            )}

            <section>
              <h2 className="text-lg font-semibold text-zinc-200 mb-3">Karty prepaid i limity</h2>

              {cards.length === 0 ? (
                <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-8 text-center">
                  <CreditCard className="text-zinc-600 mx-auto mb-3" size={28} />
                  <p className="text-zinc-400">To dziecko nie ma jeszcze żadnej karty.</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {cards.map(card => {
                    const draft = drafts[card.id] || { transactionLimit: '', dailyLimit: '' };
                    const editable = card.type === 'PREPAID';
                    return (
                      <div key={card.id} className="bg-zinc-900 border border-zinc-800 rounded-2xl p-5">
                        <div className="flex items-center justify-between mb-4">
                          <div className="flex items-center gap-3">
                            <CreditCard className="text-blue-400" size={22} />
                            <div>
                              <p className="font-mono text-sm text-zinc-300">{card.maskedNumber}</p>
                              <p className="text-xs text-zinc-500">{card.type} · {card.currency}</p>
                            </div>
                          </div>
                          {card.blocked && <span className="text-xs text-red-400 border border-red-500/30 rounded px-2 py-1">ZABLOKOWANA</span>}
                        </div>

                        {!editable ? (
                          <p className="text-zinc-500 text-sm">
                            Edycja limitów dostępna tylko dla kart typu PREPAID.
                          </p>
                        ) : (
                          <div className="space-y-3">
                            <div>
                              <label className="block text-xs text-zinc-500 mb-1.5">
                                Limit pojedynczej transakcji ({card.currency})
                              </label>
                              <input
                                type="number"
                                step="0.01"
                                min="0"
                                value={draft.transactionLimit}
                                onChange={e => setDrafts({ ...drafts, [card.id]: { ...draft, transactionLimit: e.target.value } })}
                                placeholder="np. 100"
                                className="w-full bg-zinc-800 border border-zinc-700 text-white rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:border-blue-500"
                              />
                            </div>
                            <div>
                              <label className="block text-xs text-zinc-500 mb-1.5">
                                Limit dzienny ({card.currency})
                              </label>
                              <input
                                type="number"
                                step="0.01"
                                min="0"
                                value={draft.dailyLimit}
                                onChange={e => setDrafts({ ...drafts, [card.id]: { ...draft, dailyLimit: e.target.value } })}
                                placeholder="np. 300"
                                className="w-full bg-zinc-800 border border-zinc-700 text-white rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:border-blue-500"
                              />
                              <p className="text-xs text-zinc-600 mt-1">Wydano dziś: {fmt(card.spentToday, card.currency)}</p>
                            </div>

                            <button
                              type="button"
                              onClick={() => handleSave(card.id)}
                              disabled={savingCardId === card.id}
                              className="w-full bg-blue-600 hover:bg-blue-500 disabled:bg-blue-600/50 text-white font-medium py-2.5 rounded-lg text-sm transition-colors flex items-center justify-center gap-2"
                            >
                              {savingCardId === card.id ? (
                                <div className="animate-spin rounded-full h-4 w-4 border-t-2 border-b-2 border-white"></div>
                              ) : (
                                <><Save size={16} /> Zapisz limity</>
                              )}
                            </button>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </section>
          </>
        )}
      </main>
    </div>
  );
};

export default ManageJunior;