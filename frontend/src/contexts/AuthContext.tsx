import { createContext, useContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import { authService } from '../services/authService';

interface AuthUser { email: string; role: string; customerNumber: string; pinSet: boolean }

interface AuthContextType {
  user: AuthUser | null;
  loading: boolean;
  login: (customerNumber: string, password: string) => Promise<void>;
  register: (firstName: string, lastName: string, email: string, password: string) => Promise<string>;
  setPin: (pin: string, confirmPin: string) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        const pinSet = localStorage.getItem('pinSet') === 'true';
        setUser({
          email: payload.sub,
          role: payload.role,
          customerNumber: payload.customerNumber ?? '',
          pinSet,
        });
      } catch {
        localStorage.removeItem('token');
        localStorage.removeItem('pinSet');
      }
    }
    setLoading(false);
  }, []);

  const login = async (customerNumber: string, password: string) => {
    const res = await authService.login(customerNumber, password);
    setUser({ email: res.email, role: res.role, customerNumber: res.customerNumber, pinSet: res.pinSet });
  };

  const register = async (firstName: string, lastName: string, email: string, password: string): Promise<string> => {
    const res = await authService.register(firstName, lastName, email, password);
    setUser({ email: res.email, role: res.role, customerNumber: res.customerNumber, pinSet: res.pinSet });
    return res.customerNumber;
  };

  const setPin = async (pin: string, confirmPin: string) => {
    await authService.setPin(pin, confirmPin);
    setUser((u) => (u ? { ...u, pinSet: true } : u));
  };

  const logout = () => {
    authService.logout();
    setUser(null);
    window.location.href = '/login';
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, setPin, logout, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}