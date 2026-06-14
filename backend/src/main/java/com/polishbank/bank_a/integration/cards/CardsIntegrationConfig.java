package com.polishbank.bank_a.integration.cards;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(CardsProviderProperties.class)
public class CardsIntegrationConfig {

    public static final String CARDS_OBJECT_MAPPER = "cardsObjectMapper";
    public static final String CARDS_REST_CLIENT = "cardsRestClient";

    @Bean(CARDS_OBJECT_MAPPER)
    public ObjectMapper cardsObjectMapper() {
        return new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

        @Bean(CARDS_REST_CLIENT)
    public RestClient cardsRestClient(
            CardsProviderProperties props,
            @Qualifier(CARDS_OBJECT_MAPPER) ObjectMapper cardsMapper
    ) {
        int timeoutMs = props.requestTimeoutMs() != null ? props.requestTimeoutMs() : 10_000;
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(cardsMapper);
        return RestClient.builder()
                .baseUrl(props.gatewayUrl())
                .requestFactory(timeoutFactory(timeoutMs))
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(converter);
                })
                .build();
    }

    private static ClientHttpRequestFactory timeoutFactory(int timeoutMs) {
        return new HttpComponentsClientHttpRequestFactory();
    }
}