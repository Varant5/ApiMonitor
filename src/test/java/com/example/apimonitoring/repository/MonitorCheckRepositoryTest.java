package com.example.apimonitoring.repository;

import com.example.apimonitoring.TestcontainersConfiguration;
import org.springframework.context.annotation.Import;
import com.example.apimonitoring.entity.Monitor;
import com.example.apimonitoring.entity.MonitorCheck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class MonitorCheckRepositoryTest {

    @Autowired
    private MonitorCheckRepository monitorCheckRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Monitor monitor;
    private Monitor otherMonitor;

    private final LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 12, 0);

    @BeforeEach
    void setUp() {
        monitor = entityManager.persist(createMonitor("Google", "https://google.com"));
        otherMonitor = entityManager.persist(createMonitor("GitHub", "https://github.com"));
    }

    @Test
    void shouldReturnChecksOfMonitorOrderedFromNewest() {

        MonitorCheck oldest = createCheck(monitor, "UP", baseTime);
        MonitorCheck newest = createCheck(monitor, "DOWN", baseTime.plusMinutes(2));
        MonitorCheck middle = createCheck(monitor, "UP", baseTime.plusMinutes(1));

        List<MonitorCheck> result =
                monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(monitor.getId());

        assertThat(result).containsExactly(newest, middle, oldest);
    }

    @Test
    void shouldNotReturnChecksOfOtherMonitors() {

        MonitorCheck ownCheck = createCheck(monitor, "UP", baseTime);
        createCheck(otherMonitor, "UP", baseTime.plusMinutes(1));

        List<MonitorCheck> result =
                monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(monitor.getId());

        assertThat(result).containsExactly(ownCheck);
    }

    @Test
    void shouldLimitNumberOfReturnedChecks() {

        createCheck(monitor, "UP", baseTime);
        MonitorCheck second = createCheck(monitor, "UP", baseTime.plusMinutes(1));
        MonitorCheck third = createCheck(monitor, "UP", baseTime.plusMinutes(2));

        List<MonitorCheck> result = monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(
                monitor.getId(),
                PageRequest.of(0, 2)
        );

        assertThat(result).containsExactly(third, second);
    }

    @Test
    void shouldFindLatestCheckOfMonitor() {

        createCheck(monitor, "UP", baseTime);
        MonitorCheck latest = createCheck(monitor, "DOWN", baseTime.plusMinutes(5));
        createCheck(otherMonitor, "UP", baseTime.plusMinutes(10));

        Optional<MonitorCheck> result =
                monitorCheckRepository.findTopByMonitorIdOrderByCheckedAtDesc(monitor.getId());

        assertThat(result).contains(latest);
        assertThat(result.get().getStatus()).isEqualTo("DOWN");
    }

    @Test
    void shouldReturnEmptyWhenMonitorHasNoChecks() {

        Optional<MonitorCheck> result =
                monitorCheckRepository.findTopByMonitorIdOrderByCheckedAtDesc(monitor.getId());

        assertThat(result).isEmpty();
    }

    private Monitor createMonitor(String name, String url) {
        Monitor m = new Monitor();
        m.setName(name);
        m.setUrl(url);
        m.setCheckIntervalSeconds(60);
        return m;
    }

    private MonitorCheck createCheck(Monitor owner, String status, LocalDateTime checkedAt) {
        MonitorCheck check = new MonitorCheck();
        check.setMonitor(owner);
        check.setStatus(status);
        check.setHttpStatusCode("UP".equals(status) ? 200 : 500);
        check.setResponseTimeMs(100);

        entityManager.persist(check);
        check.setCheckedAt(checkedAt);
        entityManager.flush();

        return check;
    }
}
