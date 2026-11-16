package com.example.apimonitoring.monitoring;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class MonitorHttpClientTimeoutTest {

    private static final Duration TEST_TIMEOUT = Duration.ofMillis(300);

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldReturnDownWhenApiRespondsTooSlowly() throws IOException {

        server = startServer(exchange -> {
            sleep(2000);
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });

        MonitorHttpClient client = new MonitorHttpClient(RestClient.builder(), TEST_TIMEOUT);

        MonitorCheckResult result = client.checkUrl(urlOf(server));

        assertThat(result.getStatus()).isEqualTo(MonitorStatus.DOWN);
        assertThat(result.getHttpStatusCode()).isNull();
        assertThat(result.getErrorMessage()).containsIgnoringCase("timed out");
        assertThat(result.getResponseTimeMs()).isLessThan(2000);
    }

    @Test
    void shouldReturnUpWhenApiRespondsWithinTimeout() throws IOException {

        server = startServer(exchange -> {
            sleep(50);
            byte[] body = "OK".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        MonitorHttpClient client = new MonitorHttpClient(RestClient.builder(), TEST_TIMEOUT);

        MonitorCheckResult result = client.checkUrl(urlOf(server));

        assertThat(result.getStatus()).isEqualTo(MonitorStatus.UP);
        assertThat(result.getHttpStatusCode()).isEqualTo(200);
        assertThat(result.getResponseTimeMs()).isGreaterThanOrEqualTo(50);
    }

    @Test
    void shouldReturnDownWhenNothingListensOnPort() throws IOException {

        server = startServer(exchange -> exchange.close());
        String url = urlOf(server);
        server.stop(0);
        server = null;

        MonitorHttpClient client = new MonitorHttpClient(RestClient.builder(), TEST_TIMEOUT);

        MonitorCheckResult result = client.checkUrl(url);

        assertThat(result.getStatus()).isEqualTo(MonitorStatus.DOWN);
        assertThat(result.getHttpStatusCode()).isNull();
        assertThat(result.getErrorMessage()).isNotBlank();
    }

    @Test
    void shouldReturnDownWhenHostDoesNotExist() {

        MonitorHttpClient client = new MonitorHttpClient(RestClient.builder(), TEST_TIMEOUT);

        MonitorCheckResult result = client.checkUrl("http://not-existing-host.invalid/health");

        assertThat(result.getStatus()).isEqualTo(MonitorStatus.DOWN);
        assertThat(result.getHttpStatusCode()).isNull();
        assertThat(result.getErrorMessage()).isNotBlank();
    }


    private HttpServer startServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpServer.createContext("/health", handler);
        httpServer.start();
        return httpServer;
    }

    private String urlOf(HttpServer httpServer) {
        return "http://localhost:" + httpServer.getAddress().getPort() + "/health";
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
