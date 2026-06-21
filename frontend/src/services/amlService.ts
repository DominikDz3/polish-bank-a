import { api } from './api';

export interface AmlHoldView {
  id: string;
  holdType: string;
  status: string;
  amount: number;
  currency: string;
  receiverInfo: string | null;
  reason: string | null;
  triggeredRule: string | null;
  clientExplanation: string | null;
  decisionNote: string | null;
  clientEmail: string | null;
  createdAt: string;
  decisionAt: string | null;
}

export const amlService = {
  myHolds: () => api.get<AmlHoldView[]>('/api/aml/holds').then(r => r.data),
  myCount: () => api.get<{ count: number }>('/api/aml/holds/count').then(r => r.data.count),
  submitExplanation: (id: string, text: string) =>
    api.post<AmlHoldView>(`/api/aml/holds/${id}/explanation`, { text }).then(r => r.data),
  adminPending: () => api.get<AmlHoldView[]>('/api/aml/admin/holds/pending').then(r => r.data),
  adminAll: () => api.get<AmlHoldView[]>('/api/aml/admin/holds').then(r => r.data),
  approve: (id: string, note: string) =>
    api.post<AmlHoldView>(`/api/aml/admin/holds/${id}/approve`, { note }).then(r => r.data),
  reject: (id: string, note: string) =>
    api.post<AmlHoldView>(`/api/aml/admin/holds/${id}/reject`, { note }).then(r => r.data),
};