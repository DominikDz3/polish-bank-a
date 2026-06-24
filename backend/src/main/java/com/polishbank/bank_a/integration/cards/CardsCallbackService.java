package com.polishbank.bank_a.integration.cards;

import com.polishbank.bank_a.domain.card.CardLimitService;
import com.polishbank.bank_a.entity.Account;
import com.polishbank.bank_a.entity.Card;
import com.polishbank.bank_a.entity.CardAuthorization;
import com.polishbank.bank_a.entity.Transaction;
import com.polishbank.bank_a.integration.cards.dto.AuthorizeWebhookRequest;
import com.polishbank.bank_a.integration.cards.dto.AuthorizeWebhookResponse;
import com.polishbank.bank_a.integration.cards.dto.CaptureWebhookRequest;
import com.polishbank.bank_a.integration.cards.dto.CaptureWebhookResponse;
import com.polishbank.bank_a.integration.cards.dto.RefundWebhookRequest;
import com.polishbank.bank_a.integration.cards.dto.RefundWebhookResponse;
import com.polishbank.bank_a.integration.cards.dto.TopupCardRequest;
import com.polishbank.bank_a.repository.AccountRepository;
import com.polishbank.bank_a.repository.CardAuthorizationRepository;
import com.polishbank.bank_a.repository.CardRepository;
import com.polishbank.bank_a.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardsCallbackService {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final CardAuthorizationRepository cardAuthorizationRepository;
    private final TransactionRepository transactionRepository;
    private final CardLimitService cardLimitService;
    private final CardTransactionAuditService cardTransactionAuditService;
    private final CardsProviderClient cardsProviderClient;

    @Transactional
    public AuthorizeWebhookResponse authorize(AuthorizeWebhookRequest req) {
        if (req.amount() == null || req.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return AuthorizeWebhookResponse.declined("INVALID_AMOUNT");
        }
        if (req.transactionId() == null || req.transactionId().isBlank()) {
            return AuthorizeWebhookResponse.declined("INVALID_REQUEST");
        }

        Optional<CardAuthorization> existing =
                cardAuthorizationRepository.findByExternalTransactionId(req.transactionId());
        if (existing.isPresent()) {
            CardAuthorization ca = existing.get();
            if ("HELD".equals(ca.getStatus()) || "SETTLED".equals(ca.getStatus())) {
                return AuthorizeWebhookResponse.approved(ca.getAuthorizationCode());
            }
        }

        Card card = null;
        Account account = null;

        if (req.cardToken() != null && !req.cardToken().isBlank()) {
            Optional<Card> cardOpt = cardRepository.findByProviderToken(req.cardToken());
            if (cardOpt.isEmpty()) {
                return AuthorizeWebhookResponse.declined("CARD_NOT_FOUND");
            }
            card = cardOpt.get();
            account = card.getAccount();
        } else if (req.accountId() != null && !req.accountId().isBlank()) {
            account = lookupAccount(req.accountId());
            if (account == null) {
                return AuthorizeWebhookResponse.declined("ACCOUNT_NOT_FOUND");
            }
            card = resolveCardForAccount(account, req.cardLastDigits());
        } else {
            return AuthorizeWebhookResponse.declined("INVALID_REQUEST");
        }

        if (card != null) {
            String declineReason = cardLimitDeclineReason(card, req.amount());
            if (declineReason != null) {
                saveRejectedOnce(card, req.transactionId(), req.amount(), req.merchantName(), declineReason);
                restorePrepaidBalanceIfNeeded(card, req.amount());
                return AuthorizeWebhookResponse.declined(mapDeclineReasonToCode(declineReason));
            }
        }

        BigDecimal blocked = account.getBlockedFunds() != null ? account.getBlockedFunds() : BigDecimal.ZERO;
        BigDecimal available = account.getBalance().subtract(blocked);
        if (available.compareTo(req.amount()) < 0) {
            if (card != null) {
                saveRejectedOnce(card, req.transactionId(), req.amount(), req.merchantName(), "Brak wystarczających środków");
                restorePrepaidBalanceIfNeeded(card, req.amount());
            }
            return AuthorizeWebhookResponse.declined("INSUFFICIENT_FUNDS");
        }

        account.setBlockedFunds(blocked.add(req.amount()));
        accountRepository.save(account);

        String authCode = "AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String currency = req.currency() != null ? req.currency() : account.getCurrency();

        CardAuthorization ca = CardAuthorization.builder()
                .authorizationCode(authCode)
                .externalTransactionId(req.transactionId())
                .card(card)
                .account(account)
                .amount(req.amount())
                .currency(currency)
                .merchantName(req.merchantName())
                .status("HELD")
                .build();
        cardAuthorizationRepository.save(ca);

        Transaction pending = buildCardPaymentTransaction(card, account, req.amount(), currency,
                req.transactionId(), req.merchantName(), "PENDING", null);
        transactionRepository.save(pending);

        return AuthorizeWebhookResponse.approved(authCode);
    }

    @Transactional
    public CaptureWebhookResponse capture(CaptureWebhookRequest req) {
        CardAuthorization ca = null;
        if (req.authorizationCode() != null && !req.authorizationCode().isBlank()) {
            ca = cardAuthorizationRepository.findByAuthorizationCode(req.authorizationCode()).orElse(null);
        }
        if (ca == null && req.transactionId() != null && !req.transactionId().isBlank()) {
            ca = cardAuthorizationRepository.findByExternalTransactionId(req.transactionId()).orElse(null);
        }

        if (ca != null && "SETTLED".equals(ca.getStatus())) {
            return new CaptureWebhookResponse("SETTLED");
        }

        if (ca != null) {
            Account account = ca.getAccount();
            BigDecimal amount = ca.getAmount();

            account.setBalance(account.getBalance().subtract(amount));
            BigDecimal blocked = account.getBlockedFunds() != null ? account.getBlockedFunds() : BigDecimal.ZERO;
            account.setBlockedFunds(blocked.subtract(amount).max(BigDecimal.ZERO));
            accountRepository.save(account);

            Transaction tx = findPendingCardPayment(ca.getExternalTransactionId());
            if (tx != null) {
                tx.setStatus("COMPLETED");
                tx.setExecutionDate(LocalDateTime.now(ZONE));
                transactionRepository.save(tx);
            } else {
                transactionRepository.save(buildCardPaymentTransaction(ca.getCard(), account, amount, ca.getCurrency(),
                        ca.getExternalTransactionId(), ca.getMerchantName(), "COMPLETED", LocalDateTime.now(ZONE)));
            }

            ca.setStatus("SETTLED");
            ca.setSettledAt(LocalDateTime.now(ZONE));
            cardAuthorizationRepository.save(ca);
            return new CaptureWebhookResponse("SETTLED");
        }

        // Ścieżka capture-only
        if (req.cardToken() == null || req.amount() == null) {
            throw new IllegalArgumentException("Brak danych do rozliczenia (authorization_code/transaction_id albo card_token+amount).");
        }
        if (req.transactionId() != null && hasCompletedCardPayment(req.transactionId())) {
            return new CaptureWebhookResponse("SETTLED");
        }
        Card card = cardRepository.findByProviderToken(req.cardToken())
                .orElseThrow(() -> new IllegalArgumentException("Karta nie znaleziona dla podanego card_token."));
        Account account = card.getAccount();

        String declineReason = cardLimitDeclineReason(card, req.amount());
        if (declineReason == null) {
            BigDecimal blocked = account.getBlockedFunds() != null ? account.getBlockedFunds() : BigDecimal.ZERO;
            if (account.getBalance().subtract(blocked).compareTo(req.amount()) < 0) {
                declineReason = "Brak wystarczających środków";
            }
        }
        if (declineReason != null) {
            saveRejectedOnce(card, req.transactionId(), req.amount(), req.merchantLabel(), declineReason);
            // Cards system już zdjął kasę z PREPAID — zwracamy ją topupem, bo my odrzuciliśmy płatność
            restorePrepaidBalanceIfNeeded(card, req.amount());
            return new CaptureWebhookResponse("DECLINED");
        }

        account.setBalance(account.getBalance().subtract(req.amount()));
        accountRepository.save(account);
        transactionRepository.save(buildCardPaymentTransaction(card, account, req.amount(), account.getCurrency(),
                req.transactionId(), req.merchantLabel(), "COMPLETED", LocalDateTime.now(ZONE)));

        return new CaptureWebhookResponse("SETTLED");
    }

    @Transactional
    public RefundWebhookResponse refund(RefundWebhookRequest req) {
        if (req.cardToken() == null || req.amount() == null
                || req.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Wymagane: card_token i dodatnia amount.");
        }
        Card card = cardRepository.findByProviderToken(req.cardToken())
                .orElseThrow(() -> new IllegalArgumentException("Karta nie znaleziona dla podanego card_token."));
        Account account = card.getAccount();

        account.setBalance(account.getBalance().add(req.amount()));
        accountRepository.save(account);

        Transaction tx = Transaction.builder()
                .receiverAccount(account)
                .receiverAccountNumber(account.getAccountNumber())
                .card(card)
                .title("Zwrot karty")
                .amount(req.amount())
                .currency(account.getCurrency())
                .type("CARD_REFUND")
                .status("COMPLETED")
                .externalPaymentId(req.originalTransactionId())
                .executionDate(LocalDateTime.now(ZONE))
                .build();
        transactionRepository.save(tx);

        return new RefundWebhookResponse("REFUNDED");
    }

    // ---- helpers ----

    /**
     * Cards system optymistycznie zdejmuje kasę z PREPAID przed wynikiem od banku.
     * Gdy MY odrzucamy płatność, oddajemy tę kwotę z powrotem na PREPAID przez topup.
     * Best-effort: jeśli provider nie odpowiada, nie pogarszamy odpowiedzi DECLINED.
     */
    private void restorePrepaidBalanceIfNeeded(Card card, BigDecimal amount) {
        if (card == null || !"PREPAID".equals(card.getType())
                || card.getProviderToken() == null || amount == null) {
            return;
        }
        try {
            cardsProviderClient.topupCard(card.getProviderToken(),
                    new TopupCardRequest(amount.doubleValue(), card.getCurrency()));
        } catch (Exception ignored) {
            // log w przyszłości; teraz silently best-effort
        }
    }

    private String cardLimitDeclineReason(Card card, BigDecimal amount) {
        if (card == null) return null;
        if (card.isBlocked()) {
            return "Karta jest zablokowana";
        }
        if (card.getExpiryDate() != null && card.getExpiryDate().isBefore(LocalDate.now(ZONE))) {
            return "Karta wygasła";
        }
        if (card.getTransactionLimit() != null && amount.compareTo(card.getTransactionLimit()) > 0) {
            return "Limit transakcji karty (" + card.getTransactionLimit() + " " + card.getCurrency() + ") przekroczony";
        }
        if (card.getDailyLimit() != null) {
            BigDecimal spentToday = cardLimitService.sumSpentToday(card.getId());
            if (spentToday.add(amount).compareTo(card.getDailyLimit()) > 0) {
                return "Limit dzienny karty przekroczony (wydano dziś " + spentToday + ", limit " + card.getDailyLimit() + ")";
            }
        }
        return null;
    }

    private String mapDeclineReasonToCode(String reason) {
        if (reason == null) return "DECLINED";
        if (reason.startsWith("Karta jest zablokowana")) return "CARD_BLOCKED";
        if (reason.startsWith("Karta wygasła")) return "CARD_EXPIRED";
        if (reason.startsWith("Limit transakcji")) return "TRANSACTION_LIMIT_EXCEEDED";
        if (reason.startsWith("Limit dzienny")) return "DAILY_LIMIT_EXCEEDED";
        return "DECLINED";
    }

    private Card resolveCardForAccount(Account account, String lastDigits) {
        List<Card> cards = cardRepository.findByAccount_Id(account.getId());
        if (cards.isEmpty()) return null;
        if (lastDigits != null && !lastDigits.isBlank()) {
            String digits = lastDigits.replaceAll("\\D", "");
            if (!digits.isEmpty()) {
                Card match = cards.stream()
                        .filter(c -> {
                            String pan = c.getMaskedPan() != null ? c.getMaskedPan() : c.getCardNumber();
                            if (pan == null) return false;
                            return pan.replaceAll("\\D", "").endsWith(digits);
                        })
                        .findFirst()
                        .orElse(null);
                if (match != null) return match;
            }
        }
        return cards.size() == 1 ? cards.get(0) : null;
    }

    private void saveRejectedOnce(Card card, String transactionId, BigDecimal amount, String merchant, String reason) {
        boolean alreadyRejected = transactionId != null && !transactionRepository
                .findByExternalPaymentIdAndType(transactionId, "CARD_PAYMENT_REJECTED")
                .isEmpty();
        if (!alreadyRejected) {
            cardTransactionAuditService.saveRejectedCardPayment(card, amount, transactionId, merchant, reason);
        }
    }

    private boolean hasCompletedCardPayment(String externalTransactionId) {
        return transactionRepository
                .findByExternalPaymentIdAndType(externalTransactionId, "CARD_PAYMENT")
                .stream()
                .anyMatch(t -> "COMPLETED".equals(t.getStatus()));
    }

    private Transaction findPendingCardPayment(String externalTransactionId) {
        if (externalTransactionId == null) return null;
        return transactionRepository
                .findByExternalPaymentIdAndType(externalTransactionId, "CARD_PAYMENT")
                .stream()
                .filter(t -> "PENDING".equals(t.getStatus()))
                .findFirst()
                .orElse(null);
    }

    private Transaction buildCardPaymentTransaction(Card card, Account account, BigDecimal amount, String currency,
            String externalId, String merchant, String status, LocalDateTime executionDate) {
        String cardLabel = (card != null && card.getMaskedPan() != null && card.getMaskedPan().length() >= 4)
                ? " (•••• " + card.getMaskedPan().substring(card.getMaskedPan().length() - 4) + ")"
                : "";
        return Transaction.builder()
                .senderAccount(account)
                .senderAccountNumber(account.getAccountNumber())
                .card(card)
                .receiverName(merchant)
                .title("Płatność kartą" + (merchant != null ? ": " + merchant : "") + cardLabel)
                .amount(amount)
                .currency(currency != null ? currency : account.getCurrency())
                .type("CARD_PAYMENT")
                .status(status)
                .externalPaymentId(externalId)
                .executionDate(executionDate)
                .build();
    }

    private Account lookupAccount(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.length() == 28 && trimmed.startsWith("PL")) {
            return accountRepository.findByAccountNumber(trimmed).orElse(null);
        }
        try {
            return accountRepository.findById(UUID.fromString(trimmed)).orElse(null);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}