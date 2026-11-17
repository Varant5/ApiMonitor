package com.example.apimonitoring.controller;

import com.example.apimonitoring.dto.DashboardResponse;
import com.example.apimonitoring.security.CurrentUserId;
import com.example.apimonitoring.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
            summary = "Get dashboard overview",
            description = "Counts of monitors by state (up / down / pending / paused), overall uptime "
                    + "and the latest result of every monitor of the logged-in user."
    )
    @GetMapping
    public DashboardResponse getDashboard(@CurrentUserId Long userId) {
        return dashboardService.getDashboard(userId);
    }
}
