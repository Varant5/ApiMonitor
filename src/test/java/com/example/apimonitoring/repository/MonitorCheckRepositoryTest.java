package com.example.apimonitoring.repository;

import com.example.apimonitoring.TestcontainersConfiguration;
import org.springframework.context.annotation.Import;
import com.example.apimonitoring.entity.Monitor;
import com.example.apimonitoring.entity.MonitorCheck;
import com.example.apimonitoring.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    private final Instant baseTime = Instant.parse("2026-01-01T12:00:00Z");

    @BeforeEach
    void setUp() {
        User owner = new User();
        owner.setEmail("owner@example.com");
        owner.setPasswordHash("not-relevant-here");
        entityManager.persist(owner);

        monitor = entityManager.persist(createMonitor("Google", "https://google.com", owner));
        otherMonitor = entityManager.persist(createMonitor("GitHub", "https://github.com", owner));
    }

    @Test
    void shouldReturnChecksOfMonitorOrderedFromNewest() {

        MonitorCheck oldest = createCheck(monitor, "UP", baseTime);
        MonitorCheck newest = createCheck(monitor, "DOWN", baseTime.plus(2, ChronoUnit.MINUTES));
        MonitorCheck middle = createCheck(monitor, "UP", baseTime.plus(1, ChronoUnit.MINUTES));

        List<MonitorCheck> result =
                monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(monitor.getId());

        assertThat(result).containsExactly(newest, middle, oldest);
    }

    @Test
    void shouldNotReturnChecksOfOtherMonitors() {

        MonitorCheck ownCheck = createCheck(monitor, "UP", baseTime);
        createCheck(otherMonitor, "UP", baseTime.plus(1, ChronoUnit.MINUTES));

        List<MonitorCheck> result =
                monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(monitor.getId());

        assertThat(result).containsExactly(ownCheck);
    }

    @Test
    void shouldLimitNumberOfReturnedChecks() {

        createCheck(monitor, "UP", baseTime);
        MonitorCheck second = createCheck(monitor, "UP", baseTime.plus(1, ChronoUnit.MINUTES));
        MonitorCheck third = createCheck(monitor, "UP", baseTime.plus(2, ChronoUnit.MINUTES));

        List<MonitorCheck> result = monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(
                monitor.getId(),
                PageRequest.of(0, 2)
        );

        assertThat(result).containsExactly(third, second);
    }

    @Test
    void shouldFindLatestCheckOfMonitor() {

        createCheck(monitor, "UP", baseTime);
        MonitorCheck latest = createCheck(monitor, "DOWN", baseTime.plus(5, ChronoUnit.MINUTES));
        createCheck(otherMonitor, "UP", baseTime.plus(10, ChronoUnit.MINUTES));

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

    private Monitor createMonitor(String name, String url, User owner) {
        Monitor m = new Monitor();
        m.setName(name);
        m.setUrl(url);
        m.setCheckIntervalSeconds(60);
        m.setOwner(owner);
        return m;
    }

    private MonitorCheck createCheck(Monitor owner, String status, Instant checkedAt) {
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
