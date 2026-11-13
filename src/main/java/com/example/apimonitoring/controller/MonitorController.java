package com.example.apimonitoring.controller;

import com.example.apimonitoring.dto.CreateMonitorRequest;
import com.example.apimonitoring.dto.UpdateMonitorStatusRequest;
import com.example.apimonitoring.entity.Monitor;
import com.example.apimonitoring.entity.MonitorCheck;
import com.example.apimonitoring.service.MonitorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitors")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping
    public List<Monitor> getAllMonitors() {
        return monitorService.getAllMonitors();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Monitor createMonitor(@Valid @RequestBody CreateMonitorRequest request) {
        return monitorService.createMonitor(request);
    }

    @GetMapping("/{id}")
    public Monitor getMonitorById(@PathVariable Long id) {
        return monitorService.getMonitorById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMonitor(@PathVariable Long id) {
        monitorService.deleteMonitor(id);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Monitor> updateMonitorStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMonitorStatusRequest request
    ) {
        Monitor updatedMonitor = monitorService.updateMonitorStatus(id, request);

        return ResponseEntity.ok(updatedMonitor);
    }

    @PostMapping("/{id}/check")
    public MonitorCheck checkMonitor(@PathVariable Long id) {
        return monitorService.checkMonitor(id);
    }
}
