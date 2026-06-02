import { api } from './api';

export interface CardSummary {
  id: string;
  accountId: string;
  accountNumber: string;
  maskedNumber: string;
  transactionLimit: number | null;
  dailyLimit: number | null;
  spentToday: number;
  currency: string;
  expiryDate: string | null;
  type: string;
  blocked: boolean;
}

export interface CardPaymentPayload {
  cardId: string;
  amount: number;
  merchant: string;
  currency: string;
}

export interface UpdateCardLimitsPayload {
  transactionLimit?: number;
  dailyLimit?: number;
}

export const cardService = {
  list: () => api.get<CardSummary[]>('/api/cards').then(r => r.data),
  listForJunior: (juniorAccountId: string) =>
    api.get<CardSummary[]>(`/api/cards/junior/${juniorAccountId}`).then(r => r.data),
  pay: (payload: CardPaymentPayload) =>
    api.post<{ message: string; status: string }>('/api/cards/payment', payload).then(r => r.data),
  updateLimits: (cardId: string, payload: UpdateCardLimitsPayload) =>
    api.patch<{ message: string }>(`/api/cards/${cardId}/limits`, payload).then(r => r.data),
};