package com.polishbank.bank_a.domain.klik.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record KlikPendingAuthorizationResponse(
        UUID id,
        UUID klikTransactionId,
        BigDecimal amount,
        String currency,
        String merchantName,
        String accountNumber,
        LocalDateTime expiryTime
) {}