package com.polishbank.bank_a.domain.klik.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

public record KlikWebhookAuthorizeRequest(
        @JsonProperty("transaction_id") UUID transactionId,
        @JsonProperty("user_id") String userId,
        BigDecimal amount,
        String currency,
        @JsonProperty("merchant_name") String merchantName,
        @JsonProperty("is_on_us") boolean isOnUs,
        @JsonProperty("expiry_time") String expiryTime,
        String zone
) {}