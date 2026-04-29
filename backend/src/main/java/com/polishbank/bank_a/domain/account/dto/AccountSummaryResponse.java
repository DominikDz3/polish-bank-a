package com.polishbank.bank_a.domain.account.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountSummaryResponse (
    UUID id,
    String accountNumber,
    BigDecimal balance,
    BigDecimal blockedFunds,
    String currency,
    String type
){}
