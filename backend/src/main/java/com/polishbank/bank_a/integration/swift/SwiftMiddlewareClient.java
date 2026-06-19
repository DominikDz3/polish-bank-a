package com.polishbank.bank_a.integration.swift;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class SwiftMiddlewareClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final SwiftAuthClient authClient;

    public SwiftMiddlewareClient(
            @Qualifier(SwiftIntegrationConfig.SWIFT_REST_CLIENT) RestClient restClient,
            @Qualifier(SwiftIntegrationConfig.SWIFT_OBJECT_MAPPER) ObjectMapper objectMapper,
            SwiftAuthClient authClient) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.authClient = authClient;
    }

    public SendResult sendMessage(String pacs008Xml) {
        String token = authClient.getAccessToken();
        try {
            String body = restClient.post()
                    .uri("/swift/message")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_XML)
                    .body(pacs008Xml)
                    .retrieve()
                    .body(String.class);
            return parseSendResponse(body);
        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            if (status == 401) {
                authClient.invalidateToken();
            }
            String message = extractErrorMessage(e.getResponseBodyAsString(), e.getStatusText());
            throw new SwiftMiddlewareException("SEND_HTTP_" + status, status, message, e);
        } catch (RestClientException e) {
            throw new SwiftMiddlewareException(
                    "SEND_CONNECTION", 0,
                    "Brak połączenia z middleware SWIFT (send)", e);
        }
    }

    public void cancel(String uetr) {
        String token = authClient.getAccessToken();
        try {
            restClient.post()
                    .uri("/swift/cancel/" + uetr)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            if (status == 401) {
                authClient.invalidateToken();
            }
            throw new SwiftMiddlewareException(
                    "CANCEL_HTTP_" + status, status,
                    extractErrorMessage(e.getResponseBodyAsString(), e.getStatusText()), e);
        } catch (RestClientException e) {
            throw new SwiftMiddlewareException(
                    "CANCEL_CONNECTION", 0,
                    "Brak połączenia z middleware SWIFT (cancel)", e);
        }
    }

    private SendResult parseSendResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            String uetr = root.path("uetr").asText(null);
            String messageId = root.path("message_id").asText(null);
            String receiverBank = root.path("receiver_bank").asText(null);
            BigDecimal estimatedSeconds = root.has("estimated_seconds")
                    ? new BigDecimal(root.path("estimated_seconds").asText("0"))
                    : null;
            BigDecimal cancelWindowSeconds = root.has("cancel_window_seconds")
                    ? new BigDecimal(root.path("cancel_window_seconds").asText("0"))
                    : null;

            List<String> route = new ArrayList<>();
            JsonNode routeNode = root.path("route");
            if (routeNode.isArray()) {
                routeNode.forEach(node -> route.add(node.asText()));
            }

            FeeBreakdown fees = null;
            JsonNode feeNode = root.path("fee_breakdown");
            if (feeNode.isObject()) {
                fees = new FeeBreakdown(
                        feeNode.path("charge_bearer").asText(null),
                        parseMoney(feeNode.path("total_fee").asText("0")),
                        parseMoney(feeNode.path("sender_fee").asText("0")),
                        parseMoney(feeNode.path("receiver_fee").asText("0")),
                        parseMoney(feeNode.path("intermediary_fee").asText("0")),
                        feeNode.path("hop_count").asInt(0)
                );
            }

            return new SendResult(uetr, messageId, receiverBank, route, fees,
                    estimatedSeconds, cancelWindowSeconds);
        } catch (Exception e) {
            throw new SwiftMiddlewareException(
                    "SEND_PARSE", 0,
                    "Nie udało się sparsować odpowiedzi z middleware SWIFT", e);
        }
    }

    private BigDecimal parseMoney(String raw) {
        if (raw == null || raw.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String extractErrorMessage(String body, String fallback) {
        if (body == null || body.isBlank()) {
            return fallback == null ? "Błąd middleware SWIFT" : fallback;
        }
        String trimmed = body.trim();
        if (trimmed.startsWith("<")) {
            return "Middleware SWIFT zgłosił wewnętrzny błąd serwera.";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.hasNonNull("error")) {
                return root.path("error").asText();
            }
        } catch (Exception ignored) {
        }
        return body;
    }

    public record SendResult(
            String uetr,
            String messageId,
            String receiverBank,
            List<String> route,
            FeeBreakdown feeBreakdown,
            BigDecimal estimatedSeconds,
            BigDecimal cancelWindowSeconds
    ) {}

    public record FeeBreakdown(
            String chargeBearer,
            BigDecimal totalFee,
            BigDecimal senderFee,
            BigDecimal receiverFee,
            BigDecimal intermediaryFee,
            int hopCount
    ) {}
}