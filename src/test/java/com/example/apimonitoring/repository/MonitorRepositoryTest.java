package com.example.apimonitoring.repository;

import com.example.apimonitoring.TestcontainersConfiguration;
import org.springframework.context.annotation.Import;
import com.example.apimonitoring.entity.Monitor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class MonitorRepositoryTest {

    @Autowired
    private MonitorRepository monitorRepository;

    @Test
    void shouldFindMonitorByUrl() {

        monitorRepository.save(createMonitor("Google", "https://google.com"));

        Optional<Monitor> result = monitorRepository.findByUrl("https://google.com");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Google");
    }

    @Test
    void shouldReturnEmptyWhenUrlDoesNotExist() {

        monitorRepository.save(createMonitor("Google", "https://google.com"));

        Optional<Monitor> result = monitorRepository.findByUrl("https://not-existing.com");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnTrueWhenMonitorWithUrlExists() {

        monitorRepository.save(createMonitor("Google", "https://google.com"));

        assertThat(monitorRepository.existsByUrl("https://google.com")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenMonitorWithUrlDoesNotExist() {

        assertThat(monitorRepository.existsByUrl("https://google.com")).isFalse();
    }

    @Test
    void shouldSetCreatedAtAndEnabledWhenMonitorIsSaved() {

        Monitor saved = monitorRepository.saveAndFlush(createMonitor("Google", "https://google.com"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.isEnabled()).isTrue();
    }

    private Monitor createMonitor(String name, String url) {
        Monitor monitor = new Monitor();
        monitor.setName(name);
        monitor.setUrl(url);
        monitor.setCheckIntervalSeconds(60);
        return monitor;
    }
}
