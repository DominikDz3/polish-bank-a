package com.polishbank.bank_a.domain.aml;

public interface AmlRule {
    String code();
    AmlResult check(AmlContext ctx, AmlSupportData support);
}