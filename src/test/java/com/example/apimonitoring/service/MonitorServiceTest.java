package com.example.apimonitoring.service;

import com.example.apimonitoring.dto.CreateMonitorRequest;
import com.example.apimonitoring.dto.MonitorStatisticsResponse;
import com.example.apimonitoring.dto.UpdateMonitorStatusRequest;
import com.example.apimonitoring.entity.Monitor;
import com.example.apimonitoring.entity.MonitorCheck;
import com.example.apimonitoring.exception.MonitorNotFoundException;
import com.example.apimonitoring.monitoring.MonitorCheckResult;
import com.example.apimonitoring.monitoring.MonitorHttpClient;
import com.example.apimonitoring.monitoring.MonitorStatus;
import com.example.apimonitoring.repository.MonitorCheckRepository;
import com.example.apimonitoring.repository.MonitorRepository;

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

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private MonitorCheckRepository monitorCheckRepository;

    @Mock
    private MonitorHttpClient monitorHttpClient;

    @InjectMocks
    private MonitorService monitorService;

    @Test
    void shouldReturnMonitorWhenMonitorExists() {

        Monitor monitor = new Monitor();
        monitor.setId(1L);
        monitor.setName("Google");

        when(monitorRepository.findById(1L))
                .thenReturn(Optional.of(monitor));

        Monitor result = monitorService.getMonitorById(1L);

        assertNotNull(result);
        assertEquals("Google", result.getName());

        verify(monitorRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenMonitorDoesNotExist() {

        when(monitorRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                MonitorNotFoundException.class,
                () -> monitorService.getMonitorById(1L)
        );

        verify(monitorRepository).findById(1L);
    }

    @Test
    void shouldDeleteMonitorWhenMonitorExists() {

        Monitor monitor = new Monitor();
        monitor.setId(1L);
        monitor.setName("Google");

        when(monitorRepository.findById(1L))
                .thenReturn(Optional.of(monitor));

        monitorService.deleteMonitor(1L);

        verify(monitorRepository).findById(1L);
        verify(monitorRepository).delete(monitor);
    }

    @Test
    void shouldEnableMonitorWhenStatusIsUpdatedToTrue() {

        Monitor monitor = new Monitor();
        monitor.setId(1L);
        monitor.setName("Google");
        monitor.setEnabled(false);

        UpdateMonitorStatusRequest request = new UpdateMonitorStatusRequest();
        request.setEnabled(true);

        when(monitorRepository.findById(1L))
                .thenReturn(Optional.of(monitor));

        when(monitorRepository.save(monitor))
                .thenReturn(monitor);

        Monitor result = monitorService.updateMonitorStatus(1L, request);

        assertTrue(result.isEnabled());

        verify(monitorRepository).findById(1L);
        verify(monitorRepository).save(monitor);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingStatusOfNonExistingMonitor() {

        UpdateMonitorStatusRequest request = new UpdateMonitorStatusRequest();
        request.setEnabled(true);

        when(monitorRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                MonitorNotFoundException.class,
                () -> monitorService.updateMonitorStatus(1L, request)
        );

        verify(monitorRepository).findById(1L);
        verify(monitorRepository, never()).save(any());
    }

    @Test
    void shouldCheckMonitorAndSaveResult() {

        Monitor monitor = new Monitor();
        monitor.setId(1L);
        monitor.setName("Google");
        monitor.setUrl("https://google.com");

        MonitorCheckResult result = new MonitorCheckResult(
                MonitorStatus.UP,
                200,
                150L,
                null
        );

        MonitorCheck savedCheck = new MonitorCheck();

        when(monitorRepository.findById(1L))
                .thenReturn(Optional.of(monitor));

        when(monitorHttpClient.checkUrl("https://google.com"))
                .thenReturn(result);

        when(monitorCheckRepository.save(any(MonitorCheck.class)))
                .thenReturn(savedCheck);

        MonitorCheck response = monitorService.checkMonitor(1L);

        assertNotNull(response);

        verify(monitorRepository).findById(1L);
        verify(monitorHttpClient).checkUrl("https://google.com");
        verify(monitorCheckRepository).save(any(MonitorCheck.class));
    }

    @Test
    void shouldSaveCorrectMonitorCheckData() {

        Monitor monitor = new Monitor();
        monitor.setId(1L);
        monitor.setName("Google");
        monitor.setUrl("https://google.com");

        MonitorCheckResult result = new MonitorCheckResult(
                MonitorStatus.UP,
                200,
                150L,
                null
        );

        when(monitorRepository.findById(1L))
                .thenReturn(Optional.of(monitor));

        when(monitorHttpClient.checkUrl("https://google.com"))
                .thenReturn(result);

        when(monitorCheckRepository.save(any(MonitorCheck.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MonitorCheck response = monitorService.checkMonitor(1L);

        assertNotNull(response);
        assertEquals(monitor, response.getMonitor());
        assertEquals("UP", response.getStatus());
        assertEquals(200, response.getHttpStatusCode());
        assertEquals(150, response.getResponseTimeMs());
    }

    @Test
    void shouldThrowExceptionWhenCheckingNonExistingMonitor() {

        when(monitorRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                MonitorNotFoundException.class,
                () -> monitorService.checkMonitor(1L)
        );

        verify(monitorRepository).findById(1L);

        verifyNoInteractions(monitorHttpClient);
        verifyNoInteractions(monitorCheckRepository);
    }

    @Test
    void shouldReturnAllMonitors() {

        Monitor monitor1 = new Monitor();
        monitor1.setId(1L);
        monitor1.setName("Google");

        Monitor monitor2 = new Monitor();
        monitor2.setId(2L);
        monitor2.setName("GitHub");

        when(monitorRepository.findAll())
                .thenReturn(List.of(monitor1, monitor2));

        List<Monitor> result = monitorService.getAllMonitors();

        assertEquals(2, result.size());
        assertEquals("Google", result.get(0).getName());
        assertEquals("GitHub", result.get(1).getName());

        verify(monitorRepository).findAll();
    }

    @Test
    void shouldCreateMonitor() {

        CreateMonitorRequest request = new CreateMonitorRequest();
        request.setName("Google");
        request.setUrl("https://google.com");
        request.setCheckIntervalSeconds(60);

        when(monitorRepository.save(any(Monitor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Monitor result = monitorService.createMonitor(request);

        assertNotNull(result);
        assertEquals("Google", result.getName());
        assertEquals("https://google.com", result.getUrl());
        assertEquals(60, result.getCheckIntervalSeconds());

        verify(monitorRepository).save(any(Monitor.class));
    }

    @Test
    void shouldReturnMonitorChecks() {

        Monitor monitor = new Monitor();
        monitor.setId(1L);

        MonitorCheck check1 = new MonitorCheck();
        check1.setStatus("UP");

        MonitorCheck check2 = new MonitorCheck();
        check2.setStatus("DOWN");

        List<MonitorCheck> checks = List.of(check1, check2);

        when(monitorRepository.findById(1L))
                .thenReturn(Optional.of(monitor));

        when(monitorCheckRepository
                .findByMonitorIdOrderByCheckedAtDesc(1L))
                .thenReturn(checks);

        List<MonitorCheck> result =
                monitorService.getMonitorChecks(1L);

        assertEquals(2, result.size());
        assertEquals("UP", result.get(0).getStatus());
        assertEquals("DOWN", result.get(1).getStatus());

        verify(monitorCheckRepository)
                .findByMonitorIdOrderByCheckedAtDesc(1L);
    }

    @Test
    void shouldReturnLimitedMonitorChecks() {

        Monitor monitor = new Monitor();
        monitor.setId(1L);

        List<MonitorCheck> checks = List.of(
                new MonitorCheck(),
                new MonitorCheck()
        );

        when(monitorRepository.findById(1L))
                .thenReturn(Optional.of(monitor));

        when(monitorCheckRepository
                .findByMonitorIdOrderByCheckedAtDesc(
                        eq(1L),
                        any(PageRequest.class)
                ))
                .thenReturn(checks);

        List<MonitorCheck> result =
                monitorService.getMonitorChecks(1L, 2);

        assertEquals(2, result.size());

        verify(monitorCheckRepository)
                .findByMonitorIdOrderByCheckedAtDesc(
                        eq(1L),
                        eq(PageRequest.of(0, 2))
                );
    }

    @Test
    void shouldThrowExceptionWhenLimitIsInvalid() {

        Monitor monitor = new Monitor();
        monitor.setId(1L);

        when(monitorRepository.findById(1L))
                .thenReturn(Optional.of(monitor));

        assertThrows(
                IllegalArgumentException.class,
                () -> monitorService.getMonitorChecks(1L, 0)
        );

        verify(monitorCheckRepository, never())
                .findByMonitorIdOrderByCheckedAtDesc(
                        eq(1L),
                        any(PageRequest.class)
                );
    }

    @Test
    void shouldCalculateUptime() {

        Monitor monitor = new Monitor();
        monitor.setId(1L);

        when(monitorRepository.findById(1L))
                .thenReturn(Optional.of(monitor));

        when(monitorCheckRepository
                .findByMonitorIdOrderByCheckedAtDesc(1L))
                .thenReturn(List.of(
                        createCheck("UP", 100),
                        createCheck("UP", 150),
                        createCheck("DOWN", null),
                        createCheck("UP", 200)
                ));

        double result = monitorService.calculateUptime(1L);

        assertEquals(75.0, result);
    }

    @Test
    void shouldReturnZeroUptimeWhenNoChecksExist() {

        Monitor monitor = new Monitor();
        monitor.setId(1L);

        when(monitorRepository.findById(1L))
                .thenReturn(Optional.of(monitor));

        when(monitorCheckRepository
                .findByMonitorIdOrderByCheckedAtDesc(1L))
                .thenReturn(List.of());

        double result = monitorService.calculateUptime(1L);

        assertEquals(0.0, result);
    }

    @Test
    void shouldCalculateAverageResponseTime() {

        Monitor monitor = new Monitor();
        monitor.setId(1L);

        when(monitorRepository.findById(1L))
                .thenReturn(Optional.of(monitor));

        when(monitorCheckRepository
                .findByMonitorIdOrderByCheckedAtDesc(1L))
                .thenReturn(List.of(
                        createCheck("UP", 100),
                        createCheck("UP", 200),
                        createCheck("DOWN", null)
                ));

        double result =
                monitorService.calculateAverageResponseTime(1L);

        assertEquals(150.0, result);
    }

    @Test
    void shouldCountFailedChecks() {

        Monitor monitor = new Monitor();
        monitor.setId(1L);

        when(monitorRepository.findById(1L))
                .thenReturn(Optional.of(monitor));

        when(monitorCheckRepository
                .findByMonitorIdOrderByCheckedAtDesc(1L))
                .thenReturn(List.of(
                        createCheck("UP", 100),
                        createCheck("DOWN", null),
                        createCheck("DOWN", null),
                        createCheck("UP", 200)
                ));

        long result = monitorService.countFailedChecks(1L);

        assertEquals(2, result);
    }

    private MonitorCheck createCheck(String status, Integer responseTime) {

        MonitorCheck check = new MonitorCheck();
        check.setStatus(status);
        check.setResponseTimeMs(responseTime);

        return check;
    }

    @Test
    void shouldReturnCorrectMonitorStatistics() {

        Monitor monitor = new Monitor();
        monitor.setId(1L);

        when(monitorRepository.findById(1L))
                .thenReturn(Optional.of(monitor));

        when(monitorCheckRepository
                .findByMonitorIdOrderByCheckedAtDesc(1L))
                .thenReturn(List.of(
                        createCheck("UP", 100),
                        createCheck("UP", 200),
                        createCheck("DOWN", null),
                        createCheck("UP", 300)
                ));

        MonitorStatisticsResponse result =
                monitorService.getMonitorStatistics(1L);

        assertEquals(4, result.getTotalChecks());
        assertEquals(1, result.getFailedChecks());
        assertEquals(75.0, result.getUptime());
        assertEquals(200.0, result.getAverageResponseTimeMs());
        assertEquals(100, result.getMinResponseTimeMs());
        assertEquals(300, result.getMaxResponseTimeMs());
    }

    @Test
    void shouldReturnZeroStatisticsWhenNoChecksExist() {

        Monitor monitor = new Monitor();
        monitor.setId(1L);

        when(monitorRepository.findById(1L))
                .thenReturn(Optional.of(monitor));

        when(monitorCheckRepository
                .findByMonitorIdOrderByCheckedAtDesc(1L))
                .thenReturn(List.of());

        MonitorStatisticsResponse result =
                monitorService.getMonitorStatistics(1L);

        assertEquals(0, result.getTotalChecks());
        assertEquals(0, result.getFailedChecks());
        assertEquals(0.0, result.getUptime());
        assertEquals(0.0, result.getAverageResponseTimeMs());
        assertEquals(0, result.getMinResponseTimeMs());
        assertEquals(0, result.getMaxResponseTimeMs());
    }
}
