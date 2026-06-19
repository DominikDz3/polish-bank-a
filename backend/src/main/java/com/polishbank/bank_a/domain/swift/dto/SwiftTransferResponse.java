package com.polishbank.bank_a.domain.swift.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SwiftTransferResponse(
        UUID id,
        UUID transactionId,
        String uetr,
        String messageId,
        String senderBic,
        String senderAccountNumber,
        String receiverBic,
        String receiverCountry,
        String receiverIban,
        String receiverName,
        BigDecimal amount,
        String currency,
        String chargeBearerInput,
        String chargeBearer,
        List<String> route,
        BigDecimal feeTotal,
        BigDecimal feeSender,
        BigDecimal feeReceiver,
        BigDecimal feeIntermediary,
        BigDecimal estimatedSeconds,
        String status,
        String returnReason,
        String title,
        LocalDateTime createdAt,
        LocalDateTime deliveredAt,
        LocalDateTime returnedAt,
        BigDecimal debitedAmount,
        String debitedCurrency
) {}