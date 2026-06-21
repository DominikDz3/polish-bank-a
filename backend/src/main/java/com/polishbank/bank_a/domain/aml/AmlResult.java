package com.polishbank.bank_a.domain.aml;

public record AmlResult(boolean hold, String reason, String ruleCode) {
    public static AmlResult ok() { return new AmlResult(false, null, null); }
    public static AmlResult hold(String ruleCode, String reason) {
        return new AmlResult(true, reason, ruleCode);
    }
}