package com.polishbank.bank_a.domain.aml;

public final class AmlHoldStatus {
    public static final String AWAITING_EXPLANATION = "AWAITING_EXPLANATION";
    public static final String AWAITING_DECISION = "AWAITING_DECISION";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    private AmlHoldStatus() {}
}