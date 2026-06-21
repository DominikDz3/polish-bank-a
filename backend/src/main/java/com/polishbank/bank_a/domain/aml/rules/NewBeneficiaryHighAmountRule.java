package com.polishbank.bank_a.domain.aml.rules;

import com.polishbank.bank_a.domain.aml.*;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class NewBeneficiaryHighAmountRule implements AmlRule {
    private static final BigDecimal THRESHOLD = new BigDecimal("5000");

    @Override public String code() { return "NEW_BENEFICIARY_HIGH_AMOUNT"; }

    @Override
    public AmlResult check(AmlContext ctx, AmlSupportData support) {
        if (!support.isNewBeneficiary()) return AmlResult.ok();
        if (ctx.amount().compareTo(THRESHOLD) >= 0) {
            return AmlResult.hold(code(),
                "Pierwsza transakcja do tego odbiorcy z kwotą " + ctx.amount() + " " +
                ctx.currency() + " (próg: " + THRESHOLD + " PLN).");
        }
        return AmlResult.ok();
    }
}