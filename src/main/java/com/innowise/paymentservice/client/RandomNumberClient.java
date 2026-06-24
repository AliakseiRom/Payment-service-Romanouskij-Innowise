package com.innowise.paymentservice.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class RandomNumberClient {

    private final RestClient restClient;

    @Value("${random-number-service.url}")
    private String randomNumberServiceUrl;

    public Integer getRandomNumber() {
        Integer[] response = restClient.get()
                .uri(randomNumberServiceUrl + "?min=1&max=100&count=1")
                .retrieve()
                .body(Integer[].class);

        if (response == null || response.length == 0) {
            throw new IllegalStateException("Random number service returned empty response");
        }

        return response[0];
    }
}
