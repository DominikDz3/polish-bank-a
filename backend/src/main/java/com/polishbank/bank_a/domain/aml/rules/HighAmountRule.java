package com.polishbank.bank_a.domain.aml.rules;

import com.polishbank.bank_a.domain.aml.*;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class HighAmountRule implements AmlRule {
    private static final BigDecimal THRESHOLD = new BigDecimal("15000");

    @Override public String code() { return "HIGH_AMOUNT"; }

    @Override
    public AmlResult check(AmlContext ctx, AmlSupportData support) {
        if (ctx.type() == AmlTransactionType.SWIFT) return AmlResult.ok();
        if (ctx.amount().compareTo(THRESHOLD) >= 0) {
            return AmlResult.hold(code(),
                "Kwota przelewu (" + ctx.amount() + " " + ctx.currency() +
                ") przekracza próg raportowania AML (15 000 PLN).");
        }
        return AmlResult.ok();
    }
}