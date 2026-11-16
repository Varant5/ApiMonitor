package com.example.apimonitoring.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiMonitoringOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Monitoring System")
                        .version("0.0.1")
                        .description("""
                                REST API for monitoring the availability of external APIs.
                                Allows you to manage monitors, manually trigger checks,
                                and retrieve history and statistics (uptime, response times).
                                """));
    }
}
