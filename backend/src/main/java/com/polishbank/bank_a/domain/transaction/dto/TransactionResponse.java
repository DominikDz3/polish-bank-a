package com.polishbank.bank_a.domain.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String senderAccountNumber,
        String receiverAccountNumber,
        String receiverName,
        String title,
        BigDecimal amount,
        String currency,
        String status,
        String type,
        LocalDateTime createdAt,
        LocalDateTime executionDate,
        String direction
) {}