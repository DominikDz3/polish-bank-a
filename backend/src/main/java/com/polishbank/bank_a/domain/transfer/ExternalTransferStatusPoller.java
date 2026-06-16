package com.polishbank.bank_a.domain.transfer;

import com.polishbank.bank_a.entity.Account;
import com.polishbank.bank_a.entity.ExternalTransfer;
import com.polishbank.bank_a.integration.payments.ElixirClient;
import com.polishbank.bank_a.integration.payments.ExpressElixirClient;
import com.polishbank.bank_a.integration.payments.IsoXml;
import com.polishbank.bank_a.integration.payments.SorbnetClient;
import com.polishbank.bank_a.repository.AccountRepository;
import com.polishbank.bank_a.repository.ExternalTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ExternalTransferStatusPoller {

    private final ExternalTransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final ElixirClient elixirClient;
    private final ExpressElixirClient expressClient;
    private final SorbnetClient sorbnetClient;

    @Scheduled(fixedDelayString = "${app.elixir.poll-interval-ms}")
    @Transactional
    public void pollPendingTransfers() {
        List<ExternalTransfer> pending = transferRepository.findByStatusIn(
                List.of("SENT", "GRIDLOCK_HELD")
        );
        if (pending.isEmpty()) return;

        for (ExternalTransfer t : pending) {
            try {
                switch (t.getRoutingSystem()) {
                    case "ELIXIR" -> pollElixir(t);
                    case "EXPRESS" -> pollExpress(t);
                    case "SORBNET" -> pollSorbnet(t);
                }
            } catch (Exception e) {
                System.err.println("Poll failed for " + t.getExternalPaymentId() + ": " + e.getMessage());
            }
        }
    }

    private void pollElixir(ExternalTransfer t) {
        for (String status : List.of("PROCESSED", "REJECTED", "BLOCKED", "WAITING_FOR_LIQUIDITY", "IN_SESSION")) {
            List<Map<String, Object>> list = elixirClient.getByStatus(status);
            if (list == null) continue;
            for (Map<String, Object> p : list) {
                if (t.getExternalPaymentId().equals(p.get("paymentId"))) {
                    applyStatus(t, status, null);
                    return;
                }
            }
        }
    }

    private void pollExpress(ExternalTransfer t) {
        try {
            IsoXml.ParsedResponse r = expressClient.getPaymentStatus(t.getExternalPaymentId());
            applyStatus(t, r.status(), r.reason());
        } catch (Exception ignored) {
        }
    }

    private void pollSorbnet(ExternalTransfer t) {
        try {
            IsoXml.ParsedResponse r = sorbnetClient.getPaymentStatus(t.getExternalPaymentId());
            applyStatus(t, r.status(), r.reason());
        } catch (Exception ignored) {
        }
    }

    private void applyStatus(ExternalTransfer t, String externalStatus, String reason) {
        Account account = t.getSenderAccount();
        switch (externalStatus) {
            case "SETTLED", "PROCESSED" -> {
                if (!"PROCESSED".equals(t.getStatus())) {
                    t.setStatus("PROCESSED");
                    t.setSettledAt(LocalDateTime.now());
                    account.setBlockedFunds(account.getBlockedFunds().subtract(t.getAmount()));
                    accountRepository.save(account);
                }
            }
            case "REJECTED", "BLOCKED" -> {
                if (!"REJECTED".equals(t.getStatus())) {
                    t.setStatus("REJECTED");
                    t.setRejectionReason(reason);
                    account.setBalance(account.getBalance().add(t.getAmount()));
                    account.setBlockedFunds(account.getBlockedFunds().subtract(t.getAmount()));
                    accountRepository.save(account);
                }
            }
            case "GRIDLOCK_HELD", "WAITING_FOR_LIQUIDITY" -> t.setStatus("GRIDLOCK_HELD");
            case "IN_SESSION", "NETTING_SENT", "QUEUED" -> t.setStatus("SENT");
        }
        transferRepository.save(t);
    }
}