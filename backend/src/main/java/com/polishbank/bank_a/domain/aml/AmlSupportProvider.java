package com.polishbank.bank_a.domain.aml;

import org.springframework.stereotype.Component;

@Component
public class AmlSupportProvider {
    public AmlSupportData compute(AmlContext ctx) {
        return new AmlSupportData(0, false);
    }
}