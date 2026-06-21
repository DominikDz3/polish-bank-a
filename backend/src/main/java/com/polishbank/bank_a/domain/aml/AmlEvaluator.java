package com.polishbank.bank_a.domain.aml;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AmlEvaluator {

    private final List<AmlRule> rules;
    private final AmlSupportProvider supportProvider;

    public AmlResult evaluate(AmlContext ctx) {
        AmlSupportData support = supportProvider.compute(ctx);
        for (AmlRule rule : rules) {
            AmlResult r = rule.check(ctx, support);
            if (r.hold()) return r;
        }
        return AmlResult.ok();
    }
}