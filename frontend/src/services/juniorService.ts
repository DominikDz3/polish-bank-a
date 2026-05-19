import { api } from './api';

export interface CreateJuniorPayload {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  dateOfBirth: string; // YYYY-MM-DD
  parentAccountId: string;
}

export interface JuniorResponse {
  userId: string;
  customerNumber: string;
  email: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  accountId: string;
  accountNumber: string;
  balance: number;
  currency: string;
  parentAccountNumber: string;
}

export const juniorService = {
  create: (payload: CreateJuniorPayload) =>
    api.post<JuniorResponse>('/api/junior', payload).then(r => r.data),
  list: () => api.get<JuniorResponse[]>('/api/junior').then(r => r.data),
};