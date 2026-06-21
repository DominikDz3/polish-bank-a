import { useEffect, useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { amlService, type AmlHoldView } from '../services/amlService';

const STATUS_CONFIG: Record<string, { label: string; classes: string }> = {
  AWAITING_EXPLANATION: { label: 'Wymaga wyjaśnień', classes: 'bg-amber-500/10 text-amber-400 border-amber-500/20' },
  AWAITING_DECISION: { label: 'Oczekuje decyzji', classes: 'bg-blue-500/10 text-blue-400 border-blue-500/20' },
  APPROVED: { label: 'Zatwierdzona', classes: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' },
  REJECTED: { label: 'Odrzucona', classes: 'bg-red-500/10 text-red-400 border-red-500/20' },
};

const TYPE_LABEL: Record<string, string> = {
  INTERNAL: 'Wewnętrzny',
  EXTERNAL: 'Krajowy',
  SWIFT: 'SWIFT',
  KLIK_P2P: 'KLIK P2P',
};

type Filter = 'PENDING' | 'ALL';

export default function AmlAdmin() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [filter, setFilter] = useState<Filter>('PENDING');
  const [holds, setHolds] = useState<AmlHoldView[]>([]);
  const [loading, setLoading] = useState(true);
  const [decisionFor, setDecisionFor] = useState<{ id: string; mode: 'approve' | 'reject' } | null>(null);
  const [decisionNote, setDecisionNote] = useState('');
  const [processing, setProcessing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    try { setHolds(filter === 'PENDING' ? await amlService.adminPending() : await amlService.adminAll()); }
    catch { setError('Nie udało się pobrać listy.'); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, [filter]);

  const submitDecision = async () => {
    if (!decisionFor) return;
    setProcessing(true);
    try {
      if (decisionFor.mode === 'approve') await amlService.approve(decisionFor.id, decisionNote);
      else await amlService.reject(decisionFor.id, decisionNote);
      setDecisionFor(null); setDecisionNote('');
      await load();
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        const data = err.response?.data as { detail?: string } | undefined;
        setError(data?.detail || 'Operacja nieudana.');
      }
    } finally { setProcessing(false); }
  };

  const formatCurrency = (v: number, c: string) =>
    new Intl.NumberFormat('pl-PL', { style: 'currency', currency: c }).format(v);
  const formatDate = (s: string | null) =>
    s ? new Date(s).toLocaleString('pl-PL', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '—';

  return (
    <div className="min-h-screen bg-zinc-950 text-white font-sans flex flex-col">
      <nav className="bg-zinc-900/80 backdrop-blur-md border-b border-zinc-800 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button onClick={() => navigate(-1)} className="text-zinc-400 hover:text-white">← Wróć</button>
            <div className="h-6 w-px bg-zinc-800"></div>
            <div className="text-xl font-bold"><span className="text-blue-400">Bankly</span></div>
            <span className="text-blue-400 text-sm font-semibold hidden md:inline">/ Panel AML</span>
          </div>
          <div className="flex items-center gap-6">
            <span className="text-zinc-200 text-sm hidden md:inline">{user?.email}</span>
            <button onClick={logout} className="text-zinc-400 hover:text-white px-4 py-2 rounded-lg">Wyloguj się</button>
          </div>
        </div>
      </nav>

      <main className="flex-grow p-4 md:p-8 max-w-6xl mx-auto w-full">
        <div className="flex justify-between items-center mb-6">
          <div>
            <h1 className="text-2xl font-bold">Panel AML — wstrzymane transakcje</h1>
            <p className="text-zinc-500 text-sm mt-1">Rozpatrz transakcje wstrzymane przez silnik AML.</p>
          </div>
          <div className="bg-zinc-900 border border-zinc-800 rounded-xl p-1 flex gap-1">
            <button onClick={() => setFilter('PENDING')}
              className={`px-4 py-2 rounded-lg text-sm font-medium ${filter === 'PENDING' ? 'bg-blue-600 text-white' : 'text-zinc-400 hover:text-white'}`}>
              Oczekujące
            </button>
            <button onClick={() => setFilter('ALL')}
              className={`px-4 py-2 rounded-lg text-sm font-medium ${filter === 'ALL' ? 'bg-blue-600 text-white' : 'text-zinc-400 hover:text-white'}`}>
              Wszystkie
            </button>
          </div>
        </div>

        {error && <div className="bg-red-500/10 border border-red-500/20 text-red-400 p-4 rounded-xl mb-4">{error}</div>}

        {loading ? (
          <div className="flex justify-center py-20">
            <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-blue-500"></div>
          </div>
        ) : holds.length === 0 ? (
          <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-8 text-center text-zinc-500">
            {filter === 'PENDING' ? 'Brak transakcji oczekujących na decyzję.' : 'Brak transakcji.'}
          </div>
        ) : (
          <div className="space-y-4">
            {holds.map(h => {
              const cfg = STATUS_CONFIG[h.status] ?? { label: h.status, classes: 'bg-zinc-500/10 text-zinc-400 border-zinc-500/20' };
              const canDecide = h.status === 'AWAITING_EXPLANATION' || h.status === 'AWAITING_DECISION';
              return (
                <div key={h.id} className="bg-zinc-900 border border-zinc-800 rounded-2xl p-6">
                  <div className="grid md:grid-cols-2 gap-4">
                    <div>
                      <div className="flex items-center gap-2 mb-2 flex-wrap">
                        <span className="bg-zinc-800 text-zinc-300 px-2 py-0.5 rounded text-xs">{TYPE_LABEL[h.holdType] ?? h.holdType}</span>
                        <span className="bg-zinc-800 text-zinc-300 px-2 py-0.5 rounded text-xs font-mono">{h.triggeredRule}</span>
                        <span className={`inline-flex items-center px-3 py-1 rounded-md text-xs font-medium border ${cfg.classes}`}>{cfg.label}</span>
                      </div>
                      <p className="text-3xl font-bold text-white">{formatCurrency(h.amount, h.currency)}</p>
                      <p className="text-xs text-zinc-500 font-mono mt-1">{h.receiverInfo}</p>
                      <p className="text-xs text-zinc-500 mt-2">Klient: <span className="text-zinc-300">{h.clientEmail}</span></p>
                      <p className="text-xs text-zinc-500">Utworzono: {formatDate(h.createdAt)}</p>
                      {h.decisionAt && <p className="text-xs text-zinc-500">Decyzja: {formatDate(h.decisionAt)}</p>}
                    </div>
                    <div className="space-y-3">
                      <div className="bg-amber-500/5 border border-amber-500/20 rounded-xl p-3">
                        <p className="text-xs text-amber-300 uppercase tracking-wider mb-1">Powód</p>
                        <p className="text-sm text-amber-100">{h.reason}</p>
                      </div>
                      {h.clientExplanation ? (
                        <div className="bg-zinc-950 border border-zinc-800 rounded-xl p-3">
                          <p className="text-xs text-zinc-500 uppercase tracking-wider mb-1">Wyjaśnienia klienta</p>
                          <p className="text-sm text-zinc-300 whitespace-pre-wrap">{h.clientExplanation}</p>
                        </div>
                      ) : (
                        <div className="bg-zinc-950 border border-zinc-800 rounded-xl p-3">
                          <p className="text-xs text-zinc-500 italic">Klient jeszcze nie złożył wyjaśnień.</p>
                        </div>
                      )}
                      {h.decisionNote && (
                        <div className="bg-zinc-950 border border-zinc-800 rounded-xl p-3">
                          <p className="text-xs text-zinc-500 uppercase tracking-wider mb-1">Notatka banku</p>
                          <p className="text-sm text-zinc-300">{h.decisionNote}</p>
                        </div>
                      )}
                    </div>
                  </div>
                  {canDecide && (
                    <div className="flex gap-3 mt-4">
                      <button onClick={() => { setDecisionFor({ id: h.id, mode: 'reject' }); setDecisionNote(''); }}
                        className="flex-1 bg-red-600/20 hover:bg-red-600/30 border border-red-500/40 text-red-400 font-medium py-2.5 rounded-lg">
                        Odrzuć
                      </button>
                      <button onClick={() => { setDecisionFor({ id: h.id, mode: 'approve' }); setDecisionNote(''); }}
                        className="flex-1 bg-emerald-600/20 hover:bg-emerald-600/30 border border-emerald-500/40 text-emerald-400 font-medium py-2.5 rounded-lg">
                        Zatwierdź
                      </button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </main>

      {decisionFor && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center px-4">
          <div className="w-full max-w-md bg-zinc-900 border border-zinc-800 rounded-2xl p-6 shadow-2xl">
            <h3 className="text-lg font-semibold mb-1">
              {decisionFor.mode === 'approve' ? 'Zatwierdź transakcję' : 'Odrzuć transakcję'}
            </h3>
            <p className="text-zinc-500 text-sm mb-4">
              {decisionFor.mode === 'approve'
                ? 'Środki zostaną zaksięgowane u odbiorcy / wysłane do systemu zewnętrznego.'
                : 'Środki zostaną zwrócone na konto nadawcy.'}
            </p>
            <textarea value={decisionNote} onChange={(e) => setDecisionNote(e.target.value)}
              rows={3} maxLength={500}
              placeholder="Notatka (opcjonalna, widoczna dla klienta)..."
              className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-white text-sm mb-4" />
            <div className="flex gap-3">
              <button onClick={() => setDecisionFor(null)} disabled={processing}
                className="flex-1 bg-zinc-800 hover:bg-zinc-700 py-2.5 rounded-lg text-sm disabled:opacity-50">Anuluj</button>
              <button onClick={submitDecision} disabled={processing}
                className={`flex-1 py-2.5 rounded-lg text-sm font-medium disabled:opacity-50
                  ${decisionFor.mode === 'approve' ? 'bg-emerald-600 hover:bg-emerald-500 text-white' : 'bg-red-600 hover:bg-red-500 text-white'}`}>
                {processing ? '...' : (decisionFor.mode === 'approve' ? 'Zatwierdź' : 'Odrzuć')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}