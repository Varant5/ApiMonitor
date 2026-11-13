package com.example.apimonitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonitorStatisticsResponse {

    private long totalChecks;
    private long failedChecks;
    private double uptime;
    private double averageResponseTimeMs;
    private int minResponseTimeMs;
    private int maxResponseTimeMs;
}
