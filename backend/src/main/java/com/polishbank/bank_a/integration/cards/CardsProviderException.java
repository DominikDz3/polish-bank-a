package com.polishbank.bank_a.integration.cards;

public class CardsProviderException extends RuntimeException {

    private final String errorCode;

    public CardsProviderException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CardsProviderException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}