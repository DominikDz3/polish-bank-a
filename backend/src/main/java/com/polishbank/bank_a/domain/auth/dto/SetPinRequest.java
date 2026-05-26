package com.polishbank.bank_a.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SetPinRequest(
        @NotBlank
        @Pattern(regexp = "\\d{4}", message = "PIN musi składać się z dokładnie 4 cyfr")
        String pin,

        @NotBlank
        @Pattern(regexp = "\\d{4}", message = "PIN musi składać się z dokładnie 4 cyfr")
        String confirmPin
) {}