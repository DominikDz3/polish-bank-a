package com.polishbank.bank_a.integration.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class ElixirClient {

    private final RestClient restClient;

    public ElixirClient(@Value("${integration.elixir.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void sendPayment(String pacs008Xml) {
        restClient.post()
                .uri("/api/elixir/payments")
                .contentType(new MediaType("application", "xml", StandardCharsets.UTF_8))
                .body(pacs008Xml.getBytes(StandardCharsets.UTF_8))
                .retrieve()
                .toBodilessEntity();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getByStatus(String status) {
        return restClient.get()
                .uri("/api/elixir/history/status/{status}", status)
                .retrieve()
                .body(List.class);
    }
}