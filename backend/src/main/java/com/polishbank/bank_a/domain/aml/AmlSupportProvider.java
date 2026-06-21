package com.polishbank.bank_a.domain.aml;

import com.polishbank.bank_a.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AmlSupportProvider {

    private final TransactionRepository transactionRepository;

    public AmlSupportData compute(AmlContext ctx) {
        if (ctx.receiverIdentifier() == null || ctx.accountId() == null) {
            return new AmlSupportData(0, false);
        }
        LocalDateTime hourAgo = LocalDateTime.now().minusHours(1);

        long recent = transactionRepository
                .countBySenderAccount_IdAndReceiverAccountNumberAndCreatedAtAfter(
                        ctx.accountId(), ctx.receiverIdentifier(), hourAgo);

        long totalPast = transactionRepository
                .countBySenderAccount_IdAndReceiverAccountNumber(
                        ctx.accountId(), ctx.receiverIdentifier());

        boolean isNew = totalPast == 0;
        return new AmlSupportData(recent, isNew);
    }
}