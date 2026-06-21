package com.polishbank.bank_a.domain.swift.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record SwiftTransferRequest(
        @NotNull(message = "Wybierz konto źródłowe.")
        UUID senderAccountId,

        @NotBlank(message = "Podaj BIC banku odbiorcy.")
        @Pattern(regexp = "^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$",
                message = "BIC ma format AAAA BB CC [DDD] (8 lub 11 znaków).")
        String receiverBic,

        @NotBlank(message = "Podaj numer konta odbiorcy.")
        @Size(min = 8, max = 34, message = "Numer konta ma długość 8-34 znaków.")
        String receiverIban,

        @NotBlank(message = "Podaj nazwę odbiorcy.")
        @Size(max = 140)
        String receiverName,

        @Size(min = 2, max = 2, message = "Kod kraju ma 2 litery (ISO 3166-1).")
        String receiverCountry,

        @NotBlank(message = "Podaj walutę przelewu.")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Waluta to kod ISO 4217, 3 litery.")
        String currency,

        @NotNull(message = "Podaj kwotę przelewu.")
        @DecimalMin(value = "0.01", message = "Kwota musi być większa od zera.")
        BigDecimal amount,

        @NotBlank(message = "Wybierz sposób podziału opłat.")
        @Pattern(regexp = "^(OUR|SHA|BEN)$", message = "Dozwolone wartości: OUR, SHA, BEN.")
        String chargeBearer,

        @NotBlank(message = "Podaj tytuł przelewu.")
        @Size(max = 140)
        String title,

        @NotBlank(message = "Wymagany PIN.")
        @Pattern(regexp = "^\\d{4}$", message = "PIN składa się z 4 cyfr.")
        String pin
) {}