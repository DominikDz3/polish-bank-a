import { useEffect, useState } from 'react';
import axios from 'axios';
import { useAuth } from '../contexts/AuthContext';
import { api } from '../services/api';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { AlertCircle, Settings } from 'lucide-react';
import { ROUTES } from '../constants/routes';

interface AccountSummary {
  id: string;
  accountNumber: string;
  balance: number;
  blockedFunds: number;
  currency: string;
  type: string;
}

interface JuniorAccountSummary extends AccountSummary {
  juniorFirstName: string;
  juniorLastName: string;
}

interface JuniorApiItem {
  accountId: string;
  accountNumber: string;
  balance: number;
  currency: string;
  firstName: string;
  lastName: string;
}

type TabType = 'PERSONAL' | 'JUNIOR';

export default function Dashboard() {
  const { user, logout } = useAuth();

  const navigate = useNavigate();

  const [accounts, setAccounts] = useState<AccountSummary[]>([]);
  const [parentJuniors, setParentJuniors] = useState<JuniorAccountSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab: TabType = (searchParams.get('tab') === 'junior') ? 'JUNIOR' : 'PERSONAL';

  const [pendingApprovalsCount, setPendingApprovalsCount] = useState(0);
  const [amlCount, setAmlCount] = useState(0);

  const setActiveTab = (tab: TabType) => {
    setSearchParams(tab === 'JUNIOR' ? { tab: 'junior' } : {});
  };

  useEffect(() => {
    const fetchAccounts = async () => {
      try {
        const response = await api.get('/api/accounts');
        setAccounts(response.data);
        try {
          const juniorRes = await api.get('/api/junior');
          const mapped: JuniorAccountSummary[] = juniorRes.data.map((j: JuniorApiItem) => ({
            id: j.accountId,
            accountNumber: j.accountNumber,
            balance: j.balance,
            blockedFunds: 0,
            currency: j.currency,
            type: 'JUNIOR',
            juniorFirstName: j.firstName,
            juniorLastName: j.lastName,
          }));
          setParentJuniors(mapped);
          try {
            const c = await api.get('/api/junior/pending-approvals/count');
            setPendingApprovalsCount(c.data.count || 0);
          } catch { /* nieważne jeśli się nie uda */ }
        } catch { /* user nie jest rodzicem, ignorujemy */ }
      } catch (err: unknown) {
        if (axios.isAxiosError(err)) {
          setError(err.response?.data?.message || "Nie udało się pobrać danych konta.");
        } else {
          setError("Nie udało się pobrać danych konta.");
        }
      } finally {
        setLoading(false);
      }
    };
    fetchAccounts();
  }, []);

  useEffect(() => {
    api.get('/api/aml/holds/count')
      .then(r => setAmlCount(r.data.count || 0))
      .catch(() => { /* ignoruj */ });
  }, []);

  const formatCurrency = (amount: number, currency: string) => {
    return new Intl.NumberFormat('pl-PL', {
      style: 'currency',
      currency: currency,
    }).format(amount);
  };

  const formatAccountNumber = (accNumber: string) => {
    const cleanNumber = accNumber.replace(/\s+/g, '');
    const countryCode = cleanNumber.substring(0, 2);
    const controlDigits = cleanNumber.substring(2, 4);
    const rest = cleanNumber.substring(4).match(/.{1,4}/g)?.join(' ') || '';
    return `${countryCode}${controlDigits} ${rest}`;
  };

  const personalAccounts = accounts.filter(acc => acc.type !== 'JUNIOR');

  const isJunior =
    user?.role?.toUpperCase() === 'JUNIOR' ||
    user?.role?.toUpperCase() === 'ROLE_JUNIOR' ||
    (accounts.length > 0 && accounts.every(acc => acc.type === 'JUNIOR'));

  const juniorAccounts: AccountSummary[] = isJunior
    ? accounts.filter(acc => acc.type === 'JUNIOR')
    : parentJuniors;

  const renderAccountCard = (account: AccountSummary, isFullWidth: boolean = false) => (
    <div key={account.id} className={`bg-zinc-900 border border-zinc-800 rounded-2xl p-6 hover:border-zinc-700 transition-all group relative overflow-hidden flex flex-col justify-between min-h-[240px] ${isFullWidth ? 'md:col-span-2' : ''}`}>

      <div className="absolute top-0 right-0 p-4 opacity-[0.02] group-hover:opacity-[0.05] transition-opacity pointer-events-none">
        <svg className="w-32 h-32 text-white transform translate-x-4 -translate-y-4" fill="currentColor" viewBox="0 0 24 24">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.31-8.86c-1.77-.45-2.34-.94-2.34-1.67 0-.84.79-1.43 2.1-1.43 1.38 0 1.9.66 1.94 1.64h1.71c-.05-1.34-.87-2.57-2.49-2.97V5H10.9v1.69c-1.51.32-2.72 1.3-2.72 2.81 0 1.79 1.49 2.69 3.66 3.21 1.95.46 2.34 1.15 2.34 1.87 0 .53-.39 1.64-2.25 1.64-1.74 0-2.24-.97-2.35-1.81H7.84c.14 1.7 1.59 2.99 3.06 3.37V20h2.8v-1.65c1.83-.35 3.02-1.48 3.02-3.1 0-2.1-1.64-2.81-4.41-3.41z"/>
        </svg>
      </div>

      <div className="z-10">
        <span className="inline-flex items-center px-2.5 py-1 rounded-md text-xs font-medium bg-blue-500/10 text-blue-400 border border-blue-500/20 mb-4">
          {account.type === 'STANDARD' ? 'Konto Osobiste' : account.type === 'JUNIOR' ? 'Konto Junior' : account.type}
        </span>
        <h2 className="text-3xl font-bold text-white tracking-tight">
          {formatCurrency(account.balance, account.currency)}
        </h2>
        {account.blockedFunds > 0 && (
          <p className="text-zinc-500 text-xs mt-1 font-medium">
            Blokady: <span className="text-zinc-400">-{formatCurrency(account.blockedFunds, account.currency)}</span>
          </p>
        )}
      </div>
      <div className="mt-6 flex flex-col gap-4 z-10 border-t border-zinc-800/50 pt-4">
        <div className="flex justify-between items-center bg-zinc-950/50 p-2 rounded-lg border border-zinc-800/50">
          <div className="flex flex-col">
            <span className="text-zinc-600 text-[10px] uppercase tracking-wider">Numer rachunku</span>
            <span className="text-zinc-300 font-mono text-xs">{formatAccountNumber(account.accountNumber)}</span>
          </div>
          <button onClick={() => navigator.clipboard.writeText(account.accountNumber)} className="text-zinc-500 hover:text-blue-400 p-2 rounded-md hover:bg-zinc-800" title="Skopiuj numer">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
            </svg>
          </button>
        </div>
        <div className="flex gap-2">
          <button
            className="flex-1 bg-zinc-800 text-zinc-300 py-1.5 rounded-lg text-sm font-medium hover:bg-zinc-700 transition-colors"
            onClick={() => navigate(`/history/${account.id}`)}>Historia
          </button>
          <button className="flex-1 bg-zinc-800 text-zinc-300 py-1.5 rounded-lg text-sm font-medium hover:bg-zinc-700 transition-colors">Szczegóły</button>
        </div>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-zinc-950 text-white font-sans flex flex-col">
      <nav className="bg-zinc-900/80 backdrop-blur-md border-b border-zinc-800 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="text-xl font-bold tracking-tight text-white">
            <span className="text-blue-400">Bankly</span>
          </div>

          <div className="flex items-center gap-6">
            <div className="hidden md:flex flex-col text-right">
              <span className="text-zinc-200 text-sm font-medium">{user?.email}</span>
              <span className="text-zinc-500 text-xs mt-0.5 tracking-wide">Numer klienta: <span className="text-zinc-400">{user?.customerNumber}</span></span>
            </div>

            <div className="h-8 w-px bg-zinc-800 hidden md:block"></div>

            {user?.role?.toUpperCase() === 'ADMIN' && (
              <button onClick={() => navigate('/aml/admin')} title="Panel AML"
                className="text-zinc-400 hover:text-white p-2 rounded-lg transition-colors">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                </svg>
              </button>
            )}

            <div className="flex items-center gap-3">
              {!isJunior && (
                <button onClick={() => navigate('/settings')} title="Ustawienia"
                  className="text-zinc-400 hover:text-white p-2 rounded-lg transition-colors">
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                </button>
              )}
              <button
                onClick={logout}
                className="text-zinc-400 hover:text-white px-4 py-2 rounded-lg font-medium transition-colors duration-150"
              >
                Wyloguj się
              </button>
            </div>
          </div>
        </div>
      </nav>

      <main className="flex-grow p-4 md:p-8 max-w-7xl mx-auto w-full">
        <header className="mb-6">
          <h1 className="text-3xl font-bold text-zinc-100">Witaj!</h1>
        </header>

        {!isJunior && pendingApprovalsCount > 0 && (
          <div
            onClick={() => navigate('/junior/approvals')}
            className="bg-amber-500/10 border border-amber-500/30 rounded-xl p-4 mb-6 cursor-pointer hover:bg-amber-500/20 transition-colors flex items-center justify-between"
          >
            <div className="flex items-center gap-3">
              <AlertCircle className="text-amber-400 shrink-0" size={22} />
              <div>
                <p className="text-amber-200 font-medium">
                  Masz {pendingApprovalsCount} {pendingApprovalsCount === 1 ? 'transakcję' : pendingApprovalsCount < 5 ? 'transakcje' : 'transakcji'} do zatwierdzenia
                </p>
                <p className="text-amber-300/60 text-sm">Twoje dzieci czekają na decyzję</p>
              </div>
            </div>
            <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
            </svg>
          </div>
        )}

        {amlCount > 0 && (
          <div
            onClick={() => navigate('/aml/holds')}
            className="bg-orange-500/10 border border-orange-500/30 rounded-xl p-4 mb-6 cursor-pointer hover:bg-orange-500/20 transition-colors flex items-center justify-between"
          >
            <div className="flex items-center gap-3">
              <AlertCircle className="text-orange-400 shrink-0" size={22} />
              <div>
                <p className="text-orange-200 font-medium">
                  Masz {amlCount} {amlCount === 1 ? 'wstrzymaną transakcję' : 'wstrzymane transakcje'} przez system AML
                </p>
                <p className="text-orange-300/60 text-sm">Złóż wyjaśnienia, żeby bank mógł rozpatrzyć</p>
              </div>
            </div>
            <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-orange-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
            </svg>
          </div>
        )}

        {loading ? (
          <div className="flex justify-center py-20">
            <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-blue-500"></div>
          </div>
        ) : error ? (
          <div className="bg-red-950/30 border border-red-900/50 text-red-400 p-4 rounded-xl">
            {error}
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            <div className="lg:col-span-8 space-y-6">

              {/* Zakładki całkowicie ukryte dla Juniora */}
              {!isJunior && (
                <div className="flex border-b border-zinc-800 gap-8">
                  <button onClick={() => setActiveTab('PERSONAL')} className={`pb-3 text-sm font-medium border-b-2 transition-all ${activeTab === 'PERSONAL' ? 'border-blue-500 text-white' : 'border-transparent text-zinc-500 hover:text-zinc-300'}`}>
                    Moje finanse
                  </button>
                  <button onClick={() => setActiveTab('JUNIOR')} className={`pb-3 text-sm font-medium border-b-2 transition-all flex items-center gap-2 ${activeTab === 'JUNIOR' ? 'border-blue-500 text-white' : 'border-transparent text-zinc-500 hover:text-zinc-300'}`}>
                    Strefa Junior
                    {juniorAccounts.length > 0 && (
                      <span className="bg-zinc-800 text-zinc-300 text-[10px] px-2 py-0.5 rounded-full">{juniorAccounts.length}</span>
                    )}
                  </button>
                </div>
              )}

              {/* Sekcja główna: Junior widzi to zawsze, Dorosły ma zakładke PERSONAL */}
              {(activeTab === 'PERSONAL' || isJunior) && (
                <div className="space-y-6 animate-in fade-in slide-in-from-bottom-2 duration-300">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {(isJunior ? juniorAccounts : personalAccounts).map((account, index) =>
                      renderAccountCard(account, index === 0)
                    )}
                  </div>

                  <section className="grid grid-cols-2 md:grid-cols-3 gap-4 mt-6">
                    <button
                      onClick={() => navigate('/cards')}
                      className="bg-zinc-900 border border-zinc-800 p-4 rounded-xl flex flex-col items-center justify-center gap-3 hover:bg-zinc-800/80 hover:border-zinc-700 transition-all group">
                      <div className="w-10 h-10 rounded-full bg-zinc-800 flex items-center justify-center text-zinc-400 group-hover:text-blue-400 group-hover:bg-blue-500/10 transition-colors">
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" /></svg>
                      </div>
                      <span className="text-sm font-medium text-zinc-300">Moje karty</span>
                    </button>
                    <button
                      onClick={() => navigate('/klik/code')}
                      className="bg-zinc-900 border border-zinc-800 p-4 rounded-xl flex flex-col items-center justify-center gap-3 hover:bg-zinc-800/80 hover:border-zinc-700 transition-all group">
                      <div className="w-10 h-10 rounded-full bg-zinc-800 flex items-center justify-center text-zinc-400 group-hover:text-blue-400 group-hover:bg-blue-500/10 transition-colors font-bold text-sm">KLIK</div>
                      <span className="text-sm font-medium text-zinc-300">Kod KLIK</span>
                    </button>

                    {!isJunior && (
                      <button className="bg-zinc-900 border border-zinc-800 p-4 rounded-xl flex flex-col items-center justify-center gap-3 hover:bg-zinc-800/80 hover:border-zinc-700 transition-all group">
                        <div className="w-10 h-10 rounded-full bg-zinc-800 flex items-center justify-center text-zinc-400 group-hover:text-blue-400 group-hover:bg-blue-500/10 transition-colors">
                          <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 14v3m4-3v3m4-3v3M3 21h18M3 10h18M3 7l9-4 9 4M4 10h16v11H4V10z" /></svg>
                        </div>
                        <span className="text-sm font-medium text-zinc-300">Inne banki</span>
                      </button>
                    )}
                  </section>
                </div>
              )}

              {/* Strefa Junior dla Rodzica */}
              {activeTab === 'JUNIOR' && !isJunior && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 animate-in fade-in slide-in-from-bottom-2 duration-300">
                  {juniorAccounts.map((account) => {
                    const j = account as JuniorAccountSummary;
                    return (
                      <div key={account.id} className="bg-zinc-900 border border-zinc-800 rounded-2xl p-6 hover:border-zinc-700 transition-all flex flex-col justify-between min-h-[240px]">
                        <div className="z-10">
                          <span className="inline-flex items-center px-2.5 py-1 rounded-md text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 mb-3">
                            Subkonto Junior
                          </span>
                          {j.juniorFirstName && (
                            <p className="text-zinc-400 text-sm mb-2">{j.juniorFirstName} {j.juniorLastName}</p>
                          )}
                          <h2 className="text-3xl font-bold text-white tracking-tight">
                            {formatCurrency(account.balance, account.currency)}
                          </h2>
                        </div>
                        <div className="mt-6 flex gap-2">
                          <button
                            className="flex-1 bg-zinc-800 text-zinc-300 py-2 rounded-lg text-sm font-medium hover:bg-zinc-700 transition-colors"
                            onClick={() => navigate(`/history/${account.id}?from=junior`)}>
                            Historia dziecka
                          </button>
                          <button
                            className="flex-1 bg-zinc-800 text-zinc-300 py-2 rounded-lg text-sm font-medium hover:bg-zinc-700 transition-colors flex items-center justify-center gap-1.5"
                            onClick={() => navigate(`/junior/${account.id}/manage`)}>
                            <Settings size={14} /> Zarządzaj
                          </button>
                        </div>
                      </div>
                    );
                  })}

                  <div
                    onClick={() => navigate('/junior/add')}
                    className="bg-zinc-950 border border-dashed border-zinc-800 rounded-2xl p-6 flex flex-col items-center justify-center text-center hover:border-zinc-600 hover:bg-zinc-900/50 transition-colors cursor-pointer group min-h-[240px]">
                    <div className="w-12 h-12 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-zinc-400 group-hover:text-blue-400 transition-colors" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                      </svg>
                    </div>
                    <h3 className="text-zinc-300 font-medium mb-1">Otwórz nowe konto Junior</h3>
                    <p className="text-zinc-500 text-xs px-4">Dla dzieci w wieku 7-13 lat</p>
                  </div>
                </div>
              )}
            </div>

            <div className="lg:col-span-4">
              <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-6 sticky top-24">
                <h2 className="text-xl font-semibold text-zinc-200 mb-6">Nowy przelew</h2>
                <div className="space-y-3">
                  <button
                    onClick={() => navigate('/transfer/internal')}
                    className="w-full bg-zinc-950 border border-zinc-800 p-4 rounded-xl flex items-center justify-between hover:border-blue-500/50 hover:bg-zinc-900 transition-all group"
                    >
                      <div className="flex items-center gap-4">
                      <div className="w-10 h-10 rounded-full bg-zinc-800 flex items-center justify-center text-zinc-400 group-hover:text-blue-400 group-hover:bg-blue-500/10 transition-colors">
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg>
                      </div>
                      <div className="text-left">
                        <p className="text-sm font-medium text-zinc-200">Przelew wewnętrzny</p>
                        <p className="text-xs text-zinc-500">Konta własne / Inne konto w banku</p>
                      </div>
                    </div>
                  </button>

                  {!isJunior && (
                    <>
                      <button onClick={() => navigate('/transfer/external')} className="w-full bg-zinc-950 border border-zinc-800 p-4 rounded-xl flex items-center justify-between hover:border-blue-500/50 hover:bg-zinc-900 transition-all group">
                        <div className="flex items-center gap-4">
                          <div className="w-10 h-10 rounded-full bg-zinc-800 flex items-center justify-center text-zinc-400 group-hover:text-blue-400 group-hover:bg-blue-500/10 transition-colors">
                            <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" /></svg>
                          </div>
                          <div className="text-left">
                            <p className="text-sm font-medium text-zinc-200">Przelew krajowy</p>
                            <p className="text-xs text-zinc-500">ELIXIR, Express ELIXIR</p>
                          </div>
                        </div>
                      </button>

                      <button
                        onClick={() => navigate(ROUTES.SWIFT_TRANSFER)}
                        className="w-full bg-zinc-950 border border-zinc-800 p-4 rounded-xl flex items-center justify-between hover:border-blue-500/50 hover:bg-zinc-900 transition-all group">
                        <div className="flex items-center gap-4">
                          <div className="w-10 h-10 rounded-full bg-zinc-800 flex items-center justify-center text-zinc-400 group-hover:text-blue-400 group-hover:bg-blue-500/10 transition-colors">
                            <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                          </div>
                          <div className="text-left">
                            <p className="text-sm font-medium text-zinc-200">Przelew zagraniczny</p>
                            <p className="text-xs text-zinc-500">System SWIFT (ISO 20022) · BIC/IBAN</p>
                          </div>
                        </div>
                      </button>
                    </>
                  )}

                  <button onClick={() => navigate('/transfer/klik')}
                    className="w-full bg-zinc-950 border border-zinc-800 p-4 rounded-xl flex items-center justify-between hover:border-blue-500/50 hover:bg-zinc-900 transition-all group">
                    <div className="flex items-center gap-4">
                      <div className="w-10 h-10 rounded-full bg-zinc-800 flex items-center justify-center text-zinc-400 group-hover:text-blue-400 group-hover:bg-blue-500/10 transition-colors">
                        📱
                      </div>
                      <div className="text-left">
                        <p className="text-sm font-medium text-zinc-200">Przelew na telefon</p>
                        <p className="text-xs text-zinc-500">KLIK P2P</p>
                      </div>
                    </div>
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}