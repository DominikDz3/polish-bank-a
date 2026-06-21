package com.polishbank.bank_a.domain.aml.rules;

import com.polishbank.bank_a.domain.aml.*;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class HighRiskCountryRule implements AmlRule {
    private static final Set<String> HIGH_RISK = Set.of(
        "RU", "BY", "KP", "IR", "SY", "AF", "MM", "VE", "CU", "SD", "SS", "YE"
    );

    @Override public String code() { return "HIGH_RISK_COUNTRY"; }

    @Override
    public AmlResult check(AmlContext ctx, AmlSupportData support) {
        if (ctx.receiverCountry() == null) return AmlResult.ok();
        if (HIGH_RISK.contains(ctx.receiverCountry().toUpperCase())) {
            return AmlResult.hold(code(),
                "Kraj odbiorcy (" + ctx.receiverCountry() +
                ") znajduje się na liście jurysdykcji wysokiego ryzyka (FATF / sankcje UE).");
        }
        return AmlResult.ok();
    }
}