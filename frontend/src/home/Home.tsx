import React from 'react';
import { ShieldCheck, Smartphone, TrendingUp } from 'lucide-react';

const Home: React.FC = () => {
  return (
    <div className="flex flex-col min-h-screen bg-gray-50 text-gray-900">
      <nav className="bg-white shadow-sm sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="text-2xl font-bold text-gray-900">Polski Bank A</div>
          <div className="flex items-center gap-4">
            <button className="text-gray-700 hover:text-gray-900 px-4 py-2 rounded-lg font-medium transition duration-150">
              Zaloguj się
            </button>
            <button className="bg-gray-900 text-white hover:bg-gray-700 px-6 py-2 rounded-lg font-medium shadow transition duration-150">
              Załóż konto
            </button>
          </div>
        </div>
      </nav>

      <main className="flex-grow">
      <header className="py-24 bg-white">
        <div className="max-w-7xl mx-auto px-6 text-center">
          <h1 className="text-5xl md:text-6xl font-extrabold text-gray-900 leading-tight">
            Prosty i Bezpieczny Bank.
          </h1>
          <p className="mt-6 text-xl text-gray-600 max-w-3xl mx-auto">
          </p>
        </div>
      </header>

      <section className="py-20 bg-gray-50">
        <div className="max-w-7xl mx-auto px-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-10">
            <div className="bg-white p-8 rounded-2xl shadow-lg border border-gray-100 flex flex-col items-center text-center">
              <div className="bg-gray-100 p-4 rounded-full mb-6 text-gray-900">
                <ShieldCheck size={36} />
              </div>
              <h3 className="text-2xl font-semibold text-gray-900">Gwarancja Bezpieczeństwa</h3>
              <p className="mt-4 text-gray-600 flex-grow">
              </p>
            </div>

            <div className="bg-white p-8 rounded-2xl shadow-lg border border-gray-100 flex flex-col items-center text-center">
              <div className="bg-gray-100 p-4 rounded-full mb-6 text-gray-900">
                <Smartphone size={36} />
              </div>
              <h3 className="text-2xl font-semibold text-gray-900">Intuicyjna Aplikacja</h3>
              <p className="mt-4 text-gray-600 flex-grow">
              </p>
            </div>

            <div className="bg-white p-8 rounded-2xl shadow-lg border border-gray-100 flex flex-col items-center text-center">
              <div className="bg-gray-100 p-4 rounded-full mb-6 text-gray-900">
                <TrendingUp size={36} />
              </div>
              <h3 className="text-2xl font-semibold text-gray-900">Konto pełne korzyści</h3>
              <p className="mt-4 text-gray-600 flex-grow">
              </p>
            </div>
          </div>
        </div>
      </section>
      </main>

      <footer className="py-12 bg-gray-900 text-gray-300">
        <div className="max-w-7xl mx-auto px-6 text-center">
          <p className="text-sm">© 2026 Polski Bank A. Wszelkie prawa zastrzeżone.</p>
        </div>
      </footer>
    </div>
  );
};

export default Home;