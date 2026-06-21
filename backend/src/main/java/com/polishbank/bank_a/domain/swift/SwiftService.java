package com.polishbank.bank_a.domain.swift;

import com.polishbank.bank_a.domain.auth.PinService;
import com.polishbank.bank_a.domain.swift.dto.SwiftTransferRequest;
import com.polishbank.bank_a.domain.swift.dto.SwiftTransferResponse;
import com.polishbank.bank_a.domain.user.User;
import com.polishbank.bank_a.domain.user.UserRepository;
import com.polishbank.bank_a.entity.Account;
import com.polishbank.bank_a.entity.SwiftTransfer;
import com.polishbank.bank_a.entity.Transaction;
import com.polishbank.bank_a.integration.swift.SwiftExchangeRateProvider;
import com.polishbank.bank_a.integration.swift.SwiftMiddlewareClient;
import com.polishbank.bank_a.integration.swift.SwiftMiddlewareException;
import com.polishbank.bank_a.integration.swift.SwiftPacs008Builder;
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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SwiftService {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final SwiftTransferRepository swiftTransferRepository;
    private final UserRepository userRepository;
    private final PinService pinService;
    private final SwiftMiddlewareClient middlewareClient;
    private final SwiftPacs008Builder pacs008Builder;
    private final SwiftProperties properties;
    private final SwiftExchangeRateProvider exchangeRateProvider;

    @Transactional
    public SwiftTransferResponse send(SwiftTransferRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("Użytkownik nie znaleziony."));

        Account sender = accountRepository.findById(request.senderAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono konta nadawcy."));

        if (!sender.getUser().getCustomerNumber().equals(user.getCustomerNumber())) {
            throw new IllegalStateException("Brak uprawnień do tego konta.");
        }
        if ("JUNIOR".equals(sender.getType())) {
            throw new IllegalStateException("Konto Junior nie może wysyłać przelewów SWIFT.");
        }

        pinService.verifyPin(sender.getUser(), request.pin());

        if (!exchangeRateProvider.supports(request.currency())) {
            throw new IllegalStateException(
                    "Waluta " + request.currency() + " nie jest obecnie wspierana w przelewach SWIFT.");
        }
        if (!exchangeRateProvider.supports(sender.getCurrency())) {
            throw new IllegalStateException(
                    "Waluta konta źródłowego (" + sender.getCurrency() + ") nie jest wspierana.");
        }

        BigDecimal debitAmount = exchangeRateProvider.convert(
                request.amount(), request.currency(), sender.getCurrency());

        if (sender.getBalance().compareTo(debitAmount) < 0) {
            throw new IllegalStateException(
                    "Niewystarczające środki. Wymagane: " + debitAmount + " " + sender.getCurrency()
                            + ", dostępne: " + sender.getBalance() + " " + sender.getCurrency() + ".");
        }

        String chargeBearer = mapChargeBearer(request.chargeBearer());

        SwiftPacs008Builder.MessageInput xmlInput = new SwiftPacs008Builder.MessageInput(
                properties.bic(),
                sender.getUser().getFirstName() + " " + sender.getUser().getLastName(),
                sender.getAccountNumber(),
                request.receiverBic(),
                request.receiverName(),
                request.receiverIban(),
                request.receiverCountry(),
                request.amount(),
                request.currency(),
                chargeBearer,
                request.title()
        );
        SwiftPacs008Builder.BuiltMessage built = pacs008Builder.build(xmlInput);

        Transaction transaction = Transaction.builder()
                .senderAccount(sender)
                .senderAccountNumber(sender.getAccountNumber())
                .receiverAccountNumber(request.receiverIban())
                .receiverBankBic(request.receiverBic())
                .receiverName(request.receiverName())
                .title(request.title())
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .status("SENT")
                .type("SWIFT")
                .externalPaymentId(built.uetr())
                .build();

        sender.setBalance(sender.getBalance().subtract(debitAmount));
        accountRepository.save(sender);
        transactionRepository.save(transaction);

        SwiftTransfer swift = SwiftTransfer.builder()
                .transaction(transaction)
                .uetr(built.uetr())
                .messageId(built.messageId())
                .instructionId(built.instructionId())
                .senderBic(properties.bic())
                .receiverBic(request.receiverBic())
                .receiverCountry(request.receiverCountry())
                .receiverIban(request.receiverIban())
                .chargeBearer(chargeBearer)
                .chargeBearerInput(request.chargeBearer())
                .status("SENT")
                .build();
        swiftTransferRepository.save(swift);

        try {
            SwiftMiddlewareClient.SendResult result = middlewareClient.sendMessage(built.xml());
            applyMiddlewareResult(swift, result);
            swift.setStatus("IN_TRANSIT");
            transaction.setStatus("IN_TRANSIT");
            transactionRepository.save(transaction);
            swiftTransferRepository.save(swift);
            log.info("[SWIFT_SENT] uetr={} debit={} {} amount={} {} route={} eta={}s fee={}",
                    swift.getUetr(), debitAmount, sender.getCurrency(),
                    request.amount(), request.currency(),
                    swift.getRoute(), swift.getEstimatedSeconds(), swift.getFeeTotal());
        } catch (SwiftMiddlewareException e) {
            log.warn("[SWIFT_REJECTED] uetr={} status={} code={} message={}",
                    built.uetr(), e.getHttpStatus(), e.getCode(), e.getMessage());
            sender.setBalance(sender.getBalance().add(debitAmount));
            accountRepository.save(sender);
            swift.setStatus("RETURNED");
            swift.setReturnReason(translateReturnReason(e));
            swift.setReturnedAt(LocalDateTime.now(ZONE));
            transaction.setStatus("RETURNED");
            transactionRepository.save(transaction);
            swiftTransferRepository.save(swift);
            throw new IllegalStateException(translateReturnReason(e));
        }

        return toResponse(swift, transaction, debitAmount, sender.getCurrency());
    }

    public List<SwiftTransferResponse> listForUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("Użytkownik nie znaleziony."));

        return swiftTransferRepository.findAll().stream()
                .filter(s -> s.getTransaction() != null
                        && s.getTransaction().getSenderAccount() != null
                        && s.getTransaction().getSenderAccount().getUser().getId().equals(user.getId()))
                .sorted(Comparator.comparing(
                        (SwiftTransfer s) -> s.getCreatedAt() == null ? LocalDateTime.MIN : s.getCreatedAt())
                        .reversed())
                .map(s -> toResponse(s, s.getTransaction()))
                .toList();
    }

    public SwiftTransferResponse getById(UUID id, String userEmail) {
        SwiftTransfer swift = swiftTransferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono przelewu SWIFT."));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("Użytkownik nie znaleziony."));
        Transaction tx = swift.getTransaction();
        if (tx == null || tx.getSenderAccount() == null
                || !tx.getSenderAccount().getUser().getId().equals(user.getId())) {
            throw new SecurityException("Brak dostępu do tego przelewu.");
        }
        return toResponse(swift, tx);
    }

    private void applyMiddlewareResult(SwiftTransfer swift, SwiftMiddlewareClient.SendResult result) {
        if (result.uetr() != null && !result.uetr().isBlank()) {
            swift.setUetr(result.uetr());
            swift.getTransaction().setExternalPaymentId(result.uetr());
        }
        if (result.messageId() != null && !result.messageId().isBlank()) {
            swift.setMessageId(result.messageId());
        }
        if (result.route() != null && !result.route().isEmpty()) {
            swift.setRoute(String.join(">", result.route()));
        }
        if (result.estimatedSeconds() != null) {
            swift.setEstimatedSeconds(result.estimatedSeconds());
        }
        if (result.feeBreakdown() != null) {
            swift.setFeeTotal(result.feeBreakdown().totalFee());
            swift.setFeeSender(result.feeBreakdown().senderFee());
            swift.setFeeReceiver(result.feeBreakdown().receiverFee());
            swift.setFeeIntermediary(result.feeBreakdown().intermediaryFee());
        }
    }

    private String mapChargeBearer(String input) {
        return switch (input.toUpperCase()) {
            case "OUR" -> "DEBT";
            case "BEN" -> "CRED";
            case "SHA" -> "SHAR";
            default -> "SHAR";
        };
    }

    private String translateReturnReason(SwiftMiddlewareException e) {
        return switch (e.getHttpStatus()) {
            case 404 -> "Bank lub konto odbiorcy nie istnieje. Środki zwrócone na konto.";
            case 422 -> "Konto odbiorcy zostało zamknięte. Środki zwrócone na konto.";
            case 403 -> "Brak uprawnień do wysłania w imieniu BIC " + properties.bic() + ".";
            case 401 -> "Błąd autoryzacji w sieci SWIFT. Skontaktuj się z administratorem.";
            case 500, 502, 503, 504 -> "Middleware SWIFT chwilowo niedostępny. Spróbuj ponownie za chwilę.";
            default -> e.getMessage();
        };
    }

        private SwiftTransferResponse toResponse(SwiftTransfer s, Transaction tx) {
        BigDecimal debited = null;
        String debitedCurrency = null;
        if (tx != null && tx.getSenderAccount() != null && tx.getAmount() != null && tx.getCurrency() != null) {
            try {
                debitedCurrency = tx.getSenderAccount().getCurrency();
                debited = exchangeRateProvider.convert(tx.getAmount(), tx.getCurrency(), debitedCurrency);
            } catch (Exception ignored) {
            }
        }
        return toResponse(s, tx, debited, debitedCurrency);
    }

    private SwiftTransferResponse toResponse(SwiftTransfer s, Transaction tx,
                                             BigDecimal debitedAmount, String debitedCurrency) {
        List<String> route = s.getRoute() == null || s.getRoute().isBlank()
                ? List.of()
                : List.of(s.getRoute().split(">"));
        return new SwiftTransferResponse(
                s.getId(),
                tx == null ? null : tx.getId(),
                s.getUetr(),
                s.getMessageId(),
                s.getSenderBic(),
                tx == null ? null : tx.getSenderAccountNumber(),
                s.getReceiverBic(),
                s.getReceiverCountry(),
                s.getReceiverIban(),
                tx == null ? null : tx.getReceiverName(),
                tx == null ? null : tx.getAmount(),
                tx == null ? null : tx.getCurrency(),
                s.getChargeBearerInput(),
                s.getChargeBearer(),
                route,
                s.getFeeTotal(),
                s.getFeeSender(),
                s.getFeeReceiver(),
                s.getFeeIntermediary(),
                s.getEstimatedSeconds(),
                s.getStatus(),
                s.getReturnReason(),
                tx == null ? null : tx.getTitle(),
                s.getCreatedAt(),
                s.getDeliveredAt(),
                s.getReturnedAt(),
                debitedAmount,
                debitedCurrency
        );
    }
}