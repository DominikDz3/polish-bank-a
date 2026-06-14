package com.polishbank.bank_a.integration.cards.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthorizeWebhookResponse(
        @JsonProperty("authorization_code") String authorizationCode,
        @JsonProperty("status") String status,
        @JsonProperty("decline_reason") String declineReason
) {
    public static AuthorizeWebhookResponse approved(String code) {
        return new AuthorizeWebhookResponse(code, "APPROVED", null);
    }

    public static AuthorizeWebhookResponse declined(String reason) {
        return new AuthorizeWebhookResponse(null, "DECLINED", reason);
    }
}