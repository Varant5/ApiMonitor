package com.example.apimonitoring.monitoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Component
public class MonitorHttpClient {

    private final RestClient restClient;

    @Autowired
    public MonitorHttpClient(
            RestClient.Builder builder,
            @Value("${monitoring.http.timeout:5s}") Duration timeout
    ) {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);

        this.restClient = builder
                .requestFactory(factory)
                .build();
    }

    MonitorHttpClient(RestClient restClient) {
        this.restClient = restClient;
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

        } catch (HttpServerErrorException e) {

            long responseTime = System.currentTimeMillis() - startTime;

            return new MonitorCheckResult(
                    MonitorStatus.DOWN,
                    e.getStatusCode().value(),
                    responseTime,
                    e.getMessage()
            );

        } catch (HttpClientErrorException e) {

            long responseTime = System.currentTimeMillis() - startTime;

            return new MonitorCheckResult(
                    MonitorStatus.DOWN,
                    e.getStatusCode().value(),
                    responseTime,
                    e.getMessage()
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
