package com.example.apimonitoring.service;

import com.example.apimonitoring.dto.CreateMonitorRequest;
import com.example.apimonitoring.dto.MonitorStatisticsResponse;
import com.example.apimonitoring.dto.UpdateMonitorStatusRequest;
import com.example.apimonitoring.entity.Monitor;
import com.example.apimonitoring.entity.MonitorCheck;
import com.example.apimonitoring.entity.User;
import com.example.apimonitoring.exception.MonitorNotFoundException;
import com.example.apimonitoring.monitoring.MonitorCheckResult;
import com.example.apimonitoring.monitoring.MonitorHttpClient;
import com.example.apimonitoring.monitoring.MonitorStatus;
import com.example.apimonitoring.repository.MonitorCheckRepository;
import com.example.apimonitoring.repository.MonitorRepository;
import com.example.apimonitoring.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitorServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private MonitorCheckRepository monitorCheckRepository;

    @Mock
    private MonitorHttpClient monitorHttpClient;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MonitorService monitorService;

    // ---------- getMonitorById ----------

    @Test
    void shouldReturnMonitorWhenMonitorExists() {

        Monitor monitor = createMonitor(1L, "Google");

        when(monitorRepository.findByIdAndOwnerId(1L, USER_ID))
                .thenReturn(Optional.of(monitor));

        Monitor result = monitorService.getMonitorById(1L, USER_ID);

        assertNotNull(result);
        assertEquals("Google", result.getName());

        verify(monitorRepository).findByIdAndOwnerId(1L, USER_ID);
    }

    @Test
    void shouldThrowExceptionWhenMonitorDoesNotExist() {

        when(monitorRepository.findByIdAndOwnerId(1L, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                MonitorNotFoundException.class,
                () -> monitorService.getMonitorById(1L, USER_ID)
        );

        verify(monitorRepository).findByIdAndOwnerId(1L, USER_ID);
    }

    @Test
    void shouldThrowNotFoundWhenMonitorBelongsToAnotherUser() {

        // the repository query filters by owner -> another user's monitor is simply not found
        when(monitorRepository.findByIdAndOwnerId(1L, OTHER_USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                MonitorNotFoundException.class,
                () -> monitorService.getMonitorById(1L, OTHER_USER_ID)
        );

        // must never fall back to a lookup without the owner
        verify(monitorRepository, never()).findById(any());
    }

    // ---------- list / create ----------

    @Test
    void shouldReturnOnlyMonitorsOfUser() {

        when(monitorRepository.findAllByOwnerIdOrderByIdAsc(USER_ID))
                .thenReturn(List.of(createMonitor(1L, "Google"), createMonitor(2L, "GitHub")));

        List<Monitor> result = monitorService.getMonitorsOfUser(USER_ID);

        assertEquals(2, result.size());
        assertEquals("Google", result.get(0).getName());
        assertEquals("GitHub", result.get(1).getName());

        verify(monitorRepository).findAllByOwnerIdOrderByIdAsc(USER_ID);
        verify(monitorRepository, never()).findAll();
    }

    @Test
    void shouldCreateMonitorOwnedByUser() {

        CreateMonitorRequest request = new CreateMonitorRequest();
        request.setName("Google");
        request.setUrl("https://google.com");
        request.setCheckIntervalSeconds(60);

        User owner = new User();
        owner.setId(USER_ID);

        when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);
        when(monitorRepository.save(any(Monitor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Monitor result = monitorService.createMonitor(request, USER_ID);

        assertNotNull(result);
        assertEquals("Google", result.getName());
        assertEquals("https://google.com", result.getUrl());
        assertEquals(60, result.getCheckIntervalSeconds());
        assertSame(owner, result.getOwner());

        verify(monitorRepository).save(any(Monitor.class));
    }

    // ---------- delete / update ----------

    @Test
    void shouldDeleteMonitorWhenMonitorExists() {

        Monitor monitor = createMonitor(1L, "Google");

        when(monitorRepository.findByIdAndOwnerId(1L, USER_ID))
                .thenReturn(Optional.of(monitor));

        monitorService.deleteMonitor(1L, USER_ID);

        verify(monitorRepository).delete(monitor);
    }

    @Test
    void shouldNotDeleteMonitorOfAnotherUser() {

        when(monitorRepository.findByIdAndOwnerId(1L, OTHER_USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                MonitorNotFoundException.class,
                () -> monitorService.deleteMonitor(1L, OTHER_USER_ID)
        );

        verify(monitorRepository, never()).delete(any());
    }

    @Test
    void shouldEnableMonitorWhenStatusIsUpdatedToTrue() {

        Monitor monitor = createMonitor(1L, "Google");
        monitor.setEnabled(false);

        UpdateMonitorStatusRequest request = new UpdateMonitorStatusRequest();
        request.setEnabled(true);

        when(monitorRepository.findByIdAndOwnerId(1L, USER_ID))
                .thenReturn(Optional.of(monitor));
        when(monitorRepository.save(monitor))
                .thenReturn(monitor);

        Monitor result = monitorService.updateMonitorStatus(1L, request, USER_ID);

        assertTrue(result.isEnabled());

        verify(monitorRepository).save(monitor);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingStatusOfNonExistingMonitor() {

        UpdateMonitorStatusRequest request = new UpdateMonitorStatusRequest();
        request.setEnabled(true);

        when(monitorRepository.findByIdAndOwnerId(1L, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                MonitorNotFoundException.class,
                () -> monitorService.updateMonitorStatus(1L, request, USER_ID)
        );

        verify(monitorRepository, never()).save(any());
    }

    // ---------- checks ----------

    @Test
    void shouldCheckMonitorAndSaveResult() {

        Monitor monitor = createMonitor(1L, "Google");
        monitor.setUrl("https://google.com");

        when(monitorRepository.findByIdAndOwnerId(1L, USER_ID))
                .thenReturn(Optional.of(monitor));
        when(monitorHttpClient.checkUrl("https://google.com"))
                .thenReturn(new MonitorCheckResult(MonitorStatus.UP, 200, 150L, null));
        when(monitorCheckRepository.save(any(MonitorCheck.class)))
                .thenReturn(new MonitorCheck());

        MonitorCheck response = monitorService.checkMonitor(1L, USER_ID);

        assertNotNull(response);

        verify(monitorHttpClient).checkUrl("https://google.com");
        verify(monitorCheckRepository).save(any(MonitorCheck.class));
    }

    @Test
    void shouldSaveCorrectMonitorCheckData() {

        Monitor monitor = createMonitor(1L, "Google");
        monitor.setUrl("https://google.com");

        when(monitorRepository.findByIdAndOwnerId(1L, USER_ID))
                .thenReturn(Optional.of(monitor));
        when(monitorHttpClient.checkUrl("https://google.com"))
                .thenReturn(new MonitorCheckResult(MonitorStatus.UP, 200, 150L, null));
        when(monitorCheckRepository.save(any(MonitorCheck.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MonitorCheck response = monitorService.checkMonitor(1L, USER_ID);

        assertNotNull(response);
        assertEquals(monitor, response.getMonitor());
        assertEquals("UP", response.getStatus());
        assertEquals(200, response.getHttpStatusCode());
        assertEquals(150, response.getResponseTimeMs());
    }

    @Test
    void shouldThrowExceptionWhenCheckingNonExistingMonitor() {

        when(monitorRepository.findByIdAndOwnerId(1L, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                MonitorNotFoundException.class,
                () -> monitorService.checkMonitor(1L, USER_ID)
        );

        verifyNoInteractions(monitorHttpClient);
        verifyNoInteractions(monitorCheckRepository);
    }

    @Test
    void scheduledCheckShouldWorkWithoutOwnerFilter() {

        Monitor monitor = createMonitor(1L, "Google");
        monitor.setUrl("https://google.com");

        when(monitorRepository.findById(1L)).thenReturn(Optional.of(monitor));
        when(monitorHttpClient.checkUrl("https://google.com"))
                .thenReturn(new MonitorCheckResult(MonitorStatus.DOWN, 500, 80L, "error"));
        when(monitorCheckRepository.save(any(MonitorCheck.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MonitorCheck response = monitorService.runScheduledCheck(1L);

        assertEquals("DOWN", response.getStatus());
        verify(monitorRepository, never()).findByIdAndOwnerId(any(), any());
    }

    @Test
    void schedulerShouldSeeMonitorsOfAllUsers() {

        when(monitorRepository.findAll())
                .thenReturn(List.of(createMonitor(1L, "Google"), createMonitor(2L, "GitHub")));

        assertEquals(2, monitorService.getAllMonitorsForScheduling().size());
    }

    // ---------- history ----------

    @Test
    void shouldReturnMonitorChecks() {

        MonitorCheck check1 = new MonitorCheck();
        check1.setStatus("UP");

        MonitorCheck check2 = new MonitorCheck();
        check2.setStatus("DOWN");

        givenMonitorWithChecks(List.of(check1, check2));

        List<MonitorCheck> result = monitorService.getMonitorChecks(1L, USER_ID);

        assertEquals(2, result.size());
        assertEquals("UP", result.get(0).getStatus());
        assertEquals("DOWN", result.get(1).getStatus());
    }

    @Test
    void shouldReturnLimitedMonitorChecks() {

        when(monitorRepository.findByIdAndOwnerId(1L, USER_ID))
                .thenReturn(Optional.of(createMonitor(1L, "Google")));
        when(monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(eq(1L), any(PageRequest.class)))
                .thenReturn(List.of(new MonitorCheck(), new MonitorCheck()));

        List<MonitorCheck> result = monitorService.getMonitorChecks(1L, 2, USER_ID);

        assertEquals(2, result.size());

        verify(monitorCheckRepository)
                .findByMonitorIdOrderByCheckedAtDesc(eq(1L), eq(PageRequest.of(0, 2)));
    }

    @Test
    void shouldThrowExceptionWhenLimitIsInvalid() {

        when(monitorRepository.findByIdAndOwnerId(1L, USER_ID))
                .thenReturn(Optional.of(createMonitor(1L, "Google")));

        assertThrows(
                IllegalArgumentException.class,
                () -> monitorService.getMonitorChecks(1L, 0, USER_ID)
        );

        verify(monitorCheckRepository, never())
                .findByMonitorIdOrderByCheckedAtDesc(eq(1L), any(PageRequest.class));
    }

    @Test
    void shouldNotReturnChecksOfAnotherUsersMonitor() {

        when(monitorRepository.findByIdAndOwnerId(1L, OTHER_USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                MonitorNotFoundException.class,
                () -> monitorService.getMonitorChecks(1L, 20, OTHER_USER_ID)
        );

        verifyNoInteractions(monitorCheckRepository);
    }

    // ---------- statistics ----------

    @Test
    void shouldCalculateUptime() {

        givenMonitorWithChecks(List.of(
                createCheck("UP", 100),
                createCheck("UP", 150),
                createCheck("DOWN", null),
                createCheck("UP", 200)
        ));

        assertEquals(75.0, monitorService.calculateUptime(1L, USER_ID));
    }

    @Test
    void shouldReturnZeroUptimeWhenNoChecksExist() {

        givenMonitorWithChecks(List.of());

        assertEquals(0.0, monitorService.calculateUptime(1L, USER_ID));
    }

    @Test
    void shouldCalculateAverageResponseTime() {

        givenMonitorWithChecks(List.of(
                createCheck("UP", 100),
                createCheck("UP", 200),
                createCheck("DOWN", null)
        ));

        assertEquals(150.0, monitorService.calculateAverageResponseTime(1L, USER_ID));
    }

    @Test
    void shouldCalculateMinAndMaxResponseTime() {

        givenMonitorWithChecks(List.of(
                createCheck("UP", 120),
                createCheck("UP", 80),
                createCheck("DOWN", null),
                createCheck("UP", 300)
        ));

        assertEquals(80, monitorService.calculateMinResponseTime(1L, USER_ID));
        assertEquals(300, monitorService.calculateMaxResponseTime(1L, USER_ID));
    }

    @Test
    void shouldCountFailedChecks() {

        givenMonitorWithChecks(List.of(
                createCheck("UP", 100),
                createCheck("DOWN", null),
                createCheck("DOWN", null),
                createCheck("UP", 200)
        ));

        assertEquals(2, monitorService.countFailedChecks(1L, USER_ID));
    }

    @Test
    void shouldReturnCorrectMonitorStatistics() {

        givenMonitorWithChecks(List.of(
                createCheck("UP", 100),
                createCheck("UP", 200),
                createCheck("DOWN", null),
                createCheck("UP", 300)
        ));

        MonitorStatisticsResponse result = monitorService.getMonitorStatistics(1L, USER_ID);

        assertEquals(4, result.getTotalChecks());
        assertEquals(1, result.getFailedChecks());
        assertEquals(75.0, result.getUptime());
        assertEquals(200.0, result.getAverageResponseTimeMs());
        assertEquals(100, result.getMinResponseTimeMs());
        assertEquals(300, result.getMaxResponseTimeMs());
    }

    @Test
    void shouldReturnZeroStatisticsWhenNoChecksExist() {

        givenMonitorWithChecks(List.of());

        MonitorStatisticsResponse result = monitorService.getMonitorStatistics(1L, USER_ID);

        assertEquals(0, result.getTotalChecks());
        assertEquals(0, result.getFailedChecks());
        assertEquals(0.0, result.getUptime());
        assertEquals(0.0, result.getAverageResponseTimeMs());
        assertEquals(0, result.getMinResponseTimeMs());
        assertEquals(0, result.getMaxResponseTimeMs());
    }

    // ---------- helpers ----------

    /** Monitor 1 belongs to USER_ID and has the given check history. */
    private void givenMonitorWithChecks(List<MonitorCheck> checks) {
        when(monitorRepository.findByIdAndOwnerId(1L, USER_ID))
                .thenReturn(Optional.of(createMonitor(1L, "Google")));
        when(monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(1L))
                .thenReturn(checks);
    }

    private Monitor createMonitor(Long id, String name) {
        Monitor monitor = new Monitor();
        monitor.setId(id);
        monitor.setName(name);
        return monitor;
    }

    private MonitorCheck createCheck(String status, Integer responseTime) {
        MonitorCheck check = new MonitorCheck();
        check.setStatus(status);
        check.setResponseTimeMs(responseTime);
        return check;
    }
}
