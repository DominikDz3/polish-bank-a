package com.polishbank.bank_a.integration.cards.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record RefundWebhookRequest(
        @JsonProperty("card_token") String cardToken,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("original_transaction_id") String originalTransactionId
) {
}