package com.polishbank.bank_a.domain.aml.rules;

import com.polishbank.bank_a.domain.aml.*;
import org.springframework.stereotype.Component;

@Component
public class StructuringRule implements AmlRule {
    @Override public String code() { return "STRUCTURING"; }

    @Override
    public AmlResult check(AmlContext ctx, AmlSupportData support) {
        if (support.recentTransfersToSameReceiver() > 5) {
            return AmlResult.hold(code(),
                "Wykryto wzorzec structuring: " + support.recentTransfersToSameReceiver() +
                " przelewów do tego samego odbiorcy w ostatniej godzinie.");
        }
        return AmlResult.ok();
    }
}