package com.polishbank.bank_a.domain.transfer.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record ExternalTransferRequest(
        @NotNull UUID senderAccountId,
        @NotBlank String receiverAccountNumber,
        @NotBlank String receiverName,
        @NotBlank String receiverBankBicfi,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String title,
        @NotBlank @Pattern(regexp = "ELIXIR|EXPRESS|SORBNET") String routingSystem,
        @NotBlank @Pattern(regexp = "\\d{4}") String pin
) {}