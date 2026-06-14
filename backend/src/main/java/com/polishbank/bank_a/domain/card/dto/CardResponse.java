package com.polishbank.bank_a.domain.card.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CardResponse(
    UUID id,
    UUID accountId,
    String accountNumber,
    String maskedNumber,
    BigDecimal transactionLimit,
    BigDecimal dailyLimit,
    BigDecimal spentToday,
    String currency,
    LocalDate expiryDate,
    String type,
    boolean blocked,
    String providerToken,
    String providerStatus,
    String maskedPan
) {}