package com.polishbank.bank_a.domain.klik.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record KlikAliasView(
        UUID id,
        String alias,
        String accountNumber,
        LocalDateTime createdAt
) {}