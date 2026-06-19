package com.polishbank.bank_a.integration.cards.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TopupCardRequest(
        @JsonProperty("amount") Double amount,
        @JsonProperty("currency") String currency
) {
}