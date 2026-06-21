package com.polishbank.bank_a.domain.aml.dto;

import jakarta.validation.constraints.Size;

public record AmlDecisionRequest(
        @Size(max = 500) String note
) {}