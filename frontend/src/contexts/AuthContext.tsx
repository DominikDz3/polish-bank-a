import { createContext, useContext, useState, useEffect} from 'react';
import type { ReactNode } from 'react';
import { authService } from '../services/authService';

interface AuthUser { email: string; role: string }

interface AuthContextType {
  user: AuthUser | null;
  login: (email: string, password: string) => Promise<void>;
  register: (firstName: string, lastName: string, email: string, password: string) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        setUser({ email: payload.sub, role: payload.role });
      } catch {
        localStorage.removeItem('token');
      }
    }
  }, []);

  const login = async (email: string, password: string) => {
    const res = await authService.login(email, password);
    setUser({ email: res.email, role: res.role });
  };

  const register = async (firstName: string, lastName: string, email: string, password: string) => {
    const res = await authService.register(firstName, lastName, email, password);
    setUser({ email: res.email, role: res.role });
  };

  const logout = () => {
    authService.logout();
    setUser(null);
    window.location.href = '/';
  };

  return (
    <AuthContext.Provider value={{ user, login, register, logout, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}