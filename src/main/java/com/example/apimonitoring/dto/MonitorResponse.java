package com.example.apimonitoring.dto;

import com.example.apimonitoring.entity.Monitor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Monitored API")
public record MonitorResponse(

        @Schema(description = "Monitor id", example = "1")
        Long id,

        @Schema(description = "Human-readable monitor name", example = "GitHub API")
        String name,

        @Schema(description = "URL checked with HTTP GET", example = "https://api.github.com")
        String url,

        @Schema(description = "How often the URL is checked, in seconds", example = "60")
        Integer checkIntervalSeconds,

        @Schema(description = "Whether the scheduler checks this monitor", example = "true")
        boolean enabled,

        @Schema(description = "When the monitor was created", example = "2026-11-16T10:15:30Z")
        Instant createdAt
) {

    public static MonitorResponse from(Monitor monitor) {
        return new MonitorResponse(
                monitor.getId(),
                monitor.getName(),
                monitor.getUrl(),
                monitor.getCheckIntervalSeconds(),
                monitor.isEnabled(),
                monitor.getCreatedAt()
        );
    }
}
