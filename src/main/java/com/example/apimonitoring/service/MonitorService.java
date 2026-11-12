package com.example.apimonitoring.service;

import com.example.apimonitoring.entity.Monitor;
import com.example.apimonitoring.repository.MonitorRepository;
import org.springframework.stereotype.Service;
import com.example.apimonitoring.dto.CreateMonitorRequest;

import java.util.List;

@Service
public class MonitorService {

    private final MonitorRepository monitorRepository;

    public MonitorService(MonitorRepository monitorRepository) {
        this.monitorRepository = monitorRepository;
    }

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
}