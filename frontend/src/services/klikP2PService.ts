import { api } from './api';

export interface KlikP2PRequest {
  senderAccountId: string;
  phone: string;
  receiverName: string;
  amount: number;
  title: string;
  pin: string;
}

export interface KlikP2PResponse {
  routing: 'INTERNAL' | 'EXTERNAL';
  status: string;
  receiverBank: string;
  receiverAccount: string;
  message: string;
}

export const klikP2PService = {
  send: (req: KlikP2PRequest) =>
    api.post<KlikP2PResponse>('/api/klik/p2p', req).then(r => r.data),
};