package com.polishbank.bank_a.domain.klik.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record KlikCodeResponse(
        UUID id,
        String code,
        LocalDateTime expiresAt,
        String status,
        String accountNumber
) {}