package com.polishbank.bank_a.integration.cards.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record AuthorizeWebhookRequest(
        @JsonProperty("transaction_id") String transactionId,
        @JsonProperty("account_id") String accountId,
        @JsonProperty("card_token") String cardToken,
        @JsonProperty("card_last_digits") String cardLastDigits,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("merchant_name") String merchantName
) {
}