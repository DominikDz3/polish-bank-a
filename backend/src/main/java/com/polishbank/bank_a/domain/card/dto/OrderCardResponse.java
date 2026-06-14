package com.polishbank.bank_a.domain.card.dto;

import java.util.UUID;

public record OrderCardResponse(
        UUID id,
        String providerToken,
        String maskedPan,
        String fullPan,
        String cvv,
        Integer expiryMonth,
        Integer expiryYear,
        String providerStatus,
        String cardType
) {
}