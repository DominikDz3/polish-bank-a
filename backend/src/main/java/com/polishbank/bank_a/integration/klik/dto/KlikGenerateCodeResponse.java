package com.polishbank.bank_a.integration.klik.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KlikGenerateCodeResponse(
        String code,
        @JsonProperty("expires_in") Integer expiresIn,
        @JsonProperty("expires_at") String expiresAt
) {}