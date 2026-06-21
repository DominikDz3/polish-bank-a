import { api } from './api';

export interface KlikAliasView {
  id: string;
  alias: string;
  accountNumber: string;
  createdAt: string;
}

export const klikAliasService = {
  list: () =>
    api.get<KlikAliasView[]>('/api/klik/aliases').then(r => r.data),

  register: (accountId: string, phone: string) =>
    api.post<KlikAliasView>('/api/klik/aliases', { accountId, phone }).then(r => r.data),

  remove: (aliasId: string) =>
    api.delete<void>(`/api/klik/aliases/${aliasId}`).then(r => r.data),
};