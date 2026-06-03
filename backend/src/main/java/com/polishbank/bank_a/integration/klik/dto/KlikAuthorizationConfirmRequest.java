package com.polishbank.bank_a.domain.klik.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record KlikAuthorizationConfirmRequest(
        @NotBlank(message = "Decyzja jest wymagana (ACCEPT lub REJECT)")
        @Pattern(regexp = "ACCEPT|REJECT", message = "Decyzja musi być ACCEPT albo REJECT")
        String status,

        String reason,

        @Pattern(regexp = "\\d{4}", message = "PIN musi składać się z 4 cyfr")
        String pin
) {}