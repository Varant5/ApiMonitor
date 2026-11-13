package com.example.apimonitoring.monitoring;

import com.example.apimonitoring.entity.Monitor;
import com.example.apimonitoring.entity.MonitorCheck;
import com.example.apimonitoring.repository.MonitorCheckRepository;
import com.example.apimonitoring.service.MonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class MonitorScheduler {

    private final MonitorService monitorService;
    private final MonitorCheckRepository monitorCheckRepository;
    private final Set<Long> monitorsInProgress =
            ConcurrentHashMap.newKeySet();

    @Scheduled(fixedRate = 10000)
    public void runChecks() {

        List<Monitor> monitors = monitorService.getAllMonitors();

        for (Monitor monitor : monitors) {

            if (!monitor.isEnabled()) {
                continue;
            }

            Optional<MonitorCheck> lastCheck =
                    monitorCheckRepository
                            .findTopByMonitorIdOrderByCheckedAtDesc(monitor.getId());

            if (lastCheck.isPresent()) {

                LocalDateTime lastCheckedAt =
                        lastCheck.get().getCheckedAt();

                LocalDateTime nextCheckAt =
                        lastCheckedAt.plusSeconds(
                                monitor.getCheckIntervalSeconds()
                        );

                if (LocalDateTime.now().isBefore(nextCheckAt)) {
                    continue;
                }
            }

            System.out.println("Checking monitor: " + monitor.getName());

            if (monitorsInProgress.add(monitor.getId())) {
                try {
                    monitorService.checkMonitor(monitor.getId());
                } finally {
                    monitorsInProgress.remove(monitor.getId());
                }
            }
        }
    }
}
