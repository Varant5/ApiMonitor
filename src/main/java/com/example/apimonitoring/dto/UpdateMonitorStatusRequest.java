package com.example.apimonitoring.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMonitorStatusRequest {

    @NotNull(message = "Enabled status is required")
    private Boolean enabled;

}