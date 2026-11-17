package com.example.apimonitoring.monitoring;

import com.example.apimonitoring.entity.Monitor;
import com.example.apimonitoring.entity.MonitorCheck;
import com.example.apimonitoring.repository.MonitorCheckRepository;
import com.example.apimonitoring.service.MonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorScheduler {

    private final MonitorService monitorService;
    private final MonitorCheckRepository monitorCheckRepository;
    private final Set<Long> monitorsInProgress = ConcurrentHashMap.newKeySet();

    @Scheduled(fixedRate = 10000)
    public void runChecks() {

        for (Monitor monitor : monitorService.getAllMonitorsForScheduling()) {

            if (!monitor.isEnabled() || !isCheckDue(monitor)) {
                continue;
            }

            if (!monitorsInProgress.add(monitor.getId())) {
                continue;
            }

            try {
                log.debug("Checking monitor {} ({})", monitor.getId(), monitor.getName());
                monitorService.runScheduledCheck(monitor.getId());
            } catch (Exception e) {
                log.warn("Check of monitor {} failed: {}", monitor.getId(), e.getMessage());
            } finally {
                monitorsInProgress.remove(monitor.getId());
            }
        }
    }

    private boolean isCheckDue(Monitor monitor) {

        Optional<MonitorCheck> lastCheck =
                monitorCheckRepository.findTopByMonitorIdOrderByCheckedAtDesc(monitor.getId());

        if (lastCheck.isEmpty()) {
            return true;
        }

        LocalDateTime nextCheckAt = lastCheck.get()
                .getCheckedAt()
                .plusSeconds(monitor.getCheckIntervalSeconds());

        return !LocalDateTime.now().isBefore(nextCheckAt);
    }
}
