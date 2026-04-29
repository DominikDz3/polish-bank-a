import React, { use, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Eye, EyeOff, UserPlus, AlertCircle, CheckCircle2 } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';


interface FormState {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  confirmPassword: string;
}

const passwordRules = [
  { label: 'Minimum 8 znaków', test: (p: string) => p.length >= 8 },
  { label: 'Wielka litera', test: (p: string) => /[A-Z]/.test(p) },
  { label: 'Cyfra', test: (p: string) => /[0-9]/.test(p) },
];

const Register: React.FC = () => {
  const navigate = useNavigate();
  const { register } = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [customerNumber, setCustomerNumber] = useState('');
  const [success, setSuccess] = useState(false);
  const [form, setForm] = useState<FormState>({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    confirmPassword: '',
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setError('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.firstName || !form.lastName || !form.email || !form.password || !form.confirmPassword) {
      setError('Wypełnij wszystkie pola.');
      return;
    }
    if (form.password !== form.confirmPassword) {
      setError('Hasła nie są identyczne.');
      return;
    }
    if (!passwordRules.every(r => r.test(form.password))) {
      setError('Hasło nie spełnia wymagań.');
      return;
    }
    setIsLoading(true);

    try {
      const number = await register(form.firstName, form.lastName, form.email, form.password);
      setCustomerNumber(number);
      setSuccess(true);
      } catch {
        setError('Wystąpił błąd. Spróbuj ponownie.');
      } finally {
      setIsLoading(false);
    }
  };

  if (success) {
  return (
    <div className="min-h-screen bg-zinc-950 flex items-center justify-center px-4">
      <div className="w-full max-w-md text-center">
        <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-8">
          <div className="w-12 h-12 bg-blue-500/10 border border-blue-500/20 rounded-full flex items-center justify-center mx-auto mb-4">
            <span className="text-blue-400 text-xl">✓</span>
          </div>
          <h2 className="text-xl font-semibold text-white mb-2">Konto założone!</h2>
          <p className="text-zinc-400 text-sm mb-6">Twój numer klienta do logowania:</p>
          <div className="bg-zinc-800 border border-blue-500/30 rounded-xl px-6 py-4 mb-4">
            <span className="text-3xl font-mono font-bold text-blue-400 tracking-widest">
              {customerNumber}
            </span>
          </div>
          <p className="text-zinc-500 text-xs mb-8">
            Zapisz ten numer — będzie potrzebny przy każdym logowaniu.
          </p>
          <button
            onClick={() => navigate('/login')}
            className="w-full bg-blue-600 hover:bg-blue-500 text-white font-medium py-2.5 rounded-lg text-sm transition-colors"
          >
            Przejdź do logowania
          </button>
        </div>
      </div>
    </div>
  );
  }

  return (
    <div className="min-h-screen bg-zinc-950 flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-md">

        <div className="text-center mb-10">
          <button onClick={() => navigate('/')} className="text-2xl font-bold tracking-tight text-white hover:opacity-80 transition-opacity">
            <span className="text-blue-400">Bankly</span>
          </button>
          <p className="mt-2 text-zinc-500 text-sm">Otwórz nowe konto</p>
        </div>

        <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-8">
          <h1 className="text-xl font-semibold text-white mb-6">Zakładanie konta</h1>

          {error && (
            <div className="flex items-center gap-2 bg-red-500/10 border border-red-500/20 text-red-400 text-sm rounded-lg px-4 py-3 mb-6">
              <AlertCircle size={16} className="shrink-0" />
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-sm font-medium text-zinc-400 mb-1.5">Imię</label>
                <input
                  type="text"
                  name="firstName"
                  value={form.firstName}
                  onChange={handleChange}
                  placeholder="Jan"
                  className="w-full bg-zinc-800 border border-zinc-700 text-white placeholder-zinc-600 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-colors"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-zinc-400 mb-1.5">Nazwisko</label>
                <input
                  type="text"
                  name="lastName"
                  value={form.lastName}
                  onChange={handleChange}
                  placeholder="Kowalski"
                  className="w-full bg-zinc-800 border border-zinc-700 text-white placeholder-zinc-600 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-colors"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-1.5">Adres e-mail</label>
              <input
                type="email"
                name="email"
                value={form.email}
                onChange={handleChange}
                placeholder="jan@example.com"
                className="w-full bg-zinc-800 border border-zinc-700 text-white placeholder-zinc-600 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-colors"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-1.5">Hasło</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  value={form.password}
                  onChange={handleChange}
                  placeholder="••••••••"
                  className="w-full bg-zinc-800 border border-zinc-700 text-white placeholder-zinc-600 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-colors pr-10"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-zinc-500 hover:text-zinc-300 transition-colors"
                >
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>

              {form.password && (
                <div className="mt-2 space-y-1">
                  {passwordRules.map(rule => (
                    <div key={rule.label} className="flex items-center gap-1.5">
                      <CheckCircle2
                        size={12}
                        className={rule.test(form.password) ? 'text-blue-400' : 'text-zinc-700'}
                      />
                      <span className={`text-xs ${rule.test(form.password) ? 'text-zinc-400' : 'text-zinc-600'}`}>
                        {rule.label}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-1.5">Potwierdź hasło</label>
              <div className="relative">
                <input
                  type={showConfirm ? 'text' : 'password'}
                  name="confirmPassword"
                  value={form.confirmPassword}
                  onChange={handleChange}
                  placeholder="••••••••"
                  className={`w-full bg-zinc-800 border text-white placeholder-zinc-600 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-1 transition-colors pr-10 ${
                    form.confirmPassword && form.confirmPassword !== form.password
                      ? 'border-red-500/50 focus:border-red-500 focus:ring-red-500'
                      : 'border-zinc-700 focus:border-blue-500 focus:ring-blue-500'
                  }`}
                />
                <button
                  type="button"
                  onClick={() => setShowConfirm(!showConfirm)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-zinc-500 hover:text-zinc-300 transition-colors"
                >
                  {showConfirm ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
              {form.confirmPassword && form.confirmPassword !== form.password && (
                <p className="text-xs text-red-400 mt-1">Hasła nie są identyczne</p>
              )}
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full bg-blue-600 hover:bg-blue-500 disabled:bg-blue-600/50 disabled:cursor-not-allowed text-white font-medium py-2.5 rounded-lg text-sm transition-colors flex items-center justify-center gap-2 mt-2"
            >
              {isLoading ? (
                <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <>
                  <UserPlus size={16} />
                  Załóż konto
                </>
              )}
            </button>
          </form>
        </div>

        <p className="text-center text-sm text-zinc-600 mt-6">
          Masz już konto?{' '}
          <Link to="/login" className="text-blue-400 hover:text-blue-300 transition-colors">
            Zaloguj się
          </Link>
        </p>
      </div>
    </div>
  );
};

export default Register;