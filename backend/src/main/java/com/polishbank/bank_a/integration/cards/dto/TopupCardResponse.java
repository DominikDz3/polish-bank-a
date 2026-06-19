package com.polishbank.bank_a.integration.cards.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TopupCardResponse(
        @JsonProperty("new_balance") Double newBalance
) {
}