package com.polishbank.bank_a.domain.card.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record UpdateCardLimitsRequest(
    @DecimalMin(value = "0.00", message = "Limit nie może być ujemny")
    BigDecimal transactionLimit,

    @DecimalMin(value = "0.00", message = "Limit nie może być ujemny")
    BigDecimal dailyLimit
) {}