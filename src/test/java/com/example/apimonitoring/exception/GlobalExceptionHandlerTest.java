package com.example.apimonitoring.exception;

import com.example.apimonitoring.controller.MonitorController;
import com.example.apimonitoring.security.SecurityConfig;
import com.example.apimonitoring.service.MonitorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MonitorController.class)
@Import(SecurityConfig.class)
@WithMockUser
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonitorService monitorService;

    @Test
    void shouldReturn400WhenIdIsNotANumber() throws Exception {

        mockMvc.perform(get("/api/monitors/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request"))
                .andExpect(jsonPath("$.errors[0]")
                        .value("Parameter 'id' has invalid value 'abc', expected Long"));

        verifyNoInteractions(monitorService);
    }

    @Test
    void shouldReturn400WhenLimitIsNotANumber() throws Exception {

        mockMvc.perform(get("/api/monitors/1/checks").param("limit", "ten"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value(containsString("'limit'")));
    }

    @Test
    void shouldReturn400WhenJsonIsMalformed() throws Exception {

        mockMvc.perform(post("/api/monitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Google\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed request body"));

        verifyNoInteractions(monitorService);
    }

    @Test
    void shouldReturn400WhenBodyIsMissing() throws Exception {

        mockMvc.perform(post("/api/monitors").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void shouldReturn400WhenFieldHasWrongType() throws Exception {

        mockMvc.perform(post("/api/monitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Google", "url": "https://google.com", "checkIntervalSeconds": "often" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void shouldReturn404ForUnknownEndpoint() throws Exception {

        mockMvc.perform(get("/api/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Not found"));
    }

    @Test
    void shouldReturn405WhenMethodIsNotSupported() throws Exception {

        mockMvc.perform(put("/api/monitors/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.message").value("Method not allowed"))
                .andExpect(jsonPath("$.errors[0]").value(containsString("PUT")));
    }

    @Test
    void shouldReturn415WhenContentTypeIsNotJson() throws Exception {

        mockMvc.perform(post("/api/monitors")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name=Google"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.message").value("Unsupported media type"));
    }

    @Test
    void shouldReturn500WithoutInternalDetailsForUnexpectedError() throws Exception {

        when(monitorService.getAllMonitors())
                .thenThrow(new RuntimeException("DB password is secret123"));

        mockMvc.perform(get("/api/monitors"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.errors[0]").value("An unexpected error occurred"))
                // wewnętrzny komunikat wyjątku nie może wyciec do klienta
                .andExpect(content().string(not(containsString("secret123"))));
    }
}
