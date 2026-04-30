import { createContext, useContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import { authService } from '../services/authService';

interface AuthUser { email: string; role: string; customerNumber: string }

interface AuthContextType {
  user: AuthUser | null;
  login: (customerNumber: string, password: string) => Promise<void>;
  register: (firstName: string, lastName: string, email: string, password: string) => Promise<string>;
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
        setUser({ email: payload.sub, role: payload.role, customerNumber: payload.customerNumber ?? '' });
      } catch {
        localStorage.removeItem('token');
      }
    }
  }, []);

  const login = async (customerNumber: string, password: string) => {
    const res = await authService.login(customerNumber, password);
    setUser({ email: res.email, role: res.role, customerNumber: res.customerNumber });
  };

  const register = async (firstName: string, lastName: string, email: string, password: string): Promise<string> => {
    const res = await authService.register(firstName, lastName, email, password);
    setUser({ email: res.email, role: res.role, customerNumber: res.customerNumber });
    return res.customerNumber;
  };

  const logout = () => {
    authService.logout();
    setUser(null);
    window.location.href = '/login';
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