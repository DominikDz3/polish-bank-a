package com.polishbank.bank_a.domain.card.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record CardPaymentRequest(
    @NotNull UUID cardId,
    @NotNull @DecimalMin(value = "0.01", message = "Kwota musi być większa od zera") BigDecimal amount,
    @NotBlank @Size(max = 255) String merchant,
    @NotBlank @Size(min = 3, max = 3) String currency
) {}