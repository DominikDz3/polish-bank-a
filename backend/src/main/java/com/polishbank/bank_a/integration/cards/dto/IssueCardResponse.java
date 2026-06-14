package com.polishbank.bank_a.integration.cards.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IssueCardResponse(
        @JsonProperty("card_token") String cardToken,
        @JsonProperty("masked_pan") String maskedPan,
        @JsonProperty("full_pan") String fullPan,
        @JsonProperty("cvv") String cvv,
        @JsonProperty("expiry_month") Integer expiryMonth,
        @JsonProperty("expiry_year") Integer expiryYear,
        @JsonProperty("status") String status,
        @JsonProperty("card_type") String cardType,
        @JsonProperty("bank_id") String bankId
) {
}