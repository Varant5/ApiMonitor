package com.example.apimonitoring.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "Data required to register a new monitor")
public class CreateMonitorRequest {

    @Schema(description = "Human-readable monitor name", example = "GitHub API")
    @NotBlank(message = "Name is required")
    private String name;

    @Schema(description = "URL that will be checked with HTTP GET", example = "https://api.github.com")
    @NotBlank(message = "URL is required")
    private String url;

    @Schema(description = "How often the URL is checked, in seconds (minimum 10)", example = "60", minimum = "10")
    @NotNull(message = "Check interval is required")
    @Min(value = 10, message = "Check interval must be at least 10 seconds")
    private Integer checkIntervalSeconds;

    public CreateMonitorRequest() {
    }

}
