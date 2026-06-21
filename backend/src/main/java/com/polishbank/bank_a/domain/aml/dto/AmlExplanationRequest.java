package com.polishbank.bank_a.domain.aml.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AmlExplanationRequest(
        @NotBlank @Size(min = 10, max = 2000,
            message = "Wyjaśnienie powinno mieć od 10 do 2000 znaków.")
        String text
) {}