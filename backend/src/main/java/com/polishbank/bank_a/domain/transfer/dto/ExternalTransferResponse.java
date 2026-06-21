package com.polishbank.bank_a.domain.transfer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExternalTransferResponse(
        UUID id,
        String externalPaymentId,
        String senderAccountNumber,
        String receiverAccountNumber,
        String receiverName,
        String receiverBankBicfi,
        BigDecimal amount,
        String currency,
        String title,
        String routingSystem,
        String status,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime settledAt
) {}