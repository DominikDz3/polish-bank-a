import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldCheck, Smartphone, TrendingUp } from 'lucide-react';

const Home: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div className="flex flex-col min-h-screen bg-zinc-950 text-zinc-100">
      <nav className="bg-zinc-900/80 backdrop-blur-sm border-b border-zinc-800 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="text-xl font-bold tracking-tight text-white">
            Polski <span className="text-blue-400">Bank A</span>
          </div>
          <div className="flex items-center gap-3">
            <button 
            onClick={() => navigate('/login')}
            className="text-zinc-400 hover:text-white px-4 py-2 rounded-lg font-medium transition-colors duration-150">
              Zaloguj się
            </button>
            <button 
            onClick={() => navigate('/register')}
            className="bg-blue-600 hover:bg-blue-500 text-white px-5 py-2 rounded-lg font-medium transition-colors duration-150">
              Załóż konto
            </button>
          </div>
        </div>
      </nav>

      <main className="flex-grow">
        <header className="relative py-32 overflow-hidden">
          <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-blue-950/40 via-zinc-950 to-zinc-950 pointer-events-none" />
          <div className="relative max-w-7xl mx-auto px-6 text-center">
            <div className="inline-block mb-6 px-4 py-1.5 rounded-full bg-blue-500/10 border border-blue-500/20 text-blue-400 text-sm font-medium tracking-wide">
              Twój bank. Twoje zasady.
            </div>
            <h1 className="text-5xl md:text-7xl font-extrabold text-white leading-tight tracking-tight">
              Prosty i Bezpieczny<br />
              <span className="text-blue-400">Bank.</span>
            </h1>
          </div>
        </header>

        <section className="py-20">
          <div className="max-w-7xl mx-auto px-6">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div className="bg-zinc-900 border border-zinc-800 p-8 rounded-2xl flex flex-col items-center text-center hover:border-blue-500/40 transition-colors duration-300">
                <div className="bg-blue-500/10 border border-blue-500/20 p-4 rounded-2xl mb-6 text-blue-400">
                  <ShieldCheck size={32} />
                </div>
                <h3 className="text-xl font-semibold text-white">Gwarancja Bezpieczeństwa</h3>
                <p className="mt-3 text-zinc-500 text-sm leading-relaxed flex-grow">
                  Twoje środki chronione najnowszymi standardami szyfrowania.
                </p>
              </div>

              <div className="bg-zinc-900 border border-zinc-800 p-8 rounded-2xl flex flex-col items-center text-center hover:border-blue-500/40 transition-colors duration-300">
                <div className="bg-blue-500/10 border border-blue-500/20 p-4 rounded-2xl mb-6 text-blue-400">
                  <Smartphone size={32} />
                </div>
                <h3 className="text-xl font-semibold text-white">Intuicyjna Aplikacja</h3>
                <p className="mt-3 text-zinc-500 text-sm leading-relaxed flex-grow">
                  Zarządzaj finansami z dowolnego miejsca, w dowolnej chwili.
                </p>
              </div>

              <div className="bg-zinc-900 border border-zinc-800 p-8 rounded-2xl flex flex-col items-center text-center hover:border-blue-500/40 transition-colors duration-300">
                <div className="bg-blue-500/10 border border-blue-500/20 p-4 rounded-2xl mb-6 text-blue-400">
                  <TrendingUp size={32} />
                </div>
                <h3 className="text-xl font-semibold text-white">Konto pełne korzyści</h3>
                <p className="mt-3 text-zinc-500 text-sm leading-relaxed flex-grow">
                  Oszczędzaj i inwestuj z produktami dopasowanymi do Ciebie.
                </p>
              </div>
            </div>
          </div>
        </section>
      </main>

      <footer className="border-t border-zinc-800 py-8 bg-zinc-900">
        <div className="max-w-7xl mx-auto px-6 text-center">
          <p className="text-zinc-600 text-sm">© 2026 Polski Bank A. Wszelkie prawa zastrzeżone.</p>
        </div>
      </footer>
    </div>
  );
};

export default Home;