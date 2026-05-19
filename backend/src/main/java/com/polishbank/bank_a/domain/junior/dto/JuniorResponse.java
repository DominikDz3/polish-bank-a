package com.polishbank.bank_a.domain.junior.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record JuniorResponse(
    UUID userId,
    String customerNumber,
    String email,
    String firstName,
    String lastName,
    LocalDate dateOfBirth,
    UUID accountId,
    String accountNumber,
    BigDecimal balance,
    String currency,
    String parentAccountNumber
) {}