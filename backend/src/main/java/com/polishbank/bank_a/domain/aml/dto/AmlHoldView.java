package com.polishbank.bank_a.domain.aml.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AmlHoldView(
        UUID id,
        String holdType,
        String status,
        BigDecimal amount,
        String currency,
        String receiverInfo,
        String reason,
        String triggeredRule,
        String clientExplanation,
        String decisionNote,
        String clientEmail,
        LocalDateTime createdAt,
        LocalDateTime decisionAt
) {}