package com.polishbank.bank_a.domain.aml;

import com.polishbank.bank_a.domain.swift.SwiftService;
import com.polishbank.bank_a.domain.transaction.TransactionService;
import com.polishbank.bank_a.domain.transfer.ExternalTransferService;
import com.polishbank.bank_a.entity.AmlHold;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AmlDecisionExecutor {

    private final TransactionService transactionService;
    private final ExternalTransferService externalTransferService;
    private final SwiftService swiftService;

    public void executeApprove(AmlHold hold) {
        AmlTransactionType type = AmlTransactionType.valueOf(hold.getHoldType());
        log.info("[AML_APPROVE] holdId={} type={}", hold.getId(), type);
        switch (type) {
            case INTERNAL -> transactionService.finalizeAfterAml(hold.getTransaction().getId());
            case EXTERNAL -> externalTransferService.finalizeAfterAml(hold.getExternalTransfer().getId());
            case SWIFT -> swiftService.finalizeAfterAml(hold.getSwiftTransfer().getId());
            case KLIK_P2P -> finalizeKlikP2P(hold);
        }
    }

    public void executeReject(AmlHold hold) {
        AmlTransactionType type = AmlTransactionType.valueOf(hold.getHoldType());
        log.info("[AML_REJECT] holdId={} type={}", hold.getId(), type);
        switch (type) {
            case INTERNAL -> transactionService.cancelAfterAml(hold.getTransaction().getId());
            case EXTERNAL -> externalTransferService.cancelAfterAml(hold.getExternalTransfer().getId());
            case SWIFT -> swiftService.cancelAfterAml(hold.getSwiftTransfer().getId());
            case KLIK_P2P -> cancelKlikP2P(hold);
        }
    }

    private void finalizeKlikP2P(AmlHold hold) {
        if (hold.getTransaction() != null) {
            transactionService.finalizeAfterAml(hold.getTransaction().getId());
        } else if (hold.getExternalTransfer() != null) {
            externalTransferService.finalizeAfterAml(hold.getExternalTransfer().getId());
        } else {
            throw new IllegalStateException("KLIK P2P hold nie ma powiązanej transakcji ani transferu.");
        }
    }

    private void cancelKlikP2P(AmlHold hold) {
        if (hold.getTransaction() != null) {
            transactionService.cancelAfterAml(hold.getTransaction().getId());
        } else if (hold.getExternalTransfer() != null) {
            externalTransferService.cancelAfterAml(hold.getExternalTransfer().getId());
        }
    }
}