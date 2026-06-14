import React, { useEffect, useState } from 'react';
import { CreditCard, AlertCircle, CheckCircle2, Plus, Lock, Unlock, Power, Copy, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { cardService } from '../../services/cardService';
import type { CardSummary, OrderCardResponse } from '../../services/cardService';
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

const CardsPage: React.FC = () => {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [cards, setCards] = useState<CardSummary[]>([]);
  const [selected, setSelected] = useState<string>('');
  const [amount, setAmount] = useState('');
  const [merchant, setMerchant] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [orderModal, setOrderModal] = useState(false);
  const [issuedCard, setIssuedCard] = useState<OrderCardResponse | null>(null);

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
      const res = await cardService.pay({
        cardId: selected,
        amount: parseFloat(amount),
        merchant,
        currency: card.currency,
      });
      const msg = res.status === 'PENDING_APPROVAL'
        ? 'Transakcja czeka na zatwierdzenie rodzica.'
        : `Płatność ${amount} ${card.currency} u ${merchant} została zrealizowana.`;
      setSuccess(msg);
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

  const handleOrder = async (cardType: 'VIRTUAL' | 'PHYSICAL') => {
    setError(null);
    setSuccess(null);
    setActionLoading('order');
    try {
      const issued = await cardService.order(cardType);
      setIssuedCard(issued);
      setOrderModal(false);
      await load();
    } catch (err: any) {
      setError(err.response?.data?.detail
        || err.response?.data?.message
        || 'Nie udało się zamówić karty.');
      setOrderModal(false);
    } finally {
      setActionLoading(null);
    }
  };

  const handleBlock = async (cardId: string) => {
    setError(null);
    setSuccess(null);
    setActionLoading(`block-${cardId}`);
    try {
      const res = await cardService.block(cardId);
      setSuccess(res.message);
      await load();
    } catch (err: any) {
      setError(err.response?.data?.detail
        || err.response?.data?.message
        || 'Nie udało się zablokować karty.');
    } finally {
      setActionLoading(null);
    }
  };

  const handleUnblock = async (cardId: string) => {
    setError(null);
    setSuccess(null);
    setActionLoading(`unblock-${cardId}`);
    try {
      const res = await cardService.unblock(cardId);
      setSuccess(res.message);
      await load();
    } catch (err: any) {
      setError(err.response?.data?.detail
        || err.response?.data?.message
        || 'Nie udało się odblokować karty.');
    } finally {
      setActionLoading(null);
    }
  };

  const handleActivate = async (cardId: string) => {
    setError(null);
    setSuccess(null);
    setActionLoading(`activate-${cardId}`);
    try {
      const res = await cardService.activate(cardId);
      setSuccess(res.message);
      await load();
    } catch (err: any) {
      setError(err.response?.data?.detail
        || err.response?.data?.message
        || 'Nie udało się aktywować karty.');
    } finally {
      setActionLoading(null);
    }
  };

  const handleForceActivate = async (cardId: string) => {
    setError(null);
    setSuccess(null);
    setActionLoading(`force-${cardId}`);
    try {
      const res = await cardService.devForceActivate(cardId);
      setSuccess(res.message);
      await load();
    } catch (err: any) {
      setError(err.response?.data?.detail || err.response?.data?.message || 'Nie udało się wymusić aktywacji.');
    } finally {
      setActionLoading(null);
    }
  };

  const fmt = (v: number | null, c: string) =>
    v == null ? '—' : new Intl.NumberFormat('pl-PL', { style: 'currency', currency: c }).format(v);

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
  };

  return (
    <div className="min-h-screen bg-zinc-950 text-white font-sans flex flex-col">
      <nav className="bg-zinc-900/80 backdrop-blur-md border-b border-zinc-800 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button type="button" onClick={() => navigate(-1)}
              className="text-zinc-400 hover:text-white flex items-center gap-2 transition-colors">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
              Wróć
            </button>
            <div className="h-6 w-px bg-zinc-800"></div>
            <div className="text-xl font-bold tracking-tight text-white">
              <span className="text-blue-400">Bankly</span>
            </div>
            <span className="text-zinc-500 text-sm hidden md:inline">/ Moje karty</span>
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
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-semibold">Twoje karty</h1>
          <button
            onClick={() => setOrderModal(true)}
            className="bg-blue-600 hover:bg-blue-500 text-white px-4 py-2 rounded-lg text-sm font-medium flex items-center gap-2">
            <Plus size={16} /> Zamów kartę
          </button>
        </div>

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

        <div className="grid gap-4 mb-6">
          {cards.map(card => {
            const status = card.providerStatus || (card.blocked ? 'BLOCKED' : 'ACTIVE');
            const statusLabel = STATUS_LABELS[status] || status;
            const statusColor = STATUS_COLORS[status] || 'text-zinc-400 border-zinc-500/30';
            const canActivate = status === 'SHIPPED';
            const isBlocked = card.blocked || status === 'BLOCKED';
            return (
              <div key={card.id} className="bg-zinc-900 border border-zinc-800 rounded-2xl p-5">
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-3">
                    <CreditCard className="text-blue-400" size={22} />
                    <div>
                      <p className="font-mono text-sm text-zinc-300">{card.maskedPan || card.maskedNumber}</p>
                      <p className="text-xs text-zinc-500">{card.type} · {card.currency}</p>
                    </div>
                  </div>
                  <span className={`text-xs border rounded px-2 py-1 ${statusColor}`}>{statusLabel}</span>
                </div>
                <div className="grid grid-cols-3 gap-3 text-xs mb-4">
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
                <div className="flex flex-wrap gap-2">
                  {!card.providerToken && (
                    <span className="text-xs text-zinc-500 italic">Karta lokalna — nie zarejestrowana u providera</span>
                  )}
                  {card.providerToken && status !== 'ACTIVE' && status !== 'BLOCKED' && (
                    <button
                      onClick={() => handleForceActivate(card.id)}
                      disabled={actionLoading === `force-${card.id}`}
                      className="bg-amber-600/20 hover:bg-amber-600/30 border border-amber-500/30 text-amber-400 text-xs px-3 py-1.5 rounded-lg flex items-center gap-1.5 disabled:opacity-50">
                      <Power size={13} /> Wymuś aktywację (DEV)
                    </button>
                  )}
                  {canActivate && (
                    <button
                      onClick={() => handleActivate(card.id)}
                      disabled={actionLoading === `activate-${card.id}`}
                      className="bg-emerald-600/20 hover:bg-emerald-600/30 border border-emerald-500/30 text-emerald-400 text-xs px-3 py-1.5 rounded-lg flex items-center gap-1.5 disabled:opacity-50">
                      <Power size={13} /> Aktywuj
                    </button>
                  )}
                  {card.providerToken && status === 'ACTIVE' && !isBlocked && (
                    <button
                      onClick={() => handleBlock(card.id)}
                      disabled={actionLoading === `block-${card.id}`}
                      className="bg-red-600/20 hover:bg-red-600/30 border border-red-500/30 text-red-400 text-xs px-3 py-1.5 rounded-lg flex items-center gap-1.5 disabled:opacity-50">
                      <Lock size={13} /> Zablokuj
                    </button>
                  )}
                  {card.providerToken && isBlocked && (
                    <button
                      onClick={() => handleUnblock(card.id)}
                      disabled={actionLoading === `unblock-${card.id}`}
                      className="bg-zinc-800 hover:bg-zinc-700 border border-zinc-700 text-zinc-300 text-xs px-3 py-1.5 rounded-lg flex items-center gap-1.5 disabled:opacity-50">
                      <Unlock size={13} /> Odblokuj
                    </button>
                  )}
                  {card.providerToken && (status === 'REQUESTED' || status === 'PRODUCING') && card.type === 'VIRTUAL' && (
                    <span className="text-xs text-zinc-500 italic">Karta wirtualna aktywuje się automatycznie (do 1h)</span>
                  )}
                  {card.providerToken && (status === 'REQUESTED' || status === 'PRODUCING') && card.type !== 'VIRTUAL' && (
                    <span className="text-xs text-zinc-500 italic">Karta w produkcji — czekaj na wysyłkę</span>
                  )}
                </div>
              </div>
            );
          })}
          {cards.length === 0 && (
            <p className="text-zinc-500 text-sm">Nie posiadasz jeszcze żadnej karty. Kliknij "Zamów kartę", aby ją utworzyć.</p>
          )}
        </div>

        {cards.length > 0 && (
          <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-6">
            <h2 className="text-lg font-semibold mb-4">Symulacja płatności kartą</h2>

            <form onSubmit={handlePay} className="grid gap-3 md:grid-cols-2">
              <select value={selected} onChange={e => setSelected(e.target.value)}
                className="bg-zinc-800 border border-zinc-700 text-white rounded-lg px-4 py-2.5 text-sm md:col-span-2">
                {cards.map(c => (
                  <option key={c.id} value={c.id}>{c.maskedPan || c.maskedNumber} ({c.type})</option>
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

      {orderModal && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-6 max-w-md w-full">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">Zamów nową kartę</h3>
              <button onClick={() => setOrderModal(false)} className="text-zinc-500 hover:text-white">
                <X size={20} />
              </button>
            </div>
            <p className="text-zinc-400 text-sm mb-5">Wybierz typ karty:</p>
            <div className="grid gap-3">
              <button
                onClick={() => handleOrder('VIRTUAL')}
                disabled={actionLoading === 'order'}
                className="bg-zinc-800 hover:bg-zinc-750 border border-zinc-700 rounded-xl p-4 text-left disabled:opacity-50">
                <p className="font-semibold text-white">Karta wirtualna</p>
                <p className="text-xs text-zinc-400 mt-1">Aktywna natychmiast (do 1h). Do zakupów online.</p>
              </button>
              <button
                onClick={() => handleOrder('PHYSICAL')}
                disabled={actionLoading === 'order'}
                className="bg-zinc-800 hover:bg-zinc-750 border border-zinc-700 rounded-xl p-4 text-left disabled:opacity-50">
                <p className="font-semibold text-white">Karta fizyczna</p>
                <p className="text-xs text-zinc-400 mt-1">Dostawa kurierem. Wymaga aktywacji po otrzymaniu.</p>
              </button>
            </div>
            {actionLoading === 'order' && (
              <p className="text-zinc-500 text-xs text-center mt-4">Komunikacja z providerem kart...</p>
            )}
          </div>
        </div>
      )}

      {issuedCard && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-6 max-w-md w-full">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-emerald-400">Karta wydana</h3>
              <button onClick={() => setIssuedCard(null)} className="text-zinc-500 hover:text-white">
                <X size={20} />
              </button>
            </div>
            <div className="bg-amber-500/10 border border-amber-500/30 text-amber-300 text-xs rounded-lg px-3 py-2 mb-4">
              <strong>Zapisz te dane teraz</strong> — pełny PAN i CVV nie zostaną pokazane ponownie.
            </div>
            <div className="space-y-3 text-sm">
              <Field label="Numer karty" value={issuedCard.fullPan} mono onCopy={copyToClipboard} />
              <Field label="CVV" value={issuedCard.cvv} mono onCopy={copyToClipboard} />
              <Field
                label="Data ważności"
                value={`${String(issuedCard.expiryMonth).padStart(2, '0')}/${issuedCard.expiryYear}`}
                mono
                onCopy={copyToClipboard}
              />
              <div className="grid grid-cols-2 gap-3 pt-2">
                <div className="bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2">
                  <p className="text-zinc-500 text-xs mb-1">Typ</p>
                  <p className="text-zinc-200 text-sm">{issuedCard.cardType}</p>
                </div>
                <div className="bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2">
                  <p className="text-zinc-500 text-xs mb-1">Status</p>
                  <p className="text-zinc-200 text-sm">{STATUS_LABELS[issuedCard.providerStatus] || issuedCard.providerStatus}</p>
                </div>
              </div>
            </div>
            <button
              onClick={() => setIssuedCard(null)}
              className="w-full mt-5 bg-blue-600 hover:bg-blue-500 text-white py-2.5 rounded-lg text-sm font-medium">
              Zapisałem/am dane — zamknij
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

const Field: React.FC<{ label: string; value: string; mono?: boolean; onCopy: (v: string) => void }> =
  ({ label, value, mono, onCopy }) => (
    <div className="bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2 flex items-center justify-between">
      <div className="min-w-0">
        <p className="text-zinc-500 text-xs mb-1">{label}</p>
        <p className={`text-zinc-100 ${mono ? 'font-mono' : ''} truncate`}>{value}</p>
      </div>
      <button onClick={() => onCopy(value)} className="text-zinc-500 hover:text-white p-1 ml-2 flex-shrink-0" title="Kopiuj">
        <Copy size={14} />
      </button>
    </div>
  );

export default CardsPage;