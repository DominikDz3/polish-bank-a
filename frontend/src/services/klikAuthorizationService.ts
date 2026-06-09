import { api } from './api';

export interface PendingKlikAuthorization {
  id: string;
  klikTransactionId: string;
  amount: number;
  currency: string;
  merchantName: string;
  accountNumber: string;
  expiryTime: string;
}

export const klikAuthorizationService = {
  getPending: (): Promise<PendingKlikAuthorization[]> =>
    api.get('/api/klik/authorizations/pending').then(r => r.data),

  confirm: (id: string, pin: string): Promise<void> =>
    api.post(`/api/klik/authorizations/${id}/confirm`, { status: 'ACCEPT', pin }),

  reject: (id: string): Promise<void> =>
    api.post(`/api/klik/authorizations/${id}/confirm`, { status: 'REJECT', reason: 'USER_DECLINED' }),
};