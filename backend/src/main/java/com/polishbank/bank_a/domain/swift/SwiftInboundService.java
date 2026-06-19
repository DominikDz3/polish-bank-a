package com.polishbank.bank_a.domain.swift;

import com.polishbank.bank_a.entity.Account;
import com.polishbank.bank_a.entity.SwiftTransfer;
import com.polishbank.bank_a.entity.Transaction;
import com.polishbank.bank_a.integration.swift.SwiftExchangeRateProvider;
import com.polishbank.bank_a.integration.swift.SwiftInboundParser;
import com.polishbank.bank_a.integration.swift.SwiftProperties;
import com.polishbank.bank_a.repository.AccountRepository;
import com.polishbank.bank_a.repository.SwiftTransferRepository;
import com.polishbank.bank_a.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class SwiftInboundService {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    private final SwiftInboundParser parser;
    private final SwiftTransferRepository swiftTransferRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final SwiftProperties properties;
    private final SwiftExchangeRateProvider exchangeRateProvider;

    @Transactional
    public ReturnResult handleReturn(String xml, String uetrHeader) {
        SwiftInboundParser.ParsedMessage parsed;
        try {
            parsed = parser.parse(xml);
        } catch (IllegalArgumentException e) {
            log.warn("[SWIFT_RETURN] Niepoprawny XML: {}", e.getMessage());
            return new ReturnResult(false, uetrHeader);
        }
        String uetr = parsed.uetr() == null || parsed.uetr().isBlank() ? uetrHeader : parsed.uetr();
        if (uetr == null || uetr.isBlank()) {
            log.warn("[SWIFT_RETURN] Brak UETR w XML i nagłówku");
            return new ReturnResult(false, null);
        }
        var swiftOpt = swiftTransferRepository.findByUetr(uetr);
        if (swiftOpt.isEmpty()) {
            log.warn("[SWIFT_RETURN] Brak transakcji dla UETR={}", uetr);
            return new ReturnResult(false, uetr);
        }
        SwiftTransfer swift = swiftOpt.get();
        if ("RETURNED".equals(swift.getStatus())) {
            log.info("[SWIFT_RETURN] UETR={} już oznaczony jako RETURNED, pomijam", uetr);
            return new ReturnResult(true, uetr);
        }
        Transaction tx = swift.getTransaction();
        Account sender = tx == null ? null : tx.getSenderAccount();
                if (sender != null && tx != null) {
            BigDecimal refund = exchangeRateProvider.convert(
                    tx.getAmount(), tx.getCurrency(), sender.getCurrency());
            sender.setBalance(sender.getBalance().add(refund));
            accountRepository.save(sender);
            tx.setStatus("RETURNED");
            transactionRepository.save(tx);
        }
        swift.setStatus("RETURNED");
        swift.setReturnReason(parsed.returnReason() == null ? "Zwrot przelewu" : parsed.returnReason());
        swift.setReturnedAt(LocalDateTime.now(ZONE));
        swiftTransferRepository.save(swift);
        log.info("[SWIFT_RETURN] Zwrot przelewu UETR={} reason={}", uetr, swift.getReturnReason());
        return new ReturnResult(true, uetr);
    }

    @Transactional
    public IncomingResult handleIncoming(String xml, String uetrHeader) {
        SwiftInboundParser.ParsedMessage parsed;
        try {
            parsed = parser.parse(xml);
        } catch (IllegalArgumentException e) {
            return new IncomingResult(false, null, null, "rejected", 400, "invalid_xml");
        }

        if (parsed.receiverBic() != null
                && !properties.bic().equalsIgnoreCase(parsed.receiverBic())) {
            log.warn("[SWIFT_INBOUND] Otrzymano komunikat dla obcego BIC={}", parsed.receiverBic());
            return new IncomingResult(false, parsed.uetr(), null, "rejected", 422, "wrong_bic");
        }

        if (parsed.receiverIban() == null || parsed.receiverIban().isBlank()) {
            return new IncomingResult(false, parsed.uetr(), null, "rejected", 422, "missing_iban");
        }

        var accountOpt = accountRepository.findByAccountNumber(parsed.receiverIban());
        if (accountOpt.isEmpty()) {
            log.warn("[SWIFT_INBOUND] Konto odbiorcy nieznane: {}", parsed.receiverIban());
            return new IncomingResult(false, parsed.uetr(), parsed.receiverIban(),
                    "rejected", 404, "receiver_account_not_found");
        }
        Account account = accountOpt.get();
        BigDecimal amount = parsed.amount() == null ? BigDecimal.ZERO : parsed.amount();
        if (amount.signum() <= 0) {
            return new IncomingResult(false, parsed.uetr(), parsed.receiverIban(),
                    "rejected", 400, "invalid_amount");
        }
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        Transaction tx = Transaction.builder()
                .senderAccountNumber("SWIFT")
                .receiverAccount(account)
                .receiverAccountNumber(parsed.receiverIban())
                .receiverBankBic(properties.bic())
                .receiverName(account.getUser().getFirstName() + " " + account.getUser().getLastName())
                .title(parsed.remittanceInfo() == null ? "Przelew SWIFT" : parsed.remittanceInfo())
                .amount(amount)
                .currency(parsed.currency() == null ? account.getCurrency() : parsed.currency())
                .status("COMPLETED")
                .type("SWIFT")
                .externalPaymentId(parsed.uetr())
                .executionDate(LocalDateTime.now(ZONE))
                .build();
        transactionRepository.save(tx);
        log.info("[SWIFT_INBOUND] Zaksięgowano UETR={} amount={} {} na {}",
                parsed.uetr(), amount, tx.getCurrency(), account.getAccountNumber());
        return new IncomingResult(true, parsed.uetr(), account.getAccountNumber(),
                "accepted", 200, null);
    }

    public record ReturnResult(boolean matched, String uetr) {}

    public record IncomingResult(
            boolean accepted,
            String uetr,
            String creditedAccount,
            String statusLabel,
            int httpStatus,
            String reason
    ) {}
}