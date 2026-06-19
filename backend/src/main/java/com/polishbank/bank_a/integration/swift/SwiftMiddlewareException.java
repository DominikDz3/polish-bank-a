package com.polishbank.bank_a.integration.swift;

public class SwiftMiddlewareException extends RuntimeException {

    private final String code;
    private final int httpStatus;

    public SwiftMiddlewareException(String code, int httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public SwiftMiddlewareException(String code, int httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}