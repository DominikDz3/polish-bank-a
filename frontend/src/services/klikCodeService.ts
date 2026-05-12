import { api } from './api';

export interface KlikCode {
  id: string;
  code: string;
  expiresAt: string;
  status: string;
  accountNumber: string;
}

export const klikCodeService = {
  generateCode: (accountId: string): Promise<KlikCode> =>
    api.post('/api/klik/codes/generate', null, { params: { accountId } })
       .then(r => r.data),
};