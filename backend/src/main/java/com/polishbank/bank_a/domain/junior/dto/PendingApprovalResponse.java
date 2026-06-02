package com.polishbank.bank_a.domain.junior.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PendingApprovalResponse(
    UUID id,
    UUID juniorAccountId,
    String juniorAccountNumber,
    String juniorFirstName,
    String juniorLastName,
    UUID transactionId,
    String transactionType,
    String receiverName,
    String receiverAccountNumber,
    String title,
    BigDecimal amount,
    String currency,
    String description,
    String status,
    LocalDateTime createdAt
) {}