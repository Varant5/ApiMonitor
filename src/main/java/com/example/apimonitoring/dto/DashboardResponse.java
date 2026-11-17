package com.example.apimonitoring.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Overview of all monitors of the logged-in user")
public record DashboardResponse(

        @Schema(description = "Number of monitors", example = "5")
        int totalMonitors,

        @Schema(description = "Enabled monitors whose last check was UP", example = "3")
        int up,

        @Schema(description = "Enabled monitors whose last check was DOWN", example = "1")
        int down,

        @Schema(description = "Enabled monitors that have not been checked yet", example = "0")
        int pending,

        @Schema(description = "Disabled monitors", example = "1")
        int paused,

        @Schema(description = "Percentage of UP checks across all monitors (0-100)", example = "97.4")
        double overallUptime,

        List<MonitorSummary> monitors
) {

    @Schema(description = "Monitor with its latest result and uptime")
    public record MonitorSummary(

            @Schema(example = "1")
            Long id,

            @Schema(example = "GitHub API")
            String name,

            @Schema(example = "https://api.github.com")
            String url,

            @Schema(example = "true")
            boolean enabled,

            @Schema(example = "60")
            Integer checkIntervalSeconds,

            @Schema(description = "Status of the latest check; null if never checked",
                    example = "UP", nullable = true, allowableValues = {"UP", "DOWN"})
            String lastStatus,

            @Schema(example = "2026-11-17T10:20:00Z", nullable = true)
            Instant lastCheckedAt,

            @Schema(example = "137", nullable = true)
            Integer lastResponseTimeMs,

            @Schema(example = "200", nullable = true)
            Integer lastHttpStatusCode,

            @Schema(description = "Percentage of UP checks of this monitor (0-100)", example = "99.2")
            double uptime,

            @Schema(example = "250")
            long totalChecks
    ) {
    }
}
