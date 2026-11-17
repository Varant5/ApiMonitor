package com.example.apimonitoring.monitoring;

import com.example.apimonitoring.entity.Monitor;
import com.example.apimonitoring.entity.MonitorCheck;
import com.example.apimonitoring.repository.MonitorCheckRepository;
import com.example.apimonitoring.service.MonitorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitorSchedulerTest {

    @Mock
    private MonitorService monitorService;

    @Mock
    private MonitorCheckRepository monitorCheckRepository;

    @InjectMocks
    private MonitorScheduler scheduler;

    @Test
    void neverCheckedMonitorIsDue() {
        Monitor monitor = monitor(1L, 60, true);
        when(monitorCheckRepository.findTopByMonitorIdOrderByCheckedAtDesc(1L)).thenReturn(Optional.empty());

        assertThat(scheduler.isCheckDue(monitor)).isTrue();
    }

    @Test
    void checkIsDueOnTheNextTickEvenIfTheLastOneWasSavedSlightlyLate() {
        // a 10 s monitor, last check saved 9.5 s ago (the HTTP call took ~0.5 s after the tick started)
        Monitor monitor = monitor(1L, 10, true);
        givenLastCheckSecondsAgo(1L, 9.5);

        // before the fix this was false and the monitor was effectively checked every 20 s
        assertThat(scheduler.isCheckDue(monitor)).isTrue();
    }

    @Test
    void checkIsNotDueWellBeforeTheInterval() {
        Monitor monitor = monitor(1L, 60, true);
        givenLastCheckSecondsAgo(1L, 30);

        assertThat(scheduler.isCheckDue(monitor)).isFalse();
    }

    @Test
    void checkIsNotDueRightAfterPreviousCheck() {
        // even the shortest allowed interval must not be checked twice in a row
        Monitor monitor = monitor(1L, 10, true);
        givenLastCheckSecondsAgo(1L, 0.1);

        assertThat(scheduler.isCheckDue(monitor)).isFalse();
    }

    @Test
    void runChecksSkipsDisabledMonitorsAndChecksDueOnes() {
        Monitor enabled = monitor(1L, 60, true);
        Monitor disabled = monitor(2L, 60, false);

        when(monitorService.getAllMonitorsForScheduling()).thenReturn(List.of(enabled, disabled));
        when(monitorCheckRepository.findTopByMonitorIdOrderByCheckedAtDesc(1L)).thenReturn(Optional.empty());

        scheduler.runChecks();

        verify(monitorService).runScheduledCheck(1L);
        verify(monitorService, never()).runScheduledCheck(2L);
    }

    @Test
    void failureOfOneMonitorDoesNotStopTheOthers() {
        Monitor first = monitor(1L, 60, true);
        Monitor second = monitor(2L, 60, true);

        when(monitorService.getAllMonitorsForScheduling()).thenReturn(List.of(first, second));
        when(monitorCheckRepository.findTopByMonitorIdOrderByCheckedAtDesc(anyLong())).thenReturn(Optional.empty());
        when(monitorService.runScheduledCheck(1L)).thenThrow(new RuntimeException("deleted meanwhile"));

        scheduler.runChecks();

        verify(monitorService).runScheduledCheck(2L);
    }

    private void givenLastCheckSecondsAgo(Long monitorId, double seconds) {
        MonitorCheck last = new MonitorCheck();
        last.setCheckedAt(Instant.now().minusNanos((long) (seconds * 1_000_000_000)));
        when(monitorCheckRepository.findTopByMonitorIdOrderByCheckedAtDesc(monitorId)).thenReturn(Optional.of(last));
    }

    private Monitor monitor(Long id, int intervalSeconds, boolean enabled) {
        Monitor monitor = new Monitor();
        monitor.setId(id);
        monitor.setName("Monitor " + id);
        monitor.setCheckIntervalSeconds(intervalSeconds);
        monitor.setEnabled(enabled);
        return monitor;
    }
}
