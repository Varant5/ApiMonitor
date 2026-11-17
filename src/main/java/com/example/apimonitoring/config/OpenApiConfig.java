package com.example.apimonitoring.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

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

                                **Authentication:** register at `/api/auth/register`, log in at `/api/auth/login`,
                                then click **Authorize** and paste the `accessToken`.
                                """))
                // "Authorize" button in Swagger UI
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                // every endpoint requires the token unless it says otherwise (@SecurityRequirements)
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
