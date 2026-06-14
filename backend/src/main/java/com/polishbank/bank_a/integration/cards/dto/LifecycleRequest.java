package com.polishbank.bank_a.integration.cards.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LifecycleRequest(
        @JsonProperty("new_status") String newStatus,
        @JsonProperty("changed_by") String changedBy
) {
}