package com.polishbank.bank_a.integration.cards.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IssueCardRequest(
        @JsonProperty("user_id") String userId,
        @JsonProperty("account_id") String accountId,
        @JsonProperty("card_type") String cardType,
        @JsonProperty("initial_balance") Double initialBalance
) {
}