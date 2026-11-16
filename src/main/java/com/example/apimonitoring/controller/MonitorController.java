package com.example.apimonitoring.controller;

import com.example.apimonitoring.dto.CreateMonitorRequest;
import com.example.apimonitoring.dto.MonitorCheckResponse;
import com.example.apimonitoring.dto.MonitorResponse;
import com.example.apimonitoring.dto.MonitorStatisticsResponse;
import com.example.apimonitoring.dto.UpdateMonitorStatusRequest;
import com.example.apimonitoring.exception.ErrorResponse;
import com.example.apimonitoring.service.MonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitors")
@Tag(name = "Monitors", description = "Manage monitored APIs, run checks and read their history and statistics")
public class MonitorController {


    private static final String MONITOR_EXAMPLE = """
            {
              "id": 1,
              "name": "GitHub API",
              "url": "https://api.github.com",
              "checkIntervalSeconds": 60,
              "enabled": true,
              "createdAt": "2026-11-16T10:15:30"
            }
            """;

    private static final String MONITOR_LIST_EXAMPLE = "[" + MONITOR_EXAMPLE + "]";

    private static final String CHECK_UP_EXAMPLE = """
            {
              "id": 42,
              "monitorId": 1,
              "status": "UP",
              "responseTimeMs": 137,
              "httpStatusCode": 200,
              "checkedAt": "2026-11-16T10:20:00"
            }
            """;

    private static final String CHECK_LIST_EXAMPLE = """
            [
              { "id": 43, "monitorId": 1, "status": "DOWN", "responseTimeMs": 5003, "httpStatusCode": null, "checkedAt": "2026-11-16T10:21:00" },
              { "id": 42, "monitorId": 1, "status": "UP",   "responseTimeMs": 137,  "httpStatusCode": 200,  "checkedAt": "2026-11-16T10:20:00" }
            ]
            """;

    private static final String NOT_FOUND_EXAMPLE = """
            {
              "status": 404,
              "message": "Monitor not found",
              "errors": ["Monitor not found with id: 99"]
            }
            """;

    private static final String VALIDATION_ERROR_EXAMPLE = """
            {
              "status": 400,
              "message": "Validation failed",
              "errors": ["Name is required", "Check interval must be at least 10 seconds"]
            }
            """;

