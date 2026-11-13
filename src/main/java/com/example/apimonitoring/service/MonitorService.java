package com.example.apimonitoring.service;

import com.example.apimonitoring.dto.UpdateMonitorStatusRequest;
import com.example.apimonitoring.entity.Monitor;
import com.example.apimonitoring.entity.MonitorCheck;
import com.example.apimonitoring.exception.MonitorNotFoundException;
import com.example.apimonitoring.monitoring.MonitorCheckResult;
import com.example.apimonitoring.monitoring.MonitorHttpClient;
import com.example.apimonitoring.repository.MonitorCheckRepository;
import com.example.apimonitoring.repository.MonitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.apimonitoring.dto.CreateMonitorRequest;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MonitorService {

    private final MonitorRepository monitorRepository;
    private final MonitorCheckRepository monitorCheckRepository;
    private final MonitorHttpClient monitorHttpClient;

    public List<Monitor> getAllMonitors() {
        return monitorRepository.findAll();
    }

    public Monitor createMonitor(CreateMonitorRequest request) {
        Monitor monitor = new Monitor();

        monitor.setName(request.getName());
        monitor.setUrl(request.getUrl());
        monitor.setCheckIntervalSeconds(request.getCheckIntervalSeconds());

        return monitorRepository.save(monitor);
    }

    public Monitor getMonitorById(Long id) {
        return monitorRepository.findById(id)
                .orElseThrow(() -> new MonitorNotFoundException(
                        "Monitor not found with id: " + id
                ));
    }

    public void deleteMonitor(Long id) {
        Monitor monitor = getMonitorById(id);
        monitorRepository.delete(monitor);
    }

    public Monitor updateMonitorStatus(Long id, UpdateMonitorStatusRequest request) {
        Monitor monitor = getMonitorById(id);
        monitor.setEnabled(request.getEnabled());
        return monitorRepository.save(monitor);
    }

    public MonitorCheck checkMonitor(Long monitorId) {

        Monitor monitor = monitorRepository.findById(monitorId)
                .orElseThrow(() -> new MonitorNotFoundException(
                        "Monitor not found with id: " + monitorId
                ));

        MonitorCheckResult result = monitorHttpClient.checkUrl(monitor.getUrl());

        MonitorCheck monitorCheck = new MonitorCheck();

        monitorCheck.setMonitor(monitor);
        monitorCheck.setStatus(result.getStatus().name());
        monitorCheck.setHttpStatusCode(result.getHttpStatusCode());

        if (result.getResponseTimeMs() != null) {
            monitorCheck.setResponseTimeMs(
                    Math.toIntExact(result.getResponseTimeMs())
            );
        }

        return monitorCheckRepository.save(monitorCheck);
    }
}