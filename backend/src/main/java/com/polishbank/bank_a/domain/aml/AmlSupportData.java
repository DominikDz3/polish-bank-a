package com.polishbank.bank_a.domain.aml;

public record AmlSupportData(
        long recentTransfersToSameReceiver,
        boolean isNewBeneficiary
) {}