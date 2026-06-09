import { api } from './api';

export interface CreateJuniorPayload {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  dateOfBirth: string;
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

export interface PendingApproval {
  id: string;
  juniorAccountId: string;
  juniorAccountNumber: string;
  juniorFirstName: string;
  juniorLastName: string;
  transactionId: string | null;
  transactionType: string | null;
  receiverName: string | null;
  receiverAccountNumber: string | null;
  title: string | null;
  amount: number;
  currency: string;
  description: string;
  status: string;
  createdAt: string;
}

export const juniorService = {
  create: (payload: CreateJuniorPayload) =>
    api.post<JuniorResponse>('/api/junior', payload).then(r => r.data),
  list: () => api.get<JuniorResponse[]>('/api/junior').then(r => r.data),

  pendingList: () =>
    api.get<PendingApproval[]>('/api/junior/pending-approvals').then(r => r.data),
  pendingCount: () =>
    api.get<{ count: number }>('/api/junior/pending-approvals/count').then(r => r.data.count),
  approve: (id: string) =>
    api.post<{ message: string }>(`/api/junior/pending-approvals/${id}/approve`).then(r => r.data),
  reject: (id: string) =>
    api.post<{ message: string }>(`/api/junior/pending-approvals/${id}/reject`).then(r => r.data),
};