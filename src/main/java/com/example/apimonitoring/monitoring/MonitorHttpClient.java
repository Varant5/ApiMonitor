package com.example.apimonitoring.monitoring;

import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Component
public class MonitorHttpClient {

    private final RestClient restClient;

    public MonitorHttpClient(RestClient.Builder builder) {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = builder
                .requestFactory(factory)
                .build();
    }


    public MonitorCheckResult checkUrl(String url) {

        long startTime = System.currentTimeMillis();

        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .toEntity(String.class);

            long responseTime = System.currentTimeMillis() - startTime;

            MonitorStatus status = response.getStatusCode().is2xxSuccessful()
                    ? MonitorStatus.UP
                    : MonitorStatus.DOWN;

            return new MonitorCheckResult(
                    status,
                    response.getStatusCode().value(),
                    responseTime,
                    null
            );

        } catch (RestClientException e) {

            long responseTime = System.currentTimeMillis() - startTime;

            return new MonitorCheckResult(
                    MonitorStatus.DOWN,
                    null,
                    responseTime,
                    e.getMessage()
            );
        }
    }


}
