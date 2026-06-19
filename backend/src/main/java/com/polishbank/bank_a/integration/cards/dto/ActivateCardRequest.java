package com.polishbank.bank_a.integration.cards.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ActivateCardRequest(
        @JsonProperty("activated_by") String activatedBy
) {
}