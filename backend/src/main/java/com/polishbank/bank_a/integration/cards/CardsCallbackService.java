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

    @Transactional
    public AuthorizeWebhookResponse authorize(AuthorizeWebhookRequest req) {
        if (req.amount() == null || req.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return AuthorizeWebhookResponse.declined("INVALID_AMOUNT");
        }
        if (req.transactionId() == null || req.transactionId().isBlank()) {
            return AuthorizeWebhookResponse.declined("INVALID_REQUEST");
        }

        // Idempotencja: drugi raz z tym samym transaction_id zwraca zapisaną decyzję
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
            try {
                account = accountRepository.findById(UUID.fromString(req.accountId())).orElse(null);
            } catch (IllegalArgumentException ignored) {
                // niewłaściwy UUID
            }
            if (account == null) {
                return AuthorizeWebhookResponse.declined("ACCOUNT_NOT_FOUND");
            }
        } else {
            return AuthorizeWebhookResponse.declined("INVALID_REQUEST");
        }

        if (card != null) {
            if (card.isBlocked()) {
                return AuthorizeWebhookResponse.declined("CARD_BLOCKED");
            }
            if (card.getExpiryDate() != null && card.getExpiryDate().isBefore(LocalDate.now(ZONE))) {
                return AuthorizeWebhookResponse.declined("CARD_EXPIRED");
            }
            if (card.getTransactionLimit() != null
                    && req.amount().compareTo(card.getTransactionLimit()) > 0) {
                return AuthorizeWebhookResponse.declined("TRANSACTION_LIMIT_EXCEEDED");
            }
            if (card.getDailyLimit() != null) {
                BigDecimal spentToday = cardLimitService.sumSpentToday(card.getId());
                if (spentToday.add(req.amount()).compareTo(card.getDailyLimit()) > 0) {
                    return AuthorizeWebhookResponse.declined("DAILY_LIMIT_EXCEEDED");
                }
            }
        }

        BigDecimal blocked = account.getBlockedFunds() != null ? account.getBlockedFunds() : BigDecimal.ZERO;
        BigDecimal available = account.getBalance().subtract(blocked);
        if (available.compareTo(req.amount()) < 0) {
            return AuthorizeWebhookResponse.declined("INSUFFICIENT_FUNDS");
        }

        account.setBlockedFunds(blocked.add(req.amount()));
        accountRepository.save(account);

        String authCode = "AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        CardAuthorization ca = CardAuthorization.builder()
                .authorizationCode(authCode)
                .externalTransactionId(req.transactionId())
                .card(card)
                .account(account)
                .amount(req.amount())
                .currency(req.currency() != null ? req.currency() : account.getCurrency())
                .merchantName(req.merchantName())
                .status("HELD")
                .build();
        cardAuthorizationRepository.save(ca);

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

        // Idempotencja
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

            saveCardTransaction(ca.getCard(), account, amount, "CARD_PAYMENT",
                    ca.getExternalTransactionId(), ca.getMerchantName());

            ca.setStatus("SETTLED");
            ca.setSettledAt(LocalDateTime.now(ZONE));
            cardAuthorizationRepository.save(ca);
            return new CaptureWebhookResponse("SETTLED");
        }

        // Fallback gdy nie było /authorize: direct charge po card_token + amount
        if (req.cardToken() == null || req.amount() == null) {
            throw new IllegalArgumentException("Brak danych do rozliczenia (authorization_code/transaction_id albo card_token+amount).");
        }
        Card card = cardRepository.findByProviderToken(req.cardToken())
                .orElseThrow(() -> new IllegalArgumentException("Karta nie znaleziona dla podanego card_token."));
        Account account = card.getAccount();
        account.setBalance(account.getBalance().subtract(req.amount()));
        accountRepository.save(account);
                saveCardTransaction(card, account, req.amount(), "CARD_PAYMENT",
                req.transactionId(), req.merchantLabel());

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

    private void saveCardTransaction(Card card, Account account, BigDecimal amount,
                                     String type, String externalId, String merchant) {
        Transaction tx = Transaction.builder()
                .senderAccount(account)
                .senderAccountNumber(account.getAccountNumber())
                .card(card)
                .receiverName(merchant)
                .title("Płatność kartą" + (merchant != null ? ": " + merchant : ""))
                .amount(amount)
                .currency(account.getCurrency())
                .type(type)
                .status("COMPLETED")
                .externalPaymentId(externalId)
                .executionDate(LocalDateTime.now(ZONE))
                .build();
        transactionRepository.save(tx);
    }
}