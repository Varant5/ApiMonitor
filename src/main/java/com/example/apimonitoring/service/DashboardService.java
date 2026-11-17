package com.example.apimonitoring.service;

import com.example.apimonitoring.dto.DashboardResponse;
import com.example.apimonitoring.dto.DashboardResponse.MonitorSummary;
import com.example.apimonitoring.entity.Monitor;
import com.example.apimonitoring.entity.MonitorCheck;
import com.example.apimonitoring.repository.MonitorCheckRepository;
import com.example.apimonitoring.repository.MonitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the dashboard in one call, so the frontend does not need a request per monitor.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MonitorRepository monitorRepository;
    private final MonitorCheckRepository monitorCheckRepository;

    public DashboardResponse getDashboard(Long userId) {

        List<Monitor> monitors = monitorRepository.findAllByOwnerIdOrderByIdAsc(userId);

        List<MonitorSummary> summaries = new ArrayList<>();
        int up = 0, down = 0, pending = 0, paused = 0;
        long allChecks = 0, allUpChecks = 0;

        for (Monitor monitor : monitors) {

            // newest first -> element 0 is the latest check
            List<MonitorCheck> checks =
                    monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(monitor.getId());

            MonitorCheck latest = checks.isEmpty() ? null : checks.getFirst();
            long upChecks = checks.stream().filter(c -> "UP".equals(c.getStatus())).count();

            allChecks += checks.size();
            allUpChecks += upChecks;

            if (!monitor.isEnabled()) {
                paused++;
            } else if (latest == null) {
                pending++;
            } else if ("UP".equals(latest.getStatus())) {
                up++;
            } else {
                down++;
            }

            summaries.add(new MonitorSummary(
                    monitor.getId(),
                    monitor.getName(),
                    monitor.getUrl(),
                    monitor.isEnabled(),
                    monitor.getCheckIntervalSeconds(),
                    latest != null ? latest.getStatus() : null,
                    latest != null ? latest.getCheckedAt() : null,
                    latest != null ? latest.getResponseTimeMs() : null,
                    latest != null ? latest.getHttpStatusCode() : null,
                    percentage(upChecks, checks.size()),
                    checks.size()
            ));
        }

        return new DashboardResponse(
                monitors.size(),
                up,
                down,
                pending,
                paused,
                percentage(allUpChecks, allChecks),
                summaries
        );
    }

    private double percentage(long part, long total) {
        return total == 0 ? 0.0 : (double) part / total * 100;
    }
}
