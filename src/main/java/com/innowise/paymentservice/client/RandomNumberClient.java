package com.innowise.paymentservice.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class RandomNumberClient {

    private final RestClient restClient;

    @Value("${random-number-service.url}")
    private String randomNumberServiceUrl;

    public Integer getRandomNumber() {
        try {
            Integer[] response = restClient.get()
                    .uri(randomNumberServiceUrl + "?min=1&max=100&count=1")
                    .retrieve()
                    .body(Integer[].class);

            if (response == null || response.length == 0) {
                return generateFallbackNumber();
            }

            return response[0];

        } catch (RestClientException ex) {
            return generateFallbackNumber();
        }
    }

    private Integer generateFallbackNumber() {
        return ThreadLocalRandom.current().nextInt(1, 101);
    }
}
