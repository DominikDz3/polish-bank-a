import { api } from './api';

interface AuthResponse {
  token: string;
  email: string;
  role: string;
  customerNumber: string;
  pinSet: boolean;
}

const login = async (customerNumber: string, password: string): Promise<AuthResponse> => {
  const { data } = await api.post<AuthResponse>('/api/auth/login', { customerNumber, password });
  localStorage.setItem('token', data.token);
  localStorage.setItem('pinSet', String(data.pinSet));
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
  localStorage.setItem('pinSet', String(data.pinSet));
  return data;
};

const setPin = async (pin: string, confirmPin: string): Promise<void> => {
  await api.post('/api/auth/pin', { pin, confirmPin });
  localStorage.setItem('pinSet', 'true');
};

const logout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('pinSet');
};
const getToken = () => localStorage.getItem('token');

export const authService = { login, register, setPin, logout, getToken };