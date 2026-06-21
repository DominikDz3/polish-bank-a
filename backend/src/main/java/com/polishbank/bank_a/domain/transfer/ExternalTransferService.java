package com.polishbank.bank_a.domain.transfer;

import com.polishbank.bank_a.domain.auth.PinService;
import com.polishbank.bank_a.domain.transfer.dto.ExternalTransferRequest;
import com.polishbank.bank_a.domain.transfer.dto.ExternalTransferResponse;
import com.polishbank.bank_a.domain.user.User;
import com.polishbank.bank_a.domain.user.UserRepository;
import com.polishbank.bank_a.entity.Account;
import com.polishbank.bank_a.entity.ExternalTransfer;
import com.polishbank.bank_a.integration.payments.ElixirClient;
import com.polishbank.bank_a.integration.payments.ExpressElixirClient;
import com.polishbank.bank_a.integration.payments.IsoXml;
import com.polishbank.bank_a.integration.payments.SorbnetClient;
import com.polishbank.bank_a.repository.AccountRepository;
import com.polishbank.bank_a.repository.ExternalTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.polishbank.bank_a.domain.aml.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExternalTransferService {

    private final ExternalTransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final PinService pinService;
    private final ElixirClient elixirClient;
    private final ExpressElixirClient expressClient;
    private final SorbnetClient sorbnetClient;
    private final AmlEvaluator amlEvaluator;
    private final AmlHoldCreator amlHoldCreator;

    @Value("${app.bank.bicfi}")
    private String senderBicfi;

    @Value("${app.bank.sorbnet-account}")
    private String sorbnetAccount;

    @Value("${app.bank.elixir-account}")
    private String elixirAccount;

    @Transactional
    public ExternalTransferResponse createTransfer(ExternalTransferRequest req, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        Account senderAccount = accountRepository.findById(req.senderAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Konto nadawcy nie istnieje."));

        if (!senderAccount.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Brak dostępu do tego konta.");
        }
        pinService.verifyPin(user, req.pin());

        if (senderAccount.getBalance().compareTo(req.amount()) < 0) {
            throw new IllegalStateException("Niewystarczające środki na koncie.");
        }

        String externalPaymentId = generatePaymentId(req.routingSystem());
        String senderName = user.getFirstName() + " " + user.getLastName();

        senderAccount.setBalance(senderAccount.getBalance().subtract(req.amount()));
        senderAccount.setBlockedFunds(senderAccount.getBlockedFunds().add(req.amount()));
        accountRepository.save(senderAccount);

        ExternalTransfer transfer = ExternalTransfer.builder()
                .externalPaymentId(externalPaymentId)
                .senderAccount(senderAccount)
                .senderAccountNumber(senderAccount.getAccountNumber())
                .senderName(senderName)
                .receiverAccountNumber(req.receiverAccountNumber())
                .receiverName(req.receiverName())
                .receiverBankBicfi(req.receiverBankBicfi())
                .amount(req.amount())
                .currency(senderAccount.getCurrency())
                .title(req.title())
                .routingSystem(req.routingSystem())
                .status("INITIATED")
                .build();
        transferRepository.save(transfer);

        AmlResult eval = amlEvaluator.evaluate(new AmlContext(
            user.getId(),
            senderAccount.getId(),
            req.amount(),
            senderAccount.getCurrency(),
            AmlTransactionType.EXTERNAL,
            req.title(),
            req.receiverAccountNumber(),
            null
        ));
        if (eval.hold()) {
            transfer.setStatus("HELD_FOR_AML");
            transferRepository.save(transfer);
            amlHoldCreator.create(user, senderAccount, AmlTransactionType.EXTERNAL,
                    null, transfer, null, eval, req.amount(), senderAccount.getCurrency(),
                    req.receiverAccountNumber());
            return toResponse(transfer);
        }

        String senderIbanForXml = switch (req.routingSystem()) {
            case "SORBNET" -> sorbnetAccount;
            case "ELIXIR" -> elixirAccount;
            case "EXPRESS" -> senderAccount.getAccountNumber();
            default -> senderAccount.getAccountNumber();
        };

        String xml = IsoXml.buildPacs008(
                externalPaymentId,
                senderBicfi,
                req.receiverBankBicfi(),
                senderIbanForXml,
                req.receiverAccountNumber(),
                senderName,
                req.receiverName(),
                req.amount(),
                senderAccount.getCurrency(),
                req.title(),
                serviceCodeFor(req.routingSystem())
        );

        try {
            switch (req.routingSystem()) {
                case "ELIXIR" -> {
                    elixirClient.sendPayment(xml);
                    transfer.setStatus("SENT");
                    transfer.setSentAt(LocalDateTime.now());
                }
                case "EXPRESS" -> {
                    IsoXml.ParsedResponse r = expressClient.sendPayment(
                            externalPaymentId,
                            senderBicfi,
                            req.receiverBankBicfi(),
                            senderAccount.getAccountNumber(),
                            req.receiverAccountNumber(),
                            senderName,
                            req.receiverName(),
                            req.amount(),
                            senderAccount.getCurrency(),
                            req.title()
                    );
                    transfer.setSentAt(LocalDateTime.now());
                    applyImmediateResponse(transfer, senderAccount, r);
                }
                case "SORBNET" -> {
                    IsoXml.ParsedResponse r = sorbnetClient.sendPayment(xml);
                    transfer.setSentAt(LocalDateTime.now());
                    applyImmediateResponse(transfer, senderAccount, r);
                }
                default -> throw new IllegalArgumentException("Nieznany system: " + req.routingSystem());
            }
        } catch (Exception e) {
            senderAccount.setBalance(senderAccount.getBalance().add(req.amount()));
            senderAccount.setBlockedFunds(senderAccount.getBlockedFunds().subtract(req.amount()));
            accountRepository.save(senderAccount);
            transfer.setStatus("REJECTED");
            transfer.setRejectionReason("Błąd komunikacji: " + e.getMessage());
            transferRepository.save(transfer);
            throw new IllegalStateException("Nie udało się wysłać przelewu: " + e.getMessage());
        }

        transferRepository.save(transfer);
        return toResponse(transfer);
    }

    private void applyImmediateResponse(ExternalTransfer transfer, Account senderAccount, IsoXml.ParsedResponse r) {
        switch (r.status()) {
            case "SETTLED", "PROCESSED" -> {
                transfer.setStatus("PROCESSED");
                transfer.setSettledAt(LocalDateTime.now());
                senderAccount.setBlockedFunds(senderAccount.getBlockedFunds().subtract(transfer.getAmount()));
                accountRepository.save(senderAccount);
            }
            case "REJECTED" -> {
                transfer.setStatus("REJECTED");
                transfer.setRejectionReason(r.reason());
                senderAccount.setBalance(senderAccount.getBalance().add(transfer.getAmount()));
                senderAccount.setBlockedFunds(senderAccount.getBlockedFunds().subtract(transfer.getAmount()));
                accountRepository.save(senderAccount);
            }
            case "GRIDLOCK_HELD" -> transfer.setStatus("GRIDLOCK_HELD");
            default -> transfer.setStatus("SENT");
        }
    }

    public List<ExternalTransferResponse> getHistory(UUID accountId, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        Account account = accountRepository.findById(accountId).orElseThrow();
        if (!account.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Brak dostępu do tego konta.");
        }
        return transferRepository.findBySenderAccount_IdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ExternalTransferResponse toResponse(ExternalTransfer t) {
        return new ExternalTransferResponse(
                t.getId(),
                t.getExternalPaymentId(),
                t.getSenderAccountNumber(),
                t.getReceiverAccountNumber(),
                t.getReceiverName(),
                t.getReceiverBankBicfi(),
                t.getAmount(),
                t.getCurrency(),
                t.getTitle(),
                t.getRoutingSystem(),
                t.getStatus(),
                t.getRejectionReason(),
                t.getCreatedAt(),
                t.getSettledAt()
        );
    }

    private String generatePaymentId(String system) {
        String prefix = switch (system) {
            case "ELIXIR" -> "ELIX";
            case "EXPRESS" -> "EXP";
            case "SORBNET" -> "SORB";
            default -> "TRF";
        };
        return prefix + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String serviceCodeFor(String system) {
        return switch (system) {
            case "ELIXIR" -> "ELIXIR";
            case "EXPRESS" -> "ELIXIR_EXPRESS";
            case "SORBNET" -> "SORBNET";
            default -> "ELIXIR";
        };
    }

    @Transactional
public void finalizeAfterAml(UUID transferId) {
    ExternalTransfer transfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new IllegalArgumentException("Przelew nie istnieje."));
    if (!"HELD_FOR_AML".equals(transfer.getStatus())) {
        throw new IllegalStateException("Przelew nie jest w stanie HELD_FOR_AML.");
    }
    Account senderAccount = transfer.getSenderAccount();
    String senderName = transfer.getSenderName();

    String senderIbanForXml = switch (transfer.getRoutingSystem()) {
        case "SORBNET" -> sorbnetAccount;
        case "ELIXIR" -> elixirAccount;
        default -> senderAccount.getAccountNumber();
    };
    String xml = IsoXml.buildPacs008(
            transfer.getExternalPaymentId(),
            senderBicfi, transfer.getReceiverBankBicfi(),
            senderIbanForXml, transfer.getReceiverAccountNumber(),
            senderName, transfer.getReceiverName(),
            transfer.getAmount(), senderAccount.getCurrency(),
            transfer.getTitle(), serviceCodeFor(transfer.getRoutingSystem())
    );

    try {
        switch (transfer.getRoutingSystem()) {
            case "ELIXIR" -> {
                elixirClient.sendPayment(xml);
                transfer.setStatus("SENT");
                transfer.setSentAt(LocalDateTime.now());
            }
            case "EXPRESS" -> {
                IsoXml.ParsedResponse r = expressClient.sendPayment(
                        transfer.getExternalPaymentId(), senderBicfi, transfer.getReceiverBankBicfi(),
                        senderAccount.getAccountNumber(), transfer.getReceiverAccountNumber(),
                        senderName, transfer.getReceiverName(), transfer.getAmount(),
                        senderAccount.getCurrency(), transfer.getTitle());
                transfer.setSentAt(LocalDateTime.now());
                applyImmediateResponse(transfer, senderAccount, r);
            }
            case "SORBNET" -> {
                IsoXml.ParsedResponse r = sorbnetClient.sendPayment(xml);
                transfer.setSentAt(LocalDateTime.now());
                applyImmediateResponse(transfer, senderAccount, r);
            }
        }
    } catch (Exception e) {
        senderAccount.setBalance(senderAccount.getBalance().add(transfer.getAmount()));
        senderAccount.setBlockedFunds(senderAccount.getBlockedFunds().subtract(transfer.getAmount()));
        accountRepository.save(senderAccount);
        transfer.setStatus("REJECTED");
        transfer.setRejectionReason("Błąd po AML: " + e.getMessage());
    }
    transferRepository.save(transfer);
}

    @Transactional
    public void cancelAfterAml(UUID transferId) {
        ExternalTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Przelew nie istnieje."));
        if (!"HELD_FOR_AML".equals(transfer.getStatus())) {
            throw new IllegalStateException("Przelew nie jest w stanie HELD_FOR_AML.");
        }
        Account senderAccount = transfer.getSenderAccount();
        senderAccount.setBalance(senderAccount.getBalance().add(transfer.getAmount()));
        senderAccount.setBlockedFunds(senderAccount.getBlockedFunds().subtract(transfer.getAmount()));
        accountRepository.save(senderAccount);

        transfer.setStatus("REJECTED_AML");
        transfer.setRejectionReason("Odrzucono przez AML");
        transferRepository.save(transfer);
    }
}