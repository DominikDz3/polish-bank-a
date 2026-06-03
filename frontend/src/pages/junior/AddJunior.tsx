import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertCircle, UserPlus } from 'lucide-react';
import { api } from '../../services/api';
import { juniorService } from '../../services/juniorService';
import { useAuth } from '../../contexts/AuthContext';

interface AccountSummary {
  id: string;
  accountNumber: string;
  balance: number;
  currency: string;
  type: string;
}

const AddJunior: React.FC = () => {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [accounts, setAccounts] = useState<AccountSummary[]>([]);
  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    dateOfBirth: '',
    parentAccountId: '',
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState<null | { customerNumber: string; accountNumber: string }>(null);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    api.get<AccountSummary[]>('/api/accounts').then(r => {
      const parents = r.data.filter(a => a.type !== 'JUNIOR');
      setAccounts(parents);
      if (parents.length > 0) {
        setForm(f => ({ ...f, parentAccountId: parents[0].id }));
      }
    });
  }, []);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setError('');
  };

  const preventEnterSubmit = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setError('');

    if (!form.firstName || !form.lastName || !form.email || !form.password
        || !form.dateOfBirth || !form.parentAccountId) {
      setError('Wypełnij wszystkie pola.');
      return;
    }
    if (form.password.length < 8) {
      setError('Hasło musi mieć minimum 8 znaków.');
      return;
    }
    if (!/[A-Z]/.test(form.password)) {
      setError('Hasło musi zawierać wielką literę.');
      return;
    }
    if (!/[0-9]/.test(form.password)) {
      setError('Hasło musi zawierać cyfrę.');
      return;
    }

    setIsLoading(true);
    try {
      const res = await juniorService.create(form);
      setSuccess({ customerNumber: res.customerNumber, accountNumber: res.accountNumber });
    } catch (err: any) {
      console.error('Błąd tworzenia juniora:', err.response?.status, err.response?.data);
      setError(err.response?.data?.detail
        || err.response?.data?.message
        || 'Nie udało się utworzyć konta Junior.');
    } finally {
      setIsLoading(false);
    }
  };

  const renderNav = () => (
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
          <span className="text-zinc-500 text-sm hidden md:inline">/ Nowe konto Junior</span>
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
  );

  if (success) {
    return (
      <div className="min-h-screen bg-zinc-950 text-white font-sans flex flex-col">
        {renderNav()}

        <main className="flex-grow flex items-center justify-center px-4">
          <div className="w-full max-w-md text-center">
            <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-8">
              <div className="w-12 h-12 bg-blue-500/10 border border-blue-500/20 rounded-full flex items-center justify-center mx-auto mb-4">
                <span className="text-blue-400 text-xl">✓</span>
              </div>
              <h2 className="text-xl font-semibold text-white mb-2">Konto Junior utworzone</h2>
              <p className="text-zinc-400 text-sm mb-6">Przekaż dziecku dane do logowania:</p>
              <div className="bg-zinc-800 border border-blue-500/30 rounded-xl px-6 py-4 mb-4">
                <p className="text-xs text-zinc-500 uppercase tracking-wider">Numer klienta</p>
                <p className="text-2xl font-mono font-bold text-blue-400 tracking-widest">{success.customerNumber}</p>
              </div>
              <div className="bg-zinc-800 border border-zinc-700 rounded-xl px-6 py-4 mb-6 text-left">
                <p className="text-xs text-zinc-500 uppercase tracking-wider mb-1">Numer rachunku Junior</p>
                <p className="text-zinc-200 font-mono text-sm">{success.accountNumber}</p>
              </div>
              <button
                type="button"
                onClick={() => navigate('/dashboard?tab=junior')}
                className="w-full bg-blue-600 hover:bg-blue-500 text-white font-medium py-2.5 rounded-lg text-sm transition-colors"
              >
                Wróć do panelu
              </button>
            </div>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-zinc-950 text-white font-sans flex flex-col">
      {renderNav()}

      <main className="flex-grow flex items-center justify-center px-4 py-12">
        <div className="w-full max-w-md">
          <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-8">
            <h1 className="text-xl font-semibold text-white mb-2">Nowe konto Junior</h1>
            <p className="text-zinc-500 text-sm mb-6">
              Konto Junior jest dla dzieci w wieku 7-13 lat i zawsze jest podpięte pod Twoje konto.
            </p>

            {error && (
              <div className="flex items-center gap-2 bg-red-500/10 border border-red-500/20 text-red-400 text-sm rounded-lg px-4 py-3 mb-5">
                <AlertCircle size={16} className="shrink-0" />
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} noValidate className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <input name="firstName" value={form.firstName} onChange={handleChange} onKeyDown={preventEnterSubmit}
                  placeholder="Imię"
                  className="bg-zinc-800 border border-zinc-700 text-white rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:border-blue-500" />
                <input name="lastName" value={form.lastName} onChange={handleChange} onKeyDown={preventEnterSubmit}
                  placeholder="Nazwisko"
                  className="bg-zinc-800 border border-zinc-700 text-white rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:border-blue-500" />
              </div>

              <input type="email" name="email" value={form.email} onChange={handleChange} onKeyDown={preventEnterSubmit}
                placeholder="E-mail dziecka"
                className="w-full bg-zinc-800 border border-zinc-700 text-white rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:border-blue-500" />

              <input type="password" name="password" value={form.password} onChange={handleChange} onKeyDown={preventEnterSubmit}
                placeholder="Hasło (min. 8 znaków, wielka litera, cyfra)"
                className="w-full bg-zinc-800 border border-zinc-700 text-white rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:border-blue-500" />

              <div>
                <label className="block text-xs text-zinc-500 mb-1.5">Data urodzenia</label>
                <input type="date" name="dateOfBirth" value={form.dateOfBirth} onChange={handleChange} onKeyDown={preventEnterSubmit}
                  className="w-full bg-zinc-800 border border-zinc-700 text-white rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:border-blue-500" />
              </div>

              <div>
                <label className="block text-xs text-zinc-500 mb-1.5">Konto rodzica (powiązanie)</label>
                <select name="parentAccountId" value={form.parentAccountId} onChange={handleChange}
                  className="w-full bg-zinc-800 border border-zinc-700 text-white rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:border-blue-500">
                  {accounts.map(a => (
                    <option key={a.id} value={a.id}>
                      {a.accountNumber.slice(0, 8)}...{a.accountNumber.slice(-4)} ({a.currency})
                    </option>
                  ))}
                </select>
              </div>

              <button type="submit" disabled={isLoading}
                className="w-full bg-blue-600 hover:bg-blue-500 disabled:bg-blue-600/50 disabled:cursor-not-allowed text-white font-medium py-2.5 rounded-lg text-sm transition-colors flex items-center justify-center gap-2 mt-2">
                {isLoading ? (
                  <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                ) : (
                  <><UserPlus size={16} /> Utwórz konto Junior</>
                )}
              </button>
            </form>
          </div>
        </div>
      </main>
    </div>
  );
};

export default AddJunior;