    private static final String INVALID_LIMIT_EXAMPLE = """
            {
              "status": 400,
              "message": "Invalid request",
              "errors": ["Limit must be between 1 and 100"]
            }
            """;

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }


    @Operation(summary = "List all monitors")
    @ApiResponse(
            responseCode = "200",
            description = "All registered monitors",
            content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = MonitorResponse.class)),
                    examples = @ExampleObject(value = MONITOR_LIST_EXAMPLE)
            )
    )
    @GetMapping
    public List<MonitorResponse> getAllMonitors() {
        return monitorService.getAllMonitors().stream()
                .map(MonitorResponse::from)
                .toList();
    }

    @Operation(
            summary = "Create a monitor",
            description = "Registers a new URL to be monitored. Monitoring is enabled by default "
                    + "and the scheduler will check the URL every `checkIntervalSeconds`."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Monitor created",
                    content = @Content(
                            schema = @Schema(implementation = MonitorResponse.class),
                            examples = @ExampleObject(value = MONITOR_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request body failed validation",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = VALIDATION_ERROR_EXAMPLE)
                    )
            )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MonitorResponse createMonitor(@Valid @RequestBody CreateMonitorRequest request) {
        return MonitorResponse.from(monitorService.createMonitor(request));
    }

    @Operation(summary = "Get a monitor by id")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Monitor found",
                    content = @Content(
                            schema = @Schema(implementation = MonitorResponse.class),
                            examples = @ExampleObject(value = MONITOR_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Monitor does not exist",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = NOT_FOUND_EXAMPLE)
                    )
            )
    })
    @GetMapping("/{id}")
    public MonitorResponse getMonitorById(
            @Parameter(description = "Monitor id", example = "1") @PathVariable Long id
    ) {
        return MonitorResponse.from(monitorService.getMonitorById(id));
    }

    @Operation(summary = "Delete a monitor")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Monitor deleted", content = @Content),
            @ApiResponse(
                    responseCode = "404",
                    description = "Monitor does not exist",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = NOT_FOUND_EXAMPLE)
                    )
            )
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMonitor(
            @Parameter(description = "Monitor id", example = "1") @PathVariable Long id
    ) {
        monitorService.deleteMonitor(id);
    }

    @Operation(
            summary = "Enable or disable a monitor",
            description = "Disabled monitors are skipped by the scheduler. Their history is kept."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Monitor updated",
                    content = @Content(
                            schema = @Schema(implementation = MonitorResponse.class),
                            examples = @ExampleObject(value = MONITOR_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "`enabled` field is missing",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "status": 400,
                                      "message": "Validation failed",
                                      "errors": ["Enabled status is required"]
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Monitor does not exist",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = NOT_FOUND_EXAMPLE)
                    )
            )
    })
    @PatchMapping("/{id}")
    public MonitorResponse updateMonitorStatus(
            @Parameter(description = "Monitor id", example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateMonitorStatusRequest request
    ) {
        return MonitorResponse.from(monitorService.updateMonitorStatus(id, request));
    }


    @Operation(
            summary = "Run a check now",
            description = "Immediately sends an HTTP GET to the monitor's URL, stores and returns the result. "
                    + "A 2xx response is `UP`; any other status code, timeout or connection error is `DOWN` "
                    + "(for timeouts and connection errors `httpStatusCode` is null)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Check executed and saved",
                    content = @Content(
                            schema = @Schema(implementation = MonitorCheckResponse.class),
                            examples = @ExampleObject(value = CHECK_UP_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Monitor does not exist",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = NOT_FOUND_EXAMPLE)
                    )
            )
    })
    @PostMapping("/{id}/check")
    public MonitorCheckResponse checkMonitor(
            @Parameter(description = "Monitor id", example = "1") @PathVariable Long id
    ) {
        return MonitorCheckResponse.from(monitorService.checkMonitor(id));
    }

    @Operation(summary = "Get check history", description = "Returns the most recent checks, newest first.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Check history",
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = MonitorCheckResponse.class)),
                            examples = @ExampleObject(value = CHECK_LIST_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "`limit` is outside the 1-100 range",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = INVALID_LIMIT_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Monitor does not exist",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = NOT_FOUND_EXAMPLE)
                    )
            )
    })
    @GetMapping("/{id}/checks")
    public List<MonitorCheckResponse> getMonitorChecks(
            @Parameter(description = "Monitor id", example = "1") @PathVariable Long id,
            @Parameter(description = "Maximum number of checks to return (1-100)", example = "20")
            @RequestParam(defaultValue = "20") int limit
    ) {
        return monitorService.getMonitorChecks(id, limit).stream()
                .map(MonitorCheckResponse::from)
                .toList();
    }


    @Operation(
            summary = "Get monitor statistics",
            description = "Aggregated statistics over the whole check history: total and failed checks, "
                    + "uptime percentage and response times. Returns zeros when there are no checks yet."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Statistics calculated",
                    content = @Content(schema = @Schema(implementation = MonitorStatisticsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Monitor does not exist",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = NOT_FOUND_EXAMPLE)
                    )
            )
    })
    @GetMapping("/{id}/statistics")
    public MonitorStatisticsResponse getMonitorStatistics(
            @Parameter(description = "Monitor id", example = "1") @PathVariable Long id
    ) {
        return monitorService.getMonitorStatistics(id);
    }

    @Operation(summary = "Get uptime", description = "Percentage (0-100) of checks with status `UP`.")
    @ApiResponse(responseCode = "200", description = "Uptime in percent",
            content = @Content(examples = @ExampleObject(value = "98.5")))
    @GetMapping("/{id}/uptime")
    public double getMonitorUptime(
            @Parameter(description = "Monitor id", example = "1") @PathVariable Long id
    ) {
        return monitorService.calculateUptime(id);
    }

    @Operation(summary = "Get average response time", description = "Average response time in milliseconds.")
    @ApiResponse(responseCode = "200", description = "Average response time (ms)",
            content = @Content(examples = @ExampleObject(value = "142.7")))
    @GetMapping("/{id}/average-response-time")
    public double getAverageResponseTime(
            @Parameter(description = "Monitor id", example = "1") @PathVariable Long id
    ) {
        return monitorService.calculateAverageResponseTime(id);
    }

    @Operation(summary = "Get minimum response time", description = "Fastest response time in milliseconds.")
    @ApiResponse(responseCode = "200", description = "Minimum response time (ms)",
            content = @Content(examples = @ExampleObject(value = "89")))
    @GetMapping("/{id}/min-response-time")
    public int getMinResponseTime(
            @Parameter(description = "Monitor id", example = "1") @PathVariable Long id
    ) {
        return monitorService.calculateMinResponseTime(id);
    }

    @Operation(summary = "Get maximum response time", description = "Slowest response time in milliseconds.")
    @ApiResponse(responseCode = "200", description = "Maximum response time (ms)",
            content = @Content(examples = @ExampleObject(value = "512")))
    @GetMapping("/{id}/max-response-time")
    public int getMaxResponseTime(
            @Parameter(description = "Monitor id", example = "1") @PathVariable Long id
    ) {
        return monitorService.calculateMaxResponseTime(id);
    }

    @Operation(summary = "Count failed checks", description = "Number of checks with status `DOWN`.")
    @ApiResponse(responseCode = "200", description = "Number of failed checks",
            content = @Content(examples = @ExampleObject(value = "3")))
    @GetMapping("/{id}/failed-checks")
    public long getFailedChecks(
            @Parameter(description = "Monitor id", example = "1") @PathVariable Long id
    ) {
        return monitorService.countFailedChecks(id);
    }
}
