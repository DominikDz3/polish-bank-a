package com.polishbank.bank_a.integration.cards.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChangeStatusRequest(
        @JsonProperty("status") String status,
        @JsonProperty("reason") String reason
) {
}