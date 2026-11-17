package com.example.apimonitoring.integration;

import com.example.apimonitoring.TestcontainersConfiguration;
import com.example.apimonitoring.entity.Monitor;
import com.example.apimonitoring.entity.User;
import com.example.apimonitoring.repository.MonitorCheckRepository;
import com.example.apimonitoring.repository.MonitorRepository;
import com.example.apimonitoring.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Two real accounts with real tokens: each user must see and change only their own monitors.
 */
@SpringBootTest(properties = "monitoring.scheduler.enabled=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MonitorOwnershipIntegrationTest {

    private static final String PASSWORD = "Secret123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MonitorRepository monitorRepository;

    @Autowired
    private MonitorCheckRepository monitorCheckRepository;

    private String aliceToken;
    private String bobToken;
    private Long alicesMonitorId;

    @BeforeEach
    void setUp() throws Exception {
        aliceToken = registerAndLogin("alice@example.com");
        bobToken = registerAndLogin("bob@example.com");

        String body = mockMvc.perform(post("/api/monitors")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Alice's API", "url": "https://alice.example.com", "checkIntervalSeconds": 60 }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        alicesMonitorId = ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void monitorShouldBeOwnedByItsCreator() {

        Monitor monitor = monitorRepository.findById(alicesMonitorId).orElseThrow();
        User alice = userRepository.findByEmail("alice@example.com").orElseThrow();

        assertThat(monitor.getOwner().getId()).isEqualTo(alice.getId());
    }

    @Test
    void usersShouldOnlySeeTheirOwnMonitors() throws Exception {

        mockMvc.perform(get("/api/monitors").header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Alice's API"));

        mockMvc.perform(get("/api/monitors").header(HttpHeaders.AUTHORIZATION, "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void otherUserShouldGet404ForEveryOperationOnSomeoneElsesMonitor() throws Exception {

        String bob = "Bearer " + bobToken;
        Long id = alicesMonitorId;

        mockMvc.perform(get("/api/monitors/{id}", id).header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Monitor not found"));

        mockMvc.perform(patch("/api/monitors/{id}", id).header(HttpHeaders.AUTHORIZATION, bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"enabled\": false }"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/monitors/{id}/check", id).header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/monitors/{id}/checks", id).header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/monitors/{id}/statistics", id).header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/monitors/{id}/uptime", id).header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/monitors/{id}", id).header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isNotFound());

        // nothing changed for Alice: monitor still exists, still enabled, no checks were run
        Monitor monitor = monitorRepository.findById(id).orElseThrow();
        assertThat(monitor.isEnabled()).isTrue();
        assertThat(monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(id)).isEmpty();
    }

    @Test
    void ownerShouldStillHaveFullAccess() throws Exception {

        mockMvc.perform(get("/api/monitors/{id}", alicesMonitorId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice's API"));

        mockMvc.perform(delete("/api/monitors/{id}", alicesMonitorId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken))
                .andExpect(status().isNoContent());

        assertThat(monitorRepository.findById(alicesMonitorId)).isEmpty();
    }

    @Test
    void deletingUserShouldDeleteTheirMonitors() {

        User alice = userRepository.findByEmail("alice@example.com").orElseThrow();

        userRepository.delete(alice);

        assertThat(monitorRepository.findById(alicesMonitorId)).isEmpty();
    }

    // ---------- helpers ----------

    private String registerAndLogin(String email) throws Exception {

        String credentials = """
                { "email": "%s", "password": "%s" }
                """.formatted(email, PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.accessToken");
    }
}
