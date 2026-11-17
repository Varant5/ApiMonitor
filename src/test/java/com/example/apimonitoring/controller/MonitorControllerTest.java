package com.example.apimonitoring.controller;

import com.example.apimonitoring.dto.CreateMonitorRequest;
import com.example.apimonitoring.dto.MonitorStatisticsResponse;
import com.example.apimonitoring.dto.UpdateMonitorStatusRequest;
import com.example.apimonitoring.entity.Monitor;
import com.example.apimonitoring.entity.MonitorCheck;
import com.example.apimonitoring.exception.MonitorNotFoundException;
import com.example.apimonitoring.security.SecurityConfig;
import com.example.apimonitoring.service.MonitorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import com.example.apimonitoring.security.WithMockJwtUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MonitorController.class)
@Import(SecurityConfig.class)       // our real security rules, not Spring Boot's defaults
@WithMockJwtUser(userId = 1)        // every request runs as logged-in user with id 1
class MonitorControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonitorService monitorService;

    @Test
    void shouldReturnAllMonitors() throws Exception {

        when(monitorService.getMonitorsOfUser(USER_ID)).thenReturn(List.of(
                createMonitor(1L, "Google", "https://google.com"),
                createMonitor(2L, "GitHub", "https://github.com")
        ));

        mockMvc.perform(get("/api/monitors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Google"))
                .andExpect(jsonPath("$[1].url").value("https://github.com"));
    }

    @Test
    void shouldReturnMonitorById() throws Exception {

        when(monitorService.getMonitorById(1L, USER_ID))
                .thenReturn(createMonitor(1L, "Google", "https://google.com"));

        mockMvc.perform(get("/api/monitors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Google"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void shouldReturn404WhenMonitorDoesNotExist() throws Exception {

        when(monitorService.getMonitorById(99L, USER_ID))
                .thenThrow(new MonitorNotFoundException("Monitor with id 99 not found"));

        mockMvc.perform(get("/api/monitors/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Monitor not found"))
                .andExpect(jsonPath("$.errors[0]").value("Monitor with id 99 not found"));
    }

    @Test
    void shouldCreateMonitor() throws Exception {

        when(monitorService.createMonitor(any(CreateMonitorRequest.class), eq(USER_ID)))
                .thenReturn(createMonitor(1L, "Google", "https://google.com"));

        String body = """
                {
                  "name": "Google",
                  "url": "https://google.com",
                  "checkIntervalSeconds": 60
                }
                """;

        mockMvc.perform(post("/api/monitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Google"));

        // the monitor is created for the logged-in user (id from the JWT)
        verify(monitorService).createMonitor(any(CreateMonitorRequest.class), eq(USER_ID));
    }

    @Test
    void shouldReturn400WhenCreateRequestIsInvalid() throws Exception {

        String body = """
                {
                  "name": "",
                  "url": "",
                  "checkIntervalSeconds": 5
                }
                """;

        mockMvc.perform(post("/api/monitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors", containsInAnyOrder(
                        "Name is required",
                        "URL is required",
                        "Check interval must be at least 10 seconds"
                )));

        verifyNoInteractions(monitorService);
    }

    @Test
    void shouldReturn400WhenCheckIntervalIsMissing() throws Exception {

        String body = """
                {
                  "name": "Google",
                  "url": "https://google.com"
                }
                """;

        mockMvc.perform(post("/api/monitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("Check interval is required"));

        verifyNoInteractions(monitorService);
    }

    @Test
    void shouldDeleteMonitor() throws Exception {

        mockMvc.perform(delete("/api/monitors/1"))
                .andExpect(status().isNoContent());

        verify(monitorService).deleteMonitor(1L, USER_ID);
    }

    @Test
    void shouldReturn404WhenDeletingNonExistingMonitor() throws Exception {

        doThrow(new MonitorNotFoundException("Monitor with id 99 not found"))
                .when(monitorService).deleteMonitor(99L, USER_ID);

        mockMvc.perform(delete("/api/monitors/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateMonitorStatus() throws Exception {

        Monitor disabled = createMonitor(1L, "Google", "https://google.com");
        disabled.setEnabled(false);

        when(monitorService.updateMonitorStatus(eq(1L), any(UpdateMonitorStatusRequest.class), eq(USER_ID)))
                .thenReturn(disabled);

        mockMvc.perform(patch("/api/monitors/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "enabled": false }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void shouldReturn400WhenEnabledIsMissing() throws Exception {

        mockMvc.perform(patch("/api/monitors/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("Enabled status is required"));

        verifyNoInteractions(monitorService);
    }

    @Test
    void shouldReturnChecksWithDefaultLimit() throws Exception {

        when(monitorService.getMonitorChecks(1L, 20, USER_ID))
                .thenReturn(List.of(createCheck("UP", 200, 120)));

        mockMvc.perform(get("/api/monitors/1/checks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("UP"))
                .andExpect(jsonPath("$[0].httpStatusCode").value(200));

        verify(monitorService).getMonitorChecks(1L, 20, USER_ID);
    }

    @Test
    void shouldReturn400WhenLimitIsOutOfRange() throws Exception {

        when(monitorService.getMonitorChecks(1L, 500, USER_ID))
                .thenThrow(new IllegalArgumentException("Limit must be between 1 and 100"));

        mockMvc.perform(get("/api/monitors/1/checks").param("limit", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request"))
                .andExpect(jsonPath("$.errors[0]").value("Limit must be between 1 and 100"));
    }

    @Test
    void shouldPassCustomLimitToService() throws Exception {

        when(monitorService.getMonitorChecks(1L, 5, USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/monitors/1/checks").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(monitorService).getMonitorChecks(1L, 5, USER_ID);
    }

    @Test
    void shouldReturnMonitorStatistics() throws Exception {

        when(monitorService.getMonitorStatistics(1L, USER_ID))
                .thenReturn(new MonitorStatisticsResponse(10, 2, 80.0, 150.5, 90, 300));

        mockMvc.perform(get("/api/monitors/1/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalChecks").value(10))
                .andExpect(jsonPath("$.failedChecks").value(2))
                .andExpect(jsonPath("$.uptime").value(80.0))
                .andExpect(jsonPath("$.averageResponseTimeMs").value(150.5))
                .andExpect(jsonPath("$.minResponseTimeMs").value(90))
                .andExpect(jsonPath("$.maxResponseTimeMs").value(300));
    }

    private Monitor createMonitor(Long id, String name, String url) {
        Monitor monitor = new Monitor();
        monitor.setId(id);
        monitor.setName(name);
        monitor.setUrl(url);
        monitor.setCheckIntervalSeconds(60);
        return monitor;
    }

    @Test
    void shouldReturnMonitorIdInsteadOfNestedMonitorInCheck() throws Exception {

        MonitorCheck check = createCheck("UP", 200, 120);
        check.setMonitor(createMonitor(1L, "Google", "https://google.com"));

        when(monitorService.checkMonitor(1L, USER_ID)).thenReturn(check);

        mockMvc.perform(post("/api/monitors/1/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monitorId").value(1))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.monitor").doesNotExist());
    }

    private MonitorCheck createCheck(String status, int httpCode, int responseTimeMs) {
        MonitorCheck check = new MonitorCheck();
        check.setStatus(status);
        check.setHttpStatusCode(httpCode);
        check.setResponseTimeMs(responseTimeMs);
        return check;
    }
}
