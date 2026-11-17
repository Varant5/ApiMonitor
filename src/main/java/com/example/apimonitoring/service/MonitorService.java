package com.example.apimonitoring.service;

import com.example.apimonitoring.dto.CreateMonitorRequest;
import com.example.apimonitoring.dto.MonitorStatisticsResponse;
import com.example.apimonitoring.dto.UpdateMonitorStatusRequest;
import com.example.apimonitoring.entity.Monitor;
import com.example.apimonitoring.entity.MonitorCheck;
import com.example.apimonitoring.exception.MonitorNotFoundException;
import com.example.apimonitoring.monitoring.MonitorCheckResult;
import com.example.apimonitoring.monitoring.MonitorHttpClient;
import com.example.apimonitoring.notification.MonitorStatusChangedEvent;
import com.example.apimonitoring.repository.MonitorCheckRepository;
import com.example.apimonitoring.repository.MonitorRepository;
import com.example.apimonitoring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.IntSummaryStatistics;
import java.util.List;

/**
 * Every public method that serves the API takes the id of the logged-in user
 * and only ever touches that user's monitors.
 */
@Service
@RequiredArgsConstructor
public class MonitorService {

    private final MonitorRepository monitorRepository;
    private final MonitorCheckRepository monitorCheckRepository;
    private final MonitorHttpClient monitorHttpClient;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ---------- monitors ----------

    public List<Monitor> getMonitorsOfUser(Long userId) {
        return monitorRepository.findAllByOwnerIdOrderByIdAsc(userId);
    }

    public Monitor createMonitor(CreateMonitorRequest request, Long userId) {
        Monitor monitor = new Monitor();

        monitor.setName(request.getName());
        monitor.setUrl(request.getUrl());
        monitor.setCheckIntervalSeconds(request.getCheckIntervalSeconds());
        // reference by id only — no extra SELECT for the user
        monitor.setOwner(userRepository.getReferenceById(userId));

        return monitorRepository.save(monitor);
    }

    /**
     * Throws 404 both when the monitor does not exist and when it belongs to someone else,
     * so the API does not reveal which monitor ids exist.
     */
    public Monitor getMonitorById(Long id, Long userId) {
        return monitorRepository.findByIdAndOwnerId(id, userId)
                .orElseThrow(() -> new MonitorNotFoundException(
                        "Monitor not found with id: " + id
                ));
    }

    public void deleteMonitor(Long id, Long userId) {
        Monitor monitor = getMonitorById(id, userId);
        monitorRepository.delete(monitor);
    }

    public Monitor updateMonitorStatus(Long id, UpdateMonitorStatusRequest request, Long userId) {
        Monitor monitor = getMonitorById(id, userId);
        monitor.setEnabled(request.getEnabled());
        return monitorRepository.save(monitor);
    }

    // ---------- checks ----------

    /** Manual check triggered by the owner through the API. */
    public MonitorCheck checkMonitor(Long monitorId, Long userId) {
        return performCheck(getMonitorById(monitorId, userId));
    }

    /** All monitors of all users — used only by the scheduler. */
    public List<Monitor> getAllMonitorsForScheduling() {
        return monitorRepository.findAll();
    }

    /** Automatic check run by the scheduler, which works on behalf of every user. */
    public MonitorCheck runScheduledCheck(Long monitorId) {
        Monitor monitor = monitorRepository.findById(monitorId)
                .orElseThrow(() -> new MonitorNotFoundException(
                        "Monitor not found with id: " + monitorId
                ));
        return performCheck(monitor);
    }

    private MonitorCheck performCheck(Monitor monitor) {

        // status before this check — needed to detect UP -> DOWN and DOWN -> UP
        String previousStatus = monitorCheckRepository
                .findTopByMonitorIdOrderByCheckedAtDesc(monitor.getId())
                .map(MonitorCheck::getStatus)
                .orElse(null);

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

        MonitorCheck saved = monitorCheckRepository.save(monitorCheck);

        if (MonitorStatusChangedEvent.isWorthNotifying(previousStatus, saved.getStatus())) {
            eventPublisher.publishEvent(new MonitorStatusChangedEvent(
                    monitor.getId(),
                    monitor.getName(),
                    monitor.getUrl(),
                    monitor.getOwner() != null ? monitor.getOwner().getId() : null,
                    previousStatus,
                    saved.getStatus(),
                    saved.getHttpStatusCode(),
                    result.getErrorMessage(),
                    saved.getCheckedAt()
            ));
        }

        return saved;
    }

    public List<MonitorCheck> getMonitorChecks(Long monitorId, Long userId) {
        getMonitorById(monitorId, userId);
        return monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(monitorId);
    }

    public List<MonitorCheck> getMonitorChecks(Long monitorId, int limit, Long userId) {

        getMonitorById(monitorId, userId);

        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException(
                    "Limit must be between 1 and 100"
            );
        }

        return monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(
                monitorId,
                PageRequest.of(0, limit)
        );
    }

    // ---------- statistics ----------

    public double calculateUptime(Long monitorId, Long userId) {

        List<MonitorCheck> checks = getMonitorChecks(monitorId, userId);

        if (checks.isEmpty()) {
            return 0.0;
        }

        long successfulChecks = checks.stream()
                .filter(check -> "UP".equals(check.getStatus()))
                .count();

        return (double) successfulChecks / checks.size() * 100;
    }

    public double calculateAverageResponseTime(Long monitorId, Long userId) {
        return responseTimes(getMonitorChecks(monitorId, userId)).getAverage();
    }

    public int calculateMinResponseTime(Long monitorId, Long userId) {
        IntSummaryStatistics stats = responseTimes(getMonitorChecks(monitorId, userId));
        return stats.getCount() == 0 ? 0 : stats.getMin();
    }

    public int calculateMaxResponseTime(Long monitorId, Long userId) {
        IntSummaryStatistics stats = responseTimes(getMonitorChecks(monitorId, userId));
        return stats.getCount() == 0 ? 0 : stats.getMax();
    }

    public long countFailedChecks(Long monitorId, Long userId) {
        return getMonitorChecks(monitorId, userId).stream()
                .filter(check -> "DOWN".equals(check.getStatus()))
                .count();
    }

    public MonitorStatisticsResponse getMonitorStatistics(Long monitorId, Long userId) {

        List<MonitorCheck> checks = getMonitorChecks(monitorId, userId);

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

        IntSummaryStatistics responseStats = responseTimes(checks);

        return new MonitorStatisticsResponse(
                totalChecks,
                failedChecks,
                uptime,
                responseStats.getAverage(),
                responseStats.getCount() == 0 ? 0 : responseStats.getMin(),
                responseStats.getCount() == 0 ? 0 : responseStats.getMax()
        );
    }

    /** Response times of checks that have one (timeouts and connection errors may not). */
    private IntSummaryStatistics responseTimes(List<MonitorCheck> checks) {
        return checks.stream()
                .filter(check -> check.getResponseTimeMs() != null)
                .mapToInt(MonitorCheck::getResponseTimeMs)
                .summaryStatistics();
    }
}
