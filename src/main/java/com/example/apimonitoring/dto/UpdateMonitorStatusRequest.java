package com.example.apimonitoring.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Enables or disables monitoring")
public class UpdateMonitorStatusRequest {

    @Schema(description = "`true` to enable monitoring, `false` to pause it", example = "false")
    @NotNull(message = "Enabled status is required")
    private Boolean enabled;

}
