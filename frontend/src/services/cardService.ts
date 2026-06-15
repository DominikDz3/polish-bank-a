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
  providerToken: string | null;
  providerStatus: string | null;
  maskedPan: string | null;
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

export interface OrderCardResponse {
  id: string;
  providerToken: string;
  maskedPan: string;
  fullPan: string;
  cvv: string;
  expiryMonth: number;
  expiryYear: number;
  providerStatus: string;
  cardType: string;
}

export const cardService = {
  list: () => api.get<CardSummary[]>('/api/cards').then(r => r.data),
  listForJunior: (juniorAccountId: string) =>
    api.get<CardSummary[]>(`/api/cards/junior/${juniorAccountId}`).then(r => r.data),
  pay: (payload: CardPaymentPayload) =>
    api.post<{ message: string; status: string }>('/api/cards/payment', payload).then(r => r.data),
  updateLimits: (cardId: string, payload: UpdateCardLimitsPayload) =>
    api.patch<{ message: string }>(`/api/cards/${cardId}/limits`, payload).then(r => r.data),
  order: (cardType: 'VIRTUAL' | 'PHYSICAL') =>
    api.post<OrderCardResponse>('/api/cards/order', { cardType }).then(r => r.data),
  orderJunior: (juniorAccountId: string, cardType: 'PREPAID') =>
    api.post<OrderCardResponse>(`/api/cards/junior/${juniorAccountId}/order`, { cardType }).then(r => r.data),
  block: (cardId: string) =>
    api.post<{ message: string }>(`/api/cards/${cardId}/block`, {}).then(r => r.data),
  unblock: (cardId: string) =>
    api.post<{ message: string }>(`/api/cards/${cardId}/unblock`, {}).then(r => r.data),
  activate: (cardId: string) =>
    api.post<{ message: string }>(`/api/cards/${cardId}/activate`, {}).then(r => r.data),
  devForceActivate: (cardId: string) =>
    api.post<{ message: string }>(`/api/cards/${cardId}/dev/force-activate`, {}).then(r => r.data),
  topup: (cardId: string, amount: number) =>
    api.post<{ message: string; newCardBalance: number }>(`/api/cards/${cardId}/topup`, { amount }).then(r => r.data),
  delete: (cardId: string) =>
    api.delete<{ message: string }>(`/api/cards/${cardId}`).then(r => r.data),
};