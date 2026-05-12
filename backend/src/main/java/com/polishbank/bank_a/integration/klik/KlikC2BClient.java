package com.polishbank.bank_a.integration.klik;

import com.polishbank.bank_a.integration.klik.dto.KlikGenerateCodeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class KlikC2BClient {

    private final String zone;
    private final RestClient restClient;

    public KlikC2BClient(
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

    public KlikGenerateCodeResponse generateCode(String userId) {
        String body = "{\"user_id\":\"" + userId + "\",\"zone\":\"" + zone + "\"}";

        return restClient.post()
                .uri("/codes/generate")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(KlikGenerateCodeResponse.class);
    }
}