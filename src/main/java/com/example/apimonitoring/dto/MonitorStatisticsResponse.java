package com.example.apimonitoring.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Aggregated statistics of a monitor's check history")
public class MonitorStatisticsResponse {

    @Schema(description = "Number of all checks", example = "200")
    private long totalChecks;

    @Schema(description = "Number of checks with status DOWN", example = "3")
    private long failedChecks;

    @Schema(description = "Percentage of successful (UP) checks", example = "98.5")
    private double uptime;

    @Schema(description = "Average response time in milliseconds", example = "142.7")
    private double averageResponseTimeMs;

    @Schema(description = "Fastest response time in milliseconds", example = "89")
    private int minResponseTimeMs;

    @Schema(description = "Slowest response time in milliseconds", example = "512")
    private int maxResponseTimeMs;
}
