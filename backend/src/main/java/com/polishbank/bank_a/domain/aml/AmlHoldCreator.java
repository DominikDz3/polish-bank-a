package com.polishbank.bank_a.domain.aml;

import com.polishbank.bank_a.domain.user.User;
import com.polishbank.bank_a.entity.*;
import com.polishbank.bank_a.repository.AmlHoldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class AmlHoldCreator {

    private final AmlHoldRepository holdRepository;

    public AmlHold create(
            User user,
            Account account,
            AmlTransactionType type,
            Transaction transaction,
            ExternalTransfer externalTransfer,
            SwiftTransfer swiftTransfer,
            AmlResult eval,
            BigDecimal amount,
            String currency,
            String receiverInfo
    ) {
        AmlHold h = AmlHold.builder()
                .user(user)
                .account(account)
                .holdType(type.name())
                .transaction(transaction)
                .externalTransfer(externalTransfer)
                .swiftTransfer(swiftTransfer)
                .reason(eval.reason())
                .triggeredRule(eval.ruleCode())
                .amount(amount)
                .currency(currency)
                .receiverInfo(receiverInfo)
                .status(AmlHoldStatus.AWAITING_EXPLANATION)
                .createdBy("AML_ENGINE")
                .build();
        return holdRepository.save(h);
    }
}