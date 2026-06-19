package com.polishbank.bank_a.domain.klik.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record KlikP2PRequest(
        @NotNull UUID senderAccountId,
        @NotBlank @Pattern(regexp = "^\\+\\d{8,15}$", message = "Numer w formacie E.164, np. +48501234567")
        String phone,
        @NotBlank String receiverName,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String title,
        @NotBlank @Pattern(regexp = "\\d{4}") String pin
) {}