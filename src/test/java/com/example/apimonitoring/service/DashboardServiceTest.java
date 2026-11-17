package com.example.apimonitoring.service;

import com.example.apimonitoring.dto.DashboardResponse;
import com.example.apimonitoring.dto.DashboardResponse.MonitorSummary;
import com.example.apimonitoring.entity.Monitor;
import com.example.apimonitoring.entity.MonitorCheck;
import com.example.apimonitoring.repository.MonitorCheckRepository;
import com.example.apimonitoring.repository.MonitorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final Long USER_ID = 1L;
    private static final Instant NOW = Instant.parse("2026-11-17T12:00:00Z");

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private MonitorCheckRepository monitorCheckRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void shouldCountMonitorsByStateAndCalculateUptime() {

        Monitor upMonitor = monitor(1L, true);
        Monitor downMonitor = monitor(2L, true);
        Monitor pendingMonitor = monitor(3L, true);
        Monitor pausedMonitor = monitor(4L, false);

        when(monitorRepository.findAllByOwnerIdOrderByIdAsc(USER_ID))
                .thenReturn(List.of(upMonitor, downMonitor, pendingMonitor, pausedMonitor));

        // newest first, as returned by the repository
        when(monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(1L))
                .thenReturn(List.of(check("UP", 120, 200), check("DOWN", null, null), check("UP", 90, 200)));
        when(monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(2L))
                .thenReturn(List.of(check("DOWN", 30, 500)));
        when(monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(3L))
                .thenReturn(List.of());
        // paused, but its old history still counts
        when(monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(4L))
                .thenReturn(List.of(check("UP", 50, 200)));

        DashboardResponse dashboard = dashboardService.getDashboard(USER_ID);

        assertThat(dashboard.totalMonitors()).isEqualTo(4);
        assertThat(dashboard.up()).isEqualTo(1);
        assertThat(dashboard.down()).isEqualTo(1);
        assertThat(dashboard.pending()).isEqualTo(1);
        assertThat(dashboard.paused()).isEqualTo(1);
        // 3 UP checks out of 5
        assertThat(dashboard.overallUptime()).isEqualTo(60.0);

        MonitorSummary first = dashboard.monitors().getFirst();
        assertThat(first.lastStatus()).isEqualTo("UP");
        assertThat(first.lastResponseTimeMs()).isEqualTo(120);
        assertThat(first.lastHttpStatusCode()).isEqualTo(200);
        assertThat(first.totalChecks()).isEqualTo(3);
        assertThat(first.uptime()).isCloseTo(66.67, org.assertj.core.data.Offset.offset(0.01));

        MonitorSummary pending = dashboard.monitors().get(2);
        assertThat(pending.lastStatus()).isNull();
        assertThat(pending.lastCheckedAt()).isNull();
        assertThat(pending.uptime()).isZero();
    }

    @Test
    void shouldReturnEmptyDashboardForUserWithoutMonitors() {

        when(monitorRepository.findAllByOwnerIdOrderByIdAsc(USER_ID)).thenReturn(List.of());

        DashboardResponse dashboard = dashboardService.getDashboard(USER_ID);

        assertThat(dashboard.totalMonitors()).isZero();
        assertThat(dashboard.overallUptime()).isZero();
        assertThat(dashboard.monitors()).isEmpty();
    }

    private Monitor monitor(Long id, boolean enabled) {
        Monitor monitor = new Monitor();
        monitor.setId(id);
        monitor.setName("Monitor " + id);
        monitor.setUrl("https://example.com/" + id);
        monitor.setCheckIntervalSeconds(60);
        monitor.setEnabled(enabled);
        return monitor;
    }

    private MonitorCheck check(String status, Integer responseTimeMs, Integer httpStatusCode) {
        MonitorCheck check = new MonitorCheck();
        check.setStatus(status);
        check.setResponseTimeMs(responseTimeMs);
        check.setHttpStatusCode(httpStatusCode);
        check.setCheckedAt(NOW);
        return check;
    }
}
