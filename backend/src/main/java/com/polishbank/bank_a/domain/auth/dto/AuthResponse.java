package com.polishbank.bank_a.domain.auth.dto;

public record AuthResponse(String token, String email, String role, String customerNumber) {
}
