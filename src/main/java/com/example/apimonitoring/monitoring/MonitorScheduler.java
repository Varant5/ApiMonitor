package com.example.apimonitoring.monitoring;

import com.example.apimonitoring.entity.Monitor;
import com.example.apimonitoring.entity.MonitorCheck;
import com.example.apimonitoring.repository.MonitorCheckRepository;
import com.example.apimonitoring.service.MonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
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

    /** How often the scheduler wakes up. */
    static final Duration TICK = Duration.ofSeconds(10);

    /**
     * checkedAt is stored after the HTTP request finishes, i.e. a little after the tick started.
     * Without a tolerance, the next tick (exactly TICK later) would be a fraction of a second "too early",
     * the check would be skipped and a 10 s monitor would really run every 20 s (60 s -> every 70 s).
     * Half a tick absorbs that delay (and a slow response) while never checking twice in one tick.
     */
    static final Duration TOLERANCE = TICK.dividedBy(2);

    @Scheduled(fixedRate = 10000) // = TICK
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

    boolean isCheckDue(Monitor monitor) {

        Optional<MonitorCheck> lastCheck =
                monitorCheckRepository.findTopByMonitorIdOrderByCheckedAtDesc(monitor.getId());

        if (lastCheck.isEmpty()) {
            return true;
        }

        LocalDateTime nextCheckAt = lastCheck.get()
                .getCheckedAt()
                .plusSeconds(monitor.getCheckIntervalSeconds())
                .minus(TOLERANCE);

        return !LocalDateTime.now().isBefore(nextCheckAt);
    }
}
