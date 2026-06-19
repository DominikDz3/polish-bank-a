package com.polishbank.bank_a.domain.klik.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record KlikRegisterAliasRequest(
        UUID accountId,
        @NotBlank
        @Pattern(regexp = "^\\+\\d{8,15}$", message = "Numer w formacie E.164, np. +48501234567")
        String phone
) {}