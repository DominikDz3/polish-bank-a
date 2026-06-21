package com.polishbank.bank_a.integration.swift;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class SwiftAuthClient {

    private static final long EXPIRY_SAFETY_MARGIN_SECONDS = 30;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final SwiftProperties properties;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile String cachedToken;
    private volatile Instant cachedExpiry = Instant.EPOCH;

    public SwiftAuthClient(
            @Qualifier(SwiftIntegrationConfig.SWIFT_REST_CLIENT) RestClient restClient,
            @Qualifier(SwiftIntegrationConfig.SWIFT_OBJECT_MAPPER) ObjectMapper objectMapper,
            SwiftProperties properties) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public String getAccessToken() {
        if (isTokenFresh()) {
            return cachedToken;
        }
        lock.lock();
        try {
            if (isTokenFresh()) {
                return cachedToken;
            }
            refreshToken();
            return cachedToken;
        } finally {
            lock.unlock();
        }
    }

    private boolean isTokenFresh() {
        return cachedToken != null
                && Instant.now().isBefore(cachedExpiry.minusSeconds(EXPIRY_SAFETY_MARGIN_SECONDS));
    }

    private void refreshToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("bank_bic", properties.bic());

        try {
            String body = restClient.post()
                    .uri("/auth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            String accessToken = root.path("access_token").asText(null);
            if (accessToken == null || accessToken.isBlank()) {
                throw new SwiftMiddlewareException(
                        "AUTH_NO_TOKEN", 0,
                        "Middleware SWIFT nie zwrócił access_token");
            }
            long expiresIn = root.path("expires_in").asLong(3600);
            this.cachedToken = accessToken;
            this.cachedExpiry = Instant.now().plusSeconds(expiresIn);
        } catch (HttpStatusCodeException e) {
            throw new SwiftMiddlewareException(
                    "AUTH_HTTP_" + e.getStatusCode().value(),
                    e.getStatusCode().value(),
                    "Błąd autoryzacji w middleware SWIFT: " + e.getResponseBodyAsString(),
                    e);
        } catch (RestClientException e) {
            throw new SwiftMiddlewareException(
                    "AUTH_CONNECTION", 0,
                    "Brak połączenia z middleware SWIFT (auth)", e);
        } catch (Exception e) {
            throw new SwiftMiddlewareException(
                    "AUTH_PARSE", 0,
                    "Nie udało się sparsować odpowiedzi tokenu z middleware SWIFT", e);
        }
    }

    public void invalidateToken() {
        lock.lock();
        try {
            this.cachedToken = null;
            this.cachedExpiry = Instant.EPOCH;
        } finally {
            lock.unlock();
        }
    }
}