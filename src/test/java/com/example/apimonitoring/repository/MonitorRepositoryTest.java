package com.example.apimonitoring.repository;

import com.example.apimonitoring.TestcontainersConfiguration;
import com.example.apimonitoring.entity.Monitor;
import com.example.apimonitoring.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class MonitorRepositoryTest {

    @Autowired
    private MonitorRepository monitorRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = entityManager.persist(createUser("alice@example.com"));
        bob = entityManager.persist(createUser("bob@example.com"));
    }

    @Test
    void shouldFindMonitorByUrl() {

        monitorRepository.save(createMonitor("Google", "https://google.com", alice));

        Optional<Monitor> result = monitorRepository.findByUrl("https://google.com");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Google");
    }

    @Test
    void shouldReturnEmptyWhenUrlDoesNotExist() {

        monitorRepository.save(createMonitor("Google", "https://google.com", alice));

        Optional<Monitor> result = monitorRepository.findByUrl("https://not-existing.com");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnTrueWhenMonitorWithUrlExists() {

        monitorRepository.save(createMonitor("Google", "https://google.com", alice));

        assertThat(monitorRepository.existsByUrl("https://google.com")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenMonitorWithUrlDoesNotExist() {

        assertThat(monitorRepository.existsByUrl("https://google.com")).isFalse();
    }

    @Test
    void shouldSetCreatedAtAndEnabledWhenMonitorIsSaved() {

        Monitor saved = monitorRepository.saveAndFlush(createMonitor("Google", "https://google.com", alice));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.isEnabled()).isTrue();
    }

    // ---------- ownership ----------

    @Test
    void shouldReturnOnlyMonitorsOfGivenOwner() {

        Monitor google = monitorRepository.save(createMonitor("Google", "https://google.com", alice));
        Monitor github = monitorRepository.save(createMonitor("GitHub", "https://github.com", alice));
        monitorRepository.save(createMonitor("Bob's API", "https://bob.example.com", bob));

        List<Monitor> result = monitorRepository.findAllByOwnerIdOrderByIdAsc(alice.getId());

        assertThat(result).containsExactly(google, github);
    }

    @Test
    void shouldFindMonitorByIdOnlyForItsOwner() {

        Monitor alicesMonitor = monitorRepository.save(createMonitor("Google", "https://google.com", alice));

        assertThat(monitorRepository.findByIdAndOwnerId(alicesMonitor.getId(), alice.getId()))
                .contains(alicesMonitor);

        assertThat(monitorRepository.findByIdAndOwnerId(alicesMonitor.getId(), bob.getId()))
                .isEmpty();
    }

    // ---------- helpers ----------

    private User createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("not-relevant-here");
        return user;
    }

    private Monitor createMonitor(String name, String url, User owner) {
        Monitor monitor = new Monitor();
        monitor.setName(name);
        monitor.setUrl(url);
        monitor.setCheckIntervalSeconds(60);
        monitor.setOwner(owner);
        return monitor;
    }
}
