package com.polishbank.bank_a.domain.aml.rules;

import com.polishbank.bank_a.domain.aml.*;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Map;

@Component
public class SwiftHighAmountRule implements AmlRule {
    private static final Map<String, BigDecimal> THRESHOLDS = Map.of(
        "EUR", new BigDecimal("5000"),
        "USD", new BigDecimal("6000"),
        "GBP", new BigDecimal("4000"),
        "PLN", new BigDecimal("15000")
    );

    @Override public String code() { return "SWIFT_HIGH_AMOUNT"; }

    @Override
    public AmlResult check(AmlContext ctx, AmlSupportData support) {
        if (ctx.type() != AmlTransactionType.SWIFT) return AmlResult.ok();
        BigDecimal threshold = THRESHOLDS.getOrDefault(ctx.currency(), new BigDecimal("5000"));
        if (ctx.amount().compareTo(threshold) >= 0) {
            return AmlResult.hold(code(),
                "Międzynarodowy przelew SWIFT (" + ctx.amount() + " " + ctx.currency() +
                ") przekracza próg " + threshold + " " + ctx.currency() + ".");
        }
        return AmlResult.ok();
    }
}