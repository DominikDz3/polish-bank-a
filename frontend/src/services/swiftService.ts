import { api } from './api';

export type ChargeBearerInput = 'OUR' | 'SHA' | 'BEN';

export interface SwiftTransferRequest {
  senderAccountId: string;
  receiverBic: string;
  receiverIban: string;
  receiverName: string;
  receiverCountry: string;
  currency: string;
  amount: number;
  chargeBearer: ChargeBearerInput;
  title: string;
  pin: string;
}

export interface SwiftTransferResponse {
  id: string;
  transactionId: string;
  uetr: string;
  messageId: string;
  senderBic: string;
  senderAccountNumber: string;
  receiverBic: string;
  receiverCountry: string;
  receiverIban: string;
  receiverName: string;
  amount: number;
  currency: string;
  chargeBearerInput: string;
  chargeBearer: string;
  route: string[];
  feeTotal: number;
  feeSender: number;
  feeReceiver: number;
  feeIntermediary: number;
  estimatedSeconds: number;
  status: string;
  returnReason: string | null;
  title: string;
  createdAt: string;
  deliveredAt: string | null;
    returnedAt: string | null;
  debitedAmount: number | null;
  debitedCurrency: string | null;
}

export interface SwiftQuote {
  fromCurrency: string;
  toCurrency: string;
  amount: number;
  converted: number;
}

export const swiftService = {
  send: (body: SwiftTransferRequest) =>
    api.post<SwiftTransferResponse>('/api/transactions/swift', body).then(r => r.data),
  list: () =>
    api.get<SwiftTransferResponse[]>('/api/transactions/swift').then(r => r.data),
  getById: (id: string) =>
        api.get<SwiftTransferResponse>(`/api/transactions/swift/${id}`).then(r => r.data),
  quote: (amount: number, from: string, to: string) =>
    api.get<SwiftQuote>('/api/transactions/swift/quote', {
      params: { amount, from, to }
    }).then(r => r.data),
};