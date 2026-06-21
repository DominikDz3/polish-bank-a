import { useEffect, useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { amlService, type AmlHoldView } from '../services/amlService';

const STATUS_CONFIG: Record<string, { label: string; classes: string }> = {
  AWAITING_EXPLANATION: { label: 'Wymagane wyjaśnienia', classes: 'bg-amber-500/10 text-amber-400 border-amber-500/20' },
  AWAITING_DECISION: { label: 'Oczekuje na decyzję banku', classes: 'bg-blue-500/10 text-blue-400 border-blue-500/20' },
  APPROVED: { label: 'Zatwierdzona', classes: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' },
  REJECTED: { label: 'Odrzucona', classes: 'bg-red-500/10 text-red-400 border-red-500/20' },
};

const TYPE_LABEL: Record<string, string> = {
  INTERNAL: 'Przelew wewnętrzny',
  EXTERNAL: 'Przelew krajowy',
  SWIFT: 'Przelew zagraniczny (SWIFT)',
  KLIK_P2P: 'KLIK na telefon',
};

export default function AmlHolds() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [holds, setHolds] = useState<AmlHoldView[]>([]);
  const [loading, setLoading] = useState(true);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [explanationText, setExplanationText] = useState('');
  const [submittingId, setSubmittingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    try { setHolds(await amlService.myHolds()); }
    catch { setError('Nie udało się pobrać listy wstrzymanych transakcji.'); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  const submit = async (id: string) => {
    if (explanationText.trim().length < 10) {
      setError('Wyjaśnienie musi mieć co najmniej 10 znaków.');
      return;
    }
    setSubmittingId(id);
    try {
      await amlService.submitExplanation(id, explanationText);
      setExplanationText(''); setExpandedId(null); setError(null);
      await load();
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        const data = err.response?.data as { detail?: string } | undefined;
        setError(data?.detail || 'Nie udało się wysłać wyjaśnień.');
      }
    } finally { setSubmittingId(null); }
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
            <span className="text-zinc-500 text-sm hidden md:inline">/ Wstrzymane transakcje</span>
          </div>
          <div className="flex items-center gap-6">
            <span className="text-zinc-200 text-sm hidden md:inline">{user?.email}</span>
            <button onClick={logout} className="text-zinc-400 hover:text-white px-4 py-2 rounded-lg">Wyloguj się</button>
          </div>
        </div>
      </nav>

      <main className="flex-grow p-4 md:p-8 max-w-4xl mx-auto w-full">
        <h1 className="text-2xl font-bold mb-2">Wstrzymane transakcje</h1>
        <p className="text-zinc-500 text-sm mb-6">Transakcje wstrzymane przez system AML wymagają wyjaśnień przed rozpatrzeniem przez bank.</p>

        {error && <div className="bg-red-500/10 border border-red-500/20 text-red-400 p-4 rounded-xl mb-4">{error}</div>}

        {loading ? (
          <div className="flex justify-center py-20">
            <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-blue-500"></div>
          </div>
        ) : holds.length === 0 ? (
          <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-8 text-center text-zinc-500">
            Nie masz żadnych wstrzymanych transakcji.
          </div>
        ) : (
          <div className="space-y-4">
            {holds.map(h => {
              const cfg = STATUS_CONFIG[h.status] ?? { label: h.status, classes: 'bg-zinc-500/10 text-zinc-400 border-zinc-500/20' };
              const canExplain = h.status === 'AWAITING_EXPLANATION' || h.status === 'AWAITING_DECISION';
              const isExpanded = expandedId === h.id;
              return (
                <div key={h.id} className="bg-zinc-900 border border-zinc-800 rounded-2xl p-6">
                  <div className="flex justify-between items-start mb-4">
                    <div>
                      <p className="text-zinc-400 text-xs uppercase tracking-wider">{TYPE_LABEL[h.holdType] ?? h.holdType}</p>
                      <p className="text-2xl font-bold text-white mt-1">{formatCurrency(h.amount, h.currency)}</p>
                      <p className="text-xs text-zinc-500 font-mono mt-1">{h.receiverInfo}</p>
                    </div>
                    <span className={`inline-flex items-center px-3 py-1 rounded-md text-xs font-medium border ${cfg.classes}`}>{cfg.label}</span>
                  </div>

                  <div className="bg-amber-500/5 border border-amber-500/20 rounded-xl p-3 mb-4">
                    <p className="text-xs text-amber-300 uppercase tracking-wider mb-1">Powód wstrzymania</p>
                    <p className="text-sm text-amber-100">{h.reason}</p>
                  </div>

                  {h.clientExplanation && (
                    <div className="bg-zinc-950 border border-zinc-800 rounded-xl p-3 mb-4">
                      <p className="text-xs text-zinc-500 uppercase tracking-wider mb-1">Twoje wyjaśnienia</p>
                      <p className="text-sm text-zinc-300 whitespace-pre-wrap">{h.clientExplanation}</p>
                    </div>
                  )}

                  {h.decisionNote && (
                    <div className="bg-zinc-950 border border-zinc-800 rounded-xl p-3 mb-4">
                      <p className="text-xs text-zinc-500 uppercase tracking-wider mb-1">Decyzja banku</p>
                      <p className="text-sm text-zinc-300">{h.decisionNote}</p>
                    </div>
                  )}

                  <div className="flex justify-between items-center text-xs text-zinc-500 mt-3">
                    <span>Utworzono: {formatDate(h.createdAt)}</span>
                    {h.decisionAt && <span>Decyzja: {formatDate(h.decisionAt)}</span>}
                  </div>

                  {canExplain && !isExpanded && (
                    <button onClick={() => { setExpandedId(h.id); setExplanationText(h.clientExplanation ?? ''); }}
                      className="w-full mt-4 bg-blue-600 hover:bg-blue-500 text-white font-medium py-2.5 rounded-lg">
                      {h.clientExplanation ? 'Edytuj wyjaśnienia' : 'Złóż wyjaśnienia'}
                    </button>
                  )}

                  {canExplain && isExpanded && (
                    <div className="mt-4 space-y-3">
                      <textarea value={explanationText} onChange={(e) => setExplanationText(e.target.value)}
                        rows={5} maxLength={2000}
                        placeholder="Opisz cel transakcji, źródło środków, beneficjenta..."
                        className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-white text-sm" />
                      <div className="flex gap-3">
                        <button onClick={() => { setExpandedId(null); setExplanationText(''); }}
                          className="flex-1 bg-zinc-800 hover:bg-zinc-700 py-2.5 rounded-lg text-sm">Anuluj</button>
                        <button onClick={() => submit(h.id)} disabled={submittingId === h.id}
                          className="flex-1 bg-blue-600 hover:bg-blue-500 disabled:opacity-50 py-2.5 rounded-lg text-sm font-medium">
                          {submittingId === h.id ? 'Wysyłam...' : 'Wyślij wyjaśnienia'}
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </main>
    </div>
  );
}