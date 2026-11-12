package com.example.apimonitoring.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CreateMonitorRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "URL is required")
    private String url;

    @NotNull(message = "Check interval is required")
    @Min(value = 10, message = "Check interval must be at least 10 seconds")
    private Integer checkIntervalSeconds;

    public CreateMonitorRequest() {
    }

}
