package com.polishbank.bank_a.domain.aml;

import java.math.BigDecimal;
import java.util.UUID;

public record AmlContext(
        UUID userId,
        UUID accountId,
        BigDecimal amount,
        String currency,
        AmlTransactionType type,
        String title,
        String receiverIdentifier,
        String receiverCountry
) {}