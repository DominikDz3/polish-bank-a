package com.polishbank.bank_a.exception;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PinLockedException extends RuntimeException {

    private final LocalDateTime lockedUntil;

    public PinLockedException(LocalDateTime lockedUntil) {
        super("PIN został zablokowany po zbyt wielu nieudanych próbach.");
        this.lockedUntil = lockedUntil;
    }
}