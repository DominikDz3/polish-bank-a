import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { AlertCircle, CheckCircle2, CreditCard, Save, Plus, Power, Lock, Unlock, Wallet, X, Copy } from 'lucide-react';
import { cardService } from '../../services/cardService';
import type { CardSummary, OrderCardResponse } from '../../services/cardService';
import { juniorService } from '../../services/juniorService';
import type { JuniorResponse } from '../../services/juniorService';
import { useAuth } from '../../contexts/AuthContext';

const STATUS_LABELS: Record<string, string> = {
  REQUESTED: 'Zamówiona',
  PRODUCING: 'W produkcji',
  SHIPPED: 'Wysłana',
  ACTIVE: 'Aktywna',
  BLOCKED: 'Zablokowana',
};

const STATUS_COLORS: Record<string, string> = {
  REQUESTED: 'text-amber-400 border-amber-500/30',
  PRODUCING: 'text-amber-400 border-amber-500/30',
  SHIPPED: 'text-blue-400 border-blue-500/30',
  ACTIVE: 'text-emerald-400 border-emerald-500/30',
  BLOCKED: 'text-red-400 border-red-500/30',
};

const ManageJunior: React.FC = () => {
  const navigate = useNavigate();
  const { accountId } = useParams<{ accountId: string }>();
  const { user, logout } = useAuth();
  const [junior, setJunior] = useState<JuniorResponse | null>(null);
  const [cards, setCards] = useState<CardSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [savingCardId, setSavingCardId] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [drafts, setDrafts] = useState<Record<string, { transactionLimit: string; dailyLimit: string }>>({});
  const [issuedCard, setIssuedCard] = useState<OrderCardResponse | null>(null);
  const [topupCardId, setTopupCardId] = useState<string | null>(null);
  const [topupAmount, setTopupAmount] = useState('');

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
    setError(null); setSuccess(null); setSavingCardId(cardId);
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

  const handleOrderPrepaid = async () => {
    if (!accountId) return;
    setError(null); setSuccess(null); setActionLoading('order');
    try {
      const issued = await cardService.orderJunior(accountId, 'PREPAID');
      setIssuedCard(issued);
      await load();
    } catch (err: any) {
      setError(err.response?.data?.detail || err.response?.data?.message || 'Nie udało się zamówić karty.');
    } finally {
      setActionLoading(null);
    }
  };

  const handleForceActivate = async (cardId: string) => {
    setError(null); setSuccess(null); setActionLoading(`force-${cardId}`);
    try {
      const res = await cardService.devForceActivate(cardId);
      setSuccess(res.message);
      await load();
    } catch (err: any) {
      setError(err.response?.data?.detail || err.response?.data?.message || 'Nie udało się aktywować.');
    } finally {
      setActionLoading(null);
    }
  };

  const handleBlock = async (cardId: string) => {
    setError(null); setSuccess(null); setActionLoading(`block-${cardId}`);
    try {
      const res = await cardService.block(cardId);
      setSuccess(res.message);
      await load();
    } catch (err: any) {
      setError(err.response?.data?.detail || err.response?.data?.message || 'Nie udało się zablokować.');
    } finally {
      setActionLoading(null);
    }
  };

  const handleUnblock = async (cardId: string) => {
    setError(null); setSuccess(null); setActionLoading(`unblock-${cardId}`);
    try {
      const res = await cardService.unblock(cardId);
      setSuccess(res.message);
      await load();
    } catch (err: any) {
      setError(err.response?.data?.detail || err.response?.data?.message || 'Nie udało się odblokować.');
    } finally {
      setActionLoading(null);
    }
  };

  const handleTopup = async () => {
    if (!topupCardId || !topupAmount) return;
    setError(null); setSuccess(null); setActionLoading(`topup-${topupCardId}`);
    try {
      const res = await cardService.topup(topupCardId, parseFloat(topupAmount));
      setSuccess(res.message);
      setTopupCardId(null);
      setTopupAmount('');
      await load();
    } catch (err: any) {
      setError(err.response?.data?.detail || err.response?.data?.message || 'Nie udało się doładować karty.');
    } finally {
      setActionLoading(null);
    }
  };

  const fmt = (v: number, c: string) =>
    new Intl.NumberFormat('pl-PL', { style: 'currency', currency: c }).format(v);

  const copyToClipboard = (text: string) => navigator.clipboard.writeText(text);

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
            <button onClick={logout}
              className="text-zinc-400 hover:text-white px-4 py-2 rounded-lg font-medium transition-colors duration-150">
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
              <div className="flex items-center justify-between mb-3">
                <h2 className="text-lg font-semibold text-zinc-200">Karty prepaid i limity</h2>
                <button onClick={handleOrderPrepaid} disabled={actionLoading === 'order'}
                  className="bg-blue-600 hover:bg-blue-500 disabled:bg-blue-600/50 text-white px-3 py-1.5 rounded-lg text-sm font-medium flex items-center gap-2">
                  <Plus size={16} /> Zamów PREPAID
                </button>
              </div>

              {cards.length === 0 ? (
                <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-8 text-center">
                  <CreditCard className="text-zinc-600 mx-auto mb-3" size={28} />
                  <p className="text-zinc-400">To dziecko nie ma jeszcze żadnej karty. Kliknij "Zamów PREPAID".</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {cards.map(card => {
                    const draft = drafts[card.id] || { transactionLimit: '', dailyLimit: '' };
                    const editable = card.type === 'PREPAID';
                    const status = card.providerStatus || (card.blocked ? 'BLOCKED' : 'ACTIVE');
                    const statusLabel = STATUS_LABELS[status] || status;
                    const statusColor = STATUS_COLORS[status] || 'text-zinc-400 border-zinc-500/30';
                    const isBlocked = card.blocked || status === 'BLOCKED';
                    return (
                      <div key={card.id} className="bg-zinc-900 border border-zinc-800 rounded-2xl p-5">
                        <div className="flex items-center justify-between mb-4">
                          <div className="flex items-center gap-3">
                            <CreditCard className="text-blue-400" size={22} />
                            <div>
                              <p className="font-mono text-sm text-zinc-300">{card.maskedPan || card.maskedNumber}</p>
                              <p className="text-xs text-zinc-500">{card.type} · {card.currency}</p>
                            </div>
                          </div>
                          <span className={`text-xs border rounded px-2 py-1 ${statusColor}`}>{statusLabel}</span>
                        </div>

                        <div className="flex flex-wrap gap-2 mb-4">
                          {status !== 'ACTIVE' && status !== 'BLOCKED' && (
                            <button onClick={() => handleForceActivate(card.id)}
                              disabled={actionLoading === `force-${card.id}`}
                              className="bg-amber-600/20 hover:bg-amber-600/30 border border-amber-500/30 text-amber-400 text-xs px-3 py-1.5 rounded-lg flex items-center gap-1.5 disabled:opacity-50">
                              <Power size={13} /> Wymuś aktywację (DEV)
                            </button>
                          )}
                          {status === 'ACTIVE' && !isBlocked && card.type === 'PREPAID' && (
                            <button onClick={() => setTopupCardId(card.id)}
                              className="bg-emerald-600/20 hover:bg-emerald-600/30 border border-emerald-500/30 text-emerald-400 text-xs px-3 py-1.5 rounded-lg flex items-center gap-1.5">
                              <Wallet size={13} /> Doładuj
                            </button>
                          )}
                          {status === 'ACTIVE' && !isBlocked && (
                            <button onClick={() => handleBlock(card.id)}
                              disabled={actionLoading === `block-${card.id}`}
                              className="bg-red-600/20 hover:bg-red-600/30 border border-red-500/30 text-red-400 text-xs px-3 py-1.5 rounded-lg flex items-center gap-1.5 disabled:opacity-50">
                              <Lock size={13} /> Zablokuj
                            </button>
                          )}
                          {isBlocked && (
                            <button onClick={() => handleUnblock(card.id)}
                              disabled={actionLoading === `unblock-${card.id}`}
                              className="bg-zinc-800 hover:bg-zinc-700 border border-zinc-700 text-zinc-300 text-xs px-3 py-1.5 rounded-lg flex items-center gap-1.5 disabled:opacity-50">
                              <Unlock size={13} /> Odblokuj
                            </button>
                          )}
                        </div>

                        {!editable ? (
                          <p className="text-zinc-500 text-sm">Edycja limitów dostępna tylko dla kart typu PREPAID.</p>
                        ) : (
                          <div className="space-y-3">
                            <div>
                              <label className="block text-xs text-zinc-500 mb-1.5">
                                Limit pojedynczej transakcji ({card.currency})
                              </label>
                              <input type="number" step="0.01" min="0" value={draft.transactionLimit}
                                onChange={e => setDrafts({ ...drafts, [card.id]: { ...draft, transactionLimit: e.target.value } })}
                                placeholder="np. 100"
                                className="w-full bg-zinc-800 border border-zinc-700 text-white rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:border-blue-500" />
                            </div>
                            <div>
                              <label className="block text-xs text-zinc-500 mb-1.5">
                                Limit dzienny ({card.currency})
                              </label>
                              <input type="number" step="0.01" min="0" value={draft.dailyLimit}
                                onChange={e => setDrafts({ ...drafts, [card.id]: { ...draft, dailyLimit: e.target.value } })}
                                placeholder="np. 300"
                                className="w-full bg-zinc-800 border border-zinc-700 text-white rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:border-blue-500" />
                              <p className="text-xs text-zinc-600 mt-1">Wydano dziś: {fmt(card.spentToday, card.currency)}</p>
                            </div>
                            <button type="button" onClick={() => handleSave(card.id)}
                              disabled={savingCardId === card.id}
                              className="w-full bg-blue-600 hover:bg-blue-500 disabled:bg-blue-600/50 text-white font-medium py-2.5 rounded-lg text-sm transition-colors flex items-center justify-center gap-2">
                              {savingCardId === card.id ? (
                                <div className="animate-spin rounded-full h-4 w-4 border-t-2 border-b-2 border-white"></div>
                              ) : (<><Save size={16} /> Zapisz limity</>)}
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

      {issuedCard && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-6 max-w-md w-full">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-emerald-400">Karta PREPAID wydana</h3>
              <button onClick={() => setIssuedCard(null)} className="text-zinc-500 hover:text-white">
                <X size={20} />
              </button>
            </div>
            <div className="bg-amber-500/10 border border-amber-500/30 text-amber-300 text-xs rounded-lg px-3 py-2 mb-4">
              <strong>Zapisz te dane teraz</strong> — pełny PAN i CVV nie zostaną pokazane ponownie.
            </div>
            <div className="space-y-3 text-sm">
              <div className="bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2 flex items-center justify-between">
                <div className="min-w-0">
                  <p className="text-zinc-500 text-xs mb-1">Numer karty</p>
                  <p className="text-zinc-100 font-mono truncate">{issuedCard.fullPan}</p>
                </div>
                <button onClick={() => copyToClipboard(issuedCard.fullPan)} className="text-zinc-500 hover:text-white p-1 ml-2">
                  <Copy size={14} />
                </button>
              </div>
              <div className="bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2 flex items-center justify-between">
                <div>
                  <p className="text-zinc-500 text-xs mb-1">CVV</p>
                  <p className="text-zinc-100 font-mono">{issuedCard.cvv}</p>
                </div>
                <button onClick={() => copyToClipboard(issuedCard.cvv)} className="text-zinc-500 hover:text-white p-1 ml-2">
                  <Copy size={14} />
                </button>
              </div>
              <div className="bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2">
                <p className="text-zinc-500 text-xs mb-1">Data ważności</p>
                <p className="text-zinc-100 font-mono">{String(issuedCard.expiryMonth).padStart(2, '0')}/{issuedCard.expiryYear}</p>
              </div>
            </div>
            <button onClick={() => setIssuedCard(null)}
              className="w-full mt-5 bg-blue-600 hover:bg-blue-500 text-white py-2.5 rounded-lg text-sm font-medium">
              Zapisałem/am dane — zamknij
            </button>
          </div>
        </div>
      )}

      {topupCardId && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-6 max-w-sm w-full">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">Doładuj kartę PREPAID</h3>
              <button onClick={() => { setTopupCardId(null); setTopupAmount(''); }} className="text-zinc-500 hover:text-white">
                <X size={20} />
              </button>
            </div>
            <p className="text-zinc-400 text-sm mb-4">Środki zostaną pobrane z Twojego konta i doładowane na kartę dziecka.</p>
            <input type="number" step="0.01" min="0.01" value={topupAmount}
              onChange={e => setTopupAmount(e.target.value)}
              placeholder="Kwota (PLN)"
              className="w-full bg-zinc-800 border border-zinc-700 text-white rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:border-blue-500 mb-4" />
            <button onClick={handleTopup}
              disabled={!topupAmount || actionLoading === `topup-${topupCardId}`}
              className="w-full bg-blue-600 hover:bg-blue-500 disabled:bg-blue-600/50 text-white py-2.5 rounded-lg text-sm font-medium flex items-center justify-center gap-2">
              {actionLoading === `topup-${topupCardId}` ? (
                <div className="animate-spin rounded-full h-4 w-4 border-t-2 border-b-2 border-white"></div>
              ) : (<><Wallet size={16} /> Doładuj</>)}
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default ManageJunior;