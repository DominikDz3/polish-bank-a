import { useAuth } from '../contexts/AuthContext';

export default function Dashboard() {
  const { user, logout } = useAuth();

  return (
    <div className="min-h-screen bg-zinc-950 text-white">
      <nav className="bg-zinc-900 border-b border-zinc-800 px-6 py-4 flex justify-between items-center">
        <div className="text-lg font-bold">
          Polski <span className="text-blue-400">Bank A</span>
        </div>
        <div className="flex items-center gap-4">
          <span className="text-zinc-400 text-sm">{user?.email}</span>
          <button onClick={logout} className="text-sm text-zinc-400 hover:text-white transition-colors">
            Wyloguj
          </button>
        </div>
      </nav>
    </div>
  );
}