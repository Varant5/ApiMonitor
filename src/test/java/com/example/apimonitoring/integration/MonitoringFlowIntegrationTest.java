package com.example.apimonitoring.integration;

import com.example.apimonitoring.TestcontainersConfiguration;
import org.springframework.context.annotation.Import;
import com.example.apimonitoring.entity.Monitor;
import com.example.apimonitoring.entity.MonitorCheck;
import com.example.apimonitoring.monitoring.MonitorScheduler;
import com.example.apimonitoring.repository.MonitorCheckRepository;
import com.example.apimonitoring.repository.MonitorRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "monitoring.scheduler.enabled=false",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MonitoringFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MonitorRepository monitorRepository;

    @Autowired
    private MonitorCheckRepository monitorCheckRepository;

    @Autowired
    private MonitorScheduler monitorScheduler;

    private HttpServer fakeApi;

    private final AtomicInteger fakeApiStatus = new AtomicInteger(200);

    @BeforeEach
    void startFakeApi() throws IOException {
        fakeApi = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        fakeApi.createContext("/health", exchange -> {
            byte[] body = "OK".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(fakeApiStatus.get(), body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        fakeApi.start();
    }

    @AfterEach
    void cleanUp() {
        fakeApi.stop(0);
        monitorCheckRepository.deleteAll();
        monitorRepository.deleteAll();
    }

    @Test
    void shouldMonitorApiAndCalculateStatistics() throws Exception {

        mockMvc.perform(post("/api/monitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Fake API",
                                  "url": "%s",
                                  "checkIntervalSeconds": 60
                                }
                                """.formatted(fakeApiUrl())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.enabled").value(true));

        Monitor monitor = monitorRepository.findByUrl(fakeApiUrl()).orElseThrow();
        Long id = monitor.getId();

        mockMvc.perform(post("/api/monitors/{id}/check", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.httpStatusCode").value(200))
                .andExpect(jsonPath("$.responseTimeMs").isNumber());

        fakeApiStatus.set(500);

        mockMvc.perform(post("/api/monitors/{id}/check", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.httpStatusCode").value(500));

        List<MonitorCheck> savedChecks =
                monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(id);

        assertThat(savedChecks).hasSize(2);
        assertThat(savedChecks)
                .extracting(MonitorCheck::getStatus)
                .containsExactly("DOWN", "UP");

        mockMvc.perform(get("/api/monitors/{id}/checks", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].status").value("DOWN"))
                .andExpect(jsonPath("$[1].status").value("UP"));

        mockMvc.perform(get("/api/monitors/{id}/statistics", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalChecks").value(2))
                .andExpect(jsonPath("$.failedChecks").value(1))
                .andExpect(jsonPath("$.uptime").value(50.0));
    }

    @Test
    void schedulerShouldCheckOnlyEnabledMonitorsAndRespectInterval() throws Exception {

        Monitor enabled = saveMonitor("Enabled", fakeApiUrl() + "?a", true);
        Monitor disabled = saveMonitor("Disabled", fakeApiUrl() + "?b", false);

        monitorScheduler.runChecks();

        assertThat(checksOf(enabled)).hasSize(1);
        assertThat(checksOf(disabled)).isEmpty();

        monitorScheduler.runChecks();

        assertThat(checksOf(enabled)).hasSize(1);
        assertThat(checksOf(disabled)).isEmpty();
    }

    @Test
    void shouldReturn404WhenCheckingNonExistingMonitor() throws Exception {

        mockMvc.perform(post("/api/monitors/{id}/check", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Monitor not found"));

        assertThat(monitorCheckRepository.count()).isZero();
    }

    @Test
    void shouldDeleteMonitorTogetherWithItsCheckHistory() throws Exception {

        Monitor monitor = saveMonitor("To delete", fakeApiUrl(), true);

        mockMvc.perform(post("/api/monitors/{id}/check", monitor.getId()))
                .andExpect(status().isOk());

        assertThat(checksOf(monitor)).hasSize(1);

        mockMvc.perform(delete("/api/monitors/{id}", monitor.getId()))
                .andExpect(status().isNoContent());

        assertThat(monitorRepository.findById(monitor.getId())).isEmpty();
        assertThat(checksOf(monitor)).isEmpty();
    }

    private String fakeApiUrl() {
        return "http://localhost:" + fakeApi.getAddress().getPort() + "/health";
    }

    private Monitor saveMonitor(String name, String url, boolean isEnabled) {
        Monitor monitor = new Monitor();
        monitor.setName(name);
        monitor.setUrl(url);
        monitor.setCheckIntervalSeconds(60);
        monitor.setEnabled(isEnabled);
        return monitorRepository.save(monitor);
    }

    private List<MonitorCheck> checksOf(Monitor monitor) {
        return monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(monitor.getId());
    }
}
