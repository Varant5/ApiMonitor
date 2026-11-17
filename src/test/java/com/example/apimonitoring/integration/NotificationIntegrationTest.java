package com.example.apimonitoring.integration;

import com.example.apimonitoring.TestcontainersConfiguration;
import com.example.apimonitoring.entity.Monitor;
import com.example.apimonitoring.entity.User;
import com.example.apimonitoring.repository.MonitorRepository;
import com.example.apimonitoring.repository.UserRepository;
import com.example.apimonitoring.service.MonitorService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Whole notification path on a real database:
 * scheduled check -> status change detected -> event -> async listener -> email to the owner.
 *
 * Uses runScheduledCheck() on purpose: it runs outside any web request, so it also proves that
 * reading the owner (a lazy relation) does not fail in a background thread.
 */
@SpringBootTest(properties = "monitoring.scheduler.enabled=false")
@Import(TestcontainersConfiguration.class)
class NotificationIntegrationTest {

    /** Replaces the real SMTP client — we only check what would be sent. */
    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private MonitorService monitorService;

    @Autowired
    private MonitorRepository monitorRepository;

    @Autowired
    private UserRepository userRepository;

    private HttpServer fakeApi;
    private final AtomicInteger fakeApiStatus = new AtomicInteger(200);
    private Monitor monitor;

    @BeforeEach
    void setUp() throws IOException {
        fakeApi = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        fakeApi.createContext("/health", exchange -> {
            exchange.sendResponseHeaders(fakeApiStatus.get(), -1);
            exchange.close();
        });
        fakeApi.start();

        User owner = new User();
        owner.setEmail("owner@example.com");
        owner.setPasswordHash("not-relevant-here");
        owner = userRepository.save(owner);

        Monitor m = new Monitor();
        m.setName("Fake API");
        m.setUrl("http://localhost:" + fakeApi.getAddress().getPort() + "/health");
        m.setCheckIntervalSeconds(60);
        m.setOwner(owner);
        monitor = monitorRepository.save(m);
    }

    @AfterEach
    void cleanUp() {
        fakeApi.stop(0);
        userRepository.deleteAll();
    }

    @Test
    void shouldEmailOwnerOnOutageAndOnRecoveryOnly() {

        // 1. first check UP -> normal state, no email
        fakeApiStatus.set(200);
        monitorService.runScheduledCheck(monitor.getId());

        // 2. API breaks -> DOWN email
        fakeApiStatus.set(500);
        monitorService.runScheduledCheck(monitor.getId());

        // 3. still broken -> no second email
        monitorService.runScheduledCheck(monitor.getId());

        // 4. API is back -> RECOVERED email
        fakeApiStatus.set(200);
        monitorService.runScheduledCheck(monitor.getId());

        // emails are sent asynchronously, so wait (up to 5 s) for exactly two
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(5000).times(2)).send(captor.capture());

        List<SimpleMailMessage> emails = captor.getAllValues();
        assertThat(emails)
                .extracting(SimpleMailMessage::getSubject)
                .containsExactlyInAnyOrder(
                        "[DOWN] Fake API is not responding",
                        "[RECOVERED] Fake API is back up"
                );
        assertThat(emails)
                .allSatisfy(email -> assertThat(email.getTo()).containsExactly("owner@example.com"));
    }
}
