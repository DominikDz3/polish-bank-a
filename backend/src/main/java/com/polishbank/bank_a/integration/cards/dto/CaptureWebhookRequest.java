package com.polishbank.bank_a.integration.cards.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record CaptureWebhookRequest(
        @JsonProperty("authorization_code") String authorizationCode,
        @JsonProperty("transaction_id") String transactionId,
        @JsonProperty("card_token") String cardToken,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("merchant_name") String merchantName,
        @JsonProperty("merchant_id") String merchantId
) {
    public String merchantLabel() {
        if (merchantName != null && !merchantName.isBlank()) return merchantName;
        if (merchantId != null && !merchantId.isBlank()) return merchantId;
        return "Provider";
    }
}