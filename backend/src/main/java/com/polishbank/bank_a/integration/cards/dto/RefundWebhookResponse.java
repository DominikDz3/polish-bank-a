package com.polishbank.bank_a.integration.cards.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RefundWebhookResponse(
        @JsonProperty("status") String status
) {
}