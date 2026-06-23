import { useEffect, useState } from 'react';
import { useParams, useSearchParams, useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import { useAuth } from '../contexts/AuthContext';

interface Transaction {
  id: string;
  senderAccountNumber: string;
  receiverAccountNumber: string;
  receiverName: string | null;
  title: string | null;
  amount: number;
  currency: string;
  status: string;
  type: string;
  createdAt: string;
  executionDate: string | null;
  direction: 'INCOMING' | 'OUTGOING';
}

const STATUS_CONFIG: Record<string, { label: string; classes: string }> = {
  COMPLETED:        { label: 'Zrealizowany',                   classes: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' },
  PENDING:          { label: 'W trakcie',                      classes: 'bg-amber-500/10 text-amber-400 border-amber-500/20' },
  QUEUED:           { label: 'W kolejce',                      classes: 'bg-blue-500/10 text-blue-400 border-blue-500/20' },
  PENDING_APPROVAL: { label: 'Oczekuje na akceptację rodzica', classes: 'bg-orange-500/10 text-orange-400 border-orange-500/20' },
  HELD_FOR_AML:     { label: 'Wstrzymana (AML)',               classes: 'bg-red-500/10 text-red-400 border-red-500/20' },
  REJECTED_AML:     { label: 'Odrzucona (AML)',                classes: 'bg-red-500/10 text-red-400 border-red-500/20' },
  REJECTED:         { label: 'Odrzucona',                      classes: 'bg-red-500/10 text-red-400 border-red-500/20' },
  SENT:             { label: 'Wysłany do SWIFT',               classes: 'bg-blue-500/10 text-blue-400 border-blue-500/20' },
  IN_TRANSIT:       { label: 'W drodze (SWIFT)',               classes: 'bg-blue-500/10 text-blue-400 border-blue-500/20' },
  DELIVERED:        { label: 'Dostarczony (SWIFT)',            classes: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' },
  RETURNED:         { label: 'Zwrócony',                       classes: 'bg-red-500/10 text-red-400 border-red-500/20' },
};

function StatusBadge({ status }: { status: string }) {
  const config = STATUS_CONFIG[status] ?? { label: status, classes: 'bg-zinc-500/10 text-zinc-400 border-zinc-500/20' };
  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium border ${config.classes}`}>
      {config.label}
    </span>
  );
}

function formatDate(dateStr: string | null): string {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleString('pl-PL', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

function formatAmount(amount: number, currency: string, direction: string): string {
  const formatted = new Intl.NumberFormat('pl-PL', { style: 'currency', currency }).format(amount);
  return direction === 'INCOMING' ? `+${formatted}` : `-${formatted}`;
}

function mapExternalStatus(s: string): string {
  switch (s) {
    case 'PROCESSED': return 'COMPLETED';
    case 'SENT':
    case 'INITIATED':
    case 'GRIDLOCK_HELD': return 'PENDING';
    case 'REJECTED': return 'REJECTED';
    default: return s;
  }
}

export default function TransactionHistory() {
  const { accountId } = useParams<{ accountId: string }>();
  const navigate = useNavigate();
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { user, logout } = useAuth();
  const [params] = useSearchParams();
  const handleBack = () => {
    if (params.get('from') === 'junior') navigate('/dashboard?tab=junior');
    else navigate(-1);
  };


useEffect(() => {
  const fetchAll = async () => {
    try {
      const internalRes = await api.get(`/api/transactions/history/${accountId}`);

      let external: Transaction[] = [];
      try {
        const externalRes = await api.get(`/api/transfers/external/account/${accountId}`);
        external = (externalRes.data || []).map((t: {
          id: string;
          senderAccountNumber: string;
          receiverAccountNumber: string;
          receiverName: string | null;
          title: string | null;
          amount: number;
          currency: string;
          status: string;
          routingSystem: string;
          createdAt: string;
          settledAt: string | null;
        }) => ({
          id: t.id,
          senderAccountNumber: t.senderAccountNumber,
          receiverAccountNumber: t.receiverAccountNumber,
          receiverName: t.receiverName,
          title: t.title,
          amount: t.amount,
          currency: t.currency,
          status: mapExternalStatus(t.status),
          type: t.routingSystem,
          createdAt: t.createdAt,
          executionDate: t.settledAt,
          direction: 'OUTGOING' as const,
        }));
      } catch { }

      const all = [...(internalRes.data || []), ...external].sort(
        (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      );
      setTransactions(all);
    } catch {
      setError('Nie udało się pobrać historii transakcji.');
    } finally {
      setLoading(false);
    }
  };
  fetchAll();
}, [accountId]);

  return (
    <div className="min-h-screen bg-zinc-950 text-white">
      <nav className="bg-zinc-900/80 backdrop-blur-md border-b border-zinc-800 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
            <div className="flex items-center gap-4">
                <button
                    onClick={handleBack}
                    className="text-zinc-400 hover:text-white transition-colors text-sm"
                >
                ← Wróć
                </button>
                <div className="h-5 w-px bg-zinc-700" />
                    <span className="text-xl font-bold tracking-tight text-white">
                        <span className="text-blue-400">Bankly</span>
                    </span>
                    <span className="text-zinc-500 text-sm hidden md:block">/ Historia transakcji</span>
                </div>

            <div className="flex items-center gap-6">
                <div className="hidden md:flex flex-col text-right">
                    <span className="text-zinc-200 text-sm font-medium">{user?.email}</span>
                    <span className="text-zinc-500 text-xs mt-0.5">
                    Numer klienta: <span className="text-zinc-400">{user?.customerNumber}</span>
                    </span>
                </div>
            <div className="h-8 w-px bg-zinc-800 hidden md:block" />
        <button
            onClick={logout}
            className="text-zinc-400 hover:text-white px-4 py-2 rounded-lg font-medium transition-colors duration-150"
        >
        Wyloguj się
        </button>
        </div>
    </div>
    </nav>

      <main className="max-w-4xl mx-auto px-6 py-8">
        {loading && (
          <div className="flex justify-center py-20">
            <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-blue-500" />
          </div>
        )}

        {error && (
          <div className="bg-red-950/30 border border-red-900/50 text-red-400 p-4 rounded-xl">
            {error}
          </div>
        )}

        {!loading && !error && transactions.length === 0 && (
          <div className="text-center py-20 text-zinc-500">Brak transakcji na tym koncie.</div>
        )}

        {!loading && !error && transactions.length > 0 && (
          <div className="space-y-2">
            {transactions.map(tx => (
              <div key={tx.id} className="bg-zinc-900 border border-zinc-800 rounded-xl px-5 py-4 flex items-center gap-4 hover:border-zinc-700 transition-colors">

                {/* Kierunek */}
                <div className={`w-10 h-10 rounded-full flex items-center justify-center shrink-0 ${
                  tx.direction === 'INCOMING'
                    ? 'bg-emerald-500/10 text-emerald-400'
                    : 'bg-zinc-800 text-zinc-400'
                }`}>
                  {tx.direction === 'INCOMING' ? '↓' : '↑'}
                </div>

                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-zinc-100 truncate">
                    {tx.title ?? (tx.direction === 'OUTGOING' ? (tx.receiverName ?? tx.receiverAccountNumber) : tx.senderAccountNumber)}
                  </p>
                  <p className="text-xs text-zinc-500 mt-0.5 truncate">
                    {tx.direction === 'OUTGOING'
                      ? `→ ${tx.receiverName ?? tx.receiverAccountNumber}`
                      : `← ${tx.senderAccountNumber}`}
                  </p>

                  {/* Baner dla PENDING_APPROVAL */}
                  {tx.status === 'PENDING_APPROVAL' && (
                    <p className="text-xs text-orange-400 mt-1">
                      ⏳ Transakcja czeka na zatwierdzenie przez rodzica
                    </p>
                  )}
                </div>

                <div className="text-right shrink-0 space-y-1">
                  <p className={`text-sm font-semibold ${
                    tx.direction === 'INCOMING' ? 'text-emerald-400' : 'text-zinc-100'
                  }`}>
                    {formatAmount(tx.amount, tx.currency, tx.direction)}
                  </p>
                  <StatusBadge status={tx.status} />
                  <p className="text-xs text-zinc-600">
                    {formatDate(tx.executionDate ?? tx.createdAt)}
                  </p>
                </div>

              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}