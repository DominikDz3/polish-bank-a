package com.polishbank.bank_a.integration.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SorbnetClient {

    private final RestClient restClient;

    public SorbnetClient(@Value("${integration.sorbnet.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public IsoXml.ParsedResponse sendPayment(String pacs008Xml) {
        String response = restClient.post()
                .uri("/api/sorbnet/payments")
                .contentType(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_XML)
                .body(pacs008Xml)
                .retrieve()
                .body(String.class);
        return IsoXml.parsePain002(response == null ? "" : response);
    }

    public IsoXml.ParsedResponse getPaymentStatus(String paymentId) {
        String response = restClient.get()
                .uri("/api/sorbnet/payments/{id}", paymentId)
                .accept(MediaType.APPLICATION_XML)
                .retrieve()
                .body(String.class);
        return IsoXml.parsePain002(response == null ? "" : response);
    }
}