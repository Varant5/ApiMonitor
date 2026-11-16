package com.example.apimonitoring.service;

import com.example.apimonitoring.dto.MonitorStatisticsResponse;
import com.example.apimonitoring.dto.UpdateMonitorStatusRequest;
import com.example.apimonitoring.entity.Monitor;
import com.example.apimonitoring.entity.MonitorCheck;
import com.example.apimonitoring.exception.MonitorNotFoundException;
import com.example.apimonitoring.monitoring.MonitorCheckResult;
import com.example.apimonitoring.monitoring.MonitorHttpClient;
import com.example.apimonitoring.repository.MonitorCheckRepository;
import com.example.apimonitoring.repository.MonitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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

        Monitor monitor = getMonitorById(monitorId);

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

    public List<MonitorCheck> getMonitorChecks(Long monitorId) {
        getMonitorById(monitorId);
        return monitorCheckRepository
                .findByMonitorIdOrderByCheckedAtDesc(monitorId);
    }

    public List<MonitorCheck> getMonitorChecks(Long monitorId, int limit) {

        getMonitorById(monitorId);

        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException(
                    "Limit must be between 1 and 100"
            );
        }

        return monitorCheckRepository
                .findByMonitorIdOrderByCheckedAtDesc(
                        monitorId,
                        PageRequest.of(0, limit)
                );
    }

    public double calculateUptime(Long monitorId) {

        getMonitorById(monitorId);

        List<MonitorCheck> checks =
                monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(monitorId);

        if (checks.isEmpty()) {
            return 0.0;
        }

        long successfulChecks = checks.stream()
                .filter(check -> "UP".equals(check.getStatus()))
                .count();

        return (double) successfulChecks / checks.size() * 100;
    }

    public double calculateAverageResponseTime(Long monitorId) {

        getMonitorById(monitorId);

        List<MonitorCheck> checks =
                monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(monitorId);

        return checks.stream()
                .filter(check -> check.getResponseTimeMs() != null)
                .mapToInt(MonitorCheck::getResponseTimeMs)
                .average()
                .orElse(0.0);
    }

    public int calculateMinResponseTime(Long monitorId) {

        getMonitorById(monitorId);

        List<MonitorCheck> checks =
                monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(monitorId);

        return checks.stream()
                .filter(check -> check.getResponseTimeMs() != null)
                .mapToInt(MonitorCheck::getResponseTimeMs)
                .min()
                .orElse(0);
    }

    public int calculateMaxResponseTime(Long monitorId) {

        getMonitorById(monitorId);

        List<MonitorCheck> checks =
                monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(monitorId);

        return checks.stream()
                .filter(check -> check.getResponseTimeMs() != null)
                .mapToInt(MonitorCheck::getResponseTimeMs)
                .max()
                .orElse(0);
    }

    public long countFailedChecks(Long monitorId) {

        getMonitorById(monitorId);

        List<MonitorCheck> checks =
                monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(monitorId);

        return checks.stream()
                .filter(check -> "DOWN".equals(check.getStatus()))
                .count();
    }


    public MonitorStatisticsResponse getMonitorStatistics(Long monitorId) {

        getMonitorById(monitorId);

        List<MonitorCheck> checks =
                monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(monitorId);

        long totalChecks = checks.size();

        long failedChecks = checks.stream()
                .filter(check -> "DOWN".equals(check.getStatus()))
                .count();

        long successfulChecks = checks.stream()
                .filter(check -> "UP".equals(check.getStatus()))
                .count();

        double uptime = totalChecks == 0
                ? 0.0
                : (double) successfulChecks / totalChecks * 100;

        java.util.IntSummaryStatistics responseStats = checks.stream()
                .filter(check -> check.getResponseTimeMs() != null)
                .mapToInt(MonitorCheck::getResponseTimeMs)
                .summaryStatistics();

        return new MonitorStatisticsResponse(
                totalChecks,
                failedChecks,
                uptime,
                responseStats.getAverage(),
                responseStats.getCount() == 0 ? 0 : responseStats.getMin(),
                responseStats.getCount() == 0 ? 0 : responseStats.getMax()
        );
    }

}