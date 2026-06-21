package com.polishbank.bank_a.integration.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class ExpressElixirClient {

    private final RestClient restClient;

    public ExpressElixirClient(@Value("${integration.express.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public IsoXml.ParsedResponse sendPayment(
            String paymentId,
            String senderBicfi,
            String receiverBicfi,
            String senderAccount,
            String receiverAccount,
            String senderName,
            String receiverName,
            BigDecimal amount,
            String currency,
            String title) {
        Map<String, Object> body = new java.util.HashMap<>();
            body.put("paymentId", paymentId);
            body.put("amount", amount);
            body.put("currency", currency);
            body.put("senderName", senderName);
            body.put("receiverName", receiverName);
            body.put("senderAccount", senderAccount);
            body.put("receiverAccount", receiverAccount);
            body.put("title", title);
            body.put("senderBankId", senderBicfi);
            body.put("receiverBankId", receiverBicfi);
            body.put("type", "EXPRESS");
        Map<String, Object> response = restClient.post()
                .uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null) return new IsoXml.ParsedResponse("UNKNOWN", null);
        String status = (String) response.getOrDefault("status", "UNKNOWN");
        return new IsoXml.ParsedResponse(status, null);
    }

    @SuppressWarnings("unchecked")
    public IsoXml.ParsedResponse getPaymentStatus(String paymentId) {
        Map<String, Object> response = restClient.get()
                .uri("/api/express/payments/{id}", paymentId)
                .retrieve()
                .body(Map.class);
        if (response == null) return new IsoXml.ParsedResponse("UNKNOWN", null);
        String status = (String) response.getOrDefault("status", "UNKNOWN");
        String reason = (String) response.get("heldReason");
        return new IsoXml.ParsedResponse(status, reason);
    }
}