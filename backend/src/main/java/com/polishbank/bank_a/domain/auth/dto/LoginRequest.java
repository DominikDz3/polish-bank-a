package com.polishbank.bank_a.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(min = 8, max = 8) String customerNumber,
        @NotBlank String password
) {}