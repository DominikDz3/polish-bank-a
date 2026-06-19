import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import KlikAliasesSection from './KlikAliasesSection';

type Tab = 'klik' | 'security';

export default function Settings() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [tab, setTab] = useState<Tab>('klik');

  return (
    <div className="min-h-screen bg-zinc-950 text-white font-sans flex flex-col">
      <nav className="bg-zinc-900/80 backdrop-blur-md border-b border-zinc-800 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button onClick={() => navigate(-1)} className="text-zinc-400 hover:text-white">← Wróć</button>
            <div className="h-6 w-px bg-zinc-800"></div>
            <div className="text-xl font-bold"><span className="text-blue-400">Bankly</span></div>
            <span className="text-zinc-500 text-sm hidden md:inline">/ Ustawienia</span>
          </div>
          <div className="flex items-center gap-6">
            <span className="text-zinc-200 text-sm hidden md:inline">{user?.email}</span>
            <button onClick={logout} className="text-zinc-400 hover:text-white px-4 py-2 rounded-lg">Wyloguj się</button>
          </div>
        </div>
      </nav>

      <main className="flex-grow p-4 md:p-8 max-w-5xl w-full mx-auto">
        <h1 className="text-2xl font-bold mb-6">Ustawienia konta</h1>

        <div className="grid md:grid-cols-[200px_1fr] gap-6">
          <aside className="bg-zinc-900 border border-zinc-800 rounded-2xl p-3 h-fit">
            <button onClick={() => setTab('klik')}
              className={`w-full text-left px-3 py-2 rounded-lg text-sm transition-colors ${tab === 'klik' ? 'bg-blue-600 text-white' : 'text-zinc-400 hover:bg-zinc-800'}`}>
              Numery KLIK
            </button>
            <button onClick={() => setTab('security')}
              className={`w-full text-left px-3 py-2 rounded-lg text-sm transition-colors ${tab === 'security' ? 'bg-blue-600 text-white' : 'text-zinc-400 hover:bg-zinc-800'}`}>
              Bezpieczeństwo
            </button>
          </aside>

          <section className="bg-zinc-900 border border-zinc-800 rounded-2xl p-6">
            {tab === 'klik' && <KlikAliasesSection />}
            {tab === 'security' && (
              <div className="text-zinc-500 text-sm">Wkrótce — zmiana PIN-u i hasła.</div>
            )}
          </section>
        </div>
      </main>
    </div>
  );
}