package com.example.apimonitoring.monitoring;

import lombok.Getter;

@Getter
public class MonitorCheckResult {

    private MonitorStatus status;
    private Integer httpStatusCode;
    private Long responseTimeMs;
    private String errorMessage;

    public MonitorCheckResult(
            MonitorStatus status,
            Integer httpStatusCode,
            Long responseTimeMs,
            String errorMessage) {
        this.status = status;
        this.httpStatusCode = httpStatusCode;
        this.responseTimeMs = responseTimeMs;
        this.errorMessage = errorMessage;
    }

}
