package com.example.apimonitoring.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "monitoring.scheduler.enabled=false")
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeOpenApiDocumentation() throws Exception {

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("API Monitoring System"))
                .andExpect(jsonPath("$.paths['/api/monitors']").exists())
                .andExpect(jsonPath("$.paths['/api/monitors/{id}/statistics']").exists());
    }

    @Test
    void shouldDocumentEndpointsAndSchemas() throws Exception {

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/monitors'].post.summary").value("Create a monitor"))
                .andExpect(jsonPath("$.paths['/api/monitors'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/monitors'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/monitors/{id}'].get.responses['404']").exists())
                .andExpect(jsonPath("$.components.schemas.CreateMonitorRequest.properties.url.example")
                        .value("https://api.github.com"))
                .andExpect(jsonPath("$.components.schemas.ErrorResponse").exists());
    }

    @Test
    void shouldExposeSwaggerUi() throws Exception {

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
