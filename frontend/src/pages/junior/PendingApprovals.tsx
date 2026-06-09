import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertCircle, CheckCircle2, CreditCard, ArrowRight, Check, X } from 'lucide-react';
import { juniorService } from '../../services/juniorService';
import type { PendingApproval } from '../../services/juniorService';
import { useAuth } from '../../contexts/AuthContext';

const PendingApprovals: React.FC = () => {
  const navigate = useNavigate();
  const {user, logout} = useAuth();
  const [items, setItems] = useState<PendingApproval[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [processingId, setProcessingId] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    try {
      const data = await juniorService.pendingList();
      setItems(data);
    } catch (err: any) {
      setError(err.response?.data?.detail || err.response?.data?.message || 'Nie udało się pobrać listy wniosków.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handle = async (id: string, action: 'approve' | 'reject') => {
    setError(null);
    setSuccess(null);
    setProcessingId(id);
    try {
      const fn = action === 'approve' ? juniorService.approve : juniorService.reject;
      const res = await fn(id);
      setSuccess(res.message);
      await load();
    } catch (err: any) {
      setError(err.response?.data?.detail || err.response?.data?.message || 'Operacja nie powiodła się.');
    } finally {
      setProcessingId(null);
    }
  };

  const fmt = (v: number, c: string) =>
    new Intl.NumberFormat('pl-PL', { style: 'currency', currency: c }).format(v);

  const fmtDate = (iso: string) => new Date(iso).toLocaleString('pl-PL');

  const txTypeLabel = (t: string | null) => {
    switch (t) {
      case 'INTERNAL': return 'Przelew wewnętrzny';
      case 'CARD_PAYMENT': return 'Płatność kartą';
      default: return t || '—';
    }
  };

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
         <span className="text-zinc-500 text-sm hidden md:inline">/ Wnioski do zatwierdzenia</span>
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

      <main className="flex-grow p-4 md:p-8 max-w-4xl mx-auto w-full">
        <header className="mb-6">
          <h1 className="text-3xl font-bold text-zinc-100">Wnioski do zatwierdzenia</h1>
          <p className="text-zinc-500 text-sm mt-1">
            Transakcje zainicjowane przez Twoje dzieci czekają na Twoją decyzję.
          </p>
        </header>

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

        {loading ? (
          <div className="flex justify-center py-20">
            <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-blue-500"></div>
          </div>
        ) : items.length === 0 ? (
          <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-12 text-center">
            <CheckCircle2 className="text-emerald-400 mx-auto mb-3" size={32} />
            <p className="text-zinc-300 font-medium">Brak wniosków do zatwierdzenia</p>
            <p className="text-zinc-500 text-sm mt-1">Wszystkie transakcje dzieci zostały już rozpatrzone.</p>
          </div>
        ) : (
          <div className="space-y-3">
            {items.map(item => (
              <div key={item.id} className="bg-zinc-900 border border-zinc-800 rounded-2xl p-5">
                <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                  <div className="flex-grow">
                    <div className="flex items-center gap-3 mb-2">
                      <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-md text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                        {item.juniorFirstName} {item.juniorLastName}
                      </span>
                      <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-md text-xs font-medium bg-zinc-800 text-zinc-400 border border-zinc-700">
                        {item.transactionType === 'CARD_PAYMENT' && <CreditCard size={12} />}
                        {txTypeLabel(item.transactionType)}
                      </span>
                    </div>
                    <p className="text-zinc-300 font-medium">{item.description}</p>
                    {item.transactionType === 'INTERNAL' && item.receiverAccountNumber && (
                      <p className="text-zinc-500 text-xs mt-1 font-mono">
                        <ArrowRight size={10} className="inline" /> {item.receiverAccountNumber}
                      </p>
                    )}
                    {item.transactionType === 'CARD_PAYMENT' && item.receiverName && (
                      <p className="text-zinc-500 text-xs mt-1">
                        u <span className="text-zinc-400">{item.receiverName}</span>
                      </p>
                    )}
                    <p className="text-zinc-600 text-xs mt-2">{fmtDate(item.createdAt)}</p>
                  </div>

                  <div className="text-right">
                    <p className="text-2xl font-bold text-white">{fmt(item.amount, item.currency)}</p>
                  </div>
                </div>

                <div className="flex gap-2 mt-4 pt-4 border-t border-zinc-800">
                  <button
                    type="button"
                    onClick={() => handle(item.id, 'reject')}
                    disabled={processingId === item.id}
                    className="flex-1 bg-zinc-800 hover:bg-red-500/20 hover:text-red-400 text-zinc-300 py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
                  >
                    <X size={16} /> Odrzuć
                  </button>
                  <button
                    type="button"
                    onClick={() => handle(item.id, 'approve')}
                    disabled={processingId === item.id}
                    className="flex-1 bg-blue-600 hover:bg-blue-500 disabled:bg-blue-600/50 text-white py-2 rounded-lg text-sm font-medium transition-colors flex items-center justify-center gap-2"
                  >
                    {processingId === item.id ? (
                      <div className="animate-spin rounded-full h-4 w-4 border-t-2 border-b-2 border-white"></div>
                    ) : (
                      <><Check size={16} /> Zatwierdź</>
                    )}
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
};

export default PendingApprovals;