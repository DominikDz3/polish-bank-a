package com.polishbank.bank_a.integration.swift;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration.swift")
public record SwiftProperties(
        String baseUrl,
        String clientId,
        String clientSecret,
        String bic,
        String callbackUrl,
        int requestTimeoutMs
) {
    public SwiftProperties {
        if (requestTimeoutMs <= 0) {
            requestTimeoutMs = 10000;
        }
    }
}