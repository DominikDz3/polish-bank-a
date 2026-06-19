package com.polishbank.bank_a.domain.card.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TopupCardRequest(
        @NotNull @Positive BigDecimal amount
) {
}