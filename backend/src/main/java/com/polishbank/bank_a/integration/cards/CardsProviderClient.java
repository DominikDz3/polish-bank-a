package com.polishbank.bank_a.integration.cards;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polishbank.bank_a.integration.cards.dto.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.nio.charset.StandardCharsets;
import com.polishbank.bank_a.integration.cards.dto.LifecycleRequest;

@Component
public class CardsProviderClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final CardsProviderHmacSigner hmacSigner;
    private final CardsProviderProperties properties;

    public CardsProviderClient(
            @Qualifier(CardsIntegrationConfig.CARDS_REST_CLIENT) RestClient restClient,
            @Qualifier(CardsIntegrationConfig.CARDS_OBJECT_MAPPER) ObjectMapper objectMapper,
            CardsProviderHmacSigner hmacSigner,
            CardsProviderProperties properties
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.hmacSigner = hmacSigner;
        this.properties = properties;
    }

    public IssueCardResponse issueCard(IssueCardRequest request) {
        return signedPost("/api/v1/cards/issue", request, IssueCardResponse.class);
    }

    public void changeStatus(String cardToken, ChangeStatusRequest request) {
        signedPatch("/api/v1/cards/" + cardToken + "/status", request, Void.class);
    }

    public void activateCard(String cardToken, ActivateCardRequest request) {
        plainPost("/api/v1/cards/" + cardToken + "/activate", request, Void.class);
    }

    public TopupCardResponse topupCard(String cardToken, TopupCardRequest request) {
        return plainPost("/api/v1/cards/" + cardToken + "/topup", request, TopupCardResponse.class);
    }

    public CardStatusResponse getStatus(String cardToken) {
        try {
            return restClient.get()
                    .uri("/api/v1/cards/" + cardToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(CardStatusResponse.class);
        } catch (HttpStatusCodeException e) {
            throw new CardsProviderException(
                    "HTTP_" + e.getStatusCode().value(),
                    "Błąd providera kart przy pobieraniu statusu: " + e.getResponseBodyAsString(),
                    e);
        } catch (RestClientException e) {
            throw new CardsProviderException("CONNECTION_ERROR", "Brak połączenia z providerem kart", e);
        }
    }

    public void updateLifecycle(String cardToken, LifecycleRequest request) {
        try {
            restClient.patch()
                    .uri("/api/v1/cards/" + cardToken + "/lifecycle")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Admin-Key", properties.adminKey())
                    .body(request)
                    .retrieve()
                    .body(Void.class);
        } catch (HttpStatusCodeException e) {
            throw mapHttpError(e);
        } catch (RestClientException e) {
            throw new CardsProviderException("CONNECTION_ERROR", "Brak połączenia z providerem kart", e);
        }
    }

    private <R> R signedPost(String path, Object body, Class<R> responseType) {
        String canonicalJson = serialize(body);
        CardsProviderHmacSigner.SignedRequest signed = hmacSigner.sign(properties.hmacSecret(), canonicalJson);
        try {
            return restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Key", properties.apiKey())
                    .header("X-Signature", signed.signature())
                    .header("X-Timestamp", signed.timestamp())
                    .body(canonicalJson.getBytes(StandardCharsets.UTF_8))
                    .retrieve()
                    .body(responseType);
        } catch (HttpStatusCodeException e) {
            throw mapHttpError(e);
        } catch (RestClientException e) {
            throw new CardsProviderException("CONNECTION_ERROR", "Brak połączenia z providerem kart", e);
        }
    }

    private <R> R signedPatch(String path, Object body, Class<R> responseType) {
        String canonicalJson = serialize(body);
        CardsProviderHmacSigner.SignedRequest signed = hmacSigner.sign(properties.hmacSecret(), canonicalJson);
        try {
            return restClient.patch()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Key", properties.apiKey())
                    .header("X-Signature", signed.signature())
                    .header("X-Timestamp", signed.timestamp())
                    .body(canonicalJson.getBytes(StandardCharsets.UTF_8))
                    .retrieve()
                    .body(responseType);
        } catch (HttpStatusCodeException e) {
            throw mapHttpError(e);
        } catch (RestClientException e) {
            throw new CardsProviderException("CONNECTION_ERROR", "Brak połączenia z providerem kart", e);
        }
    }

    private <R> R plainPost(String path, Object body, Class<R> responseType) {
        try {
            return restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(responseType);
        } catch (HttpStatusCodeException e) {
            throw mapHttpError(e);
        } catch (RestClientException e) {
            throw new CardsProviderException("CONNECTION_ERROR", "Brak połączenia z providerem kart", e);
        }
    }

    private String serialize(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new CardsProviderException("SERIALIZATION_ERROR", "Nie udało się zserializować body", e);
        }
    }

    private CardsProviderException mapHttpError(HttpStatusCodeException e) {
        return new CardsProviderException(
                "HTTP_" + e.getStatusCode().value(),
                "Provider kart zwrócił błąd: " + e.getResponseBodyAsString(),
                e);
    }
}