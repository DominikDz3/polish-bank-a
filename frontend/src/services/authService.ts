import { api } from './api';

interface AuthResponse {
  token: string;
  email: string;
  role: string;
  customerNumber: string;
}

const login = async (customerNumber: string, password: string): Promise<AuthResponse> => {
  const { data } = await api.post<AuthResponse>('/api/auth/login', { customerNumber, password });
  localStorage.setItem('token', data.token);
  return data;
};

const register = async (
  firstName: string,
  lastName: string,
  email: string,
  password: string
): Promise<AuthResponse> => {
  const { data } = await api.post<AuthResponse>('/api/auth/register', {
    firstName, lastName, email, password,
  });
  localStorage.setItem('token', data.token);
  return data;
};

const logout = () => localStorage.removeItem('token');
const getToken = () => localStorage.getItem('token');

export const authService = { login, register, logout, getToken };