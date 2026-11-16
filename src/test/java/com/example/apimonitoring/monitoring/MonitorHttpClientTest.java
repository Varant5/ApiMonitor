package com.example.apimonitoring.monitoring;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MonitorHttpClientTest {

    @Test
    void shouldReturnUpWhenApiRespondsWith200() {

        RestClient.Builder builder = RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        MonitorHttpClient monitorHttpClient =
                new MonitorHttpClient(builder.build());

        server.expect(requestTo("https://example.com"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess());

        MonitorCheckResult result =
                monitorHttpClient.checkUrl("https://example.com");

        assertEquals(MonitorStatus.UP, result.getStatus());
        assertEquals(200, result.getHttpStatusCode());
        assertNotNull(result.getResponseTimeMs());
        assertNull(result.getErrorMessage());

        server.verify();
    }

    @Test
    void shouldReturnDownWhenApiRespondsWith500() {

        RestClient.Builder builder = RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        MonitorHttpClient monitorHttpClient =
                new MonitorHttpClient(builder.build());

        server.expect(requestTo("https://example.com"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        MonitorCheckResult result =
                monitorHttpClient.checkUrl("https://example.com");

        assertEquals(MonitorStatus.DOWN, result.getStatus());
        assertEquals(500, result.getHttpStatusCode());
        assertNotNull(result.getResponseTimeMs());
        assertNotNull(result.getErrorMessage());

        server.verify();
    }

    @Test
    void shouldReturnDownWhenApiRespondsWith404() {

        RestClient.Builder builder = RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        MonitorHttpClient monitorHttpClient =
                new MonitorHttpClient(builder.build());

        server.expect(requestTo("https://example.com"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withResourceNotFound());

        MonitorCheckResult result =
                monitorHttpClient.checkUrl("https://example.com");

        assertEquals(MonitorStatus.DOWN, result.getStatus());
        assertEquals(404, result.getHttpStatusCode());
        assertNotNull(result.getResponseTimeMs());
        assertNotNull(result.getErrorMessage());

        server.verify();
    }

    @Test
    void shouldReturnDownWhenConnectionFails() {

        RestClient.Builder builder = RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        MonitorHttpClient monitorHttpClient =
                new MonitorHttpClient(builder.build());

        server.expect(requestTo("https://example.com"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(request -> {
                    throw new IOException("Connection failed");
                });

        MonitorCheckResult result =
                monitorHttpClient.checkUrl("https://example.com");

        assertEquals(MonitorStatus.DOWN, result.getStatus());
        assertNull(result.getHttpStatusCode());
        assertNotNull(result.getResponseTimeMs());
        assertNotNull(result.getErrorMessage());

        server.verify();
    }

}
