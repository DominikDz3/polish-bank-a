import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { api } from '../services/api';
import { swiftService } from '../services/swiftService';
import type { SwiftTransferResponse, ChargeBearerInput } from '../services/swiftService';

interface AccountSummary {
  id: string;
  accountNumber: string;
  balance: number;
  currency: string;
  type: string;
}

const COUNTRY_TO_CURRENCY: Record<string, string> = {
  PL: 'PLN',
  GB: 'GBP',
  US: 'USD',
  DE: 'EUR',
  FR: 'EUR',
  CY: 'EUR',
  IT: 'EUR',
  ES: 'EUR',
  NL: 'EUR',
};

function bicCountry(bic: string): string {
  return bic.length >= 6 ? bic.substring(4, 6).toUpperCase() : '';
}

export default function SwiftTransfer() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const [accounts, setAccounts] = useState<AccountSummary[]>([]);
  const [senderAccountId, setSenderAccountId] = useState('');
  const [receiverBic, setReceiverBic] = useState('');
  const [receiverIban, setReceiverIban] = useState('');
  const [receiverName, setReceiverName] = useState('');
  const [receiverCountry, setReceiverCountry] = useState('');
  const [currency, setCurrency] = useState('PLN');
  const [amount, setAmount] = useState('');
  const [chargeBearer, setChargeBearer] = useState<ChargeBearerInput>('SHA');
  const [title, setTitle] = useState('');

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<SwiftTransferResponse | null>(null);

  const [showPinModal, setShowPinModal] = useState(false);
  const [pin, setPin] = useState('');
  const [pinError, setPinError] = useState<string | null>(null);
  
  const [debitPreview, setDebitPreview] = useState<{ converted: number; currency: string } | null>(null);


  useEffect(() => {
    api.get('/api/accounts')
      .then(res => {
        const list = res.data as AccountSummary[];
        setAccounts(list);
        if (list.length > 0) setSenderAccountId(list[0].id);
      })
      .catch(() => setError('Nie udało się pobrać Twoich kont.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    const country = bicCountry(receiverBic);
    if (country) {
      setReceiverCountry(country);
      const suggestedCurrency = COUNTRY_TO_CURRENCY[country];
      if (suggestedCurrency) setCurrency(suggestedCurrency);
    }
  }, [receiverBic]);
    
  useEffect(() => {
    const amt = parseFloat(amount);
    const senderAcc = accounts.find(a => a.id === senderAccountId);
    if (!senderAcc || !amt || amt <= 0 || !currency || currency.length !== 3) {
      setDebitPreview(null);
      return;
    }
    if (senderAcc.currency.toUpperCase() === currency.toUpperCase()) {
      setDebitPreview({ converted: amt, currency: senderAcc.currency });
      return;
    }
    const handle = setTimeout(() => {
      swiftService.quote(amt, currency, senderAcc.currency)
        .then(q => setDebitPreview({ converted: q.converted, currency: q.toCurrency }))
        .catch(() => setDebitPreview(null));
    }, 250);
    return () => clearTimeout(handle);
  }, [amount, currency, senderAccountId, accounts]);

  const validate = (): boolean => {
    const errs: Record<string, string> = {};
    const bic = receiverBic.replace(/\s+/g, '').toUpperCase();
    if (!/^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$/.test(bic)) {
      errs.receiverBic = 'BIC ma 8 lub 11 znaków (np. PLBKPL01XXX).';
    }
    const iban = receiverIban.replace(/\s+/g, '').toUpperCase();
    if (iban.length < 8 || iban.length > 34) {
      errs.receiverIban = 'Numer konta ma 8-34 znaków.';
    }
    if (!receiverName.trim()) errs.receiverName = 'Podaj nazwę odbiorcy.';
    if (!/^[A-Z]{2}$/.test(receiverCountry.toUpperCase())) {
      errs.receiverCountry = 'Kod kraju to 2 litery (np. GB, DE, US).';
    }
    if (!/^[A-Z]{3}$/.test(currency.toUpperCase())) {
      errs.currency = 'Waluta ma 3 litery (np. EUR, USD).';
    }
    const amt = parseFloat(amount);
    if (!amt || amt <= 0) errs.amount = 'Podaj poprawną kwotę.';
    if (!title.trim()) errs.title = 'Podaj tytuł przelewu.';
    setFieldErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setResult(null);
    if (!validate()) return;
    setPin('');
    setPinError(null);
    setShowPinModal(true);
  };

  const confirmWithPin = async () => {
    if (!/^\d{4}$/.test(pin)) {
      setPinError('PIN musi mieć 4 cyfry.');
      return;
    }
    setSubmitting(true);
    try {
      const response = await swiftService.send({
        senderAccountId,
        receiverBic: receiverBic.replace(/\s+/g, '').toUpperCase(),
        receiverIban: receiverIban.replace(/\s+/g, '').toUpperCase(),
        receiverName,
        receiverCountry: receiverCountry.toUpperCase(),
        currency: currency.toUpperCase(),
        amount: parseFloat(amount),
        chargeBearer,
        title,
        pin,
      });
      setResult(response);
      setShowPinModal(false);
      setReceiverBic('');
      setReceiverIban('');
      setReceiverName('');
      setAmount('');
      setTitle('');
      const refreshed = await api.get('/api/accounts');
      setAccounts(refreshed.data);
    } catch (err: any) {
      const msg = err.response?.data?.detail
        || err.response?.data?.message
        || err.response?.data?.errors?.[0]?.defaultMessage
        || 'Nie udało się wysłać przelewu SWIFT.';
      setPinError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const formatCurrency = (v: number, ccy: string) =>
    new Intl.NumberFormat('pl-PL', { style: 'currency', currency: ccy }).format(v);

  if (loading) {
    return (
      <div className="min-h-screen bg-zinc-950 flex justify-center items-center">
        <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-blue-500" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-zinc-950 text-white font-sans flex flex-col">
      <nav className="bg-zinc-900/80 backdrop-blur-md border-b border-zinc-800 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button type="button" onClick={() => navigate(-1)}
              className="text-zinc-400 hover:text-white flex items-center gap-2 transition-colors">
              ← Wróć
            </button>
            <div className="h-6 w-px bg-zinc-800" />
            <div className="text-xl font-bold tracking-tight"><span className="text-blue-400">Bankly</span></div>
            <span className="text-zinc-500 text-sm hidden md:inline">/ Przelew SWIFT</span>
          </div>
          <div className="flex items-center gap-6">
            <div className="hidden md:flex flex-col text-right">
              <span className="text-zinc-200 text-sm font-medium">{user?.email}</span>
              <span className="text-zinc-500 text-xs mt-0.5">Numer klienta: {user?.customerNumber}</span>
            </div>
            <button onClick={logout} className="text-zinc-400 hover:text-white px-4 py-2 rounded-lg">
              Wyloguj się
            </button>
          </div>
        </div>
      </nav>

      <main className="flex-grow p-4 md:p-8 flex justify-center items-start pt-12">
        <div className="w-full max-w-2xl bg-zinc-900 border border-zinc-800 rounded-2xl p-6 md:p-8 shadow-xl">
          <div className="mb-6">
            <h2 className="text-2xl font-bold">Przelew międzynarodowy SWIFT</h2>
            <p className="text-zinc-500 text-sm mt-1">
              Wyślij środki za granicę przez sieć SWIFT (ISO 20022 pacs.008). Opłata 0,35 % kwoty.
            </p>
          </div>

          {error && <div className="bg-red-500/10 border border-red-500/20 text-red-400 p-4 rounded-xl mb-6">{error}</div>}

          {result && (
            <div className="bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 p-4 rounded-xl mb-6 space-y-2 text-sm">
              <div className="font-semibold text-emerald-200">
                Przelew przyjęty przez sieć SWIFT (status {result.status}).
              </div>
              <div>UETR: <span className="font-mono">{result.uetr}</span></div>
              {result.route?.length > 0 && (
                <div>Trasa: <span className="font-mono">{result.route.join(' → ')}</span></div>
              )}
              {result.estimatedSeconds > 0 && (
                <div>Szacowany czas dostarczenia: ~{result.estimatedSeconds}s</div>
              )}
              {result.feeTotal != null && (
                <div>
                  Opłata całkowita: {formatCurrency(result.feeTotal, result.currency)}
                  {' '}(nadawca: {formatCurrency(result.feeSender, result.currency)},
                  {' '}odbiorca: {formatCurrency(result.feeReceiver, result.currency)},
                  {' '}pośrednicy: {formatCurrency(result.feeIntermediary, result.currency)})
                </div>
              )}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-1.5">Z konta</label>
              <select
                value={senderAccountId}
                onChange={e => setSenderAccountId(e.target.value)}
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-white">
                {accounts.map(a => (
                  <option key={a.id} value={a.id}>
                    {a.accountNumber} — {formatCurrency(a.balance, a.currency)}
                  </option>
                ))}
              </select>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-zinc-400 mb-1.5">BIC banku odbiorcy</label>
                <input
                  value={receiverBic}
                  onChange={e => setReceiverBic(e.target.value.toUpperCase())}
                  placeholder="np. UKBKGB01XXX"
                  className={`w-full bg-zinc-950 border ${fieldErrors.receiverBic ? 'border-red-500' : 'border-zinc-800'} rounded-xl px-4 py-3 text-white font-mono`}
                />
                {fieldErrors.receiverBic && <p className="text-red-400 text-xs mt-1.5">{fieldErrors.receiverBic}</p>}
              </div>
              <div>
                <label className="block text-sm font-medium text-zinc-400 mb-1.5">Kraj odbiorcy (ISO)</label>
                <input
                  value={receiverCountry}
                  onChange={e => setReceiverCountry(e.target.value.toUpperCase().slice(0, 2))}
                  placeholder="GB"
                  className={`w-full bg-zinc-950 border ${fieldErrors.receiverCountry ? 'border-red-500' : 'border-zinc-800'} rounded-xl px-4 py-3 text-white font-mono`}
                />
                {fieldErrors.receiverCountry && <p className="text-red-400 text-xs mt-1.5">{fieldErrors.receiverCountry}</p>}
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-1.5">Numer konta odbiorcy (IBAN)</label>
              <input
                value={receiverIban}
                onChange={e => setReceiverIban(e.target.value.toUpperCase())}
                placeholder="GB29NWBK60161331926819"
                className={`w-full bg-zinc-950 border ${fieldErrors.receiverIban ? 'border-red-500' : 'border-zinc-800'} rounded-xl px-4 py-3 text-white font-mono text-sm`}
              />
              {fieldErrors.receiverIban && <p className="text-red-400 text-xs mt-1.5">{fieldErrors.receiverIban}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-1.5">Nazwa odbiorcy</label>
              <input
                value={receiverName}
                onChange={e => setReceiverName(e.target.value)}
                placeholder="London Trading Ltd"
                className={`w-full bg-zinc-950 border ${fieldErrors.receiverName ? 'border-red-500' : 'border-zinc-800'} rounded-xl px-4 py-3 text-white`}
              />
              {fieldErrors.receiverName && <p className="text-red-400 text-xs mt-1.5">{fieldErrors.receiverName}</p>}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-zinc-400 mb-1.5">Kwota</label>
                <input
                  type="number" step="0.01" min="0.01"
                  value={amount}
                  onChange={e => setAmount(e.target.value)}
                  placeholder="0.00"
                  className={`w-full bg-zinc-950 border ${fieldErrors.amount ? 'border-red-500' : 'border-zinc-800'} rounded-xl px-4 py-3 text-white`}
                />
                {fieldErrors.amount && <p className="text-red-400 text-xs mt-1.5">{fieldErrors.amount}</p>}
              </div>
              <div>
                <label className="block text-sm font-medium text-zinc-400 mb-1.5">Waluta</label>
                <input
                  value={currency}
                  onChange={e => setCurrency(e.target.value.toUpperCase().slice(0, 3))}
                  className={`w-full bg-zinc-950 border ${fieldErrors.currency ? 'border-red-500' : 'border-zinc-800'} rounded-xl px-4 py-3 text-white font-mono`}
                />
                {fieldErrors.currency && <p className="text-red-400 text-xs mt-1.5">{fieldErrors.currency}</p>}
              </div>
            {debitPreview && (
              <div className="bg-blue-500/5 border border-blue-500/20 rounded-xl px-4 py-3 text-sm">
                <div className="text-blue-300">
                  Z konta zostanie pobrane:{' '}
                  <span className="font-semibold">
                    {new Intl.NumberFormat('pl-PL', { style: 'currency', currency: debitPreview.currency })
                      .format(debitPreview.converted)}
                  </span>
                </div>
                {debitPreview.currency.toUpperCase() !== currency.toUpperCase() && (
                  <div className="text-xs text-zinc-500 mt-1">Kurs orientacyjny, ustalany w momencie nadania.</div>
                )}
              </div>
            )}
            </div>

            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-1.5">Podział opłat</label>
              <div className="grid grid-cols-3 gap-2">
                {(['OUR', 'SHA', 'BEN'] as ChargeBearerInput[]).map(opt => (
                  <button
                    key={opt} type="button"
                    onClick={() => setChargeBearer(opt)}
                    className={`px-3 py-3 rounded-xl text-sm border transition-colors ${
                      chargeBearer === opt
                        ? 'border-blue-500 bg-blue-500/10 text-blue-300'
                        : 'border-zinc-800 bg-zinc-950 text-zinc-300 hover:border-zinc-700'
                    }`}>
                    <div className="font-semibold">{opt}</div>
                    <div className="text-xs text-zinc-500 mt-0.5">
                      {opt === 'OUR' && 'Płaci nadawca'}
                      {opt === 'SHA' && 'Dzielone 50/50'}
                      {opt === 'BEN' && 'Płaci odbiorca'}
                    </div>
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-1.5">Tytuł przelewu</label>
              <input
                value={title}
                onChange={e => setTitle(e.target.value)}
                placeholder="Faktura INV-2026-0512"
                maxLength={140}
                className={`w-full bg-zinc-950 border ${fieldErrors.title ? 'border-red-500' : 'border-zinc-800'} rounded-xl px-4 py-3 text-white`}
              />
              {fieldErrors.title && <p className="text-red-400 text-xs mt-1.5">{fieldErrors.title}</p>}
            </div>

            <div className="pt-2">
              <button type="submit" disabled={accounts.length === 0}
                className="w-full bg-blue-600 hover:bg-blue-500 disabled:opacity-50 text-white py-3 rounded-xl">
                Wyślij przelew
              </button>
            </div>
          </form>
        </div>
      </main>

      {showPinModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center px-4">
          <div className="w-full max-w-sm bg-zinc-900 border border-zinc-800 rounded-2xl p-6 shadow-2xl">
            <h3 className="text-lg font-semibold text-white text-center mb-1">Potwierdź PIN-em</h3>
            <p className="text-zinc-500 text-sm text-center mb-5">
              Wpisz 4-cyfrowy kod PIN, aby zatwierdzić przelew SWIFT.
            </p>
            <input
              type="password" inputMode="numeric" autoFocus
              value={pin}
              onChange={e => { setPin(e.target.value.replace(/\D/g, '').slice(0, 4)); setPinError(null); }}
              maxLength={4} placeholder="••••"
              className="w-full bg-zinc-800 border border-zinc-700 text-white text-center tracking-[0.6em] text-2xl rounded-lg px-4 py-3"
            />
            {pinError && <p className="text-red-400 text-xs mt-2 text-center">{pinError}</p>}
            <div className="flex gap-3 mt-5">
              <button type="button" disabled={submitting}
                onClick={() => { setShowPinModal(false); setPin(''); setPinError(null); }}
                className="flex-1 bg-zinc-800 hover:bg-zinc-700 text-zinc-200 py-2.5 rounded-lg text-sm">
                Anuluj
              </button>
              <button type="button" disabled={submitting || pin.length !== 4}
                onClick={confirmWithPin}
                className="flex-1 bg-blue-600 hover:bg-blue-500 disabled:bg-blue-600/50 text-white py-2.5 rounded-lg text-sm">
                {submitting ? 'Wysyłam...' : 'Potwierdź'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}