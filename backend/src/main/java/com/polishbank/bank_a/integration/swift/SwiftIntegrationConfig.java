package com.polishbank.bank_a.integration.swift;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(SwiftProperties.class)
public class SwiftIntegrationConfig {

    public static final String SWIFT_REST_CLIENT = "swiftRestClient";
    public static final String SWIFT_OBJECT_MAPPER = "swiftObjectMapper";

    @Bean(SWIFT_REST_CLIENT)
    public RestClient swiftRestClient(SwiftProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.requestTimeoutMs());
        factory.setReadTimeout(properties.requestTimeoutMs());
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory((ClientHttpRequestFactory) factory)
                .build();
    }

    @Bean(SWIFT_OBJECT_MAPPER)
    public ObjectMapper swiftObjectMapper() {
        return new ObjectMapper();
    }
}