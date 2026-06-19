package com.polishbank.bank_a.integration.klik;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Component
public class KlikP2PClient {

    private final RestClient restClient;
    private final String zone;

    public KlikP2PClient(
            @Value("${integration.klik.base-url}") String baseUrl,
            @Value("${integration.klik.api-key}") String apiKey,
            @Value("${integration.klik.zone}") String zone) {
        this.zone = zone;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-KLIK-Bank-Api-Key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public Map<String, Object> registerAlias(String phone, String iban) {
        String body = String.format(
                "{\"phone\":\"%s\",\"account_identifier\":{\"type\":\"iban\",\"value\":\"%s\"},\"zone\":\"%s\"}",
                phone, iban, zone
        );
        return restClient.post()
                .uri("/aliases/register")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);
    }

@SuppressWarnings("unchecked")
public Map<String, Object> lookupAlias(String phone) {
    return restClient.get()
            .uri("/aliases/lookup/{phone}", phone)
            .retrieve()
            .body(Map.class);
}

public void deleteAlias(String phone) {
    restClient.delete()
            .uri("/aliases/{phone}", phone)
            .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
            .retrieve()
            .toBodilessEntity();
}
}