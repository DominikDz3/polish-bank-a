package com.polishbank.bank_a.integration.cards.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CardStatusResponse(
        @JsonProperty("card_token") String cardToken,
        @JsonProperty("masked_pan") String maskedPan,
        @JsonProperty("status") String status,
        @JsonProperty("card_type") String cardType,
        @JsonProperty("balance") Double balance,
        @JsonProperty("daily_limit") Double dailyLimit,
        @JsonProperty("expiry_month") Integer expiryMonth,
        @JsonProperty("expiry_year") Integer expiryYear
) {
}