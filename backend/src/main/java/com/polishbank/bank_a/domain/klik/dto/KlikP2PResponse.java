package com.polishbank.bank_a.domain.klik.dto;

public record KlikP2PResponse(
        String routing,
        String status,         
        String receiverBank,   
        String receiverAccount,
        String message
) {}