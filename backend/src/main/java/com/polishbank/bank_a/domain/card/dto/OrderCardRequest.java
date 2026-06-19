package com.polishbank.bank_a.domain.card.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderCardRequest(
        @NotBlank String cardType   // VIRTUAL | PHYSICAL | PREPAID
) {
}