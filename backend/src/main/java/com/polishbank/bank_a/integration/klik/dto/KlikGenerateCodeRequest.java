package com.polishbank.bank_a.integration.klik.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KlikGenerateCodeRequest(
        @JsonProperty("user_id") String userId,
        String zone
) {}