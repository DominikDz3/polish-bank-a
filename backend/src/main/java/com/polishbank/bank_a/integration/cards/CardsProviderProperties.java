package com.polishbank.bank_a.integration.cards;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration.cards")
public record CardsProviderProperties(
        String gatewayUrl,
        String apiKey,
        String hmacSecret,
        String binPrefix,
        String adminKey,
        Integer requestTimeoutMs
) {
}