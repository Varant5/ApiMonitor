package com.example.apimonitoring.dto;

import com.example.apimonitoring.entity.MonitorCheck;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Result of a single check of a monitored API")
public record MonitorCheckResponse(

        @Schema(description = "Check id", example = "42")
        Long id,

        @Schema(description = "Id of the checked monitor", example = "1")
        Long monitorId,

        @Schema(description = "UP for a 2xx response, DOWN otherwise", example = "UP",
                allowableValues = {"UP", "DOWN"})
        String status,

        @Schema(description = "HTTP status code; null on timeout or connection error",
                example = "200", nullable = true)
        Integer httpStatusCode,

        @Schema(description = "Response time in milliseconds", example = "137")
        Integer responseTimeMs,

        @Schema(description = "When the check was performed", example = "2026-11-16T10:20:00Z")
        Instant checkedAt
) {

    public static MonitorCheckResponse from(MonitorCheck check) {
        return new MonitorCheckResponse(
                check.getId(),
                check.getMonitor() != null ? check.getMonitor().getId() : null,
                check.getStatus(),
                check.getHttpStatusCode(),
                check.getResponseTimeMs(),
                check.getCheckedAt()
        );
    }
}
