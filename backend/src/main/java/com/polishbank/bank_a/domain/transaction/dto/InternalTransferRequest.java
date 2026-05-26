package com.polishbank.bank_a.domain.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.UUID;

public record InternalTransferRequest(
    @NotNull(message = "Musisz wybrać konto nadawcy")
    UUID senderAccountId,

    @NotBlank(message = "Numer konta odbiorcy nie może być pusty")
    String receiverAccountNumber,

    @NotNull(message = "Kwota przelewu jest wymagana")
    @DecimalMin(value = "0.01", message = "Kwota przelewu musi być większa od zera")
    BigDecimal amount,

    @NotBlank(message = "Tytuł przelewu jest wymagany")
    String title,

    @NotBlank(message = "PIN jest wymagany do potwierdzenia przelewu")
    @Pattern(regexp = "\\d{4}", message = "PIN musi składać się z dokładnie 4 cyfr")
    String pin
) {}