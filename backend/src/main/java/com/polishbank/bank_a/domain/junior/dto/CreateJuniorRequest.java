package com.polishbank.bank_a.domain.junior.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

public record CreateJuniorRequest(
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String password,
    @NotNull @Past LocalDate dateOfBirth,
    @NotNull UUID parentAccountId
) {}